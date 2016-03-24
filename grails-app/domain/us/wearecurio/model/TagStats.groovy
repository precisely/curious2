package us.wearecurio.model

import org.apache.commons.logging.LogFactory

import java.math.MathContext
import java.sql.ResultSet
import java.util.TreeSet
import us.wearecurio.utility.Utils
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EntryParserService
import us.wearecurio.model.Entry
import us.wearecurio.data.RepeatType

class TagStats {

	private static def log = LogFactory.getLog(this)

	Long userId
	Long tagId
	String description
	Date mostRecentUsage
	Date thirdMostRecentUsage
	Long countLastThreeMonths
	Long countLastYear
	Long countAllTime
	BigDecimal lastAmount
	Integer lastAmountPrecision
	Boolean typicallyNoAmount
	String lastUnits
	
	static constraints = {
		mostRecentUsage(nullable:true)
		thirdMostRecentUsage(nullable:true)
		lastAmount(nullable:true)
		lastAmountPrecision(nullable:true)
		lastUnits(nullable:true)
		typicallyNoAmount(nullable:true)
	}
	static mapping = {
		version false
		table 'tag_stats'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
		mostRecentUsage column:'most_recent_usage', index:'most_recent_usage_index'
		thirdMostRecentUsage column:'third_most_recent_usage', index:'third_most_recent_usage_index'
		countLastThreeMonths column:'count_last_three_months', index:'count_last_three_months_index'
		countLastYear column:'count_last_year', index:'count_last_year_index'
		countAllTime column:'count_all_time', index:'count_all_time_index'
	}

	TagStats() {
	}
	
	boolean isEmpty() {
		return countAllTime == 0
	}
	
	static final MathContext mc = new MathContext(9)
	
	static def createOrUpdate(long userId, long tagId) {
		log.debug "TagStats.createOrUpdate() userId:" + userId + ", tag:" + Tag.get(tagId)
		
		def stats = TagStats.findByTagIdAndUserId(tagId, userId)
		
		if (!stats) {
			stats = new TagStats(tagId:tagId, userId:userId)
		}
		
		def mostRecent = Entry.executeQuery("select entry.date from Entry as entry where entry.baseTag.id = :baseTagId and entry.userId = :userId and entry.date is not null order by entry.date desc", [baseTagId:tagId, userId:userId], [max:1])
					
		stats.setDescription(Tag.get(tagId).getDescription())
		
		if (mostRecent == null || mostRecent[0] == null) {
			log.debug "Creating empty TagStats"
			stats.setMostRecentUsage(null)
			stats.setThirdMostRecentUsage(null)
			stats.setCountLastThreeMonths(0L)
			stats.setCountLastYear(0L)
			stats.setCountAllTime(0L)
			stats.setLastAmount(null)
			stats.setLastAmountPrecision(-1)
			stats.setLastUnits("")
			stats.setTypicallyNoAmount(true)
		} else {
			stats.setLastUnits(null) // initialize last units to null
			stats.setMostRecentUsage(mostRecent[0])

			DatabaseService.get().eachRow("select min(d) as thirdlast from (select e2.date as d from entry e2 where e2.base_tag_id = "
					+ tagId + " and e2.user_id = " + userId + " order by e2.date desc limit 3) as dates") {
				row ->
				stats.setThirdMostRecentUsage(row.thirdlast)
			}
			
			def countLastThreeMonths = Entry.executeQuery(
				"select count(entry.id) from Entry as entry where entry.baseTag.id = :baseTagId and entry.userId = :userId and entry.date is not null and datediff(now(), entry.date) <= 90",
				[baseTagId:tagId, userId:userId])
	
			stats.setCountLastThreeMonths(countLastThreeMonths[0])
			
			def countThreeToNineMonths = Entry.executeQuery(
				"select count(entry.id) from Entry as entry where entry.baseTag.id = :baseTagId and entry.userId = :userId and entry.date is not null and datediff(now(), entry.date) > 90 and datediff(now(), entry.date) < 365",
				[baseTagId:tagId, userId:userId])
	
			stats.setCountLastYear(countThreeToNineMonths[0] + stats.getCountLastThreeMonths())
			
			def countPriorToThisYear = Entry.executeQuery(
				"select count(entry.id) from Entry as entry where entry.baseTag.id = :baseTagId and entry.userId = :userId and entry.date is not null and datediff(now(), entry.date) >= 365",
				[baseTagId:tagId, userId:userId], [max:1])
	
			stats.setCountAllTime(countPriorToThisYear[0] + stats.getCountLastYear())
			
			if (stats.getCountAllTime() < 3) {
				stats.setThirdMostRecentUsage(null)
			}
			
			int amountLimit = 5
			
			def lastAmounts = Entry.executeQuery(
				"select entry.amount, entry.amountPrecision, entry.units, entry.repeatTypeId from Entry as entry where entry.baseTag.id = :baseTagId and entry.userId = :userId and entry.date IS NOT NULL and (entry.amount IS NOT NULL OR entry.repeatTypeId IS NOT NULL) order by entry.date desc",
				[baseTagId:tagId, userId:userId], [max:amountLimit])
			
			int totalCount = lastAmounts.size()
			def firstAmount = null
			def firstPrecision = null
			def firstUnits = null
			int firstCount = 0
			def secondAmount = null
			def secondPrecision = null
			def secondUnits = null
			int secondCount = 0
			int countUsesAmount = 0
			
			for (def amount in lastAmounts) {
				amount[3] = RepeatType.get(amount[3])
				if (amount[1] > 0) {
					++countUsesAmount
				}
				if (firstAmount == null) {
					firstAmount = amount[0]
					firstPrecision = amount[1]
					firstUnits = amount[2]
					firstCount = 1
					if (amount[3]?.isGhost()) {
						firstAmount = null
						firstPrecision = -1
					} else if (firstPrecision < 0)
						break
				} else {
					if (amount[0].equals(firstAmount))
						++firstCount
					else if (secondAmount == null) {
						secondAmount = amount[0]
						secondPrecision = amount[1]
						secondUnits = amount[2]
						
						if (amount[3]?.isGhost()) {
							secondAmount = null
							secondPrecision = -1
						}
						
						secondCount = 1
					} else if (amount[0].equals(secondAmount))
						++secondCount;
				}
			}
			
			if ((totalCount > 2 && countUsesAmount > 1) || (totalCount < 3 && countUsesAmount > 0))
				stats.setTypicallyNoAmount(false)
			else
				stats.setTypicallyNoAmount(true)

			if ((firstCount > 1 && ((float)firstCount) / ((float)totalCount) > 0.5)
					|| ((float)secondCount) / ((float)totalCount) > 0.5) {
				if (firstUnits != null && firstUnits.length() > 0) {
					stats.setLastAmount(firstAmount)
					stats.setLastAmountPrecision(firstPrecision)
					stats.setLastUnits(firstUnits)
				} else {
					stats.setLastAmount(null)
					stats.setLastAmountPrecision(-1)
				}
			} else {
				// if an amount has been set, but it's not frequent enough, then set a null "none" amount
				if (firstPrecision > 0) {
					stats.setLastAmount(null)
					stats.setLastAmountPrecision(-1)
				} else {
					stats.setLastAmount(firstAmount == null ? null : new BigDecimal("1", mc))
					stats.setLastAmountPrecision(-1)
				}
			}
			
			if (stats.getLastUnits() == null) {	
				def lastUnits = Entry.executeQuery(
					"select entry.units from Entry as entry where entry.baseTag.id = :baseTagId and entry.userId = :userId and entry.date IS NOT NULL and entry.units IS NOT NULL and length(entry.units) > 0 order by entry.date desc",
					[baseTagId:tagId, userId:userId], [max:1])
	
				if (lastUnits && lastUnits.size() > 0) {
					stats.setLastUnits(lastUnits[0])
				} else {
					stats.setLastUnits("")
				}
			}
		}
		
		Utils.save(stats, true)
		
		return stats
	}
	
	/**
	 * Updates all TagStats for given user
	 * @param user
	 * @return
	 */
	static def updateTagStats(User user) {
		log.debug "TagStats.updateTagStats() userId:" + user.getId()
		
		def userId = user.getId()

		def tags = Entry.getBaseTags(user, Entry.ONLYIDS)
		
		for (tagId in tags) {
			Model.withTransaction {
				createOrUpdate(userId, tagId)
			}
		}
	}
	
	static sharedTagEntries = [
		/*["sleep 8 hours", 11],
		["walk 3.5 miles 11000 steps", 11],
		["mood 5", 11],
		["exercise 2.5 hours", 11],
		["floss", 10],
		["heart rate 70 bpm", 10],
		["hike 5 miles 750 feet elevation 16500 steps", 10],
		["bike ride 12 miles", 10],
		["run 8 miles 12000 steps", 10],
		["weight 150 lbs", 10],
		["headache 5", 10],
		["migraine 5", 10],
		["migraine aura", 10],
		["congestion", 10],
		["energy 5", 10],
		["blood pressure 120/80", 9],
		["cough", 9],
		["anxiety 5", 9],
		["depression 5", 9],
		["sore throat", 9],
		["tremor", 9],
		["dairy", 9],
		["joint pain 5", 9],
		["hot flash", 8],
		["muscle pain 5", 8],
		["pain 5", 8],
		["skin flushing", 8],
		["activity 10000 steps", 8],
		["perspiring", 8],
		["dream", 5],
		["stomach ache 5", 5],
		["co2 700 ppm", 5],
		["dream intensity 5", 5],
		["sugar", 5],
		["caffeine", 5],
		["coffee", 5],
		["tea", 5],
		["diarrhea", 4],
		["alcohol", 4],
		["yoga", 4],
		["burpees", 4],
		["tennis", 4],
		["basketball", 4],
		["football", 4],
		["jog 8 miles 12000 steps", 4],
		["itching", 4],
		["treadmill", 4],
		["leg pain 5", 3],
		["arm pain 5", 3],
		["dull pain 5", 3],
		["lower back pain 5", 3],
		["upper back pain 5", 3],
		["neck pain 5", 3],
		["shoulder pain 5", 3],
		["forearm pain 5", 3],
		["upper arm pain 5", 3],
		["thigh pain 5", 3],
		["calf pain 5", 3],
		["fever 101 degrees", 3],
		["foot pain 5", 3],
		["right foot pain 5", 3],
		["left foot pain 5", 3],
		["right forearm pain 5", 3],
		["left forearm pain 5", 3],
		["numbness 5", 3],
		["wrist numbness 5", 3],
		["hand numbness 5", 3],
		["finger tingling 5", 3],
		["hand tingling 5", 3],
		["hand numbness 5", 3],
		["sleep quality 5", 3],
		["stress 5", 3],
		["fatigue 5", 3],
		["period", 3],
		["meditation 30 mins", 3],
		["menstruation", 3],
		["runny nose", 3],
		["sneeze", 3],
		["stomach pain 5", 3],
		["abdominal pain 5", 3],
		["temperature 74 degrees", 3],
		["barometric pressure 1000 millibars", 3],
		["rain", 3],
		["sunny", 3],
		["snow", 3],
		["hail", 3],
		["shortness of breath 5", 3],
		["sweat", 3],
		["water 1 glass", 3],
		["tingling fingers 5", 2],
		["tingling toes 5", 2],
		["heart rate variability 20", 2],
		["antibiotics", 2],
		["multivitamin 500mg", 2],
		["nausea 5", 2],
		["exercise 1 hour", 2],
		["heavy exercise 1.2 hours", 2],
		["light exercise 50 minutes", 2],
		["seizure", 2],
		["chiropractic", 2],
		["light sensitivity 5", 2],
		["bath", 2],
		["shower", 2],
		["ibuprofin 200mg", 2],
		["chocolate", 2],
		["red wine 1 glass", 2],
		["white wine 1 glass", 2],
		["wine 1 glass", 2],
		["acupuncture", 2],
		["martial arts 1.5 hours", 2],
		["tabata sprint", 2],
		["chills", 2],
		["chai 1 cup", 2],
		["craving sweets", 2],
		["fruit", 2],
		["vegetables", 2],
		["meat", 2],
		["chicken", 2],
		["fish", 2],
		["gluten", 2],
		["sex", 2],
		["citrus", 2],
		["alertness 5", 2],
		["stretch", 2],
		["prenatal vitamins", 2],
		["mold count 30", 2],
		["magnesium 100mg", 2],
		["hives", 2],
		["snore level 5", 2],
		["probiotics", 2],
		["insomnia", 2],
		["studying 2 hours", 2],
		["yogurt", 2],
		["swim 1 hour", 2],
		["stuffy nose", 2],*/
	]
	
	static Date defaultEntryDate = new Date(1277942400L) // July 1, 2010 GMT
	
	static Map<Long, TagStats> initialTagStats = new HashMap<Long, TagStats>()
	static ArrayList<TagStats> initialTagStatsList = new ArrayList<TagStats>()
	
	/*		
	Long userId
	Long tagId
	String description
	Date mostRecentUsage
	Date thirdMostRecentUsage
	Long countLastThreeMonths
	Long countLastYear
	Long countAllTime
	BigDecimal lastAmount
	Integer lastAmountPrecision
	Boolean typicallyNoAmount
	String lastUnits
	*/
	
	static initializeSharedTags() {
		EntryParserService parser = EntryParserService.get()
		
		for (def entryDef in sharedTagEntries) {
			String entryStr = entryDef[0]
			def parsed = parser.parse(defaultEntryDate, "America/New_York", entryStr, null, null, defaultEntryDate, true)

			TagStats stats = new TagStats()
			
			stats.tagId = parsed.baseTag.id
			stats.description = parsed.baseTag.description
			stats.mostRecentUsage = defaultEntryDate
			stats.thirdMostRecentUsage = defaultEntryDate
			stats.countLastThreeMonths = 0
			stats.countLastYear = 0
			stats.countAllTime = entryDef[1]
			stats.lastAmount = parsed.amount == null ? null : new BigDecimal(parsed.amount)
			stats.lastAmountPrecision = parsed.amounts[0].precision
			stats.typicallyNoAmount = parsed.amount == null
			stats.lastUnits = parsed.amounts[0].units ?: ""
			
			initialTagStats[stats.tagId] = stats
			initialTagStatsList.add(stats)
		}
	}
	
	/**
	 * Returns all TagStats for given user, minus currently repeating events
	 * @param user
	 * @return
	 */
	static def activeForUser(User user, Date now) {
		log.debug "TagStats.activeForUser() userId:" + user.getId()
		
		def cFreq = [ compare: {
				TagStats a, TagStats b ->
				if (a.getCountLastThreeMonths() > b.getCountLastThreeMonths())
					return -1
				if (a.getCountLastThreeMonths() < b.getCountLastThreeMonths())
					return 1
				if (a.getCountLastYear() > b.getCountLastYear())
					return -1
				if (a.getCountLastYear() < b.getCountLastYear())
					return 1
				if (a.getCountAllTime() > b.getCountAllTime())
					return -1
				if (a.getCountAllTime() < b.getCountAllTime())
					return 1
				return -a.getDescription().compareTo(b.getDescription())
			}
		] as Comparator
		
		def cAlg = [ compare: {
				TagStats a, TagStats b ->
				long aMostRecent = a.getMostRecentUsage()?.getTime() ?: -1
				long bMostRecent = b.getMostRecentUsage()?.getTime() ?: -1
				long aThirdMostRecent = a.getThirdMostRecentUsage()?.getTime() ?: -1
				long bThirdMostRecent = b.getThirdMostRecentUsage()?.getTime() ?: -1
				
				if (aThirdMostRecent > bMostRecent)
					return -1
				if (bThirdMostRecent > aMostRecent)
					return 1
				if (a.getCountLastThreeMonths() > b.getCountLastThreeMonths())
					return -1
				if (a.getCountLastThreeMonths() < b.getCountLastThreeMonths())
					return 1
				if (a.getCountLastYear() > b.getCountLastYear())
					return -1
				if (a.getCountLastYear() < b.getCountLastYear())
					return 1
				if (a.getCountAllTime() > b.getCountAllTime())
					return -1
				if (a.getCountAllTime() < b.getCountAllTime())
					return 1
				return -a.getDescription().compareTo(b.getDescription())
			}
		] as Comparator
	
		def freqTagStats = new TreeSet( cFreq )
		def algTagStats = new TreeSet( cAlg )
		
		Long userId = user.getId()
		Date monthAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 60)

		/* def c = Entry.createCriteria()

		def repeatingEntries = c {
			and {
				not { isNull("repeat") }
				eq("userId", userId)
				gt("date", monthAgo)
			}
		}

		TreeSet repeatingTagIds = new TreeSet()
		
		for (entry in repeatingEntries) {
			repeatingTagIds.add(entry.getTag().getId())
		} */

		def c2 = TagStats.createCriteria()

		def allTagStats = c2 {
			and {
				eq("userId", userId)
				not {
					isNull("mostRecentUsage")
				}
			}
		}
		
		def tagStatsMap = [:]
		
		TagStats defaultTagStats
		
		for (TagStats stats in allTagStats) {
			Long tagId = stats.getTagId()
			tagStatsMap[tagId] = stats
			defaultTagStats = initialTagStats[tagId]
			if (defaultTagStats) {
				stats.countAllTime = stats.countAllTime + defaultTagStats.countAllTime
			}
		}

		def tags = Entry.getBaseTags(user, Entry.ONLYIDS)
		
		for (tagId in tags) {
			/* if (!repeatingTagIds.contains(tagId)) { */
			def stats = tagStatsMap[tagId]
			if (stats == null) {
				stats = createOrUpdate(user.getId(), tagId)
				tagStatsMap[tagId] = stats
			}
			freqTagStats.add(stats)
			algTagStats.add(stats)
		}
		
		for (TagStats initialStats in initialTagStatsList) {
			if (tagStatsMap[initialStats.tagId] != null)
				continue
				
			TagStats stats = new TagStats()
			
			stats.userId = userId
			stats.tagId = initialStats.tagId
			stats.description = initialStats.description
			stats.mostRecentUsage = defaultEntryDate
			stats.thirdMostRecentUsage = defaultEntryDate
			stats.countLastThreeMonths = 0
			stats.countLastYear = 0
			stats.countAllTime = initialStats.countAllTime
			stats.lastAmount = initialStats.lastAmount
			stats.lastAmountPrecision = initialStats.lastAmountPrecision
			stats.typicallyNoAmount = initialStats.typicallyNoAmount
			stats.lastUnits = initialStats.lastUnits
			
			freqTagStats.add(stats)
			algTagStats.add(stats)
		}
		
		return [freq:freqTagStats, alg:algTagStats]
	}

	String toString() {
		return "TagStats(userId:" + userId + ", tagId:" + tagId + ", description:" \
				+ description + ", mostRecentUsage:" + Utils.dateToGMTString(mostRecentUsage) \
				+ ", thirdMostRecentUsage:" + Utils.dateToGMTString(thirdMostRecentUsage) + ", countLastThreeMonths:" + countLastThreeMonths \
				+ ", countLastYear:" + countLastYear + ", countAllTime:" + countAllTime + ", lastAmount:" + lastAmount \
				+ ", lastAmountPrecision:" + lastAmountPrecision + ", lastUnits:" + lastUnits + ", typicallyNoAmount:" + typicallyNoAmount + ")"
	}
	
	def getJSONDesc() {
		return [description,lastAmount,lastAmountPrecision,lastUnits,typicallyNoAmount]
	}
	
	def getJSONShortDesc() {
		return [description,lastAmount,lastAmountPrecision,lastUnits,typicallyNoAmount]
	}
}

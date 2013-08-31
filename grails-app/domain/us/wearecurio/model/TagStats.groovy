package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import java.math.MathContext;
import java.sql.ResultSet
import java.util.TreeSet
import us.wearecurio.utility.Utils
import us.wearecurio.services.DatabaseService

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
	String lastUnits
	
	static constraints = {
		mostRecentUsage(nullable:true)
		thirdMostRecentUsage(nullable:true)
		lastAmount(nullable:true)
		lastAmountPrecision(nullable:true)
		lastUnits(nullable:true)
	}
	static mapping = {
		table 'tag_stats'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
		mostRecentUsage column:'most_recent_usage', index:'most_recent_usage_index'
		thirdMostRecentUsage column:'third_most_recent_usage', index:'third_most_recent_usage_index'
		countLastThreeMonths column:'count_last_three_months', index:'count_last_three_months_index'
		countLastYear column:'count_last_year', index:'count_last_year_index'
		countAllTime column:'count_all_time', index:'count_all_time_index'
	}

	public TagStats() {
	}
	
	static final MathContext mc = new MathContext(9)
	
	static public def createOrUpdate(long userId, long tagId) {
		log.debug "TagStats.createOrUpdate() userId:" + userId + ", tag:" + Tag.get(tagId)
		
		def stats = TagStats.findByTagIdAndUserId(tagId, userId)
		
		if (!stats) {
			stats = new TagStats(tagId:tagId, userId:userId)
		}
		
		def mostRecent = Entry.executeQuery("select entry.date from Entry as entry where entry.tag.id = ? and entry.userId = ? and entry.date is not null order by entry.date desc", [tagId, userId], [max:1])
					
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
		} else {
			stats.setLastUnits(null) // initialize last units to null
			stats.setMostRecentUsage(mostRecent[0])
		
			DatabaseService.get().eachRow("select min(d) as thirdlast from (select e2.date as d from entry e2 where e2.tag_id = "
					+ tagId + " and e2.user_id = " + userId + " order by e2.date desc limit 3) as dates") { row ->
				stats.setThirdMostRecentUsage(row.thirdlast)
			}
			
			def countLastThreeMonths = Entry.executeQuery(
				"select count(entry.id) from Entry as entry where entry.tag.id = ? and entry.userId = ? and entry.date is not null and datediff(now(), entry.date) <= 90",
				[tagId, userId])
	
			stats.setCountLastThreeMonths(countLastThreeMonths[0])
			
			def countThreeToNineMonths = Entry.executeQuery(
				"select count(entry.id) from Entry as entry where entry.tag.id = ? and entry.userId = ? and entry.date is not null and datediff(now(), entry.date) > 90 and datediff(now(), entry.date) < 365",
				[tagId, userId])
	
			stats.setCountLastYear(countThreeToNineMonths[0] + stats.getCountLastThreeMonths())
			
			def countPriorToThisYear = Entry.executeQuery(
				"select count(entry.id) from Entry as entry where entry.tag.id = ? and entry.userId = ? and entry.date is not null and datediff(now(), entry.date) >= 365",
				[tagId, userId], [max:1])
	
			stats.setCountAllTime(countPriorToThisYear[0] + stats.getCountLastYear())
			
			if (stats.getCountAllTime() < 3) {
				stats.setThirdMostRecentUsage(null)
			}
			
			int amountLimit = 5
			
			def lastAmounts = Entry.executeQuery(
				"select entry.amount, entry.amountPrecision, entry.units, entry.repeatType from Entry as entry where entry.tag.id = ? and entry.userId = ? and entry.date IS NOT NULL and (entry.amount IS NOT NULL OR entry.repeatType IS NOT NULL) order by entry.date desc",
				[tagId, userId], [max:amountLimit])
			
			int totalCount = lastAmounts.size()
			def firstAmount = null
			def firstPrecision = null
			def firstUnits = null
			int firstCount = 0
			def secondAmount = null
			def secondPrecision = null
			def secondUnits = null
			int secondCount = 0
			
			for (def amount in lastAmounts) {
				if (firstAmount == null) {
					firstAmount = amount[0]
					firstPrecision = amount[1]
					firstUnits = amount[2]
					firstCount = 1
					if (amount[3]?.isReminder()) {
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
						
						if (amount[3]?.isReminder()) {
							secondAmount = null
							secondPrecision = -1
						}
						
						secondCount = 1
					} else if (amount[0].equals(secondAmount))
						++secondCount;
				}
			}

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
					"select entry.units from Entry as entry where entry.tag.id = ? and entry.userId = ? and entry.date IS NOT NULL and entry.units IS NOT NULL and length(entry.units) > 0 order by entry.date desc",
					[tagId, userId], [max:1])
	
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
	static public def updateTagStats(User user) {
		log.debug "TagStats.updateTagStats() userId:" + user.getId()
		
		def userId = user.getId()

		def tagStats = []
		
		def tags = Entry.getTags(user, Entry.ONLYIDS)
		
		for (tagId in tags) {
			tagStats.add(createOrUpdate(userId, tagId))
		}
	}
	
	/**
	 * Returns all TagStats for given user, minus currently repeating events
	 * @param user
	 * @return
	 */
	static public def activeForUser(User user) {
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
		
		def userId = user.getId()
		def monthAgo = new Date(new Date().getTime() - 1000L * 60 * 60 * 24 * 60)

		/* def c = Entry.createCriteria()

		def repeatingEntries = c {
			and {
				not { isNull("repeatType") }
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
			}
		}
		
		def tagStatsMap = [:]
		
		for (stats in allTagStats) {
			tagStatsMap[stats.getTagId()] = stats
		}

		def tags = Entry.getTags(user, Entry.ONLYIDS)
		
		for (tagId in tags) {
			/* if (!repeatingTagIds.contains(tagId)) { */
			def stats = tagStatsMap[tagId]
			if (stats == null) stats = createOrUpdate(user.getId(), tagId)
			freqTagStats.add(stats)
			algTagStats.add(stats)
		}
		
		return [freq:freqTagStats, alg:algTagStats]
	}

	public String toString() {
		return "TagStats(userId:" + userId + ", tagId:" + tagId + ", description:" \
				+ description + ", mostRecentUsage:" + Utils.dateToGMTString(mostRecentUsage) \
				+ ", thirdMostRecentUsage:" + Utils.dateToGMTString(thirdMostRecentUsage) + ", countLastThreeMonths:" + countLastThreeMonths \
				+ ", countLastYear:" + countLastYear + ", countAllTime:" + countAllTime + ", lastAmount:" + lastAmount \
				+ ", lastAmountPrecision:" + lastAmountPrecision + ", lastUnits:" + lastUnits + ")"
	}
	
	public def getJSONDesc() {
		return [description,lastAmount,lastAmountPrecision,lastUnits]
	}
	
	public def getJSONShortDesc() {
		return [description,lastAmount,lastAmountPrecision,lastUnits]
	}
}

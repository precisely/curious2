package us.wearecurio.data

import java.util.Date

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalTime

import us.wearecurio.cache.BoundedCache
import us.wearecurio.data.UnitGroupMap.UnitGroup
import us.wearecurio.data.UnitRatio
import us.wearecurio.datetime.LocalTimeRepeater
import us.wearecurio.model.User;

class DataRetriever {
	static DatabaseServiceInterface databaseService

	static setDatabaseService(DatabaseServiceInterface _databaseService) {
		databaseService = _databaseService
	}
	
	static DataRetriever retriever = new DataRetriever()
	
	static DataRetriever get() { return retriever }

	static final double twoPi = 2.0d * Math.PI

	protected static final long HOURTICKS = 60L * 60000
	protected static final long DAYTICKS = HOURTICKS * 24
	protected static final long HALFDAYTICKS = HOURTICKS * 12
	protected static final long WEEKTICKS = DAYTICKS * 7
	
	DataRetriever() {
	}

	// limit repeat generation to 300 entries, for now
	static final int MAX_PLOT_REPEAT = 300

	protected static final int SHORT_DESC_DATE = 0
	protected static final int SHORT_DESC_AMOUNT = 1
	protected static final int SHORT_DESC_DESCRIPTION = 2
	
	def generateRepeaterEntries(def repeaters, long endTimestamp, def results) {
		def repeaterOrder = { LocalTimeRepeater a, LocalTimeRepeater b ->
			if (a.getCurrentDateTime() == null) return 1 // sort null repeaters to end
			if (b.getCurrentDateTime() == null) return -1 // sort null repeaters to end
			if (!a.isActive()) return 1 // sort inactive repeaters to end
			if (!b.isActive()) return -1 // sort inactive repeaters to end
			return a.getCurrentDateTime().getMillis() <=> b.getCurrentDateTime().getMillis()
		}
		if (repeaters.size() == 0) return

		// sort repeaters by next date time
		repeaters.sort(repeaterOrder)

		// if current entry time is later than or equal to next repeater timestamp, iterate through repeaters adding new repeat entries
		LocalTimeRepeater repeater = repeaters[0]
		Date nextDate = null
		if (repeaters.size() > 1) {
			nextDate = repeaters[1].getDate()
		}
		int c = 0
		while (repeater.isActive() && repeater.getDate().getTime() <= endTimestamp) {
			def desc = repeater.getPayload().collect()
			desc[SHORT_DESC_DATE] = repeater.getDate()
			repeater.incrementDate()
			results.add(desc)
			if (++c > MAX_PLOT_REPEAT)
				break
			if (nextDate != null && (repeater.getDate() > nextDate || (!repeater.isActive()))) {
				repeaters.sort(repeaterOrder)
				repeater = repeaters[0]
				if (repeaters.size() > 1) {
					nextDate = repeaters[1].getDate()
				}
			}
		}
	}

	def interpolateTagIds(queryStr, queryMap) {
		def interpolated = queryStr.replace(/:tagIds/, queryMap.tagIds.join(','))
		if (queryMap['tagIds']) {
			queryMap.remove('tagIds')
		}
		[interpolated, queryMap]
	}
	
	static String alertText(Map result) {
		StringBuffer text = new StringBuffer((String)result['description'])
		BigDecimal amount = result['amount']
		String units = result['units']
		String comment = result['comment']
		/*def amountPrecision = result['amount_precision']
		if (amountPrecision >= 0) {
			text.append(" ")
			text.append(amount.toString())
			if (units) {
				text.append(" ")
				text.append(units)
			}
		}*/
		if (comment) {
			text.append(" ")
			text.append(comment)
		}
		
		return text.toString()
	}

	def fetchRemindData(Long userId, Date startDate, Date endDate, Date currentTime) {
		if (endDate == null) endDate = currentTime // don't query past now if endDate isn't specified
		if (startDate == null) startDate = new Date(-30610137600000) // set start date to 1000 AD if startDate isn't specified

		// get timed daily repeats (not ghosted)

		String queryStr = "select entry.id, t.description, entry.comment, entry.repeat_type_id, entry.repeat_end, entry.date as entry_date " \
				+ "from entry inner join tag t on entry.tag_id = t.id where entry.user_id = :userId and entry.amount is not null and (entry.repeat_end is null or entry.repeat_end >= :startDate) and entry.date < :endDate and (entry.repeat_type_id & :ghostBit = 0) and (entry.repeat_type_id & :repeatBit <> 0) " \
				+ "and (entry.repeat_type_id & :remindBit <> 0) order by entry.date asc"

		def queryMap = [userId:userId, startDate:startDate, endDate:endDate, ghostBit:RepeatType.GHOST_BIT, repeatBit:RepeatType.REPEAT_BITS, remindBit:RepeatType.REMIND_BIT]
		def rawResults = databaseService.sqlRows(queryStr, queryMap)

		def timedResults = []

		// make this repeater list ordered by current timestamp
		def repeaters = []

		Long nextRepeaterTimestamp = null

		for (result in rawResults) {
			Long entryId = result['id']
			Long repeatTypeBits = result['repeat_type_id']
			
			if (!(repeatTypeBits == null || repeatTypeBits == 0L)) {
				Date repeatEnd = result['repeat_end']
				if (repeatEnd == null)
					repeatEnd = endDate
				else if (repeatEnd.getTime() > endDate.getTime())
					repeatEnd = endDate
				Date entryDate = result['entry_date']
				
				LocalTimeRepeater repeater = new LocalTimeRepeater([entryDate, entryId, alertText(result)], new DateTime(entryDate), startDate, repeatEnd, (int)(repeatTypeBits & RepeatType.INTERVAL_BITS))
				repeaters.add(repeater)
			}
		}
		// get regular results
		queryStr = "select e.id, e.date, e.tag_id, t.description, e.comment " \
				+ "from entry e, tag t where e.user_id = :userId and e.tag_id = t.id " \
				+ "and e.amount is not null and e.date >= :startDate and e.date < :endDate and ((e.repeat_type_id & :concreteRepeatBits)= 0) " \
				+ "and (e.repeat_type_id & :remindBit <> 0) order by e.date asc"


		queryMap = [userId:userId, startDate:startDate, endDate:endDate, concreteRepeatBits:(RepeatType.REPEAT_BITS | RepeatType.CONCRETEGHOST_BIT), remindBit:RepeatType.REMIND_BIT ]

		rawResults = databaseService.sqlRows(queryStr, queryMap)

		def results = []

		for (result in rawResults) {
			Date date = result['date']
			def entryJSON = [
				new Date(date.getTime()),
				result['id'].longValue(),
				alertText(result)
			]
			long entryTimestamp = date.getTime()
			generateRepeaterEntries(repeaters, entryTimestamp, results)
			results.add(entryJSON)
		}
		// keep generating remaining repeaters until endDate
		generateRepeaterEntries(repeaters, endDate.getTime(), results)

		return results
	}

	def fetchPlotData(Long userId, List tagIds, Date startDate, Date endDate, Date currentTime, Map plotInfo = null) {
		if (!tagIds.size()) {
			return []
		}

		if (endDate == null) endDate = currentTime + 1 // don't query past now if endDate isn't specified
		if (startDate == null) startDate = new Date(-30610137600000) // set start date to 1000 AD if startDate isn't specified

		// get timed daily repeats (not ghosted)

		String queryStr = "select entry.id, t.description, entry.amount, entry.repeat_type_id, entry.repeat_end, entry.date as entry_date " \
				+ "from entry inner join tag t on entry.tag_id = t.id where entry.user_id = :userId and entry.amount is not null and (entry.repeat_end is null or entry.repeat_end >= :startDate) and entry.date < :endDate and (entry.repeat_type_id & :ghostBit = 0) and (entry.repeat_type_id & :repeatBit <> 0) " \
				+ "and entry.tag_id in (:tagIds) order by entry.date asc"

		def queryMap = [userId:userId, startDate:startDate, endDate:endDate, ghostBit:RepeatType.GHOST_BIT, repeatBit:RepeatType.REPEAT_BITS, tagIds:tagIds]
		(queryStr, queryMap) = interpolateTagIds(queryStr, queryMap)
		def rawResults = databaseService.sqlRows(queryStr, queryMap)

		def timedResults = []

		// make this repeater list ordered by current timestamp
		def repeaters = []

		Long nextRepeaterTimestamp = null

		for (result in rawResults) {
			Long entryId = result['id']
			Long repeatTypeBits = result['repeat_type_id']
			
			if (!(repeatTypeBits == null || repeatTypeBits == 0L)) {
				Date repeatEnd = result['repeat_end']
				if (repeatEnd == null)
					repeatEnd = endDate
				else if (repeatEnd.getTime() > endDate.getTime())
					repeatEnd = endDate
				Date entryDate = result['entry_date']
				LocalTimeRepeater repeater = new LocalTimeRepeater([entryDate, result['amount'], result['description']], new DateTime(entryDate), startDate, repeatEnd, (int)(repeatTypeBits & RepeatType.INTERVAL_BITS))
				repeaters.add(repeater)
			}
		}
		// get regular results
		DecoratedUnitRatio mostUsedUnitRatioForTags = UnitGroupMap.theMap.mostUsedUnitRatioForTagIds(userId, tagIds)

		queryStr = "select e.date, e.amount, e.tag_id, t.description, e.units " \
				+ "from entry e, tag t where e.user_id = :userId and e.tag_id = t.id " \
				+ "and e.amount is not null and e.date >= :startDate and e.date < :endDate and (e.repeat_type_id is null or (e.repeat_type_id & :repeatAndContinuousBits = 0)) " \
				+ "and e.tag_id in (:tagIds) order by e.date asc"


		queryMap = [userId:userId, startDate:startDate, endDate:endDate, repeatAndContinuousBits:RepeatType.REPEAT_AND_CONTINUOUS_BITS, /* concreteRepeatBits:(RepeatType.REPEAT_BITS | RepeatType.CONCRETEGHOST_BIT), */ tagIds:tagIds ]

		(queryStr, queryMap) = interpolateTagIds(queryStr, queryMap)
		rawResults = databaseService.sqlRows(queryStr, queryMap)

		def results = []

		double mostUsedUnitRatio = mostUsedUnitRatioForTags ? mostUsedUnitRatioForTags.ratio : 1.0d

		for (result in rawResults) {
			Date date = result['date']
			BigDecimal amount = result['amount']
			def entryJSON = [
				date,
				amount,
				result['description'],
				result['tag_id']
			]
			DecoratedUnitRatio unitRatio = mostUsedUnitRatioForTags?.lookupDecoratedUnitRatio(result['units'])
			if (unitRatio) {
				entryJSON[1] = (amount * unitRatio.ratio) / mostUsedUnitRatio
			}
			long entryTimestamp = date.getTime()
			generateRepeaterEntries(repeaters, entryTimestamp, results)
			results.add(entryJSON)
		}
		// keep generating remaining repeaters until endDate
		generateRepeaterEntries(repeaters, endDate.getTime(), results)

		if (plotInfo != null) {
			plotInfo['unitRatio'] = mostUsedUnitRatioForTags?.unitRatio
			plotInfo['unitGroupId'] = mostUsedUnitRatioForTags == null ? -1 : mostUsedUnitRatioForTags.unitGroup.id
			plotInfo['valueScale'] = mostUsedUnitRatio
		}

		return results
	}

	def fetchPlotData(Long userId, List tagIds, Date startDate, Date endDate, Date currentTime, String timeZoneName, Map plotInfo = null) {
		return fetchPlotData(userId, tagIds, startDate, endDate, currentTime, plotInfo)
	}

	def fetchAverageTime(Long userId, def tagIds, Date startDate, Date endDate, Date currentTime, DateTimeZone currentTimeZone) {
		DateTime startDateTime = new DateTime((Date)(startDate ?: currentTime), currentTimeZone) // calculate midnight relative to current time
		// set time to zero
		Date dateMidnight = startDateTime.toLocalDate().toDateTime(LocalTime.MIDNIGHT, currentTimeZone).toDate()

		// figure out the angle of the startDate
		String queryStr = "select (time_to_sec(:dateMidnight) / 43200.0) * pi() as angle"

		def queryMap = [dateMidnight:dateMidnight]

		double startDateAngle = databaseService.sqlRows(queryStr, queryMap)[0]['angle']

		if (endDate == null) endDate = currentTime // don't query past now if endDate isn't specified
		if (startDate == null) startDate = new Date(-30610137600000) // set start date to 1000 AD if startDate isn't specified

		// fetch sum of non-repeat entries
		
		queryStr = "select count(*) as c, sum(sin((time_to_sec(entry.date) / 43200.0) * pi())) as y, sum(cos((time_to_sec(entry.date) / 43200.0) * pi())) as x " \
				+ "from entry entry where entry.user_id = :userId and entry.amount is not null and (entry.repeat_end is null or (entry.repeat_type_id & :ghostBit = 0)) " \
				+ "and entry.tag_id in (:tagIds) and entry.date >= :startDate and entry.date < :endDate"

		queryMap = [userId:userId, startDate:startDate, endDate:endDate, tagIds:tagIds, ghostBit:RepeatType.GHOST_BIT]

		(queryStr, queryMap) = interpolateTagIds(queryStr, queryMap)
		def nonRepeatSum = databaseService.sqlRows(queryStr, queryMap)[0]

		// fetch sum of repeat entries
		queryStr = "select sum(datediff(if(entry.repeat_end is null, :endDate, if(entry.repeat_end < :endDate, entry.repeat_end, :endDate)), if(entry.date > :startDate, entry.date, :startDate))) as c, " \
				+ "sum(sin((time_to_sec(entry.date) / 43200.0) * pi()) * datediff(if(entry.repeat_end is null, :endDate, if(entry.repeat_end < :endDate, entry.repeat_end, :endDate)), if(entry.date > :startDate, entry.date, :startDate))) as y, " \
				+ "sum(cos((time_to_sec(entry.date) / 43200.0) * pi()) * datediff(if(entry.repeat_end is null, :endDate, if(entry.repeat_end < :endDate, entry.repeat_end, :endDate)), if(entry.date > :startDate, entry.date, :startDate))) as x " \
				+ "from entry entry where entry.user_id = :userId and entry.amount is not null and ((entry.repeat_end is not null and entry.repeat_end > :startDate) or entry.repeat_end is null) and entry.date < :endDate and (entry.repeat_type_id & :dailyBit <> 0) and (entry.repeat_type_id & :ghostBit = 0)" \
				+ "and entry.tag_id in (:tagIds)"

		queryMap = [userId:userId, startDate:startDate, endDate:endDate, ghostBit:RepeatType.GHOST_BIT, dailyBit:RepeatType.DAILY_BIT, tagIds:tagIds]

		(queryStr, queryMap) = interpolateTagIds(queryStr, queryMap)
		def repeatSum = databaseService.sqlRows(queryStr, queryMap)[0]

		int c = (nonRepeatSum['c'] ?: 0) + (repeatSum['c'] ?: 0)
		if (c == 0)
			return HALFDAYSECS

		double x = ((nonRepeatSum['x'] ?: 0.0) + (repeatSum['x'] ?: 0.0)) / c
		double y = ((nonRepeatSum['y'] ?: 0.0) + (repeatSum['y'] ?: 0.0)) / c

		double averageAngle

		if (Math.abs(x) < 1E-8d) x = 0.0d
		if (Math.abs(y) < 1E-8d) y = 0.0d

		if ((x == 0.0d) && (y == 0.0d))
			averageAngle = Math.PI
		else {
			averageAngle = Math.atan2(y, x)
			averageAngle -= startDateAngle
			averageAngle = ((averageAngle % twoPi) + twoPi) % twoPi
		}

		return (int) (Math.round((averageAngle / twoPi) * 86400.0d)) % 86400
	}
	
	static final long HALFDAYTICKS = 12 * 60 * 60 * 1000L
	static final int DAYSECS = 24 * 60 * 60
	static final int HALFDAYSECS = 12 * 60 * 60
	static final int HOURSECS = 60 * 60

	def fetchSumPlotData(Long userId, def tagIds, Date startDate, Date endDate, Date currentDate, String timeZoneName, Map plotInfo = null) {
		DateTimeZone currentTimeZone = DateTimeZone.forID(timeZoneName ?: "Etc/UTC")

		if (endDate == null) endDate = new Date() // don't query past now if endDate isn't specified

		int averageSecs = fetchAverageTime(userId, tagIds, startDate, endDate, currentDate, currentTimeZone)

		if (averageSecs > HALFDAYSECS + HOURSECS * 9) { // if the average time is after 9pm, do a "night sum"
			averageSecs -= DAYSECS
		}

		long offset = (averageSecs * 1000L) - HALFDAYTICKS
		if (startDate != null)
			startDate = new Date(startDate.getTime() + offset)
		else if (endDate != null)
			endDate = new Date(endDate.getTime() + offset)

		def rawResults = fetchPlotData(userId, tagIds, startDate, endDate, currentDate, timeZoneName)

		Long startTime = startDate?.getTime()
		Long startDayTime = startTime
		Long endDayTime = startDayTime != null ? startDayTime + DAYTICKS : null

		def currentResult = null
		SortedSet<String> currentDescriptions = new TreeSet<String>()
		def summedResults = []
		
		DecoratedUnitRatio mostUsedUnitRatio = UnitGroupMap.theMap.mostUsedUnitRatioForTagIds(userId, tagIds)

		for (result in rawResults) {
			Date resultDate = result[SHORT_DESC_DATE]
			currentDescriptions.add(result[SHORT_DESC_DESCRIPTION])
			if (startDate == null) { // no start date, start a date at midnight
				DateTime startDateTime = new DateTime(resultDate, currentTimeZone) // set start date to 1000 AD if startDate isn't specified
				// set time to zero
				startDate = startDateTime.toLocalDate().toDateTime(LocalTime.MIDNIGHT, currentTimeZone).toDate()
				startDate = new Date(startDate.getTime() + offset)
				startDayTime = startDate.getTime()
				endDayTime = startDayTime + DAYTICKS
			}
			Long resultTime = resultDate.getTime()
			if (resultTime < startDayTime || resultTime > endDayTime) { // switch to new day
				currentResult = null
				startDayTime = resultTime - ((resultTime - startDayTime) % DAYTICKS) // start of next day
				endDayTime = startDayTime + DAYTICKS
			}
			if (resultTime < endDayTime) {
				if (currentResult == null) {
					currentResult = result
					result[SHORT_DESC_DATE] = new Date(startDayTime + HALFDAYTICKS)
					result[SHORT_DESC_DESCRIPTION] = currentDescriptions.join('+')
					currentDescriptions.clear()
					summedResults.add(currentResult)
				} else {
					currentResult[SHORT_DESC_AMOUNT] = currentResult[SHORT_DESC_AMOUNT] + result[SHORT_DESC_AMOUNT]
				}
			}
		}
		
		double valueScale = mostUsedUnitRatio ? mostUsedUnitRatio.ratio : 1.0d

		if (plotInfo != null) {
			plotInfo['unitRatio'] = mostUsedUnitRatio
			plotInfo['unitGroupId'] = mostUsedUnitRatio == null ? -1 : mostUsedUnitRatio.unitGroup.id
			plotInfo['valueScale'] = valueScale
		}

		return summedResults
	}

	/*
	 * Tag unit data methods
	 */
	
	protected static Map tagUnitStatsCache = Collections.synchronizedMap(new BoundedCache<UserTagId, TagUnitStatsInterface>(10000))
	
	// must registerTestReset in Bootstrap if you are using this class inside Grails
	static resetCache() {
		tagUnitStatsCache.clear()
	}
	
	static addToTagUnitStatsCache(TagUnitStatsInterface tagUnitStats) {
		tagUnitStatsCache.put(new UserTagId(tagUnitStats.userId, tagUnitStats.tagId), tagUnitStats)
	}
	
	static class UserTagId {
		Long userId
		Long tagId
		
		UserTagId(Long userId, Long tagId) {
			this.userId = userId
			this.tagId = tagId
		}
		
		int hashCode() {
			return (int) (this.userId + this.tagId)
		}
		
		boolean equals(Object o) {
			if (o instanceof UserTagId) {
				return (((UserTagId)o).userId == this.userId) && (((UserTagId)o).tagId == this.tagId)
			}
			
			return false
		}
	}
	
	static class TagUnitStatsInfo implements TagUnitStatsInterface {
		Long userId
		Long tagId
		UnitGroup unitGroup
		String unit
		Long timesUsed
		
		TagUnitStatsInfo(Long userId, Long tagId, UnitGroup unitGroup, String unit, Long timesUsed) {
			this.userId = userId
			this.tagId = tagId
			this.unitGroup = unitGroup
			this.unit = unit
			this.timesUsed = timesUsed
		}
		
		Long getUserId() { userId }
		Long getTagId() { tagId }
		UnitGroup getUnitGroup() { unitGroup }
		String getUnit() { unit }
		Long getTimesUsed() { timesUsed }
	}
	
	TagUnitStatsInterface mostUsedTagUnitStats(Long userId, Long tagId) {
		TagUnitStatsInterface stats = tagUnitStatsCache.getAt(new UserTagId(userId, tagId))
		if (stats != null)
			return stats
		
		def results = databaseService.sqlRows("select t.unit_group_id, t.unit, t.times_used from tag_unit_stats t where t.tag_id = :tagId and t.user_id = :userId limit 1",
				[tagId: tagId, userId: userId])
		
		if (!results) {
			return null
		}
		
		def result = results[0]
		
		Long unitGroupId = result['unit_group_id']
		String unit = result['unit']
		Long timesUsed = result['times_used']

		stats = new TagUnitStatsInfo(userId, tagId, UnitGroup.get(unitGroupId), unit, timesUsed)
		
		addToTagUnitStatsCache(stats)
		
		return stats
	}
	
	TagUnitStatsInterface mostUsedTagUnitStatsForTags(Long userId, def tagIds) {
		def r = databaseService.sqlRows("select ts.unit_group_id, sum(ts.times_used) as s from tag_unit_stats ts where ts.tag_id in (:tagIds) and ts.user_id = :userId group by ts.unit_group_id order by s desc limit 1",
				[tagIds: tagIds, userId: userId])
		
		if ((!r) || (!r[0]))
			return null
			
		def unitGroupId = r[0]['unit_group_id']
		
		if (unitGroupId == null)
			return null
		def queryStr = "select ts.unit, sum(ts.times_used) as s from tag_unit_stats ts where ts.tag_id in (:tagIds) and ts.user_id = :userId and ts.unit_group_id = :unitGroupId group by ts.unit order by s desc limit 2"
		def queryMap = [tagIds: tagIds, userId: userId, unitGroupId: unitGroupId]
		(queryStr, queryMap) = interpolateTagIds(queryStr, queryMap)
		r = databaseService.sqlRows(queryStr, queryMap)
		
		if ((!r) || (!r[0]))
			return null
			
		return new TagUnitStatsInfo(userId, null, UnitGroup.get(unitGroupId), r[0]['unit'], r[0]['s'].longValue())
	}
	
}

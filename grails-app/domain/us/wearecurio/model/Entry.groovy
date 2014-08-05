package us.wearecurio.model;

import groovy.time.*
import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.java.EntryJava.RepeatType
import static us.wearecurio.model.java.EntryJava.*
import us.wearecurio.parse.ParseUtils

import us.wearecurio.datetime.LocalTimeRepeater
import us.wearecurio.parse.PatternScanner
import us.wearecurio.services.DatabaseService
import us.wearecurio.utility.Utils
import us.wearecurio.model.Tag
import us.wearecurio.units.UnitGroupMap
import us.wearecurio.units.UnitGroupMap.UnitRatio

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern
import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.*
import org.junit.Before;

class Entry implements Comparable {

	private static def log = LogFactory.getLog(this)

	public static final int MAXCOMMENTLENGTH = 100000
	public static final int MAXUNITSLENGTH = 50
	public static final int MAXSETNAMELENGTH = 50
	public static final long HOURTICKS = 60L * 60000
	public static final long DAYTICKS = HOURTICKS * 24
	public static final long HALFDAYTICKS = HOURTICKS * 12
	public static final long WEEKTICKS = DAYTICKS * 7

	public static final int DEFAULT_DATEPRECISION_SECS = 3 * 60
	public static final int VAGUE_DATE_PRECISION_SECS = 86400
	
	static constraints = {
		group(nullable:true)
		amount(scale:9, nullable:true)
		date(nullable:true)
		datePrecisionSecs(nullable:true)
		amountPrecision(nullable:true)
		repeatType(nullable:true)
		repeatEnd(nullable:true)
		units(maxSize:MAXUNITSLENGTH)
		comment(maxSize:MAXCOMMENTLENGTH)
		baseTag(nullable:true)
		durationType(nullable:true)
		timeZoneId(nullable:true)
		setIdentifier(nullable:true)
	}

	static mapping = {
		version false
		table 'entry'
		group column: 'group_id', index:'group_id_index'
		userId column:'user_id', index:'user_id_index'
		date column:'date', index:'date_index'
		tag column:'tag_id', index:'tag_id_index'
		repeatType column:'repeat_type', index:'repeat_type_index'
		repeatEnd column:'repeat_end', index:'repeat_end_index'
		durationType column:'duration_type', index:'duration_type_index'
		setIdentifier column:'set_identifier', index:'set_identifier_index'
	}


	public static def DAILY_IDS = [
		RepeatType.DAILY.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
		RepeatType.REMINDDAILY.getId(),
		RepeatType.REMINDDAILYGHOST.getId()
	]

	public static def WEEKLY_IDS = [
		RepeatType.WEEKLY.getId(),
		RepeatType.WEEKLYCONCRETEGHOST.getId(),
		RepeatType.REMINDWEEKLY.getId(),
		RepeatType.REMINDWEEKLYGHOST.getId()
	]

	public static def REPEAT_IDS = [
		RepeatType.DAILY.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
		RepeatType.WEEKLY.getId(),
		RepeatType.WEEKLYCONCRETEGHOST.getId()
	]

	public static def REMIND_IDS = [
		RepeatType.REMINDDAILY.getId(),
		RepeatType.REMINDWEEKLY.getId(),
		RepeatType.REMINDWEEKLY.getId(),
		RepeatType.REMINDDAILYGHOST.getId(),
		RepeatType.REMINDWEEKLYGHOST.getId()
	]

	public static def CONTINUOUS_IDS = [
		RepeatType.CONTINUOUS.getId(),
		RepeatType.CONTINUOUSGHOST.getId()
	]

	public static def GHOST_IDS = [
		RepeatType.CONTINUOUSGHOST.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
		RepeatType.WEEKLYCONCRETEGHOST.getId(),
		RepeatType.REMINDDAILYGHOST.getId(),
		RepeatType.REMINDWEEKLYGHOST.getId()
	]

	public static def DAILY_UNGHOSTED_IDS = [
		RepeatType.DAILY.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
	]

	public static def UNGHOSTED_SINGULAR_IDS = [
		RepeatType.REMINDDAILY.getId(),
	]

	Long userId
	EntryGroup group // used to group multidimensional entries together
	Date date
	Integer timeZoneId
	Integer datePrecisionSecs
	Tag tag
	BigDecimal amount
	Integer amountPrecision
	String units
	String comment
	RepeatType repeatType
	Date repeatEnd
	Tag baseTag
	DurationType durationType
	Identifier setIdentifier
	
	public String fetchSuffix() {
		if (baseTag == null || baseTag == tag) {
			return ""
		}
		
		return tag.getDescription().substring(baseTag.length() + 1)
	}

    int compareTo(obj) {
        releaseDate.compareTo(obj.releaseDate)
    }

	static class TagStatsRecord {
		TagStats oldTagStats
		TagStats newTagStats
	}

	static final MathContext mc = new MathContext(9)

	public static final int DEFAULT_AMOUNTPRECISION = 3

	/**
	* Create and save new entry
	*/
	static Entry create(Long userId, Date date, TimeZoneId timeZoneId, String description, BigDecimal amount, String units,
			String comment, String setName, int amountPrecision = DEFAULT_AMOUNTPRECISION) {
		return create(userId,
		[
			'date':date,
			'timeZoneId':(Integer) timeZoneId.getId(),
			'datePrecisionSecs':DEFAULT_DATEPRECISION_SECS,
			'description':description,
			'amount':amount,
			'units':units,
			'comment':comment,
			'setName':setName,
			'amountPrecision': amountPrecision,
		],
		null)
	}

	static Entry updatePartialOrCreate(Long userId, Map m, TagStatsRecord tagStatsRecord) {
		log.debug("Entry.updatePartialOrCreate: Trying to find a partial entry to update")
		def partialEntry = lookForPartialEntry(userId, m)
		log.debug("Entry.updatePartialOrCreate: " + partialEntry.size() +" partial entries found")
		if (partialEntry.size() < 1) {
			return Entry.create(userId, m, tagStatsRecord)
		}

		for (entry in partialEntry) {
			log.debug ("Updating partial entry " + entry)
			entry.amount = m.amount
			Utils.save(entry, true)
			return entry
		}
	}

	static List lookForPartialEntry(userId, Map m) {
		/**
		* Making sure it is a summary entry. Non summary entries will
		* not have partial entries
		*/

		if (!m.description.endsWith(" summary")) {
			return []
		}

		Tag tag = Tag.look(m.description)
		DateTimeZone userTimezone = TimeZoneId.fromId(m.timeZoneId)?.toDateTimeZone()
		DateTime userDateTime = new DateTime(m.date.getTime()).withZone(userTimezone)

		DateTime startOfDay = userDateTime.withTime(0, 0, 0, 0).minusMinutes(1)
		DateTime endOfDay = startOfDay.plusHours(24)
		log.debug("Entry.lookForPartialEntry: m.date " + m.date)
		log.debug("Entry.lookForPartialEntry: userTimezone " + userTimezone)
		log.debug("Entry.lookForPartialEntry: start date " + startOfDay)
		log.debug("Entry.lookForPartialEntry: start " + endOfDay)
		
		Identifier setId = Identifier.look(m.setName)

		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", userId)
				eq("tag", tag)
				eq("comment", m.comment)
				if (setId) {
					eq("setIdentifier", setId)
				}
				eq("timeZoneId", m.timeZoneId)
				between("date", startOfDay.toDate(), endOfDay.toDate())
			}
		}

		return results
	}

	static final BigDecimal smallDifference = new BigDecimal("0.01")

	// Prevent duplicates for imported entries
	static boolean hasDuplicate(Long userId, Map m) {
		if (m.setName == null)
			return false

		log.debug "Trying to find duplicate for " + userId + " for " + m

		Tag tag = Tag.look(m.description)
		
		Identifier setId = Identifier.look(m.setName)

		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", userId)
				eq("date", m.date)
				eq("tag", tag)
				eq("datePrecisionSecs", m.datePrecisionSecs == null ? DEFAULT_DATEPRECISION_SECS : m.datePrecisionSecs)
				eq("amount", m.amount)
				eq("amountPrecision", m.amountPrecision == null ? 3 : m.amountPrecision)
				eq("units", m.units)
				eq("comment", m.comment)
				eq("setIdentifier", setId)
			}
		}

		for (r in results) {
			log.debug ("Found duplicate " + r)
			return true
		}

		log.debug "No duplicate found"

		return false
	}

	WORK STOPPED HERE:
		modify update to handle multidimensional entries
		modify delete to handle multidimensional entries
		migrate existing multidimensinal entries
			find entries matching certain patterns, add baseTag to entry
			set entry group id to unique number for all matching entries
		modify list method to combine entry group into single compound entry
		modify web and mobile app to correctly display multidimensional entry
		create special elevation units (feet elevation)
		use entry creation stats to update statistics after creating entries in a batch
		
	/*
	 * Class to return entry creation statistics for later updating of entry and tag statistics
	 */
	static class EntryStats {
		Long userId
		Set<Long> tagIds = new HashSet<Long>()
		Integer timeZoneId = null
		
		public EntryStats(Long userId) {
			this.userId = userId
		}
	
		public void addTag(Tag tag) {
			tags.add(tag.getId())
		}
		
		public def finalize() { // after finalizing all entry creation, call this to update statistics
			def tagStats = []
			
			for (Long tagId: tags) {
				tagStats.add(TagStats.createOrUpdate(userId, tagId))
			}

			if (timeZoneId != null)
				User.setTimeZoneId(userId, timeZoneId)
				
			return tagStats
		}
	}
	
	static protected Entry createSingle(Long userId, Map m, EntryGroup group, EntryStats stats) {
		log.debug "Entry.createSingle() userId:" + userId + ", m:" + m

		if (hasDuplicate(userId, m)) {
			log.debug "Entry map is a duplicate, do not add: " + m
			return null
		}

		Tag baseTag = m['baseTag']
		DurationType durationType = m['durationType']
		
		if (durationType != null && ((RepeatType)m['repeatType'])?.isGhost())
			durationType = durationType.makeGhost()
		
		Tag tag = m['tag']

		Integer timeZoneId = (Integer) m['timeZoneId'] ?: (Integer)TimeZoneId.look(m['timeZoneName']).getId()

		def tagUnitStats
		if (!m['units']) {
			// Using the most used unit in case the unit is unknown
			tagUnitStats = TagUnitStats.mostUsedTagUnitStats(userId, tag.getId())
			if (tagUnitStats) {
				m['units'] = tagUnitStats.unit
			}
		} else {
			TagUnitStats.createOrUpdate(userId, tag.getId(), m['units'] == null?'':m['units'])
		}
		
		Entry entry = new Entry(
				userId:userId,
				group:group,
				date:m['date'],
				timeZoneId:timeZoneId,
				datePrecisionSecs:m['datePrecisionSecs'] == null ? DEFAULT_DATEPRECISION_SECS : m['datePrecisionSecs'],
				tag:tag,
				amount:m['amount'],
				units:m['units'] ?: '',
				comment:m['comment']==null?'':m['comment'],
				repeatType: m['repeatType'],
				repeatEnd: m['repeatEnd'],
				baseTag:baseTag,
				durationType:durationType,
				setIdentifier:Identifier.look(m['setName']),
				amountPrecision:m['amountPrecision']==null?3:m['amountPrecision']
				)

		log.debug "Created entry:" + entry
		
		group.addEntry(entry)

		entry.processAndSave()

		entry.createRepeat()

		Entry generator = entry.fetchGeneratorEntry()

		if (generator != null)
			generator.updateDurationEntry() // make sure duration entries remain consistent

		stats.addTag(entry.getTag())
		stats.setTimeZoneId(entry.getTimeZoneId())

		return entry
	}
	
	static Tag tagWithSuffixForUnits(Tag baseTag, String units) {
		if (unit) {
			String suffix = UnitGroupMap.theMap.getSuffixForUnit(baseTagDescription, units)
			if (suffix)
				return Tag.look(baseTag.getDescription() + ' ' + suffix)
		}
		
		return baseTag
	}

	/**
	 * Create Entry
	 * 
	 * EntryStats are passed in to handle batch jobs, so entry statistics are updated all at once at the end of
	 * the entry creation sequence, rather than on each entry creation
	 */
	static Entry create(Long userId, Map m, EntryStats stats = null) {
		log.debug "Entry.create() userId:" + userId + ", m:" + m

		if (m == null) return null
		
		boolean singleEntryCreation = false
		
		if (stats == null) {
			// if no passed-in stats, this is a one-time entry creation event
			singleEntryCreation = true
			stats = startEntryCreation(userId)
		}

		if (m['description'] == null) return null

		m['description'] = m['description'].toLowerCase()

		Tag baseTag = Tag.look(m['baseTagDescription'] ?: m['description'])
		
		Integer timeZoneId = (Integer) m['timeZoneId'] ?: (Integer)TimeZoneId.look(m['timeZoneName']).getId()

		def tagUnitStats
		if (!m['units']) {
			// Using the most used unit in case the unit is unknown
			tagUnitStats = TagUnitStats.mostUsedTagUnitStats(userId, tag.getId())
			if (tagUnitStats) {
				m['units'] = tagUnitStats.unit
			}
		} else {
			TagUnitStats.createOrUpdate(userId, tag.getId(), m['units'] == null?'':m['units'])
		}

		def amounts = m['amounts']
		
		EntryGroup entryGroup = null
		
		if (amounts.size() > 1) {
			entryGroup = new EntryGroup()
		}
		
		Entry firstEntry = null
		
		for (ParseAmount amount : amounts) {
			m['baseTag'] = baseTag
			m['tag'] = tagWithSuffixForUnits(baseTag, amount.getUnits())
			Entry e = createSingle(userId, m, entryGroup, stats)
			if (firstEntry == null) firstEntry = e
		}
		
		if (entryGroup) {
			Utils.save(entryGroup, true)
		}
		
		if (singleEntryCreation) {
			finishEntryCreation(stats)
		}

		return firstEntry
	}

	Date fetchPreviousDate(int minusDays = 1) {
		DateTime dateTime = fetchDateTime()
		DateTimeZone dateTimeZone = dateTime.getZone()
		LocalTime localTime = dateTime.toLocalTime()
		LocalDate newLocalDate = dateTime.toLocalDate().minusDays(minusDays)
		return newLocalDate.toDateTime(localTime, dateTimeZone).toDate()
	}

	/**
	 * check to see if parameters match current entry, so repeat doesn't need to update
	 */
	boolean repeatParametersMatch(Map m) {
		log.debug "Entry.repeatParametersMatch() m:" + m + " for " + toString()

		if (m == null) return false

		if (m['description'] == null) return false

		if (!(this.tag.description == m['description'].toLowerCase()))
			return false

		DateTime entryDateTime = fetchDateTime()
		LocalTime localTime = entryDateTime.toLocalTime()
		DateTimeZone origTimeZone = entryDateTime.getZone()
		DateTime newDateTime = new DateTime(m['date'], origTimeZone)

		if (!localTime.equals(newDateTime.toLocalTime()))
			return false

		return m['datePrecisionSecs'] == this.datePrecisionSecs && amount == m['amount'] \
			&& comment == (m['comment'] ?: '') && repeatType == m['repeatType'] && units == (m['units'] ?: '') \
			&& amountPrecision == (m['amountPrecision'] ?: 3)

	}
	
	private def entryMap() {
		def m = [:]

		m['description'] = this.tag.getDescription()
		m['units'] = this.units
		m['repeatType'] = this.repeatType
		m['date'] = this.date
		m['timeZoneName'] = this.fetchTimeZoneName()
		m['datePrecisionSecs'] = this.datePrecisionSecs
		m['amount'] = this.amount
		m['units'] = this.units
		m['comment'] = this.comment
		m['repeatEnd'] = this.repeatEnd
		m['durationType'] = this.durationType
		m['setName'] = this.setIdentifier?.toString()
		m['amountPrecision'] = this.amountPrecision

		log.debug "Creating entry map:" + m

		return m
	}

	private Entry createRepeatOnBaseDate(Date baseDate, Map m, TagStatsRecord record, int addDays) {
		if (this.repeatEnd != null && this.repeatEnd <= this.date) { // do not create repeat if no future repeats
			return null
		}

		DateTimeZone dateTimeZone = DateTimeZone.forID(m['timeZoneName'])
		LocalTime mLocalTime = new DateTime(m['date'], dateTimeZone).toLocalTime()
		LocalDate baseLocalDate = new DateTime(baseDate, dateTimeZone).toLocalDate()
		DateTime repeatEndDateTime = new DateTime(this.repeatEnd, dateTimeZone)
		LocalTime repeatEndLocalTime = repeatEndDateTime.toLocalTime()
		LocalDate repeatEndLocalDate = repeatEndDateTime.toLocalDate()
		Date tomorrowRepeatEnd = repeatEndLocalDate.plusDays(1).toDateTime(repeatEndLocalTime, dateTimeZone).toDate()

		if (addDays > 0) baseLocalDate = baseLocalDate.plusDays(addDays)

		Date newDate = baseLocalDate.toDateTime(mLocalTime, dateTimeZone).toDate()

		// compute previous repeatEnd time by keeping original local time but subtracting a day
		DateTime currentDateTime = fetchDateTime()
		LocalTime currentLocalTime = currentDateTime.toLocalTime()
		DateTimeZone currentDateTimeZone = fetchDateTimeZone()
		Date prevDate = baseLocalDate.minusDays(2).toDateTime(currentLocalTime, currentDateTimeZone).toDate()
		while (newDate.getTime() - prevDate.getTime() > DAYTICKS)
			prevDate = prevDate + 1

		if (this.repeatEnd == null || newDate < tomorrowRepeatEnd) {
			m['date'] = newDate
			m['repeatEnd'] = this.repeatEnd

			def newEntry = create(this.userId, m, record)

			// set the repeat end of this entry to the day before the current date

			if (prevDate >= this.date)
				this.setRepeatEnd(prevDate)
			else
				this.setRepeatEnd(this.date)

			Utils.save(this, true)

			return newEntry
		}

		return null
	}

	protected static Entry updateGhostSingle(Entry entry, Map m, TagStatsRecord record, Date baseDate, boolean allFuture) {
		boolean isToday = isToday(entry.getDate(), baseDate)

		if (entry.repeatParametersMatch(m))
			return entry // no need to update when entry doesn't change

		if (isToday) {
			if (allFuture) {
				entry.doUpdate(m, record)

				return entry // allFuture updates can just edit the entry and return
			} else {
				entry.createRepeatOnBaseDate(baseDate, entry.entryMap(), record, 1)

				if (entry.getRepeatEnd() == entry.getDate()) {
					m['repeatEnd'] = m['date']
				} else
					m['repeatEnd'] = entry.getRepeatEnd()

				entry.doUpdate(m, record)

				return entry
			}
		} else {
			if (!allFuture) { // create another entry after the base date
				entry.createRepeatOnBaseDate(baseDate, entry.entryMap(), record, 1)
			}

			return entry.createRepeatOnBaseDate(baseDate, m, record, 0)
		}
	}

	/**
	 * Update entry, possibly updating multiple entries, given map of parameters (output of the parse method or manually created)
	 */
	protected static Entry updateSingle(Entry entry, Map m, Date baseDate, boolean allFuture = true) {
		log.debug "Entry.update() entry:" + entry + ", m:" + m + ", baseDate:" + baseDate + ", allFuture:" + allFuture
		def repeatType = entry.getRepeatType()
		if (repeatType != null && repeatType.isRepeat()) {
			return updateGhostSingle(entry, m, tagStatsRecord, baseDate, allFuture)
		} else {
			entry.doUpdate(m, tagStatsRecord)

			return entry
		}
	}

	/**
	 * Delete actually just hides the entries, rather than deleting them
	 */
	public static delete(Entry entry, TagStatsRecord tagStatsRecord) {
		log.debug "Entry.delete() entry:" + entry

		Entry updateDurationEntry = entry.preDeleteProcessing()

		// if this was an end entry, delete current duration entry, delete this entry, then recalculate subsequent duration entry

		long oldUserId = entry.getUserId()

		log.debug "Zeroing the user id of the entry and saving, updating the tag stats"
		entry.setUserId(0L) // don't delete entry, just zero its user id
		Utils.save(entry, true)

		TagStats tagStats = TagStats.createOrUpdate(oldUserId, entry.getTag().getId())

		if (tagStatsRecord != null)
			tagStatsRecord.setOldTagStats(tagStats)

		if (updateDurationEntry != null) {
			updateDurationEntry.updateDurationEntry()
		}
	}

	public boolean isDeleted() {
		return userId == 0L
	}

	/**
	 * Delete a ghost entry on a given baseDate and for a current date
	 * @param ghostTime
	 * @param currentDate
	 * @return
	 */
	public static deleteGhost(Entry entry, Date baseDate, boolean allFuture) {
		if (entry.getRepeatType().isContinuous()) {
			Entry.delete(entry, null)

			return
		}

		long entryTime = entry.getDate().getTime()
		long baseTime = baseDate.getTime()
		Date repeatEndDate = entry.getRepeatEnd()
		boolean endAfterToday = repeatEndDate == null ? true : repeatEndDate.getTime() >= (entry.getDate().getTime() + DAYTICKS - HOURTICKS)

		if (allFuture) {
			if (entryTime >= baseTime && entryTime - baseTime < DAYTICKS) { // entry is in today's data
				TagStatsRecord record = new TagStatsRecord()
				Entry.delete(entry, record)
			} else if (entryTime <= baseTime) { // this should always be true
				entry.setRepeatEnd(baseDate)
				Utils.save(entry, true)
			} else {
				log.error("Invalid call for entry: " + entry + " for baseDate " + baseDate)
			}
		} else {
			if (entryTime >= baseTime && entryTime - baseTime < DAYTICKS) { // entry is in today's date, change to next day
				if (endAfterToday) {
					Date newDate = TimeZoneId.nextDaySameTime(entry.fetchDateTime()).toDate()

					if (repeatEndDate != null && repeatEndDate < newDate) {
						Entry.delete(entry, null)
					} else {
						entry.setDate(newDate)

						Utils.save(entry, true)
					}
				} else { // otherwise, delete entry
					Entry.delete(entry, null)
				}
			} else if (entryTime <= baseTime) { // this should always be true
				Date prevDate = TimeZoneId.previousDayFromDateSameTime(baseDate, entry.fetchDateTime()).toDate()
				long prevTime = prevDate.getTime()
				if (prevTime <= entryTime)
					entry.setRepeatEnd(entry.getDate())
				else
					entry.setRepeatEnd(prevDate)
				Utils.save(entry, true)

				if (endAfterToday) {
					DateTime newDateTime = TimeZoneId.nextDayFromDateSameTime(baseDate, entry.fetchDateTime())

					Date newDate = newDateTime.toDate()

					if (repeatEndDate == null || repeatEndDate >= newDate) {
						def m = [:]
						m['description'] = entry.getTag().getDescription()
						m['date'] = newDateTime.toDate()

						def repeatId = entry.getRepeatType().getId()
						def toggleType = entry.getRepeatType().toggleGhost()

						def repeatTypes

						if (toggleType != null)
							repeatTypes = [
								repeatId,
								entry.getRepeatType().toggleGhost().getId()
							]
						else
							repeatTypes = [ repeatId ]

						// look for entry that already exists one day later
						// new date is one day after current baseDate

						String queryStr = "from Entry entry where entry.tag.description = :desc and entry.date = :entryDate and entry.userId = :userId and entry.repeatType.id in (:repeatIds) order by entry.date desc limit 1"

						def entries = Entry.executeQuery(queryStr, [desc:m['description'], entryDate:m['date'], userId:entry.getUserId(), repeatIds:repeatTypes])

						Entry e = entries[0]

						if (e != null) {
							return // subsequent repeat entry already exists
						}

						// create new entry one day after current baseDate
						m['timeZoneName'] = entry.fetchTimeZoneName()
						m['amount'] = entry.getAmount()
						m['amountPrecision'] = entry.getAmountPrecision()
						m['units'] = entry.getUnits()
						m['baseTag'] = entry.getBaseTag()
						m['durationType'] = entry.getDurationType()
						m['repeatType'] = entry.getRepeatType()
						m['comment'] = entry.getComment()
						m['datePrecisionSecs'] = entry.getDatePrecisionSecs()
						def retVal = Entry.create(entry.getUserId(), m, null)
					}
				}
			} else {
				log.error("Invalid call for entry: " + entry + " for baseDate " + baseDate)
			}
		}
	}

	// look for any duration entry that needs to be updated after a delete
	protected Entry preDeleteProcessing() {
		// if this is the start entry for a subsequent end entry, find it and queue it for updating
		if (fetchIsStart()) {
			def next = fetchNextStartOrEndEntry()
			if (next != null && next.fetchIsEnd())
				return next
		}

		// if this is an end entry, delete current duration entry if it exists, find subsequent end entry if it exists,
		// queue it for updating
		if (fetchIsEnd()) {
			def duration = findDurationEntry()
			if (duration != null) delete(duration, null)
			def next = fetchNextStartOrEndEntry()
			if (next != null && next.fetchIsEnd())
				return next
		}

		return null
	}

	/**
	* Retrieve tags by user
	*/
	public static final int BYALPHA = 0
	public static final int BYCOUNT = 1
	public static final int BYRECENT = 2
	public static final int ONLYIDS = 3
	public static final int BYCOUNTONLYDESCRIPTION = 4

	public static def countSeries(userId, tagId, amountPrecision) {
		def c = Entry.createCriteria()
		def tag = Tag.get(tagId)
		def results = []
		// Can we compose/add onto criteria to avoid repeated code?
		if (amountPrecision != null) {
			results = c {
				projections {
					count('id')
				}
				and {
					eq("userId", userId)
					eq("tag", tag)
					eq("amountPrecision", amountPrecision)
				}
			}
		} else {
			results = c {
				projections {
					count('id')
				}
				and {
					eq("userId", userId)
					eq("tag", tag)
				}
			}
		}
		results[0]
	}

	static def getTags(User user, type = BYALPHA) {
		log.debug "Entry.getTags() userID:" + user.getId() + ", type:" + type

		def retVal

		DatabaseService databaseService = DatabaseService.get()

		if (type == BYCOUNT) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = ? and e.date is not null group by t.id order by count(e.id) desc", [user.getId()])
		} /*else if (type == BYRECENT) {
		 retVal = Entry.executeQuery("select entry.tag.description, max(entry.date) as maxdate from Entry as entry left join TagProperties prop on prop.userId = entry.userId and prop.tagId = entry.tag.id where entry.userId = ? and entry.date is not null " + (recentOnly ? "and datediff(now(), entry.date) < 90 " : "") + "group by entry.tag.id order by max(entry.date) desc", [user.getId()])
		 } */ else if (type == BYALPHA) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = ? and e.date is not null group by t.id order by t.description", [user.getId()])
		} else if (type == ONLYIDS) {
			retVal = Entry.executeQuery("select entry.tag.id from Entry as entry where entry.userId = ? and entry.date is not null group by entry.tag.id", [user.getId()])
		} else if (type == BYCOUNTONLYDESCRIPTION) {
			retVal = Entry.executeQuery("select entry.tag.description from Entry as entry where entry.userId = ? and entry.date is not null group by entry.tag.id order by count(entry.id) desc", [user.getId()])
		}

		//log.debug "Returned:" + retVal

		return retVal
	}

	static def getAutocompleteTags(User user) {
		log.debug "Entry.getAutocompleteTags() userID:" + user.getId()

		def tagStats = TagStats.activeForUser(user)

		def freqTagStats = tagStats['freq']
		def algTagStats = tagStats['alg']

		def freqTags = []
		def algTags = []

		for (tag in freqTagStats) {
			freqTags.add(tag.getDescription())
		}

		for (tag in algTagStats) {
			algTags.add(tag.getDescription())
		}

		def retVal = [freq: freqTags, alg: algTags]

		return retVal
	}

	static def getAutocompleteTagsWithInfo(User user) {
		log.debug "Entry.getAutocompleteTagsWithInfo() userID:" + user.getId()

		def tagStats = TagStats.activeForUser(user)

		def freqTagStats = tagStats['freq']
		def algTagStats = tagStats['alg']

		def allTagStats = []

		for (tag in freqTagStats) {
			allTagStats.add([
				tag.getDescription(),
				tag.getLastAmount(),
				tag.getLastAmountPrecision(),
				tag.getLastUnits()
			])
		}

		def freqTags = []
		def algTags = []

		for (tag in freqTagStats) {
			freqTags.add(tag.getDescription())
		}

		for (tag in algTagStats) {
			algTags.add(tag.getDescription())
		}

		def retVal = [all: allTagStats, freq: freqTags, alg: algTags]

		return retVal
	}

	/**
	 * fetchDateTimeZone()
	 *
	 * Returns Joda time zone for this entry
	 */
	DateTimeZone fetchDateTimeZone() {
		TimeZoneId timeZoneId = TimeZoneId.fromId(timeZoneId)

		return timeZoneId.toDateTimeZone()
	}

	/**
	 * fetchLocalTimeInZone()
	 *
	 * Returns Joda time zone for this entry
	 */
	DateTime fetchDateTimeInZone(DateTimeZone dateTimeZone) {
		return new DateTime(this.date, dateTimeZone)
	}

	/**
	 * fetchLocalTimeInZone()
	 *
	 * Returns Joda date time in current time zone
	 */
	DateTime fetchDateTime() {
		return new DateTime(this.date, this.fetchDateTimeZone())
	}

	String fetchTimeZoneName() {
		TimeZoneId timeZoneId = TimeZoneId.fromId(timeZoneId)

		return timeZoneId.getName()
	}

	/**
	 * fetchIsGenerated()
	 *
	 * Returns true if this entry is dynamically generated, not manually entered. This method is used to prevent user
	 * editing of generated entries
	 */
	boolean fetchIsGenerated() {
		return fetchGeneratorEntry() ? true : false
	}

	/**
	 * fetchGeneratorEntry()
	 *
	 * Returns end entry corresponding to this generated entry if it is a generated entry
	 */
	Entry fetchGeneratorEntry() {
		if (!durationType.equals(DurationType.NONE)) return null

		if (units && units.equals('hours')) {
			def c = Entry.createCriteria()

			def results = c {
				eq("userId", userId)
				eq("date", date)
				eq("baseTag", tag)
				eq("durationType", DurationType.END)
			}

			for (Entry r in results) {
				if (r.getId() != this.id) return r
			}
		}
		return null
	}

	/**
	 * fetchIsEnd()
	 *
	 * Returns true if this entry is the end of a duration pair
	 */
	boolean fetchIsEnd() {
		return durationType.equals(DurationType.END)
	}

	/**
	 * fetchIsStart()
	 *
	 * Returns true if this entry is the start of a duration pair
	 */
	boolean fetchIsStart() {
		return durationType.equals(DurationType.START)
	}

	/**
	 * retrieveDurationEntry()
	 *
	 * If this is an end entry, returns corresponding duration entry
	 * @return
	 */
	Entry fetchDurationEntry() {
		if (!fetchIsEnd()) return null

		def c = Entry.createCriteria()

		// look for entries with the exact end time, with tag = base tag
		// see findDurationEntry to find with looser time criteria

		def results = c {
			eq("userId", userId)
			eq("date", date)
			eq("tag", baseTag)
			eq("units", "hours")
			or {
				eq("durationType", DurationType.NONE)
				isNull("durationType")
			}
			maxResults(1)
		}

		return results[0]
	}

	/**
	 * Get the end entry corresponding to this if it is a start entry
	 * @return
	 */
	Entry fetchEndEntry() {
		if (!fetchIsStart()) return null

		Entry next = fetchNextStartOrEndEntry()

		if (next && next.fetchIsEnd())
			return next

		return null
	}

	/**
	 * Get the start entry corresponding to this if it is an end entry
	 * @return
	 */
	Entry fetchStartEntry() {
		if (!fetchIsEnd()) return null

		Entry prev = fetchPreviousStartOrEndEntry()

		if (prev && prev.fetchIsStart())
			return prev

		return null
	}

	/**
	 * findDurationEntry()
	 *
	 * Look for duration entries within 5 minutes of end entry time
	 * @return
	 */
	Entry findDurationEntry() {
		def durationEntry = fetchDurationEntry()

		if (durationEntry != null) return durationEntry

		if (!fetchIsEnd()) return null

		def c = Entry.createCriteria()

		// look for duration entry within 5 minutes of the current end entry

		long t = date.getTime()

		def results = c {
			eq("userId", userId)
			gt("date", new Date(t - 5 * 60000))
			lt("date", date)
			eq("tag", baseTag)
			eq("units", "hours")
			or {
				eq("durationType", DurationType.NONE)
				isNull("durationType")
			}
			maxResults(1)
			order ("date", "desc")
		}

		def candidate = results[0]

		c = Entry.createCriteria()

		results = c {
			eq("userId", userId)
			lt("date", new Date(t + 5 * 60000))
			gt("date", date)
			eq("tag", baseTag)
			eq("units", "hours")
			or {
				eq("durationType", DurationType.NONE)
				isNull("durationType")
			}
			maxResults(1)
			order ("date", "asc")
		}

		if (results[0] != null) {
			def above = results[0]
			if (candidate == null || date.getTime() - candidate.getDate().getTime() > above.getDate().getTime() - date.getTime()) {
				candidate = above
			}
		}

		if (candidate == null)
			return null

		// normalize duration entry to same date as end entry

		candidate.setDate(date)

		Utils.save(candidate, true)

		return candidate
	}

	/**
	 * calculateDuration()
	 *
	 * Returns duration width for this end entry in milliseconds
	 */
	protected Long calculateDuration() {
		if (!fetchIsEnd()) return null

		def pre = fetchPreviousStartOrEndEntry()
		if (pre != null) {
			if (pre.fetchIsEnd()) return null
			return date.getTime() - pre.getDate().getTime()
		}

		return null
	}

	protected Entry fetchPreviousStartOrEndEntry() {
		def c = Entry.createCriteria()

		// look for latest matching entry prior to this one

		def results = c {
			eq("userId", userId)
			lt("date", date)
			eq("baseTag", baseTag)
			or {
				eq("durationType", DurationType.START)
				eq("durationType", DurationType.END)
			}
			maxResults(1)
			order("date", "desc")
		}

		return results[0]
	}

	/**
	 * Return next start or end entry matching this entry
	 */
	protected Entry fetchNextStartOrEndEntry() {
		def c = Entry.createCriteria()

		// look for the next start or end entry

		def results = c {
			eq("userId", userId)
			gt("date", date)
			eq("baseTag", baseTag)
			or {
				eq("durationType", DurationType.START)
				eq("durationType", DurationType.END)
			}
			maxResults(1)
			order("date", "asc")
		}

		if (results[0] != null)
			return results[0]

		return null
	}

	protected Entry updateDurationEntry() {
		if (!fetchIsEnd()) return null

		Long duration = calculateDuration()

		def c = Entry.createCriteria()

		def results = c {
			eq("userId", userId)
			eq("date", date)
			eq("tag", baseTag)
			eq("units", "hours")
			or {
				eq("durationType", DurationType.NONE)
				isNull("durationType")
			}
		}

		Entry durationEntry = null
		Entry lastDurationEntry = null

		for (Entry entry in results) {
			if (lastDurationEntry != null) {
				Entry.delete(entry, null)
			} else
				durationEntry = entry
			lastDurationEntry = entry
		}

		if (durationEntry == null)
			durationEntry = findDurationEntry()

		if (duration == null) {
			while (durationEntry != null) {
				Entry.delete(durationEntry, null)
				durationEntry = findDurationEntry()
			}
			return null
		} else {
			BigDecimal amount = new BigDecimal(duration).divide(new BigDecimal(3600000L), 6, BigDecimal.ROUND_HALF_UP)

			if (durationEntry == null) {
				def m = [:]
				m['date'] = date
				m['datePrecisionSecs'] = datePrecisionSecs
				m['timeZoneName'] = TimeZoneId.fromId(timeZoneId).getName()
				m['description'] = baseTag.getDescription()
				m['amount'] = amount
				m['units'] = "hours"
				m['comment'] = this.comment ?: ""
				m['repeatType'] = null
				m['baseTag'] = baseTag
				m['durationType'] = DurationType.NONE
				m['amountPrecision'] = DEFAULT_AMOUNTPRECISION
				durationEntry = Entry.create(userId, m, null)
			} else {
				DatabaseService.retry(durationEntry) {
					durationEntry.setAmount(amount)
					durationEntry.save(flush:true)
					return durationEntry
				}
			}

			return durationEntry
		}
	}

	/**
	 * If this is a new end entry, update duration entries
	 * @return
	 */
	protected def processAndSave() {
		try {
			Utils.save(this, true)
		} catch (Throwable t) {
			System.err.println("Invalid entry, failed to save: " + this)
			t.printStackTrace()
			return
		}

		if (fetchIsEnd()) {
			updateDurationEntry()
			Entry subsequent = fetchNextStartOrEndEntry()
			if (subsequent != null && subsequent.fetchIsEnd()) {
				Entry subsequentDuration = subsequent.findDurationEntry()

				if (subsequentDuration != null)
					Entry.delete(subsequentDuration, null)
			}
		}

		if (fetchIsStart()) {
			Entry subsequent = fetchNextStartOrEndEntry()
			if (subsequent != null && subsequent.fetchIsEnd()) {
				subsequent.updateDurationEntry()
			}
		}
	}

	private findNeighboringDailyRepeatEntry(String queryStr, parms) {
		parms['entryDate'] = this.date
		while (true) {
			def entries = DatabaseService.get().sqlRows(queryStr, parms)

			if (entries.size() > 0) {
				Entry e = Entry.get(entries[0]['id'])

				// check to see if this entry is actually the same local time as the deleted entry:
				// find the date of this entry in JodaTime with the time zone of the deleted entry, get its local time in that zone, compare with the local time of the deleted entry

				DateTimeZone dateTimeZone = this.fetchDateTimeZone()
				LocalTime thisLocalTime = this.fetchDateTimeInZone(dateTimeZone).toLocalTime()
				LocalTime entryLocalTime = e.fetchDateTimeInZone(dateTimeZone).toLocalTime()

				if (thisLocalTime.equals(entryLocalTime)) {
					return e
				} else {
					parms['entryDate'] = e.getDate()
				}
			} else
				break
		}

		return null
	}

	private findPreviousRepeatEntry() {
		return findNeighboringDailyRepeatEntry(
		"select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date < :entryDate and (time(entry.date) = time(:entryDate) or time(date_add(entry.date, interval 1 hour)) = time(:entryDate) or time(entry.date) = time(date_sub(:entryDate, interval 1 hour))) and entry.user_id = :userId and (entry.repeat_type in (:repeatIds)) order by entry.date desc limit 1",
		[desc:this.tag.getDescription(), userId:this.userId, repeatIds:DAILY_IDS])
	}

	private findNextRepeatEntry() {
		return findNeighboringDailyRepeatEntry("select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date > :entryDate and (time(entry.date) = time(:entryDate) or time(date_add(entry.date, interval 1 hour)) = time(:entryDate) or time(entry.date) = time(date_sub(:entryDate, interval 1 hour))) and entry.user_id = :userId and (entry.repeat_type in (:repeatIds)) order by entry.date asc limit 1",
		[desc:this.tag.getDescription(), userId:this.userId, repeatIds:DAILY_IDS])
	}

	private createRepeat() {
		if (this.repeatType == null) return

		if (this.repeatType.isContinuous()) {
			// search for matching continuous tag
			String queryStr = "select entry.id from entry entry, tag tag where entry.id != :entryId and entry.tag_id = tag.id and entry.user_id = :userId and tag.description = :desc and entry.repeat_type in (:repeatIds)"

			def entries = DatabaseService.get().sqlRows(queryStr, [entryId:this.id, desc:this.tag.description, userId:this.userId, repeatIds:CONTINUOUS_IDS])

			for (def v in entries) {
				if (v != null) {
					Entry e = Entry.get(v['id'])
					Entry.delete(e, null) // delete matching entries, to avoid duplicates
					Utils.save(e, true)
				}
			}
		} else {
			Entry e = findPreviousRepeatEntry()

			if (e != null) {
				if (e.getRepeatEnd() == null || e.getRepeatEnd() > this.date) { // only reset repeatEnd if it is later
					e.setRepeatEnd(this.fetchPreviousDate())
					Utils.save(e, true)
				}
			}

			e = findNextRepeatEntry()

			if (e != null) {
				Date newPrevious = e.fetchPreviousDate()
				if (repeatEnd == null || newPrevious < repeatEnd)
					repeatEnd = newPrevious
			}
		}
	}

	def unGhost() {
		if (this.repeatType?.isGhost()) {
			this.repeatType = this.repeatType.toggleGhost()

			this.durationType = durationType?.unGhost()
			this.processAndSave()
		}

		return this
	}


	/**
	 * Calculates corresponding DateTime for this entry if it were to have happened on baseDate in the specified time zone
	 *
	 * For example if this entry were at "2pm" in the original time zone, figure out what would be 2pm on a day close to the
	 * baseDate in the original time zone, adjusting for daylight savings time. Then translate to the new time zone.
	 *
	 * @param baseDate
	 * @param currentTimeZone
	 * @return translated dateTime corresponding to new date and new time zone
	 */
	DateTime fetchCorrespondingDateTimeInTimeZone(Date baseDate, DateTimeZone currentTimeZone) {
		DateTimeZone entryTimeZone = this.fetchDateTimeZone()

		DateTime thisDateTime = this.fetchDateTime()
		LocalTime localTime = thisDateTime.toLocalTime()
		// create dateTime in the original time zone, but with today's localDate
		DateTime baseDateTimeInEntryTimeZone = new DateTime(baseDate, entryTimeZone)
		LocalDate currentLocalDateInEntryTimeZone = baseDateTimeInEntryTimeZone.toLocalDate()
		DateTime tryDateTime = currentLocalDateInEntryTimeZone.toDateTime(localTime, entryTimeZone)

		// compare the currentLocalDate with the local date of tryDateTime
		LocalDate currentLocalDateInCurrentTimeZone = new DateTime(baseDate, currentTimeZone).toLocalDate()
		LocalDate tryLocalDate = tryDateTime.toLocalDate()

		int compare = tryLocalDate.compareTo(currentLocalDateInCurrentTimeZone)

		// if tryLocalDate is before currentLocalDate, add one day to tryLocalDate and recreate
		if (compare < 0) {
			tryDateTime = tryLocalDate.plusDays(1).toDateTime(localTime, entryTimeZone)
		} else if (compare > 0) {
			tryDateTime = tryLocalDate.minusDays(1).toDateTime(localTime, entryTimeZone)
		}

		return new DateTime(tryDateTime.getMillis(), currentTimeZone)
	}

	def activateGhostEntry(Date currentBaseDate, Date nowDate, String timeZoneName) {
		long dayStartTime = currentBaseDate.getTime()
		def now = nowDate.getTime()
		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()
		long diff = now - dayStartTime
		long thisDiff = this.date.getTime() - dayStartTime

		if (this.repeatType == null)
			return null

		if (!this.repeatType.isContinuous() && (thisDiff >=0 && thisDiff < DAYTICKS)) {
			// activate this entry

			this.unGhost()

			return this
		}

		def m = [:]
		m['timeZoneName'] = timeZoneName
		m['description'] = this.tag.getDescription()
		m['amount'] = this.amount
		m['amountPrecision'] = this.amountPrecision
		m['units'] = this.units
		m['durationType'] = this.durationType?.unGhost()
		m['baseTag'] = baseTag

		if (this.repeatType.isContinuous()) {
			// continuous tags create new tags based on the template
			m['comment'] = ''
			m['repeatType'] = null
			if (diff >= 0 && diff < DAYTICKS) {
				m['date'] = nowDate
				m['datePrecisionSecs'] = DEFAULT_DATEPRECISION_SECS
			} else {
				m['date'] = new Date(dayStartTime + HALFDAYTICKS)
				m['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS
			}
			def retVal = Entry.create(userId, m, null)

			Utils.save(retVal, true)
			return retVal
		} else { // this repeat element isn't continuous
			m['comment'] = this.comment
			m['repeatType'] = this.repeatType
			m['date'] = fetchCorrespondingDateTimeInTimeZone(currentBaseDate, currentTimeZone).toDate()
			m['datePrecisionSecs'] = this.datePrecisionSecs
			def retVal = Entry.create(userId, m, null)

			retVal.unGhost()

			return retVal
		}
	}

	/**
	 * Update existing entry and save
	 */
	private doUpdate(Map m, TagStatsRecord tagStatsRecord) {
		long oldTagId = this.tag.getId()
		doUpdate(m)
		long newTagId = this.tag.getId()

		TagStats oldTagStats = TagStats.createOrUpdate(this.userId, oldTagId)
		TagStats newTagStats = null

		if (newTagId != oldTagId) {
			newTagStats = TagStats.createOrUpdate(this.userId, newTagId)
		}

		if (tagStatsRecord != null) {
			tagStatsRecord.setOldTagStats(oldTagStats)
			tagStatsRecord.setNewTagStats(newTagStats)
		}
	}

	private doUpdateSingle(Map m) {
		log.debug "Entry.doUpdate() this:" + this + ", m:" + m

		if (m == null) return null

		def newDescription = m['description']

		if (newDescription == null) return null

		newDescription = newDescription.toLowerCase()

		WORK STOPPED HERE --- finish new update logic
		
		def (newBaseTag, newDurationType) = getDurationInfoFromStrings(newDescription, m['units'], m['repeatType'])

		def updateDurationEntry = null

		if (!date.equals(m['date'])) {
			updateDurationEntry = preDeleteProcessing()
		}

		def descriptionChanged = !description.equals(newDescription)

		def amt = m['amount']
		amt = amt == null ? null : new BigDecimal(amt, mc)

		/**
		 * check to see if entry repeat type has changed. If so, merge and/or create new repeat structure
		 */
		boolean updateRepeatType = false

		if (m['repeatType'] != null && this.date != m['date'] || (!Utils.equals(amt, this.amount))
				|| (!Utils.equals(this.repeatType, m['repeatType']))) {
			updateRepeatType = true
		}

		setDate(m['date'])
		setDatePrecisionSecs(m['datePrecisionSecs'])
		updateDescription(newDescription)
		setAmount(amt)
		setAmountPrecision(m['amountPrecision']?:DEFAULT_AMOUNTPRECISION)
		setUnits(m['units']?:'')
		setComment(m['comment']?:'')
		setRepeatType(m['repeatType']?.forUpdate())
		setSetIdentifier(Identifier.look(m['setName']))
		setBaseTag(newBaseTag)
		setDurationType(newDurationType)
		setTimeZoneId((Integer)TimeZoneId.look(m['timeZoneName']).getId())
		setRepeatEnd(m['repeatEnd'] ? m['repeatEnd'] : this.repeatEnd)

		Utils.save(this, true)

		if (updateRepeatType)
			createRepeat()

		if (updateDurationEntry != null) {
			updateDurationEntry.updateDurationEntry()
		}

		processAndSave()

		log.debug "Repeat type: " + m['repeatType']
	}

	/**
	 * Fetch methods - for getting different subsets of entries
	 */
	public static def fetchContinuousRepeats(Long userId, Date now, Date currentDate) {
		String continuousQuery = "from Entry entry where entry.userId = :userId and entry.repeatEnd is null and (not entry.repeatType is null) and entry.repeatType.id in (:repeatIds)"

		def entries = Entry.executeQuery(continuousQuery, [userId:userId, repeatIds:CONTINUOUS_IDS])

		return entries
	}

	static def fetchReminders(User user, Date startDate, long intervalSecs) {
		Date endDate = new Date(startDate.getTime() + intervalSecs * 1000L)

		String queryStr = "select distinct entry.id " \
		+ "from entry entry where entry.user_id = :userId and " \
		+ "entry.date < :endDate and (entry.repeat_end is null or entry.repeat_end >= :startDate) and (not entry.repeat_type is null) and (entry.repeat_type & :remindBit <> 0) " \
		+ "and (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400 < :interval"

		def rawResults = DatabaseService.get().sqlRows(queryStr, [userId:user.getId(), endDate:endDate, startDate:startDate, interval:intervalSecs, remindBit:RepeatType.REMIND_BIT])
	}

	static long abs(long x) { return x < 0 ? -x : x }

	static def fetchListData(User user, String timeZoneName, Date baseDate, Date now) {
		long nowTime = now.getTime()
		long baseTime = baseDate.getTime()

		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()

		// get regular elements + timed repeating elements next
		String queryStr = "select distinct entry.id " \
				+ "from entry entry, tag tag where entry.user_id = :userId and (((entry.date >= :startDate and entry.date < :endDate) and (entry.repeat_type is null or (not entry.repeat_type in (:continuousIds)))) or " \
				+ "(entry.date < :startDate and (entry.repeat_end is null or entry.repeat_end >= :startDate) and (not entry.repeat_type is null) and (not entry.repeat_type in (:continuousIds)))) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by case when entry.date_precision_secs < 1000 and (entry.repeat_type is null or (not entry.repeat_type in (:continuousIds))) " \
				+ "then unix_timestamp(timestampadd(second, (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400, :startDate)) else " \
				+ "(case when entry.repeat_type is null then 99999999999 else 0 end) end desc, tag.description asc"

		def queryMap = [userId:user.getId(), startDate:baseDate, endDate:baseDate + 1, continuousIds:CONTINUOUS_IDS]

		def rawResults = DatabaseService.get().sqlRows(queryStr, queryMap)

		def timedResults = []

		Set resultTagIds = new HashSet<Long>()

		for (result in rawResults) {
			Entry entry = Entry.get(result['id'])
			def desc = entry.getJSONDesc()

			// use JodaTime to figure out the correct time in the current time zone
			desc['timeZoneName'] = timeZoneName
			if (entry.repeatType != null) { // adjust dateTime for repeat entries
				def date = entry.fetchCorrespondingDateTimeInTimeZone(baseDate, currentTimeZone).toDate()
				if (entry.repeatEnd != null && entry.repeatEnd.getTime() < date.getTime()) // check if adjusted time is after repeat end
					continue
				desc['date'] = date
			}
			if ((entry.getDate().getTime() != desc['date'].getTime()) && entry.repeatType != null) { // repeat entry on a future date
				if ((desc['repeatType'] & RepeatType.REMIND_BIT) != 0) {
					desc['repeatType'] = entry.repeatType.id | RepeatType.GHOST_BIT // no longer ghost repeat entries, but do ghost remind
					/* desc['amount'] = null
					 desc['amountPrecision'] = -1 */
				}
			} else if (entry.repeatType != null) {
				desc['repeatType'] = entry.repeatType.id
				/* no longer nullify amount for remind entries
				 if (((entry.repeatType.id & RepeatType.GHOST_BIT) != 0) && ((entry.repeatType.id & RepeatType.REMIND_BIT) != 0)) {
				 desc['amount'] = null
				 desc['amountPrecision'] = -1
				 }*/
			} else
				desc['repeatType'] = entry.repeatType?.id
			desc['setName'] = entry.setIdentifier?.toString()
			timedResults.add(desc)
		}

		// get continuous repeating elements
		String continuousQueryStr = "select distinct entry.id as id, tag.description " \
				+ "from entry entry, tag tag where entry.user_id = :userId and " \
				+ "entry.repeat_end is null and (not entry.repeat_type is null) and (entry.repeat_type in (:continuousIds)) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by tag.description asc"

		def continuousResults = DatabaseService.get().sqlRows(continuousQueryStr, [userId:user.getId(), continuousIds:CONTINUOUS_IDS])

		def results = []

		// add continuous elements that aren't already in the timed list
		for (result in continuousResults) {
			Entry entry = Entry.get(result['id'])
			if (!resultTagIds.contains(entry.getTag().getId())) {
				def desc = entry.getJSONDesc()
				desc['date'] = baseDate
				desc['repeatType'] = entry.repeatType?.id
				desc['timeZoneName'] = timeZoneName
				results.add(desc)
			}
		}

		// add timed results
		for (result in timedResults) {
			results.add(result)
			log.debug "Entry id: " + result['id'] + " tag: " + result['description']
		}

		return results
	}

	static def fetchListDataNoRepeats(User user, Date date) {
		def c = Entry.createCriteria()

		def results = []

		String queryStr = "select distinct entry.id " \
				+ "from entry entry, tag tag where entry.user_id = :userId and (entry.date >= :startDate and entry.date < :endDate) and (entry.repeat_type is null or (not entry.repeat_type in (:ghostIds))) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by case when entry.date_precision_secs < 1000 " \
				+ "then unix_timestamp(entry.date) else 0 end desc, tag.description asc"

		def rawResults = DatabaseService.get().sqlRows(queryStr, [userId:user.getId(), startDate:date, endDate:date + 1, ghostIds:GHOST_IDS])

		for (result in rawResults) {
			Entry entry = Entry.get(result['id'])
			def desc = entry.getJSONDesc()
			results.add(desc)
		}

		return Utils.listJSONDesc(results)
	}

	// limit repeat generation to 300 entries, for now
	public static final int MAX_PLOT_REPEAT = 300

	private static def generateRepeaterEntries(def repeaters, long endTimestamp, def results) {
		def repeaterOrder = { LocalTimeRepeater a, LocalTimeRepeater b ->
			if (a.getCurrentDateTime() == null) return 1 // sort null repeaters to end
			if (b.getCurrentDateTime() == null) return -1 // sort null repeaters to end
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
			Entry entry = repeater.getPayload()
			def desc = entry.getJSONShortDesc()
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

	static final double twoPi = 2.0d * Math.PI

	static def fetchAverageTime(User user, def tagIds, Date startDate, Date endDate, Date currentTime, DateTimeZone currentTimeZone) {
		DateTime startDateTime = new DateTime((Date)(startDate ?: currentTime), currentTimeZone) // calculate midnight relative to current time
		// set time to zero
		Date dateMidnight = startDateTime.toLocalDate().toDateTime(LocalTime.MIDNIGHT, currentTimeZone).toDate()

		// figure out the angle of the startDate
		String queryStr = "select (time_to_sec(:dateMidnight) / 43200.0) * pi() as angle"

		def queryMap = [dateMidnight:dateMidnight]

		double startDateAngle = DatabaseService.get().sqlRows(queryStr, queryMap)[0]['angle']

		if (endDate == null) endDate = currentTime // don't query past now if endDate isn't specified
		if (startDate == null) startDate = new Date(-30610137600000) // set start date to 1000 AD if startDate isn't specified

		// fetch sum of non-repeat entries
		queryStr = "select count(*) as c, sum(sin((time_to_sec(entry.date) / 43200.0) * pi())) as y, sum(cos((time_to_sec(entry.date) / 43200.0) * pi())) as x " \
				+ "from entry entry where entry.user_id = :userId and entry.amount is not null and (entry.repeat_end is null or entry.repeat_type in (:unghostedSingularIds)) " \
				+ "and entry.tag_id in (:tagIds) and entry.date >= :startDate and entry.date < :endDate"

		queryMap = [userId:user.getId(), startDate:startDate, endDate:endDate, tagIds:tagIds, unghostedSingularIds:UNGHOSTED_SINGULAR_IDS]

		def nonRepeatSum = DatabaseService.get().sqlRows(queryStr, queryMap)[0]

		// fetch sum of repeat entries
		queryStr = "select sum(datediff(if(entry.repeat_end is null, :endDate, if(entry.repeat_end < :endDate, entry.repeat_end, :endDate)), if(entry.date > :startDate, entry.date, :startDate))) as c, " \
				+ "sum(sin((time_to_sec(entry.date) / 43200.0) * pi()) * datediff(if(entry.repeat_end is null, :endDate, if(entry.repeat_end < :endDate, entry.repeat_end, :endDate)), if(entry.date > :startDate, entry.date, :startDate))) as y, " \
				+ "sum(cos((time_to_sec(entry.date) / 43200.0) * pi()) * datediff(if(entry.repeat_end is null, :endDate, if(entry.repeat_end < :endDate, entry.repeat_end, :endDate)), if(entry.date > :startDate, entry.date, :startDate))) as x " \
				+ "from entry entry where entry.user_id = :userId and entry.amount is not null and ((entry.repeat_end is not null and entry.repeat_end > :startDate) or entry.repeat_end is null) and entry.date < :endDate and entry.repeat_type in (:repeatIds) " \
				+ "and entry.tag_id in (:tagIds)"

		queryMap = [userId:user.getId(), startDate:startDate, endDate:endDate, repeatIds:DAILY_UNGHOSTED_IDS, tagIds:tagIds]

		def repeatSum = DatabaseService.get().sqlRows(queryStr, queryMap)[0]

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

	static def fetchPlotData(User user, def tagIds, Date startDate, Date endDate, Date currentTime, String timeZoneName) {
		log.debug "Entry.fetchPlotData() userId:" + user.getId() + ", tagIds:" + tagIds + ", startDate:" + startDate \
				+ ", endDate:" + endDate + ", timeZoneName:" + timeZoneName

		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()

		if (endDate == null) endDate = currentTime // don't query past now if endDate isn't specified
		if (startDate == null) startDate = new Date(-30610137600000) // set start date to 1000 AD if startDate isn't specified

		// get timed daily repeats (not ghosted)

		String queryStr = "select distinct entry.id " \
				+ "from entry entry, tag tag where entry.user_id = :userId and entry.amount is not null and (entry.repeat_end is null or entry.repeat_end >= :startDate) and entry.date < :endDate and entry.repeat_type in (:repeatIds) " \
				+ "and entry.tag_id in (:tagIds) order by entry.date asc"

		def queryMap = [userId:user.getId(), startDate:startDate, endDate:endDate, repeatIds:DAILY_UNGHOSTED_IDS, tagIds:tagIds]
		def rawResults = DatabaseService.get().sqlRows(queryStr, queryMap)

		def timedResults = []

		Set resultTagIds = new HashSet<Long>()

		// make this repeater list ordered by current timestamp

		def repeaters = []

		Long nextRepeaterTimestamp = null

		for (result in rawResults) {
			Entry entry = Entry.get(result['id'])

			RepeatType repeatType = entry.getRepeatType()
			if (repeatType != null) { // generate repeat values for query
				Date repeatEnd = entry.getRepeatEnd()
				if (repeatEnd == null)
					repeatEnd = endDate
				else if (repeatEnd.getTime() > endDate.getTime())
					repeatEnd = endDate
				LocalTimeRepeater repeater = new LocalTimeRepeater(entry, entry.fetchDateTime(), repeatEnd.getTime())
				repeaters.add(repeater)
			}
		}
		// get regular results
		
		Long userId = user.getId()
		
		UnitRatio mostUsedUnitRatioForTags = UnitGroupMap.theMap.mostUsedUnitRatioForTagIds(userId, tagIds)
		
		queryStr = "select e.date, e.amount, e.tag_id, e.units " \
				+ "from entry e where e.user_id = :userId " \
				+ "and e.amount is not null and e.date >= :startDate and e.date < :endDate and (e.repeat_type is null or e.repeat_type in (:unghostedSingularIds)) " \
				+ "and e.tag_id in (:tagIds) order by e.date asc"

		queryMap = [userId:user.getId(), startDate:startDate, endDate:endDate, unghostedSingularIds:UNGHOSTED_SINGULAR_IDS, tagIds:tagIds ]

		log.debug("Finding plot data for tag ids: " + tagIds)
		
		rawResults = DatabaseService.get().sqlRows(queryStr, queryMap)
		
		log.debug("Number of results: " + rawResults.size())
		
		def results = []
		
		double mostUsedUnitRatio = mostUsedUnitRatioForTags ? mostUsedUnitRatioForTags.ratio : 1.0d

		for (result in rawResults) {
			Date date = result['date']
			BigDecimal amount = result['amount']
			def entryJSON = [date, amount, Tag.fetch(result['tag_id'].longValue())?.getDescription()]
			UnitRatio unitRatio = mostUsedUnitRatioForTags?.unitRatioForUnits(result['units'])
			if (unitRatio) {
				entryJSON[1] = (amount * unitRatio.ratio) / mostUsedUnitRatio
			}
			long entryTimestamp = date.getTime()
			generateRepeaterEntries(repeaters, entryTimestamp, results)
			results.add(entryJSON)
		}
		// keep generating remaining repeaters until endDate
		generateRepeaterEntries(repeaters, endDate.getTime(), results)
		return results
	}

	static final long HALFDAYTICKS = 12 * 60 * 60 * 1000L
	static final int DAYSECS = 24 * 60 * 60
	static final int HALFDAYSECS = 12 * 60 * 60
	static final int HOURSECS = 60 * 60

	static def fetchSumPlotData(User user, def tagIds, Date startDate, Date endDate, Date currentDate, String timeZoneName) {
		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()

		if (endDate == null) endDate = new Date() // don't query past now if endDate isn't specified

		int averageSecs = fetchAverageTime(user, tagIds, startDate, endDate, currentDate, currentTimeZone)

		if (averageSecs > HALFDAYSECS + HOURSECS * 9) { // if the average time is after 9pm, do a "night sum"
			averageSecs -= DAYSECS
		}

		long offset = (averageSecs * 1000L) - HALFDAYTICKS
		if (startDate != null)
			startDate = new Date(startDate.getTime() + offset)
		else if (endDate != null)
			endDate = new Date(endDate.getTime() + offset)

		def rawResults = fetchPlotData(user, tagIds, startDate, endDate, currentDate, timeZoneName)

		Long startTime = startDate?.getTime()
		Long startDayTime = startTime
		Long endDayTime = startDayTime != null ? startDayTime + DAYTICKS : null

		def currentResult = null
		def summedResults = []
		
		UnitRatio mostUsedUnitRatio = UnitGroupMap.theMap.mostUsedUnitRatioForTagIds(user.getId(), tagIds)

		for (result in rawResults) {
			Date resultDate = result[SHORT_DESC_DATE]
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
					summedResults.add(currentResult)
				} else {
					currentResult[SHORT_DESC_AMOUNT] = currentResult[SHORT_DESC_AMOUNT] + result[SHORT_DESC_AMOUNT]
				}
			}

		}

		return summedResults
	}

	static def parseInt(String str) {
		return str == null ? null : Integer.parseInt(str)
	}

	static def parseMeta(String entryStr) {
		return parse(null, null, entryStr, null, false)
	}

	private static boolean isSameDay(Date time1, Date time2) {
		def t1 = time1.getTime()
		def t2 = time2.getTime()
		if (t1 < t2) return false
		if (t2 - t1 > DAYTICKS) return false

		return true
	}

	private static prependComment(retVal, str) {
		if (str != null && str.length() > 0 && retVal['comment'] != null && retVal['comment'].length() > 0)
			retVal['comment'] = str + ' ' + retVal['comment']
		else {
			retVal['comment'] = (str ?: '') + (retVal['comment'] ?: '')
			if (retVal['comment'].length() == 0)
				retVal['comment'] = null
		}
	}

	private static appendComment(retVal, str) {
		if (str != null && str.length() > 0 && retVal['comment'] != null && retVal['comment'].length() > 0)
			retVal['comment'] = retVal['comment'] + ' ' + str
		else {
			retVal['comment'] = (retVal['comment'] ?: '') + (str ?: '')
			if (retVal['comment'].length() == 0)
				retVal['comment'] = null
		}
	}

	/**
	 * If baseDate is null, do not store a date in the entry. This means it is a pure metadata entry
	 *
	 * Parsing using the following subpatterns:
	 *
	 *		[time] [tag] <[amount] <[units]>> <[comment]>
	 *
	 *		OR
	 *
	 *		[tag] <[amount] <[units]>> <[time]> <[comment]>
	 *
	 */

	static Pattern timePattern = ~/(?i)^((@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)($|[^A-Za-z0-9])|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?($|[^A-Za-z0-9])|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm)($|[^A-Za-z0-9]))/
	static Pattern timeEndPattern = ~/(?i)^((@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)($|[^A-Za-z0-9])|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?($|[^A-Za-z0-9])|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm)($|[^A-Za-z0-9]))$/
	static Pattern tagWordPattern = ~/(?i)^([^0-9\(\)@\s\.:=][^\(\)@\s:=]*)/
	static Pattern commentWordPattern = ~/^([^\s]+)/
	static Pattern amountPattern = ~/(?i)^(-?\.\d+|-?\d+\.\d+|\d+|_\b|-\b|__\b|___\b|zero\b|yes\b|no\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b)(\s*\/\s*(-?\.\d+|-?\d+\.\d+|\d+|_\b|-\b|__\b|___\b|zero\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b))?/
	static Pattern endTagPattern = ~/(?i)^(yes|no)/
	static Pattern unitsPattern = ~/(?i)^(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s+([^0-9\(\)@\s\.:][^\(\)@\s:]*))?)/
	static Pattern tagAmountSeparatorPattern = ~/(?i)^[:=]\s*/

	static def numberMap = ['zero' : '0', 'one':'1', 'two':'2', 'three':'3', 'four':'4', 'five':'5', 'six':'6', 'seven':'7', 'eight':'8', 'nine':'9']

	static boolean isToday(Date time, Date baseDate) {
		boolean today = false

		if (baseDate != null) {
			long diff = time.getTime() - baseDate.getTime();

			if (diff >= 0 && diff < DAYTICKS) {
				today = true
			}
		}

		return today
	}
	
	// see EntryJava.java for more repeat parsing code
	public static final Map<ArrayList<String>, ArrayList<Object>> durationSynonyms = [
		['wake']:['sleep', 'end', DurationType.END],
		['wake','up']:['sleep', 'end', DurationType.END],
		['woke']:['sleep', 'end', DurationType.END],
		['awakened']:['sleep', 'end', DurationType.END],
		['awoke']:['sleep', 'end', DurationType.END],
		['went','to','sleep']:['sleep', 'start', DurationType.START],
		['go','to','sleep']:['sleep', 'start', DurationType.START],
	]

	public static final Map<ArrayList<String>, ArrayList<Object>> durationSuffixes = [
		['start']:['start', DurationType.START],
		['starts']:['start', DurationType.START],
		['begin']:['start', DurationType.START],
		['begins']:['start', DurationType.START],
		['starting']:['start', DurationType.START],
		['beginning']:['start', DurationType.START],
		['started']:['start', DurationType.START],
		['begun']:['start', DurationType.START],
		['began']:['start', DurationType.START],
		['end']:['end', DurationType.END],
		['ends']:['end', DurationType.END],
		['stop']:['end', DurationType.END],
		['stops']:['end', DurationType.END],
		['finish']:['end', DurationType.END],
		['finished']:['end', DurationType.END],
		['ended']:['end', DurationType.END],
		['stopped']:['end', DurationType.END],
		['stopping']:['end', DurationType.END],
		['ending']:['end', DurationType.END],
		['finishing']:['end', DurationType.END],
	]

	public static final Map<ArrayList<String>, RepeatType> repeatWords = [
		['repeat']:RepeatType.DAILYCONCRETEGHOST,
		['repeat','daily']:RepeatType.DAILYCONCRETEGHOST,
		['repeat','weekly']:RepeatType.WEEKLYCONCRETEGHOST,
		['pinned']:RepeatType.CONTINUOUSGHOST,
		['remind']:RepeatType.REMINDDAILYGHOST,
		['remind','daily']:RepeatType.REMINDDAILYGHOST,
		['remind','weekly']:RepeatType.REMINDWEEKLYGHOST,
		['reminder']:RepeatType.REMINDDAILYGHOST,
		['reminder','daily']:RepeatType.REMINDDAILYGHOST,
		['reminder','weekly']:RepeatType.REMINDWEEKLYGHOST,
		['daily']:RepeatType.DAILYCONCRETEGHOST,
		['daily','repeat']:RepeatType.DAILYCONCRETEGHOST,
		['daily','remind']:RepeatType.REMINDDAILYGHOST,
		['daily','reminder']:RepeatType.REMINDDAILYGHOST,
		['weekly']:RepeatType.WEEKLYCONCRETEGHOST,
		['weekly','repeat']:RepeatType.WEEKLYCONCRETEGHOST,
		['weekly','remind']:RepeatType.REMINDWEEKLYGHOST,
		['weekly','reminder']:RepeatType.REMINDWEEKLYGHOST,
	]
	
	static class ParseAmount {
		BigDecimal amount
		int precision
		String units
		
		ParseAmount(BigDecimal amount, int precision) {
			this.amount = amount
			this.precision = precision
		}
	}
	
	static def parse(Date time, String timeZoneName, String entryStr, Date baseDate, boolean defaultToNow, boolean forUpdate = false) {
		log.debug "Entry.parse() time:" + time + ", timeZoneName:" + timeZoneName + ", entryStr:" + entryStr + ", baseDate:" + baseDate + ", defaultToNow:" + defaultToNow

		if (entryStr == '') return null // no input

		Date date = baseDate
		Long baseDateTime = baseDate?.getTime()
		Integer hours = null
		Integer minutes = null
		boolean today = isToday(time, date)

		// start by giving the time a precision of 3 minutes
		def retVal = [:]
		if (date == null)
			retVal['datePrecisionSecs'] = 0
		else
			retVal['datePrecisionSecs'] = DEFAULT_DATEPRECISION_SECS

		retVal['timeZoneName'] = timeZoneName
		retVal['today'] = today

		def matcher;
		def parts;

		// [time] [tag] <[amount] <[units]>>... <repeat|remind|pinned> (<[comment]>)
		//
		// OR
		//
		// [tag] <[amount] <[units]>>... <[time]> <repeat|remind|pinned> (<[comment]>)
		//
		// <repeat|remind|pinned> ahead of the above

		PatternScanner scanner = new PatternScanner(entryStr)

		ArrayList<ParseAmount> amounts = new ArrayList<ParseAmount>()
		boolean ghostAmount = false
		boolean foundTime = false
		boolean foundAMPM = false

		Closure timeClosure = {
			foundTime = true
			def noonmid = scanner.group(3)
			if (noonmid != null) {
				if (noonmid.equals('noon')) {
					hours = 12
					foundAMPM = true
					return
				} else if (noonmid.equals('midnight')) {
					hours = 0
					foundAMPM = true
					return
				}
			}
			hours = parseInt(scanner.group(17) ?: (scanner.group(11) ?: (scanner.group(4) ?: '0')))
			if (hours == 12) hours = 0
			minutes = parseInt(scanner.group(13) ?: (scanner.group(7) ?: '0'))
			if (scanner.group(9).equals('p') || scanner.group(15).equals('p') || scanner.group(21).equals('pm')) {
				foundAMPM = true
				hours += 12
			} else if (scanner.group(9).equals('a') || scanner.group(15).equals('a') || scanner.group(21).equals('am')) {
				foundAMPM = true
			}
		}

		Closure amountClosure = {
			def amountStr, amountStrs = []
			int amountIndex = amounts.size()
			boolean twoDAmount = false
			
			amountStrs.add(scanner.group(1))
			amountStr = scanner.group(3)
			
			if (amountStr) {
				amountStrs.add(amountStr)
				twoDAmount = true
			}
			
			for (amount in amountStrs) {
				if (amount =~ /-\s/ || amount.startsWith('_')) {
					amounts.add(new ParseAmount(null, -1))
					ghostAmount = true
				} else if (amount.equalsIgnoreCase('yes')) {
					amounts.add(new ParseAmount(new BigDecimal(1, mc), 0))
				} else if (amount.equalsIgnoreCase('no')) {
					amounts.add(new ParseAmount(new BigDecimal(0, mc), 0))
				} else {
					amounts.add(new ParseAmount(new BigDecimal(amount, mc), DEFAULT_AMOUNTPRECISION)) 
				}
			}
			
			scanner.skipWhite()

			boolean foundUnits = false
			if (!scanner.matchField(timePattern, timeClosure)) {
				scanner.matchField(unitsPattern) {
					String unitsStr = scanner.group(1)
					((ParseAmount)amounts[amountsIndex++]).setUnits(unitsStr)
					if (amountsIndex < amounts.size())
						((ParseAmount)amounts[amountsIndex++]).setUnits(unitsStr)
					foundUnits = true
				}
			}
		}

		scanner.skipWhite()

		scanner.matchField(timePattern, timeClosure)

		LinkedList<String> words = new LinkedList<String>()

		boolean endTag = false
		boolean tagAmountSeparator = false

		while (scanner.match(tagWordPattern)) {
			words.add(scanner.group(1))

			scanner.matchWhite()
			if (scanner.match(tagAmountSeparatorPattern)) {
				endTag = true
				tagAmountSeparator = true
			}

			// allow time to appear before amount
			if (scanner.match(timePattern, timeClosure)) {
				scanner.skipWhite()
				if (!endTag) { // tag separator can be after time
					scanner.match(tagAmountSeparatorPattern) { tagAmountSeparator = true }
				}
				endTag = true
			}

			// allow multiple amounts and units
			while (scanner.match(amountPattern, amountClosure)) {
				// if no units, break, otherwise attempt to match another amount/units pair
				if (((ParseAmount)amounts[amounts.size() - 1]).getUnits() == null)
					break
			}

			if (endTag)
				break;
		}
		
		parseTagSuffix(words, retVal)
		
		retVal['description'] = ParseUtils.implode(words)
		
		if (amounts.size() == 0) {
			amounts.add(new ParseAmount(new BigDecimal(1, mc), -1))
		}

		if (!foundTime) {
			scanner.matchField(timePattern, timeClosure)
		}

		LinkedList<String> commentWords = new LinkedList<String>()

		while (scanner.match(commentWordPattern)) {
			String word = scanner.group(1)
			commentWords.add(scanner.group(1))
			scanner.matchWhite()

			if (scanner.match(timeEndPattern, timeClosure))
				break;
		}
		
		parseSuffix(commentWords, retVal)

		if (commentWords.size() > 0) {
			prependComment(retVal, ParseUtils.implode(commentWords))
			
			log.debug "comment " + retVal['comment']
		}		

		if (!foundTime) {
			if (date != null) {
				date = time;
				if (!(defaultToNow && today && (!forUpdate))) { // only default to now if entry is for today and not editing
					date = new Date(baseDate.getTime() + HALFDAYTICKS);
					log.debug "SETTING DATE TO " + Utils.dateToGMTString(date)
					retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				} else {
					if (!isSameDay(time, baseDate)) {
						retVal['status'] = "Entering event for today, not displayed";
					}
				}
			}
		} else if (date != null) {
			if (hours == null && date != null) {
				date = time;
				if ((!defaultToNow) || forUpdate) {
					date = new Date(date.getTime() + 12 * 3600000L);
					retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				} else if (!isSameDay(time, baseDate)) {
					retVal['status'] = "Entering event for today, not displayed";
				}
			} else {
				long ts = date.getTime();
				if (hours >= 0 && hours < 24) {
					ts += hours * 3600000L;
				}
				if (minutes == null) minutes = 0;
				if (minutes >=0 && minutes < 60) {
					ts += minutes * 60000L;
				}
				date = new Date(ts);
			}
		} else {
			date = null;
		}

		for (int i = 0; i < units.size(); ++i) {
			if (units[i]?.equals("at"))
				units[i] = ''
			if (units[i]?.length > MAXUNITSLENGTH) {
				units[i] = units[i].substring(0, MAXUNITSLENGTH)
			}
		}
		
		// if comment is duration modifier, append to tag name
		// only do this on exact match, to avoid comments being accidentally interpreted as duration modifiers

		if (retVal['description']?.length() > Tag.MAXLENGTH) {
			def desc = retVal['description']
			def lastSpace = desc.lastIndexOf(' ', Tag.MAXLENGTH - 1)
			retVal['description'] = desc.substring(0, lastSpace)
			String comment = retVal['comment']
			if (comment.length() > 0) {
				if (comment.charAt(0) == '(') {
					if (comment.charAt(comment.length() - 1) == ')') {
						retVal['comment'] = comment.substring(1, comment.length() - 2)
					} else {
						retVal['comment'] = comment.substring(1)
					}
				}
			}
			retVal['comment'] = '(' + (retVal['comment'] ? retVal['comment'] + ' ' : '') + desc.substring(lastSpace + 1) + ')'
		}

		if (retVal['comment']?.length() > MAXCOMMENTLENGTH) {
			retVal['comment'] = retVal['comment'].substring(0, MAXCOMMENTLENGTH)
		}

		if (retVal['setName']?.length() > MAXSETNAMELENGTH) {
			retVal['setName'] = retVal['setName'].substring(0, MAXSETNAMELENGTH)
		}

		log.debug("retVal: parse " + retVal)

		if (retVal['repeatType'] != null) {
			if (!retVal['repeatType'].isReminder()) {
				if (retVal['repeatType'].isContinuous()) { // continuous repeat are always vague date precision
					date = new Date(baseDate.getTime() + HALFDAYTICKS);
					retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				}
			}
		}

		if (today && (!forUpdate)) {
			// if base date is today and time is greater than one hour from now, assume
			// user meant yesterday, unless the element is a ghost
			if (retVal['repeatType'] == null || (!retVal['repeatType'].isRepeat())) {
				if (date.getTime() > time.getTime() + HOURTICKS) {
					// if am/pm is not specified, then assume user meant half a day ago
					if (!foundAMPM) {
						date = new Date(date.getTime() - HALFDAYTICKS)
						retVal['status'] = "Entering event a half day ago";
					} else {
						date = new Date(date.getTime() - DAYTICKS);
						retVal['status'] = "Entering event for yesterday";
					}
				} else
				// if base date is today and AM/PM isn't specified and time is more than 12 hours ago,
				// enter as just 12 hours ago
				if ((!foundAMPM) && (time.getTime() - date.getTime() > HALFDAYTICKS)) {
					date = new Date(date.getTime() + HALFDAYTICKS)
				}
			}
		}

		retVal['date'] = date

		if (ghostAmount) {
			if (retVal['repeatType'] == null)
				retVal['repeatType'] = RepeatType.GHOST
			else
				retVal['repeatType'] = retVal['repeatType'].makeGhost()
		}
		
		retVal['amounts'] = amounts

		return retVal
	}
	
	def Integer fetchDatePrecisionSecs() {
		return datePrecisionSecs == null ? 60 : datePrecisionSecs;
	}

	def Integer fetchAmountPrecision() {
		return amountPrecision == null ? DEFAULT_AMOUNTPRECISION : amountPrecision;
	}

	def getDescription() {
		return tag.getDescription()
	}

	static DateFormat gmtFormat
	static long GMTMIDNIGHTSECS

	static {
		gmtFormat = new SimpleDateFormat("k:mm")
		gmtFormat.setTimeZone(Utils.createTimeZone(0, "GMT", false))
		GMTMIDNIGHTSECS = new SimpleDateFormat("yyyy-MM-dd h:mm a z").parse("2010-01-01 12:00 AM GMT").getTime() / 1000
	}

	def String exportString() {
		return description + " " + (amount == null ? "none " : amountPrecision == 0 ? (amount ? "yes " : "no ") : (amountPrecision > 0 ? amount + " " : '')) + (units ? units + ' ' : '') + (datePrecisionSecs < 40000 ? (date ? gmtFormat.format(date) : '') + ' ' : '') + comment
	}

	private def updateDescription(d) {
		tag = Tag.look(d)
	}

	/**
	 * Scan existing entries and create duration entries if they don't exist
	 * @param user
	 */
	public static void updateDurationEntries(User user) {
		// process start/end tags first
		log.debug "Entry.updateDurationEntries() this:" + this + ", userId:" + user.getId()

		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", user.getId())
				not { isNull("date") }
				eq("durationType", DurationType.END)
			}
			order("date", "asc")
		}

		for (Entry entry in results)
			entry.updateDurationEntry()
	}

	/**
	 * Scan existing entries and create duration entries if they don't exist
	 * @param user
	 */
	public static void initializeDurationEntries(User user) {
		// process start/end tags first
		log.debug "Entry.initializeDurationEntries() this:" + this + ", userId:" + user.getId()

		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", user.getId())
				not { isNull("date") }
			}
			order("date", "asc")
		}

		for (Entry entry in results) {
			entry.initializeDurations()
		}
	}

	/**
	 * Get data for the main entries lists
	 */
	def getJSONDesc() {
		return [id:this.id,
			userId:userId,
			date:date,
			datePrecisionSecs:fetchDatePrecisionSecs(),
			timeZoneName:this.fetchTimeZoneName(),
			description:tag.getDescription(),
			amount:amount,
			amountPrecision:fetchAmountPrecision(),
			units:units,
			comment:comment,
			repeatType:repeatType != null ? repeatType.getId() : 0L]
	}

	/**
	 * Get minimal data
	 */
	private static final int SHORT_DESC_DATE = 0
	private static final int SHORT_DESC_AMOUNT = 1
	private static final int SHORT_DESC_DESCRIPTION = 2

	def getJSONShortDesc() {
		return [
			date,
			amount,
			getDescription()
		];
	}

	def getDateString() {
		return Utils.dateToGMTString(date)
	}

	String toString() {
		return "Entry(id: ${id ?: 'un-saved'}, userId:" + userId \
				+ ", date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneName:" + TimeZoneId.fromId(timeZoneId).getName() \
				+ ", description:" + getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatType?.getId() \
				+ ", repeatEnd:" + Utils.dateToGMTString(repeatEnd) \
				+ ", setName:" + setIdentifier?.getValue() \
				+ ")"
	}

	def String valueString() {
		return "Entry(userId:" + userId \
				+ ", date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneName:" + TimeZoneId.fromId(timeZoneId).getName() \
				+ ", description:" + getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatType?.getId() \
				+ ", repeatEnd:" + Utils.dateToGMTString(repeatEnd) \
				+ ")"
	}

	def String contentString() {
		return "Entry(date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneName:" + TimeZoneId.fromId(timeZoneId).getName() \
				+ ", description:" + getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatType?.getId() \
				+ ", repeatEnd:" + Utils.dateToGMTString(repeatEnd) \
				+ ")"
	}

	boolean equals(Object o) {
		if (!o instanceof Entry)
			return false
		return ((Entry)o).getId() == id
	}
}

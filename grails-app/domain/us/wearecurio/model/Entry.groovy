package us.wearecurio.model

import groovy.time.*
import grails.compiler.GrailsCompileStatic
import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.collection.SingleCollection
import us.wearecurio.datetime.LocalTimeRepeater
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.EntryParserService.ParseAmount
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.utility.Utils
import us.wearecurio.model.Tag
import us.wearecurio.model.EntryGroup
import us.wearecurio.data.RepeatType
import us.wearecurio.data.TagUnitStatsInterface
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.data.UnitGroupMap.UnitGroup
import us.wearecurio.data.UnitGroupMap.UnitRatio
import us.wearecurio.data.DataRetriever

import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.ConcurrentHashMap
import java.util.LinkedList
import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalTime
import org.joda.time.LocalDate

class Entry implements Comparable {
	
	private static def log = LogFactory.getLog(this)

	protected static final int DEFAULT_DATEPRECISION_SECS = 3 * 60
	protected static final int VAGUE_DATE_PRECISION_SECS = 86400
	
	static final MathContext mc = new MathContext(9)

	protected static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.999999999")
	protected static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999.999999999")
	
	protected static final int MAXCOMMENTLENGTH = 100000
	protected static final int MAXUNITSLENGTH = 50
	protected static final int MAXSETNAMELENGTH = 50
	protected static final long HOURTICKS = 60L * 60000
	protected static final long DAYTICKS = HOURTICKS * 24
	protected static final long HALFDAYTICKS = HOURTICKS * 12
	protected static final long WEEKTICKS = DAYTICKS * 7
	
	static constraints = {
		group(nullable:true)
		amount(scale:9, max:9999999999999999999.999999999, nullable:true)
		date(nullable:true)
		datePrecisionSecs(nullable:true)
		amountPrecision(nullable:true)
		repeatEnd(nullable:true)
		units(maxSize:MAXUNITSLENGTH)
		comment(maxSize:MAXCOMMENTLENGTH)
		baseTag(nullable:true)
		durationType(nullable:true)
		timeZoneId(nullable:true)
		setIdentifier(nullable:true)
		userId(nullable:true)
		repeatTypeId(nullable:true)
	}

	static mapping = {
		version false
		table 'entry'
		group column: 'group_id', index:'group_id_index'
		userId column:'user_id', index:'user_id_index'
		date column:'date', index:'date_index'
		tag column:'tag_id', index:'tag_id_index'
		repeatEnd column:'repeat_end', index:'repeat_end_index'
		durationType column:'duration_type', index:'duration_type_index'
		setIdentifier column:'set_identifier', index:'set_identifier_index'
	}
	
	static belongsTo = [ group : EntryGroup ]

	Long userId
	Date date
	Integer timeZoneId
	Integer datePrecisionSecs
	Tag tag
	BigDecimal amount
	Integer amountPrecision
	String units
	String comment
	Long repeatTypeId
	Date repeatEnd // the end of the repeat cycle, inclusive
	Tag baseTag
	DurationType durationType
	Identifier setIdentifier
	
	static transients = [ 'repeatType' ]
	
	RepeatType getRepeatType() { repeatTypeId == null ? null : RepeatType.look(repeatTypeId) }
	
	void setRepeatType(RepeatType repeatType) {
		this.repeatTypeId = repeatType?.id
	}
	
	boolean isGhost() {
		if (repeatTypeId == null) return false
		return getRepeatType().isGhost()
	}
	
	String fetchSuffix() { 
		if (baseTag == null || baseTag == tag) {
			return ""
		}
		
		return tag.getDescription().substring(baseTag.getDescription().length() + 1)
	}
	
	Iterable<Entry> fetchGroupEntries() {
		if (group != null)
			return new TreeSet<Entry>(group.fetchSortedEntries())
		
		return new SingleCollection(this)
	}

	static final int DEFAULT_AMOUNTPRECISION = 3

	/**
	* Create and save new entry
	*/
	static Entry createArgs(EntryCreateMap creationMap, EntryStats stats, Long userId, Date date, TimeZoneId timeZoneId, String description, BigDecimal amount, String units,
			String comment, String setName, int amountPrecision = DEFAULT_AMOUNTPRECISION) {
		
		Map<String,Object> m = [
			'date':date,
			'timeZoneId':(Integer) timeZoneId.id,
			'timeZoneName': timeZoneId.name,
			'datePrecisionSecs':DEFAULT_DATEPRECISION_SECS,
			'amount':amount,
			'units':units,
			'comment':comment,
			'setName':setName,
			'amountPrecision': amountPrecision,
		]
		
		m['baseTag'] = Tag.look(description.toLowerCase())
		m['tag'] = EntryParserService.get().tagWithSuffixForUnits(m['baseTag'], units, 0)
		
		Entry e = createSingle(userId, m, creationMap.groupForDate(date), stats)
		creationMap.add(e)
		
		return e
	}

	static Entry updatePartialOrCreate(Long userId, Map m, EntryGroup group, EntryStats stats) {
		log.debug("Entry.updatePartialOrCreate: Trying to find a partial entry to update")
		def partialEntry = lookForPartialEntry(userId, m)
		log.debug("Entry.updatePartialOrCreate: " + partialEntry.size() +" partial entries found")
		if (partialEntry.size() < 1) {
			return Entry.createSingle(userId, m, group, stats)
		}

		for (entry in partialEntry) {
			log.debug ("Updating partial entry " + entry)
			entry.amount = m.amount
			Utils.save(entry, true)
			return entry
		}
	}

	protected static List lookForPartialEntry(userId, Map m) {
		/**
		* Making sure it is a summary entry. Non summary entries will
		* not have partial entries
		*/

		if (!m['tag'].getDescription().endsWith(" summary")) {
			return []
		}

		Tag tag = m.tag
		TimeZoneId timeZoneId
		if (!m.timeZoneId) {
			timeZoneId = TimeZoneId.look(m.timeZoneName)
		} else
			timeZoneId = TimeZoneId.fromId(m.timeZoneId)
		Integer timeZoneIdId = (Integer) timeZoneId.id
		DateTimeZone userTimezone = timeZoneId.toDateTimeZone()
			
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
				eq("timeZoneId", timeZoneIdId)
				between("date", startOfDay.toDate(), endOfDay.toDate())
			}
		}

		return results
	}

	protected static final BigDecimal smallDifference = new BigDecimal("0.01")

	// Prevent duplicates for imported entries
	protected static boolean hasDuplicate(Long userId, Map m) {
		if (m.setName == null)
			return false

		log.debug "Trying to find duplicate for " + userId + " for " + m

		Identifier setId = Identifier.look(m.setName)
		
		RepeatType repeatType = (RepeatType) m['repeatType']
		
		if (repeatType != null && repeatType.isContinuous()) {
			def c = Entry.createCriteria()
			def results = c {
				and {
					eq("userId", userId)
					eq("tag", m.tag)
					eq("amount", m.amount)
					eq("amountPrecision", m.amountPrecision == null ? 3 : m.amountPrecision)
					eq("units", m.units)
					eq("repeatTypeId", repeatType?.id)
				}
			}
	
			for (r in results) {
				log.debug ("Found duplicate " + r)
				return true
			}
		} else {
			def c = Entry.createCriteria()
			def results = c {
				and {
					eq("userId", userId)
					eq("date", m.date)
					eq("tag", m.tag)
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
		}
		
		log.debug "No duplicate found"

		return false
	}
	
	protected static void checkAmount(Map m) {
		BigDecimal amount = m['amount']
		
		if (amount) {
			if (amount.compareTo(MAX_AMOUNT) > 0) {
				amount = MAX_AMOUNT
				if (!m['comment'].endsWith("(amount too large to store)"))
					m['comment'] += "(amount too large to store)"
			} else if (amount.compareTo(MIN_AMOUNT) < 0) {
				amount = MIN_AMOUNT
				if (!m['comment'].endsWith("(amount too small to store)"))
					m['comment'] += "(amount too small to store)"
			}
		}
		
		m['amount'] = amount
	}

	protected static Entry createSingle(Long userId, Map m, EntryGroup group, EntryStats stats) {
		log.debug "Entry.createSingle() userId:" + userId + ", m:" + m

		if ((!stats.getBatchCreation()) && hasDuplicate(userId, m)) {
			log.debug "Entry map is a duplicate, do not add: " + m
			return null
		}
		
		stats.setUserId(userId)

		Tag baseTag = m['baseTag']
		DurationType durationType = m['durationType']
		
		if (durationType != null && ((RepeatType)m['repeatType'])?.isGhost())
			durationType = durationType.makeGhost()
			
		Tag tag = m['tag']
		
		TimeZoneId timeZoneId
		if (m['timeZoneId']) timeZoneId = TimeZoneId.fromId(m['timeZoneId'])
		else timeZoneId = TimeZoneId.look(m['timeZoneName'])

		checkAmount(m)
		
		if (m['units'] == null)
			m['units'] = ''
		
		TagUnitStatsInterface tagUnitStats
		if ((!m['units']) && (m['amountPrecision'] > 0)) {
			// Using the most used unit in case the unit is unknown, if the amountPrecision is > 0
			tagUnitStats = TagUnitStats.mostUsedTagUnitStats(userId, tag.getId())
			if (tagUnitStats) {
				m['units'] = tagUnitStats.getUnitGroup()?.lookupUnitString(tagUnitStats.unit, (m['amount'].compareTo(BigDecimal.ONE) != 0))
			}
		} else {
			TagUnitStats.createOrUpdate(userId, tag.getId(), m['units'])
			TagUnitStats.createOrUpdate(userId, baseTag.getId(), m['units'])
		}
		
		if (m['repeatType'] != null && m['repeatEnd'] != null) {
			m['repeatEnd'] = ((RepeatType)m['repeatType']).makeRepeatEnd(m['repeatEnd'], m['date'], timeZoneId)
		}
		
		Entry entry = new Entry(
				userId:userId,
				group:group,
				date:m['date'],
				timeZoneId:timeZoneId.getId(),
				datePrecisionSecs:m['datePrecisionSecs'] == null ? DEFAULT_DATEPRECISION_SECS : m['datePrecisionSecs'],
				tag:tag,
				amount:m['amount'],
				repeatTypeId: m['repeatType']?.id,
				repeatEnd: m['repeatEnd'],
				baseTag:baseTag,
				durationType:durationType,
				setIdentifier:Identifier.look(m['setName']),
				amountPrecision:m['amountPrecision']==null?3:m['amountPrecision']
				)

		entry.setUnits(m['units']?:'')
		entry.setComment(m['comment']?:'')

		log.debug "Created entry:" + entry
		
		if (group != null) {
			group.add(entry)
		}

		entry.processAndSave(stats)
		
		entry.createRepeat(stats)

		Entry generator = entry.fetchGeneratorEntry()

		if (generator != null)
			generator.updateDurationEntry(stats) // make sure duration entries remain consistent

		if (tag.hasEntry(userId)) {
			User.addToCache(userId, tag)
		}
		stats.addEntry(entry)
		stats.setTimeZoneId(entry.getTimeZoneId())

		return entry
	}
	
	/**
	 * Create Entry
	 * 
	 * EntryStats are passed in to handle batch jobs, so entry statistics are updated all at once at the end of
	 * the entry creation sequence, rather than on each entry creation
	 */
	static Entry create(Long userId, Map m, EntryStats stats) {
		log.debug "Entry.create() userId:" + userId + ", m:" + m

		if (m == null) return null
		
		if (m['tag'] == null) return null

		def amounts = m['amounts']
		
		EntryGroup entryGroup = null
		
		if (amounts.size() > 1) {
			entryGroup = new EntryGroup()
		}
		
		int index = 0
		
		Entry e = null
		for (ParseAmount amount : amounts) {
			if (!amount.isActive()) continue
			amount.copyToMap(m)
			e = createSingle(userId, m, entryGroup, stats)
		}
		if (entryGroup != null) {
			Utils.save(entryGroup, true)
			return entryGroup.fetchSortedEntries().first() // return first entry in sorted list
		}
		
		return e
	}
	
	protected boolean ifDurationRemoveFromGroup() {
		if (durationType != null && durationType.isStartOrEnd()) {
			if (group != null) {
				group.remove(this)
			}
			return true
		}

		return false
	}
	
	protected EntryGroup createOrFetchGroup() {
		if (ifDurationRemoveFromGroup()) {
			return null
		}
		
		if (group != null) return group
		
		group = new EntryGroup()
		group.add(this)
		
		return group
	}

	protected Date fetchPreviousDate(int minusDays = 1) {
		DateTime dateTime = fetchDateTime()
		DateTimeZone dateTimeZone = dateTime.getZone()
		LocalTime localTime = dateTime.toLocalTime()
		LocalDate newLocalDate = dateTime.toLocalDate().minusDays(minusDays)
		return newLocalDate.toDateTime(localTime, dateTimeZone).toDate()
	}

	/**
	 * check to see if parameters match current entry, so repeat doesn't need to update
	 */
	protected boolean repeatParametersMatch(Map m) {
		log.debug "Entry.repeatParametersMatch() m:" + m + " for " + toString()

		if (m == null) return false

		if (m['tag'] == null) return false

		if (this.tag.getId() != m['tag'].getId())
			return false

		DateTime entryDateTime = fetchDateTime()
		LocalTime localTime = entryDateTime.toLocalTime()
		DateTimeZone origTimeZone = entryDateTime.getZone()
		DateTime newDateTime = new DateTime(m['date'], origTimeZone)

		if (!localTime.equals(newDateTime.toLocalTime()))
			return false

		return m['datePrecisionSecs'] == this.datePrecisionSecs && amount == m['amount'] \
			&& comment == (m['comment'] ?: '') && repeatTypeId == m['repeatType']?.id && units == (m['units'] ?: '') \
			&& amountPrecision == (m['amountPrecision'] ?: 3)

	}
	
	protected def entryMap() {
		def m = [:]

		m['tag'] = this.tag
		m['baseTag'] = this.baseTag
		m['units'] = this.units
		m['repeatType'] = this.repeatType
		m['date'] = this.date
		m['timeZoneName'] = this.fetchTimeZoneName()
		m['datePrecisionSecs'] = this.datePrecisionSecs
		m['amount'] = this.amount
		m['amountPrecision'] = this.amountPrecision
		m['units'] = this.units
		m['comment'] = this.comment
		m['repeatEnd'] = this.repeatEnd
		m['durationType'] = this.durationType
		m['setName'] = this.setIdentifier?.toString()

		log.debug "Creating entry map:" + m

		return m
	}

	/*
	 * Create a new repeat entry on the baseDate + addDays, if needed, and cut the current entry at the next base date
	 */
	protected Entry createRepeatOnBaseDate(Date baseDate, Map m, EntryCreateMap creationMap, EntryStats stats, int addDays) {
		if (this.repeatEnd != null && this.repeatEnd <= this.date) { // do not create repeat if no future repeats
			return null
		}

		DateTimeZone dateTimeZone = DateTimeZone.forID(m['timeZoneName'])
		LocalTime mLocalTime = new DateTime(m['date'], dateTimeZone).toLocalTime()
		LocalDate baseLocalDate = new DateTime(baseDate, dateTimeZone).toLocalDate()
		DateTime repeatEndDateTime = new DateTime(this.repeatEnd, dateTimeZone)
		LocalTime repeatEndLocalTime = repeatEndDateTime.toLocalTime()
		LocalDate repeatEndLocalDate = repeatEndDateTime.toLocalDate()
		Date oneDayAfterCurrentRepeatEnd = repeatEndLocalDate.plusDays(1).toDateTime(repeatEndLocalTime, dateTimeZone).toDate()

		if (addDays > 0) baseLocalDate = baseLocalDate.plusDays(addDays)

		Date newRepeatEntryDateOnThisBaseDate = baseLocalDate.toDateTime(mLocalTime, dateTimeZone).toDate()

		// compute previous repeatEnd time by keeping original local time but subtracting a day
		DateTime currentDateTime = fetchDateTime()
		LocalTime currentLocalTime = currentDateTime.toLocalTime()
		DateTimeZone currentDateTimeZone = fetchDateTimeZone()
		Date prevDate = baseLocalDate.minusDays(2).toDateTime(currentLocalTime, currentDateTimeZone).toDate()
		
		while (newRepeatEntryDateOnThisBaseDate.getTime() - prevDate.getTime() > DAYTICKS)
			prevDate = prevDate + 1

		// if no repeat end or the new repeat entry date is earlier than one day after the current repeat end, create new repeat entry
		// and change the end of this repeat entry.
		if (this.repeatEnd == null || newRepeatEntryDateOnThisBaseDate < oneDayAfterCurrentRepeatEnd) {
			m['date'] = newRepeatEntryDateOnThisBaseDate
			m['repeatEnd'] = this.repeatEnd

			def newEntry = createSingle(this.userId, m, creationMap.groupForDate(newRepeatEntryDateOnThisBaseDate), stats)
			
			creationMap.add(newEntry)

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
	
	protected static Entry updateGhostSingle(Entry entry, Map m, EntryCreateMap creationMap, EntryStats stats, Date baseDate, boolean allFuture, boolean unGhost) {
		boolean isToday = EntryParserService.isToday(entry.getDate(), baseDate)

		if (entry.repeatParametersMatch(m) && ((!entry.isGhost()) || (!unGhost)))
			return entry // no need to update when entry doesn't change

		if (isToday) {
			if (allFuture) {
				entry.doUpdate(m, stats)

				if (entry.isGhost() && unGhost) {
					// create an entry a day after the baseDate with the new entry data
					entry.createRepeatOnBaseDate(baseDate, entry.entryMap(), creationMap, stats, 1)
					entry.unGhost(stats)
				}				
				return entry // allFuture updates can just edit the entry and return if the entry is today
			} else {
				// create an entry a day after the baseDate with the old entry data
				entry.createRepeatOnBaseDate(baseDate, entry.entryMap(), creationMap, stats, 1)

				if (entry.getRepeatEnd() == entry.getDate()) {
					m['repeatEnd'] = m['date']
				} else
					m['repeatEnd'] = entry.getRepeatEnd()

				entry.doUpdate(m, stats)
				
				if (unGhost) entry.unGhost(stats)

				return entry
			}
		} else {
			if ((!allFuture) || (unGhost && entry.isGhost())) { // create another entry after the base date
				entry.createRepeatOnBaseDate(baseDate, entry.entryMap(), creationMap, stats, 1)
			}

			Entry newEntry = entry.createRepeatOnBaseDate(baseDate, m, creationMap, stats, 0)
			
			if (unGhost && newEntry.isGhost())
				newEntry.unGhost()
			
			return newEntry
		}
	}

	/**
	 * Update entry, possibly updating multiple entries, given map of parameters (output of the parse method or manually created)
	 */
	protected static Entry updateSingle(Entry entry, Map m, EntryCreateMap creationMap, EntryStats stats, Date baseDate, boolean allFuture = true, boolean unGhost = true) {
		log.debug "Entry.updateSingle() entry:" + entry + ", m:" + m + ", baseDate:" + baseDate + ", allFuture:" + allFuture
		RepeatType repeatType = entry.getRepeatType()
		if (repeatType != null && repeatType.isRepeat()) {
			Entry retVal = updateGhostSingle(entry, m, creationMap, stats, baseDate, allFuture, unGhost)
			
			if (retVal != null) {
				if (creationMap != null)
					creationMap.add(retVal)
				
				retVal.refreshSort()
			}
			
			return retVal
		} else {
			entry.doUpdate(m, stats)
			
			entry.refreshSort()
			
			if (creationMap != null)
				creationMap.add(entry)

			return entry
		}
	}
	
	void refreshSort() {
		if (group != null) {
			group.resort(this)
		}
	}

	/**
	 * Update entry, possibly updating multiple entries, given map of parameters (output of the parse method or manually created)
	 * 
	 * allFuture - if repeat entry, update this entry and all future entries
	 * unGhost - if ghost entry, make baseDate's entry unghosted, but future entries will still be ghosted
	 * 
	 */
	Entry update(Map m, EntryStats stats, Date baseDate, boolean allFuture = true, boolean unGhost = true) {
		log.debug "Entry.update() entry:" + this + ", m:" + m + ", baseDate:" + baseDate + ", allFuture:" + allFuture
		
		EntryCreateMap creationMap = new EntryCreateMap()
		
		Entry retVal = null
		Entry firstUpdated = null
		
		if (m == null) {
			for (Entry e : this.fetchGroupEntries()) {
				Entry updated = updateSingle(e, e.entryMap(), creationMap, stats, baseDate, allFuture, unGhost)
				if (firstUpdated == null)
					firstUpdated = updated
			}
			
			return firstUpdated
		}
		
		ArrayList<ParseAmount> amounts = new ArrayList<ParseAmount>()
		if (m != null && m['amounts'] != null) amounts.addAll(m['amounts'])
		ArrayList<ParseAmount> unmatchedEntries = new ArrayList<ParseAmount>()
		
		// first, update entries that match the amounts already specified
		for (Entry e : this.fetchGroupEntries()) {
			boolean foundMatch = false
			for (ParseAmount a : amounts) {
				if (!a.isActive()) continue
				if (a.matches(e)) {
					foundMatch = true
					amounts.remove(a)
					a.copyToMap(m)
					Entry updated = updateSingle(e, m, creationMap, stats, baseDate, allFuture, unGhost)
					if (e.is(this))
						retVal = updated
					if (firstUpdated == null)
						firstUpdated = updated
					break
				}
			}			
			if (!foundMatch) for (ParseAmount a : amounts) {
				if (!a.isActive()) continue
				if (a.matchesWeak(e)) {
					foundMatch = true
					amounts.remove(a)
					a.copyToMap(m)
					Entry updated = updateSingle(e, m, creationMap, stats, baseDate, allFuture, unGhost)
					if (e.is(this))
						retVal = updated
					if (firstUpdated == null)
						firstUpdated = updated
					break
				}
			}
			if (!foundMatch) {
				unmatchedEntries.add(e)
			}
		}
		// next, update remaining entries
		while (unmatchedEntries.size() > 0) {
			Entry e = unmatchedEntries.remove(0)
			ParseAmount a = null
			while (amounts.size() > 0) {
				a = amounts.remove(0)
				if (!a.isActive()) {
					a = null
					continue
				} else
					break
			}
			if (a != null) {
				a.copyToMap(m)
				Entry updated = updateSingle(e, m, creationMap, stats, baseDate, allFuture, unGhost)
				if (e.is(this))
					retVal = updated
				if (firstUpdated == null)
					firstUpdated = updated
			} else { // no more amounts, delete remaining entries
				deleteSingle(e, stats)
			}
		}
		// finally, create new entries in group for remaining amounts
		if (amounts.size() > 0) {
			for (ParseAmount a : amounts) {
				if (!a.isActive()) continue
				a.copyToMap(m)
				Entry created = createSingle(userId, m, creationMap.groupForDate(m['date']), stats)
				if (retVal == null)
					retVal = created
				if (firstUpdated == null)
					firstUpdated = created
			}
		}
		
		retVal = retVal == null ? firstUpdated : retVal
		
		return retVal
	}

	/**
	 * Delete actually just hides the entries, rather than deleting them
	 */
	protected static deleteSingle(Entry entry, EntryStats stats) {
		log.debug "Entry.deleteSingle() entry:" + entry

		Entry updateDurationEntry = entry.preDeleteProcessing(stats)

		// if this was an end entry, delete current duration entry, delete this entry, then recalculate subsequent duration entry

		long oldUserId = entry.getUserId()

		log.debug "Zeroing the user id of the entry and saving, updating the tag stats"
		entry.setUserId(0L) // don't delete entry, just zero its user id
		EntryGroup group = entry.getGroup()
		
		if (group != null) {
			group.remove(entry)
			Utils.save(group, false)
		}
		Utils.save(entry, true)
		
		stats.setUserId(oldUserId)
		
		stats.addEntry(entry)

		if (updateDurationEntry != null) {
			updateDurationEntry.updateDurationEntry(stats)
		}

		if (!entry.tag.hasEntry(oldUserId)) {
			User.removeFromCache(oldUserId, entry.tag)
		}
	}

	/**
	 * Delete actually just hides the entries, rather than deleting them
	 */
	static delete(Entry entry, EntryStats stats) {
		log.debug "Entry.delete() entry:" + entry
		
		for (Entry e : entry.fetchGroupEntries()) {
			deleteSingle(e, stats)
		}
	}

	boolean isDeleted() {
		return userId == 0L
	}

	/**
	 * Delete a ghost entry on a given baseDate and for a current date
	 * @param ghostTime
	 * @param currentDate
	 * @return
	 */
	protected static deleteGhostSingle(Entry entry, EntryCreateMap creationMap, EntryStats stats, Date baseDate, boolean allFuture) {
		log.debug "Entry.deleteGhostSingle() entry:" + entry + " baseDate: " + baseDate + " allFuture: " + allFuture

		if (entry.getRepeatType()?.isContinuous()) {
			Entry.deleteSingle(entry, stats)
			return
		}

		long entryTime = entry.getDate().getTime()
		long baseTime = baseDate.getTime()
		Date repeatEndDate = entry.getRepeatEnd()
		boolean endAfterToday = repeatEndDate == null ? true : repeatEndDate.getTime() >= (entry.getDate().getTime() + DAYTICKS - HOURTICKS)

		if (allFuture) {
			if (entryTime >= baseTime && entryTime - baseTime < DAYTICKS) { // entry is in today's data
				Entry.deleteSingle(entry, stats)
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
						Entry.deleteSingle(entry, stats)
					} else {
						entry.setDate(newDate)

						Utils.save(entry, true)
					}
				} else { // otherwise, delete entry
					Entry.deleteSingle(entry, stats)
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
						m['tag'] = entry.getTag()
						m['date'] = newDate

						Long repeatId = entry.getRepeatType().getId().longValue()
						RepeatType toggleType = entry.getRepeatType().toggleGhost()

						def repeatTypes

						if (toggleType != null)
							repeatTypes = [
								repeatId,
								entry.getRepeatType().toggleGhost().getId().longValue()
							]
						else
							repeatTypes = [ repeatId ]

						// look for entry that already exists one day later
						// new date is one day after current baseDate

						String queryStr = "from Entry entry where entry.tag.description = :desc and entry.date = :entryDate and entry.userId = :userId and entry.repeatTypeId in (:repeatIds) order by entry.date desc"
						def entries = Entry.executeQuery(queryStr, [desc:m['tag'].getDescription(), entryDate:m['date'], userId:entry.getUserId(), repeatIds:repeatTypes], [max: 1])

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
						def retVal = Entry.createSingle(entry.getUserId(), m, creationMap.groupForDate(m['date']), stats)
						creationMap.add(retVal)
					}
				}
			} else {
				log.error("Invalid call for entry: " + entry + " for baseDate " + baseDate)
			}
		}
	}

	/**
	 * Delete a ghost entry on a given baseDate and for a current date
	 * @param ghostTime
	 * @param currentDate
	 * @return
	 */
	static deleteGhost(Entry entry, EntryStats stats, Date baseDate, boolean allFuture) {
		log.debug "Entry.deleteGhost() entry:" + entry + " baseDate: " + baseDate + " allFuture: " + allFuture
		
		EntryCreateMap creationMap = new EntryCreateMap()
		
		for (Entry e : entry.fetchGroupEntries()) {
			deleteGhostSingle(e, creationMap, stats, baseDate, allFuture)
		}
	}

	// look for any duration entry that needs to be updated after a delete
	protected Entry preDeleteProcessing(EntryStats stats) {
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
			if (duration != null) delete(duration, stats)
			def next = fetchNextStartOrEndEntry()
			if (next != null && next.fetchIsEnd())
				return next
		}

		return null
	}

	/**
	* Retrieve tags by user
	*/
	static final int BYALPHA = 0
	static final int BYCOUNT = 1
	static final int BYRECENT = 2
	static final int ONLYIDS = 3
	static final int BYCOUNTONLYDESCRIPTION = 4

	protected static def countSeries(userId, tagId, amountPrecision) {
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

	static def getBaseTags(User user, type = BYALPHA) {
		log.debug "Entry.getBaseTags() userID:" + user.getId() + ", type:" + type

		def retVal

		DatabaseService databaseService = DatabaseService.get()

		if (type == BYCOUNT) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.base_tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = :userId and e.date is not null group by t.id order by count(e.id) desc", [userId:user.getId()])
		} /*else if (type == BYRECENT) {
		 retVal = Entry.executeQuery("select entry.tag.description, max(entry.date) as maxdate from Entry as entry left join TagProperties prop on prop.userId = entry.userId and prop.tagId = entry.tag.id where entry.userId = :userId and entry.date is not null " + (recentOnly ? "and datediff(now(), entry.date) < 90 " : "") + "group by entry.tag.id order by max(entry.date) desc", [userId:user.getId()])
		 } */ else if (type == BYALPHA) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.base_tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = :userId and e.date is not null group by t.id order by t.description", [userId:user.getId()])
		} else if (type == ONLYIDS) {
			retVal = Entry.executeQuery("select entry.baseTag.id from Entry as entry where entry.userId = :userId and entry.date is not null and entry.baseTag is not null group by entry.baseTag.id", [userId:user.getId()])
		} else if (type == BYCOUNTONLYDESCRIPTION) {
			retVal = Entry.executeQuery("select entry.baseTag.description from Entry as entry where entry.userId = :userId and entry.date is not null and entry.baseTag is not null group by entry.baseTag.id order by count(entry.id) desc", [userId:user.getId()])
		}

		//log.debug "Returned:" + retVal

		return retVal
	}

	static def getTags(User user, type = BYALPHA) {
		log.debug "Entry.getTags() userID:" + user.getId() + ", type:" + type

		def retVal

		DatabaseService databaseService = DatabaseService.get()

		if (type == BYCOUNT) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = :userId and e.date is not null group by t.id order by count(e.id) desc", [userId:user.getId()])
		} /*else if (type == BYRECENT) {
		 retVal = Entry.executeQuery("select entry.tag.description, max(entry.date) as maxdate from Entry as entry left join TagProperties prop on prop.userId = entry.userId and prop.tagId = entry.tag.id where entry.userId = :userId and entry.date is not null " + (recentOnly ? "and datediff(now(), entry.date) < 90 " : "") + "group by entry.tag.id order by max(entry.date) desc", [userId:user.getId()])
		 } */ else if (type == BYALPHA) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = :userId and e.date is not null group by t.id order by t.description", [userId:user.getId()])
		} else if (type == ONLYIDS) {
			retVal = Entry.executeQuery("select entry.tag.id from Entry as entry where entry.userId = :userId and entry.date is not null group by entry.tag.id", [userId:user.getId()])
		} else if (type == BYCOUNTONLYDESCRIPTION) {
			retVal = Entry.executeQuery("select entry.tag.description from Entry as entry where entry.userId = :userId and entry.date is not null group by entry.tag.id order by count(entry.id) desc", [userId:user.getId()])
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
		log.debug("Entry.getAutocompleteTagsWithInfo() userID:" + user.id)

		def tagStats = TagStats.activeForUser(user)

		def freqTagStats = tagStats['freq']
		def algTagStats = tagStats['alg']

		def allTagStats = []
		
		for (TagStats tag in freqTagStats) {
			if (tag.description.startsWith("headache"))
				tag = tag

			allTagStats.add([
				tag.getDescription(),
				tag.getLastAmount(),
				tag.getLastAmountPrecision(),
				tag.getLastUnits()
			])
		}

		def freqTags = []
		def algTags = []

		for (TagStats tag in freqTagStats) {
			freqTags.add(tag.description)
		}

		for (TagStats tag in algTagStats) {
			algTags.add(tag.description)
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
	protected boolean fetchIsGenerated() {
		return durationType?.isGenerated()
	}

	protected boolean oldFetchIsGenerated() {
		return oldFetchGeneratorEntry() ? true : false
	}

	static def START_DURATIONS = [
		DurationType.START, DurationType.GHOSTSTART, DurationType.GENERATEDSTART, DurationType.GENERATEDSPRINTSTART
	]
	
	static def END_DURATIONS = [
		DurationType.END, DurationType.GHOSTEND, DurationType.GENERATEDEND, DurationType.GENERATEDSPRINTEND
	]
	
	static def STARTOREND_DURATIONS = [
		DurationType.START, DurationType.GHOSTSTART, DurationType.GENERATEDSTART, DurationType.GENERATEDSPRINTSTART,
		DurationType.END, DurationType.GHOSTEND, DurationType.GENERATEDEND, DurationType.GENERATEDSPRINTEND
	]
	
	/**
	 * fetchGeneratorEntry()
	 *
	 * Returns end entry corresponding to this generated entry if it is a generated entry
	 */
	protected Entry fetchGeneratorEntry() {
		if (durationType != DurationType.GENERATEDDURATION) return null

		def c = Entry.createCriteria()

		def results = c {
			eq("userId", userId)
			eq("date", date)
			eq("baseTag", tag)
			'in'("durationType", START_DURATIONS)
		}

		for (Entry r in results) {
			if (r.getId() != this.id) return r
		}
		return null
	}

	protected Entry oldFetchGeneratorEntry() {
		if (!durationType.equals(DurationType.NONE)) return null

		if (units && units.equals('hours')) {
			def c = Entry.createCriteria()

			def results = c {
				eq("userId", userId)
				eq("date", date)
				eq("baseTag", tag)
				'in'("durationType", END_DURATIONS)
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
		return durationType?.isEnd()
	}

	/**
	 * fetchIsStart()
	 *
	 * Returns true if this entry is the start of a duration pair
	 */
	boolean fetchIsStart() {
		return durationType?.isStart()
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
				eq("durationType", DurationType.GENERATEDDURATION)
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
				eq("durationType", DurationType.GENERATEDDURATION)
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
				eq("durationType", DurationType.GENERATEDDURATION)
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
	
	static Entry fetchPreviousEqualStartOrEndEntry(Long userId, Tag baseTag, Date date) {
		def c = Entry.createCriteria()
		
		// look for latest matching entry prior to this one
		
		def results = c {
			eq("userId", userId)
			le("date", date)
			eq("baseTag", baseTag)
			'in'("durationType", STARTOREND_DURATIONS)
			maxResults(1)
			order("date", "desc")
		}

		return results[0]
	}

	static Entry fetchPreviousStartOrEndEntry(Long userId, Tag baseTag, Date date) {
		def c = Entry.createCriteria()
		
		// look for latest matching entry prior to this one

		def results = c {
			eq("userId", userId)
			lt("date", date)
			eq("baseTag", baseTag)
			'in'("durationType", STARTOREND_DURATIONS)
			maxResults(1)
			order("date", "desc")
		}

		return results[0]
	}

	protected Entry fetchPreviousStartOrEndEntry() {
		return fetchPreviousStartOrEndEntry(userId, baseTag, date)
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
			'in'("durationType", STARTOREND_DURATIONS)
			maxResults(1)
			order("date", "asc")
		}

		if (results[0] != null)
			return results[0]

		return null
	}

	protected Entry updateDurationEntry(EntryStats stats) {
		if (!fetchIsEnd()) return null

		Long duration = calculateDuration()

		def c = Entry.createCriteria()

		def results = c {
			eq("userId", userId)
			eq("date", date)
			eq("tag", baseTag)
			eq("units", "hours")
			or {
				eq("durationType", DurationType.GENERATEDDURATION)
				isNull("durationType")
			}
		}

		Entry durationEntry = null
		Entry lastDurationEntry = null

		for (Entry entry in results) {
			if (lastDurationEntry != null) {
				Entry.delete(entry, stats)
			} else
				durationEntry = entry
			lastDurationEntry = entry
		}

		if (durationEntry == null)
			durationEntry = findDurationEntry()

		if (duration == null) {
			while (durationEntry != null) {
				Entry.delete(durationEntry, stats)
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
				m['tag'] = baseTag
				m['amount'] = amount
				m['units'] = "hours"
				m['comment'] = this.comment ?: ""
				m['repeatType'] = null
				m['baseTag'] = baseTag
				m['durationType'] = DurationType.GENERATEDDURATION
				m['amountPrecision'] = DEFAULT_AMOUNTPRECISION
				durationEntry = Entry.createSingle(userId, m, createOrFetchGroup(), stats)
			} else {
				DatabaseService.retry(durationEntry) {
					durationEntry.setAmount(amount)
					Utils.save(durationEntry, true)
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
	protected def processAndSave(EntryStats stats) {
		try {
			Utils.save(this, true)
		} catch (Throwable t) {
			System.err.println("Invalid entry, failed to save: " + this)
			t.printStackTrace()
			return
		}

		if (fetchIsEnd()) {
			updateDurationEntry(stats)
			Entry subsequent = fetchNextStartOrEndEntry()
			if (subsequent != null && subsequent.fetchIsEnd()) {
				Entry subsequentDuration = subsequent.findDurationEntry()

				if (subsequentDuration != null)
					Entry.delete(subsequentDuration, stats)
			}
		}

		if (fetchIsStart()) {
			Entry subsequent = fetchNextStartOrEndEntry()
			if (subsequent != null && subsequent.fetchIsEnd()) {
				subsequent.updateDurationEntry(stats)
			}
		}
	}

	protected findNeighboringDailyRepeatEntry(String queryStr, parms) {
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

	protected findPreviousRepeatEntry() {
		return findNeighboringDailyRepeatEntry(
		"select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date < :entryDate and (time(entry.date) = time(:entryDate) or time(date_add(entry.date, interval 1 hour)) = time(:entryDate) or time(entry.date) = time(date_sub(:entryDate, interval 1 hour))) and entry.user_id = :userId and ((entry.repeat_type_id & :dailyBit) <> 0) order by entry.date desc limit 1",
		[desc:this.tag.getDescription(), userId:this.userId, dailyBit:RepeatType.DAILY_BIT])
	}

	protected findNextRepeatEntry() {
		return findNeighboringDailyRepeatEntry("select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date > :entryDate and (time(entry.date) = time(:entryDate) or time(date_add(entry.date, interval 1 hour)) = time(:entryDate) or time(entry.date) = time(date_sub(:entryDate, interval 1 hour))) and entry.user_id = :userId and ((entry.repeat_type_id & :dailyBit) <> 0) order by entry.date asc limit 1",
		[desc:this.tag.getDescription(), userId:this.userId, dailyBit:RepeatType.DAILY_BIT])
	}

	protected createRepeat(EntryStats stats) {
		if (this.repeatTypeId == null) return

		if (this.repeatType.isContinuous()) {
			// search for matching continuous tag
			String queryStr = "select entry.id from entry entry, tag tag where entry.id != :entryId and entry.tag_id = tag.id and entry.user_id = :userId and tag.description = :desc and (entry.repeat_type_id & :continuousBit <> 0)"

			def entries = DatabaseService.get().sqlRows(queryStr, [entryId:this.id, desc:this.tag.description, userId:this.userId, continuousBit:RepeatType.CONTINUOUS_BIT])

			for (def v in entries) {
				if (v != null) {
					Entry e = Entry.get(v['id'])
					Entry.delete(e, stats) // delete matching entries, to avoid duplicates
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

	protected Entry unGhost(EntryStats stats) {
		if (this.repeatType?.isGhost()) {
			this.repeatTypeId = this.repeatType.toggleGhost().id

			this.durationType = durationType?.unGhost()
			this.processAndSave(stats)
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
	protected DateTime fetchCorrespondingDateTimeInTimeZone(Date baseDate, DateTimeZone currentTimeZone) {
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
	
	protected Entry activateContinuousEntrySingle(Date currentBaseDate, Date nowDate, String timeZoneName, DateTimeZone currentTimeZone, EntryCreateMap creationMap,
			EntryStats stats) {
		long dayStartTime = currentBaseDate.getTime()
		def now = nowDate.getTime()
		long diff = now - dayStartTime
		long thisDiff = this.date.getTime() - dayStartTime

		def m = [:]
		m['timeZoneName'] = timeZoneName
		m['tag'] = this.tag
		m['amount'] = this.amount
		m['amountPrecision'] = this.amountPrecision
		m['units'] = this.units
		m['durationType'] = this.durationType?.unGhost()
		m['baseTag'] = baseTag
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
		Entry retVal = Entry.createSingle(userId, m, creationMap.groupForDate(m['date']), stats)
		
		creationMap.add(retVal)

		Utils.save(retVal, true)
		return retVal
	}

	Entry activateContinuousEntry(Date currentBaseDate, Date nowDate, String timeZoneName, EntryStats stats) {
		if ((this.repeatTypeId == null) || (!this.repeatType.isContinuous()))
			return this
			
		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()
		
		EntryCreateMap creationMap = new EntryCreateMap()
		
		Entry firstActivated = null
		
		for (Entry e : fetchGroupEntries()) {
			Entry activated = e.activateContinuousEntrySingle(currentBaseDate, nowDate, timeZoneName, currentTimeZone, creationMap, stats)
			if (firstActivated == null) firstActivated = activated
		}
		
		return firstActivated
	}

	protected Entry activateTemplateEntrySingle(Long forUserId, Date currentBaseDate, Date nowDate, String timeZoneName, DateTimeZone currentTimeZone, EntryCreateMap creationMap,
			EntryStats stats, String setName) {
		long dayStartTime = currentBaseDate.getTime()
		def now = nowDate.getTime()
		long diff = now - dayStartTime
		long thisDiff = this.date.getTime() - dayStartTime

		def m = [:]
		m['timeZoneName'] = timeZoneName
		m['tag'] = this.tag
		m['amount'] = this.amount
		m['amountPrecision'] = this.amountPrecision
		m['units'] = this.units
		m['durationType'] = this.durationType?.unGhost()
		m['baseTag'] = baseTag
		m['repeatType'] = this.repeatType
		m['setName'] = setName
		m['comment'] = this.comment
		m['date'] = fetchCorrespondingDateTimeInTimeZone(currentBaseDate, currentTimeZone).toDate()
		log.debug "activateTemplateEntrySingle(), data: ${m['date']} and currentTimeZone Calculated: $currentTimeZone"
		m['datePrecisionSecs'] = this.datePrecisionSecs
		def retVal = Entry.createSingle(forUserId, m, creationMap.groupForDate(m['date']), stats)
		
		creationMap.add(retVal)

		return retVal
	}

	Entry activateTemplateEntry(Long forUserId, Date currentBaseDate, Date nowDate, String timeZoneName, EntryStats stats, String setName) {
		if (this.repeatTypeId == null)
			return null
			
		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()
		
		EntryCreateMap creationMap = new EntryCreateMap()
		
		Entry firstActivated = null
		
		for (Entry e : fetchGroupEntries()) {
			Entry activated = e.activateTemplateEntrySingle(forUserId, currentBaseDate, nowDate, timeZoneName, currentTimeZone, creationMap, stats, setName)
			if (firstActivated == null) firstActivated = activated
		}
		
		return firstActivated
	}

	/**
	 * Update existing entry and save
	 */
	protected void doUpdate(Map m, EntryStats stats) {
		if (m == null) return
		
		stats.addEntry(this)
		
		doUpdateSingle(m, stats)
		
		stats.addEntry(this)
	}

	protected void doUpdateSingle(Map m, EntryStats stats) {
		log.debug "Entry.updateSingle() this:" + this + ", m:" + m

		if (m == null) return

		def newTag = m['tag']

		if (newTag == null) return null
		
		Tag newBaseTag = m['baseTag']
		DurationType newDurationType = m['durationType']

		def updateDurationEntry = null

		if (!date.equals(m['date'])) {
			updateDurationEntry = preDeleteProcessing(stats)
		}

		def descriptionChanged = tag.getId() != newTag.getId()

		m['amount'] = m['amount'] == null ? null : new BigDecimal(m['amount'], mc)

		TimeZoneId timeZoneId = TimeZoneId.look(m['timeZoneName'])
		if (timeZoneId == null) {
			timeZoneId = TimeZoneId.fromId(m['timeZoneId'])
		}
		
		/**
		 * check to see if entry repeat type has changed. If so, merge and/or create new repeat structure
		 */
		boolean updateRepeatType = false
		if (m['repeatType'] != null && this.date != m['date'] || (!Utils.equals(m['amount'], this.amount))
				|| (this.repeatTypeId != m['repeatType']?.id)) {
			updateRepeatType = true
		}

		if (m['repeatType'] != null && m['repeatEnd'] != null) {
			m['repeatEnd'] = ((RepeatType)m['repeatType']).makeRepeatEnd(m['repeatEnd'], m['date'], timeZoneId)
		}
		
		setTag(m['tag'])
		setDate(m['date'])
		setDatePrecisionSecs(m['datePrecisionSecs'])
		checkAmount(m)
		setAmount(m['amount'])
		setAmountPrecision(m['amountPrecision']?:DEFAULT_AMOUNTPRECISION)
		setUnits(m['units']?:'')
		setComment(m['comment']?:'')
		setRepeatTypeId(m['repeatType']?.id)
		setSetIdentifier(Identifier.look(m['setName']))
		setBaseTag(newBaseTag)
		setDurationType(newDurationType)
		setTimeZoneId((Integer)timeZoneId.getId())
		setRepeatEnd(m['repeatEnd'] ? m['repeatEnd'] : this.repeatEnd)

		Utils.save(this, true)

		if (updateRepeatType)
			createRepeat(stats)

		if (updateDurationEntry != null) {
			updateDurationEntry.updateDurationEntry(stats)
		}
		
		processAndSave(stats)

		ifDurationRemoveFromGroup()

		log.debug "Repeat type: " + m['repeatType']
	}

	/**
	 * Fetch methods - for getting different subsets of entries
	 */
	static def fetchContinuousRepeats(Long userId, Date now, Date currentDate) {
		String continuousQuery = "from Entry entry where entry.userId = :userId and entry.repeatEnd is null and (not entry.repeatTypeId is null) and (entry.repeatTypeId & :continuousBit <> 0)"

		def entries = Entry.executeQuery(continuousQuery, [userId:userId, continuousBit:RepeatType.CONTINUOUS_BIT])

		return entries
	}

	static def fetchReminders(User user, Date startDate, long intervalSecs) {
		Date endDate = new Date(startDate.getTime() + intervalSecs * 1000L)

		String queryStr = "select distinct entry.id " \
		+ "from entry entry where entry.user_id = :userId and " \
		+ "entry.date < :endDate and (entry.repeat_end is null or entry.repeat_end >= :startDate) and (not entry.repeat_type_id is null) and (entry.repeat_type_id & :remindBit <> 0) " \
		+ "and (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400 < :interval"

		def rawResults = DatabaseService.get().sqlRows(queryStr, [userId:user.getId(), endDate:endDate, startDate:startDate, interval:intervalSecs, remindBit:RepeatType.REMIND_BIT])
	}
	
	boolean isPrimaryEntry() {
		if (group == null || group.isEmpty()) {
			return true
		}
		
		if (durationType.isStart())
			return true
		
		Entry firstEntry = group.fetchSortedEntries().first()
		
		if (getId() == firstEntry?.getId()) {
			return true
		}
		
		return false
	}

	protected static long abs(long x) { return x < 0 ? -x : x }

	static fetchByUserAndTag(Long userId, String tagDescription) {
		// TODO: write more efficient SQL.
		Tag tag = Tag.findByDescription(tagDescription)
			Entry.findAllWhere(userId: userId, tag: tag)
	}

	static fetchByUserAndTaggable(userId, taggable) {
		def tags = taggable.tags()
		def entries = []
		for (tag in tags) {
			entries += fetchByUserAndTag(userId, tag.description)
		}
		entries
	}

	static Date userStartTime(userId) {
		def results = Entry.executeQuery("select min(e.date) from Entry e where e.userId = :userId", [userId:userId])
		if (results && results[0])
			return results[0]
		
		return new Date(0L)
	}

	static Date userStopTime(userId) {
		def results = Entry.executeQuery("select max(e.date) from Entry e where e.userId = :userId", [userId:userId])
		if (results && results[0])
			return results[0]
		
		return new Date(0L)
	}
	
	static def fetchListData(User user, String timeZoneName, Date baseDate, Date now) {
		long nowTime = now.getTime()
		long baseTime = baseDate.getTime()

		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()

		// get regular elements + timed repeating elements next
		String queryStr = "select distinct entry.id " \
				+ "from entry entry, tag tag where entry.user_id = :userId and (((entry.date >= :startDate and entry.date < :endDate) and (entry.repeat_type_id is null or (entry.repeat_type_id & :continuousBit = 0))) or " \
				+ "(entry.date < :startDate and (entry.repeat_end is null or entry.repeat_end >= :startDate) and (not entry.repeat_type_id is null) and (entry.repeat_type_id & :continuousBit = 0))) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by case when entry.date_precision_secs < 1000 and (entry.repeat_type_id is null or (entry.repeat_type_id & :continuousBit = 0)) " \
				+ "then unix_timestamp(timestampadd(second, (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400, :startDate)) else " \
				+ "(case when entry.repeat_type_id is null then 99999999999 else 0 end) end desc, tag.description asc"

		def queryMap = [userId:user.getId(), startDate:baseDate, endDate:baseDate + 1, continuousBit:RepeatType.CONTINUOUS_BIT]

		def rawResults = DatabaseService.get().sqlRows(queryStr, queryMap)

		def timedResults = []

		Set resultTagIds = new HashSet<Long>()

		for (result in rawResults) {
			Entry entry = Entry.get(result['id'])
			
			if (!entry.isPrimaryEntry())
				continue
			
			def desc = entry.getJSONDesc()

			// use JodaTime to figure out the correct time in the current time zone
			desc['timeZoneName'] = timeZoneName
			if (entry.repeatTypeId != null) { // adjust dateTime for repeat entries
				def date = entry.fetchCorrespondingDateTimeInTimeZone(baseDate, currentTimeZone).toDate()
				if (entry.repeatEnd != null && entry.repeatEnd.getTime() < date.getTime()) // check if adjusted time is after repeat end
					continue
				desc['date'] = date
				desc['repeatType'] = entry.repeatTypeId
			} else
				desc['repeatType'] = null
			desc['setName'] = entry.setIdentifier?.toString()
			timedResults.add(desc)
		}

		// get continuous repeating elements
		String continuousQueryStr = "select distinct entry.id as id, tag.description " \
				+ "from entry entry, tag tag where entry.user_id = :userId and " \
				+ "entry.repeat_end is null and (not entry.repeat_type_id is null) and (entry.repeat_type_id & :continuousBit <> 0) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by tag.description asc"

		def continuousResults = DatabaseService.get().sqlRows(continuousQueryStr, [userId:user.getId(), continuousBit:RepeatType.CONTINUOUS_BIT])

		def results = []

		// add continuous elements that aren't already in the timed list
		for (result in continuousResults) {
			Entry entry = Entry.get(result['id'])

			if (!entry.isPrimaryEntry())
				continue
			
			if (!resultTagIds.contains(entry.getTag().getId())) {
				def desc = entry.getJSONDesc()
				desc['date'] = baseDate
				desc['repeatType'] = entry.repeatTypeId
				desc['timeZoneName'] = timeZoneName
				results.add(desc)
			}
		}

		// add timed results
		for (result in timedResults) {
			results.add(result)
			log.debug "Entry id: " + result['id'] + " tag: " + result['tag']
		}

		return results
	}

	static def fetchListDataNoRepeats(User user, Date date) {
		def c = Entry.createCriteria()

		def results = []

		String queryStr = "select distinct entry.id " \
				+ "from entry entry, tag tag where entry.user_id = :userId and (entry.date >= :startDate and entry.date < :endDate) and (entry.repeat_type_id is null or (entry.repeat_type_id & :ghostBit = 0)) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by case when entry.date_precision_secs < 1000 " \
				+ "then unix_timestamp(entry.date) else 0 end desc, tag.description asc"

		def rawResults = DatabaseService.get().sqlRows(queryStr, [userId:user.getId(), startDate:date, endDate:date + 1, ghostBit:RepeatType.GHOST_BIT])

		for (result in rawResults) {
			Entry entry = Entry.get(result['id'])
			
			if (!entry.isPrimaryEntry())
				continue
			
			def desc = entry.getJSONDesc()
			results.add(desc)
		}

		return Utils.listJSONDesc(results)
	}

	/**
	 * return [min, max] for list of tagIds for given user
	 */
	static def fetchTagIdsMinMax(Long userId, def tagIds) {
		def tagValueStats = []
		
		for (Long tagId : tagIds) {
			tagValueStats.add(TagValueStats.look(userId, tagId))
		}
		
		BigDecimal min = null, max = null
		
		for (TagValueStats stats : tagValueStats) {
			if ((min == null) || (stats.minimum != null && stats.minimum < min))
				min = stats.minimum
			if ((max == null) || (stats.maximum != null && stats.maximum < max))
				max = stats.maximum
		}
		
		return [min, max]
	}
	
	static def fetchPlotData(User user, def tagIds, Date startDate, Date endDate, Date currentTime, String timeZoneName, Map plotInfo = null) {
		log.debug "Entry.fetchPlotData() userId:" + user.getId() + ", tagIds:" + tagIds + ", startDate:" + startDate \
				+ ", endDate:" + endDate + ", timeZoneName:" + timeZoneName
		
		return DataRetriever.get().fetchPlotData(user.id, tagIds, startDate, endDate, currentTime, timeZoneName, plotInfo)
	}
	
	static def fetchSumPlotData(User user, def tagIds, Date startDate, Date endDate, Date currentDate, String timeZoneName, Map plotInfo = null) {
		log.debug "Entry.fetchSumPlotData() userId:" + user.getId() + ", tagIds:" + tagIds + ", startDate:" + startDate \
				+ ", endDate:" + endDate + ", timeZoneName:" + timeZoneName

		return DataRetriever.get().fetchSumPlotData(user.id, tagIds, startDate, endDate, currentDate, timeZoneName, plotInfo)
	}
	
	static def fetchAverageTime(User user, def tagIds, Date startDate, Date endDate, Date currentTime, DateTimeZone currentTimeZone) {
		log.debug "Entry.fetchAverageTime() userId:" + user.getId() + ", tagIds:" + tagIds + ", startDate:" + startDate \
				+ ", endDate:" + endDate + ", currentTimeZone:" + currentTimeZone
		
		return DataRetriever.get().fetchAverageTime(user.id, tagIds, startDate, endDate, currentTime, currentTimeZone)
	}

	Integer fetchDatePrecisionSecs() {
		return datePrecisionSecs == null ? 60 : datePrecisionSecs;
	}

	Integer fetchAmountPrecision() {
		return amountPrecision == null ? DEFAULT_AMOUNTPRECISION : amountPrecision;
	}

	String getDescription() {
		return tag.getDescription()
	}

	String exportString() {
		return description + " " + (amount == null ? "none " : amountPrecision == 0 ? (amount ? "yes " : "no ") : (amountPrecision > 0 ? amount + " " : '')) + (units ? units + ' ' : '') + (datePrecisionSecs < 40000 ? (date ? gmtFormat.format(date) : '') + ' ' : '') + comment
	}

	/**
	 * Scan existing entries and create duration entries if they don't exist
	 * @param user
	 */
	static void updateDurationEntries(User user) {
		// process start/end tags first
		log.debug "Entry.updateDurationEntries() this:" + this + ", userId:" + user.getId()

		EntryStats stats = new EntryStats(user.getId())
		
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
			entry.updateDurationEntry(stats)
	}

	/**
	 * Get data for the main entries lists
	 */
	Map getJSONDesc() {
		def retVal = [id:this.id,
			userId:userId,
			date:date,
			datePrecisionSecs:fetchDatePrecisionSecs(),
			timeZoneName:this.fetchTimeZoneName(),
			description:((durationType?.isStartOrEnd()) && baseTag != null) ? tag.getDescription() : baseTag.getDescription(),
			amount:amount,
			amountPrecision:fetchAmountPrecision(),
			units:units,
			comment:comment,
			repeatType:repeatTypeId,
			repeatEnd:repeatEnd]
		
		Integer index = 0
		
		Map amounts = [:]
		
		for (Entry e : fetchGroupEntries()) {
			UnitGroupMap.theMap.getJSONAmounts(userId, tag.id, amounts, e.getAmount(), e.fetchAmountPrecision(), e.getUnits())
		}
		
		retVal['amounts'] = amounts
		
		return retVal
	}

	String getDateString() {
		return Utils.dateToGMTString(date)
	}

	String toString() {
		return "Entry(id: ${id ?: 'un-saved'}, userId:" + userId \
				+ ", date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneName:" + fetchTimeZoneName() \
				+ ", baseTag:" + getBaseTag().getDescription() \
				+ ", description:" + getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatTypeId \
				+ ", repeatEnd:" + Utils.dateToGMTString(repeatEnd) \
				+ ", setName:" + setIdentifier?.getValue() \
				+ ")"
	}

	def String valueString() {
		return "Entry(userId:" + userId \
				+ ", date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneName:" + fetchTimeZoneName() \
				+ ", description:" + getBaseTag().getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatTypeId \
				+ ", repeatEnd:" + Utils.dateToGMTString(repeatEnd) \
				+ ")"
	}

	String contentString() {
		return "Entry(date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneName:" + fetchTimeZoneName() \
				+ ", description:" + getBaseTag().getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatTypeId \
				+ ", repeatEnd:" + Utils.dateToGMTString(repeatEnd) \
				+ ")"
	}

	int compareTo(Object obj) {
		if (!obj instanceof Entry) {
			return -1
		}
		
		if (this.is(obj))
			return 0
			
		Entry e = (Entry) obj
		String eUnits = e.getUnits()
		if (!units) {
			if (eUnits) {
				return -1 // no units always comes first
			}
			int v = this.valueString().compareTo(obj.valueString())
			if (v == 0)
				return -1
			return v
		}
		
		if (!eUnits) {
			return 1 // no units always comes first
		}
		
		UnitRatio thisUnitRatio = UnitGroupMap.theMap.unitRatioForUnits(units)
		UnitRatio thatUnitRatio = UnitGroupMap.theMap.unitRatioForUnits(eUnits)
		
    	int thisPri = (thisUnitRatio?.getGroupPriority()) ?: -1
		int thatPri = (thatUnitRatio?.getGroupPriority()) ?: -1
		
		if (thisPri < thatPri) return -1
		else if (thisPri > thatPri) return 1

		thisPri = (thisUnitRatio?.affinity) ?: -1
		thatPri = (thatUnitRatio?.affinity) ?: -1
				
		if (thisPri < thatPri) return -1
		else if (thisPri > thatPri) return 1

		int v = this.units.compareTo(obj.units)
		
		if (v == 0)
			return this.valueString().compareTo(obj.valueString())
		
		return v
	}
	
	int hashCode() {
		return ((int)userId?:0) + ((int)date.getTime()) + ((int)tag.id) + (amount.toString().hashCode()) + ((int)repeatTypeId?:0)
	}

	boolean equals(Object o) {
		if (!(o instanceof Entry))
			return false
		return this.is(o)
	}

	static Map canDelete(Entry entry, User user) {
		Map result

		if (user == null) {
			result = [canDelete: false, messageCode: "auth.error.message"]
		} else {
			def userId = entry.getUserId();
			if (userId != user.getId()) {
				User entryOwner = User.get(userId)
				if (entryOwner.virtual && Sprint.findByVirtualUserId(userId)?.hasAdmin(user.getId())) {
					result = [canDelete: true]
				} else {
					result = [canDelete: false, messageCode: "delete.entry.permission.denied"]
				}
			} else {
				result = [canDelete: true]
			}
		}

		return result
	}
}

enum DurationType {
	NONE(0), START(DURATIONSTART_BIT), END(DURATIONEND_BIT),
			GHOSTSTART(DURATIONSTART_BIT | DURATIONGHOST_BIT),
			GHOSTEND(DURATIONEND_BIT | DURATIONGHOST_BIT),
			GENERATEDSTART(DURATIONSTART_BIT | DURATIONGENERATED_BIT),
			GENERATEDEND(DURATIONEND_BIT | DURATIONGENERATED_BIT),
			GENERATEDDURATION(DURATIONGENERATED_BIT), // this is a generated calculated duration entry
			GENERATEDSPRINT(DURATIONGENERATED_BIT | SPRINT_BIT),
			GENERATEDSPRINTSTART(DURATIONGENERATED_BIT | SPRINT_BIT | DURATIONSTART_BIT),
			GENERATEDSPRINTEND(DURATIONGENERATED_BIT | SPRINT_BIT | DURATIONEND_BIT)
			
	static final int DURATIONSTART_BIT = 1
	static final int DURATIONEND_BIT = 2
	static final int SPRINT_BIT = 4
	static final int DURATIONGHOST_BIT = 16
	static final int DURATIONGENERATED_BIT = 32
	
	final Integer id

	private static final Map<Integer, RepeatType> map = new HashMap<Integer, RepeatType>()
	
	static {
		for (DurationType t : DurationType.values()) {
			map.put(t.getId(), t)
		}
	}
	
	static DurationType get(int id) {
		return map.get(id)
	}
	
	DurationType(Integer id) {
		this.id = id
	}
	
	boolean isDurationGhost() {
		return this.id & DURATIONGHOST_BIT != 0
	}
	
	boolean isGhost() {
		return (this.id & DURATIONGHOST_BIT) != 0
	}
	
	boolean isGenerated() {
		return (this.id & DURATIONGENERATED_BIT) != 0
	}
	
	boolean isStart() {
		return (this.id & DURATIONSTART_BIT) != 0
	}
	
	boolean isEnd() {
		return (this.id & DURATIONEND_BIT) != 0
	}
	
	boolean isSprint() {
		return (this.id & SPRINT_BIT) != 0
	}
	
	boolean isStartOrEnd() {
		return (this.id & (DURATIONSTART_BIT | DURATIONEND_BIT)) != 0
	}
	
	DurationType unGhost() {
		return get(this.id & ~DURATIONGHOST_BIT)
	}
	
	DurationType makeGhost() {
		return get(this.id | DURATIONGHOST_BIT)
	}
	
	DurationType makeGenerated() {
		return get(this.id | DURATIONGENERATED_BIT)
	}
	
	DurationType makeSprint() {
		return get(this.id | SPRINT_BIT)
	}
}


package us.wearecurio.model

import grails.orm.PagedResultList
import org.apache.commons.logging.LogFactory
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime

import us.wearecurio.collection.SingleCollection
import us.wearecurio.data.*
import us.wearecurio.data.DecoratedUnitRatio.AmountUnits
import us.wearecurio.data.RepeatType
import us.wearecurio.data.TagUnitStatsInterface
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.datetime.IncrementingDateTime
import us.wearecurio.exception.InstanceNotFoundException
import us.wearecurio.parse.EntryCommaSegmenter
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.EntryParserService.ParseAmount
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.utility.Utils
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.EntryParserService.ParseAmount

import java.math.MathContext
import java.text.DateFormat
import java.util.Date;

class Entry implements Comparable {

	private static def log = LogFactory.getLog(this)

	static final int ENTRY_SCALE = 9
	static final MathContext mc = new MathContext(ENTRY_SCALE)

	static final BigDecimal MAX_AMOUNT = 9999999999.999999999g
	static final BigDecimal MIN_AMOUNT = -9999999999.999999999g

	static final int DEFAULT_DATEPRECISION_SECS = 3 * 60
	static final int VAGUE_DATE_PRECISION_SECS = 86400
	static final int MAXCOMMENTLENGTH = 100000
	static final int MAXUNITSLENGTH = 50
	static final int MAXSETNAMELENGTH = 50
	static final long HOURTICKS = 60L * 60000
	static final long DAYTICKS = HOURTICKS * 24
	static final long HALFDAYTICKS = HOURTICKS * 12
	static final long WEEKTICKS = DAYTICKS * 7

	static constraints = {
		group(nullable:true)
		amount(scale:ENTRY_SCALE, max:9999999999999999999.999999999, nullable:true)
		date(nullable:true)
		startDate(nullable:true)
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
	Date startDate // the start date of a duration, if this is a completed duration, otherwise null
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

	static transients = ['repeatType']

	static Date theBookmarkBaseDate

	static {
		def entryTimeZone = Utils.createTimeZone(0, "GMT", true)
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
		dateFormat.setTimeZone(entryTimeZone)
		theBookmarkBaseDate = dateFormat.parse("January 1, 2000 12:00 am")
	}

	RepeatType getRepeatType() { repeatTypeId == null ? null : RepeatType.look(repeatTypeId) }

	void setRepeatType(RepeatType repeatType) {
		this.repeatTypeId = repeatType?.id
	}

	boolean isGhost() {
		if (repeatTypeId == null) return false
		return getRepeatType().isGhost()
	}

	boolean isAlert() {
		if (repeatTypeId == null) return false
		return getRepeatType().isReminder()
	}

	boolean isReminder() {
		if (repeatTypeId == null) return false
		return getRepeatType().isReminder()
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
	static Entry updatePartialOrCreate(Long userId, Map m, EntryGroup group, EntryStats stats) {
		stats.userId = userId

		log.debug("Entry.updatePartialOrCreate: Trying to find a partial entry to update")
		def partialEntry = lookForPartialEntry(userId, m)
		log.debug("Entry.updatePartialOrCreate: " + partialEntry.size() +" partial entries found")
		if (partialEntry.size() < 1) {
			return Entry.createOrCompleteSingle(userId, m, group, stats)
		}

		BigDecimal amount = ((ParseAmount)m.amount).amount

		for (entry in partialEntry) {
			log.debug ("Updating partial entry " + entry)
			entry.amount = amount
			Utils.save(entry, true)
			stats.addEntry(entry)
			return entry
		}
	}

	protected static List lookForPartialEntry(userId, Map m) {
		/**
		 * Making sure it is a summary entry. Non summary entries will
		 * not have partial entries
		 */
		ParseAmount amount = m.amount

		if (!amount.tag.description.endsWith(" summary")) {
			return []
		}

		Tag tag = amount.tag
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

	protected static final BigDecimal smallDifference = 0.01g

	// Prevent duplicates for imported entries
	protected static boolean hasDuplicate(Long userId, Map m) {
		if (m.setName == null)
			return false

		log.debug "Trying to find duplicate for " + userId + " for " + m

		Identifier setId = Identifier.look(m.setName)

		RepeatType repeatType = (RepeatType) m['repeatType']
		ParseAmount amount = m['amount']

		if (repeatType != null && repeatType.isContinuous()) {
			def c = Entry.createCriteria()
			def results = c {
				and {
					eq("userId", userId)
					eq("tag", amount.tag)
					eq("amount", amount.amount.setScale(9, BigDecimal.ROUND_HALF_EVEN))
					eq("amountPrecision", (Integer)amount.precision)
					eq("units", amount.units)
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
					eq("tag", amount.tag)
					eq("datePrecisionSecs", m.datePrecisionSecs == null ? DEFAULT_DATEPRECISION_SECS : m.datePrecisionSecs)
					eq("amount", amount.amount.setScale(9, BigDecimal.ROUND_HALF_EVEN))
					eq("amountPrecision", (Integer)amount.precision)
					eq("units", amount.units)
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

	static Entry createBookmark(Long userId, String entryText, EntryStats stats) {
		return Entry.create(userId, EntryParserService.get().parse(theBookmarkBaseDate, "Etc/UTC", entryText, RepeatType.CONTINUOUSGHOST.id, null, theBookmarkBaseDate), stats)
	}

	protected static void checkAmount(Map m) {
		BigDecimal amount = m['amount'].amount

		if (amount) {
			if (amount.compareTo(MAX_AMOUNT) > 0) {
				m['amount'].amount = MAX_AMOUNT
				if (!m['comment'].endsWith("(amount too large to store)"))
					m['comment'] += "(amount too large to store)"
			} else if (amount.compareTo(MIN_AMOUNT) < 0) {
				m['amount'].amount = MIN_AMOUNT
				if (!m['comment'].endsWith("(amount too small to store)"))
					m['comment'] += "(amount too small to store)"
			}
		}
	}

	protected static prepareStartDate(Long userId, Map m) {
		ParseAmount amount = m['amount']
		Date date = m['date']
		Long milliseconds = amount.durationInMilliseconds()
		if (milliseconds != null)
			m['startDate'] = new Date(date.getTime() - milliseconds)
		else
			m['startDate'] = null
	}

	protected static Entry createOrCompleteSingle(Long userId, Map m, EntryGroup group, EntryStats stats) {
		log.debug "Entry.createOrCompleteSingle() userId:" + userId + ", m:" + m

		ParseAmount amount = m['amount']
		Date date = m['date']

		// intercept duration creation --- if creating duration end, search for previous duration start
		DurationType durationType = amount.durationType
		if (durationType != DurationType.NONE && durationType != null && ((RepeatType)m['repeatType'])?.isRepeatOrContinuous()) {
			// cannot combine duration edge and repeat
			amount.durationType = DurationType.NONE
			amount.tag = amount.baseTag
		} else if (durationType == DurationType.END) {
			Entry updated = completeDurationEntrySingle(userId, m, stats, DurationType.END) {
				return findPreviousStartEntry(userId, amount.baseTag, date)
			}

			if (updated != null)
				return updated
		} else if (durationType == DurationType.START) {
			Entry updated = completeDurationEntrySingle(userId, m, stats, DurationType.START) {
				return findNextEndEntry(userId, amount.baseTag, date)
			}

			if (updated != null)
				return updated
		}

		return createSingle(userId, m, group, stats)
	}

	protected static Entry createSingle(Long userId, Map m, EntryGroup group, EntryStats stats) {
		log.debug "Entry.createSingle() userId:" + userId + ", m:" + m

		if ((!stats.getBatchCreation()) && hasDuplicate(userId, m)) {
			log.debug "Entry map is a duplicate, do not add: " + m
			return null
		}

		stats.setUserId(userId)

		ParseAmount amount = m['amount']

		Tag baseTag = amount.baseTag
		DurationType durationType = amount.durationType

		Tag tag = amount.tag

		TimeZoneId timeZoneId
		if (m['timeZoneId']) timeZoneId = TimeZoneId.fromId(m['timeZoneId'])
		else timeZoneId = TimeZoneId.look(m['timeZoneName'])

		checkAmount(m)

		if (amount.units == null)
			amount.units = ''

		TagUnitStatsInterface tagUnitStats
		if ((!amount.units) && (amount.precision > 0)) {
			// Using the most used unit in case the unit is unknown, if the amountPrecision is > 0 and used more than 2 times
			tagUnitStats = TagUnitStats.mostUsedTagUnitStats(userId, tag.getId())
			if (tagUnitStats && tagUnitStats.timesUsed > 2) {
				amount.units = tagUnitStats.getUnitGroup()?.lookupUnitString(tagUnitStats.unit, (amount.amount.compareTo(BigDecimal.ONE) != 0))
			}
		} else {
			TagUnitStats.createOrUpdate(userId, tag.id, amount.units)
			TagUnitStats.createOrUpdate(userId, baseTag.id, amount.units)
		}

		if (m['repeatType'] != null && m['repeatEnd'] != null) {
			m['repeatEnd'] = ((RepeatType)m['repeatType']).makeRepeatEnd(m['repeatEnd'], m['date'], timeZoneId.toDateTimeZone())
		}

		prepareStartDate(userId, m)

		Entry entry = new Entry(
						userId:userId,
						group:group,
						date:m['date'],
						startDate:m['startDate'],
						timeZoneId:timeZoneId.getId(),
						datePrecisionSecs:m['datePrecisionSecs'] == null ? DEFAULT_DATEPRECISION_SECS : m['datePrecisionSecs'],
						tag:tag,
						amount:amount.amount,
						repeatTypeId: m['repeatType']?.id,
						repeatEnd: m['repeatEnd'],
						baseTag:baseTag,
						durationType:durationType,
						setIdentifier:Identifier.look(m['setName']),
						amountPrecision:amount.precision==null ? 3 : amount.precision
						)

		entry.setUnits(amount.units ?: '')
		entry.setComment(m['comment'] ?: '')

		log.debug "Created entry:" + entry

		if (group != null) {
			group.add(entry)
		}

		entry.processAndSave(stats)

		entry.createRepeat(stats)

		if (tag.hasEntry(userId)) {
			User.addToCache(userId, tag)
		}
		stats.addEntry(entry)
		stats.setTimeZoneId(entry.getTimeZoneId())

		return entry
	}

	static final BigDecimal DAYTICKSDECIMAL = (1000.0g * 60.0g * 60.0g * 24.0g).setScale(100, BigDecimal.ROUND_HALF_UP)
	static protected DecoratedUnitRatio daysUnitRatio = null

	static Entry completeDurationEntry(Long userId, Map m, EntryStats stats, DurationType durationType, Closure findOtherEntry) {
		Date date = m['date']
		def amounts = m['amounts']
		Entry other = null
		if (!m['hasDurationAmount']) { // only try to complete duration with another entry if duration amount not specified
			other = findOtherEntry()
		}
		ParseAmount durationAmount = null
		if (other != null) {
			BigDecimal durationTicks = ((BigDecimal) date.getTime() - other.date.getTime()).setScale(100, BigDecimal.ROUND_HALF_UP)
			if (durationType == DurationType.START)
				durationTicks = -durationTicks
			BigDecimal days = (durationTicks / DAYTICKSDECIMAL).setScale(100, BigDecimal.ROUND_HALF_UP)
			if (amounts) {
				for (ParseAmount amount : amounts) {
					if (!amount.isActive())
						continue
					if (amount.isDuration() && amount.unitRatio != null && amount.unitRatio.unitSuffix == null) {
						durationAmount = amount
						if (amount.amount != null) {
							m['tag'] = m['baseTag'] // get rid of modifier tag, this is a complete duration tag
							amount.tag = amount.baseTag
						}
						break
					}
					if (amount.unitRatio == null && amount.precision < 0) {
						amount.deactivate() // default amount removed and replaced by duration value
					}
				}
			}
			if (durationAmount == null) {
				AmountUnits newAmountUnits = UnitGroupMap.DAYSDURATIONRATIO.getRightSizedAmountUnits(days, 3)
				Tag baseTag = m.baseTag
				durationAmount = new ParseAmount(baseTag, newAmountUnits.amount, newAmountUnits.amountPrecision, newAmountUnits.units, newAmountUnits.unitRatio, DurationType.NONE)
				TagUnitStats.createOrUpdate(userId, baseTag.id, newAmountUnits.units)
				amounts.add(durationAmount)
			}
			m['durationType'] = DurationType.NONE
			other.update(m, stats, date, true, true)

			return other
		} else {
			// create entry with start or end time because there is no matching
			for (ParseAmount amount : amounts) {
				if (!amount.isActive())
					continue
				// duration has been specified with this entry, so complete the entry
				if (amount.isDuration() && amount.unitRatio != null && amount.unitRatio.unitSuffix == null) {
					if (amount.amount != null) {
						// if this is the start of the interval
						if (durationType == DurationType.START) {
							// move date of entry to the end of the interval
							BigDecimal durationTicksDecimal = amount.durationInMilliseconds()
							if (durationTicksDecimal != null) {
								long durationTicks = durationTicksDecimal.longValue()
								m['date'] = new Date(m['date'].getTime() + durationTicks)
							}
						}
						// complete this entry and rewrite it as a full duration entry
						m['durationType'] = DurationType.NONE
						AmountUnits newAmountUnits = UnitGroupMap.DAYSDURATIONRATIO.getRightSizedAmountUnits(amount.durationInDays(), 3)
						amount.update(amount.tag, m['baseTag'], newAmountUnits.amount, newAmountUnits.amountPrecision,
								newAmountUnits.units + (amount.unitSuffix ? ' ' + amount.unitSuffix : ''), newAmountUnits.unitRatio, DurationType.NONE)
						return null
					} else {
						m['durationType'] = DurationType.NONE
						amount.update(amount.tag, m['baseTag'], null, 3,
								amount.units + (amount.unitSuffix ? ' ' + amount.unitSuffix : ''), amount.unitRatio, DurationType.NONE)
						return null
					}
				} else if (amount.unitRatio == null && amount.precision < 0) {
					amount.durationType = durationType
					return null
				}
			}

			amounts.add(new ParseAmount(m['tag'], m['baseTag'], BigDecimal.ONE, -1, "", null, durationType))
			return null
		}
	}

	static Entry completeDurationEntrySingle(Long userId, Map m, EntryStats stats, DurationType durationType, Closure findOtherEntry) {
		Date date = m['date']
		ParseAmount amount = m['amount']
		Entry other = findOtherEntry()
		if (other != null) {
			BigDecimal durationTicks = ((BigDecimal) date.getTime() - other.date.getTime()).setScale(100, BigDecimal.ROUND_HALF_UP)
			if (durationType == DurationType.START)
				durationTicks = -durationTicks
			BigDecimal days = (durationTicks / DAYTICKSDECIMAL).setScale(100, BigDecimal.ROUND_HALF_UP)
			if (amount.isDuration() && amount.unitRatio != null && amount.unitRatio.unitSuffix == null) {
				if (amount.amount != null) {
					amount.tag = amount.baseTag
				}
			} else {
				AmountUnits newAmountUnits = UnitGroupMap.DAYSDURATIONRATIO.getRightSizedAmountUnits(days, 3)
				m['amount'] = new ParseAmount(amount.baseTag, newAmountUnits.amount, newAmountUnits.amountPrecision, newAmountUnits.units, newAmountUnits.unitRatio, DurationType.NONE)
			}
			other.doUpdateSingle(m, stats)

			return other
		} else {
			// create entry with start or end time because there is no matching
				// duration has been specified with this entry, so complete the entry
			if (amount.isDuration() && amount.unitRatio != null && amount.unitRatio.unitSuffix == null) {
				if (amount.amount != null) {
					// if this is the start of the interval
					if (durationType == DurationType.START) {
						// move date of entry to the end of the interval
						BigDecimal durationTicksDecimal = amount.durationInMilliseconds()
						if (durationTicksDecimal != null) {
							long durationTicks = durationTicksDecimal.longValue()
							m['date'] = new Date(m['date'].getTime() + durationTicks)
						}
					}
					// complete this entry and rewrite it as a full duration entry
					amount.tag = amount.baseTag
					AmountUnits newAmountUnits = UnitGroupMap.DAYSDURATIONRATIO.getRightSizedAmountUnits(amount.durationInDays(), 3)
					amount.update(amount.baseTag, newAmountUnits.amount, newAmountUnits.amountPrecision,
							newAmountUnits.units + (amount.unitSuffix ? ' ' + amount.unitSuffix : ''), newAmountUnits.unitRatio, DurationType.NONE)
					return null
				}
			} else if (amount.unitRatio == null && amount.precision < 0) {
				amount.durationType = durationType
				return null
			}

			m['amount'] = new ParseAmount(amount.tag, amount.baseTag, BigDecimal.ONE, -1, "", null, durationType)
			return null
		}
	}

	/**
	 * Create Entry from parse parameters, entryStr may be comma-separated list of entries
	 * 
	 * returns [status, entries array]
	 */
	static def parseAndCreate(Long userId, EntryStats stats, Date currentTime, String timeZoneName, String entryStr, Long repeatTypeId, Date repeatEnd, Date baseDate, boolean defaultToNow = true, int updateMode) {
		EntryCommaSegmenter segmenter = new EntryCommaSegmenter(entryStr)
		
		def entries = []
		def parsedList = []
		def previousParsedList = []
		def status
		def parsed
		
		String text
		
		Date dateSpecified = null
		
		while (text = segmenter.next()) {	
			parsed = EntryParserService.get().parse(currentTime, timeZoneName, text, repeatTypeId, repeatEnd, baseDate, defaultToNow, updateMode)
		
			status = parsed.status
			
			if (parsed.foundTime) {
				for (previousParsed in previousParsedList) {
					previousParsed.date = parsed.date
				}
				previousParsedList = []
			} else
				previousParsedList.add(parsed)
			
			parsedList.add(parsed)
		}

		for (parsed2 in parsedList) {
			entries.add(Entry.create(userId, parsed2, stats))
		}
		
		return [status, entries]
	}
	/**
	 * Create Entry
	 *
	 * EntryStats are passed in to handle batch jobs, so entry statistics are updated all at once at the end of
	 * the entry creation sequence, rather than on each entry creation
	 */
	static Entry create(Long userId, Map m, EntryStats stats) {
		stats.userId = userId

		log.debug "Entry.create() userId:" + userId + ", m:" + m

		if (m == null) return null

		if (m['tag'] == null) return null

		def amounts = m['amounts']
		Date date = m['date']

		if (((String)m['setName'])?.length() > MAXSETNAMELENGTH) {
			m['setName'] = ((String)m['setName']).substring(0, MAXSETNAMELENGTH)
		}

		// intercept duration creation --- if creating duration end, search for previous duration start
		DurationType durationType = m['durationType']
		if (durationType != DurationType.NONE && durationType != null && ((RepeatType)m['repeatType'])?.isRepeatOrContinuous()) {
			// cannot combine duration edge and repeat
			m['durationType'] = DurationType.NONE
			m['tag'] = m['baseTag']
		} else if (durationType == DurationType.END) {
			Entry updated = completeDurationEntry(userId, m, stats, DurationType.END) {
				return findPreviousStartEntry(userId, m['baseTag'], date)
			}

			if (updated != null)
				return updated
		} else if (durationType == DurationType.START) {
			Entry updated = completeDurationEntry(userId, m, stats, DurationType.START) {
				return findNextEndEntry(userId, m['baseTag'], date)
			}

			if (updated != null)
				return updated
		}

		EntryGroup entryGroup = null

		if (amounts.size() > 1) {
			entryGroup = new EntryGroup()
		}

		int index = 0

		Entry e = null
		for (ParseAmount amount : amounts) {
			if (!amount.isActive())
				continue
			m['amount'] = amount
			e = createSingle(userId, m, entryGroup, stats)
		}
		if (entryGroup != null) {
			Utils.save(entryGroup, true)
			return entryGroup.fetchSortedEntries().first() // return first entry in sorted list
		}

		return e
	}

	protected EntryGroup createOrFetchGroup() {
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

		ParseAmount amount = m.amount

		if (amount.tag == null) return false

		if (this.tag.id != amount.tag.id)
			return false

		DateTime entryDateTime = fetchDateTime()
		LocalTime localTime = entryDateTime.toLocalTime()
		DateTimeZone origTimeZone = entryDateTime.getZone()
		DateTime newDateTime = new DateTime(m['date'], origTimeZone)

		if (!localTime.equals(newDateTime.toLocalTime()))
			return false

		boolean retVal = amount.precision == this.amountPrecision && this.amount == amount.amount \
				&& this.comment == (m['comment'] ?: '') && this.repeatTypeId == m['repeatType']?.id && this.units == (amount.units ?: '') \
				&& m['repeatEnd'] == this.repeatEnd

		return retVal
	}

	protected def entryMap() {
		def m = [:]

		m['repeatType'] = this.repeatType
		m['date'] = this.date
		m['timeZoneName'] = this.fetchTimeZoneName()
		m['datePrecisionSecs'] = this.datePrecisionSecs
		m['comment'] = this.comment
		m['repeatEnd'] = this.repeatEnd
		m['setName'] = this.setIdentifier?.toString()

		m['amount'] = new ParseAmount(this.tag, this.baseTag, this.amount, this.amountPrecision, this.units, UnitGroupMap.theMap.lookupDecoratedUnitRatio(this.units),
				this.durationType)

		log.debug "Creating entry map:" + m

		return m
	}

	protected updateRepeatType(Map m) {
		RepeatType newRepeatType = m['repeatType']
		if (!newRepeatType) {
			this.repeatTypeId = null
			this.repeatEnd = null
			return
		}
		DateTimeZone dateTimeZone = DateTimeZone.forID(m['timeZoneName'])
		Date repeatEndLimit = m['repeatEnd']
		if (!repeatEndLimit) {
			this.repeatEnd = null
			return
		}
		DateTime repeatEndLimitDateTime = new DateTime(repeatEndLimit, dateTimeZone)
		this.repeatTypeId = newRepeatType.id
		this.repeatEnd = IncrementingDateTime.lastRepeatBeforeOrEqualToDateTime(fetchDateTime(), repeatEndLimitDateTime, newRepeatType.intervalCode())?.toDate()
	}

	/*
	 * Create a new repeat entry on the baseDate + added interval, if needed, and cut the current entry at the next base date
	 */
	protected Entry createRepeatAfterInterval(Date baseDate, Map m, EntryCreateMap creationMap, EntryStats stats, int numIntervals) {
		if (m.repeatEnd != null && m.repeatEnd <= this.date) { // do not create repeat if no future repeats
			return null
		}

		RepeatType newRepeatType = m['repeatType']
		int newIntervalCode

		if (newRepeatType == null) {
			if (numIntervals > 0)
				return null // if no repeat type, interval not defined
			newIntervalCode = 0
		} else {
			newIntervalCode = newRepeatType.intervalCode()
		}

		DateTimeZone dateTimeZone = DateTimeZone.forID(m['timeZoneName'])

		// figure out local time and local date of new edited repeat time if it occurred on the passed-in baseDate in the passed-in time zone
		LocalTime mLocalTime = new DateTime(m['date'], dateTimeZone).toLocalTime()
		LocalDate baseLocalDate = new DateTime(baseDate, dateTimeZone).toLocalDate()
		DateTime newRepeatEntryDateTime = IncrementingDateTime.incrementDateTime(baseLocalDate.toDateTime(mLocalTime, dateTimeZone), numIntervals, newIntervalCode)
		Date newRepeatEntryDate = newRepeatEntryDateTime.toDate()

		DateTime currentDateTime = fetchDateTime()
		Date newRepeatEnd = IncrementingDateTime.lastRepeatBeforeDateTime(currentDateTime, newRepeatEntryDateTime, repeatType.intervalCode())?.toDate()

		if (newRepeatEnd != null && (this.repeatEnd == null || this.repeatEnd > newRepeatEnd)) {
			m['date'] = newRepeatEntryDate
			m['repeatEnd'] = this.repeatEnd

			this.repeatEnd = newRepeatEnd

			def newEntry = createSingle(this.userId, m, creationMap.groupForDate(newRepeatEntryDate, this.baseTag.description), stats)

			creationMap.add(newEntry)

			Utils.save(this, true)

			return newEntry
		}

		Date previousDate = IncrementingDateTime.incrementDateTime(currentDateTime, -1, newIntervalCode).toDate()

		// get dateTime of current repeatEnd in the passed-in time zone
		DateTime repeatEndDateTime = new DateTime(this.repeatEnd, dateTimeZone)
		Date oneIntervalAfterCurrentRepeatEnd = IncrementingDateTime.incrementDateTime(repeatEndDateTime, 1, newIntervalCode).toDate()

		// if no repeat end or the new repeat entry date is earlier than one day after the current repeat end, create new repeat entry
		// and change the end of this repeat entry.
		if (this.repeatEnd == null || newRepeatEntryDate < oneIntervalAfterCurrentRepeatEnd) {
			m['date'] = newRepeatEntryDate
			m['repeatEnd'] = this.repeatEnd

			def newEntry = createSingle(this.userId, m, creationMap.groupForDate(newRepeatEntryDate, this.baseTag.description), stats)

			creationMap.add(newEntry)

			// set the repeat end of this entry to the day before the current date

			if (previousDate >= this.date)
				this.setRepeatEnd(previousDate)
			else
				this.setRepeatEnd(this.date)

			Utils.save(this, true)

			return newEntry
		}

		return null
	}

	protected static Entry updateGhostSingle(Entry entry, Map m, EntryCreateMap creationMap, EntryStats stats, Date baseDate, boolean allFuture, boolean unGhost) {
		boolean entryIsOnBaseDate = EntryParserService.isToday(entry.getDate(), baseDate)

		if (entry.repeatParametersMatch(m) && ((!entry.isGhost()) || (!unGhost))) {
			return entry // no need to update when entry doesn't change
		}

		if (!m['repeatType']) {
			entry.doUpdate(m, stats)
			return entry
		}

		if (entryIsOnBaseDate) {
			if (allFuture) {
				entry.doUpdate(m, stats)

				if (entry.isGhost() && unGhost) {
					// create new ghosted entry one interval after the baseDate with the NEW entry data
					entry.createRepeatAfterInterval(baseDate, entry.entryMap(), creationMap, stats, 1)
					entry.unGhost(stats)
				}
				return entry // allFuture updates can just edit the entry and return if the entry is today
			} else {
				// create an entry one interval after the baseDate with the OLD entry data
				entry.updateRepeatType(m)

				entry.createRepeatAfterInterval(baseDate, entry.entryMap(), creationMap, stats, 1)

				if (entry.getRepeatEnd() == entry.getDate()) {
					m['repeatEnd'] = m['date']
				} else
					m['repeatEnd'] = entry.getRepeatEnd()

				entry.doUpdate(m, stats)

				if (unGhost) entry.unGhost(stats)

				return entry
			}
		} else {
			entry.updateRepeatType(m)

			if ((!allFuture) || (unGhost && entry.isGhost())) { // create another entry after the base date
				entry.createRepeatAfterInterval(baseDate, entry.entryMap(), creationMap, stats, 1)
			}

			Entry newEntry = entry.createRepeatAfterInterval(baseDate, m, creationMap, stats, 0)

			Utils.save(entry, true)
			stats.addEntry(entry)

			if (unGhost && newEntry?.isGhost())
				newEntry.unGhost()

			return newEntry
		}
	}

	/**
	 * Update entry, possibly updating multiple entries, given map of parameters (output of the parse method or manually created)
	 */
	protected static Entry updateSingle(Entry entry, Map m, EntryCreateMap creationMap, EntryStats stats, Date baseDate, boolean allFuture = true, boolean unGhost = true) {
		log.debug "Entry.updateSingle() entry:" + entry + ", m:" + m + ", baseDate:" + baseDate + ", allFuture:" + allFuture

		Long userId = entry.userId

		prepareStartDate(userId, m)

		Date updatedDate = m.date

		// intercept duration updating
		DurationType durationType = m['durationType']
		if (durationType != DurationType.NONE && durationType != null && ((RepeatType)m['repeatType'])?.isRepeatOrContinuous()) {
			// cannot combine duration edge and repeat
			m['durationType'] = DurationType.NONE
			m['tag'] = m['baseTag']
		} else if (durationType == DurationType.END) {
			Entry updated = completeDurationEntry(userId, m, stats, DurationType.END) {
				return findPreviousStartEntry(userId, m['baseTag'], updatedDate, entry.id)
			}

			if (updated != null) {
				Entry.delete(entry, stats)
				return updated
			}
		} else if (durationType == DurationType.START) {
			Entry updated = completeDurationEntry(userId, m, stats, DurationType.START) {
				return findNextEndEntry(userId, m['baseTag'], updatedDate, entry.id)
			}

			if (updated != null) {
				Entry.delete(entry, stats)
				return updated
			}
		}

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
						m['amount'] = a
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
						m['amount'] = a
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
				m['amount'] = a
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
				if (!a.isActive())
					continue
				m['amount'] = a
				Entry created = createSingle(userId, m, creationMap.groupForDate(m['date'], this.baseTag.description), stats)
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

		if (!entry.tag.hasEntry(oldUserId)) {
			User.removeFromCache(oldUserId, entry.tag)
		}
	}

	/**
	 * This is a bulk delete operation where all the entries of a particular User is marked deleted.
	 * Delete actually just hides the entries, rather than deleting them
	 */
	static void deleteUserEntries(Long userId) {
		if (!userId) {
			throw new InstanceNotFoundException()
		}

		Entry.executeUpdate("UPDATE Entry entry set entry.group = null, entry.userId = 0 where entry.userId = :userId",
				[userId: userId])

		TagStats.executeUpdate("UPDATE TagStats stats set stats.userId = 0 where stats.userId = :userId",
				[userId: userId])

		User.removeTagIdCache(userId)
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
				stats.addEntry(entry)
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
						stats.addEntry(entry)
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
				stats.addEntry(entry)

				if (endAfterToday) {
					DateTime newDateTime = TimeZoneId.nextDayFromDateSameTime(baseDate, entry.fetchDateTime())

					Date newDate = newDateTime.toDate()

					if (repeatEndDate == null || repeatEndDate >= newDate) {
						def m = [:]
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
							repeatTypes = [repeatId]

						// look for entry that already exists one day later
						// new date is one day after current baseDate

						String queryStr = "from Entry entry where entry.tag.description = :desc and entry.date = :entryDate and entry.userId = :userId and entry.repeatTypeId in (:repeatIds) order by entry.date desc"
						def entries = Entry.executeQuery(queryStr, [desc:entry.tag.getDescription(), entryDate:m['date'], userId:entry.getUserId(), repeatIds:repeatTypes], [max: 1])

						Entry e = entries[0]

						if (e != null) {
							return // subsequent repeat entry already exists
						}

						// create new entry one day after current baseDate
						m['timeZoneName'] = entry.fetchTimeZoneName()
						m['amount'] = new ParseAmount(entry.tag, entry.baseTag, entry.amount, entry.amountPrecision, entry.units,
								UnitGroupMap.theMap.lookupDecoratedUnitRatio(entry.units), entry.durationType)
						m['repeatType'] = entry.getRepeatType()
						m['comment'] = entry.getComment()
						m['datePrecisionSecs'] = entry.getDatePrecisionSecs()
						def retVal = Entry.createSingle(entry.getUserId(), m, creationMap.groupForDate(m['date'], entry.baseTag.description), stats)
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
				projections { count('id') }
				and {
					eq("userId", userId)
					eq("tag", tag)
					eq("amountPrecision", amountPrecision)
				}
			}
		} else {
			results = c {
				projections { count('id') }
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

	static def getAutocompleteTags(User user, Date now) {
		log.debug "Entry.getAutocompleteTags() userID:" + user.getId()

		def tagStats = TagStats.activeForUser(user, now)

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

	static def getAutocompleteTagsWithInfo(User user, Date now) {
		log.debug("Entry.getAutocompleteTagsWithInfo() userID:" + user.id)

		def tagStats = TagStats.activeForUser(user, now)

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
				tag.getLastUnits(),
				tag.getTypicallyNoAmount()
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

	static def START_DURATIONS = [
		DurationType.START,
	]

	static def STARTOREND_DURATIONS = [
		DurationType.START, DurationType.END
	]

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

	static Entry findPreviousStartEntry(Long userId, Tag baseTag, Date date, Long excludeId = null) {
		def c = Entry.createCriteria()

		// look for latest matching start entry prior to this one

		def results = c {
			and {
				eq("userId", userId)
				le("date", date)
				eq("baseTag", baseTag)
				eq("durationType", DurationType.START)
				if (excludeId != null) {
					not { eq("id", excludeId) }
				}
			}
			maxResults(1)
			order("date", "desc")
		}

		return results[0]
	}

	static Entry findNextEndEntry(Long userId, Tag baseTag, Date date, excludeId = null) {
		def c = Entry.createCriteria()

		// look for latest matching start entry prior to this one

		def results = c {
			and {
				eq("userId", userId)
				ge("date", date)
				eq("baseTag", baseTag)
				eq("durationType", DurationType.END)
				if (excludeId != null) {
					not { eq("id", excludeId) }
				}
			}
			maxResults(1)
			order("date", "asc")
		}

		return results[0]
	}

	/**
	 * If this is a new end entry, update duration entries
	 * @return
	 */
	protected def processAndSave(EntryStats stats) {
		try {
			Utils.save(this, true)
		} catch (Throwable t) {
			Utils.reportError("Invalid entry, failed to save: " + this, t)
			return
		}
	}

	protected createRepeat(EntryStats stats) {
		if (this.repeatTypeId == null)
			return

		if (this.repeatType.isContinuous()) {
			// search for matching continuous tag
			String queryStr = "select entry.id from entry entry, tag tag where entry.id != :entryId and entry.tag_id = tag.id and entry.user_id = :userId and entry.amount = :amount and entry.comment = :comment and tag.description = :desc and (entry.repeat_type_id & :continuousBit <> 0)"

			def entries = DatabaseService.get().sqlRows(queryStr, [entryId:this.id, desc:this.tag.description, amount:this.amount, comment:this.comment, userId:this.userId, continuousBit:RepeatType.CONTINUOUS_BIT])

			for (def v in entries) {
				if (v != null) {
					Entry e = Entry.get(v['id'])
					Entry.delete(e, stats) // delete matching entries, to avoid duplicates
					Utils.save(e, true)
				}
			}
		}
	}

	/**
	* Fetches all entries for a given date range and source
	* @param startDate
	* @param endDate
	* @param source
	* @param userId
	* @param max
	* @param offset
	* @return List of entries that match the given criteria
	*
	* @author Ujjawal Gayakwad
	*/
	static Map getImportedEntriesWithinRange(Date startDate, Date endDate, List<Identifier> setIdentifiers, Long userId,
			 int max, int offset) {
		log.debug "Entry.getImportedEntriesWithinRange() startDate: $startDate endDate: $endDate and setIdentifiers: " +
				"$setIdentifiers"
		if (!setIdentifiers) {
			return [entries: [], totalEntries: 0]
		}

		def criteria = Entry.createCriteria()
		PagedResultList entryList = criteria.list(max: max ?: 1000, offset: offset ?: 0) {
			between("date", startDate, endDate)
			'in'("setIdentifier", setIdentifiers)
			eq("userId", userId)
		}
		return [entries: entryList.resultList, totalEntries: entryList.totalCount]
	}

	protected Entry unGhost(EntryStats stats) {
		if (this.repeatType?.isGhost()) {
			this.repeatTypeId = this.repeatType.toggleGhost().id
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

		def m = [:]
		m['timeZoneName'] = timeZoneName
		m['amount'] = new ParseAmount(this.tag, this.baseTag, this.amount, this.amountPrecision, this.units, UnitGroupMap.theMap.lookupDecoratedUnitRatio(this.units),
				this.durationType)
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

		Entry retVal = Entry.createSingle(userId, m, creationMap.groupForDate(m['date'], this.baseTag.description), stats)

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

		def m = [:]
		m['timeZoneName'] = timeZoneName
		m['amount'] = new ParseAmount(this.tag, this.baseTag, this.amount, this.amountPrecision, this.units, UnitGroupMap.theMap.lookupDecoratedUnitRatio(this.units),
				this.durationType)
		m['repeatType'] = this.repeatType
		m['setName'] = setName
		m['comment'] = this.comment
		m['date'] = fetchCorrespondingDateTimeInTimeZone(currentBaseDate, currentTimeZone).toDate()
		log.debug "activateTemplateEntrySingle(), data: ${m['date']} and currentTimeZone Calculated: $currentTimeZone"
		m['datePrecisionSecs'] = this.datePrecisionSecs

		def retVal = Entry.createSingle(forUserId, m, creationMap.groupForDate(m['date'], this.baseTag.description), stats)

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
		if (m == null)
			return

		stats.addEntry(this)

		doUpdateSingle(m, stats)

		stats.addEntry(this)
	}

	protected void doUpdateSingle(Map m, EntryStats stats) {
		log.debug "Entry.updateSingle() this:" + this + ", m:" + m

		if (m == null)
			return

		ParseAmount amount = m['amount']

		def newTag = amount.tag

		if (newTag == null) return

		Tag newBaseTag = amount.baseTag
		DurationType newDurationType = amount.durationType

		amount.amount = amount.amount?.setScale(ENTRY_SCALE, BigDecimal.ROUND_HALF_UP)

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
			m['repeatEnd'] = ((RepeatType)m['repeatType']).makeRepeatEnd(m['repeatEnd'], m['date'], timeZoneId.toDateTimeZone())
		}

		setTag(amount.tag)
		setDate(m['date'])
		setStartDate(m['startDate'])
		setDatePrecisionSecs(m['datePrecisionSecs'])
		checkAmount(m)
		setAmount(amount.amount)
		setAmountPrecision(amount.precision)
		setUnits(amount.units?:'')
		setComment(m['comment']?:'')
		setRepeatTypeId(m['repeatType']?.id)

		// Only update setIdentifier if set name is available to prevent loosing existing identifier for device entries
		if (m["setName"]) {
			setSetIdentifier(Identifier.look(m['setName']))
		}

		setBaseTag(newBaseTag)
		setDurationType(newDurationType)
		setTimeZoneId((Integer)timeZoneId.getId())
		if (!repeatTypeId) {
			this.repeatEnd = null
		} else
			setRepeatEnd(m['repeatEnd'] ? m['repeatEnd'] : this.repeatEnd)

		Utils.save(this, true)

		if (updateRepeatType)
			createRepeat(stats)

		processAndSave(stats)

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

		return AlertNotification.pendingAlerts(user.id, startDate, endDate)
	}

	boolean isPrimaryEntry() {
		if (group == null || group.isEmpty()) {
			return true
		}

		if (durationType?.isStart())
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

	DateTime firstRepeatAfterDate(Date startDate) {
		return IncrementingDateTime.firstRepeatAfterDate(fetchDateTime(), startDate, repeatType.intervalCode())
	}

	static def fetchListData(User user, String timeZoneName, Date baseDate, Date now) {
		DateTimeZone currentTimeZone = DateTimeZone.forID(timeZoneName)
		DateTime baseDateTime = new DateTime(baseDate, currentTimeZone)
		LocalDate baseLocalDate = baseDateTime.toLocalDate()

		// get regular elements + timed repeating elements next
		String queryStr = "select distinct entry.id " \
				+ "from entry entry where entry.user_id = :userId and (((entry.date >= :startDate and entry.date < :endDate) and (entry.repeat_type_id is null or (entry.repeat_type_id & :continuousBit = 0))) or " \
				+ "(entry.date < :startDate and (entry.repeat_end is null or entry.repeat_end >= :startDate) and (not entry.repeat_type_id is null) and (entry.repeat_type_id & :repeatBits <> 0) and (entry.repeat_type_id & :continuousBit = 0)))"

		def queryMap = [userId:user.getId(), startDate:baseDate, endDate:baseDate + 1, repeatBits:RepeatType.REPEAT_BITS, continuousBit:RepeatType.CONTINUOUS_BIT]

		def rawResults = DatabaseService.get().sqlRows(queryStr, queryMap)

		def timedResults = []

		Set resultEntryIds = new HashSet<Long>()

		for (result in rawResults) {
			Long entryId = result['id']

			Entry entry = Entry.get(entryId)

			resultEntryIds.add(entryId)

			if (!entry.isPrimaryEntry())
				continue

			def desc = entry.getJSONDesc()

			// use JodaTime to figure out the correct time in the current time zone
			desc['timeZoneName'] = timeZoneName
			if (entry.repeatTypeId != null && ((entry.repeatTypeId & RepeatType.REPEAT_BITS) != 0)) { // adjust dateTime for repeat entries
				DateTime firstRepeat = entry.firstRepeatAfterDate(baseDate)

				if (firstRepeat.toLocalDate().equals(baseLocalDate)) {
					Date date = entry.fetchCorrespondingDateTimeInTimeZone(baseDate, currentTimeZone).toDate()
					if (entry.repeatEnd != null && entry.repeatEnd.getTime() < date.getTime()) // check if adjusted time is after repeat end
						continue
					desc['date'] = date
					desc['repeatType'] = entry.repeatTypeId
				} else
					continue
			}
			String setName = entry.setIdentifier?.toString()
			desc['setName'] = setName
			desc['sourceName'] = entry.sourceName
			timedResults.add(desc)
		}

		// get start duration elements (durations with start time on this day)
		queryStr = "select distinct entry.id " \
				+ "from entry entry where entry.user_id = :userId and ((entry.start_date >= :startDate and entry.start_date < :endDate) and (entry.repeat_type_id is null or (entry.repeat_type_id & :repeatAndContinuousBits = 0)))"

		queryMap = [userId:user.getId(), startDate:baseDate, endDate:baseDate + 1, repeatAndContinuousBits:RepeatType.REPEAT_AND_CONTINUOUS_BITS]

		rawResults = DatabaseService.get().sqlRows(queryStr, queryMap)

		for (result in rawResults) {
			Long entryId = result['id']

			Entry entry = Entry.get(entryId)

			if (resultEntryIds.contains(entryId))
				continue;

			if (entry.startDate != null && entry.date != null && (entry.date.getTime() - entry.startDate.getTime()) < DAYTICKS)
				continue;

			def desc = entry.getStartJSONDesc()

			desc['timeZoneName'] = timeZoneName
			desc['repeatType'] = null
			String setName = entry.setIdentifier?.toString()
			desc['setName'] = setName
			desc['sourceName'] = entry.sourceName
			timedResults.add(desc)
		}

		// get continuous repeating elements
		String continuousQueryStr = "select distinct entry.id as id " \
				+ "from entry entry where entry.user_id = :userId and " \
				+ "entry.repeat_end is null and (not entry.repeat_type_id is null) and (entry.repeat_type_id & :continuousBit <> 0)"

		def continuousResults = DatabaseService.get().sqlRows(continuousQueryStr, [userId:user.getId(), continuousBit:RepeatType.CONTINUOUS_BIT])

		def comparator = { a, b ->
			long aRepeatType = a['repeatType'] ?: 0L
			long bRepeatType = b['repeatType'] ?: 0L

			if ((aRepeatType & RepeatType.CONTINUOUS_BIT) > 0) {
				if ((bRepeatType & RepeatType.CONTINUOUS_BIT) > 0) {
					int cmp = (a['description'] <=> b['description'])
					if (cmp != 0)
						return cmp
					cmp = (a.amount <=> b.amount)
					if (cmp != 0)
						return cmp
					cmp = (a.units <=> b.units)
					if (cmp != 0)
						return cmp
					return 1
				}
				return -1
			} else if ((bRepeatType & RepeatType.CONTINUOUS_BIT) > 0) {
				return 1
			}

			long aTime = ((Date)a['date']).getTime()
			long bTime = ((Date)b['date']).getTime()

			if (aTime == bTime) {
				int cmp = (a['description'] <=> b['description'])
				if (cmp != 0)
					return cmp
				cmp = (a.amount <=> b.amount)
				if (cmp != 0)
					return cmp
				cmp = (a.units <=> b.units)
				if (cmp != 0)
					return cmp
				return 1
			}

			return aTime <=> bTime
		}

		SortedSet results = new TreeSet(comparator)

		// add continuous elements that aren't already in the timed list
		for (result in continuousResults) {
			Entry entry = Entry.get(result['id'])

			if (!entry.isPrimaryEntry())
				continue

			def desc = entry.getJSONDesc()
			desc['date'] = baseDate
			desc['repeatType'] = entry.repeatTypeId
			desc['timeZoneName'] = timeZoneName
			results.add(desc)
		}

		// add timed results
		for (result in timedResults) {
			results.add(result)
			log.debug "Entry id: " + result['id'] + " description: " + result['description']
		}

		return results
	}

	String getSourceName() {
		String setName = this.setIdentifier?.value
		return setName == null ? null : TagUnitMap.setIdentifierToSource(setName)
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
			if ((max == null) || (stats.maximum != null && stats.maximum > max))
				max = stats.maximum
		}

		return [min, max]
	}

	static def fetchPlotData(User user, def tagIds, Date startDate, Date endDate, Date currentTime, Map plotInfo = null) {
		log.debug "Entry.fetchPlotData() userId:" + user.getId() + ", tagIds:" + tagIds + ", startDate:" + startDate \
				+ ", endDate:" + endDate

		return DataRetriever.get().fetchPlotData(user.id, tagIds, startDate, endDate, currentTime, plotInfo)
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
			tagId:tag.id,
			description:((durationType?.isStartOrEnd()) && baseTag != null && (!repeatType?.isContinuous())) ? tag.getDescription() : baseTag.getDescription(),
			amount:amount,
			amountPrecision:fetchAmountPrecision(),
			units:units,
			comment:comment,
			repeatType:repeatTypeId,
			repeatEnd:repeatEnd]

		Integer index = 0

		Map amounts = [:]
		Map normalizedAmounts = [:]

		for (Entry e : fetchGroupEntries()) {
			UnitGroupMap.theMap.getJSONAmounts(userId, e.tag.id, amounts, normalizedAmounts, e.getAmount(), e.fetchAmountPrecision(), e.getUnits())
		}

		retVal['amounts'] = amounts
		retVal['normalizedAmounts'] = normalizedAmounts

		return retVal
	}

	Map getStartJSONDesc() {
		def retVal = [id:this.id,
			userId:userId,
			date:startDate,
			datePrecisionSecs:fetchDatePrecisionSecs(),
			timeZoneName:this.fetchTimeZoneName(),
			tagId:tag.id,
			description:baseTag.getDescription() + " start",
			amount:amount,
			amountPrecision:fetchAmountPrecision(),
			units:units,
			comment:comment,
			repeatType:repeatTypeId,
			repeatEnd:repeatEnd]

		Integer index = 0

		Map amounts = [:]
		Map normalizedAmounts = [:]

		for (Entry e : fetchGroupEntries()) {
			UnitGroupMap.theMap.getJSONAmounts(userId, e.tag.id, amounts, normalizedAmounts, e.getAmount(), e.fetchAmountPrecision(), e.getUnits())
		}

		retVal['amounts'] = amounts
		retVal['normalizedAmounts'] = normalizedAmounts

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

		DecoratedUnitRatio thisUnitRatio = UnitGroupMap.theMap.lookupDecoratedUnitRatio(units)
		DecoratedUnitRatio thatUnitRatio = UnitGroupMap.theMap.lookupDecoratedUnitRatio(eUnits)

		int thisPri = (thisUnitRatio?.getGroupPriority()) ?: -1
		int thatPri = (thatUnitRatio?.getGroupPriority()) ?: -1

		if (thisPri < thatPri) return -1
		else if (thisPri > thatPri) return 1

		thisPri = (thisUnitRatio?.affinity) ?: -1
		thatPri = (thatUnitRatio?.affinity) ?: -1

		if (thisPri < thatPri) return -1
		else if (thisPri > thatPri) return 1

		int v = this.units.compareTo(e.units)

		if (v == 0)
			return this.valueString().compareTo(e.valueString())

		return v
	}

	int hashCode() {
		return ((int)(userId?:0)) + ((int)date.getTime()) + ((int)tag.id) + (amount == null ? 0 : amount.toString().hashCode()) + ((int)(repeatTypeId?:0))
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
		} else if (!entry.userId) {
			result = [canDelete: false, messageCode: "entry.already.deleted"]
		} else {
			def userId = entry.getUserId();
			if (userId != user.getId()) {
				User entryOwner = User.get(userId)
				if (entryOwner?.virtual && Sprint.findByVirtualUserId(userId)?.hasAdmin(user.getId())) {
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
	NONE(0), START(DURATIONSTART_BIT),
	END(DURATIONEND_BIT),
	GENERATEDDURATION(DURATIONGENERATED_BIT), // deprecated
	GENERATEDSPRINT(DURATIONGENERATED_BIT | SPRINT_BIT)

	static final int DURATIONSTART_BIT = 1
	static final int DURATIONEND_BIT = 2
	static final int SPRINT_BIT = 4
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

	DurationType makeGenerated() {
		return get(this.id | DURATIONGENERATED_BIT)
	}

	DurationType makeSprint() {
		return get(this.id | SPRINT_BIT)
	}
}

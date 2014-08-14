package us.wearecurio.model

import groovy.time.*
import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.parse.ParseUtils
import us.wearecurio.parse.WordMatcher

import us.wearecurio.datetime.LocalTimeRepeater
import us.wearecurio.parse.PatternScanner
import us.wearecurio.services.DatabaseService
import us.wearecurio.utility.Utils
import us.wearecurio.model.Tag
import us.wearecurio.units.UnitGroupMap
import us.wearecurio.units.UnitGroupMap.UnitRatio

import java.util.ArrayList
import java.util.Map
import java.util.regex.Pattern
import java.util.LinkedList

import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.*

class Entry implements Comparable {

	private static def log = LogFactory.getLog(this)

	protected static final int MAXCOMMENTLENGTH = 100000
	protected static final int MAXUNITSLENGTH = 50
	protected static final int MAXSETNAMELENGTH = 50
	protected static final long HOURTICKS = 60L * 60000
	protected static final long DAYTICKS = HOURTICKS * 24
	protected static final long HALFDAYTICKS = HOURTICKS * 12
	protected static final long WEEKTICKS = DAYTICKS * 7

	protected static final int DEFAULT_DATEPRECISION_SECS = 3 * 60
	protected static final int VAGUE_DATE_PRECISION_SECS = 86400
	
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

	// NOTE: Unghosted repeat entries are repeated in an unghosted manner all
	// the way until their repeatEnd
	// but unghosted remind entries are only unghosted on their start date, and
	// not on subsequent dates
	
	// new end-of-entry repeat indicators:
	// repeat daily
	// delete repeat indicator to end repeat sequence
	// OR: repeat end
	
	// like a ghost, but it appears in plot data, it is just a repetition of a
	// prior
	// entry, must be activated to edit
	
	public static enum RepeatType { // IMPORTANT: there must be ghost entries
		// for all non-ghost entries and vice-versa
		// if you add more remind types, please edit the sql in
		// RemindEmailService
		DAILY(DAILY_BIT, 24L * 60L * 60000), WEEKLY(WEEKLY_BIT,
				7L * 24L * 60L * 60000), DAILYGHOST(DAILY_BIT | GHOST_BIT,
				24L * 60L * 60000), WEEKLYGHOST(WEEKLY_BIT | GHOST_BIT,
				7L * 24L * 60L * 60000), REMINDDAILY(REMIND_BIT | DAILY_BIT,
				24L * 60L * 60000), REMINDWEEKLY(REMIND_BIT | WEEKLY_BIT,
				7L * 24L * 60L * 60000), REMINDDAILYGHOST(REMIND_BIT
				| DAILY_BIT | GHOST_BIT, 24L * 60L * 60000), REMINDWEEKLYGHOST(
				REMIND_BIT | WEEKLY_BIT | GHOST_BIT, 7L * 24L * 60L * 60000),
		
		CONTINUOUS(CONTINUOUS_BIT, 1), CONTINUOUSGHOST(CONTINUOUS_BIT
				| GHOST_BIT, 1),
		
		DAILYCONCRETEGHOST(CONCRETEGHOST_BIT | DAILY_BIT, 24L * 60L * 60000), DAILYCONCRETEGHOSTGHOST(
				CONCRETEGHOST_BIT | GHOST_BIT | DAILY_BIT, 24L * 60L * 60000), WEEKLYCONCRETEGHOST(
				CONCRETEGHOST_BIT | WEEKLY_BIT, 7L * 24L * 60L * 60000), WEEKLYCONCRETEGHOSTGHOST(
				CONCRETEGHOST_BIT | GHOST_BIT | WEEKLY_BIT,
				7L * 24L * 60L * 60000),
		
		GHOST(GHOST_BIT, 0), NOTHING(0, 0);
		
		static final int DAILY_BIT = 1;
		static final int WEEKLY_BIT = 2;
		static final int REMIND_BIT = 4;
		static final int CONTINUOUS_BIT = 0x0100;
		static final int GHOST_BIT = 0x0200;
		static final int CONCRETEGHOST_BIT = 0x0400;
	
		final Integer id;
		final long duration;
		
		private static final Map<Integer, RepeatType> map = new HashMap<Integer, RepeatType>();
		
		static {
			for (RepeatType t : RepeatType.values()) {
				map.put(t.getId(), t)
			}
		}
		
		static RepeatType get(int id) {
			return map.get(id);
		}
		
		RepeatType(int id, long duration) {
			this.id = id;
			this.duration = duration;
		}
		
		public int getId() {
			return id;
		}
		
		public boolean isGhost() {
			return (this.id & GHOST_BIT) > 0;
		}
		
		public boolean isConcreteGhost() {
			return (this.id & CONCRETEGHOST_BIT) > 0;
		}
		
		public boolean isAnyGhost() {
			return (this.id & (GHOST_BIT | CONCRETEGHOST_BIT)) > 0;
		}
		
		public boolean isContinuous() {
			return (this.id & CONTINUOUS_BIT) > 0;
		}
		
		public boolean isDaily() {
			return (this.id & DAILY_BIT) > 0;
		}
		
		public boolean isWeekly() {
			return (this.id & WEEKLY_BIT) > 0;
		}
		
		public boolean isReminder() {
			return (this.id & REMIND_BIT) > 0;
		}
		
		public boolean isTimed() {
			return (this.id & (DAILY_BIT | WEEKLY_BIT | REMIND_BIT)) > 0;
		}
		
		public boolean isRepeat() {
			return (this.id & (DAILY_BIT | WEEKLY_BIT | REMIND_BIT | CONTINUOUS_BIT)) > 0;
		}
		
		public RepeatType unGhost() {
			return RepeatType.get(this.id & (~GHOST_BIT));
		}
		
		public RepeatType toggleGhost() {
			if (isGhost())
				return RepeatType.get(this.id & (~GHOST_BIT));
			return RepeatType.get(this.id | GHOST_BIT);
		}
		
		public RepeatType makeGhost() {
			return RepeatType.get(this.id | GHOST_BIT);
		}
		
		public RepeatType makeConcreteGhost() {
			return RepeatType.get(this.id | CONCRETEGHOST_BIT);
		}
		
		public RepeatType forUpdate() {
			if (isReminder())
				return RepeatType.get(this.id
						& (~(GHOST_BIT | CONCRETEGHOST_BIT)));
			return this;
		}
	}
	
	protected static def DAILY_IDS = [
		RepeatType.DAILY.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
		RepeatType.REMINDDAILY.getId(),
		RepeatType.REMINDDAILYGHOST.getId()
	]

	protected static def WEEKLY_IDS = [
		RepeatType.WEEKLY.getId(),
		RepeatType.WEEKLYCONCRETEGHOST.getId(),
		RepeatType.REMINDWEEKLY.getId(),
		RepeatType.REMINDWEEKLYGHOST.getId()
	]

	protected static def REPEAT_IDS = [
		RepeatType.DAILY.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
		RepeatType.WEEKLY.getId(),
		RepeatType.WEEKLYCONCRETEGHOST.getId()
	]

	protected static def REMIND_IDS = [
		RepeatType.REMINDDAILY.getId(),
		RepeatType.REMINDWEEKLY.getId(),
		RepeatType.REMINDWEEKLY.getId(),
		RepeatType.REMINDDAILYGHOST.getId(),
		RepeatType.REMINDWEEKLYGHOST.getId()
	]

	protected static def CONTINUOUS_IDS = [
		RepeatType.CONTINUOUS.getId(),
		RepeatType.CONTINUOUSGHOST.getId()
	]

	protected static def GHOST_IDS = [
		RepeatType.CONTINUOUSGHOST.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
		RepeatType.WEEKLYCONCRETEGHOST.getId(),
		RepeatType.REMINDDAILYGHOST.getId(),
		RepeatType.REMINDWEEKLYGHOST.getId()
	]

	protected static def DAILY_UNGHOSTED_IDS = [
		RepeatType.DAILY.getId(),
		RepeatType.DAILYCONCRETEGHOST.getId(),
	]

	protected static def UNGHOSTED_SINGULAR_IDS = [
		RepeatType.REMINDDAILY.getId(),
	]

	public static enum DurationType {
		NONE(0), START(DURATIONSTART_BIT), END(DURATIONEND_BIT),
				GHOSTSTART(DURATIONSTART_BIT | DURATIONGHOST_BIT),
				GHOSTEND(DURATIONEND_BIT | DURATIONGHOST_BIT);

		static final int DURATIONSTART_BIT = 1;
		static final int DURATIONEND_BIT = 2;
		static final int DURATIONGHOST_BIT = 16;
		
		final Integer id;

		DurationType(Integer id) {
			this.id = id;
		}
		
		public boolean isGhost() {
			return (this.id & DURATIONGHOST_BIT) != 0;
		}
		
		public DurationType unGhost() {
			if (this == GHOSTSTART) return START;
			else if (this == GHOSTEND) return END;
			
			return this;
		}
		
		public DurationType makeGhost() {
			if (this == START) return GHOSTSTART;
			else if (this == END) return GHOSTEND;
			
			return this;
		}
	}
	
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
		
		return tag.getDescription().substring(baseTag.getDescription().length() + 1)
	}
	
	public Iterable<Entry> fetchGroupEntries() {
		if (group != null)
			return group.getEntries()
		
		return [this]
	}

	static final MathContext mc = new MathContext(9)

	public static final int DEFAULT_AMOUNTPRECISION = 3

	/**
	* Create and save new entry
	*/
	static Entry createArgs(EntryCreationMap creationMap, EntryStats stats, Long userId, Date date, TimeZoneId timeZoneId, String description, BigDecimal amount, String units,
			String comment, String setName, int amountPrecision = DEFAULT_AMOUNTPRECISION) {
		
		Map<String,Object> m = [
			'date':date,
			'timeZoneId':(Integer) timeZoneId.getId(),
			'datePrecisionSecs':DEFAULT_DATEPRECISION_SECS,
			'amount':amount,
			'units':units,
			'comment':comment,
			'setName':setName,
			'amountPrecision': amountPrecision,
		]
		
		m['baseTag'] = Tag.look(description.toLowerCase())
		m['tag'] = UnitGroupMap.theMap.tagWithSuffixForUnits(m['baseTag'], units, 0)
		
		return createSingle(userId, m, creationMap.groupForDate(date), stats)
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

	protected static final BigDecimal smallDifference = new BigDecimal("0.01")

	// Prevent duplicates for imported entries
	protected static boolean hasDuplicate(Long userId, Map m) {
		if (m.setName == null)
			return false

		log.debug "Trying to find duplicate for " + userId + " for " + m

		Identifier setId = Identifier.look(m.setName)

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

		log.debug "No duplicate found"

		return false
	}

	/**
	 * 
	 * WORK STOPPED HERE:
		modify web and mobile app to correctly display multidimensional entry
	 */
	
	/*
	 * Class to return entry creation statistics for later updating of entry and tag statistics
	 */
	public static class EntryCreationMap {
		Map<Date, Entry> entries = new HashMap<Date, Entry>()
		
		public EntryCreationMap() {
		}
	
		public EntryGroup groupForDate(Date date) {
			Entry entry = entries.get(date)
			
			if (entry != null) {
				return entry.createOrFetchGroup()
			}
			
			return null
		}
		
		public void add(Entry entry) {
			entries.put(entry.getDate(), entry)
		}
	}
	
	/*
	 * Class to return entry creation statistics for later updating of entry and tag statistics
	 */
	public static class EntryStats {
		Long userId
		Set<Long> tagIds = new HashSet<Long>()
		Integer timeZoneId = null
		boolean batchCreation = false
		
		public EntryStats() {
		}
	
		public EntryStats(boolean batchCreation) {
			this.batchCreation = true
		}
	
		public EntryStats(Long userId) {
			this.userId = userId
		}
	
		public void addTag(Tag tag) {
			tagIds.add(tag.getId())
		}
		
		public void addTagId(Long tagId) {
			tagIds.add(tagId)
		}
		
		public ArrayList<TagStats> finish() { // after finalizing all entry creation, call this to update statistics
			ArrayList<TagStats> tagStats = new ArrayList<TagStats>()
			
			for (Long tagId: tagIds) {
				tagStats.add(TagStats.createOrUpdate(userId, tagId))
			}

			if (timeZoneId != null)
				User.setTimeZoneId(userId, timeZoneId)
				
			return tagStats
		}
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

		Integer timeZoneId = (Integer) m['timeZoneId'] ?: (Integer)TimeZoneId.look(m['timeZoneName']).getId()

		def tagUnitStats
		if (!m['units']) {
			// Using the most used unit in case the unit is unknown
			tagUnitStats = TagUnitStats.mostUsedTagUnitStats(userId, tag.getId())
			if (tagUnitStats) {
				m['units'] = tagUnitStats.unit
			}
		} else {
			TagUnitStats.createOrUpdate(userId, tag.getId(), m['units'])
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
		
		if (group != null) {
			group.add(entry)
			Utils.save(group, false)
		}

		entry.processAndSave(stats)
		
		entry.createRepeat()

		Entry generator = entry.fetchGeneratorEntry()

		if (generator != null)
			generator.updateDurationEntry(stats) // make sure duration entries remain consistent

		stats.addTag(entry.getTag())
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

		Integer timeZoneId = (Integer) m['timeZoneId'] ?: (Integer)TimeZoneId.look(m['timeZoneName']).getId()
		
		def amounts = m['amounts']
		
		EntryGroup entryGroup = null
		
		if (amounts.size() > 1) {
			entryGroup = new EntryGroup()
		}
		
		int index = 0
		
		Entry e = null
		for (ParseAmount amount : amounts) {
			amount.copyToMap(m)
			e = createSingle(userId, m, entryGroup, stats)
		}
		
		if (entryGroup != null) {
			Utils.save(entryGroup, false)
			return entryGroup.getEntries().first() // return first entry in sorted list
		}
		
		return e
	}
	
	protected boolean ifDurationRemoveFromGroup() {
		if (durationType != null && durationType != DurationType.NONE) {
			if (group != null) {
				group = null
				group.remove(this)
				Utils.save(this)
				Utils.save(group, false)
			}
			return true
		}

		return false
	}
	
	protected EntryGroup createOrFetchGroup() {
		if (ifDurationRemoveFromGroup()) {
			return false
		}
		
		EntryGroup group
		
		if (group != null) return group
		
		group = new EntryGroup()
		group.add(this)
		Utils.save(this, false)
		Utils.save(group, false)
		
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
			&& comment == (m['comment'] ?: '') && repeatType == m['repeatType'] && units == (m['units'] ?: '') \
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

	protected Entry createRepeatOnBaseDate(Date baseDate, Map m, EntryCreationMap creationMap, EntryStats stats, int addDays) {
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

			def newEntry = createSingle(this.userId, m, creationMap.groupForDate(newDate), stats)

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

	protected static Entry updateGhostSingle(Entry entry, Map m, EntryCreationMap creationMap, EntryStats stats, Date baseDate, boolean allFuture) {
		boolean isToday = isToday(entry.getDate(), baseDate)

		if (entry.repeatParametersMatch(m))
			return entry // no need to update when entry doesn't change

		if (isToday) {
			if (allFuture) {
				entry.doUpdate(m, stats)

				return entry // allFuture updates can just edit the entry and return
			} else {
				entry.createRepeatOnBaseDate(baseDate, entry.entryMap(), creationMap, stats, 1)

				if (entry.getRepeatEnd() == entry.getDate()) {
					m['repeatEnd'] = m['date']
				} else
					m['repeatEnd'] = entry.getRepeatEnd()

				entry.doUpdate(m, stats)

				return entry
			}
		} else {
			if (!allFuture) { // create another entry after the base date
				entry.createRepeatOnBaseDate(baseDate, entry.entryMap(), creationMap, stats, 1)
			}

			return entry.createRepeatOnBaseDate(baseDate, m, creationMap, stats, 0)
		}
	}

	/**
	 * Update entry, possibly updating multiple entries, given map of parameters (output of the parse method or manually created)
	 */
	protected static Entry updateSingle(Entry entry, Map m, EntryCreationMap creationMap, EntryStats stats, Date baseDate, boolean allFuture = true) {
		log.debug "Entry.updateSingle() entry:" + entry + ", m:" + m + ", baseDate:" + baseDate + ", allFuture:" + allFuture
		def repeatType = entry.getRepeatType()
		if (repeatType != null && repeatType.isRepeat()) {
			return updateGhostSingle(entry, m, creationMap, stats, baseDate, allFuture)
		} else {
			entry.doUpdate(m, stats)

			return entry
		}
	}

	/**
	 * Update entry, possibly updating multiple entries, given map of parameters (output of the parse method or manually created)
	 */
	public static Entry update(Entry entry, Map m, EntryStats stats, Date baseDate, boolean allFuture = true) {
		log.debug "Entry.update() entry:" + entry + ", m:" + m + ", baseDate:" + baseDate + ", allFuture:" + allFuture
		ArrayList<ParseAmount> amounts = new ArrayList<ParseAmount>()
		amounts.addAll(m['amounts'])
		ArrayList<ParseAmount> unmatchedEntries = new ArrayList<ParseAmount>()
		
		EntryGroup newEntryGroup = null
		EntryCreationMap creationMap = new EntryCreationMap()
		
		if (amounts.size() > 1)
			newEntryGroup = new EntryGroup()
		
		Long userId = entry.getUserId()
		
		// first, update entries that match the amounts already specified
		for (Entry e : entry.fetchGroupEntries()) {
			boolean foundMatch = false
			for (ParseAmount a : amounts) {
				if (a.matches(e)) {
					foundMatch = true
					amounts.remove(a)
					a.copyToMap(m)
					updateSingle(entry, m, creationMap, stats, baseDate, allFuture)
					break
				}
			}			
			if (!foundMatch) for (ParseAmount a : amounts) {
				if (a.matchesWeak(e)) {
					foundMatch = true
					amounts.remove(a)
					a.copyToMap(m)
					updateSingle(e, m, creationMap, stats, baseDate, allFuture)
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
			if (amounts.size() > 0) {
				ParseAmount a = amounts.remove(0)
				a.copyToMap(m)
				updateSingle(e, m, creationMap, stats, baseDate, allFuture)
			} else { // no more amounts, delete remaining entries
				deleteSingle(e, stats)
			}
		}
		// finally, create new entries in group for remaining amounts
		if (amounts.size() > 0) {
			for (ParseAmount a : amounts) {
				a.copyToMap(m)
				createSingle(userId, m, creationMap, stats)
			}
		}
	}

	/**
	 * Delete actually just hides the entries, rather than deleting them
	 */
	protected static deleteSingle(Entry entry, EntryStats stats) {
		log.debug "Entry.deleteSingle() entry:" + entry

		Entry updateDurationEntry = entry.preDeleteProcessing()

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
		
		stats.addTag(entry.getTag())

		if (updateDurationEntry != null) {
			updateDurationEntry.updateDurationEntry(stats)
		}
	}

	/**
	 * Delete actually just hides the entries, rather than deleting them
	 */
	public static delete(Entry entry, EntryStats stats) {
		log.debug "Entry.delete() entry:" + entry

		for (Entry e : entry.fetchGroupEntries()) {
			deleteSingle(e, stats)
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
	protected static deleteGhostSingle(Entry entry, EntryCreationMap creationMap, EntryStats stats, Date baseDate, boolean allFuture) {
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
						m['date'] = newDateTime.toDate()

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

						String queryStr = "from Entry entry where entry.tag.description = :desc and entry.date = :entryDate and entry.userId = :userId and entry.repeatType.id in (:repeatIds) order by entry.date desc limit 1"
						def entries = Entry.executeQuery(queryStr, [desc:m['tag'].getDescription(), entryDate:m['date'], userId:entry.getUserId(), repeatIds:repeatTypes])

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
	public static deleteGhost(Entry entry, EntryStats stats, Date baseDate, boolean allFuture) {
		log.debug "Entry.deleteGhost() entry:" + entry + " baseDate: " + baseDate + " allFuture: " + allFuture
		
		EntryCreationMap creationMap = new EntryCreationMap()
		
		for (Entry e : entry.fetchGroupEntries()) {
			deleteGhostSingle(e, creationMap, stats, baseDate, allFuture)
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

	public static def getTags(User user, type = BYALPHA) {
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

	public static def getAutocompleteTags(User user) {
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

	public static def getAutocompleteTagsWithInfo(User user) {
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
	public DateTimeZone fetchDateTimeZone() {
		TimeZoneId timeZoneId = TimeZoneId.fromId(timeZoneId)

		return timeZoneId.toDateTimeZone()
	}

	/**
	 * fetchLocalTimeInZone()
	 *
	 * Returns Joda time zone for this entry
	 */
	public DateTime fetchDateTimeInZone(DateTimeZone dateTimeZone) {
		return new DateTime(this.date, dateTimeZone)
	}

	/**
	 * fetchLocalTimeInZone()
	 *
	 * Returns Joda date time in current time zone
	 */
	public DateTime fetchDateTime() {
		return new DateTime(this.date, this.fetchDateTimeZone())
	}

	public String fetchTimeZoneName() {
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
		return fetchGeneratorEntry() ? true : false
	}

	/**
	 * fetchGeneratorEntry()
	 *
	 * Returns end entry corresponding to this generated entry if it is a generated entry
	 */
	protected Entry fetchGeneratorEntry() {
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
	public boolean fetchIsEnd() {
		return durationType.equals(DurationType.END)
	}

	/**
	 * fetchIsStart()
	 *
	 * Returns true if this entry is the start of a duration pair
	 */
	public boolean fetchIsStart() {
		return durationType.equals(DurationType.START)
	}

	/**
	 * retrieveDurationEntry()
	 *
	 * If this is an end entry, returns corresponding duration entry
	 * @return
	 */
	public Entry fetchDurationEntry() {
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
	public Entry fetchEndEntry() {
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
	public Entry fetchStartEntry() {
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
	public Entry findDurationEntry() {
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
				m['tag'] = baseTag
				m['amount'] = amount
				m['units'] = "hours"
				m['comment'] = this.comment ?: ""
				m['repeatType'] = null
				m['baseTag'] = baseTag
				m['durationType'] = DurationType.NONE
				m['amountPrecision'] = DEFAULT_AMOUNTPRECISION
				durationEntry = Entry.createSingle(userId, m, createOrFetchGroup(), stats)
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
					Entry.delete(subsequentDuration, null)
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
		"select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date < :entryDate and (time(entry.date) = time(:entryDate) or time(date_add(entry.date, interval 1 hour)) = time(:entryDate) or time(entry.date) = time(date_sub(:entryDate, interval 1 hour))) and entry.user_id = :userId and (entry.repeat_type in (:repeatIds)) order by entry.date desc limit 1",
		[desc:this.tag.getDescription(), userId:this.userId, repeatIds:DAILY_IDS])
	}

	protected findNextRepeatEntry() {
		return findNeighboringDailyRepeatEntry("select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date > :entryDate and (time(entry.date) = time(:entryDate) or time(date_add(entry.date, interval 1 hour)) = time(:entryDate) or time(entry.date) = time(date_sub(:entryDate, interval 1 hour))) and entry.user_id = :userId and (entry.repeat_type in (:repeatIds)) order by entry.date asc limit 1",
		[desc:this.tag.getDescription(), userId:this.userId, repeatIds:DAILY_IDS])
	}

	protected createRepeat() {
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

	protected Entry unGhost(EntryStats stats) {
		if (this.repeatType?.isGhost()) {
			this.repeatType = this.repeatType.toggleGhost()

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
	
	protected Entry activateGhostEntrySingle(Date currentBaseDate, Date nowDate, String timeZoneName, DateTimeZone currentTimeZone, EntryCreationMap creationMap,
			EntryStats stats) {
		long dayStartTime = currentBaseDate.getTime()
		def now = nowDate.getTime()
		long diff = now - dayStartTime
		long thisDiff = this.date.getTime() - dayStartTime

		if (this.repeatType == null)
			return null

		if (!this.repeatType.isContinuous() && (thisDiff >=0 && thisDiff < DAYTICKS)) {
			// activate this entry

			this.unGhost(stats)

			return this
		}

		def m = [:]
		m['timeZoneName'] = timeZoneName
		m['tag'] = this.tag
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
			def retVal = Entry.createSingle(userId, m, creationMap.groupForDate(m['date']), stats)

			Utils.save(retVal, true)
			return retVal
		} else { // this repeat element isn't continuous
			m['comment'] = this.comment
			m['repeatType'] = this.repeatType
			m['date'] = fetchCorrespondingDateTimeInTimeZone(currentBaseDate, currentTimeZone).toDate()
			m['datePrecisionSecs'] = this.datePrecisionSecs
			def retVal = Entry.createSingle(userId, m, creationMap.groupForDate(m['date']), stats)

			retVal.unGhost(stats)

			return retVal
		}
	}

	public Entry activateGhostEntry(Date currentBaseDate, Date nowDate, String timeZoneName, EntryStats stats) {
		if (this.repeatType == null)
			return null
			
		DateTimeZone currentTimeZone = TimeZoneId.look(timeZoneName).toDateTimeZone()
		
		EntryCreationMap creationMap = new EntryCreationMap()
		
		Entry firstActivated = null
		
		for (Entry e : fetchGroupEntries()) {
			Entry activated = e.activateGhostEntrySingle(currentBaseDate, nowDate, timeZoneName, currentTimeZone, creationMap, stats)
			if (firstActivated == null) firstActivated = activated
		}
		
		return firstActivated
	}

	/**
	 * Update existing entry and save
	 */
	protected doUpdate(Map m, EntryStats stats) {
		long oldTagId = this.tag.getId()
		doUpdateSingle(m, stats)
		long newTagId = this.tag.getId()
		
		stats.addTagId(oldTagId)
		stats.addTagId(newTagId)
	}

	protected doUpdateSingle(Map m, EntryStats stats) {
		log.debug "Entry.doUpdateSingle() this:" + this + ", m:" + m

		if (m == null) return null

		def newTag = m['tag']

		if (newTag == null) return null
		
		Tag newBaseTag = m['baseTag']
		DurationType newDurationType = m['durationType']

		def updateDurationEntry = null

		if (!date.equals(m['date'])) {
			updateDurationEntry = preDeleteProcessing()
		}

		def descriptionChanged = tag.getId() != newTag.getId()

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

		setTag(m['tag'])
		setDate(m['date'])
		setDatePrecisionSecs(m['datePrecisionSecs'])
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
			updateDurationEntry.updateDurationEntry(stats)
		}
		
		processAndSave(stats)

		ifDurationRemoveFromGroup()

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

	public static def fetchReminders(User user, Date startDate, long intervalSecs) {
		Date endDate = new Date(startDate.getTime() + intervalSecs * 1000L)

		String queryStr = "select distinct entry.id " \
		+ "from entry entry where entry.user_id = :userId and " \
		+ "entry.date < :endDate and (entry.repeat_end is null or entry.repeat_end >= :startDate) and (not entry.repeat_type is null) and (entry.repeat_type & :remindBit <> 0) " \
		+ "and (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400 < :interval"

		def rawResults = DatabaseService.get().sqlRows(queryStr, [userId:user.getId(), endDate:endDate, startDate:startDate, interval:intervalSecs, remindBit:RepeatType.REMIND_BIT])
	}
	
	public boolean isPrimaryEntry() {
		if (group == null) {
			return true
		}
		
		if (durationType == DurationType.START)
			return true

		Entry firstEntry = group.getEntries().first()
				
		if (getId() == firstEntry?.getId()) {
			return true
		}
		
		return false
	}

	protected static long abs(long x) { return x < 0 ? -x : x }

	public static def fetchListData(User user, String timeZoneName, Date baseDate, Date now) {
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
			
			if (!entry.isPrimaryEntry())
				continue
			
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

			if (!entry.isPrimaryEntry())
				continue
			
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
			log.debug "Entry id: " + result['id'] + " tag: " + result['tag']
		}

		return results
	}

	public static def fetchListDataNoRepeats(User user, Date date) {
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
			
			if (!entry.isPrimaryEntry())
				continue
			
			def desc = entry.getJSONDesc()
			results.add(desc)
		}

		return Utils.listJSONDesc(results)
	}

	// limit repeat generation to 300 entries, for now
	protected static final int MAX_PLOT_REPEAT = 300

	protected static def generateRepeaterEntries(def repeaters, long endTimestamp, def results) {
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

	protected static def fetchAverageTime(User user, def tagIds, Date startDate, Date endDate, Date currentTime, DateTimeZone currentTimeZone) {
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

	public static def fetchPlotData(User user, def tagIds, Date startDate, Date endDate, Date currentTime, String timeZoneName) {
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
			UnitRatio unitRatio = mostUsedUnitRatioForTags?.lookupUnitRatio(result['units'])
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

	public static def fetchSumPlotData(User user, def tagIds, Date startDate, Date endDate, Date currentDate, String timeZoneName) {
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
	
	public static def parseInt(String str) {
		return str == null ? null : Integer.parseInt(str)
	}

	public static def parseMeta(String entryStr) {
		return parse(null, null, entryStr, null, false)
	}

	protected static boolean isSameDay(Date time1, Date time2) {
		def t1 = time1.getTime()
		def t2 = time2.getTime()
		if (t1 < t2) return false
		if (t2 - t1 > DAYTICKS) return false

		return true
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

	protected static final Pattern timePattern    = ~/(?i)^(@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)\b|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?\b|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm)\b/
	protected static final Pattern timeEndPattern = ~/(?i)^(@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)\b|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?\b|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm)\b$/
	protected static final Pattern tagWordPattern = ~/(?i)^([^0-9\(\)@\s\.:=][^\(\)@\s:=]*)/
	protected static final Pattern commentWordPattern = ~/^([^\s]+)/
	protected static final Pattern amountPattern = ~/(?i)^(-?\.\d+|-?\d+\.\d+|\d+|_\b|-\b|__\b|___\b|zero\b|yes\b|no\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b)(\s*\/\s*(-?\.\d+|-?\d+\.\d+|\d+|_\b|-\b|__\b|___\b|zero\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b))?/
	protected static final Pattern endTagPattern = ~/(?i)^(yes|no)\b/
	protected static final Pattern unitsPattern = ~/(?i)^(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s+([^0-9\(\)@\s\.:][^\(\)@\s:]*))*)\b/
	protected static final Pattern tagAmountSeparatorPattern = ~/(?i)^[:=]\s*/
	
	protected static def numberMap = ['zero' : '0', 'one':'1', 'two':'2', 'three':'3', 'four':'4', 'five':'5', 'six':'6', 'seven':'7', 'eight':'8', 'nine':'9']

	/**
	 * Regex pattern to match repeat modifiers
	 * 
	 * If you update repeatPattern, you need to update repeatMap
	 */
	protected static final Pattern repeatPattern = ~/^(repeat daily|repeat weekly|remind daily|remind weekly|reminder daily|reminder weekly|daily repeat|daily remind|daily reminder|weekly repeat|weekly remind|weekly reminder|repeat|pinned|favorite|remind|reminder|daily|weekly)\b/
	
	protected static final int REPEATMAP_SYNONYM = 0
	protected static final int REPEATMAP_TYPE = 1
	
	protected static Map<String, ArrayList<Object>> repeatMap = [
		'repeat':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'repeat daily':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'repeat weekly':['repeat weekly', RepeatType.WEEKLYCONCRETEGHOST],
		'pinned':['favorite', RepeatType.CONTINUOUSGHOST],
		'favorite':['favorite', RepeatType.CONTINUOUSGHOST],
		'remind':['remind', RepeatType.REMINDDAILYGHOST],
		'remind daily':['remind', RepeatType.REMINDDAILYGHOST],
		'remind weekly':['remind weekly', RepeatType.REMINDWEEKLYGHOST],
		'reminder':['remind', RepeatType.REMINDDAILYGHOST],
		'reminder daily':['remind', RepeatType.REMINDDAILYGHOST],
		'reminder weekly':['remind weekly', RepeatType.REMINDWEEKLYGHOST],
		'daily':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'daily repeat':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'daily remind':['remind', RepeatType.REMINDDAILYGHOST],
		'daily reminder':['remind', RepeatType.REMINDDAILYGHOST],
		'weekly':['repeat weekly', RepeatType.WEEKLYCONCRETEGHOST],
		'weekly repeat':['repeat weekly', RepeatType.WEEKLYCONCRETEGHOST],
		'weekly remind':['remind weekly', RepeatType.REMINDWEEKLYGHOST],
		'weekly reminder':['remind weekly', RepeatType.REMINDWEEKLYGHOST],
	]

	/**
	 * Regex pattern to match duration modifiers
	 * 
	 * If you update durationPattern, you need to update durationMap
	 */
	protected static final Pattern durationPattern = ~/^(start|starts|begin|begins|starting|beginning|started|begun|began|end|ends|stop|stops|finish|finished|ended|stopped|stopping|ending|finishing)\b/
	
	protected static final int DURATIONMAP_SYNONYM = 0
	protected static final int DURATIONMAP_TYPE = 1
	
	protected static final Map<String, ArrayList<Object>> durationMap = [
		'start':['start', DurationType.START],
		'starts':['start', DurationType.START],
		'begin':['start', DurationType.START],
		'begins':['start', DurationType.START],
		'starting':['start', DurationType.START],
		'beginning':['start', DurationType.START],
		'started':['start', DurationType.START],
		'begun':['start', DurationType.START],
		'began':['start', DurationType.START],
		'end':['end', DurationType.END],
		'ends':['end', DurationType.END],
		'stop':['end', DurationType.END],
		'stops':['end', DurationType.END],
		'finish':['end', DurationType.END],
		'finished':['end', DurationType.END],
		'ended':['end', DurationType.END],
		'stopped':['end', DurationType.END],
		'stopping':['end', DurationType.END],
		'ending':['end', DurationType.END],
		'finishing':['end', DurationType.END],
	]
	
	/**
	 * Regex pattern to match duration synonyms
	 * 
	 * If you update durationSynonymPattern, you need to update durationSynonymMap
	 */
	protected static final Pattern durationSynonymPattern = ~/^(wake up|went to sleep|go to sleep|wake|woke|awakened|awoke|slept)\b/
	
	protected static final int DURATIONSYNONYM_TAG = 0
	protected static final int DURATIONSYNONYM_SUFFIX = 1
	protected static final int DURATIONSYNONYM_TYPE = 2
	
	protected static final Map<String, ArrayList<Object>> durationSynonymMap = [
		'wake':['sleep', 'end', DurationType.END],
		'wake up':['sleep', 'end', DurationType.END],
		'woke':['sleep', 'end', DurationType.END],
		'awakened':['sleep', 'end', DurationType.END],
		'awoke':['sleep', 'end', DurationType.END],
		'went to sleep':['sleep', 'start', DurationType.START],
		'go to sleep':['sleep', 'start', DurationType.START],
		'slept':['sleep', 'start', DurationType.START],
	]

	protected static boolean isToday(Date time, Date baseDate) {
		boolean today = false

		if (baseDate != null) {
			long diff = time.getTime() - baseDate.getTime();

			if (diff >= 0 && diff < DAYTICKS) {
				today = true
			}
		}

		return today
	}
		
	public static class ParseAmount {
		Tag tag // tag including units suffix
		String suffix
		BigDecimal amount
		int precision
		String units
		
		ParseAmount(BigDecimal amount, int precision) {
			this.amount = amount
			this.precision = precision
		}
		
		ParseAmount setTagAndBaseTag(Tag tag, Tag baseTag) {
			this.tag = tag
			if (baseTag == null || baseTag == tag) {
				this.suffix = ""
			} else {
				this.suffix = tag.getDescription().substring(baseTag.getDescription().length() + 1)
			}
			return this
		}
		
		boolean matches(Entry e) {
			return e.getTag().getId() == tag.getId()
		}
		
		boolean matchesWeak(Entry e) {
			return e.fetchSuffix().equals(suffix) || e.getUnits().equals(units)
		}
		
		Map copyToMap(Map m) {
			m['tag'] = tag
			m['amount'] = amount
			m['amountPrecision'] = precision
			m['units'] = units
			
			return m
		}
	}
	
	public static def parse(Date time, String timeZoneName, String entryStr, Date baseDate, boolean defaultToNow, boolean forUpdate = false) {
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
			def noonmid = scanner.group(2)
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
			hours = parseInt(scanner.group(14) ?: (scanner.group(9) ?: (scanner.group(3) ?: '0')))
			if (hours == 12) hours = 0
			minutes = parseInt(scanner.group(11) ?: (scanner.group(6) ?: '0'))
			if (scanner.group(8).equals('p') || scanner.group(13).equals('p') || scanner.group(18).equals('pm')) {
				foundAMPM = true
				hours += 12
			} else if (scanner.group(8).equals('a') || scanner.group(13).equals('a') || scanner.group(18).equals('am')) {
				foundAMPM = true
			}
		}

		LinkedList<String> words = new LinkedList<String>()
		String suffix = ''
		String repeatSuffix = ''
		
		boolean foundRepeat = false
		boolean foundDuration = false

		Closure repeatClosure = {
			foundRepeat = true
			
			String modifier = scanner.group(1)
			
			ArrayList<Object> info = repeatMap[modifier]
			retVal['repeatType'] = (RepeatType) info[REPEATMAP_TYPE]
			repeatSuffix = (String) info[REPEATMAP_SYNONYM]
		}
		
		Closure durationClosure = {
			foundDuration = true
			
			String modifier = scanner.group(1)
			
			ArrayList<Object> info = durationMap[modifier]
			retVal['durationType'] = (DurationType) info[DURATIONMAP_TYPE]
			suffix =  (String) info[DURATIONMAP_SYNONYM]
		}

		Closure amountClosure = {
			String amountStr
			ArrayList<String> amountStrs = []
			int amountIndex = amounts.size()
			boolean twoDAmount = false
			
			amountStrs.add(scanner.group(1))
			amountStr = scanner.group(3)
			
			if (amountStr) {
				amountStrs.add(amountStr)
				twoDAmount = true
			}
			
			for (String amount in amountStrs) {
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
			
			if ( !((!foundRepeat) && scanner.matchField(repeatPattern, repeatClosure))
					|| ((!foundDuration) && scanner.matchField(durationPattern, durationClosure)) ) {
				boolean foundUnits = false
				if (!scanner.matchField(timePattern, timeClosure)) {
					scanner.matchField(unitsPattern) {
						String unitsStr = scanner.group(1)
						((ParseAmount)amounts[amountIndex++]).setUnits(unitsStr)
						if (amountIndex < amounts.size())
							((ParseAmount)amounts[amountIndex++]).setUnits(unitsStr)
						foundUnits = true
					}
				}
			}
		}
		
		Closure durationSynonymClosure = {
			foundDuration = true
			
			String modifier = scanner.group(1)
			
			ArrayList<Object> info = durationSynonymMap[modifier]
			retVal['durationType'] = (DurationType) info[DURATIONSYNONYM_TYPE]
			suffix = (String) info[DURATIONSYNONYM_SUFFIX]
			words.add((String) info[DURATIONSYNONYM_TAG])
		}

		scanner.skipWhite()

		scanner.matchField(timePattern, timeClosure)

		boolean endTag = false
		boolean tryStart
		boolean skipModifiers = false
		
		while (true) {
			boolean tagAmountSeparator = false
			boolean tryFollowedByTagWord = false
			boolean tryFollowedByTagEnd = false
			
			if (!skipModifiers) {
				// look for repeat pattern, but only look for these at start
				if ( ((!foundRepeat) && scanner.tryField(repeatPattern, repeatClosure))
						|| ((!foundDuration) && scanner.tryField(durationPattern, durationClosure)) ) {
					if (words.size() == 0) {
						// conditioned on: followed by tag word
						tryFollowedByTagWord = true
					} else {
						// conditioned on: followed by tag end
						tryFollowedByTagEnd = true
					}
				}
			} else
				skipModifiers = false
			
			// look for duration synonym at start of tag
			if (words.size() == 0 && (!foundDuration)) {
				scanner.matchField(durationSynonymPattern, durationSynonymClosure)
			}
			// match next word
			if (scanner.matchField(tagWordPattern)) {
				if (tryFollowedByTagWord) {
					// success at this condition
					scanner.confirmTry()
				} else if (tryFollowedByTagEnd) {
					// failed this condition, backtrack try, keep processing words
					scanner.backtrackTry()
					skipModifiers = true
					continue
				}
				words.add(scanner.group(1))
	
				if (scanner.match(tagAmountSeparatorPattern)) {
					endTag = true
					tagAmountSeparator = true
				}
				
				// allow time to appear before amount
				if ((!foundTime) && scanner.matchField(timePattern, timeClosure)) {
					if (!endTag) { // tag separator can be after time
						scanner.match(tagAmountSeparatorPattern) { tagAmountSeparator = true }
					}
					endTag = true
				}
			} else
				endTag = true
			
			if (endTag) {
				if (tryFollowedByTagWord) {
					// failed at this condition
					scanner.backtrackTry() // start over processing words
					skipModifiers = true
					continue
				} else if (tryFollowedByTagEnd) {
					// success at this condition
					scanner.confirmTry()
				}
				break
			}
		}

		// look for amount after tag		
		if (words.size() > 0) {
			while (scanner.matchField(amountPattern, amountClosure)) {
				// if no units, break, otherwise attempt to match another amount/units pair
				if (((ParseAmount)amounts[amounts.size() - 1]).getUnits() == null)
					break
			}
		}

		String description = ParseUtils.implode(words).toLowerCase()
		
		if (amounts.size() == 0) {
			amounts.add(new ParseAmount(new BigDecimal(1, mc), -1))
		}

		if (!foundTime) {
			scanner.matchField(timePattern, timeClosure)
		}

		words = new LinkedList<String>()

		skipModifiers = false
		
		while (true) {
			boolean tryFollowedByTagEnd = false
			
			if (!skipModifiers) {
				// look for repeat pattern, but only look for these at start or end
				if ( ((!foundRepeat) && scanner.tryField(repeatPattern, repeatClosure))
						|| ((!foundDuration) && scanner.tryField(durationPattern, durationClosure)) ) {
					if (words.size() > 0) {
						// conditioned on: followed by tag end
						tryFollowedByTagEnd = true
					}
				}
			} else
				skipModifiers = false
			
			// match next word
			if (scanner.matchField(commentWordPattern)) {
				if (tryFollowedByTagEnd) {
					// failed this condition, backtrack try, keep processing words
					scanner.backtrackTry()
					skipModifiers = true
					continue
				}
				words.add(scanner.group(1))
				
				// allow time to appear at end of comment
				if ((!foundTime) && scanner.matchField(timeEndPattern, timeClosure)) {
					endTag = true
				}
			} else
				endTag = true
			
			if (endTag) {
				if (tryFollowedByTagEnd) {
					// success at this condition
					scanner.confirmTry()
				}
				break
			}
		}

		String comment = ''
		if (words.size() > 0) {
			comment = ParseUtils.implode(words)
		}
		
		comment = ''
		
		if (repeatSuffix)
			if (comment)
				comment += ' ' + repeatSuffix
			else
				comment = repeatSuffix
		
		if (!foundTime) {
			if (date != null) {
				date = time;
				if (!(defaultToNow && today && (!forUpdate))) { // only default to now if entry is for today and not editing
					date = new Date(baseDate.getTime() + HALFDAYTICKS);
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

		for (int i = 0; i < amounts.size(); ++i) {
			if (amounts[i].getUnits()?.equals("at"))
				amounts[i].setUnits('')
			String units = amounts[i].getUnits()
			if (units?.length() > MAXUNITSLENGTH) {
				amounts[i].setUnits(units.substring(0, MAXUNITSLENGTH))
			}
		}
		
		// if comment is duration modifier, append to tag name
		// only do this on exact match, to avoid comments being accidentally interpreted as duration modifiers

		if (description?.length() > Tag.MAXLENGTH) {
			def lastSpace = description.lastIndexOf(' ', Tag.MAXLENGTH - 1)
			description = description.substring(0, lastSpace)

			if (comment.length() > 0) {
				if (comment.charAt(0) == '(') {
					if (comment.charAt(comment.length() - 1) == ')') {
						comment = comment.substring(1, comment.length() - 2)
					} else {
						comment = comment.substring(1)
					}
				}
			}
			comment = '(' + (comment ? comment + ' ' : '') + description.substring(lastSpace + 1) + ')'
		}

		if (comment?.length() > MAXCOMMENTLENGTH) {
			comment = comment.substring(0, MAXCOMMENTLENGTH)
		}
		
		retVal['comment'] = comment

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

		retVal['tag'] = Tag.look(description)
		String baseTagDescription = description
		if (suffix) baseTagDescription += ' ' + suffix
		retVal['baseTag'] = Tag.look(baseTagDescription)
		
		int index = 0
		for (ParseAmount amount : amounts)
			amount.setTagAndBaseTag(UnitGroupMap.theMap.tagWithSuffixForUnits(retVal['baseTag'], amount.getUnits(), index++), retVal['baseTag'])
		
		retVal['amounts'] = amounts

		return retVal
	}
	
	public Integer fetchDatePrecisionSecs() {
		return datePrecisionSecs == null ? 60 : datePrecisionSecs;
	}

	public Integer fetchAmountPrecision() {
		return amountPrecision == null ? DEFAULT_AMOUNTPRECISION : amountPrecision;
	}

	public  getDescription() {
		return tag.getDescription()
	}

	protected static DateFormat gmtFormat
	protected static long GMTMIDNIGHTSECS

	static {
		gmtFormat = new SimpleDateFormat("k:mm")
		gmtFormat.setTimeZone(Utils.createTimeZone(0, "GMT", false))
		GMTMIDNIGHTSECS = new SimpleDateFormat("yyyy-MM-dd h:mm a z").parse("2010-01-01 12:00 AM GMT").getTime() / 1000
	}

	public String exportString() {
		return description + " " + (amount == null ? "none " : amountPrecision == 0 ? (amount ? "yes " : "no ") : (amountPrecision > 0 ? amount + " " : '')) + (units ? units + ' ' : '') + (datePrecisionSecs < 40000 ? (date ? gmtFormat.format(date) : '') + ' ' : '') + comment
	}

	/**
	 * Scan existing entries and create duration entries if they don't exist
	 * @param user
	 */
	public static void updateDurationEntries(User user) {
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
	public Map getJSONDesc() {
		def retVal = [id:this.id,
			userId:userId,
			date:date,
			datePrecisionSecs:fetchDatePrecisionSecs(),
			timeZoneName:this.fetchTimeZoneName(),
			description:(durationType != DurationType.NONE) ? baseTag.getDescription() : tag.getDescription(),
			amount:amount,
			amountPrecision:fetchAmountPrecision(),
			units:units,
			comment:comment,
			repeatType:repeatType != null ? repeatType.getId() : 0L]
		
		Integer i = 0
		
		Map amounts = [:]
		
		for (Entry e : fetchGroupEntries()) {
			amounts = [i:[amount:e.getAmount(), amountPrecision:e.fetchAmountPrecision(), units:e.getUnits()]]
			i++
		}
		
		retVal['amounts'] = amounts
		
		return retVal
	}

	/**
	 * Get minimal data
	 */
	protected static final int SHORT_DESC_DATE = 0
	protected static final int SHORT_DESC_AMOUNT = 1
	protected static final int SHORT_DESC_DESCRIPTION = 2

	public Map getJSONShortDesc() {
		return [
			date,
			amount,
			getDescription()
		];
	}

	public String getDateString() {
		return Utils.dateToGMTString(date)
	}

	public String toString() {
		return "Entry(id: ${id ?: 'un-saved'}, userId:" + userId \
				+ ", date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneName:" + TimeZoneId.fromId(timeZoneId).getName() \
				+ ", baseTag:" + getBaseTag().getDescription() \
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
				+ ", baseTag:" + getBaseTag().getDescription() \
				+ ", description:" + getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatType?.getId() \
				+ ", repeatEnd:" + Utils.dateToGMTString(repeatEnd) \
				+ ")"
	}

	public String contentString() {
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

	public int compareTo(Object obj) {
		if (!obj instanceof Entry) {
			return -1
		}
		
		Entry e = (Entry) obj
		String eUnits = e.getUnits()
		if (!units) {
			if (eUnits) {
				return -1 // no units always comes first
			}
			return 0 // neither has units, undefined order
		}
		
		if (!eUnits) {
			return 1 // no units always comes first
		}
		
    	int thisPri = UnitGroupMap.theMap.unitRatioForUnits(units)?.getGroupPriority() ?: -1
		int thatPri = UnitGroupMap.theMap.unitRatioForUnits(eUnits)?.getGroupPriority() ?: -1
		
		return thisPri < thatPri ? -1 : (thisPri > thatPri ? 1 : 0)
	}

	public boolean equals(Object o) {
		if (!o instanceof Entry)
			return false
		return ((Entry)o).getId() == id
	}
}

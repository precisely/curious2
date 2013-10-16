package us.wearecurio.model;

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.parse.PatternScanner
import us.wearecurio.services.DatabaseService
import us.wearecurio.utility.Utils

import java.util.regex.Pattern
import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

class Entry {
	
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
		amount(scale:9, nullable:true)
		date(nullable:true)
		datePrecisionSecs(nullable:true)
		amountPrecision(nullable:true)
		repeatType(nullable:true)
		repeatEnd(nullable:true)
		units(maxSize:MAXUNITSLENGTH)
		comment(maxSize:MAXCOMMENTLENGTH)
		setName(maxSize:MAXSETNAMELENGTH, nullable:true)
		tweetId(nullable:true)
		baseTag(nullable:true)
		durationType(nullable:true)
		timeZoneOffsetSecs(nullable:true)
	}
	
	static mapping = {
		table 'entry'
		userId column:'user_id', index:'user_id_index'
		tweetId column:'tweet_id', index:'tweet_id_index'
		date column:'date', index:'date_index'
		tag column:'tag_id', index:'tag_id_index'
		repeatType column:'repeat_type', index:'repeat_type_index'
		repeatEnd column:'repeat_end', index:'repeat_end_index'
		durationType column:'duration_type', index:'duration_type_index'
	}
	
	public static enum RepeatType {
		// if you add more remind types, please edit the sql in RemindEmailService
		DAILY(DAILY_BIT, 24L * 60L * 60000), WEEKLY(WEEKLY_BIT, 7L * 24L * 60L * 60000),
		REMINDDAILY(REMIND_BIT | DAILY_BIT, 24L * 60L * 60000), REMINDWEEKLY(REMIND_BIT | WEEKLY_BIT, 7L * 24L * 60L * 60000),
		
		CONTINUOUS(CONTINUOUS_BIT, 1),
		CONTINUOUSGHOST(CONTINUOUS_BIT | GHOST_BIT, 1),
		
		DAILYGHOST(GHOST_BIT | DAILY_BIT | GHOST_BIT, 24L * 60L * 60000),
		WEEKLYGHOST(GHOST_BIT | WEEKLY_BIT | GHOST_BIT, 7L * 24L * 60L * 60000),
		
		REMINDDAILYGHOST(REMIND_BIT | DAILY_BIT | GHOST_BIT, 24L * 60L * 60000), REMINDWEEKLYGHOST(REMIND_BIT | WEEKLY_BIT | GHOST_BIT, 7L * 24L * 60L * 60000)
		
		public static final int DAILY_BIT = 1
		public static final int WEEKLY_BIT = 2
		public static final int REMIND_BIT = 4
		public static final int CONTINUOUS_BIT = 0x0100
		public static final int GHOST_BIT = 0x0200
		
		final Long id
		final long duration
		final boolean reminder
		final boolean continuous


		private static final Map<Integer, RepeatType> map = new HashMap<Integer, RepeatType>();
		
		static {
			RepeatType.each {
				map.put(it.getId(), it)
			}
		 }
		
		static RepeatType get(Long id) {
			return map.get(id)
		}
		
		RepeatType(Long id, long duration) {
			this.id = id
			this.duration = duration
		}
		
		def isGhost() {
			return (this.id & GHOST_BIT) > 0
		}
		
		def isContinuous() {
			return (this.id & CONTINUOUS_BIT) > 0
		}
		
		def isDaily() {
			return (this.id & DAILY_BIT) > 0
		}
		
		def isWeekly() {
			return (this.id & WEEKLY_BIT) > 0
		}
		
		def isReminder() {
			return (this.id & REMIND_BIT) > 0
		}
		
		def RepeatType toggleGhost() {
			if (isGhost())
				return RepeatType.get(this.id & (~GHOST_BIT))
			return RepeatType.get(this.id | GHOST_BIT)
		}
		
		def RepeatType forUpdate() {
			return RepeatType.get(this.id & ~(CONTINUOUS_BIT | GHOST_BIT))
		}
	}
	
	public static def DAILY_IDS = [ RepeatType.DAILY.getId(), RepeatType.DAILYGHOST.getId(), RepeatType.REMINDDAILY.getId(), RepeatType.REMINDDAILYGHOST.getId() ]
			
	public static def WEEKLY_IDS = [ RepeatType.WEEKLY.getId(), RepeatType.WEEKLYGHOST.getId(), RepeatType.REMINDWEEKLY.getId(), RepeatType.REMINDWEEKLYGHOST.getId() ]
			
	public static def REMIND_IDS = [ RepeatType.REMINDDAILY.getId(), RepeatType.REMINDWEEKLY.getId(), RepeatType.REMINDWEEKLY.getId(), RepeatType.REMINDDAILYGHOST.getId(), RepeatType.REMINDWEEKLYGHOST.getId() ]
	
	public static def CONTINUOUS_IDS = [ RepeatType.CONTINUOUS.getId(), RepeatType.CONTINUOUSGHOST.getId() ]
	
	public static def GHOST_IDS = [ RepeatType.CONTINUOUSGHOST.getId(), RepeatType.DAILYGHOST.getId(), RepeatType.WEEKLYGHOST.getId(), RepeatType.REMINDDAILYGHOST.getId(), RepeatType.REMINDWEEKLYGHOST.getId() ]

	public static enum DurationType {
		NONE(0), START(1), END(2)
		
		final Integer id
		
		DurationType(Integer id) {
			this.id = id
		}
	}

	Long userId
	Long tweetId
	Date date
	Integer timeZoneOffsetSecs
	Integer datePrecisionSecs
	Tag tag
	static hasMany = [hashtags:Tag,commentHashtags:Tag]
	BigDecimal amount
	Integer amountPrecision
	String units
	String comment
	RepeatType repeatType
	Date repeatEnd
	Tag baseTag
	DurationType durationType
	String setName // used for importing to identify entries as having been imported from a specific file
	
	static final def durationSynonyms = [
		'wake':'sleep end',
		'wake up':'sleep end',
		'woke':'sleep end',
		'awakened':'sleep end',
		'awoke':'sleep end',
		'went to sleep':'sleep start',
		'go to sleep':'sleep start',
		'sleep':'sleep start',
	]
	
	static final def startSynonyms = [
		'start', 'starts', 'begin', 'begins', 'starting', 'beginning', 'started', 'begun', 'began'
	]
	
	static final def startSynonymsSize = startSynonyms.size()
	
	static final def endSynonyms = [
		'end', 'ends', 'stop', 'stops', 'finish', 'finished', 'ended', 'stopped', 'stopping', 'ending', 'finishing'
	]
	
	static final def endSynonymsSize = endSynonyms.size()
	
	static final def startOrEndSynonyms = startSynonyms + endSynonyms
	
	static final def startOrEndSynonymsSize = startOrEndSynonyms.size()
	
	static class TagStatsRecord {
		TagStats oldTagStats
		TagStats newTagStats
	}
	
	static final MathContext mc = new MathContext(9)
	
	public static final int DEFAULT_AMOUNTPRECISION = 3
	
	/**
	 * Create and save new entry
	 */
	static Entry create(Long userId, Date date, Integer timeZoneOffsetSecs, String description, BigDecimal amount, String units,
			String comment, String setName, int amountPrecision = DEFAULT_AMOUNTPRECISION) {
		return create(userId,
				[
				'date':date,
				'timeZoneOffsetSecs':timeZoneOffsetSecs,
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
	
	static Entry create(Long userId, Map m, TagStatsRecord tagStatsRecord) {
		log.debug "Entry.create() userId:" + userId + ", m:" + m
		
		if (m == null) return null
		
		if (m['description'] == null) return null

		m['description'] = m['description'].toLowerCase()

		def (baseTag, durationType) = getDurationInfoFromStrings(m['description'], m['units'])
		
		Tag tag = Tag.look(m['description'])
		
		Entry entry = new Entry( \
			userId:userId, \
			tweetId:m['tweetId'], \
			date:m['date'], \
			timeZoneOffsetSecs:m['timeZoneOffsetSecs'] ?: 0, \
			datePrecisionSecs:m['datePrecisionSecs'], \
			tag:tag, \
			amount:m['amount'], \
			units:m['units']==null?'':m['units'], \
			comment:m['comment']==null?'':m['comment'], \
			repeatType: m['repeatType'], \
			baseTag:baseTag, \
			durationType:durationType, \
			setName:m['setName']==null?'':m['setName'], \
			amountPrecision:m['amountPrecision']==null?3:m['amountPrecision']
		)
		
		log.debug "Created entry:" + entry

		entry.processAndSave()
		
		def TagStats tagStats = TagStats.createOrUpdate(userId, entry.getTag().getId())
		
		if (tagStatsRecord != null) {
			tagStatsRecord.setOldTagStats(tagStats)
		}
		
		Entry generator = entry.fetchGeneratorEntry()
		
		if (generator != null)
			entry = generator.updateDurationEntry() // make sure duration entries remain consistent
			
		entry.createRepeat()
		
		return entry
	}

	/**
	 * Update entry given map of parameters (output of the parse method or manually created)
	 */
	public static TagStatsRecord update(Entry entry, Map m, TagStatsRecord tagStatsRecord) {
		log.debug "Entry.update() entry:" + entry + ", m:" + m
		
		long oldTagId = entry.getTag().getId()
		entry.doUpdate(m)
		long newTagId = entry.getTag().getId()
		
		TagStats oldTagStats = TagStats.createOrUpdate(entry.getUserId(), oldTagId)
		TagStats newTagStats = null
		
		if (newTagId != oldTagId) {
			newTagStats = TagStats.createOrUpdate(entry.getUserId(), newTagId)
		}
		
		if (tagStatsRecord != null) {
			tagStatsRecord.setOldTagStats(oldTagStats)
			tagStatsRecord.setNewTagStats(newTagStats)
		}
	}
	
	/**
	 * Delete actually just hides the entries, rather than deleting them
	 * 
	 * isToday - if true, delete subsequent repeating entries, if present
	 */
	public static delete(Entry entry, TagStatsRecord tagStatsRecord) {
		log.debug "Entry.delete() entry:" + entry

		Entry updateDurationEntry = entry.preDeleteProcessing()

		entry.deleteRepeat()
				
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
	
	/**
	 * Delete a ghost entry on a given baseDate and for a current date
	 * @param ghostTime
	 * @param currentDate
	 * @return
	 */
	public static deleteGhost(Entry entry, Date baseDate) {
		long entryTime = entry.getDate().getTime()
		long baseDateTime = baseDate.getTime()
		if (entryTime >= baseDateTime && entryTime - baseDateTime < DAYTICKS) { // entry is in today's data
			TagStatsRecord record = new TagStatsRecord()
			Entry.delete(entry, record)
		} else if (entryTime <= baseDateTime) { // this should always be true
			entry.setRepeatEnd(baseDate)
			Utils.save(entry, true)
		} else {
			log.error("Invalid call for entry: " + entry + " for baseDate " + baseDate)
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
	
	static def getTags(User user, type = BYALPHA) {
		log.debug "Entry.getTags() userID:" + user.getId() + ", type:" + type
		
		def retVal
		
		DatabaseService databaseService = DatabaseService.get()
		
		if (type == BYCOUNT) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, prop.is_continuous as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = ? and e.date is not null group by t.id order by count(e.id) desc", [user.getId()])
		} /*else if (type == BYRECENT) {
			retVal = Entry.executeQuery("select entry.tag.description, max(entry.date) as maxdate from Entry as entry left join TagProperties prop on prop.userId = entry.userId and prop.tagId = entry.tag.id where entry.userId = ? and entry.date is not null " + (recentOnly ? "and datediff(now(), entry.date) < 90 " : "") + "group by entry.tag.id order by max(entry.date) desc", [user.getId()])
		} */ else if (type == BYALPHA) {
			retVal = databaseService.sqlRows("select t.id, t.description, count(e.id) as c, prop.is_continuous as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = ? and e.date is not null group by t.id order by t.description", [user.getId()])
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
			allTagStats.add([tag.getDescription(),tag.getLastAmount(),tag.getLastAmountPrecision(),tag.getLastUnits()])
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
	
	/**
	 * Used only in batch jobs to renormalize all entries
	 * 
	 * @return
	 */
	def initializeDurations() {
		// fill in durations fields (for legacy entries)
		
		if (durationType != null) return; // already calculated
		
		def (calculatedBaseTag, calculatedDurationType) = getDurationInfo(tag, units)
		
		this.baseTag = calculatedBaseTag
		this.durationType = calculatedDurationType
		
		if (fetchIsEnd()) updateDurationEntry()
		
		Utils.save(this, true)
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
				m['timeZoneOffsetSecs'] = timeZoneOffsetSecs
				m['description'] = baseTag.getDescription()
				m['amount'] = amount
				m['units'] = "hours"
				m['comment'] = ''
				m['repeatType'] = null
				m['baseTag'] = baseTag
				m['durationType'] = DurationType.NONE
				m['amountPrecision'] = DEFAULT_AMOUNTPRECISION
				durationEntry = Entry.create(userId, m, null)
			} else {
				durationEntry.setAmount(amount)
			}
			Utils.trySave(durationEntry)
			
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
	
	/**
	 * Returns [startBaseTag, endBaseTag, synonym] array if the passed-in tag is a start or end tag
	 * If the passed-in tag is a start tag, calculates the base tag 
	 */
	protected static def getDurationInfo(Tag tag, String units) {
		return getDurationInfoFromStrings(tag.getDescription(), units)
	}
	
	/**
	 * Returns [startBaseTag, endBaseTag, synonym] array if the passed-in tag is a start or end tag
	 * If the passed-in tag is a start tag, calculates the base tag 
	 */
	protected static def getDurationInfoFromStrings(String description, String units) {
		log.debug "Entry.getDurationInfoFromStrings() description:" + description + ", units:" + units
		
		if (units.equals('hours'))
			return [Tag.look(description), DurationType.NONE]

		String synonym = durationSynonyms[description] ?: description
		
		// if this is start tag, try to find matching end tag
		for (def i = 0; i < startSynonymsSize; ++i) {
			def startEnd = startSynonyms[i]
			if (synonym.endsWith(' ' + startEnd)) {
				def startBaseTag = Tag.look(synonym.substring(0, synonym.length() - (startEnd.length() + 1)))
			
				log.debug "Found start tag with suffix '" + startEnd + "', base tag is " + startBaseTag

				return [startBaseTag, DurationType.START]
			}
		}
		
		// if this is start tag, try to find matching end tag
		for (def i = 0; i < endSynonymsSize; ++i) {
			def endEnd = endSynonyms[i]
			if (synonym.endsWith(' ' + endEnd)) {
				def endBaseTag = Tag.look(synonym.substring(0, synonym.length() - (endEnd.length() + 1)))
			
				log.debug "Found end tag with suffix '" + endEnd + "', base tag is " + endBaseTag

				return [endBaseTag, DurationType.END]
			}
		}
		
		return [Tag.look(synonym), DurationType.NONE]
	}
	
	/**
	 * Update existing entry and save
	 */
	private deleteRepeat() {
		if (this.repeatType == null) return
		
		if (this.repeatType.isContinuous()) {
			String queryStr = "from Entry entry where entry.tag.description = :desc and entry.date < :entryDate and entry.userId = :userId and entry.repeatType.id in (:repeatIds) order by entry.date desc limit 1"
			
			def entries = Entry.executeQuery(queryStr, [desc:this.tag.getDescription(), entryDate:this.date, userId:this.userId, repeatIds:CONTINUOUS_IDS])
			
			Entry e = entries[0]
			
			if (e != null) {
				long yesterday = this.date.getTime() - DAYTICKS
				if (yesterday < e.getDate().getTime()) {
					e.setRepeatEnd(e.getDate())
				} else
					e.setRepeatEnd(new Date(yesterday))
				Utils.save(e, true)
			}	
		} else {
			String queryStr = "from Entry entry where entry.tag.description = :desc and entry.date < :entryDate and time(entry.date) = time(:entryDate) and entry.userId = :userId and entry.repeatType.id in (:repeatIds) order by entry.date desc limit 1"
			
			def entries = Entry.executeQuery(queryStr, [desc:this.tag.getDescription(), entryDate:this.date, userId:this.userId, repeatIds:CONTINUOUS_IDS])
			
			Entry e = entries[0]
			
			if (e != null) {
				long yesterday = e.getDate().getTime() - DAYTICKS
				if (yesterday < this.date.getTime()) {
					repeatEnd = this.date
				} else
					repeatEnd = new Date(yesterday)
			}
		}
	}
	
	private createRepeat() {
		if (this.repeatType == null) return
		
		if (this.repeatType.isContinuous()) {
			String queryStr = "select entry.id from entry entry, tag tag where entry.tag_id = tag.id and entry.user_id = :userId and tag.description = :desc and entry.date < :entryDate and entry.repeat_type in (:repeatIds) order by entry.date desc limit 1"
			
			def entries = DatabaseService.get().sqlRows(queryStr, [desc:this.tag.description, entryDate:this.date, userId:this.userId, repeatIds:CONTINUOUS_IDS])
			
			def v = entries[0]
			
			if (v != null) {
				Entry e = Entry.get(v['id'])
				
				if (e != null) {
					long yesterday = this.date.getTime() - DAYTICKS
					if (yesterday < e.getDate().getTime()) {
						e.setRepeatEnd(e.getDate())
					} else
						e.setRepeatEnd(new Date(yesterday))
					Utils.save(e, true)
				}
			}
			queryStr = "select entry.id from entry entry, tag tag where entry.tag_id = tag.id and entry.user_id = :userId and tag.description = :desc and entry.date > :entryDate order by entry.date asc limit 1"
			
			entries = DatabaseService.get().sqlRows(queryStr, [desc:this.tag.description, entryDate:this.date, userId:this.userId])
			
			v = entries[0]
			
			if (v != null) {
				Entry e = Entry.get(v['id'])
				
				if (e != null) {
					long yesterday = e.getDate().getTime() - DAYTICKS
					if (yesterday < this.date.getTime()) {
						repeatEnd = this.date
					} else
						repeatEnd = new Date(yesterday)
				}
			}
		} else {
			String queryStr = "select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date < :entryDate and entry.user_id = :userId and entry.repeat_type in (:repeatIds) order by entry.date desc limit 1"
			
			def entries = DatabaseService.get().sqlRows(queryStr, [desc:this.tag.description, entryDate:this.date, userId:this.userId, repeatIds:DAILY_IDS])
			
			def v = entries[0]
			
			if (v != null) {
				Entry e = Entry.get(v['id'])
				
				if (e != null) {
					long yesterday = this.date.getTime() - DAYTICKS
					if (yesterday < e.getDate().getTime()) {
						e.setRepeatEnd(e.getDate())
					} else
						e.setRepeatEnd(new Date(yesterday))
					Utils.save(e, true)
				}
			}
			queryStr = "select entry.id from entry entry, tag tag where entry.tag_id = tag.id and tag.description = :desc and entry.date > :entryDate and entry.user_id = :userId order by entry.date asc limit 1"
			
			entries = DatabaseService.get().sqlRows(queryStr, [desc:this.tag.description, entryDate:this.date, userId:this.userId])
			
			v = entries[0]
			
			if (v != null) {
				Entry e = Entry.get(v['id'])
				
				if (e != null) {
					long yesterday = e.getDate().getTime() - DAYTICKS
					if (yesterday < this.date.getTime()) {
						repeatEnd = this.date
					} else
						repeatEnd = new Date(yesterday)
				}
			}
		}
	}
	
	def unGhost() {
		if (this.repeatType?.isGhost()) {
			this.repeatType = this.repeatType.toggleGhost()
		}
		
		return this
	}
	
	def activateGhostEntry(Date currentDate, Date nowDate) {
		long dayStartTime = currentDate.getTime()
		def now = nowDate.getTime()
		long diff = now - dayStartTime
		long thisDiff = this.date.getTime() - dayStartTime
		
		if (this.repeatType == null)
			return null
		
		if (!this.repeatType.isContinuous() && (thisDiff >=0 && thisDiff < DAYTICKS)) {
			// activate this entry
			this.unGhost()
			Utils.save(this, true)
			
			return this
		}
		
		def m = [:]
		m['timeZoneOffsetSecs'] = this.timeZoneOffsetSecs
		m['description'] = this.tag.getDescription()
		m['amount'] = this.repeatType.isReminder() ? null : this.amount
		m['units'] = this.units
		m['baseTag'] = this.baseTag
		m['durationType'] = DurationType.NONE
		m['amountPrecision'] = this.amountPrecision

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
		} else { // this repeat element isn't today
			m['comment'] = this.comment
			m['repeatType'] = this.repeatType
			m['date'] = new Date(((date.getTime() - dayStartTime) % DAYTICKS + DAYTICKS) + dayStartTime)
			m['datePrecisionSecs'] = DEFAULT_DATEPRECISION_SECS
			def retVal = Entry.create(userId, m, null)
			retVal.unGhost()
			
			Utils.save(retVal, true)
			return retVal
		}
	}
	
	/**
	 * Update existing entry and save
	 */
	private doUpdate(Map m) {
		log.debug "Entry.doUpdate() this:" + this + ", m:" + m
		
		if (m == null) return null
		
		def newDescription = m['description']
		
		if (newDescription == null) return null
		
		newDescription = newDescription.toLowerCase()
		
		def (newBaseTag, newDurationType) = getDurationInfoFromStrings(newDescription, m['units'])

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
			deleteRepeat()
			updateRepeatType = true
		}
		
		setDate(m['date'])
		setDatePrecisionSecs(m['datePrecisionSecs'])
		updateDescription(newDescription)
		setAmount(amt)
		setAmountPrecision(m['amountPrecision']?:DEFAULT_AMOUNTPRECISION)
		setUnits(m['units']?:'')
		setComment(m['comment']?:'')
		setRepeatType(m['repeatType']?.forUpdate()) // do not set continuous or ghost repeat types on edit
		setSetName(m['setName']?:'')
		setBaseTag(newBaseTag)
		setDurationType(newDurationType)
		
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
		String continuousQuery = "from Entry entry where entry.userId = :userId and entry.date < :entryDate and entry.repeatEnd is null or entry.repeatEnd >= :entryDate and (not entry.repeatType is null) and entry.repeatType.id in (:repeatIds)"
		
		def entries = Entry.executeQuery(continuousQuery, [userId:userId, entryDate:now, repeatIds:CONTINUOUS_IDS])

		return entries		
	}

	static SimpleDateFormat systemFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	static def fetchReminders(User user, Date startDate, long intervalMillis) {
		String queryStr = "select distinct entry.id " \
		+ "from entry entry where entry.user_id = :userId and " \
		+ "entry.date < :startDate and (entry.repeat_end is null or entry.repeat_end > :startDate) and (not entry.repeat_type is null) and (entry.repeat_type & :remindBit <> 0) " \
		+ "and (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400 < :intervalMs"
		
		def rawResults = DatabaseService.get().sqlRows(queryStr, [userId:user.getId(), startDate:startDate, intervalMs:intervalMillis, remindBit:RepeatType.REMIND_BIT])
	}
	
	static long abs(long x) { return x < 0 ? -x : x }
	
	static def fetchListData(User user, Date date, Date now) {
		long nowTime = now.getTime()
		
		// get regular elements + timed repeating elements next
		String queryStr = "select distinct entry.id, timestamp(timestampadd(second, (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400, :startDate)) as dateTime " \
				+ "from entry entry, tag tag where entry.user_id = :userId and (((entry.date >= :startDate and entry.date < :endDate) and (entry.repeat_type is null or (not entry.repeat_type in (:continuousIds)))) or " \
				+ "(entry.date < :startDate and (entry.repeat_end is null or entry.repeat_end > :startDate) and (not entry.repeat_type is null) and (not entry.repeat_type in (:continuousIds)))) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by case when entry.date_precision_secs < 1000 and (entry.repeat_type is null or (not entry.repeat_type in (:continuousIds))) " \
				+ "then unix_timestamp(timestampadd(second, (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400, :startDate)) else " \
				+ "(case when entry.repeat_type is null then 99999999999 else 0 end) end desc, tag.description asc"
				
		def queryMap = [userId:user.getId(), startDate:date, endDate:date + 1, continuousIds:CONTINUOUS_IDS]
		
		def rawResults = DatabaseService.get().sqlRows(queryStr, queryMap)

		def timedResults = []
		
		Set resultTagIds = new HashSet<Long>()
		
		for (result in rawResults) {
			Entry entry = Entry.get(result['id'])
			def desc = entry.getJSONDesc()
			desc['date'] = result['dateTime']
			if ((!entry.getDate().equals(result['dateTime'])) && entry.repeatType != null) { // ghost timed entry
				desc['repeatType'] = entry.repeatType.id | RepeatType.GHOST_BIT
			} else
				desc['repeatType'] = entry.repeatType?.id
			timedResults.add(desc)
			/* if ((!entry.repeatType?.isGhost()) && abs(entry.getDate().getTime() - nowTime) < HOURTICKS) {
				resultTagIds.add(entry.getTag().getId())
			} */
		}
		
		// get continuous repeating elements
		String continuousQueryStr = "select distinct entry.id, timestamp(timestampadd(second, (timestampdiff(second, :startDate, entry.date) % 86400 + 86400) % 86400, :startDate)) as dateTime " \
				+ "from entry entry, tag tag where entry.user_id = :userId and " \
				+ "(entry.date < :endDate and (entry.repeat_end is null or entry.repeat_end > :startDate) and (not entry.repeat_type is null) and (entry.repeat_type in (:continuousIds))) " \
				+ "and entry.tag_id = tag.id " \
				+ "order by tag.description asc"
		
		def continuousResults = DatabaseService.get().sqlRows(continuousQueryStr, queryMap)
		
		def results = []
		
		// add continuous elements that aren't already in the timed list
		for (result in continuousResults) {
			Entry entry = Entry.get(result['id'])
			if (!resultTagIds.contains(entry.getTag().getId())) {
				def desc = entry.getJSONDesc()
				desc['date'] = result['dateTime']
				desc['repeatType'] = entry.repeatType?.id
				results.add(desc)
			}
		}

		// add timed results
		for (result in timedResults) {
			results.add(result)
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

	static def fetchPlotData(User user, def tagIds, Date startDate, Date endDate) {
		log.debug "Entry.fetchPlotData() userId:" + user.getId() + ", tagIds:" + tagIds + ", startDate:" + startDate \
				+ ", endDate:" + endDate
		
		def map = [userId:user.getId(), tagIds:tagIds, ghostIds:GHOST_IDS]
		
		String queryStr = "from Entry entry where entry.userId = :userId and entry.tag.id in (:tagIds) "
		
		if (startDate != null) {
			queryStr += "and entry.date >= :startDate "
			map['startDate'] = startDate
		}
		
		if (endDate != null) {
			queryStr += "and entry.date < :endDate "
			map['endDate'] = endDate
		}
		
		queryStr += "and entry.date is not null and entry.amount is not null and (not entry.repeatType.id in (:ghostIds)) order by entry.date asc"

		Utils.listJSONShortDesc(Entry.executeQuery(queryStr, map))
	}
	
	static final long HALFDAYTICKS = 12 * 60 * 60 * 1000L

	static def fetchSumPlotData(boolean sumNights, User user, def tagIds, Date startDate, Date endDate, int timeZoneSec) {
		if (sumNights) {
			if (startDate != null)
				startDate = new Date(startDate.getTime() + HALFDAYTICKS)
			if (endDate != null)
				endDate = new Date(endDate.getTime() + HALFDAYTICKS)
		}
		
		def map = [userId:user.getId(), tagIds:tagIds, ghostIds:GHOST_IDS]
		
		int timeZoneHours = (timeZoneSec / 3600)
		int timeZoneMinutes = ((int)((timeZoneSec < 0 ? -timeZoneSec : timeZoneSec) / 60)) % 60
		
		String mysqlTimeZone = timeZoneHours + ":" + ((timeZoneMinutes < 10 ? "0" : "") + timeZoneMinutes)

		def sourceTimeZone = sumNights ? '+12:00' : '+00:00';

		String queryStr = "select date as dt, sum(amount) as amount from Entry entry where entry.userId = :userId and entry.amount IS NOT NULL and " \
			+ "entry.tag.id in (:tagIds) and (not entry.repeatType.id in (:ghostIds)) "
		
		if (startDate != null) {
			queryStr += "and entry.date >= :startDate "
			map['startDate'] = startDate
		}
		
		if (endDate != null) {
			queryStr += "and entry.date < :endDate "
			map['endDate'] = endDate
		}
		
		queryStr += "and entry.date is not null group by date(convert_tz(entry.date,'" + sourceTimeZone + "','" + mysqlTimeZone + "')) " \
			+ "order by date(convert_tz(entry.date,'+00:00','" + mysqlTimeZone + "')) asc";

		log.debug "QUERY: " + queryStr;

		def rawResults = Entry.executeQuery(queryStr, map)
		def results = []

		for (result in rawResults) {
			log.debug "RESULT: " + result
			results.push([
				new Date(result[0].getTime() + 43200000),
				result[1]
			])
		}

		return results
	}
	
	static def parseInt(String str) {
		return str == null ? null : Integer.parseInt(str)
	}

	static def parseMeta(String entryStr) {
		return parse(null, null, entryStr, null, false)
	}

	// new end-of-entry repeat indicators:
	// repeat daily
	// delete repeat indicator to end repeat sequence
	// OR: repeat end

	private static def matchEndStr(String str, String match, RepeatType repeatType) {
		if (str.endsWith(match)) {
			return [
				str.substring(0, str.length() - match.length()),
				match,
				repeatType
			]
		}

		return null
	}

	private static def matchRepeatStr(comment) {
		def retVal

		if (comment != null) {
			if ((retVal = matchEndStr(comment, "repeat weekly", RepeatType.WEEKLY)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "remind weekly", RepeatType.REMINDWEEKLYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "reminder weekly", RepeatType.REMINDWEEKLYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "weekly reminder", RepeatType.REMINDWEEKLYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "repeat daily", RepeatType.DAILY)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "remind daily", RepeatType.REMINDDAILYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "reminder daily", RepeatType.REMINDDAILYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "daily remind", RepeatType.REMINDDAILYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "daily reminder", RepeatType.REMINDDAILYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "repeat", RepeatType.DAILY)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "remind", RepeatType.REMINDDAILYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "reminder", RepeatType.REMINDDAILYGHOST)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "daily", RepeatType.DAILY)) != null) {
				return retVal
			}
			if ((retVal = matchEndStr(comment, "weekly", RepeatType.WEEKLY)) != null) {
				return retVal
			}
		}

		return [comment, "", null]
	}

	/**
	 * pre-process entry to see if there are additional tags
	 *
	 * currently only looks for repeat type
	 */
	private static def preprocessEntry(retVal, String str) {
		def (remain, comment, repeatType) = matchRepeatStr(str)

		retVal['repeatType'] = repeatType
		retVal['comment'] = comment

		return remain
	}

	private static boolean isSameDay(Date time1, Date time2, TimeZone timeZone) {
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
	static Pattern amountPattern = ~/(?i)^(-?\.\d+|-?\d+\.\d+|\d+|_\s|-\s|__\s|___\s)/
	static Pattern endTagPattern = ~/(?i)^(yes|no)/
	static Pattern unitsPattern = ~/(?i)^([^0-9\(\)@\s\.:][^\(\)@\s:]*)/
	static Pattern tagAmountSeparatorPattern = ~/(?i)^[:=]\s*/
	
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
	
	static def parse(Date time, TimeZone timeZone, String entryStr, Date baseDate, boolean defaultToNow, boolean forUpdate = false) {
		log.debug "Entry.parse() time:" + time + ", timeZone:" + timeZone?.getID() + ", entryStr:" + entryStr + ", baseDate:" + baseDate + ", defaultToNow:" + defaultToNow

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
		retVal['timeZoneOffsetSecs'] = timeZone == null ? 0 : timeZone.getOffset(baseDateTime) / 1000
		retVal['today'] = today
		entryStr = preprocessEntry(retVal, entryStr)
		
		def matcher;
		def parts;

		// [time] [tag] <[amount] <[units]>> <[comment]>
		//
		// OR
		//
		// [tag] <[amount] <[units]>> <[time]> <[comment]>

		PatternScanner scanner = new PatternScanner(entryStr)

		boolean foundAmount = false
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
			def amount = scanner.group(1)

			if (amount =~ /-\s/ || amount.startsWith('_')) {
				retVal['amount'] = null
				retVal['amountPrecision'] = -1
			} else {
				retVal['amount'] = amount
				retVal['amountPrecision'] = DEFAULT_AMOUNTPRECISION			
			}
			
			foundAmount = true
			
			scanner.skipWhite()

			if (!scanner.matchField(timePattern, timeClosure)) {
				scanner.matchField(unitsPattern) {
					retVal['units'] = scanner.group(1)
				}
			}
		}
		
		Closure yesNoClosure = { word ->
			Integer yesno = word.equalsIgnoreCase('yes') ? 1 : word.equalsIgnoreCase('no') ? 0 : null
			if (yesno != null) {
				retVal['amount'] = yesno
				retVal['amountPrecision'] = 0
			
				foundAmount = true
				
				return true
			}
			
			if (word.equalsIgnoreCase('none')) {
				retVal['amount'] = null
				retVal['amountPrecision'] = -1
				
				foundAmount = true
				
				return true
			}
			
			return false
		}
		
		scanner.skipWhite()

		scanner.matchField(timePattern, timeClosure)

		def descriptionBuf = new StringBuffer()

		String lastWhite = null
		String lastTagWord
		int lastAppendLen = 0
		
		boolean endTag = false
		boolean tagAmountSeparator = false

		while (scanner.match(tagWordPattern)) {
			if (lastWhite != null) {
				lastAppendLen = lastWhite.length()
				descriptionBuf.append(lastWhite)
			} else
				lastAppendLen = 0

			lastTagWord = scanner.group(1)
			lastAppendLen += lastTagWord.length()
			descriptionBuf.append(scanner.group(1))
			
			if (scanner.matchWhite()) {
				lastWhite = scanner.group()
			}
			if (scanner.match(tagAmountSeparatorPattern)) {
				endTag = true
				tagAmountSeparator = true
			}
			
			// allow time to appear before amount
			if (scanner.match(timePattern, timeClosure)) {
				scanner.skipWhite()
				if (!endTag) { // tag separator can be after time
					scanner.match(tagAmountSeparatorPattern) {
						tagAmountSeparator = true
					}
				}
				endTag = true
			}

			if (scanner.match(amountPattern, amountClosure))
				break;
 
			if (endTag)
				break;
		}
		
		if ((!foundAmount) && (!tagAmountSeparator) && (lastTagWord != null)) {
			if (yesNoClosure(lastTagWord)) {
				int l = descriptionBuf.length()
				descriptionBuf.delete(l - lastAppendLen, l)
			}
		}

		if (descriptionBuf.length() > 0) {
			retVal['description'] = descriptionBuf.toString().trim()

			log.debug "description " + retVal['description']
		}

		if (!foundAmount) {
			retVal['amount'] = '1'
			retVal['amountPrecision'] = -1;
		}

		if (!foundTime) {
			scanner.matchField(timePattern, timeClosure)
		}

		def commentBuf = new StringBuffer()

		lastWhite = null
		boolean firstWord = true

		while (scanner.match(commentWordPattern)) {
			if (lastWhite != null)
				commentBuf.append(lastWhite)

			String word = scanner.group(1)
			if (firstWord && yesNoClosure(word)) {
				if (scanner.matchWhite())
					lastWhite = null
			} else {
				commentBuf.append(scanner.group(1))
				if (scanner.matchWhite()) {
					lastWhite = scanner.group()
				}
			}
			
			firstWord = false

			if (scanner.match(timeEndPattern, timeClosure))
				break;
		}

		if (commentBuf.length() > 0) {
			prependComment(retVal, commentBuf.toString().trim())

			log.debug "comment " + retVal['comment']
		}

		if (!foundTime) {
			if (date != null) {
				date = time;
				if (!(defaultToNow && today)) { // only default to now if entry is for today
					date = new Date(baseDate.getTime() + HALFDAYTICKS);
					log.debug "SETTING DATE TO " + Utils.dateToGMTString(date) 
					retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				} else {
					if (!isSameDay(time, baseDate, timeZone)) {
						retVal['status'] = "Entering event for today, not displayed";
					}
				}
			}
			retVal['date'] = date;
		} else if (date != null) {
			if (hours == null && date != null) {
				date = time;
				if (!defaultToNow) {
					date = new Date(date.getTime() + 12 * 3600000L);
					retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				} else if (!isSameDay(time, baseDate, timeZone)) {
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
				if (today) {
					// if base date is today and time is greater than one hour from now, assume
					// user meant yesterday
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
			retVal['date'] = date;
		} else {
			retVal['date'] = null;
		}

		// post-process parsed tag

		def (remain, comment, repeatType) = matchRepeatStr(retVal['description'])

		if (repeatType != null) {
			retVal['repeatType'] = repeatType
			retVal['description'] = remain.trim()
			prependComment(retVal, comment)
		} else {
			(remain, comment, repeatType) = matchRepeatStr(retVal['comment'])

			if (repeatType != null) {
				retVal['repeatType'] = repeatType
			}
		}
		
		// if comment is duration modifier, append to tag name
		// only do this on exact match, to avoid comments being accidentally interpreted as duration modifiers
		
		String comm = retVal['comment']
		
		boolean matchedDuration = false
		
		for (def i = 0; i < startOrEndSynonymsSize; ++i) {
			String syn = startOrEndSynonyms[i]
			if (comm.equals(syn)) {
				retVal['description'] += ' ' + comm
				retVal['comment'] = ''
				matchedDuration = true
				break
			} else if (comm.startsWith(syn)) {
				def rem = comm.substring(syn.length())
				if (rem.startsWith('(')) {
					retVal['description'] += ' ' + syn
					retVal['comment'] = rem
				} else if (rem.startsWith(' (')) {
					retVal['description'] += ' ' + syn
					retVal['comment'] = rem.substring(1)
				}
			}
		}
		
		if (retVal['description']?.length() > Tag.MAXLENGTH) {
			def desc = retVal['description']
			def lastSpace = desc.lastIndexOf(' ', Tag.MAXLENGTH - 1)
			retVal['description'] = desc.substring(0, lastSpace)
			comment = retVal['comment']
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

		if (retVal['units']?.length() > MAXUNITSLENGTH) {
			retVal['units'] = retVal['units'].substring(0, MAXUNITSLENGTH)
		}

		if (retVal['setName']?.length() > MAXSETNAMELENGTH) {
			retVal['setName'] = retVal['setName'].substring(0, MAXSETNAMELENGTH)
		}
		
		log.debug("retVal: parse " + retVal)
		
		def amt = retVal['amount']
		
		retVal['amount'] = amt == null ? null : new BigDecimal(amt, mc)
		
		if (retVal['repeatType'] != null) {
			if (!retVal['repeatType'].isReminder()) {
				if (retVal['repeatType'].isDaily()) {
					if (foundAmount) {
						if (foundTime) {
							retVal['repeatType'] = RepeatType.DAILY
						} else {
							if (forUpdate)
								retVal['repeatType'] = RepeatType.DAILY
							else
								retVal['repeatType'] = RepeatType.CONTINUOUSGHOST
						}
					} else {
						if (foundTime || forUpdate) {
							retVal['repeatType'] = RepeatType.DAILYGHOST
						} else {
							retVal['repeatType'] = RepeatType.CONTINUOUSGHOST
						}
					}
				} else if (retVal['repeatType'].isWeekly()) {
					if (foundAmount) {
						retVal['repeatType'] = RepeatType.WEEKLY
					} else {
						retVal['repeatType'] = RepeatType.WEEKLYGHOST
					}
				}
				if (retVal['repeatType'].isContinuous()) { // continuous repeat are always vague date precision
					retVal['date'] = new Date(baseDate.getTime() + HALFDAYTICKS);
					retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				}
			} else {
				if (foundAmount) {
					retVal['repeatType'] = retVal['repeatType'].toggleGhost()
				}
			}
		}

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
	
	static {
		gmtFormat = new SimpleDateFormat("k:mm")
		gmtFormat.setTimeZone(Utils.createTimeZone(0, "GMT"))
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
				not {
					isNull("date")
				}
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
				not {
					isNull("date")
				}
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
			timeZoneOffsetSecs:timeZoneOffsetSecs,
			description:tag.getDescription(),
			amount:amount,
			amountPrecision:fetchAmountPrecision(),
			units:units,
			comment:comment,
			repeatType:repeatType]
	}

	/**
	 * Get minimal data
	 */
	def getJSONShortDesc() {
		return [date, amount, getDescription()];
	}

	def getDateString() {
		return Utils.dateToGMTString(date)
	}

	String toString() {
		return "Entry('" + this.exportString() \
				+ "', date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", repeatType:" + repeatType?.getId() \
				+ ")"
	}

	def String valueString() {
		return "Entry(userId:" + userId \
				+ ", date:" + Utils.dateToGMTString(date) \
				+ ", datePrecisionSecs:" + fetchDatePrecisionSecs() \
				+ ", timeZoneOffsetSecs:" + timeZoneOffsetSecs \
				+ ", description:" + getDescription() \
				+ ", amount:" + (amount == null ? 'null' : amount.toPlainString()) \
				+ ", units:" + units \
				+ ", amountPrecision:" + fetchAmountPrecision() \
				+ ", comment:" + comment \
				+ ", repeatType:" + repeatType?.getId() \
				+ ")"
	}
	
	boolean equals(Object o) {
		if (!o instanceof Entry)
			return false
		return ((Entry)o).getId() == id
	}
}

package us.wearecurio.services

import java.math.MathContext
import java.text.DateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashSet
import java.util.regex.Pattern

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.DurationType
import us.wearecurio.data.RepeatType
import static us.wearecurio.model.Entry.DAYTICKS
import static us.wearecurio.model.Entry.HALFDAYTICKS
import static us.wearecurio.model.Entry.HOURTICKS
import static us.wearecurio.model.Entry.DEFAULT_AMOUNTPRECISION
import static us.wearecurio.model.Entry.DEFAULT_DATEPRECISION_SECS
import static us.wearecurio.model.Entry.VAGUE_DATE_PRECISION_SECS
import static us.wearecurio.model.Entry.MAXUNITSLENGTH
import static us.wearecurio.model.Entry.MAXCOMMENTLENGTH
import static us.wearecurio.model.Entry.MAXSETNAMELENGTH

import org.joda.time.DateTime
import org.joda.time.DateTimeFieldType

import us.wearecurio.parse.ParseUtils
import us.wearecurio.parse.ScannerPattern
import us.wearecurio.parse.PatternScanner
import us.wearecurio.parse.PatternScanner.ScannerTry
import us.wearecurio.parse.ScannerPattern
import static us.wearecurio.parse.PatternScanner.CONDITION_ATEND
import static us.wearecurio.parse.PatternScanner.CONDITION_ANY
import static us.wearecurio.model.Entry.mc

import us.wearecurio.data.UnitGroupMap
import us.wearecurio.data.UnitGroupMap.UnitGroup
import us.wearecurio.data.UnitGroupMap.UnitRatio

import java.text.SimpleDateFormat
import us.wearecurio.utility.Utils

class EntryParserService {

	private static def log = LogFactory.getLog(this)

    static transactional = false

	static def service
	
	static def set(s) { service = s }

	static EntryParserService get() { return service }
	
	static Integer parseInt(String str) {
		return str == null ? null : Integer.parseInt(str)
	}

	def parseMeta(String entryStr) {
		return parse(null, null, entryStr, null, null, null, false)
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

	protected static final Pattern timePattern    = ~/(?i)^(@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)\b\s*|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?\b\s*|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm|a|p)\b\s*/
	protected static final Pattern timeWordPattern    = ~/(?i)^(noon|midnight)\b\s*/
	protected static final Pattern tagWordPattern = ~/(?i)^([^0-9\(\)@\s\.:=][^\(\)@\s:=]*)($|\s*)/
	protected static final Pattern commentWordPattern = ~/^([^\s]+)($|\s*)/
	protected static final Pattern amountPattern = ~/(?i)^([:=]\s*)?(-?\.\d+|-?\d+[\d,]*\.\d+|-?\d[\d,]*+|-|_\b|__\b|___\b|none\b|zero\b|yes\b|no\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b)(\s*\/\s*(-?\.\d+|-?\d+\.\d+|-?\d+|-|_\b|-\b|__\b|___\b|zero\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b))?\s*/
	
	protected static Map<String, BigDecimal> numberMap = [
		'zero' : [new BigDecimal(0, mc), DEFAULT_AMOUNTPRECISION],
		'one' : [new BigDecimal(1, mc), DEFAULT_AMOUNTPRECISION],
		'two' : [new BigDecimal(2, mc), DEFAULT_AMOUNTPRECISION],
		'three' : [new BigDecimal(3, mc), DEFAULT_AMOUNTPRECISION],
		'four' : [new BigDecimal(4, mc), DEFAULT_AMOUNTPRECISION],
		'five' : [new BigDecimal(5, mc), DEFAULT_AMOUNTPRECISION],
		'six' : [new BigDecimal(6, mc), DEFAULT_AMOUNTPRECISION],
		'seven' : [new BigDecimal(7, mc), DEFAULT_AMOUNTPRECISION],
		'eight' : [new BigDecimal(8, mc), DEFAULT_AMOUNTPRECISION],
		'nine' : [new BigDecimal(9, mc), DEFAULT_AMOUNTPRECISION],
		'yes' : [new BigDecimal(1, mc), 0],
		'no' : [new BigDecimal(0, mc), 0],
		'none' : [ null, -1],
		'-' : [ null, -1],
		'_' : [ null, -1],
		'__' : [ null, -1],
		'___' : [ null, -1],
	]

	/**
	 * Regex pattern to match repeat modifiers
	 * 
	 * If you update repeatPattern, you need to update repeatMap
	 */
	protected static final Pattern repeatPattern = ~/^(repeat daily|repeat weekly|remind daily|remind weekly|reminder daily|reminder weekly|daily repeat|daily remind|daily reminder|weekly repeat|weekly remind|weekly reminder|button|repeat|pinned|favorite|remind|reminder|daily|weekly)\b\s*/
	
	protected static final int REPEATMAP_SYNONYM = 0
	protected static final int REPEATMAP_TYPE = 1
	
	protected static Map<String, ArrayList<Object>> repeatMap = [
		'repeat':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'repeat daily':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'repeat weekly':['repeat weekly', RepeatType.WEEKLYCONCRETEGHOST],
		'button':['pinned', RepeatType.CONTINUOUSGHOST],
		'pinned':['pinned', RepeatType.CONTINUOUSGHOST],
		'favorite':['pinned', RepeatType.CONTINUOUSGHOST],
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
	protected static final Pattern durationPattern = ~/^(start|starts|begin|begins|starting|beginning|started|begun|began|end|ends|stop|stops|finish|finished|ended|stopped|stopping|ending|finishing)\b\s*/
	
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
	protected static final Pattern durationSynonymPattern = ~/^(wake up|went to sleep|go to sleep|wake|woke|awakened|awoke|slept|sleep)\b\s*/
	
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
		'sleep':['sleep', 'start', DurationType.START],
	]

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
		
	static class ParseAmount {
		Tag tag // tag including units suffix
		String suffix
		BigDecimal amount
		int precision
		UnitRatio unitRatio
		String units
		
		ParseAmount(BigDecimal amount, int precision) {
			this.tag = null
			this.amount = amount
			this.precision = precision
		}
		
		void deactivate() {
			tag = null
		}
		
		boolean isActive() {
			return tag != null
		}
		
		ParseAmount setTags(Tag tag, Tag baseTag) {
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
		
		String toString() {
			if (tag == null)
				return "ParseAmount(inactive)"
			return "ParseAmount(Tag: " + tag.getDescription() + " amount: " + amount + " units: " + units + ")"
		}
	}
	
	protected static DateFormat gmtFormat
	protected static long GMTMIDNIGHTSECS

	static {
		gmtFormat = new SimpleDateFormat("k:mm")
		gmtFormat.setTimeZone(Utils.createTimeZone(0, "GMT", false))
		GMTMIDNIGHTSECS = new SimpleDateFormat("yyyy-MM-dd h:mm a z").parse("2010-01-01 12:00 AM GMT").getTime() / 1000
	}
	
	static protected HashSet<String> bloodPressureTags = new HashSet<String>()
	
	static {
		bloodPressureTags.add("blood pressure")
		bloodPressureTags.add("bp")
		bloodPressureTags.add("blood")
	}
	
	/**
	 * Return tag with suffix for given units and offset. Hack blood pressure for now.
	 */
	Tag tagWithSuffixForUnits(Tag baseTag, String units, int index) {
		if (!units) {
			return baseTag
		}
		
		UnitRatio unitRatio = UnitGroupMap.theMap.unitRatioForUnits(units)
		String suffix
		if (unitRatio)
			suffix = unitRatio.getSuffix()
		else
			suffix = units
			
		if (bloodPressureTags.contains(baseTag.getDescription())) {
			if (suffix) {
				if (suffix.equals("[pressure]")) {
					if (index == 0) suffix = "[systolic]"
					else suffix = "[diastolic]"
				}
			}
		}
		
		return Tag.look(baseTag.getDescription() + ' ' + suffix)
	}

	/**
	 * Return tag with suffix for given units and offset. Hack blood pressure for now.
	 */
	def baseTagAndTagWithSuffixForUnits(Tag baseTag, String units, int index) {
		if (!units) {
			return baseTag
		}
		
		UnitRatio unitRatio = UnitGroupMap.theMap.unitRatioForUnits(units)
		String suffix, coreSuffix
		if (unitRatio)
			suffix = unitRatio.getSuffix()
		else
			suffix = units
		
		if (EntryParserService.bloodPressureTags.contains(baseTag.getDescription())) {
			if (suffix) {
				if (suffix.equals("[pressure]")) {
					if (index == 0) suffix = "[systolic]"
					else suffix = "[diastolic]"
				}
			}
		}
		
		if (suffix.startsWith('[') && suffix.endsWith(']'))
			coreSuffix = suffix.substring(1, suffix.length() - 1)
		else
			coreSuffix = suffix
		
		while (true) {
			String baseDescription = baseTag.getDescription()
		
			if (baseDescription.endsWith(' ' + suffix)) {
				baseTag = Tag.look(baseDescription.substring(0, baseDescription.length() - (suffix.length() + 1)))
			} else if (baseDescription.endsWith(' ' + coreSuffix)) {
				baseTag = Tag.look(baseDescription.substring(0, baseDescription.length() - (coreSuffix.length() + 1)))
			} else
				break
		}
			
		Tag tag = Tag.look(baseTag.getDescription() + ' ' + suffix)
		
		return [baseTag, tag]
	}

	static final int CONDITION_TAGWORD = 1
	static final int CONDITION_TAGSEPARATOR = 2
	static final int CONDITION_DURATION = 3
	static final int CONDITION_REPEAT = 4
	static final int CONDITION_AMOUNT = 5
	static final int CONDITION_UNITSA = 6
	static final int CONDITION_UNITSB = 7
	static final int CONDITION_TIME = 8
	static final int CONDITION_COMMENT = 9
	
	def parse(Date time, String timeZoneName, String entryStr, Long repeatTypeId, Date repeatEnd, Date baseDate, boolean defaultToNow = true, boolean forUpdate = false) {
		log.debug "EntryParserService.parse() time:" + time + ", timeZoneName:" + timeZoneName + ", entryStr:" + entryStr + ", baseDate:" + baseDate + ", defaultToNow:" + defaultToNow

		if (entryStr == '') return null // no input
		
		// truncate time to hours:minutes
		
		time = new DateTime(time).withField(DateTimeFieldType.secondOfMinute(), 0).toDate()
		
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
			
		retVal['repeatType'] = RepeatType.get(repeatTypeId)
		retVal['repeatEnd'] = repeatEnd
		retVal['timeZoneName'] = timeZoneName
		retVal['today'] = today

		def matcher
		def parts

		// [time] [tag] <[amount] <[units]>>... <repeat|remind|button> (<[comment]>)
		//
		// OR
		//
		// [tag] <[amount] <[units]>>... <[time]> <repeat|remind|button> (<[comment]>)
		//
		// <repeat|remind|button> ahead of the above

		PatternScanner scanner = new PatternScanner(entryStr)

		ScannerPattern atEndScanPattern = new ScannerPattern(scanner, CONDITION_ATEND)
		ScannerPattern anyScanPattern = new ScannerPattern(scanner, CONDITION_ANY)		

		ArrayList<ParseAmount> amounts = new ArrayList<ParseAmount>()
		boolean ghostAmount = false
		boolean foundTime = false
		boolean foundAMPM = false

		ScannerPattern timeScanPattern = new ScannerPattern(scanner, CONDITION_TIME, timePattern, true, {
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
			def g18 = scanner.group(18)
			if (scanner.group(8).equals('p') || scanner.group(13).equals('p') || scanner.group(18).equals('p') || scanner.group(18).equals('pm')) {
				foundAMPM = true
				hours += 12
			} else if (scanner.group(8).equals('a') || scanner.group(13).equals('a') || scanner.group(18).equals('a') || scanner.group(18).equals('am')) {
				foundAMPM = true
			}
		})
		ScannerPattern timeWordScanPattern = new ScannerPattern(scanner, CONDITION_TIME, timeWordPattern, true, {
			foundTime = true
			def noonmid = scanner.group(1)
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
		})
		
		LinkedList<String> words = new LinkedList<String>()
		LinkedList<String> commentWords = new LinkedList<String>()
		String suffix = ''
		String repeatSuffix = ''
		
		boolean foundRepeat = false
		boolean foundDuration = false
		boolean foundTag = false
		boolean matchTag = true
		boolean inComment = false

		ScannerPattern tagWordScanPattern = new ScannerPattern(scanner, CONDITION_TAGWORD, tagWordPattern, false, {
			words.add(scanner.group(1))
			foundTag = true
		})

		Closure repeatClosure = {
			foundRepeat = true
			
			String modifier = scanner.group(1)
			
			ArrayList<Object> info = repeatMap[modifier]
			retVal['repeatType'] = (RepeatType) info[REPEATMAP_TYPE]
			repeatSuffix = (String) info[REPEATMAP_SYNONYM]
		}
		
		ScannerPattern repeatScanPattern = new ScannerPattern(scanner, CONDITION_REPEAT, repeatPattern, true, repeatClosure)
		ScannerPattern repeatStartScanPattern = new ScannerPattern(scanner, CONDITION_REPEAT, repeatPattern, true, repeatClosure)
		
		Closure durationClosure = {
			foundDuration = true
			
			String modifier = scanner.group(1)
			
			ArrayList<Object> info = durationMap[modifier]
			retVal['durationType'] = (DurationType) info[DURATIONMAP_TYPE]
			suffix = (String) info[DURATIONMAP_SYNONYM]
		}
		
		ScannerPattern durationScanPattern = new ScannerPattern(scanner, CONDITION_DURATION, durationPattern, true, durationClosure)
		ScannerPattern durationStartScanPattern = new ScannerPattern(scanner, CONDITION_DURATION, durationPattern, true, durationClosure)
		
		int currentAmountIndex = 0
		String currentUnits = null
		
		// amount
		Closure amountClosure = {
			String amountStr
			ArrayList<String> amountStrs = []
			currentAmountIndex = amounts.size()
			boolean twoDAmount = false
			
			amountStrs.add(scanner.group(2).replace(",",""))
			amountStr = scanner.group(4)
			
			if (amountStr) {
				amountStrs.add(amountStr.replace(",",""))
				twoDAmount = true
			}
			
			for (String amount in amountStrs) {
				if (numberMap.containsKey(amount)) {
					ArrayList num = numberMap.get(amount)
					amounts.add(new ParseAmount(num[0], num[1]))
				} else {
					amounts.add(new ParseAmount(new BigDecimal(amount, mc), DEFAULT_AMOUNTPRECISION)) 
				}
			}
		}
		
		// amountFirst is used before a tag has appeared in the text
		// amount is the scan pattern to use after the tag has appeared in the text
		ScannerPattern amountFirstScanPattern = new ScannerPattern(scanner, CONDITION_AMOUNT, amountPattern, false, amountClosure)
		ScannerPattern amountScanPattern = new ScannerPattern(scanner, CONDITION_AMOUNT, amountPattern, false, amountClosure)
		
		// units
		Closure unitsAClosure = {
			currentUnits = scanner.group(1)
			for (int i = currentAmountIndex; i < amounts.size(); ++i) {
				((ParseAmount)amounts[i]).setUnits(currentUnits)
			}
		}
		
		// first word of units if a tag has appeared
		// unitsFirst - if a tag has not appeared, only parse one word of units
		ScannerPattern unitsScanPatternA = new ScannerPattern(scanner, CONDITION_UNITSA, tagWordPattern, false, unitsAClosure)
		ScannerPattern unitsFirstScanPatternA = new ScannerPattern(scanner, CONDITION_UNITSA, tagWordPattern, false, unitsAClosure)
		
		// second word of units
		ScannerPattern unitsScanPatternB = new ScannerPattern(scanner, CONDITION_UNITSB, tagWordPattern, false, {
			currentUnits = currentUnits + ' ' + scanner.group(1)
			for (int i = currentAmountIndex; i < amounts.size(); ++i) {
				((ParseAmount)amounts[i]).setUnits(currentUnits)
			}
		})

		ScannerPattern durationSynonymScanPattern = new ScannerPattern(scanner, CONDITION_TAGWORD, durationSynonymPattern, false, {
			foundDuration = true
			
			String modifier = scanner.group(1)
			
			ArrayList<Object> info = durationSynonymMap[modifier]
			retVal['durationType'] = (DurationType) info[DURATIONSYNONYM_TYPE]
			suffix = (String) info[DURATIONSYNONYM_SUFFIX]
			words.add((String) info[DURATIONSYNONYM_TAG])
		})
		
		ScannerPattern commentScanPattern = new ScannerPattern(scanner, CONDITION_COMMENT, commentWordPattern, false, {
			commentWords.add(scanner.group(1))
			inComment = true
		}, { (!foundTag) && ((!matchTag) || scanner.trying(CONDITION_AMOUNT)) })

		// set up structural rules
		
		timeWordScanPattern.followedBy([ atEndScanPattern, durationScanPattern, commentScanPattern, amountScanPattern, durationScanPattern, repeatScanPattern, durationSynonymScanPattern ])
		repeatScanPattern.followedBy([ atEndScanPattern, durationScanPattern, timeScanPattern, timeWordScanPattern, commentScanPattern ])
		repeatStartScanPattern.followedBy([tagWordScanPattern, timeScanPattern, timeWordScanPattern, durationStartScanPattern])
		durationScanPattern.followedBy([ atEndScanPattern, repeatScanPattern, timeScanPattern, timeWordScanPattern, commentScanPattern ])
		durationStartScanPattern.followedBy([tagWordScanPattern, timeScanPattern, timeWordScanPattern, repeatStartScanPattern])
		durationSynonymScanPattern.followedBy([ atEndScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern ])
		
		amountScanPattern.followedBy([atEndScanPattern, timeScanPattern, timeWordScanPattern, repeatScanPattern, durationScanPattern, unitsScanPatternA, anyScanPattern])
		amountFirstScanPattern.followedBy([atEndScanPattern, timeScanPattern, timeWordScanPattern, repeatScanPattern, durationScanPattern, unitsFirstScanPatternA, anyScanPattern])
		unitsScanPatternA.followedBy([atEndScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, unitsScanPatternB, anyScanPattern])
		unitsScanPatternB.followedBy([atEndScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, anyScanPattern])
		unitsFirstScanPatternA.followedBy([atEndScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, anyScanPattern])
		
		// start parsing!
		
		// time can appear at the beginning, and since it is context-free can just run it at the outset
		timeScanPattern.match()
		timeWordScanPattern.match()
		
		while (scanner.ready()) {
			if (words.size() == 0) { // try repeat at start
				amountFirstScanPattern.tryMatch()
				repeatStartScanPattern.tryMatch()
				durationStartScanPattern.tryMatch()
				durationSynonymScanPattern.tryMatch() { matchTag = false }
			}
			
			if (matchTag) tagWordScanPattern.match()
			repeatScanPattern.tryMatch() { matchTag = false }
			timeScanPattern.tryMatch() { matchTag = false }
			timeWordScanPattern.tryMatch() { matchTag = false }
			durationScanPattern.tryMatch() { matchTag = false }
			amountScanPattern.tryMatch() { matchTag = false }
			if (!matchTag) {
				repeatScanPattern.tryMatch() { matchTag = false }
				timeScanPattern.tryMatch() { matchTag = false }
				durationScanPattern.tryMatch() { matchTag = false }
				commentScanPattern.match() { inComment = true }
			}
			if (inComment) break
		}

		if (scanner.resetReady()) while (scanner.ready()) {
			commentScanPattern.match()
		}
		
		String description = ParseUtils.implode(words).toLowerCase()
		
		if (amounts.size() == 0) {
			amounts.add(new ParseAmount(new BigDecimal(1, mc), -1))
		}

		// Duration (start/stop) only when amount(s) not specified		
		if (amounts.size() > 1 || (amounts[0].getPrecision() != -1)) {
			retVal['durationType'] = DurationType.NONE
			suffix = ''
		}

		String comment = ''
		if (commentWords.size() > 0) {
			comment = ParseUtils.implode(commentWords)
		}
		
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
			if (lastSpace < 0) {
				lastSpace = Tag.MAXLENGTH - 1
			}
			def remainder = description.substring(lastSpace + 1)
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
			comment = '(' + (comment ? comment + ' ' : '') + remainder + ')'
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

		if (!description) description = "unknown"
		retVal['baseTag'] = Tag.look(description)
		String tagDescription = description
		if (suffix) tagDescription += ' ' + suffix
		retVal['tag'] = Tag.look(tagDescription)
		
		int index = 0
		Tag baseTag = retVal['baseTag']
		UnitGroupMap unitGroupMap = UnitGroupMap.theMap
		
		Map<String, ParseAmount> suffixToParseAmount = new HashMap<String, ParseAmount>()
		
		for (ParseAmount amount : amounts) {
			String units = amount.getUnits()
			
			if (!units) {
				if (foundDuration)
					amount.setTags(retVal['tag'], baseTag)
				else
					amount.setTags(baseTag, baseTag)
			} else {
				UnitRatio unitRatio = unitGroupMap.unitRatioForUnits(units)
				String amountSuffix
				if (unitRatio) {
					amountSuffix = unitRatio.getSuffix()
					amount.setUnitRatio(unitRatio)
				} else
					amountSuffix = units
					
				if (bloodPressureTags.contains(baseTag.getDescription())) {
					if (amountSuffix) {
						if (amountSuffix.equals("[pressure]")) {
							if (index == 0) amountSuffix = "[systolic]"
							else amountSuffix = "[diastolic]"
						}
					}
				}
				
				Tag tag = Tag.look(baseTag.getDescription() + ' ' + amountSuffix)
				
				amount.setTags(tag, baseTag)
				
				ParseAmount prevAmount = suffixToParseAmount.get(amountSuffix)
				
				if (prevAmount) {
					UnitRatio prevUnitRatio = prevAmount.getUnitRatio()
					if (prevUnitRatio != null && prevUnitRatio.unitGroup == unitRatio.unitGroup) {
						// merge amounts together, they are the same unit group
						// use first amount units
						prevAmount.amount = prevAmount.amount.add(new BigDecimal(amount.amount.doubleValue()
								* (unitRatio.ratio / prevUnitRatio.ratio)))
						amount.deactivate()
					} // otherwise do nothing, don't merge the amounts
				}
				
				if (amount.isActive()) {
					suffixToParseAmount.put(amountSuffix, amount)
				}
			}
			index++
		}
		
		retVal['amounts'] = amounts

		return retVal
	}
}

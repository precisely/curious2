package us.wearecurio.services

import java.math.MathContext
import java.math.RoundingMode
import java.text.DateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashSet
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.DurationType
import us.wearecurio.data.DecoratedUnitRatio
import us.wearecurio.data.RepeatType
import static us.wearecurio.model.Entry.DAYTICKS
import static us.wearecurio.model.Entry.HALFDAYTICKS
import static us.wearecurio.model.Entry.HOURTICKS
import static us.wearecurio.model.Entry.DEFAULT_AMOUNTPRECISION
import static us.wearecurio.model.Entry.DEFAULT_DATEPRECISION_SECS
import static us.wearecurio.model.Entry.VAGUE_DATE_PRECISION_SECS
import static us.wearecurio.model.Entry.MAXUNITSLENGTH
import static us.wearecurio.model.Entry.MAXCOMMENTLENGTH

import org.joda.time.DateTime
import org.joda.time.DateTimeFieldType

import com.google.javascript.jscomp.parsing.parser.trees.ThisExpressionTree;

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
import us.wearecurio.data.UnitRatio

import java.text.SimpleDateFormat

import us.wearecurio.utility.Utils
import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
class EntryParserService {

	private static Log log = LogFactory.getLog(this)

    static transactional = false

	static EntryParserService service
	
	static def set(EntryParserService s) { service = s }

	static EntryParserService get() { return service }
	
	static Integer parseInt(String str) {
		return str == null ? null : Integer.parseInt(str)
	}

	def parseMeta(String entryStr) {
		return parse(null, null, entryStr, null, null, null, false)
	}

	protected static boolean isSameDay(Date time1, Date time2) {
		long t1 = time1.getTime()
		long t2 = time2.getTime()
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

	protected static final Pattern timePattern    = ~/(?i)^(at )(([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)\b\s*|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?\b\s*|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm|a|p)\b\s*/
	protected static final Pattern timeWordPattern    = ~/(?i)^(at noon|at midnight|noon|midnight)\b\s*/
	protected static final Pattern tagWordPattern = ~/(?i)^([^0-9\(\)@\s\.=][^\(\)@\s=]*)($|\s*)/
	protected static final Pattern commentWordPattern = ~/^([^\s]+)($|\s*)/
	protected static final Pattern commentPattern = ~/^(\(.*\)|;.*|\/\/.*)$/
	protected static final Pattern amountPattern = ~/(?i)^(=?\s*)(-?\.\d+|-?\d+[\d,]*\.\d+|-?\d[\d,]*+|_\b)(\s*\/\s*(-?\.\d+|-?\d+\.\d+|-?\d+|_\b))?\s*/
	protected static final Pattern amountWordPattern = ~/(?i)^(none\b|zero\b|yes\b|no\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b)\s*/
	
	protected static Map<String, List> numberMap = [
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
		'_' : [ null, -1],
	]

	/**
	 * Regex pattern to match repeat modifiers
	 * 
	 * If you update repeatPattern, you need to update repeatMap
	 */
	protected static final Pattern repeatPattern = ~/^(repeat daily|repeat weekly|repeat monthly|repeat yearly|repeat annual|repeat annually|remind daily|remind weekly|remind monthly|remind yearly|remind annual|remind annually|button|repeat|pinned|bookmark|remind|reminder|daily|weekly|monthly|yearly|annual|annually)\b\s*/
	
	protected static final int REPEATMAP_SYNONYM = 0
	protected static final int REPEATMAP_TYPE = 1
	
	protected static Map<String, List> repeatMap = [
		'repeat':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'repeat daily':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'repeat weekly':['repeat weekly', RepeatType.WEEKLYCONCRETEGHOST],
		'repeat monthly':['repeat monthly', RepeatType.MONTHLYCONCRETEGHOST],
		'repeat yearly':['repeat yearly', RepeatType.YEARLYCONCRETEGHOST],
		'repeat annual':['repeat yearly', RepeatType.YEARLYCONCRETEGHOST],
		'repeat annually':['repeat yearly', RepeatType.YEARLYCONCRETEGHOST],
		'button':['bookmark', RepeatType.CONTINUOUSGHOST],
		'pinned':['bookmark', RepeatType.CONTINUOUSGHOST],
		'bookmark':['bookmark', RepeatType.CONTINUOUSGHOST],
		'remind':['remind', RepeatType.REMINDDAILYGHOST],
		'remind daily':['remind', RepeatType.REMINDDAILYGHOST],
		'remind weekly':['remind weekly', RepeatType.REMINDWEEKLYGHOST],
		'remind monthly':['remind monthly', RepeatType.REMINDMONTHLYGHOST],
		'remind yearly':['remind yearly', RepeatType.REMINDYEARLYGHOST],
		'remind annual':['remind yearly', RepeatType.REMINDYEARLYGHOST],
		'remind annually':['remind yearly', RepeatType.REMINDYEARLYGHOST],
		'reminder':['remind', RepeatType.REMINDDAILYGHOST],
		'daily':['repeat', RepeatType.DAILYCONCRETEGHOST],
		'weekly':['repeat weekly', RepeatType.WEEKLYCONCRETEGHOST],
		'monthly':['repeat monthly', RepeatType.MONTHLYCONCRETEGHOST],
		'yearly':['repeat yearly', RepeatType.YEARLYCONCRETEGHOST],
		'annual':['repeat yearly', RepeatType.YEARLYCONCRETEGHOST],
		'annually':['repeat yearly', RepeatType.YEARLYCONCRETEGHOST],
	]

	/**
	 * Regex pattern to match duration modifiers
	 * 
	 * If you update durationPattern, you need to update durationMap
	 */
	protected static final Pattern durationPattern = ~/^(start|starts|begin|begins|starting|beginning|started|begun|began|end|ends|stop|stops|finish|finished|ended|stopped|stopping|ending|finishing)\b\s*/
	
	protected static final int DURATIONMAP_SYNONYM = 0
	protected static final int DURATIONMAP_TYPE = 1
	
	protected static final Map<String, List> durationMap = [
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
	protected static final int DURATIONSYNONYM_WITHDURATIONAMOUNT_SUFFIX = 3
	protected static final int DURATIONSYNONYM_WITHDURATIONAMOUNT_TYPE = 4

	protected static final Map<String, List> durationSynonymMap = [
		'wake':['sleep', 'end', DurationType.END, 'end', DurationType.END],
		'wake up':['sleep', 'end', DurationType.END, 'end', DurationType.END],
		'woke':['sleep', 'end', DurationType.END, 'end', DurationType.END],
		'awakened':['sleep', 'end', DurationType.END, 'end', DurationType.END],
		'awoke':['sleep', 'end', DurationType.END, 'end', DurationType.END],
		'went to sleep':['sleep', 'start', DurationType.START, 'end', DurationType.END],
		'go to sleep':['sleep', 'start', DurationType.START, 'end', DurationType.END],
		'slept':['sleep', 'start', DurationType.START, 'end', DurationType.END],
		'sleep':['sleep', 'start', DurationType.START, 'end', DurationType.END],
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
		Tag baseTag // tag without units suffix
		Tag tag // tag including units suffix
		String tagSuffix
		String unitSuffix
		BigDecimal amount
		int precision
		DecoratedUnitRatio unitRatio
		String units
		DurationType durationType
		
		ParseAmount(BigDecimal amount, Integer precision, DurationType durationType, String unitSuffix = null) {
			this.tag = null
			this.amount = amount
			this.precision = precision == null ? DEFAULT_AMOUNTPRECISION : precision
			this.durationType = durationType
			this.unitSuffix = unitSuffix
		}
		
		ParseAmount(Tag baseTag, BigDecimal amount, Integer precision, String units, DecoratedUnitRatio unitRatio, DurationType durationType) {
			this.update(baseTag, amount, precision, units, unitRatio, durationType)
		}
		
		ParseAmount(Tag tag, Tag baseTag, BigDecimal amount, Integer precision, String units, DecoratedUnitRatio unitRatio, DurationType durationType) {
			this.update(tag, baseTag, amount, precision, units, unitRatio, durationType)
		}
		
		void update(Tag baseTag, BigDecimal amount, Integer precision, String units, DecoratedUnitRatio unitRatio, DurationType durationType) {
			this.units = units ?: ''
			this.unitRatio = unitRatio
			this.baseTag = baseTag
			if (unitRatio != null) {
				this.tag = Tag.look(baseTag.description + ' ' + unitRatio.tagSuffix)
				this.tagSuffix = unitRatio.tagSuffix
				this.unitSuffix = unitRatio.unitSuffix
			} else if (units) {
				this.tagSuffix = "[" + units + "]"
				this.unitSuffix = units
				this.tag = Tag.look(baseTag.description + ' ' + this.tagSuffix)
			} else {
				this.tag = baseTag
				this.tagSuffix = ""
				this.unitSuffix = ""
			}
			this.amount = amount
			this.precision = precision ?: DEFAULT_AMOUNTPRECISION
			this.durationType = durationType
		}
		
		void update(Tag tag, Tag baseTag, BigDecimal amount, Integer precision, String units, DecoratedUnitRatio unitRatio, DurationType durationType) {
			this.units = units ?: ''
			this.unitRatio = unitRatio
			this.setTags(tag, baseTag, unitRatio ? unitRatio.unitSuffix : (units ?: ""))
			
			this.amount = amount
			this.precision = precision ?: DEFAULT_AMOUNTPRECISION
			this.durationType = durationType
		}
		
		void deactivate() {
			tag = null
		}
		
		boolean isActive() {
			return tag != null
		}
		
		ParseAmount setTags(Tag tag, Tag baseTag, String unitSuffix) {
			this.tag = tag
			this.baseTag = baseTag
			if (baseTag == null || baseTag == tag) {
				this.tagSuffix = ""
			} else {
				this.tagSuffix = tag.getDescription().substring(baseTag.getDescription().length() + 1)
			}
			if ((!this.unitSuffix) && unitSuffix) {
				this.unitSuffix = unitSuffix
			}
			return this
		}
		
		boolean matches(Entry e) {
			return e.tag.id == tag.id
		}
		
		boolean matchesWeak(Entry e) {
			return e.fetchSuffix().equals(tagSuffix) || e.getUnits().equals(units)
		}
		
		String toString() {
			if (tag == null)
				return "ParseAmount(inactive)"
			return "ParseAmount(tag: " + tag?.getDescription() + ", baseTag: " + baseTag?.getDescription() + ", amount: " + amount + ", units: '" + units + "', tagSuffix: " + tagSuffix + ", unitSuffix:" + unitSuffix + ", precision: " + precision + ", durationType: " + durationType + ")"
		}
		
		boolean isDuration() {
			if (unitRatio == null) return false
			return unitRatio.unitGroup == UnitGroup.DURATION
		}
		
		DurationType fetchDurationType() {
			if (amount == null && isDuration()) {
				return DurationType.START
			}
			
			return durationType
		}
		
		Long durationInMilliseconds() {
			return unitRatio?.durationInMilliseconds(amount)
		}
		
		BigDecimal durationInHours() {
			if (!isDuration())
				return null
			if (amount == null)
				return null
			return (amount * unitRatio.ratio) / (1000.0g * 60.0g * 60.0g)
		}
		
		BigDecimal durationInDays() {
			if (!isDuration())
				return null
			if (amount == null)
				return null
			return (amount * unitRatio.ratio) / (1000.0g * 60.0g * 60.0g * 24.0g)
		}
	}
	
	protected static DateFormat gmtFormat
	protected static long GMTMIDNIGHTSECS

	static protected HashSet<String> bloodPressureTags = new HashSet<String>()
	
	static initialize() {
		gmtFormat = new SimpleDateFormat("k:mm")
		gmtFormat.setTimeZone(Utils.createTimeZone(0, "GMT", false))
		GMTMIDNIGHTSECS = (new SimpleDateFormat("yyyy-MM-dd h:mm a z").parse("2010-01-01 12:00 AM GMT").getTime() / 1000L).longValue()
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
		
		DecoratedUnitRatio unitRatio = UnitGroupMap.theMap.lookupDecoratedUnitRatio(units)
		String suffix
		if (unitRatio)
			suffix = unitRatio.tagSuffix
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
		
		DecoratedUnitRatio unitRatio = UnitGroupMap.theMap.lookupDecoratedUnitRatio(units)
		String suffix, coreSuffix
		if (unitRatio)
			suffix = unitRatio.tagSuffix
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
	static final int CONDITION_STRICTUNITSA = 8
	static final int CONDITION_STRICTUNITSB = 9
	static final int CONDITION_TIME = 10
	static final int CONDITION_COMMENT = 11
	static class ParserContext {
		ArrayList<ParseAmount> amounts = new ArrayList<ParseAmount>()
		
		boolean ghostAmount
		boolean foundTime
		boolean foundAMPM
		Integer hours
		Integer minutes
		
		LinkedList<String> words = new LinkedList<String>()
		LinkedList<String> commentWords = new LinkedList<String>()
		String suffix = ''
		String repeatSuffix = ''
		String withDurationAmountSuffix = ''
		DurationType withDurationAmountDurationType = null
		
		boolean foundRepeat = false
		boolean foundDuration = false
		boolean foundTag = false
		boolean matchTag = true
		boolean inComment = false
		
		int currentAmountIndex = 0
		String currentUnits = null
		
		Map retVal = [:]
	}
	
	ScannerPattern atEndScanPattern
	ScannerPattern anyScanPattern
	ScannerPattern timeScanPattern
	ScannerPattern timeWordScanPattern
	ScannerPattern tagWordScanPattern
	ScannerPattern repeatScanPattern
	ScannerPattern repeatStartScanPattern
	ScannerPattern durationScanPattern
	ScannerPattern durationStartScanPattern
	ScannerPattern amountFirstScanPattern
	ScannerPattern amountScanPattern
	ScannerPattern amountWordScanPattern
	ScannerPattern unitsScanPatternA
	ScannerPattern unitsFirstScanPatternA
	ScannerPattern unitsScanPatternB
	ScannerPattern unitsSingleScanPattern
	ScannerPattern durationSynonymScanPattern
	ScannerPattern fillerScanPattern
	ScannerPattern commentScanPattern

	EntryParserService() {
		initialize()
		
		atEndScanPattern = new ScannerPattern(CONDITION_ATEND)
		anyScanPattern = new ScannerPattern(CONDITION_ANY)		

		timeScanPattern = new ScannerPattern(CONDITION_TIME, timePattern, true, { PatternScanner scanner, ParserContext context ->
			context.foundTime = true
			context.hours = parseInt(scanner.group(14) ?: (scanner.group(9) ?: (scanner.group(3) ?: '0')))
			if (context.hours == 12) context.hours = 0
			context.minutes = parseInt(scanner.group(11) ?: (scanner.group(6) ?: '0'))
			if (scanner.group(8)?.equals('p') || scanner.group(13)?.equals('p') || scanner.group(18)?.equals('p') || scanner.group(18)?.equals('pm')) {
				context.foundAMPM = true
				context.hours += 12
			} else if (scanner.group(8)?.equals('a') || scanner.group(13)?.equals('a') || scanner.group(18)?.equals('a') || scanner.group(18)?.equals('am')) {
				context.foundAMPM = true
			}
		})
		
		timeWordScanPattern = new ScannerPattern(CONDITION_TIME, timeWordPattern, true, { PatternScanner scanner, ParserContext context ->
			context.foundTime = true
			String noonmid = scanner.group(1)
			if (noonmid != null) {
				if (noonmid.equals('noon') || noonmid.equals('at noon')) {
					context.hours = 12
					context.minutes = 0
					context.foundAMPM = true
					return
				} else if (noonmid.equals('midnight') || noonmid.equals('at midnight')) {
					context.hours = 0
					context.minutes = 0
					context.foundAMPM = true
					return
				}
			}
		})
		
		tagWordScanPattern = new ScannerPattern(CONDITION_TAGWORD, tagWordPattern, false, { PatternScanner scanner, ParserContext context ->
			context.words.add(scanner.group(1))
			context.foundTag = true
		})

		Closure repeatClosure = { PatternScanner scanner, ParserContext context ->
			context.foundRepeat = true
			
			String modifier = scanner.group(1)
			
			def info = repeatMap[modifier]
			context.retVal['repeatType'] = (RepeatType) info[REPEATMAP_TYPE]
			context.repeatSuffix = (String) info[REPEATMAP_SYNONYM]
		}
		
		repeatScanPattern = new ScannerPattern(CONDITION_REPEAT, repeatPattern, true, repeatClosure)
		repeatStartScanPattern = new ScannerPattern(CONDITION_REPEAT, repeatPattern, true, repeatClosure)
		
		Closure durationClosure = { PatternScanner scanner, ParserContext context ->
			context.foundDuration = true
			
			String modifier = scanner.group(1)
			
			List info = durationMap[modifier]
			context.retVal['durationType'] = (DurationType) info[DURATIONMAP_TYPE]
			context.suffix = (String) info[DURATIONMAP_SYNONYM]
			context.withDurationAmountSuffix = (String) info[DURATIONMAP_SYNONYM]
			context.withDurationAmountDurationType = (DurationType) info[DURATIONMAP_TYPE]
		}
		
		durationScanPattern = new ScannerPattern(CONDITION_DURATION, durationPattern, true, durationClosure)
		durationStartScanPattern = new ScannerPattern(CONDITION_DURATION, durationPattern, true, durationClosure)
		
		// amount
		Closure amountClosure = { PatternScanner scanner, ParserContext context ->
			String amountStr
			ArrayList<String> amountStrs = []
			context.currentAmountIndex = context.amounts.size()
			boolean twoDAmount = false
			
			amountStrs.add(scanner.group(2)?.replace(",",""))
			amountStr = scanner.group(4)
			
			if (amountStr != null) {
				amountStrs.add(amountStr.replace(",",""))
				twoDAmount = true
			}
			
			for (String amount in amountStrs) {
				if (numberMap.containsKey(amount)) {
					List num = numberMap.get(amount)
					context.amounts.add(new ParseAmount((BigDecimal)num[0], (int)num[1], DurationType.NONE))
				} else {
					context.amounts.add(new ParseAmount(new BigDecimal(amount, mc), (int)DEFAULT_AMOUNTPRECISION, DurationType.NONE))
				}
			}
		}
		
		Closure amountWordClosure = { PatternScanner scanner, ParserContext context ->
			String amountStr
			context.currentAmountIndex = context.amounts.size()
			boolean twoDAmount = false
			
			amountStr = scanner.group(1).replace(",","")
			
			if (numberMap.containsKey(amountStr)) {
				List num = numberMap.get(amountStr)
				context.amounts.add(new ParseAmount((BigDecimal)num[0], (int)num[1], DurationType.NONE))
			}
		}
		
		// amountFirst is used before a tag has appeared in the text
		// amount is the scan pattern to use after the tag has appeared in the text
		amountFirstScanPattern = new ScannerPattern(CONDITION_AMOUNT, amountPattern, false, amountClosure)
		amountScanPattern = new ScannerPattern(CONDITION_AMOUNT, amountPattern, false, amountClosure)
		amountWordScanPattern = new ScannerPattern(CONDITION_AMOUNT, amountWordPattern, false, amountWordClosure)
		
		// units
		Closure unitsAClosure = { PatternScanner scanner, ParserContext context ->
			context.currentUnits = scanner.group(1).toLowerCase()
			for (int i = context.currentAmountIndex; i < context.amounts.size(); ++i) {
				((ParseAmount)context.amounts[i]).setUnits(context.currentUnits)
			}
		}
		
		// first word of units if a tag has appeared
		// unitsFirst - if a tag has not appeared, only parse one word of units
		unitsScanPatternA = new ScannerPattern(CONDITION_UNITSA, tagWordPattern, false, unitsAClosure)
		unitsFirstScanPatternA = new ScannerPattern(CONDITION_UNITSA, tagWordPattern, false, unitsAClosure)
		
		// second word of units
		ScannerPattern unitsScanPatternB = new ScannerPattern(CONDITION_UNITSB, tagWordPattern, false, { PatternScanner scanner, ParserContext context ->
			context.currentUnits = context.currentUnits + ' ' + scanner.group(1).toLowerCase()
			for (int i = context.currentAmountIndex; i < context.amounts.size(); ++i) {
				((ParseAmount)context.amounts[i]).setUnits(context.currentUnits)
			}
		})
		
		// this scanner pattern includes a check to see if the units are actually a standard unit in the database or not. If not, the scanner pattern doesn't match and
		// the pattern will backtrack (fail)
		ScannerPattern unitsSingleScanPattern = new ScannerPattern(CONDITION_STRICTUNITSA, tagWordPattern, false, unitsAClosure)
		
		durationSynonymScanPattern = new ScannerPattern(CONDITION_TAGWORD, durationSynonymPattern, false, { PatternScanner scanner, ParserContext context ->
			context.foundDuration = true
			
			String modifier = scanner.group(1)
			
			List info = durationSynonymMap[modifier]
			context.retVal['durationType'] = (DurationType) info[DURATIONSYNONYM_TYPE]
			context.suffix = (String) info[DURATIONSYNONYM_SUFFIX]
			context.withDurationAmountSuffix = (String) info[DURATIONSYNONYM_WITHDURATIONAMOUNT_SUFFIX]
			context.withDurationAmountDurationType = (DurationType) info[DURATIONSYNONYM_WITHDURATIONAMOUNT_TYPE]
			context.words.add((String) info[DURATIONSYNONYM_TAG])
		})
		
		fillerScanPattern = new ScannerPattern(CONDITION_COMMENT, commentWordPattern, false, { PatternScanner scanner, ParserContext context ->
			context.commentWords.add(scanner.group(1))
			context.inComment = true
		}, {  PatternScanner scanner, ParserContext context -> (!context.foundTag) && ((!context.matchTag) || scanner.trying(CONDITION_AMOUNT)) })

		commentScanPattern = new ScannerPattern(CONDITION_COMMENT, commentPattern, false, { PatternScanner scanner, ParserContext context ->
			context.commentWords.add(scanner.group(1))
			context.inComment = true
		})

		// set up structural rules
		
		timeWordScanPattern.followedBy([ atEndScanPattern, commentScanPattern, durationScanPattern, fillerScanPattern, amountScanPattern, amountWordScanPattern, durationScanPattern, repeatScanPattern, durationSynonymScanPattern ])
		repeatScanPattern.followedBy([ atEndScanPattern, commentScanPattern, durationScanPattern, timeScanPattern, timeWordScanPattern, fillerScanPattern ])
		repeatStartScanPattern.followedBy([tagWordScanPattern, timeScanPattern, timeWordScanPattern, durationStartScanPattern])
		durationScanPattern.followedBy([ atEndScanPattern, repeatScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, amountWordScanPattern, commentScanPattern, fillerScanPattern ])
		durationStartScanPattern.followedBy([tagWordScanPattern, timeScanPattern, timeWordScanPattern, repeatStartScanPattern])
		durationSynonymScanPattern.followedBy([ atEndScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, amountWordScanPattern, commentScanPattern, repeatScanPattern ])
		
		amountScanPattern.followedBy([atEndScanPattern, commentScanPattern, timeScanPattern, timeWordScanPattern, repeatScanPattern, durationScanPattern, amountScanPattern, unitsScanPatternA, anyScanPattern])
		amountWordScanPattern.followedBy([atEndScanPattern, commentScanPattern, timeScanPattern, timeWordScanPattern, repeatScanPattern, durationScanPattern, amountScanPattern, unitsSingleScanPattern])
		amountFirstScanPattern.followedBy([atEndScanPattern, commentScanPattern, timeScanPattern, timeWordScanPattern, repeatScanPattern, durationScanPattern, unitsFirstScanPatternA, anyScanPattern])
		unitsScanPatternA.followedBy([atEndScanPattern, commentScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, unitsScanPatternB, anyScanPattern])
		unitsScanPatternB.followedBy([atEndScanPattern, commentScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, anyScanPattern])
		unitsSingleScanPattern.followedBy([atEndScanPattern, commentScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern])
		unitsFirstScanPatternA.followedBy([atEndScanPattern, commentScanPattern, timeScanPattern, timeWordScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, anyScanPattern])
		
		commentScanPattern.followedBy([atEndScanPattern])
	}
	
	static final int UPDATEMODE_NONE = 0
	static final int UPDATEMODE_UPDATE = 1
	static final int UPDATEMODE_TUTORIAL = 2
	
	def parse(Date time, String timeZoneName, String entryStr, Long repeatTypeId, Date repeatEnd, Date baseDate, boolean defaultToNow = true, int updateMode = UPDATEMODE_NONE) {
		log.debug "EntryParserService.parse() time:" + time + ", timeZoneName:" + timeZoneName + ", entryStr:" + entryStr + ", baseDate:" + baseDate + ", defaultToNow:" + defaultToNow

		if (entryStr == '') return null // no input
		
		boolean forUpdate = (updateMode == UPDATEMODE_UPDATE)
		boolean dontChangeDayOnLateTime = (updateMode != UPDATEMODE_NONE)
		
		// truncate time to hours:minutes
		
		time = new DateTime(time).withField(DateTimeFieldType.secondOfMinute(), 0).toDate()
		
		Date date = baseDate
		Long baseDateTime = baseDate?.getTime()
		boolean today = isToday(time, date)

		// [time] [tag] <[amount] <[units]>>... <repeat|remind|button> (<[comment]>)
		//
		// OR
		//
		// [tag] <[amount] <[units]>>... <[time]> <repeat|remind|button> (<[comment]>)
		//
		// <repeat|remind|button> ahead of the above

		ParserContext context = new ParserContext()
		PatternScanner<ParserContext> scanner = new PatternScanner<ParserContext>(entryStr, context)
		
		// start by giving the time a precision of 3 minutes
		if (date == null)
			context.retVal['datePrecisionSecs'] = 0
		else
			context.retVal['datePrecisionSecs'] = DEFAULT_DATEPRECISION_SECS
			
		context.retVal['repeatType'] = RepeatType.get(repeatTypeId)
		context.retVal['repeatEnd'] = repeatEnd
		context.retVal['timeZoneName'] = timeZoneName
		context.retVal['today'] = today

		// start parsing!
		
		// time can appear at the beginning, and since it is context-free can just run it at the outset
		timeScanPattern.match(scanner)
		timeWordScanPattern.match(scanner)
		
		Closure stopMatchingTag = { PatternScanner patternScanner, ParserContext parserContext ->
			parserContext.matchTag = false
		}
		
		while (scanner.ready()) {
			if (context.words.size() == 0) { // try repeat at start
				amountFirstScanPattern.tryMatch(scanner)
				repeatStartScanPattern.tryMatch(scanner)
				durationStartScanPattern.tryMatch(scanner)
				durationSynonymScanPattern.tryMatch(scanner, stopMatchingTag)
			}
			
			if (context.matchTag) tagWordScanPattern.match(scanner)
			repeatScanPattern.tryMatch(scanner, stopMatchingTag)
			timeScanPattern.tryMatch(scanner, stopMatchingTag)
			timeWordScanPattern.tryMatch(scanner, stopMatchingTag)
			durationScanPattern.tryMatch(scanner, stopMatchingTag)
			amountScanPattern.tryMatch(scanner, stopMatchingTag)
			amountWordScanPattern.tryMatch(scanner, stopMatchingTag)
			if (!context.matchTag) {
				repeatScanPattern.tryMatch(scanner, stopMatchingTag)
				timeScanPattern.tryMatch(scanner, stopMatchingTag)
				durationScanPattern.tryMatch(scanner, stopMatchingTag)
				fillerScanPattern.match(scanner) { PatternScanner patternScanner, ParserContext parserContext -> parserContext.inComment = true }
			}
			if (context.inComment) break
		}

		if (scanner.resetReady()) while (scanner.ready()) {
			fillerScanPattern.match(scanner)
		}
		
		String description = ParseUtils.implode(context.words).toLowerCase()
		
		RepeatType repeatType = (RepeatType) context.retVal['repeatType']
		
		if (repeatType != null && repeatType.isRepeat()) {
			context.retVal['durationType'] = DurationType.NONE
			context.retVal['tag'] = context.retVal['baseTag']
		}
		
		String comment = ''
		if (context.commentWords.size() > 0) {
			comment = ParseUtils.implode(context.commentWords)
		}
		
		if (context.repeatSuffix)
			if (comment)
				comment += ' ' + context.repeatSuffix
			else
				comment = context.repeatSuffix
		
		if (!context.foundTime) {
			if (date != null) {
				date = time;
				if (!(defaultToNow && today && (!forUpdate))) { // only default to now if entry is for today and not editing
					date = new Date(baseDate.getTime() + HALFDAYTICKS);
					context.retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				} else {
					if (!isSameDay(time, baseDate)) {
						context.retVal['status'] = "Entering event for today, not displayed";
					}
				}
			}
		} else if (date != null) {
			if (context.hours == null && date != null) {
				date = time;
				if ((!defaultToNow) || forUpdate) {
					date = new Date(date.getTime() + 12 * 3600000L);
					context.retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				} else if (!isSameDay(time, baseDate)) {
					context.retVal['status'] = "Entering event for today, not displayed";
				}
			} else {
				long ts = date.getTime();
				if (context.hours >= 0 && context.hours < 24) {
					ts += context.hours * 3600000L;
				}
				if (context.minutes == null) context.minutes = 0;
				if (context.minutes >=0 && context.minutes < 60) {
					ts += context.minutes * 60000L;
				}
				date = new Date(ts);
			}
		} else {
			date = null;
		}

		for (int i = 0; i < context.amounts.size(); ++i) {
			if (context.amounts[i].getUnits()?.equals("at"))
				context.amounts[i].setUnits('')
			String units = context.amounts[i].getUnits()
			if (units?.length() > MAXUNITSLENGTH) {
				context.amounts[i].setUnits(units.substring(0, MAXUNITSLENGTH))
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
		
		context.retVal['comment'] = comment
		
		log.debug("retVal: parse " + context.retVal)

		if (context.retVal['repeatType'] != null) {
			if (!((RepeatType)context.retVal['repeatType']).isReminder()) {
				if (((RepeatType)context.retVal['repeatType']).isContinuous()) { // continuous repeat are always vague date precision
					date = new Date(baseDate.getTime() + HALFDAYTICKS);
					context.retVal['datePrecisionSecs'] = VAGUE_DATE_PRECISION_SECS;
				}
			}
		}

		if (today && (!dontChangeDayOnLateTime)) {
			// if base date is today and time is greater than one hour from now, assume
			// user meant yesterday, unless the element is a ghost
			if (context.retVal['repeatType'] == null) {
				if (date.getTime() > time.getTime() + HOURTICKS) {
					// if am/pm is not specified, then assume user meant half a day ago
					if (!context.foundAMPM) {
						date = new Date(date.getTime() - HALFDAYTICKS)
						context.retVal['status'] = "Entering event a half day ago";
					} else {
						date = new Date(date.getTime() - DAYTICKS);
						context.retVal['status'] = "Entering event for yesterday";
					}
				} else
				// if base date is today and AM/PM isn't specified and time is more than 12 hours ago,
				// enter as just 12 hours ago
				if ((!context.foundAMPM) && (time.getTime() - date.getTime() > HALFDAYTICKS)) {
					date = new Date(date.getTime() + HALFDAYTICKS)
				}
			}
		}

		context.retVal['date'] = date

		if (context.ghostAmount) {
			if (context.retVal['repeatType'] == null)
				context.retVal['repeatType'] = RepeatType.GHOST
			else
				context.retVal['repeatType'] = ((RepeatType)context.retVal['repeatType']).makeGhost()
		}

		if (!description) description = "cannot understand what you typed"
		Tag baseTag = Tag.look(description)
		if (!baseTag) baseTag = Tag.look("cannot understand what you typed")
		context.retVal['baseTag'] = baseTag
		String tagDescription = description
		UnitGroupMap unitGroupMap = UnitGroupMap.theMap
		if (context.suffix) {
			boolean durationAmount = false
			for (ParseAmount amount : context.amounts) {
				if (amount.units) {
					DecoratedUnitRatio unitRatio = unitGroupMap.lookupDecoratedUnitRatio(amount.units)
					if (unitRatio?.isDuration()) {
						durationAmount = true
						break
					}
				}
			}
			if (durationAmount) {
				context.retVal['hasDurationAmount'] = true
				tagDescription += ' ' + context.withDurationAmountSuffix
				context.retVal['durationType'] = context.withDurationAmountDurationType
			} else {
				tagDescription += ' ' + context.suffix
			}
		}
		Tag tag = Tag.look(tagDescription)
		context.retVal['tag'] = tag
		
		int index = 0
		
		boolean bloodPressure = bloodPressureTags?.contains(description)
		
		ParseAmount prevAmount = null
		String prevSuffix = null
		
		for (ParseAmount amount : context.amounts) {
			if (bloodPressure && (!amount.units)) {
				if (index == 0) amount.units = "over"
				else amount.units = "mmHg"
			}
				
			String units = amount.getUnits()
			String amountSuffix
			
			if (!units) {
				if (context.foundDuration)
					amount.setTags((Tag)context.retVal['tag'], baseTag, "")
				else
					amount.setTags(baseTag, baseTag, "")
			} else {
				DecoratedUnitRatio unitRatio = unitGroupMap.lookupDecoratedUnitRatio(units)
				if (unitRatio) {
					amountSuffix = unitRatio.tagSuffix
					amount.unitRatio = unitRatio
				} else {
					amountSuffix = '[' + units + ']'
					// null duration amount equals duration start
				}
					
				if (bloodPressure) {
					if (amountSuffix) {
						if (amountSuffix.equals("[pressure]")) {
							if (index == 0) amountSuffix = "[systolic]"
							else amountSuffix = "[diastolic]"
						}
					}
				}
				
				Tag amountTag = Tag.look(baseTag.getDescription() + ' ' + amountSuffix)
				
				amount.setTags(amountTag, baseTag, unitRatio?.unitSuffix)
				
				if (prevAmount != null) {
					DecoratedUnitRatio prevUnitRatio = prevAmount.getUnitRatio()
					if (prevUnitRatio != null && unitRatio != null && prevUnitRatio.unitGroup == unitRatio.unitGroup && (prevSuffix == amountSuffix || ((unitRatio.unitGroup == UnitGroup.DURATION) && (!prevUnitRatio.customSuffix) && unitRatio.customSuffix)) && prevUnitRatio.ratio != unitRatio.ratio) {
						// merge amounts together, they are the same unit group, provided prevAmount's unitSuffix is compatible
						// use first amount units
						prevAmount.amount = prevUnitRatio.addAmount(prevAmount.amount, amount.amount, unitRatio)
						prevAmount.setTags(amountTag, baseTag, unitRatio.unitSuffix) // update tags to the last suffix
						amount.deactivate()
						amount = prevAmount
						amountSuffix = prevSuffix
					} // otherwise do nothing, don't merge the amounts
				}
				
				prevAmount = amount
				prevSuffix = amountSuffix
			}
			index++
		}
		
		DurationType durationType = (DurationType) context.retVal['durationType']
		
		if (context.amounts.size() == 0) {
			context.amounts.add(new ParseAmount(tag, baseTag, 1.0g, -1, "", null, DurationType.NONE))
		}

		if (durationType != null && durationType != DurationType.NONE) {
			boolean setDuration = false
			for (ParseAmount amount : context.amounts) {
				if (!amount.isActive())
					continue
				if (amount.isDuration() || amount.precision < 0) {
					amount.durationType = durationType
					setDuration = true
				} else {
					amount.durationType = DurationType.NONE
				}
			}
			if (!setDuration)
				context.amounts.add(new ParseAmount(tag, baseTag, 1.0g, -1, "", null, DurationType.NONE))
		}
				
		context.retVal['amounts'] = context.amounts

		return context.retVal
	}
}

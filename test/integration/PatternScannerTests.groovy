import static org.junit.Assert.*

import java.util.regex.Pattern

import us.wearecurio.parse.ParseUtils
import us.wearecurio.parse.ScannerPattern

import grails.test.*
import grails.test.mixin.*

import org.junit.*

import groovy.transform.TypeChecked

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.parse.PatternScanner
import static us.wearecurio.parse.PatternScanner.*

@TypeChecked
class PatternScannerTests extends CuriousTestCase {
	static transactional = true

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	protected static final Pattern timePattern    = ~/(?i)^(@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)\b\s*|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?\b\s*|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm)\b\s*/
	protected static final Pattern tagWordPattern = ~/(?i)^([^0-9\(\)@\s\.:=][^\(\)@\s:=]*)($|\s*)/
	protected static final Pattern commentWordPattern = ~/^([^\s]+)($|\s*)/
	protected static final Pattern amountPattern = ~/(?i)^([:=]\s*)?(-?\.\d+|-?\d+\.\d+|-?\d+|-|_\b|__\b|___\b|none\b|zero\b|yes\b|no\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b)(\s*\/\s*(-?\.\d+|-?\d+\.\d+|-?\d+|-|_\b|-\b|__\b|___\b|zero\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b))?\s*/
	protected static final Pattern endTagPattern = ~/(?i)^(yes|no)\b\s*/
	
	protected static final Pattern repeatPattern = ~/^(repeat daily|repeat weekly|remind daily|remind weekly|reminder daily|reminder weekly|daily repeat|daily remind|daily reminder|weekly repeat|weekly remind|weekly reminder|repeat|button|pinned|favorite|remind|reminder|daily|weekly)\b\s*/
	protected static final Pattern durationPattern = ~/^(start|starts|begin|begins|starting|beginning|started|begun|began|end|ends|stop|stops|finish|finished|ended|stopped|stopping|ending|finishing)\b\s*/
	protected static final Pattern durationSynonymPattern = ~/^(wake up|went to sleep|go to sleep|wake|woke|awakened|awoke|slept)\b\s*/
	
	static final int CONDITION_TAGWORD = 1
	static final int CONDITION_TAGSEPARATOR = 2
	static final int CONDITION_DURATION = 3
	static final int CONDITION_REPEAT = 4
	static final int CONDITION_AMOUNT = 5
	static final int CONDITION_UNITSA = 6
	static final int CONDITION_UNITSB = 7
	static final int CONDITION_TIME = 8
	static final int CONDITION_COMMENT = 9
	
	static def matchStartPattern(String text) {
		PatternScanner scanner = new PatternScanner(text)
		
		def retVal = [:]

		LinkedList<String> words = new LinkedList<String>()
		LinkedList<String> commentWords = new LinkedList<String>()
		LinkedList<String> amounts = new LinkedList<String>()
		LinkedList<String> units = new LinkedList<String>()
		
		boolean matchTag = true
		boolean inComment = false

		ScannerPattern atEndScanPattern = new ScannerPattern(scanner, CONDITION_ATEND)
		ScannerPattern anyScanPattern = new ScannerPattern(scanner, CONDITION_ANY)
		ScannerPattern tagWordScanPattern = new ScannerPattern(scanner, CONDITION_TAGWORD, tagWordPattern, false, {
			words.add(scanner.group(1))
		})
		ScannerPattern durationScanPattern = new ScannerPattern(scanner, CONDITION_DURATION, durationPattern, true, {
			retVal['suffix'] = scanner.group(1)
		})
		ScannerPattern repeatScanPattern = new ScannerPattern(scanner, CONDITION_REPEAT, repeatPattern, true, {
			retVal['repeat'] = scanner.group(1)
		})
		ScannerPattern repeatStartScanPattern = new ScannerPattern(scanner, CONDITION_REPEAT, repeatPattern, true, {
			retVal['repeat'] = scanner.group(1)
		})
		ScannerPattern timeScanPattern = new ScannerPattern(scanner, CONDITION_TIME, timePattern, true, {
			retVal['time'] = scanner.group(0)
		})
		ScannerPattern commentScanPattern = new ScannerPattern(scanner, CONDITION_COMMENT, commentWordPattern, false, {
			commentWords.add(scanner.group(1))
			inComment = true
		}, { (!matchTag) || scanner.trying(CONDITION_AMOUNT) })
		ScannerPattern amountScanPattern = new ScannerPattern(scanner, CONDITION_AMOUNT, amountPattern, false, {
			String amountStr
			boolean twoDAmount = false
			
			amounts.add(scanner.group(2))
			amountStr = scanner.group(4)
			
			if (amountStr) {
				amounts.add(amountStr)
				twoDAmount = true
			}
		})
		
		// first word of units
		ScannerPattern unitsScanPatternA = new ScannerPattern(scanner, CONDITION_UNITSA, tagWordPattern, false, {
			units.add(scanner.group(1))
		})
		
		// second word of units
		ScannerPattern unitsScanPatternB = new ScannerPattern(scanner, CONDITION_UNITSB, tagWordPattern, false, {
			units.add(scanner.group(1))
		})

		repeatStartScanPattern.followedBy([ tagWordScanPattern ])
		repeatScanPattern.followedBy([ atEndScanPattern, durationScanPattern, timeScanPattern, commentScanPattern ])
		durationScanPattern.followedBy([ atEndScanPattern, repeatScanPattern, timeScanPattern, commentScanPattern ])
		
		amountScanPattern.followedBy([atEndScanPattern, timeScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, unitsScanPatternA, anyScanPattern])
		unitsScanPatternA.followedBy([atEndScanPattern, timeScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, unitsScanPatternB, anyScanPattern])
		unitsScanPatternB.followedBy([atEndScanPattern, timeScanPattern, amountScanPattern, repeatScanPattern, durationScanPattern, anyScanPattern])
		
		while (scanner.ready()) {
			if (words.size() == 0) { // try repeat at start
				repeatStartScanPattern.tryMatch()
			}
			
			if (matchTag) tagWordScanPattern.match()
			repeatScanPattern.tryMatch() { matchTag = false }
			timeScanPattern.tryMatch() { matchTag = false }
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
		
		retVal['remainder'] = scanner.remainder()
		retVal['tag'] = ParseUtils.implode(words)
		retVal['comment'] = ParseUtils.implode(commentWords)
		retVal['amounts'] = amounts
		retVal['units'] = units
		
		return retVal
	}
	
	@Test
	void testPatternBacktrack() {
		def r
		
		r = matchStartPattern("this is a test start")
		
		assert r['tag'] == "this is a test"
		assert r['suffix'] == "start"
		assert r['remainder'] == ""
		
		r = matchStartPattern("this is a start test")
		
		assert r['tag'] == "this is a start test"
		assert !r['suffix']
		assert !r['remainder']
		
		r = matchStartPattern("this is a start 9:30pm test")
		
		assert r['tag'] == "this is a"
		assert r['suffix'] == "start"
		assert r['time'] == "9:30pm "
		assert r['comment'] == "test"
		
		r = matchStartPattern("make sure time only parsed once 8am 9pm wow")
		
		assert r['tag'] == "make sure time only parsed once"
		assert r['time'] == "8am "
		assert r['amounts'].toString() == "[9]"
		assert r['units'].toString() == "[pm, wow]"
		
		r = matchStartPattern("this is an end whoo 9:30pm test")
		
		assert r['tag'] == "this is an end whoo"
		assert !r['suffix']
		assert r['time'] == "9:30pm "
		assert r['comment'] == "test"

		r = matchStartPattern("this is an (end whoo 9:30pm test)")
		
		assert r['tag'] == "this is an"
		assert !r['suffix']
		assert !r['time']
		assert r['comment'] == "(end whoo 9:30pm test)"

		r = matchStartPattern("repeat this is an (end whoo 9:30pm test)")
		
		assert r['tag'] == "this is an"
		assert !r['suffix']
		assert !r['time']
		assert r['repeat'] == 'repeat'
		assert r['comment'] == "(end whoo 9:30pm test)"

		r = matchStartPattern("repeat daily this is an (end whoo 9:30pm test)")
		
		assert r['tag'] == "this is an"
		assert !r['suffix']
		assert !r['time']
		assert r['repeat'] == 'repeat daily'
		assert r['comment'] == "(end whoo 9:30pm test)"

		r = matchStartPattern("hello 5")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5]"
		assert r['units'].toString() == "[]"
		
		r = matchStartPattern("hello 5 bees")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5]"
		assert r['units'].toString() == "[bees]"
		
		r = matchStartPattern("hello 5 start")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5]"
		assert r['units'].toString() == "[]"
		assert r['suffix'] == "start"
		
		r = matchStartPattern("hello 5 start at 3:30pm")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5]"
		assert r['units'].toString() == "[]"
		assert r['suffix'] == "start"
		assert r['time'] == "at 3:30pm"
		
		r = matchStartPattern("hello 5 bees start")

		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5]"
		assert r['units'].toString() == "[bees]"
		assert r['suffix'] == "start"
		
		r = matchStartPattern("hello 5 calories 18 steps")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5, 18]"
		assert r['units'].toString() == "[calories, steps]"
		assert !r['suffix']
		
		r = matchStartPattern("hello 5 calories 18 steps repeat")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5, 18]"
		assert r['units'].toString() == "[calories, steps]"
		assert r['repeat'] == "repeat"
		
		r = matchStartPattern("hello 5 calories 18 steps repeat comment")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5, 18]"
		assert r['units'].toString() == "[calories, steps]"
		assert r['repeat'] == "repeat"
		assert r['comment'] == "comment"
		
		r = matchStartPattern("hello 5 calories (repeat)")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5]"
		assert r['units'].toString() == "[calories]"
		assert !r['repeat']
		assert r['comment'] == "(repeat)"
		
		r = matchStartPattern("hello 5 calories 18 (renew)")
		
		assert r['tag'] == "hello"
		assert r['amounts'].toString() == "[5, 18]"
		assert r['units'].toString() == "[calories]"
		assert !r['repeat']
		assert r['comment'] == "(renew)"
		
		r = matchStartPattern("bread 5 repeat")
		
		assert r['tag'] == "bread"
		assert r['amounts'].toString() == "[5]"
		assert r['repeat'] == "repeat"
		assert !r['comment']
		
		r = matchStartPattern("bread 5 3pm repeat")
		
		assert r['tag'] == "bread"
		assert r['amounts'].toString() == "[5]"
		assert r['time'] == "3pm "
		assert r['repeat'] == "repeat"
		assert !r['comment']
		
		r = matchStartPattern("bread 5 repeat ")
		
		assert r['tag'] == "bread"
		assert r['amounts'].toString() == "[5]"
		assert r['repeat'] == "repeat"
		assert !r['comment']

		r = matchStartPattern("bread#3: 5 bags")
		
		assert r['tag'] == "bread#3"
		assert r['amounts'].toString() == "[5]"
		assert r['units'].toString() == "[bags]"
		assert !r['comment']
		
		r = matchStartPattern("test - foo")
		
		assert r['tag'] == "test"
		assert r['amounts'].toString() == "[-]"
		assert r['units'].toString() == "[foo]"
		assert !r['comment']
	}
}

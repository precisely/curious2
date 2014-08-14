import static org.junit.Assert.*

import java.util.regex.Pattern

import us.wearecurio.parse.ParseUtils

import grails.test.*
import grails.test.mixin.*

import org.junit.*

import groovy.transform.TypeChecked

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.parse.PatternScanner

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

	protected static final Pattern timePattern = ~/(?i)^(@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)\b|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?\b|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm)\b/
	protected static final Pattern timeEndPattern = ~/(?i)^(@\s*|at )(noon|midnight|([012]?[0-9])((:|h)([0-5]\d))?\s?((a|p)m?)?)\b|([012]?[0-9])(:|h)([0-5]\d)\s?((a|p)m?)?\b|([012]?[0-9])((:|h)([0-5]\d))?\s?(am|pm)\b$/
	protected static final Pattern tagWordPattern = ~/(?i)^([^0-9\(\)@\s\.:=][^\(\)@\s:=]*)/
	protected static final Pattern startEndPattern = ~/(start|starts|begin|begins|starting|beginning|started|begun|began|end|ends|stop|stops|finish|finished|ended|stopped|stopping|ending|finishing)\b/
	protected static final Pattern repeatPattern = ~/(repeat|repeat daily|repeat weekly|pinned|remind|remind daily|remind weekly|reminder|reminder daily|reminder weekly|daily|daily repeat|daily remind|daily reminder|weekly|weekly repeat|weekly remind|weekly reminder)\b/
	protected static final Pattern commentWordPattern = ~/^([^\s]+)/
	protected static final Pattern amountPattern = ~/(?i)^(-?\.\d+|-?\d+\.\d+|\d+|_\b|-\b|__\b|___\b|zero\b|yes\b|no\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b)(\s*\/\s*(-?\.\d+|-?\d+\.\d+|\d+|_\b|-\b|__\b|___\b|zero\b|one\b|two\b|three\b|four\b|five\b|six\b|seven\b|eight\b|nine\b))?/
	protected static final Pattern endTagPattern = ~/(?i)^(yes|no)\b/
	protected static final Pattern unitsPattern = ~/(?i)^(([^0-9\(\)@\s\.:][^\(\)@\s:]*)(\s+([^0-9\(\)@\s\.:][^\(\)@\s:]*))*)\b/
	protected static final Pattern tagAmountSeparatorPattern = ~/(?i)^[:=]\s*/
	
	static def matchStartPattern(String text) {
		PatternScanner scanner = new PatternScanner(text)
		
		def retVal = [:]

		LinkedList<String> words = new LinkedList<String>()	
		boolean start = false
		
		while (scanner.matchField(tagWordPattern)) {
			words.add(scanner.group(1))
			
			if (scanner.tryField(startEndPattern, {
				retVal['suffix'] = scanner.group(1)
			})) {
				if ((!scanner.followedByEnd())
				&& (!scanner.followedBy(timePattern, {
					retVal['time'] = scanner.group(0)
				})))
					scanner.backtrackTry()
				else
					break
			}
		}
		
		retVal['remainder'] = scanner.remainder()
		retVal['tag'] = ParseUtils.implode(words)
		
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
		assert r['remainder'] == "test"
		
		r = matchStartPattern("this is an end whoo 9:30pm test")
		
		assert r['tag'] == "this is an end whoo"
		assert !r['suffix']
		assert r['remainder'] == "9:30pm test"
	}
}

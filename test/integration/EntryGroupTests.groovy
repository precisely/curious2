import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.DurationType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EntryParserService

import groovy.transform.TypeChecked

import org.joda.time.DateTimeZone
import org.junit.*
import grails.test.mixin.*
import us.wearecurio.utility.Utils

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
class EntryGroupTests extends CuriousTestCase {
	static transactional = true
	
	EntryParserService entryParserService

	DateFormat dateFormat
	Date earlyBaseDate
	Date earlyBaseDate2
	Date currentTime
	Date currentTime2
	Date slightDifferentCurrentTime
	Date endTime
	String timeZone // simulated server time zone
	String timeZone2 // simulated server time zone
	TimeZoneId timeZoneId
	TimeZoneId timeZoneId2
	DateTimeZone dateTimeZone
	Date yesterdayBaseDate
	Date baseDate
	Date baseDateShifted
	Date baseDate2
	Date tomorrowBaseDate
	Date tomorrowCurrentTime
	Date dayAfterTomorrowBaseDate
	Date dayAfterDayAfterTomorrowBaseDate
	Date lateBaseDate
	Date timeZone2BaseDate
	Date lateCurrentTime
	Date microlateCurrentTime
	Date veryLateBaseDate
	
	@Before
	void setUp() {
		super.setUp()
		
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		Utils.resetForTesting()
		
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		timeZone = "America/Los_Angeles"
		timeZone2 = "America/New_York"
		timeZoneId = TimeZoneId.look(timeZone)
		timeZoneId2 = TimeZoneId.look(timeZone2)
		dateTimeZone = timeZoneId.toDateTimeZone()
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		earlyBaseDate = dateFormat.parse("June 25, 2010 12:00 am")
		earlyBaseDate2 = dateFormat.parse("June 24, 2010 11:00 pm")
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		currentTime2 = dateFormat.parse("July 1, 2010 5:00 pm")
		slightDifferentCurrentTime = dateFormat.parse("July 1, 2010 4:00 pm")
		lateCurrentTime = dateFormat.parse("July 3, 2010 3:30 pm")
		endTime = dateFormat.parse("July 1, 2010 5:00 pm")
		yesterdayBaseDate = dateFormat.parse("June 30, 2010 12:00 am")
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		baseDateShifted = dateFormat.parse("June 30, 2010 11:00 pm")
		baseDate2 = dateFormat.parse("July 1, 2010 1:00 am") // switch time zone
		tomorrowBaseDate = dateFormat.parse("July 2, 2010 12:00 am")
		tomorrowCurrentTime = dateFormat.parse("July 2, 2010 2:15 pm")
		dayAfterTomorrowBaseDate = dateFormat.parse("July 3, 2010 12:00 am")
		dayAfterDayAfterTomorrowBaseDate = dateFormat.parse("July 4, 2010 12:00 am")
		lateBaseDate = dateFormat.parse("July 3, 2010 12:00 am")
		timeZone2BaseDate = dateFormat.parse("July 3, 2010 3:00 am")
		microlateCurrentTime = dateFormat.parse("July 3, 2010 1:58 pm")
		veryLateBaseDate = dateFormat.parse("July 20, 2010 12:00 am")
	}
	
	static within(long a, long b, long d) {
		long diff = a - b
		diff = diff < 0 ? -diff : diff
		return diff <= d
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testMultiCreateSuffix() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "run 5 miles 1000 feet elevation", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.units
		}
		
		assert x == ":run [distance: elevation]:run:feet elevation:run [distance]:run:miles"
	}
	
	@Test
	void testMultiCreateUnitDecoratorDuration() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "sleep 5 hours 30 minutes deep", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 5
			assert amounts[0].units == "hrs"
			assert amounts[1].amount.intValue() == 30
			assert amounts[1].units == "mins deep"
		}
		
		assert c == 1
	}
	
	@Test
	void testTwoWordSuffix() {
		EntryStats stats = new EntryStats()
		
		def parsed = entryParserService.parse(currentTime, timeZone, "run 5 miles elevation 2pm", null, null, baseDate, true)

		assert parsed.amounts[0].tag.description == 'run [distance: elevation]'
		
		parsed = entryParserService.parse(currentTime, timeZone, "activity 5 mins nonactive 2pm", null, null, baseDate, true)

		assert parsed.amounts[0].tag.description == 'activity [time: nonactive]'
	}
	
	@Test
	void testMultiCreateSuffixDifferent() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "run 5 pounds 1000 feet elevation", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.units
		}
		
		assert x == ":run [distance: elevation]:run:feet elevation:run [amount]:run:pounds"
	}
	
	@Test
	void testMultiCreatePrioritizeSort() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "run 1279 steps 2 miles", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 1279
			assert amounts[0].units == "steps"
			assert amounts[1].amount.intValue() == 2
			assert amounts[1].units == "mi"
		}
		
		assert c == 1
		
		def entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "run 3 miles 8900 steps", null, null, earlyBaseDate, true), new EntryStats())
		
		entries = Entry.fetchListData(user, timeZone, earlyBaseDate, currentTime)
		c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 8900
			assert amounts[0].units == "steps"
			assert amounts[1].amount.intValue() == 3
			assert amounts[1].units == "mi"
		}
		
		assert c == 1
		
	}
	
	@Test
	void testMultiUpdateAddElements() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 slices 500 calories", null, null, baseDate, true), new EntryStats())
		
		assert entry.update(entryParserService.parse(currentTime, timeZone, "bread 2 beans 255 calories 127 steps", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null

		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 2
			assert amounts[0].units == "beans"
			assert amounts[1].amount.intValue() == 127
			assert amounts[1].units == "steps"
			assert amounts[2].amount.intValue() == 255
			assert amounts[2].units == "calories"
		}
		
		assert c == 1
	}
	
	@Test
	// if units are the same with the same suffix, don't create duplicate entries but instead sum them together
	void testMultiCreateAddSimilarUnits() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "run 2 hours 30 minutes", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.amount
			x += ":" + e.units
		}
		
		assert x == ":run [time]:run:2.500000000:hours"
	}
	
	@Test
	// if units are the same with the same suffix, don't create duplicate entries but instead sum them together
	void testMultiCreateAddSimilarUnitsCase() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "run 2 Hours 30 MINS", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.amount
			x += ":" + e.units
		}
		
		assert x == ":run [time]:run:2.500000000:hours"
	}
	
	@Test
	void testBloodPressure() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "blood pressure 120/80", null, null, baseDate, true), new EntryStats())

		Iterable<Entry> group = entry.fetchGroupEntries()
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			entryDesc = entryDesc
			assert entryDesc.amounts.size() == 2
			++c
		}
		
		assert c == 1
	}

	@Test
	void testMultiCreateUnitDecorator() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "sleep 5 hours 2 hours deep 3 hours rem", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
		}
		
		assert x == ":sleep [time]:sleep:sleep [time: deep]:sleep:sleep [time: rem]:sleep"
	}
	
	@Test
	void testExpandPartialUnitsSingularSubUnit() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 hour 1 minute", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.toString() == "1"
			assert amounts[0].units == "hr"
			assert amounts[1].amount.intValue() == 1
			assert amounts[1].units == "min"
		}
		
		assert c == 1
	}
	
	@Test
	void testExpandPartialUnitsSingular() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1.5 hours", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.toString() == "1"
			assert amounts[0].units == "hr"
			assert amounts[1].amount.intValue() == 30
			assert amounts[1].units == "mins"
		}
		
		assert c == 1
	}
	
	@Test
	void testExpandPartialUnits() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2.5 hours", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.toString() == "2"
			assert amounts[0].units == "hrs"
			assert amounts[1].amount.intValue() == 30
			assert amounts[1].units == "mins"
		}
		
		assert c == 1
	}
	
	@Test
	void testExpandSubOneSubUnit() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread .5 hours", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.toString() == "30"
			assert amounts[0].units == "mins"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiCreate() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "bread 5 slices 500 calories", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
		}
		
		assert x == ":bread [slices]:bread:bread [calories]:bread"
	}
	
	@Test
	// if units are the same with the same suffix, don't create duplicate entries but instead sum them together
	void testMultiCreateMinutesSeconds() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "swam 40 minutes 30 seconds", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.amount
			x += ":" + e.units
		}
		
		assert x == ":swam [time]:swam:40.500000000:minutes"
	}
	
	@Test
	// if units are the same with the same suffix, don't create duplicate entries but instead sum them together
	void testMultiCreateAddSimilarUnitsListExpansion() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "run 2 hours 30 minutes", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 2
			assert amounts[0].units == "hrs"
			assert amounts[1].amount.intValue() == 30
			assert amounts[1].units == "mins"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiList() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 slices 500 calories", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 5
			assert amounts[0].units == "slices"
			assert amounts[1].amount.intValue() == 500
			assert amounts[1].units == "calories"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiUpdateSimilar() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 slices 500 calories", null, null, baseDate, true), new EntryStats())
		
		assert entry.update(entryParserService.parse(currentTime, timeZone, "bread 2 slices 255 calories", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null

		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 2
			assert amounts[0].units == "slices"
			assert amounts[1].amount.intValue() == 255
			assert amounts[1].units == "calories"
			assert amounts.size() == 2
		}
		
		assert c == 1
	}

	@Test
	void testMultiUpdateDissimilar() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 slices 500 calories", null, null, baseDate, true), new EntryStats())
		
		assert entry.update(entryParserService.parse(currentTime, timeZone, "bread 2 beans 255 calories", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null

		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 2
			assert amounts[0].units == "beans"
			assert amounts[1].amount.intValue() == 255
			assert amounts[1].units == "calories"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiUpdateDissimilarAddUnits() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 slices 500 calories", null, null, baseDate, true), new EntryStats())
		
		assert entry.update(entryParserService.parse(currentTime, timeZone, "bread 2 hours 30 minutes 255 calories", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null

		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.toString() == "2"
			assert amounts[0].units == "hrs"
			assert amounts[1].amount.intValue() == 30
			assert amounts[1].units == "mins"
			assert amounts[2].amount.intValue() == 255
			assert amounts[2].units == "calories"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiUpdatePreserveOrder() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "run 1 aye 2 b", null, null, baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 1
			assert amounts[0].units == "aye"
			assert amounts[1].amount.intValue() == 2
			assert amounts[1].units == "b"
		}
		
		assert c == 1
		
		assert entry.update(entryParserService.parse(currentTime, timeZone, "run 3 aye 2 b", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null

		entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 3
			assert amounts[0].units == "aye"
			assert amounts[1].amount.intValue() == 2
			assert amounts[1].units == "b"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiDelete() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 slices 500 calories", null, null, baseDate, true), new EntryStats())

		Entry.delete(entry, new EntryStats())
				
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
		}
		
		assert c == 0
	}
	
	@Test
	void testNoUnitsDoubleEntries() {
		def entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "bread 10 20", null, null, baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.amount + ":" + e.comment
		}
		
		assert x == ":bread:bread:10.000000000::bread:bread:20.000000000:"
	}
	/**/
}

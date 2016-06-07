package us.wearecurio.services.integration

import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.data.RepeatType
import us.wearecurio.model.DurationType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagValueStats;
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.Model.Visibility
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EntryParserService
import groovy.transform.TypeChecked

import org.joda.time.DateTimeZone
import org.junit.*
import org.joda.time.*

import grails.test.mixin.*
import us.wearecurio.utility.Utils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.Integration
import grails.test.mixin.integration.IntegrationTestMixin

@Integration
class EntryParserServiceTests extends CuriousTestCase {
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
		
		Entry.executeUpdate("delete Entry")
		Entry.executeUpdate("delete TagUnitStats")
		Entry.executeUpdate("delete TagValueStats")
		Entry.executeUpdate("delete TagStats")
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

	int testEntries(User user, String timeZoneName, Date baseDate, Date currentTime, Closure test) {
		def list = Entry.fetchListData(user, timeZoneName, baseDate, currentTime)
		
		int c = 0
		for (record in list) {
			test(record)
			++c
		}
		
		return c
	}
	
	@Test
	void testAmountBeforeTag() {
		// make sure pinned durations do not complete each other
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "2 bananas", null, null, baseDate, true), new EntryStats())

		assert entry.amount == 2.0d
		assert entry.units == ""
		assert entry.description == "bananas"
	}

	@Test
	void testTimeAppliedToCommaList() {
		// make sure pinned durations do not complete each other
		println("== Test creation of multiple entries")
		
		def (status, entries) = Entry.parseAndCreate(userId, new EntryStats(), currentTime, timeZone, "coffee, bananas 2pm, running 5pm", null, null, baseDate, true, 0)

		assert entries[0].date == entries[1].date
		assert entries[2].date != entries[0].date
	}

/*	@Test
	void testNumberField() {
		// make sure pinned durations do not complete each other
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep # hours", null, null, baseDate, true), new EntryStats())

		assert entry.amount == null
		assert entry.units == "hours"
	}

	@Test
	void testNullField() {
		// make sure pinned durations do not complete each other
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep _ hours", null, null, baseDate, true), new EntryStats())

		assert entry.amount == null
		assert entry.units == "hours"
	}

	@Test
	void testDurationPinned() {
		// make sure pinned durations do not complete each other
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start pinned", null, null, baseDate, true), new EntryStats())

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end pinned", null, null, baseDate, true), new EntryStats())
		assert !entry2.is(entry) // should not have completed entry
	}

	@Test
	void testStartExtendedDuration() {
		// test creation of duration with start entry converting to duration entry
		println("== Test creation of duration with start entry converting to duration entry ==")
		
		Entry endEntry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "fooey 4:30pm", null, null, baseDate, true), new EntryStats())
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start 2 hours 2:30pm", null, null, baseDate, true), new EntryStats())
		
		assert entry.durationType == DurationType.NONE
		assert entry.baseTag == Tag.look("testxyz")
		assert entry.tag == Tag.look("testxyz [time]")
		assert entry.date == endEntry.date
		
		testEntries(user, timeZone, baseDate, currentTime) {
			if (it.description == "testxyz") {
				def amount = it.amounts[0]
				assert amount.amount.longValue() == 2
				assert amount.units == 'hrs'
				
				assert it.amounts.size() == 1
			}
		}
		assert entry.date.getTime() - entry.startDate.getTime() == 7200000L
	}

	@Test
	void testSleepStart() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 3:30pm", null, null, baseDate, true), new EntryStats())
		assert entry.getDurationType().equals(DurationType.START)

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep start at 4pm", null, null, baseDate, true), new EntryStats())
		assert entry2.getDurationType().equals(DurationType.START)
		
		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 4 hours 3:30pm", null, null, baseDate, true), new EntryStats())
		assert entry3.getDurationType().equals(DurationType.NONE)

		Entry entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm start", null, null, baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getDurationType().equals(DurationType.START)

		entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm starts", null, null, baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getDurationType().equals(DurationType.START)

		entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 4pm starts(hola)", null, null, baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getComment().equals("(hola)")
		assert entry4.getDurationType().equals(DurationType.START)
	}

	@Test
	void testStartImpliedDuration() {
		// test creation of duration with start entry converting to duration entry
		println("== Test creation of duration with start entry converting to duration entry ==")
		
		Entry endEntry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "fooey 2:30pm", null, null, baseDate, true), new EntryStats())
		
		def parseResult = entryParserService.parse(currentTime, timeZone, "sleep 2 hours 2:30pm", null, null, baseDate, true)
		Entry entry = Entry.create(userId, parseResult, new EntryStats())
		
		assert entry.durationType == DurationType.NONE
		assert entry.baseTag == Tag.look("sleep")
		assert entry.tag == Tag.look("sleep [time]")
		assert entry.date == endEntry.date
		
		testEntries(user, timeZone, baseDate, currentTime) {
			if (it.description == "sleep") {
				def amount = it.amounts[0]
				assert amount.amount.longValue() == 2
				assert amount.units == 'hrs'
				
				assert it.amounts.size() == 1
			}
		}
		assert entry.date.getTime() - entry.startDate.getTime() == 7200000L
	}

	@Test
	void testReverseDuration() {
		// test creation of start of a duration pair
		println("== Test creation of end entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(lateCurrentTime, timeZone, "testxyz end 5pm", null, null, baseDate, true), new EntryStats())
		def x = entry.valueString()

		// test creation of the other side of the duration pair
		println("== Test creation of start entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(lateCurrentTime, timeZone, "testxyz start 3pm", null, null, baseDate, true), new EntryStats())
		assert entry2.is(entry) // should have completed entry
		testEntries(user, timeZone, baseDate, currentTime) {
			int c = 0
			def amount = it.amounts[0]
			assert amount.amount.longValue() == 2
			assert amount.units == 'hrs'
			
			assert it.amounts.size() == 1
		}
	}

	@Test
	void testParse() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 slice", null, null, baseDate, true), new EntryStats())
		def x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 SLICE", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// noon variations
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100 noon", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100 noon", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "noon aspirin 100", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin noon 100", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin noon", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin noon who 100", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin noon who, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// no value
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		
		// no value with time
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		
		// no value with space between time
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack 4 pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		
		// units not filled in automatically if only used once
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// units filled in automatically if used a few times
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 slice", null, null, baseDate, true), new EntryStats())
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 slice", null, null, baseDate, true), new EntryStats())
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 slice", null, null, baseDate, true), new EntryStats())
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet pinned", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:bookmark, repeatType:768, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 4pm repeat weekly", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test assume recent time
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet 3:00", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 4:00 pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// make sure at is parsed correctly
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 at 4p", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 at 4p", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 at 4p", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "at 4pm mood 7", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "at 4pm aspirin 1 tablet", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "at 4pm mood 7", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "at 4pm aspirin 1 tablet", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test a and p as time modifiers
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 4p", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test a and p as time modifiers
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 4a", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T11:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood seven 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "eat four slices 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:eat, amount:4.000000000, units:slices, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin one tablet 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 16h00", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 16:00", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "9am aspirin 1 tablet repeat weekly", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 9h00 after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 9:00 after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 09h00 after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 9pm after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "craving sweets/starches/carbohydrates 3 at 9pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:craving sweets/starches/carbohydrates, amount:3.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// decimal value
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "9am aspirin 1.2 mg repeat weekly", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.200000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "9am aspirin .7 mg repeat weekly", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:0.700000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		// test metadata entry parsing

		entry = Entry.create(userId, entryParserService.parseMeta("fast caffeine metabolizer"), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:null, datePrecisionSecs:0, timeZoneName:Etc/UTC, description:fast caffeine metabolizer, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test odd tweets

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "what's going on???", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:what's going on???, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "headache 8 at 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:headache, amount:8.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test current time for late base date

		entry = Entry.create(userId, entryParserService.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "just a tag", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:just a tag, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test time before amount

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "heart rate at 4pm 72bpm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "heart rate at 4pm = 72bpm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// number in tag

		entry = Entry.create(userId, entryParserService.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "methyl b-12", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// yes/no values

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 yes at 4pm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 no at 4pm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:0.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 4pm yes", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 yes we can at 4pm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12 yes we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 yes we can 4pm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12 yes we can, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// none value
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "tranch 4pm none", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "tranch _ 4pm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// tag with number in middle of tag name
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "book two of the series 8 at 4pm", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:book two of the series, amount:8.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test time parsing with unambiguous formats

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg 9pm after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg 9:00 after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "heart rate 4pm 72bpm", null, null, baseDate, false), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat weekly at 9", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 12am after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 12pm after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at noon ;after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:;after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at midnight ;after coffee", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:;after coffee, repeatType:null, repeatEnd:null)")

	}

	@Test
	void testDuration() {
		// test creation of start of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:30pm", null, null, baseDate, true), new EntryStats())
		def x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 3:31pm", null, null, baseDate, true), new EntryStats())
		assert entry2.is(entry) // should have completed entry
		testEntries(user, timeZone, baseDate, currentTime) {
			int c = 0
			def amount = it.amounts[0]
			assert amount.amount.longValue() == 1
			assert amount.units == 'min'
			
			assert it.amounts.size() == 1
		}
		assert entry.date.getTime() - entry.startDate.getTime() == 60000L
	}

	@Test
	void testUpdateRepeatVagueDate() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "bread 5 repeat", null, null, baseDate, true), new EntryStats())
		
 		String v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:bread, amount:5.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")

		def parse = entryParserService.parse(currentTime, timeZone, "bread 8 repeat ", null, null, baseDate, true, 1)
		Entry updated = entry.update(parse, new EntryStats(), baseDate, true)

 		v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:8.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")

		assert true
	}
	
	@Test
	void testSleepEnd() {
		Entry entry5 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 4pm stop", null, null, baseDate, true), new EntryStats())
		assert entry5.getDescription().equals("sleep end")
		assert entry5.getDurationType().equals(DurationType.END)

		Entry entry6 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm end", null, null, baseDate, true), new EntryStats())
		assert entry6.getDescription().equals("sleep end")
		assert entry6.getDurationType().equals(DurationType.END)
	}
	
	@Test
	void testSleepWake() {
		// test creation of start of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 3:30pm", null, null, baseDate, true), new EntryStats())
		def x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		assert entry.getDurationType().equals(DurationType.START)
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "wake 3:31pm", null, null, baseDate, true), new EntryStats())
		assert entry2.is(entry) // should have completed entry
		testEntries(user, timeZone, baseDate, currentTime) {
			int c = 0
			def amount = it.amounts[0]
			assert amount.amount.longValue() == 1
			assert amount.units == 'min'
			
			assert it.amounts.size() == 1
		}
	}

	@Test
	void testWideDuration() {
		// test creation of wide duration
		println("== Test creation of wide duration entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:30pm", null, null, earlyBaseDate, true), new EntryStats())

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 3:31pm", null, null, veryLateBaseDate, true), new EntryStats())
		assert entry2.is(entry) // should have completed entry
		testEntries(user, timeZone, veryLateBaseDate, currentTime) {
			int c = 0
			def amount = it.amounts[0]
			assert amount.amount.longValue() == 25
			assert amount.units == 'days'
			amount = it.amounts[1]
			assert amount.amount.longValue() == 1
			assert amount.units == 'min'

			assert it.amounts.size() == 2
		}
	}

	@Test
	void testLongTagTwoSpaces() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "dream pissed off mob they want to kill me women fake death put me in coffin w food bury me can play  yrs", null, null, baseDate, true), new EntryStats())
		
		String v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:dream pissed off mob they want to kill me women fake death put me in coffin w food bury me can play, amount:1.000000000, units:, amountPrecision:-1, comment:(yrs), repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testNoTag() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "1 slice bread", null, null, baseDate, true), new EntryStats())
		String v = entry.valueString()
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "1 slice", null, null, baseDate, true), new EntryStats())
		v = entry.valueString()
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:cannot understand what you typed, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testRepeatFirst() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "repeat cream pie 8pm", null, null, baseDate, true), new EntryStats())
		String v = entry.valueString()
		assert v.endsWith("date:2010-07-02T03:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:cream pie, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
	}
	
	@Test
	void testSleepQuality() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep quality 7", null, null, baseDate, true), new EntryStats())
		def x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep quality, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testRepeat() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
	}
	
	@Test
	void testRepeatChangeTimeZone() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		testEntries(user, timeZone, baseDate2, currentTime) {
			assert it.id == entry.getId()
			assert it.timeZoneName == "America/Los_Angeles"
		}
	}
	
	@Test
	void testRepeatParsing() {
		def res = entryParserService.parse(currentTime, timeZone, "bread repeat daily at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread repeat daily at 4pm foo", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('foo repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices repeat daily at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices daily at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices repeat at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices weekly at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == RepeatType.WEEKLYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices repeat weekly at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == RepeatType.WEEKLYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread weekly", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == RepeatType.WEEKLYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()
		
		res = entryParserService.parse(currentTime, timeZone, "bread remind at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread remind daily at 4pm foo", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('foo remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices remind daily at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices remind at 4pm", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread remind weekly", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == RepeatType.REMINDWEEKLYGHOST
		assert !res['amounts'][0].getUnits()
		
		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices remind weekly", null, null, baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == RepeatType.REMINDWEEKLYGHOST
		assert res['amounts'][0].getUnits().equals('slices')
		
	}

/*	@Test
	void testBigNumbers() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1000 slices", null, null, baseDate, true), new EntryStats())
		def x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1000.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1,000 slices", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1000.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1000000000000000000000000000000 slices", null, null, baseDate, true), new EntryStats())
		x = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1000.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}*/
	
}

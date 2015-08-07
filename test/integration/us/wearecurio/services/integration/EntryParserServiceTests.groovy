package us.wearecurio.services.integration

import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.RepeatType
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
	void testLongTagTwoSpaces() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "dream pissed off mob they want to kill me women fake death put me in coffin w food bury me can play  yrs", baseDate, true), new EntryStats())
		
		String v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:dream pissed off mob they want to kill me women fake death put me in coffin w food bury me can play, amount:1.000000000, units:, amountPrecision:-1, comment:(yrs), repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testUpdateRepeatVagueDate() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "bread 5 repeat", baseDate, true), new EntryStats())
		
 		String v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:bread, amount:5.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")

		def parse = entryParserService.parse(currentTime, timeZone, "bread 8 repeat ", baseDate, true, true)
		Entry updated = entry.update(parse, new EntryStats(), baseDate, true)

 		v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:8.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")

		assert true
	}
	
	@Test
	void testNoTag() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "1 slice bread", baseDate, true), new EntryStats())
		String v = entry.valueString()
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "1 slice", baseDate, true), new EntryStats())
		v = entry.valueString()
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:unknown, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testRepeatFirst() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "repeat cream pie 8pm", baseDate, true), new EntryStats())
		String v = entry.valueString()
		assert v.endsWith("date:2010-07-02T03:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:cream pie, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
	}
	
	@Test
	void testSleepQuality() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep quality 7", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep quality, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}
	
/*	@Test
	void testBigNumbers() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1000 slices", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1000.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1,000 slices", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1000.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1000000000000000000000000000000 slices", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1000.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}*/
	
	@Test
	void testParse() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 slice", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// noon variations
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100 noon", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "noon aspirin 100", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin noon 100", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin noon", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin noon who 100", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin noon who, amount:100.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// no value
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		
		// no value with time
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		
		// no value with space between time
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack 4 pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		
		// units filled in automatically
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet pinned", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:pinned, repeatType:768, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet @4pm repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test assume recent time
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet 3:00", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 4:00 pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// make sure at is parsed correctly
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 at 4p", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 @ 4p", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet @ 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 @4p", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet @4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "at 4pm mood 7", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "at 4pm aspirin 1 tablet", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "@4pm mood 7", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "@4pm aspirin 1 tablet", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test a and p as time modifiers
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 4p", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test a and p as time modifiers
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood 7 4a", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T11:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "mood seven 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin one tablet 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 16h00", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet at 16:00", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "9am aspirin 1 tablet repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg @9h00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 9:00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 09h00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at 9pm after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "craving sweets/starches/carbohydrates 3 at 9pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:craving sweets/starches/carbohydrates, amount:3.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// decimal value
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "9am aspirin 1.2 mg repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.200000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "9am aspirin .7 mg repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:0.700000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		// test metadata entry parsing

		entry = Entry.create(userId, entryParserService.parseMeta("fast caffeine metabolizer"), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:null, datePrecisionSecs:0, timeZoneName:Etc/UTC, description:fast caffeine metabolizer, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test odd tweets

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "what's going on???", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:what's going on???, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack#3", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack#3: 2 bags of chips", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:2.000000000, units:bags of, amountPrecision:3, comment:chips, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "snack#3", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "weight: 310#", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:weight, amount:310.000000000, units:#, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "weight: 310lbs", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:weight, amount:310.000000000, units:lbs, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "headache 8 at 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:headache, amount:8.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test current time for late base date

		entry = Entry.create(userId, entryParserService.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "just a tag", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:just a tag, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test time before amount

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "heart rate at 4pm 72bpm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "heart rate at 4pm = 72bpm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// number in tag

		entry = Entry.create(userId, entryParserService.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "methyl b-12", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// yes/no values

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 yes at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12: yes at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 no at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:0.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 4pm yes", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 4pm: yes", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 yes we can at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:we can, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "methyl b-12 yes we can 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:we can, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		// none value
		
		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "tranch 4pm: none", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "tranch - 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "tranch none we can at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:we can, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test time parsing with unambiguous formats

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg 9pm after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg 9:00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "heart rate 4pm 72bpm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat weekly at 9", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg @12am after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg @12pm after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg @noon after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "aspirin 100mg at midnight after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

	}

	@Test
	void testRepeat() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
	}
	
	@Test
	void testRepeatChangeTimeZone() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		testEntries(user, timeZone, baseDate2, currentTime) {
			assert it.id == entry.getId()
			assert it.timeZoneName == "America/Los_Angeles"
		}
	}
	
	@Test
	void testRepeatParsing() {
		def res = entryParserService.parse(currentTime, timeZone, "bread repeat daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread repeat daily at 4pm foo", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('foo repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices repeat daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices repeat at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices weekly at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == RepeatType.WEEKLYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices repeat weekly at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == RepeatType.WEEKLYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread weekly", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == RepeatType.WEEKLYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()
		
		res = entryParserService.parse(currentTime, timeZone, "bread remind at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread remind daily at 4pm foo", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('foo remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert !res['amounts'][0].getUnits()

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices remind daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices remind at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == RepeatType.REMINDDAILYGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = entryParserService.parse(currentTime, timeZone, "bread remind weekly", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == RepeatType.REMINDWEEKLYGHOST
		assert !res['amounts'][0].getUnits()
		
		res = entryParserService.parse(currentTime, timeZone, "bread 8 slices remind weekly", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == RepeatType.REMINDWEEKLYGHOST
		assert res['amounts'][0].getUnits().equals('slices')
		
	}

	@Test
	void testSleep() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 3:30pm", baseDate, true), new EntryStats())
		assert entry.getDurationType().equals(DurationType.START)

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep start at 4pm", baseDate, true), new EntryStats())
		assert entry2.getDurationType().equals(DurationType.START)
		
		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 4 hours at 3:30pm", baseDate, true), new EntryStats())
		assert entry3.getDurationType().equals(DurationType.NONE)

		Entry entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm start", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getDurationType().equals(DurationType.START)

		entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm starts", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getDurationType().equals(DurationType.START)

		entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm starts (hola)", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getComment().equals("(hola)")
		assert entry4.getDurationType().equals(DurationType.START)

		entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm starts(hola)", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getComment().equals("(hola)")
		assert entry4.getDurationType().equals(DurationType.START)

		Entry entry5 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm stop", baseDate, true), new EntryStats())
		assert entry5.getDescription().equals("sleep end")
		assert entry5.getDurationType().equals(DurationType.END)

		Entry entry6 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep at 4pm end", baseDate, true), new EntryStats())
		assert entry6.getDescription().equals("sleep end")
		assert entry6.getDurationType().equals(DurationType.END)
	}

	@Test
	void testDuration() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 3:31pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("== Test update start entry ==")

		entry.update(entryParserService.parse(currentTime, timeZone, "testxyz start at 3:01pm", baseDate, true, true), new EntryStats(), baseDate, true)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between start and end entry ==")

		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:16pm", baseDate, true), new EntryStats())
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:16:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry3.fetchEndEntry().equals(entry2)
		assert entry.fetchEndEntry() == null
		assert entry2.fetchStartEntry().equals(entry3)
		
		// test deletion of start end of a duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry3, new EntryStats())

		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new end after the duration pair
		println("== Test creation of new end after the duration pair ==")
		
		Entry entry4 = Entry.create(userId, entryParserService.parse(endTime, timeZone, "testxyz end at 4:31pm", baseDate, true), new EntryStats())
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry4.fetchStartEntry() == null
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test deletion of start of duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry2, new EntryStats(userId))
		
		assert entry.fetchEndEntry().equals(entry4)
		assert entry4.fetchStartEntry().equals(entry)
		
		assert entry4.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}

	@Test
	void testSleepWake() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "wake 3:31pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.getDurationType().equals(DurationType.END)

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("== Test update start entry ==")

		entry.update(entryParserService.parse(currentTime, timeZone, "sleep at 3:01pm", baseDate, true, true), new EntryStats(), baseDate, true)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between start and end entry ==")

		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep start at 3:16pm", baseDate, true), new EntryStats())
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:16:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry3.fetchEndEntry().equals(entry2)
		assert entry.fetchEndEntry() == null
		assert entry2.fetchStartEntry().equals(entry3)
		
		// test deletion of start end of a duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry3, new EntryStats())

		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new end after the duration pair
		println("== Test creation of new end after the duration pair ==")
		
		Entry entry4 = Entry.create(userId, entryParserService.parse(endTime, timeZone, "sleep end at 4:31pm", baseDate, true), new EntryStats())
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry4.fetchStartEntry() == null
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test deletion of start of duration pair
		println("== Test deletion of start entry from duration pair ==")
		
		Entry.delete(entry2, new EntryStats())
		
		assert entry.fetchEndEntry().equals(entry4)
		assert entry4.fetchStartEntry().equals(entry)
		
		assert entry4.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}

	//
	// Test creating an interleaved duration pair: create a duration pair, then create another start before the
	// pair and a new end in the middle of the first pair
	//
	@Test
	void testInterleavedDuration() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test creation of new start of a duration pair
		println("== Test creation of new start entry before start and end entry ==")

		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3pm", baseDate, true), new EntryStats())
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry3.fetchEndEntry() == null
		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2

		// test creation of new end in between first duration pair
		println("== Test creation of new end in between first duration pair ==")
		
		Entry entry4 = Entry.create(userId, entryParserService.parse(endTime, timeZone, "testxyz end at 3:45pm", baseDate, true), new EntryStats())
		assert entry4.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:45:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		
		assert entry4.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry4
		println entry4.fetchDurationEntry().valueString()
		
		assert entry2.fetchStartEntry() == null
		assert entry2.fetchDurationEntry() == null
	}

	@Test
	void testInterleavedDuration2() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between first duration pair ==")

		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:45pm", baseDate, true), new EntryStats())
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:45:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == null
		
		assert entry3.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry3
	}

	@Test
	void testOrphanDurationEnd() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test creation of orphan end of duration pair
		println("== Test creation of new end entry after first duration pair ==")

		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 4:30pm", baseDate, true), new EntryStats())
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry3.fetchDurationEntry() == null
		assert entry3.fetchStartEntry() == null
		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		
		// test deletion of start of duration pair
		
		println("== Test deletion of middle end entry ==")
		
		Entry.delete(entry2, new EntryStats())
		
		Entry newDurationEntry = entry.fetchEndEntry().fetchDurationEntry()
				
		assert newDurationEntry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry.fetchEndEntry() == entry3
		assert entry3.fetchStartEntry() == entry
	}
}
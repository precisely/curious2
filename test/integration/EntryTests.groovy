import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.Entry.DurationType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagValueStats;
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.services.DatabaseService
import groovy.transform.TypeChecked

import org.joda.time.DateTimeZone
import org.junit.*
import org.joda.time.*

import grails.test.mixin.*
import us.wearecurio.utility.Utils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
class EntryTests extends CuriousTestCase {
	static transactional = true

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

	int testPlot(User user, def tagIds, Date startDate, Date endDate, Date currentDate, String timeZoneName, Closure test) {
		def results = Entry.fetchPlotData(user, tagIds, startDate, endDate, currentDate, timeZoneName)
		
		int c = 0
		for (result in results) {
			test(result)
			++c
		}
		
		return c
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
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "dream pissed off mob they want to kill me women fake death put me in coffin w food bury me can play  yrs", baseDate, true), new EntryStats())
		
		def v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:dream pissed off mob they want to kill me women fake death put me in coffin w food bury me can play, amount:1.000000000, units:, amountPrecision:-1, comment:(yrs), repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testUpdateRepeatVagueDate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "bread 5 repeat", baseDate, true), new EntryStats())
		
 		def v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:bread, amount:5.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")

		def parse = Entry.parse(currentTime, timeZone, "bread 8 repeat ", baseDate, true, true)
		def updated = Entry.update(entry, parse, new EntryStats(), baseDate, true)

 		v = entry.valueString()
		
		assert v.endsWith("date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:8.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")

		assert true
	}
	
	@Test
	void testNoTag() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "1 slice bread", baseDate, true), new EntryStats())
		def v = entry.valueString()
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "1 slice", baseDate, true), new EntryStats())
		v = entry.valueString()
		assert v.endsWith("date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:unknown, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testRepeatFirst() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "repeat cream pie 8pm", baseDate, true), new EntryStats())
		def v = entry.valueString()
		assert v.endsWith("date:2010-07-02T03:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:cream pie, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
	}
	
	@Test
	void testSleepQuality() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep quality 7", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep quality, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testParse() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 slice", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// units filled in automatically
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet pinned", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:pinned, repeatType:768, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet @4pm repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test assume recent time
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet 3:00", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 4:00 pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test a and p as time modifiers
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "mood 7 4p", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test a and p as time modifiers
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "mood 7 4a", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T11:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "mood seven 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:mood, amount:7.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin one tablet 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 16h00", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet at 16:00", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin 1 tablet repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @9h00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 9:00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 09h00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at 9pm after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "craving sweets/starches/carbohydrates 3 at 9pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:craving sweets/starches/carbohydrates, amount:3.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// decimal value
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin 1.2 mg repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.200000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "9am aspirin .7 mg repeat weekly", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:0.700000000, units:mg, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		// test metadata entry parsing

		entry = Entry.create(userId, Entry.parseMeta("fast caffeine metabolizer"), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:null, datePrecisionSecs:0, timeZoneName:Etc/UTC, description:fast caffeine metabolizer, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test odd tweets

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "what's going on???", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:what's going on???, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "snack#3", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "snack#3: 2 bags of chips", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:2.000000000, units:bags of, amountPrecision:3, comment:chips, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "snack#3", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:snack#3, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "weight: 310#", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:weight, amount:310.000000000, units:#, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "weight: 310lbs", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:weight, amount:310.000000000, units:lbs, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "headache 8 at 4pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:headache, amount:8.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test current time for late base date

		entry = Entry.create(userId, Entry.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "just a tag", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:just a tag, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test time before amount

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate at 4pm 72bpm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate at 4pm = 72bpm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// number in tag

		entry = Entry.create(userId, Entry.parse(dateFormat.parse("July 1, 2010 6:00 pm"), timeZone, "methyl b-12", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// yes/no values

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12: yes at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 no at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:0.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm yes", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 4pm: yes", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:we can, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "methyl b-12 yes we can 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:methyl b-12, amount:1.000000000, units:we can, amountPrecision:0, comment:, repeatType:null, repeatEnd:null)")

		// none value
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "tranch 4pm: none", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "tranch - 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "tranch none we can at 4pm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:tranch, amount:null, units:we can, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test time parsing with unambiguous formats

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg 9pm after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T04:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg 9:00 after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "heart rate 4pm 72bpm", baseDate, false), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:heart rate, amount:72.000000000, units:bpm, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat weekly at 9", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T16:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat weekly, repeatType:1026, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @12am after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @12pm after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg @noon after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "aspirin 100mg at midnight after coffee", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T07:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:aspirin, amount:100.000000000, units:mg, amountPrecision:3, comment:after coffee, repeatType:null, repeatEnd:null)")

	}

	@Test
	void testMixedRepeatPlotData() {
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 3pm repeat", yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2 4pm repeat", baseDate, true), new EntryStats())
		
		int c = 0
		def expected = [ 1, 1, 2, 1, 2 ]
		
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			it = it
			assert it[1].intValue() == expected[c++]
			assert it[2] == "bread"
		} == expected.size()
	}
	
	@Test
	void testMixedStaticAndRepeatPlotData() {
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 4 1pm", yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 3pm repeat", yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 4pm repeat", baseDate, true), new EntryStats())
		
		int c = 0
		def expected = [ 4, 1, 1, 2, 1, 2 ]
		
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			it = it
			assert it[1].intValue() == expected[c++]
			assert it[2] == "bread"
		} == expected.size()
	}

	@Test
	void testMostUsedUnitNormalization() {
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 2000 meter 1pm", yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 2 km 3pm", yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "jog 2 kilometer 4pm", baseDate, true), new EntryStats())
		
		int c = 0
		
		testPlot(user, [Tag.look("jog distance").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
			++c
			assert it[1].intValue() == 2
		}
		
		assert c == 3
	}
	
	@Test
	void testRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
		EntryStats stats = new EntryStats(userId)
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime, timeZone, stats)
		def v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		def activated2 = entry.activateGhostEntry(baseDate, currentTime, timeZone2, stats)
		v = activated2.valueString()
		assert activated2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/New_York, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
	}
	
	@Test
	void testRepeatChangeTimeZone() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		testEntries(user, timeZone, baseDate2, currentTime) {
			assert it.id == entry.getId()
			assert it.timeZoneName == "America/Los_Angeles"
		}
	}
	
	@Test
	void testRemindActivate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread remind", earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")
		
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		} == 1
	
		EntryStats stats = new EntryStats(userId)
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime, timeZone, stats)
		def v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:5, repeatEnd:null)")

		assert testEntries(user, timeZone, veryLateBaseDate, lateCurrentTime) {
		} == 1

		def updated = Entry.update(activated, Entry.parse(lateCurrentTime, timeZone, "bread remind", baseDate, true, true), new EntryStats(), baseDate, true)
		
		assert updated == activated

		assert testEntries(user, timeZone, veryLateBaseDate, lateCurrentTime) {
		} == 1
	}
	
	@Test
	void testPlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	
		assert testPlot(user, [Tag.look("bread").getId()], null, null, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			assert date == "2010-07-01T22:30:00"
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 1
		
		assert testPlot(user, [Tag.look("bread").getId()], lateBaseDate, null, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		assert testPlot(user, [Tag.look("bread").getId()], earlyBaseDate, baseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		assert testPlot(user, [Tag.look("bread").getId()], earlyBaseDate, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 1
	}
	
	@Test
	void testSumPlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	
		Entry.create(userId, Entry.parse(slightDifferentCurrentTime, timeZone, "bread 3", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(lateCurrentTime, timeZone, "bread 2", tomorrowBaseDate, true), new EntryStats())
		
		def results = Entry.fetchSumPlotData(user, [Tag.look("bread").getId()], null, null, veryLateBaseDate, "America/Los_Angeles")
		
		def c = 0
		for (result in results) {
			def date = Utils.dateToGMTString(result[0])
			if (date == "2010-07-01T21:32:48") {
				assert result[1].intValue() == 4
				assert result[2] == "bread"
			} else if (date == "2010-07-02T21:32:48") {
				assert result[1].intValue() == 2
				assert result[2] == "bread"
			} else
				assert false
			++c
		}
		
		assert c == 2
		
		results = Entry.fetchSumPlotData(user, [Tag.look("bread").getId()], lateBaseDate, null, veryLateBaseDate, "America/Los_Angeles")
		
		c = 0
		for (result in results) {
			++c
		}
		
		assert c == 0
		
		results = Entry.fetchSumPlotData(user, [Tag.look("bread").getId()], earlyBaseDate, baseDate, veryLateBaseDate, "America/Los_Angeles")
		
		c = 0
		for (result in results) {
			++c
		}
		
		assert c == 0
		
		results = Entry.fetchSumPlotData(user, [Tag.look("bread").getId()], earlyBaseDate, lateBaseDate, veryLateBaseDate, "America/Los_Angeles")
		
		c = 0
		for (result in results) {
			++c
		}
		
		assert c == 2
	}
	
	@Test
	void testSumPlotDataNight() {
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 2am", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 3am", tomorrowBaseDate, true), new EntryStats())
		
		def results = Entry.fetchSumPlotData(user, [Tag.look("bread").getId()], null, null, veryLateBaseDate, "America/Los_Angeles")
		
		def c = 0
		for (result in results) {
			if (c == 0) {
				assert result[1].intValue() == 4
			} else {
				assert result[1].intValue() == 2
			}
			++c
		}
	}
	
	@Test
	void testSumPlotDataAfternoon() {
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 3pm", baseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 5am", tomorrowBaseDate, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 2pm", tomorrowBaseDate, true), new EntryStats())
		
		def results = Entry.fetchSumPlotData(user, [Tag.look("bread").getId()], null, null, veryLateBaseDate, "America/Los_Angeles")
		
		def c = 0
		for (result in results) {
			if (c == 0) {
				assert result[1].intValue() == 8
			} else {
				assert result[1].intValue() == 2
			}
			++c
		}
		
		assert c == 2
	}
	
	@Test
	void testAverageTimeMidnight() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 12am", dayOne, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 0
	}
	
	@Test
	void testAverageTimeOneAM() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 1am", dayOne, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 3600
	}
	
	@Test
	void testAverageTimeNoon() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 12pm", dayOne, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 43200
	}
	
	@Test
	void testAverageTimeNoonNoStartDateNoEndDate() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 12pm", dayOne, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, null, null, currentDate, dateTimeZone)
		
		assert breadResult == 43200
	}
	
	@Test
	void testAverageTimeOnePM() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 1pm", dayOne, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert Math.abs(breadResult - 46800) < 1000
	}
	
	@Test
	void testAverageTimeAllPoints() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 6pm", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 6am", dayOne, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert breadResult == 43200 // when times zero out, it should use noon
	}
	
	@Test
	void testAverageTimeMorningTwoAfternoons() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 7am", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 3pm", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 5pm", dayOne, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert Math.abs(breadResult - 50400) < 1000
	}
	
	@Test
	void testAverageTime() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 3am", dayOne, true), new EntryStats())
		Entry repeatBreadEntry = Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 2am repeat", dayTwo, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 6 9am", dayThree, true), new EntryStats())
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 1 1pm", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 3 5pm", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 2 7pm repeat", dayTwo, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 6 4pm", dayThree, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		def circusIds = [ Tag.look("circus").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		def circusResult = Entry.fetchAverageTime(user, circusIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert Math.abs(breadResult - 9519) < 1000
		assert Math.abs(circusResult - 63215) < 1000
	}
	
	@Test
	void testAverageTimeNoRepeats() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 3am", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 2am", dayTwo, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 6 9am", dayThree, true), new EntryStats())
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 1 1pm", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 3 5pm", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 2 7pm", dayTwo, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 6 4pm", dayThree, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		def circusIds = [ Tag.look("circus").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		def circusResult = Entry.fetchAverageTime(user, circusIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert Math.abs(breadResult - 11968) < 1000
		assert Math.abs(circusResult - 58651) < 1000
	}
	
	@Test
	void testAverageTimeOnlyRepeats() {
		def dayOne = dateFormat.parse("July 1, 2010 12:00 am")
		def dayTwo = dateFormat.parse("July 2, 2010 12:00 am")
		def dayThree = dateFormat.parse("July 4, 2010 12:00 am")
		def startDate = dayOne
		def endDate = dateFormat.parse("July 5, 2010 12:00 am")
		def currentDate = dateFormat.parse("July 7, 2010 12:00 am")
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 1 1am repeat", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 3 3am repeat", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 2 2am repeat", dayTwo, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "bread 6 9am repeat", dayThree, true), new EntryStats())
		
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 1 1pm repeat", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 3 5pm repeat", dayOne, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 2 7pm repeat", dayTwo, true), new EntryStats())
		Entry.create(userId, Entry.parse(veryLateBaseDate, timeZone, "circus 6 4pm repeat", dayThree, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		def circusIds = [ Tag.look("circus").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		def circusResult = Entry.fetchAverageTime(user, circusIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert Math.abs(breadResult - 9207) < 1000
		assert Math.abs(circusResult - 58211) < 1000
	}
	
	@Test
	void testRepeatPlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 3pm repeat", earlyBaseDate, true), new EntryStats())
	
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 8
	}
	
	@Test
	void testRemindActivatePlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1 3pm remind", earlyBaseDate, true), new EntryStats())
	
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		entry.activateGhostEntry(earlyBaseDate, currentTime, "America/Los_Angeles", new EntryStats())

		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
			def date = Utils.dateToGMTString(it[0])
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 1
	}
	
	@Test
	void testRepeatNullValuePlotData() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread _ 3pm remind", earlyBaseDate, true), new EntryStats())
	
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0
		
		def activated = entry.activateGhostEntry(earlyBaseDate, currentTime, "America/Los_Angeles", new EntryStats())

		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 0

		def updated = Entry.update(activated, Entry.parse(currentTime, timeZone, "bread 6 3pm remind", earlyBaseDate, true, true), new EntryStats(), earlyBaseDate, true)
		
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate, "America/Los_Angeles") {
		} == 1
	}
		
	void testUpdateNonRepeatToRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm ", baseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 pinned", baseDate, true, true), new EntryStats(), baseDate, true)
		
		assert updated == entry
		
		entry.getRepeatType().equals(Entry.RepeatType.CONTINUOUSGHOST)
	}
	
	@Test
	void testUpdateRepeatAllFuture() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", baseDate, true, true), new EntryStats(), baseDate, true)
		
		assert updated != entry

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id != entry.getId()
			assert it.amount.intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
	}
	
	@Test
	void testUpdateRepeatJustOnce() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", baseDate, true, true), new EntryStats(), baseDate, false)
		
		assert updated != entry && updated != null

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatVagueChangeTimeZone() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "bread 5 repeat", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime2, timeZone, "bread 8 repeat", baseDateShifted, true, true), new EntryStats(), baseDateShifted, false)
		
		def v = entry.valueString()
	
		assert entry.toString().contains("amount:5.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:2010-06-29T19:00:00")
		
		assert updated != entry && updated != null

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatJustTodayTwice() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat", baseDate, true, true), new EntryStats(), baseDate, false)
		
		assert updated != entry && updated != null

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1

		def updated2 = Entry.update(updated, Entry.parse(currentTime, timeZone, "bread 7 at 2pm repeat", baseDate, true, true), new EntryStats(), baseDate, false)
		
		assert updated2 == updated

		// verify event edited on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == updated.getId()
			assert it['amount'].intValue() == 7
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify event with old amount after baseDate still there, by itself
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatJustTodayDeleteUpdate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat", baseDate, true, true), new EntryStats(), baseDate, false)
		
		assert updated != entry && updated != null

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	
		def newEntryId

		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			newEntryId = it['id']
			assert it['amount'].intValue() == 5
		} == 1
	
		def futureEntry = Entry.get(newEntryId)

		Entry.deleteGhost(futureEntry, new EntryStats(user.getId()), tomorrowBaseDate, true)
		
		assert futureEntry.isDeleted()
		
		assert updated.getRepeatEnd() == updated.getDate()
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		assert testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
		} == 0
	
		def updated2 = Entry.update(updated, Entry.parse(currentTime, timeZone, "bread 7 at 2pm repeat", baseDate, true, true), new EntryStats(), baseDate, false)
		
		assert updated2 == updated

		// verify event edited on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == updated.getId()
			assert it['amount'].intValue() == 7
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify tomorrow has no entries
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
		} == 0
	}
	
	@Test
	void testDeleteRepeatThenUpdate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat", earlyBaseDate, true), new EntryStats())
		
		Entry.deleteGhost(entry, new EntryStats(user.getId()), lateBaseDate, true)
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify old event still at tomorrowBaseDate
		assert testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify old event not at lateBaseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
		} == 0

		// update event at baseDate		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 7 2pm repeat", baseDate, true, true), new EntryStats(), baseDate, false)
		
		assert updated != entry
		
		// verify event edited on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == updated.getId()
			assert it['amount'].intValue() == 7
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify old event still at tomorrowBaseDate with new id
		assert testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify old event still not at lateBaseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
		} == 0
	}
	
	@Test
	void testUpdateRepeatNotGhostJustOnce() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		
		entry.getRepeatType().unGhost()
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", baseDate, true, true), new EntryStats(), baseDate, false)
		
		assert updated != entry && updated != null
		
		Utils.save(entry, true)

		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatNoChange() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 5 at 2pm repeat daily", baseDate, true, true), new EntryStats(), baseDate, true)

		assert updated == entry && updated != null
		
		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
		
		// verify new event with old amount after baseDate
		assert testEntries(user, timeZone, lateBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatOnFirstDayJustOnce() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", earlyBaseDate, true, true), new EntryStats(), earlyBaseDate, false)

		assert updated == entry && updated != null
		
		// verify old event updated on earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify new event created after earlyBaseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
		} == 1
	}
	
	@Test
	void testUpdateRepeatOnFirstDayAllFuture() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		
		def updated = Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", earlyBaseDate, true, true), new EntryStats(), earlyBaseDate, true)

		assert updated == entry && updated != null
		
		// verify old event updated on earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
		
		// verify old event still at baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 8
		} == 1
	}
	
	@Test
	void testRepeatAgain() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), new EntryStats())
		
		def entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5pm repeat daily", baseDate, true), new EntryStats())
		
		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", tomorrowBaseDate, true), new EntryStats())
		
		assert entry.getRepeatEnd().equals(entry2.fetchPreviousDate())
		
		Entry.delete(entry2, new EntryStats())
		
		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", dayAfterTomorrowBaseDate, true), new EntryStats())

		assert entry.getRepeatEnd().equals(entry2.fetchPreviousDate())
	}
	
	@Test
	void testLateTimestamps() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 11pm remind", baseDate, true), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T06:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 11pm", baseDate, true), new EntryStats())
		v = entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T06:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testRepeatStart() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start repeat", earlyBaseDate, true), new EntryStats())
		assert entry.getDurationType().equals(Entry.DurationType.START)
	}
	
	@Test
	void testRepeatCreateInMiddle() {
		// test creating repeats in middle of earlier range
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
		}
		
		def activated = entry.activateGhostEntry(lateBaseDate, lateCurrentTime, timeZone, new EntryStats(userId))
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		// create repeat event in the middle
		def middleEntry = Entry.create(userId, Entry.parse(lateCurrentTime, timeZone, "bread repeat daily", baseDate, true), new EntryStats())
		
		// make sure prior repeatEnd is reset
		def ere = entry.getRepeatEnd()
		assert entry.getRepeatEnd().equals(middleEntry.fetchPreviousDate())
		
		// delete middle event
		Entry.delete(middleEntry, new EntryStats())		
	}
	
	@Test
	void testDeleteContinuousRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread pinned", baseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:pinned, repeatType:768, repeatEnd:null)")
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, new EntryStats(userId), tomorrowBaseDate, true)
		
		assert entry.isDeleted()
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.identity != entry.getId()
		}
	}
	
	@Test
	void testDeleteTimedRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert (it.id == entry.getId())
		}
		
		Entry.deleteGhost(entry, new EntryStats(userId), tomorrowBaseDate, false)
		
		assert entry.getRepeatEnd().equals(entry.getDate())
		
		testEntries(user, timeZone, dayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			def e2 = Entry.get(it.id)
			assert e2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
			assert e2.getId() != entry.getId()
		}
	}
	
	@Test
	void testDeleteTimedRepeatEndsToday() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, new EntryStats(userId), tomorrowBaseDate, true)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		Entry.deleteGhost(entry, new EntryStats(userId), baseDate, false)
		
		assert entry.getUserId() == 0L
	}
	
	@Test
	void testDeleteTimedRepeatOnceAtBeginning() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), new EntryStats())
		
		Date origDate = entry.getDate()
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, new EntryStats(userId), baseDate, false)
		
		assert entry.getDate().equals(origDate + 1)
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert false // should have no entries any more at base date
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}		
	}
	
	@Test
	void testDeleteTimedRepeatTomorrow() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), new EntryStats())
		
		Date origDate = entry.getDate()
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}

		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
		
		Entry.deleteGhost(entry, new EntryStats(userId), tomorrowBaseDate, false)
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert false // should have no entries tomorrow
		}
		
		boolean found = false
		testEntries(user, timeZone, dayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id != entry.getId()
			found = true
		}
		
		assert found
	}
	
	@Test
	void testDeleteTimedRepeatDayAfterTomorrow() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), new EntryStats())
		
		Entry.deleteGhost(entry, new EntryStats(userId), dayAfterTomorrowBaseDate, false)
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		}
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.id == entry.getId()
		}
		
		testEntries(user, timeZone, dayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			assert false
		}
		
		boolean found = false
		testEntries(user, timeZone, dayAfterDayAfterTomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id != entry.getId()
			found = true
		}
		
		assert found
	}
	
	@Test
	void testDeleteTimedRepeatAll() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", baseDate, true), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		} == 1

		assert testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		} == 1
		
		Entry.deleteGhost(entry, new EntryStats(userId), tomorrowBaseDate, true)
		
		assert entry.getRepeatEnd().equals(tomorrowBaseDate)
		
		testEntries(user, timeZone, tomorrowBaseDate, this.tomorrowCurrentTime) {
			assert it.id == entry.getId()
		}
	}
	
	@Test
	void testRepeatNonContinuous() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm repeat daily", earlyBaseDate, true), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		EntryStats stats = new EntryStats(userId)
		
		def activated = entry.activateGhostEntry(baseDate, lateCurrentTime, timeZone, stats)
		v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		def activated2 = entry.activateGhostEntry(baseDate, currentTime, timeZone, stats)
		v = activated2.valueString()
		assert activated2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")

		def activated3 = entry.activateGhostEntry(earlyBaseDate, currentTime, timeZone, stats)
		v = activated3.valueString()
		assert activated3.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:2010-06-30T21:00:00)")
		assert activated3 == entry
	}
	
	@Test
	void testOldRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread repeat daily", earlyBaseDate, true), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		def entries = Entry.fetchListDataNoRepeats(user, baseDate)
		assert entries.size() == 0
		
		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 3pm pinned", baseDate, true), new EntryStats())
		entries = Entry.fetchListDataNoRepeats(user, baseDate)
		assert entries.size() == 0

		entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 3pm", baseDate, true), new EntryStats())
		entries = Entry.fetchListDataNoRepeats(user, baseDate)
		assert entries.size() == 1
	}
	
	@Test
	void testRemind() {
		EntryStats stats = new EntryStats()
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm remind", baseDate, true), stats)
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")

		// tag stats for reminders should be "none" value for the amount
		def tagStats = stats.finish()[0]
		assert tagStats.getLastAmount() == null
		assert tagStats.getLastAmountPrecision() < 0
		assert tagStats.getTypicallyNoAmount()
		
		// query for reminder should generate the ghost entry only
		testEntries(user, timeZone, lateBaseDate, lateCurrentTime) {
			assert it['id'] == entry.getId()
			assert Utils.dateToGMTString(it['date']).equals("2010-07-03T21:00:00")
		}
		
		// repeat should be here
		def entryIds = Entry.fetchReminders(user, microlateCurrentTime, 5 * 60)
		for (entryId in entryIds) {
			assert entryId['id'] == entry.getId()
		}
		
		// should be no repeats here
		entryIds = Entry.fetchReminders(user, microlateCurrentTime, 100L)
		for (entryId in entryIds) {
			assert false
		}
	}

	@Test
	void testRemindList() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 75 2pm remind", baseDate, true), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:75.000000000, units:, amountPrecision:3, comment:remind, repeatType:517, repeatEnd:null)")
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.amount.intValue() == 75
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.amount.intValue() == 75
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}
	}

	@Test
	void testRemindList2() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 2pm remind", baseDate, true), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.amount.intValue() == 1
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.amount.intValue() == 1
			assert (it.repeatType & Entry.RepeatType.GHOST_BIT) != 0
		}
	}

	@Test
	void testRepeatParsing() {
		def res = Entry.parse(currentTime, timeZone, "bread repeat daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()

		res = Entry.parse(currentTime, timeZone, "bread repeat daily at 4pm foo", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('foo repeat')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat')
		assert res['repeatType'] == Entry.RepeatType.DAILYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices weekly at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices repeat weekly at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLYCONCRETEGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread weekly", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('repeat weekly')
		assert res['repeatType'] == Entry.RepeatType.WEEKLYCONCRETEGHOST
		assert !res['amounts'][0].getUnits()
		
		res = Entry.parse(currentTime, timeZone, "bread remind at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('remind')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert !res['amounts'][0].getUnits()

		res = Entry.parse(currentTime, timeZone, "bread remind daily at 4pm foo", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('foo remind')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert !res['amounts'][0].getUnits()

		res = Entry.parse(currentTime, timeZone, "bread 8 slices remind daily at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread 8 slices remind at 4pm", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind')
		assert res['repeatType'] == Entry.RepeatType.REMINDDAILYGHOST
		assert res['amounts'][0].getUnits().equals('slices')

		res = Entry.parse(currentTime, timeZone, "bread remind weekly", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 1
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == Entry.RepeatType.REMINDWEEKLYGHOST
		assert !res['amounts'][0].getUnits()
		
		res = Entry.parse(currentTime, timeZone, "bread 8 slices remind weekly", baseDate, true)

		assert res['baseTag'].getDescription().equals('bread')
		assert res['amounts'][0].getAmount().intValue() == 8
		assert res['comment'].equals('remind weekly')
		assert res['repeatType'] == Entry.RepeatType.REMINDWEEKLYGHOST
		assert res['amounts'][0].getUnits().equals('slices')
		
	}

	@Test
	void testUpdate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 1 slice", baseDate, true, true), new EntryStats(), baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", baseDate, true, true), new EntryStats(), baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")
	}
	
	@Test
	void testUpdatePartialOrCreate() {
		def m = Entry.parse(currentTime, timeZone2, "bread summary 1", baseDate, true)
		m.put('comment','(Withings)')
		m.put('setName','withings import')
		def timezone = (Integer) TimeZoneId.look(timeZone2).getId()
		m.put('timeZoneId', timezone)
		def entry = Entry.updatePartialOrCreate(userId, m, null, new EntryStats())
		println entry.valueString()
		assert Entry.count() == 1
		entry = Entry.updatePartialOrCreate(userId, m, null, new EntryStats())
		println Entry.list()
		assert Entry.count() == 1
	}

	@Test
	void testUpdateDate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 1", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "bread 1 slice", tomorrowBaseDate, true, true), new EntryStats(), baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}

	@Test
	void testUpdateRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo repeat daily", baseDate, true, false), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		println("Attempting to update")
		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "bread repeat daily", baseDate, true, true), new EntryStats(), baseDate, true) != null
		
		testEntries(user, timeZone2, timeZone2BaseDate, lateCurrentTime) {
			assert it['id'] == entry.getId()
			def ds = Utils.dateToGMTString(it['date'])
			assert Utils.dateToGMTString(it['date']).equals("2010-07-03T19:00:00")
		}
	}

	@Test
	void testUpdateRepeatChangeTime() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo repeat daily", earlyBaseDate, true, false), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		println("Attempting to update")
		
		assert Entry.update(entry, Entry.parse(currentTime, timeZone, "headache 8 at 3pm repeat daily", earlyBaseDate, true, true), new EntryStats(), earlyBaseDate, true) != null
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			def ds = Utils.dateToGMTString(it['date'])
			assert Utils.dateToGMTString(it['date']).equals("2010-07-01T22:00:00")
		}
	}

	@Test
	void testUpdateRepeatChangeTimeZone() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread foo 2pm", earlyBaseDate, true, false), new EntryStats())
		def v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		println("Attempting to update")
		
		Entry.update(entry, Entry.parse(currentTime, timeZone2, "bread foo 3pm", earlyBaseDate2, true, true), new EntryStats(), baseDate, true)
		v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
	}

	@Test
	void testReplaceRepeat() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "exercise 12pm repeat daily", earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:exercise, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		println("Attempting to update")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "exercise 8 12pm repeat daily", baseDate, true), new EntryStats())
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:exercise, amount:8.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		def repeatEnd = entry.getRepeatEnd()
		assert repeatEnd.equals(entry2.fetchPreviousDate())
	}
	
	@Test
	void testSleep() {
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 3:30pm", baseDate, true), new EntryStats())
		assert entry.getDurationType().equals(Entry.DurationType.START)

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start at 4pm", baseDate, true), new EntryStats())
		assert entry2.getDurationType().equals(Entry.DurationType.START)
		
		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep 4 hours at 3:30pm", baseDate, true), new EntryStats())
		assert entry3.getDurationType().equals(Entry.DurationType.NONE)

		Entry entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm start", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm starts", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm starts (hola)", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getComment().equals("(hola)")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm starts(hola)", baseDate, true), new EntryStats())
		assert entry4.getDescription().equals("sleep start")
		assert entry4.getComment().equals("(hola)")
		assert entry4.getDurationType().equals(Entry.DurationType.START)

		Entry entry5 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm stop", baseDate, true), new EntryStats())
		assert entry5.getDescription().equals("sleep end")
		assert entry5.getDurationType().equals(Entry.DurationType.END)

		Entry entry6 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep at 4pm end", baseDate, true), new EntryStats())
		assert entry6.getDescription().equals("sleep end")
		assert entry6.getDurationType().equals(Entry.DurationType.END)
	}

	@Test
	void testDuration() {
		// test creation of one side of a duration pair
		println("== Test creation of start entry ==")
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 3:31pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("== Test update start entry ==")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "testxyz start at 3:01pm", baseDate, true, true), new EntryStats(), baseDate, true)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between start and end entry ==")

		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:16pm", baseDate, true), new EntryStats())
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
		
		Entry entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "testxyz end at 4:31pm", baseDate, true), new EntryStats())
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
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		Entry entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "wake 3:31pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.getDurationType().equals(DurationType.END)

		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:0.016667000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)
		assert entry2.fetchDurationEntry().fetchIsGenerated()
		
		// test editing the start time of the duration pair
		println("== Test update start entry ==")

		Entry.update(entry, Entry.parse(currentTime, timeZone, "sleep at 3:01pm", baseDate, true, true), new EntryStats(), baseDate, true)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:01:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry().fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:31:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:sleep, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
		assert entry2.fetchStartEntry().equals(entry)
		assert entry.fetchEndEntry().equals(entry2)

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between start and end entry ==")

		Entry entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "sleep start at 3:16pm", baseDate, true), new EntryStats())
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
		
		Entry entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "sleep end at 4:31pm", baseDate, true), new EntryStats())
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
		
		Entry entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test creation of new start of a duration pair
		println("== Test creation of new start entry before start and end entry ==")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3pm", baseDate, true), new EntryStats())
		println entry3.valueString()
		assert entry3.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry3.fetchEndEntry() == null
		assert entry2.fetchStartEntry() == entry
		assert entry.fetchEndEntry() == entry2

		// test creation of new end in between first duration pair
		println("== Test creation of new end in between first duration pair ==")
		
		def entry4 = Entry.create(userId, Entry.parse(endTime, timeZone, "testxyz end at 3:45pm", baseDate, true), new EntryStats())
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test creation of new start of a duration pair
		println("== Test creation of new start entry in between first duration pair ==")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:45pm", baseDate, true), new EntryStats())
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
		
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz start at 3:30pm", baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == null
		
		// test creation of the other side of the duration pair
		println("== Test creation of end entry ==")

		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4pm", baseDate, true), new EntryStats())
		println entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")

		assert entry.fetchEndEntry() == entry2
		assert entry2.fetchStartEntry() == entry
		assert entry2.fetchDurationEntry().valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T23:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:testxyz, amount:0.500000000, units:hours, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		// test creation of orphan end of duration pair
		println("== Test creation of new end entry after first duration pair ==")

		def entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, "testxyz end at 4:30pm", baseDate, true), new EntryStats())
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
	
	static final MathContext mc = new MathContext(9)
	
	@Test
	void testTagStats() {
		def now = new Date()
		now = new org.joda.time.DateTime(now).withField(DateTimeFieldType.secondOfMinute(), 0).toDate()
		def yesterday = new Date(now.getTime() - 1000L * 60 * 60 * 24)
		def twoMonthsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 60)
		def nineMonthsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 9 * 30)
		def twoYearsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 24 * 30)
		
		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, Entry.parse(now, timeZone, "abcdef 3 tablet", now, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "abcdef 1 toblet", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "abcdef 1 tablet", twoMonthsAgo, true), stats)

		entry = Entry.create(userId, Entry.parse(nineMonthsAgo, timeZone, "abcdef 1 tablet", nineMonthsAgo, true), stats)

		entry = Entry.create(userId, Entry.parse(twoYearsAgo, timeZone, "abcdef 1 tablet", twoYearsAgo, true), stats)

		entry = Entry.create(userId, Entry.parse(now, timeZone, "qwerty 1 mg", now, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "qwerty 1.5 mg", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "qwerty 1 mg", yesterday, true), stats)
		
		entry = Entry.create(userId, Entry.parse(now, timeZone, "sando 1 mg", now, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "sando 1.5 mg", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "sando 3 mg", yesterday, true), stats)
		
		entry = Entry.create(userId, Entry.parse(now, timeZone, "whaat 24", now, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "whaat 14", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "whaat 11", yesterday, true), stats)
		
		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "gorby", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "gorby", yesterday, true), stats)
		
		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "gorby 12", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "whoa 1 gee repeat daily", yesterday, true), stats)
		
		stats.finish() // generate new tag stats

		def tag = Tag.look("abcdef")
		
		TagStats result = TagStats.createOrUpdate(userId, tag.getId())
		
		println "Usage of abcdef: " + result.getMostRecentUsage()

		assert within((result.getMostRecentUsage().getTime() / 1000L).longValue(), (now.getTime() / 1000L).longValue(), 2)
		
		assert within((result.getThirdMostRecentUsage().getTime() / 1000L).longValue(), (twoMonthsAgo.getTime() / 1000L).longValue(), 2)
		
		assert result.getCountLastThreeMonths() == 3
		
		assert result.getCountLastYear() == 4
		
		assert result.getCountAllTime() == 5
		
		assert result.getLastAmount().intValue() == 3
		
		assert result.getLastAmountPrecision() == 3
		
		assert result.getLastUnits().equals("tablet")
		
		def tabletTag = Tag.look("abcdef tablet")
		
		TagValueStats resultV = TagValueStats.createOrUpdate(userId, tabletTag.getId(), null)
		
		assert resultV.getMinimum().intValue() == 1
		
		assert resultV.getMaximum().intValue() == 3
		
		def tag2 = Tag.look("qwerty")
		
		TagStats result2 = TagStats.createOrUpdate(userId, tag2.getId())
		
		println "Usage of qwerty: " + result2.getMostRecentUsage()

		assert result2.getLastAmount().intValue() == 1
		
		assert result2.getLastAmountPrecision() > 0
		
		assert result2.getLastUnits().equals("mg")
		
		def tag3 = Tag.look("sando")
		
		TagStats result3 = TagStats.createOrUpdate(userId, tag3.getId())
		
		println "Usage of sando: " + result2.getMostRecentUsage()

		assert result3.getLastAmount() == null
		
		assert result3.getLastAmountPrecision() < 0
		
		assert result3.getLastUnits().equals("mg")
		
		def tag4 = Tag.look("whaat")
		
		TagStats result4 = TagStats.createOrUpdate(userId, tag4.getId())
		
		println "Usage of whaat: " + result2.getMostRecentUsage()

		assert result4.getLastAmount() == null
		
		assert result4.getLastAmountPrecision() < 0
		
		assert result4.getLastUnits().equals("")
		
		assert !result4.getTypicallyNoAmount()
		
		tag4 = Tag.look("gorby")
		
		result4 = TagStats.createOrUpdate(userId, tag4.getId())
		
		println "Usage of gorby: " + result2.getMostRecentUsage()

		assert result4.getLastAmount().intValue() == 1
		
		assert result4.getLastAmountPrecision() < 0
		
		assert result4.getLastUnits().equals("")
		
		assert result4.getTypicallyNoAmount()
		
		stats = new EntryStats(userId)
		
		Entry.delete(entry, stats)
		
		result = stats.finish()[0]
		
		assert result.getMostRecentUsage() == null
		
		assert result.getThirdMostRecentUsage() == null
		
		assert result.getCountLastThreeMonths() == 0
		
		assert result.getCountLastYear() == 0
		
		assert result.getCountAllTime() == 0
		
		assert result.getLastAmount() == null
		
		assert result.getLastAmountPrecision() == -1
		
		assert result.getLastUnits().equals("")
	}

	@Test
	void testTagValueStats() {
		def now = new Date()
		def yesterday = new Date(now.getTime() - 1000L * 60 * 60 * 24)
		def twoMonthsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 60)
		def nineMonthsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 9 * 30)
		def twoYearsAgo = new Date(now.getTime() - 1000L * 60 * 60 * 24 * 24 * 30)
		
		EntryStats stats = new EntryStats(userId)
		
		def entry = Entry.create(userId, Entry.parse(now, timeZone, "yoohoo 3 km", now, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "yoohoo 1 km", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "yoohoo 1000 meters", twoMonthsAgo, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "yoohoo 4000 meters", twoMonthsAgo, true), stats)

		entry = Entry.create(userId, Entry.parse(now, timeZone, "woohoo 3 km", now, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "woohoo 1 km", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "woohoo 10000 eggs", twoMonthsAgo, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "woohoo 1000 meters", twoMonthsAgo, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "woohoo 4000 meters", twoMonthsAgo, true), stats)

		stats.finish() // generate new tag stats

		def tag = Tag.look("yoohoo distance")
		
		TagValueStats resultV = TagValueStats.createOrUpdate(userId, tag.getId(), null)
		
		assert resultV.getMinimum().intValue() == 1
		
		assert resultV.getMaximum().intValue() == 4
		
		def tag2 = Tag.look("woohoo distance")
		
		TagValueStats resultV2 = TagValueStats.createOrUpdate(userId, tag.getId(), null)
		
		assert resultV2.getMinimum().intValue() == 1
		
		assert resultV2.getMaximum().intValue() == 4
	}
}

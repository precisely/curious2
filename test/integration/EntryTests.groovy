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
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
class EntryTests extends CuriousTestCase {
	static transactional = true
	
	EntryParserService entryParserService

	DateFormat dateFormat
	Date veryEarlyBaseDate
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
	Date veryVeryLateBaseDate
	
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
		veryEarlyBaseDate = dateFormat.parse("January 12, 2010 12:00 am")
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
		veryVeryLateBaseDate = dateFormat.parse("July 20, 2011 12:00 am")
		
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
	
	int testPlot(User user, def tagIds, Date startDate, Date endDate, Date currentDate, Closure test) {
		def results = Entry.fetchPlotData(user, tagIds, startDate, endDate, currentDate)
		
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
	void testUpdateRepeatOnFirstDayAllFuture() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", null, null, earlyBaseDate, true, 1), new EntryStats(), earlyBaseDate, true)

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
	void testAlertNoRepeat() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 3pm", RepeatType.ALERT.id, null, earlyBaseDate, true), new EntryStats())
	
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
		} == 1
		
		assert testEntries(user, timeZone, earlyBaseDate, earlyBaseDate) {
			assert it['description'] == "bread"
		} == 1
	
		assert testEntries(user, timeZone, earlyBaseDate + 1, earlyBaseDate + 1) {
		} == 0
	
		assert testEntries(user, timeZone, earlyBaseDate + 7, earlyBaseDate + 7) {
		} == 0
	}
	
	@Test
	void testUpdateToStart() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm", null, null, baseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread start 2pm", null, null, baseDate, true, 1), new EntryStats(), baseDate, true)

		assert updated == entry && updated != null
		
		assert updated.durationType == DurationType.START
		
		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 1
			assert it['description'] == 'bread start'
		} == 1
	}
	
	@Test
	void testUpdateDurationEndTime() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep 8 hours 8am", null, null, baseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "sleep 8 hours 9am", null, null, baseDate, true, 1), new EntryStats(), baseDate, true)

		assert updated == entry && updated != null
		
		assert updated.durationType == DurationType.NONE
		
		// verify new event created on baseDate
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amounts'][0].amount.intValue() == 8
			assert it['description'] == 'sleep'
		} == 1
	}
	
	@Test
	void testRemindFutureToday() {
		EntryStats stats = new EntryStats()
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5pm", RepeatType.ALERT.id, null, baseDate, true), stats)
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T00:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:4, repeatEnd:null)")
	}

	@Test
	void testDeleteRepeatThenUpdate() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat", null, null, earlyBaseDate, true), new EntryStats())
		
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
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 7 2pm repeat", null, entry.repeatEnd, baseDate, true, 1), new EntryStats(), baseDate, false)
		
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
	void "Test remove repeat type"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.DAILYCONCRETEGHOST.id, dateFormat.parse("July 25, 2010 12:00 am"), baseDate, true), new EntryStats())
		assert entry.repeatEnd == dateFormat.parse("July 24, 2010 4:00 pm")
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone2, "testxyz 4pm", null, null, lateBaseDate, true, 1), new EntryStats(), lateBaseDate, true)
		assert updated.repeatTypeId == null
		assert updated.repeatEnd == null
		String x = updated.valueString()
		assert x.endsWith("date:2010-07-03T23:00:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void "Test remove repeat end"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.DAILYCONCRETEGHOST.id, dateFormat.parse("July 25, 2010 12:00 am"), baseDate, true), new EntryStats())
		assert entry.repeatEnd == dateFormat.parse("July 24, 2010 4:00 pm")
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone2, "testxyz 4pm", RepeatType.DAILYCONCRETEGHOST.id, null, lateBaseDate, true, 1), new EntryStats(), lateBaseDate, true)
		assert updated.repeatEnd == null
		String x = updated.valueString()
		assert x.endsWith("date:2010-07-03T23:00:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:1025, repeatEnd:null)")
	}
	
	@Test
	void "Test remove repeat type clear repeat end"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.DAILYCONCRETEGHOST.id, dateFormat.parse("July 25, 2010 12:00 am"), baseDate, true), new EntryStats())
		assert entry.repeatEnd == dateFormat.parse("July 24, 2010 4:00 pm")
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone2, "testxyz 4pm", null, dateFormat.parse("July 25, 2010 12:00 am"), lateBaseDate, true, 1), new EntryStats(), lateBaseDate, true)
		assert updated.repeatEnd == null
		String x = updated.valueString()
		assert x.endsWith("date:2010-07-03T23:00:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:testxyz, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void "Test update repeat daily entry end date"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.DAILYCONCRETEGHOST.id, dateFormat.parse("July 25, 2010 12:00 am"), baseDate, true), new EntryStats())
		assert entry.repeatEnd == dateFormat.parse("July 24, 2010 4:00 pm")
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone2, "testxyz 4pm", RepeatType.DAILYCONCRETEGHOST.id, dateFormat.parse("August 4, 2010 12:00 am"), lateBaseDate, true, 1), new EntryStats(), lateBaseDate, true)
		Date end = updated.repeatEnd
		assert end == dateFormat.parse("August 3, 2010 4:00 pm")
	}
	
	@Test
	void testSumPlotData() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	
		Entry.create(userId, entryParserService.parse(slightDifferentCurrentTime, timeZone, "bread 3", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(lateCurrentTime, timeZone, "bread 2", null, null, tomorrowBaseDate, true), new EntryStats())
		
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
	void testNoRepeatStartDuration() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "sleep start 4pm repeat", null, null, earlyBaseDate, true), new EntryStats())
		assert entry.durationType == DurationType.NONE
		assert entry.repeatType == RepeatType.DAILYCONCRETEGHOST
	}
	
	@Test
	void testUpdateRepeatNoChange() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 5 at 2pm repeat daily", null, null, baseDate, true, 1), new EntryStats(), baseDate, true)

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
	void testUpdatePartialOrCreate() {
		def m = entryParserService.parse(currentTime, timeZone2, "bread summary 1", null, null, baseDate, true)
		m.put('comment','(Withings)')
		m.put('setName','withings import')
		def timezone = (Integer) TimeZoneId.look(timeZone2).getId()
		m.put('timeZoneId', timezone)
		m.amount = m.amounts[0]
		Entry entry = Entry.updatePartialOrCreate(userId, m, null, new EntryStats())
		println entry.valueString()
		assert Entry.count() == 1
		entry = Entry.updatePartialOrCreate(userId, m, null, new EntryStats())
		println Entry.list()
		assert Entry.count() == 1
	}

	@Test
	void testWeeklyRepeatFetchListData() {
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2 4pm weekly", null, null, earlyBaseDate, true), new EntryStats())
				
		assert testEntries(user, timeZone, earlyBaseDate, earlyBaseDate) {
			assert it['description'] == "bread"
		} == 1
	
		assert testEntries(user, timeZone, earlyBaseDate + 1, earlyBaseDate + 1) {
		} == 0
	
		assert testEntries(user, timeZone, earlyBaseDate + 7, earlyBaseDate + 7) {
			assert it['description'] == "bread"
		} == 1
	}
	
	@Test
	void testMonthlyRepeatPlotData() {
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2 4pm monthly", null, null, veryEarlyBaseDate, true), new EntryStats())
		
		DateTimeZone dateTimeZone = timeZoneId.toDateTimeZone()
		
		int c = 0
		LocalDate lastLocalDate = null
		
		assert testPlot(user, [Tag.look("bread").getId()], veryEarlyBaseDate, veryVeryLateBaseDate, veryVeryLateBaseDate) {
			def date = it[0]
			DateTime dateTime = new DateTime(date, dateTimeZone)
			LocalDate localDate = dateTime.toLocalDate()
			
			if (lastLocalDate != null) {
				assert localDate.dayOfMonth() == lastLocalDate.dayOfMonth()
			}
			lastLocalDate = localDate
			assert it[1].intValue() == 2
		} == 19
	}
	
	@Test
	void testAnnualRepeatPlotData() {
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2 4pm yearly", null, null, veryEarlyBaseDate, true), new EntryStats())
		
		DateTimeZone dateTimeZone = timeZoneId.toDateTimeZone()
		
		int c = 0
		LocalDate lastLocalDate = null
		
		assert testPlot(user, [Tag.look("bread").getId()], veryEarlyBaseDate, veryVeryLateBaseDate, veryVeryLateBaseDate) {
			def date = it[0]
			DateTime dateTime = new DateTime(date, dateTimeZone)
			LocalDate localDate = dateTime.toLocalDate()
			
			if (lastLocalDate != null) {
				assert localDate.dayOfMonth() == lastLocalDate.dayOfMonth()
				assert localDate.monthOfYear() == lastLocalDate.monthOfYear()
			}
			lastLocalDate = localDate
			assert it[1].intValue() == 2
		} == 2
	}
	
	@Test
	void testWeeklyRepeatPlotData() {
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2 4pm weekly", null, null, earlyBaseDate, true), new EntryStats())
		
		int c = 0
		def expected = [ 2, 2, 2, 2 ]
		Date lastDate = null
		
		assert testPlot(user, [Tag.look("bread").getId()], earlyBaseDate, veryLateBaseDate, veryLateBaseDate) {
			def date = it[0]
			if (lastDate != null) {
				assert date.getTime() - lastDate.getTime() == 1000L* 60L * 60L * 24L * 7L
			}
			lastDate = date
			assert it[1].intValue() == expected[c++]
			assert it[2] == "bread"
		} == expected.size()
	}
	
	@Test
	void testNoPinnedPlotData() {
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2 4pm button", null, null, earlyBaseDate, true), new EntryStats())
		
		int c = 0
		
		assert testPlot(user, [Tag.look("bread").getId()], earlyBaseDate, veryLateBaseDate, veryLateBaseDate) {
		} == 0
	}
	
	@Test
	void testRemindActivatePlotData() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 3pm remind", null, null, earlyBaseDate, true), new EntryStats())
	
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
		} == 0
		
		entry.update(null, new EntryStats(), earlyBaseDate)

		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
			def date = Utils.dateToGMTString(it[0])
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 1
	}
	
	@Test
	void testMixedRepeatPlotData() {
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 3pm repeat", null, null, veryEarlyBaseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2 4pm repeat", null, null, baseDate, true), new EntryStats())
		
		int c = 0
		def expected = [ 1, 1, 1, 1, 1, 1, 1, 2, 1, 2 ]
		
		assert testPlot(user, [Tag.look("bread").getId()], earlyBaseDate, lateBaseDate, veryLateBaseDate) {
			def date = Utils.dateToGMTString(it[0])
			it = it
			assert it[1].intValue() == expected[c++]
			assert it[2] == "bread"
		} == expected.size()
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
	void testMixedStaticAndRepeatPlotData() {
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 4 1pm", null, null, yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 3pm repeat", null, null, yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 4pm repeat", null, null, baseDate, true), new EntryStats())
		
		int c = 0
		def expected = [ 4, 1, 1, 2, 1, 2 ]
		
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
			def date = Utils.dateToGMTString(it[0])
			it = it
			assert it[1].intValue() == expected[c++]
			assert it[2] == "bread"
		} == expected.size()
	}

	@Test
	void testMostUsedUnitNormalization() {
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "jog 2000 meter 1pm", null, null, yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "jog 2 km 3pm", null, null, yesterdayBaseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "jog 2 kilometer 4pm", null, null, baseDate, true), new EntryStats())
		
		int c = 0
		
		testPlot(user, [Tag.look("jog [distance]").getId()], null, lateBaseDate, veryLateBaseDate
			) {
			++c
			assert it[1].intValue() == 2
		}
		
		assert c == 3
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
	void testRemindActivate() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread remind", null, null, earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")
		
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it.id == entry.getId()
		} == 1
	
		EntryStats stats = new EntryStats(userId)
		
		Entry activated = entry.update(null, stats, baseDate)
		String v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:5, repeatEnd:2010-07-01T19:00:00)")

		assert testEntries(user, timeZone, veryLateBaseDate, lateCurrentTime) {
		} == 1

		Entry updated = activated.update(entryParserService.parse(lateCurrentTime, timeZone, "bread remind",null, null,  baseDate, true, 1), new EntryStats(), baseDate, true)
		
		assert updated == activated

		assert testEntries(user, timeZone, veryLateBaseDate, lateCurrentTime) {
		} == 1
	}
	
	@Test
	void testUpdateRemindJustOnce() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 12pm remind", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry activated = entry.update(null, new EntryStats(), baseDate)
		
		Entry updated = activated.update(entryParserService.parse(currentTime, timeZone, "bread 8 12pm remind", activated.repeatTypeId, activated.repeatEnd, baseDate, true, 1), new EntryStats(), baseDate, false)
		
		assert updated != entry && updated != null

		// verify new event created on baseDate unghosted
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
			def x = it['repeatType']
			assert (it['repeatType'] & RepeatType.GHOST_BIT) == 0
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
			assert (it['repeatType'] & RepeatType.GHOST_BIT) != 0
		} == 1
		
		// verify new event with new amount much after baseDate, but ghosted
		assert testEntries(user, timeZone, veryLateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
			assert (it['repeatType'] & RepeatType.GHOST_BIT) != 0
		} == 1
	
		// verify new event with new amount tomorrow after baseDate, but ghosted
		assert testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
			assert (it['repeatType'] & RepeatType.GHOST_BIT) != 0
		} == 1
	}
	
	@Test
	void testUpdateRemindJustOnceNoTimestamp() {
		Entry entry = Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 5 remind", null, null, earlyBaseDate, true), new EntryStats())
		
		def activated = entry.update(null, new EntryStats(), baseDate)
		
		Entry updated = activated.update(entryParserService.parse(veryLateBaseDate, timeZone, "bread 8 remind", activated.repeatTypeId, activated.repeatEnd, baseDate, true, 1), new EntryStats(), baseDate, false)
		
		assert updated != entry && updated != null

		// verify new event created on baseDate unghosted
		assert testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 8
			def x = it['repeatType']
			assert (it['repeatType'] & RepeatType.GHOST_BIT) == 0
		} == 1
		
		// verify old event still at earlyBaseDate
		assert testEntries(user, timeZone, earlyBaseDate, currentTime) {
			assert it['id'] == entry.getId()
			assert it['amount'].intValue() == 5
			assert (it['repeatType'] & RepeatType.GHOST_BIT) != 0
		} == 1
		
		// verify new event with new amount much after baseDate, but ghosted
		assert testEntries(user, timeZone, veryLateBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
			assert (it['repeatType'] & RepeatType.GHOST_BIT) != 0
		} == 1
	
		// verify new event with new amount tomorrow after baseDate, but ghosted
		assert testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it['id'] != entry.getId()
			assert it['amount'].intValue() == 5
			assert (it['repeatType'] & RepeatType.GHOST_BIT) != 0
		} == 1
	}
	
	@Test
	void testPlotData() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	
		assert testPlot(user, [Tag.look("bread").getId()], null, null, veryLateBaseDate) {
			def date = Utils.dateToGMTString(it[0])
			assert date == "2010-07-01T22:30:00"
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 1
		
		assert testPlot(user, [Tag.look("bread").getId()], lateBaseDate, null, veryLateBaseDate) {
		} == 0
		
		assert testPlot(user, [Tag.look("bread").getId()], earlyBaseDate, baseDate, veryLateBaseDate) {
		} == 0
		
		assert testPlot(user, [Tag.look("bread").getId()], earlyBaseDate, lateBaseDate, veryLateBaseDate) {
		} == 1
	}
	
	@Test
	void testSumPlotDataNight() {
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1am", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 3 2am", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 3am", null, null, tomorrowBaseDate, true), new EntryStats())
		
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
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 3pm", null, null, baseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 5am", null, null, tomorrowBaseDate, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 2pm", null, null, tomorrowBaseDate, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 12am", null, null, dayOne, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 1am", null, null, dayOne, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 12pm", null, null, dayOne, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 12pm", null, null, dayOne, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 1pm", null, null, dayOne, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 6pm", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 6am", null, null, dayOne, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 7am", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 3pm", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 5pm", null, null, dayOne, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1am", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 3 3am", null, null, dayOne, true), new EntryStats())
		Entry repeatBreadEntry = Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 2am repeat", null, null, dayTwo, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 6 9am", null, null, dayThree, true), new EntryStats())
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 1 1pm", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 3 5pm", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 2 7pm repeat", null, null, dayTwo, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 6 4pm", null, null, dayThree, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1am", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 3 3am", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 2am", null, null, dayTwo, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 6 9am", null, null, dayThree, true), new EntryStats())
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 1 1pm", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 3 5pm", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 2 7pm", null, null, dayTwo, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 6 4pm", null, null, dayThree, true), new EntryStats())
		
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
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 1 1am repeat", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 3 3am repeat", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 2 2am repeat", null, null, dayTwo, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "bread 6 9am repeat", null, null, dayThree, true), new EntryStats())
		
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 1 1pm repeat", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 3 5pm repeat", null, null, dayOne, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 2 7pm repeat", null, null, dayTwo, true), new EntryStats())
		Entry.create(userId, entryParserService.parse(veryLateBaseDate, timeZone, "circus 6 4pm repeat", null, null, dayThree, true), new EntryStats())
		
		def breadIds = [ Tag.look("bread").getId() ]
		def circusIds = [ Tag.look("circus").getId() ]
		
		def breadResult = Entry.fetchAverageTime(user, breadIds, startDate, endDate, currentDate, dateTimeZone)
		def circusResult = Entry.fetchAverageTime(user, circusIds, startDate, endDate, currentDate, dateTimeZone)
		
		assert Math.abs(breadResult - 9207) < 1000
		assert Math.abs(circusResult - 58211) < 1000
	}
	
	@Test
	void testRepeatPlotData() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1 3pm repeat", null, null, earlyBaseDate, true), new EntryStats())
	
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
			def date = Utils.dateToGMTString(it[0])
			assert it[1].intValue() == 1
			assert it[2] == "bread"
		} == 8
	}
	
	@Test
	void testRepeatNullValuePlotData() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread _ 3pm remind", null, null, earlyBaseDate, true), new EntryStats())
	
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
		} == 0
		
		def activated = entry.update(null, new EntryStats(), earlyBaseDate)

		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
		} == 0

		Entry updated = activated.update(entryParserService.parse(currentTime, timeZone, "bread 6 3pm remind", null, null, earlyBaseDate, true, 1), new EntryStats(), earlyBaseDate, true)
		
		assert testPlot(user, [Tag.look("bread").getId()], null, lateBaseDate, veryLateBaseDate) {
		} == 1
	}
		
	void testUpdateNonRepeatToRepeat() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm ", null, null, baseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 pinned", null, null, baseDate, true, 1), new EntryStats(), baseDate, true)
		
		assert updated == entry
		
		entry.getRepeatType().equals(RepeatType.CONTINUOUSGHOST)
	}
	
	@Test
	void testUpdateRepeatAllFuture() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", null, null, baseDate, true, 1), new EntryStats(), baseDate, true)
		
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", null, null, baseDate, true, 1), new EntryStats(), baseDate, false)
		
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone2, "bread 5 repeat", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime2, timeZone, "bread 8 repeat", null, null, baseDateShifted, true, 1), new EntryStats(), baseDateShifted, false)
		
		String v = entry.valueString()
	
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 at 2pm repeat", entry.repeatTypeId, entry.repeatEnd, baseDate, true, 1), new EntryStats(), baseDate, false)
		
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

		Entry updated2 = updated.update(entryParserService.parse(currentTime, timeZone, "bread 7 at 2pm repeat", updated.repeatTypeId, updated.repeatEnd, baseDate, true, 1), new EntryStats(), baseDate, false)
		
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 at 2pm repeat", entry.repeatTypeId, entry.repeatEnd, baseDate, true, 1), new EntryStats(), baseDate, false)
		
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
	
		Entry futureEntry = Entry.get(newEntryId)

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
	
		Entry updated2 = updated.update(entryParserService.parse(currentTime, timeZone, "bread 7 at 2pm repeat", updated.repeatTypeId, updated.repeatEnd, baseDate, true, 1), new EntryStats(), baseDate, false)
		
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
	void testUpdateRepeatNotGhostJustOnce() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		
		entry.getRepeatType().unGhost()
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", null, null, baseDate, true, 1), new EntryStats(), baseDate, false)
		
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
	void testUpdateRepeatOnFirstDayJustOnce() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		
		Entry updated = entry.update(entryParserService.parse(currentTime, timeZone, "bread 8 at 2pm repeat daily", null, null, earlyBaseDate, true, 1), new EntryStats(), earlyBaseDate, false)

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
	void testLateTimestamps() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 11pm remind", null, null, baseDate, true), new EntryStats())
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T06:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 11pm", null, null, baseDate, true), new EntryStats())
		v = entry2.valueString()
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T06:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
	}
	
	@Test
	void testDeleteContinuousRepeat() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread pinned", null, null, baseDate, true), new EntryStats())
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, baseDate, true), new EntryStats())
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
			Entry e2 = Entry.get(it.id)
			assert e2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
			assert e2.getId() != entry.getId()
		}
	}
	
	@Test
	void testDeleteTimedRepeatEndsToday() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, baseDate, true), new EntryStats())
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, baseDate, true), new EntryStats())
		
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, baseDate, true), new EntryStats())
		
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, baseDate, true), new EntryStats())
		
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, baseDate, true), new EntryStats())
		String v = entry.valueString()
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
	void testRepeatGhostNonContinuous() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		entry.setRepeatType(RepeatType.DAILYGHOST)
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:513, repeatEnd:null)")
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		for (entryDesc in entries) {
			assert entryDesc['id'] == entry.getId()
		}
		
		EntryStats stats = new EntryStats(userId)
		
		Entry activated = entry.update(null, stats, baseDate)
		v = activated.valueString()
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1, repeatEnd:2010-07-01T21:00:00)")
		
		Entry activated3 = entry.update(null, stats, earlyBaseDate)
		v = activated3.valueString()
		assert activated3.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1, repeatEnd:2010-06-25T21:00:00)")
		assert activated3 == entry
	}
	
	@Test
	void testRemind() {
		EntryStats stats = new EntryStats()
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm remind", null, null, baseDate, true), stats)
		String v = entry.valueString()
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
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 75 2pm remind", null, null, baseDate, true), new EntryStats())
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:75.000000000, units:, amountPrecision:3, comment:remind, repeatType:517, repeatEnd:null)")
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.amount.intValue() == 75
			assert (it.repeatType & RepeatType.GHOST_BIT) != 0
		}

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.amount.intValue() == 75
			assert (it.repeatType & RepeatType.GHOST_BIT) != 0
		}
	}

	@Test
	void testRemindList2() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm remind", null, null, baseDate, true), new EntryStats())
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")
		
		testEntries(user, timeZone, tomorrowBaseDate, currentTime) {
			assert it.amount.intValue() == 1
			assert (it.repeatType & RepeatType.GHOST_BIT) != 0
		}

		testEntries(user, timeZone, baseDate, currentTime) {
			assert it.amount.intValue() == 1
			assert (it.repeatType & RepeatType.GHOST_BIT) != 0
		}
	}

	@Test
	void testUpdate() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		assert entry.update(entryParserService.parse(currentTime, timeZone, "bread 1 slice", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		assert entry.update(entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")

		assert entry.update(entryParserService.parse(currentTime, timeZone, "aspirin 1 tablet repeat daily", null, dayAfterTomorrowBaseDate, baseDate, true, 1), new EntryStats(), baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:aspirin, amount:1.000000000, units:tablet, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:2010-07-02T19:00:00)")
	}
	
	@Test
	void testUpdateDate() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 1", null, null, baseDate, true), new EntryStats())
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")

		assert entry.update(entryParserService.parse(currentTime, timeZone, "bread 1 slice", null, null, tomorrowBaseDate, true, 1), new EntryStats(), baseDate, true) != null
		println entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-02T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:slice, amountPrecision:3, comment:, repeatType:null, repeatEnd:null)")
	}

	@Test
	void testUpdateRepeat() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread foo repeat daily", null, null, baseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T22:30:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		println("Attempting to update")
		assert entry.update(entryParserService.parse(currentTime, timeZone, "bread repeat daily", null, null, baseDate, true, 1), new EntryStats(), baseDate, true) != null
		
		testEntries(user, timeZone2, timeZone2BaseDate, lateCurrentTime) {
			assert it['id'] == entry.getId()
			def ds = Utils.dateToGMTString(it['date'])
			assert Utils.dateToGMTString(it['date']).equals("2010-07-03T19:00:00")
		}
	}

	@Test
	void testUpdateRepeatChangeTime() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread foo repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		println("Attempting to update")
		
		assert entry.update(entryParserService.parse(currentTime, timeZone, "headache 8 at 3pm repeat daily", null, null, earlyBaseDate, true, 1), new EntryStats(), earlyBaseDate, true) != null
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
			def ds = Utils.dateToGMTString(it['date'])
			assert Utils.dateToGMTString(it['date']).equals("2010-07-01T22:00:00")
		}
	}

	@Test
	void testUpdateRepeatChangeTimeZone() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread foo 2pm", null, null, earlyBaseDate, true), new EntryStats())
		String v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
		println("Attempting to update")
		
		entry.update(entryParserService.parse(currentTime, timeZone2, "bread foo 3pm", null, null, earlyBaseDate2, true, 1), new EntryStats(), baseDate, true)
		v = entry.valueString()
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T21:00:00, datePrecisionSecs:180, timeZoneName:America/New_York, description:bread foo, amount:1.000000000, units:, amountPrecision:-1, comment:, repeatType:null, repeatEnd:null)")
	}

	@Test
	void "Test canDelete when user is null"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 4pm", null, null, baseDate, true), new EntryStats())
		Map result = Entry.canDelete(entry, null)
		assert result.canDelete == false
		assert result.messageCode == "auth.error.message"
	}

	@Test
	void "Test canDelete when user has created the entry"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz end at 4pm", null, null, baseDate, true), new EntryStats())
		Map result = Entry.canDelete(entry, user)
		assert result.canDelete == true
	}

	@Test
	void "Test canDelete when user has not created the entry but is admin of the sprint which contains the entry"() {
		def sprint = Sprint.create(new Date(), user, "Sprint", Visibility.PUBLIC)
		
		Entry entry = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(currentTime, timeZone, "testxyz end at 4pm", null, null, baseDate, true), new EntryStats())
		Map result = Entry.canDelete(entry, user)
		assert result.canDelete == true
	}

	@Test
	void "Test canDelete when user has not created the entry and is not an admin of the sprint containing the entry"() {
		Map params = [username: "z", sex: "M", name: "z z", email: "z@z.com", birthdate: "01/01/2001", password: "z"]
		
		User user2 = User.create(params)
		Utils.save(user2, true)
		
		def sprint = Sprint.create(new Date(), user2, "Sprint", Visibility.PUBLIC)
		
		Entry entry = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(currentTime, timeZone, "testxyz end at 4pm", null, null, baseDate, true), new EntryStats())
		Map result = Entry.canDelete(entry, user)
		assert result.canDelete == false
		assert result.messageCode == "delete.entry.permission.denied"
	}

	@Test
	void "Test create repeat daily entry when end date is before entry date"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime2, timeZone, "testxyz 4pm", RepeatType.DAILYCONCRETEGHOST.id, currentTime, baseDate2, true), new EntryStats())
		assert entry.repeatEnd == entry.date
	}

	@Test
	void "Test create repeat weekly entry when end date is before entry date"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.WEEKLYCONCRETEGHOST.id, dateFormat.parse("July 4, 2010 1:58 pm"), baseDate, true), new EntryStats())
		assert entry.repeatEnd == entry.date
	}

	@Test
	void "Test create repeat monthly entry when end date is before entry date"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.MONTHLYCONCRETEGHOST.id, dateFormat.parse("July 25, 2010 1:58 pm"), baseDate, true), new EntryStats())
		assert entry.repeatEnd == entry.date
	}

	@Test
	void "Test create repeat monthly entry for end date"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.MONTHLYCONCRETEGHOST.id, dateFormat.parse("Aug 25, 2010 12:00 am"), earlyBaseDate, true), new EntryStats())
		assert entry.repeatEnd == dateFormat.parse("July 25, 2010 4:00 pm")
	}

	@Test
	void "Test create repeat weekly entry for end date"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.WEEKLYCONCRETEGHOST.id, dateFormat.parse("July 25, 2010 12:00 am"), baseDate, true), new EntryStats())
		assert entry.repeatEnd == dateFormat.parse("July 15, 2010 4:00 pm")
	}
	
	@Test
	void "Test toggle ghost"() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "testxyz 4pm", RepeatType.WEEKLYGHOST.id, dateFormat.parse("July 25, 2010 12:00 am"), baseDate, true), new EntryStats())
		entry.unGhost(new EntryStats())
		assert !entry.repeatType.isGhost()
	}
	
	/*@Test
	void testRepeatAgain() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, baseDate, true), new EntryStats())
		
		Entry entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 5pm repeat daily", null, null, baseDate, true), new EntryStats())
		
		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, tomorrowBaseDate, true), new EntryStats())
		
		assert entry.getRepeatEnd().equals(entry2.fetchPreviousDate())
		
		Entry.delete(entry2, new EntryStats())
		
		Entry entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread 2pm repeat daily", null, null, dayAfterTomorrowBaseDate, true), new EntryStats())

		assert entry.getRepeatEnd().equals(entry2.fetchPreviousDate())
	}
	
	@Test
	void testRemindCreateInMiddle() {
		// test creating repeats in middle of earlier range
		
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "bread remind daily", null, null, earlyBaseDate, true), new EntryStats())
		Utils.save(entry, true)
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:517, repeatEnd:null)")
		
		testEntries(user, timeZone, baseDate, currentTime) {
			assert it['id'] == entry.getId()
		}
		
		Entry activated = entry.update(null, new EntryStats(userId), lateBaseDate)
		assert activated.valueString().equals("Entry(userId:" + userId + ", date:2010-07-03T19:00:00, datePrecisionSecs:86400, timeZoneName:America/Los_Angeles, description:bread, amount:1.000000000, units:, amountPrecision:-1, comment:remind, repeatType:5, repeatEnd:2010-07-03T19:00:00)")
		
		// create repeat event in the middle
		Entry middleEntry = Entry.create(userId, entryParserService.parse(lateCurrentTime, timeZone, "bread repeat daily", null, null, baseDate, true), new EntryStats())
		
		// make sure prior repeatEnd is reset
		assert entry.getRepeatEnd().equals(middleEntry.fetchPreviousDate())
		
		// delete middle event
		Entry.delete(middleEntry, new EntryStats())		
	}
	
	@Test
	void testReplaceRepeat() {
		Entry entry = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "exercise 12pm repeat daily", null, null, earlyBaseDate, true), new EntryStats())
		assert entry.valueString().equals("Entry(userId:" + userId + ", date:2010-06-25T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:exercise, amount:1.000000000, units:, amountPrecision:-1, comment:repeat, repeatType:1025, repeatEnd:null)")
		println("Attempting to update")

		Entry entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, "exercise 8 12pm repeat daily", null, null, baseDate, true), new EntryStats())
		assert entry2.valueString().equals("Entry(userId:" + userId + ", date:2010-07-01T19:00:00, datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:exercise, amount:8.000000000, units:, amountPrecision:3, comment:repeat, repeatType:1025, repeatEnd:null)")
		
		def repeatEnd = entry.getRepeatEnd()
		assert repeatEnd.equals(entry2.fetchPreviousDate())
	}*/
}

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
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.services.DatabaseService

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
	void testMultiCreate() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "bread 5 slices 500 calories", baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
		}
		
		assert x == ":bread [slices]:bread:bread [calories]:bread"
	}
	
	@Test
	void testMultiCreateSuffix() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "run 5 miles 1000 feet elevation", baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.units
		}
		
		assert x == ":run [elevation]:run:feet elevation:run [distance]:run:miles"
	}
	
	@Test
	// if units are the same with the same suffix, don't create duplicate entries but instead sum them together
	void testMultiCreateAddSimilarUnits() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "run 2 hours 30 minutes", baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.amount
			x += ":" + e.units
		}
		
		assert x == ":run [duration]:run:2.500000000:hours"
	}
	
	@Test
	// if units are the same with the same suffix, don't create duplicate entries but instead sum them together
	void testMultiCreateMinutesSeconds() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "swam 40 minutes 30 seconds", baseDate, true), new EntryStats())
		
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		String x = ""
		for (Entry e : group) {
			x += ":" + e.tag.getDescription()
			x += ":" + e.baseTag.getDescription()
			x += ":" + e.amount
			x += ":" + e.units
		}
		
		assert x == ":swam [duration]:swam:40.500000000:minutes"
	}
	
	@Test
	// if units are the same with the same suffix, don't create duplicate entries but instead sum them together
	void testMultiCreateAddSimilarUnitsListExpansion() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone2, "run 2 hours 30 minutes", baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 2
			assert amounts[0].units == "hours"
			assert amounts[1].amount.intValue() == 30
			assert amounts[1].units == "mins"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiList() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 slices 500 calories", baseDate, true), new EntryStats())
		
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
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 slices 500 calories", baseDate, true), new EntryStats())
		
		assert entry.update(Entry.parse(currentTime, timeZone, "bread 2 slices 255 calories", baseDate, true, true), new EntryStats(), baseDate, true) != null

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
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 slices 500 calories", baseDate, true), new EntryStats())
		
		assert entry.update(Entry.parse(currentTime, timeZone, "bread 2 beans 255 calories", baseDate, true, true), new EntryStats(), baseDate, true) != null

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
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 slices 500 calories", baseDate, true), new EntryStats())
		
		assert entry.update(Entry.parse(currentTime, timeZone, "bread 2 hours 30 minutes 255 calories", baseDate, true, true), new EntryStats(), baseDate, true) != null

		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.toString() == "2"
			assert amounts[0].units == "hours"
			assert amounts[1].amount.intValue() == 30
			assert amounts[1].units == "mins"
			assert amounts[2].amount.intValue() == 255
			assert amounts[2].units == "calories"
		}
		
		assert c == 1
	}
	
	@Test
	void testMultiUpdateAddElements() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 slices 500 calories", baseDate, true), new EntryStats())
		
		assert entry.update(Entry.parse(currentTime, timeZone, "bread 2 beans 255 calories 127 steps", baseDate, true, true), new EntryStats(), baseDate, true) != null

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
	void testMultiCreatePrioritizeSort() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "run 1279 steps 2 miles", baseDate, true), new EntryStats())
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 1279
			assert amounts[0].units == "steps"
			assert amounts[1].amount.intValue() == 2
			assert amounts[1].units == "miles"
		}
		
		assert c == 1
		
		def entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, "run 3 miles 8900 steps", earlyBaseDate, true), new EntryStats())
		
		entries = Entry.fetchListData(user, timeZone, earlyBaseDate, currentTime)
		c = 0
		for (entryDesc in entries) {
			++c
			def amounts = entryDesc['amounts']
			assert amounts[0].amount.intValue() == 8900
			assert amounts[0].units == "steps"
			assert amounts[1].amount.intValue() == 3
			assert amounts[1].units == "miles"
		}
		
		assert c == 1
		
	}
	
	@Test
	void testMultiUpdatePreserveOrder() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "run 1 aye 2 b", baseDate, true), new EntryStats())
		
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
		
		assert entry.update(Entry.parse(currentTime, timeZone, "run 3 aye 2 b", baseDate, true, true), new EntryStats(), baseDate, true) != null

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
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "bread 5 slices 500 calories", baseDate, true), new EntryStats())

		Entry.delete(entry, new EntryStats())
				
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
		}
		
		assert c == 0
	}
	
/*	@Test
	void testBloodPressure() {
		def entry = Entry.create(userId, Entry.parse(currentTime, timeZone, "blood pressure 120/80", baseDate, true), new EntryStats())

		Entry.delete(entry, new EntryStats())
				
		Iterable<Entry> group = entry.fetchGroupEntries()
		
		def entries = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		int c = 0
		for (entryDesc in entries) {
			++c
		}
		
		assert c == 0
	}*/
}

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
class TagStatsTests extends CuriousTestCase {
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

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "poiguj", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(twoMonthsAgo, timeZone, "poiguj", yesterday, true), stats)
		
		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "poiguj", yesterday, true), stats)

		entry = Entry.create(userId, Entry.parse(yesterday, timeZone, "poiguj", yesterday, true), stats)

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
		
		assert !result.getTypicallyNoAmount()
		
		def tabletTag = Tag.look("abcdef tablets")
		
		TagValueStats resultV = TagValueStats.createOrUpdate(userId, tabletTag.getId(), null)
		
		assert resultV.getMinimum().intValue() == 1
		
		assert resultV.getMaximum().intValue() == 3
		
		def tag2 = Tag.look("qwerty")
		
		TagStats result2 = TagStats.createOrUpdate(userId, tag2.getId())
		
		println "Usage of qwerty: " + result2.getMostRecentUsage()

		assert result2.getLastAmount().intValue() == 1
		
		assert result2.getLastAmountPrecision() > 0
		
		assert result2.getLastUnits().equals("mg")
		
		assert !result2.getTypicallyNoAmount()
		
		def tag3 = Tag.look("sando")
		
		TagStats result3 = TagStats.createOrUpdate(userId, tag3.getId())
		
		println "Usage of sando: " + result2.getMostRecentUsage()

		assert result3.getLastAmount() == null
		
		assert result3.getLastAmountPrecision() < 0
		
		assert result3.getLastUnits().equals("mg")
		
		assert !result3.getTypicallyNoAmount()
		
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
		
		def tag5 = Tag.look("poiguj")
		
		TagStats result5 = TagStats.createOrUpdate(userId, tag4.getId())
		
		assert result5.getTypicallyNoAmount()
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

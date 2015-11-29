package us.wearecurio.model.integration

import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

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
import us.wearecurio.services.AlertGenerationService
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EntryParserService
import groovy.transform.TypeChecked

import org.joda.time.*

import grails.test.mixin.*
import us.wearecurio.utility.Utils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

import us.wearecurio.model.AlertNotification

@TestMixin(IntegrationTestMixin)
class AlertNotificationTests extends CuriousTestCase {
	static transactional = true
	
	EntryParserService entryParserService
	AlertGenerationService alertGenerationService

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
	void testStandaloneAlertGenerationServiceGenAlerts() {
		def m = entryParserService.parse(currentTime, timeZone, "bread", null, null, baseDate, true)
		m['repeatType'] = RepeatType.ALERT
		
		Entry entry = Entry.create(userId, m, new EntryStats())
		
		alertGenerationService.generate(baseDate)
		
		def pendingAlerts = AlertNotification.pendingAlerts(userId, baseDate - 1, baseDate + 1)
		
		assert pendingAlerts.size () == 1
		assert pendingAlerts[0].date == currentTime
		assert pendingAlerts[0].text == "bread"
	}
	
	@Test
	void testStandaloneAlertGenerationService() {
		def m = entryParserService.parse(currentTime, timeZone, "bread", null, null, baseDate, true)
		m['repeatType'] = RepeatType.ALERT
		
		Entry entry = Entry.create(userId, m, new EntryStats())
		
		alertGenerationService.regenerate(userId, baseDate)
		
		def pendingAlerts = AlertNotification.pendingAlerts(userId, baseDate - 1, baseDate + 1)
		
		assert pendingAlerts.size () == 1
		assert pendingAlerts[0].date == currentTime
		assert pendingAlerts[0].text == "bread"
	}
	
	@Test
	void testStandaloneAlert() {
		def m = entryParserService.parse(currentTime, timeZone, "bread", null, null, baseDate, true)
		m['repeatType'] = RepeatType.ALERT
		
		Entry entry = Entry.create(userId, m, new EntryStats())
		
		AlertNotification.generate(userId, baseDate - 1, baseDate + 1)
		
		def pendingAlerts = AlertNotification.pendingAlerts(userId, baseDate - 1, baseDate + 1)
		
		assert pendingAlerts.size () == 1
		assert pendingAlerts[0].date == currentTime
		assert pendingAlerts[0].text == "bread"
	}
	
	@Test
	void testAlertNotificationCreation() {
		AlertNotification alert = AlertNotification.createOrUpdate(userId, currentTime, 1L, "This is an alert")
		
		assert alert.userId == userId
		assert alert.date == currentTime
		assert alert.objectId == 1L
		assert alert.text == "This is an alert"
		
		def pendingAlerts = AlertNotification.pendingAlerts(userId, currentTime - 1, currentTime + 1)
		
		assert pendingAlerts.size () == 1
		assert pendingAlerts[0].id == alert.id 
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
	
		AlertNotification.generate(userId, baseDate - 1, baseDate + 1)
		
		def pendingAlerts = AlertNotification.pendingAlerts(userId, baseDate - 1, baseDate + 1)
		
		assert pendingAlerts.size () == 1
		assert pendingAlerts[0].date == new Date(baseDate.getTime() + 12L * 60L * 60000L)
		assert pendingAlerts[0].text == "bread remind"
	}
	/**/	
}

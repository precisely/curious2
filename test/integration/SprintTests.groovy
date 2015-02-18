import java.text.DateFormat;
import java.util.Date;

import grails.test.*
import us.wearecurio.server.Session
import us.wearecurio.support.EntryStats
import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.TimeZoneId;
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.Entry
import us.wearecurio.model.Model.Visibility
import us.wearecurio.utility.Utils
import static org.junit.Assert.*

import org.joda.time.DateTimeZone;
import org.junit.*

import grails.test.mixin.*

class SprintTests extends CuriousTestCase {
	static transactional = true

	User user2
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
		
		Map params = new HashMap()
		params.put("username", "testuser2")
		params.put("email", "test2@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first2")
		params.put("last", "last2")
		params.put("sex", "M")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1991")

		user2 = User.create(params)
		Utils.save(user2, true)
		
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

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreateSprint() {
		def sprint = Sprint.create(user)
		assert sprint.userId == user.id
		
		sprint = Sprint.create(user2, "Sprint", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Sprint"
		assert sprint.visibility == Visibility.PUBLIC
	}
	
	@Test
	void testCreateSprintTags() {
		Sprint sprint = Sprint.create(user2, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar sprint"
		
		def entry1 = Entry.create(sprint.getVirtualUserId(), Entry.parse(Sprint.getSprintBaseDate(), "UTC", "coffee pinned", Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry2 = Entry.create(sprint.getVirtualUserId(), Entry.parse(Sprint.getSprintBaseDate(), "UTC", "sugar pinned", Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry3 = Entry.create(sprint.getVirtualUserId(), Entry.parse(Sprint.getSprintBaseDate(), "UTC", "aspirin 200mg 3pm repeat", Sprint.getSprintBaseDate(), true), new EntryStats())
		
		sprint.addMember(user.id)
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		def list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		assert sprint.hasStarted(user.id, currentTime)
		assert sprint.hasStarted(user.id, tomorrowCurrentTime)
		
		boolean sugar = false, aspirin = false, coffee = false, sprintStart = false
		
		for (record in list) {
			if (record.description == 'sugar' && (record.repeatType & Entry.RepeatType.CONTINUOUS_BIT))
				sugar = true
			else if (record.description == 'aspirin' && (record.repeatType & Entry.RepeatType.DAILY_BIT))
				aspirin = true
			else if (record.description == 'coffee' && (record.repeatType & Entry.RepeatType.CONTINUOUS_BIT))
				coffee = true
			else if (record.description == 'caffeine sugar sprint start')
				sprintStart = true
			else
				assert false
		}
		
		assert sugar && aspirin && coffee && sprintStart
		
		sprint.stop(user.id, tomorrowBaseDate, tomorrowCurrentTime, timeZone, new EntryStats())
		
		assert sprint.hasStarted(user.id, currentTime)
		assert !sprint.hasStarted(user.id, tomorrowCurrentTime)
		
		list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		sprintStart = false
		aspirin = false
		
		for (record in list) {
			if (record.description == 'aspirin' && (record.repeatType & Entry.RepeatType.DAILY_BIT))
				aspirin = true
			else if (record.description == 'caffeine sugar sprint start')
				sprintStart = true
			else
				assert false
		}
		
		assert aspirin && sprintStart

		list = Entry.fetchListData(user, timeZone, tomorrowBaseDate, tomorrowCurrentTime)
		
		boolean sprintStop = false, sprintDuration = false
		
		for (record in list) {
			if (record.description == 'caffeine sugar sprint end')
				sprintStop = true
			else if (record.description == 'caffeine sugar sprint')
				sprintDuration = true
			else
				assert false
		}
		
		assert sprintStop && sprintDuration
	}
	
	@Test
	void testStartTwice() {
		Sprint sprint = Sprint.create(user2, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar sprint"
		
		def entry1 = Entry.create(sprint.getVirtualUserId(), Entry.parse(Sprint.getSprintBaseDate(), "UTC", "coffee pinned", Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry2 = Entry.create(sprint.getVirtualUserId(), Entry.parse(Sprint.getSprintBaseDate(), "UTC", "sugar pinned", Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry3 = Entry.create(sprint.getVirtualUserId(), Entry.parse(Sprint.getSprintBaseDate(), "UTC", "aspirin 200mg 3pm repeat", Sprint.getSprintBaseDate(), true), new EntryStats())
		
		sprint.addMember(user.id)
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		def list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		int sugar = 0, aspirin = 0, coffee = 0, sprintStart = 0
		
		for (record in list) {
			if (record.description == 'sugar' && (record.repeatType & Entry.RepeatType.CONTINUOUS_BIT))
				sugar++
			else if (record.description == 'aspirin' && (record.repeatType & Entry.RepeatType.DAILY_BIT))
				aspirin++
			else if (record.description == 'coffee' && (record.repeatType & Entry.RepeatType.CONTINUOUS_BIT))
				coffee++
			else if (record.description == 'caffeine sugar sprint start')
				sprintStart++
			else
				assert false
		}
		
		assert sugar == 1 && aspirin == 1 && coffee == 1 && sprintStart == 1
		
		sprint.stop(user.id, tomorrowBaseDate, tomorrowCurrentTime, timeZone, new EntryStats())
		
		sprint.stop(user.id, tomorrowBaseDate, tomorrowCurrentTime, timeZone, new EntryStats())
		
		list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		sprintStart = 0
		aspirin = 0
		
		for (record in list) {
			if (record.description == 'aspirin' && (record.repeatType & Entry.RepeatType.DAILY_BIT))
				aspirin++
			else if (record.description == 'caffeine sugar sprint start')
				sprintStart++
			else
				assert false
		}
		
		assert aspirin == 1 && sprintStart == 1

		list = Entry.fetchListData(user, timeZone, tomorrowBaseDate, tomorrowCurrentTime)
		
		boolean sprintStop = false, sprintDuration = false
		
		for (record in list) {
			if (record.description == 'caffeine sugar sprint end')
				sprintStop = true
			else if (record.description == 'caffeine sugar sprint')
				sprintDuration = true
			else
				assert false
		}
		
		assert sprintStop && sprintDuration
	}
}

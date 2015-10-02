import java.text.DateFormat;
import java.util.Date;

import grails.test.*
import us.wearecurio.server.Session
import us.wearecurio.services.EntryParserService
import us.wearecurio.support.EntryStats
import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.TimeZoneId;
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.Entry
import us.wearecurio.data.RepeatType
import us.wearecurio.model.Model.Visibility
import us.wearecurio.utility.Utils
import static org.junit.Assert.*

import org.joda.time.DateTimeZone;
import org.junit.*

import grails.test.mixin.*

class SprintTests extends CuriousTestCase {
	static transactional = true
	
	EntryParserService entryParserService

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
		Sprint.list()*.delete(flush: true)		// For some reason, Sprint list is not getting flushed after tests

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
	void testCreateSprintTags() {
		Sprint sprint = Sprint.create(currentTime, user2, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar sprint"
		
		def entry1 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "coffee pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry2 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "sugar pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry3 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "aspirin 200mg 3pm repeat", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		
		sprint.addMember(user.id)
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		def list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		assert sprint.hasStarted(user.id, currentTime)
		assert sprint.hasStarted(user.id, tomorrowCurrentTime)
		
		boolean sugar = false, aspirin = false, coffee = false, sprintStart = false
		
		for (record in list) {
			if (record.description == 'sugar' && (record.repeatType & RepeatType.CONTINUOUS_BIT))
				sugar = true
			else if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT))
				aspirin = true
			else if (record.description == 'coffee' && (record.repeatType & RepeatType.CONTINUOUS_BIT))
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
			if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT))
				aspirin = true
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
	
/*	@Test
	void testCreateSprint() {
		def sprint = Sprint.create(user)
		assert sprint.userId == user.id
		
		sprint = Sprint.create(currentTime, user2, "Sprint", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Sprint"
		assert sprint.visibility == Visibility.PUBLIC
	}
	
	@Test
	void testCreateSprintAdmin() {
		def sprint = Sprint.create(currentTime, user2, "Sprint", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Sprint"
		assert sprint.visibility == Visibility.PUBLIC
		
		assert sprint.hasAdmin(user2.id)
		
		sprint.addAdmin(user.id)
		assert sprint.hasAdmin(user.id)
		assert sprint.hasReader(user.id)
		assert sprint.hasWriter(user.id)
		
		sprint.removeAdmin(user.id)
		assert !sprint.hasAdmin(user.id)
	}
	
	@Test
	void testInviteSprintUsers() {
		def sprint = Sprint.create(currentTime, user2, "Sprint", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Sprint"
		assert sprint.visibility == Visibility.PUBLIC
		
		assert sprint.hasAdmin(user2.id)
		
		sprint.addInvited(user.id)
		assert sprint.hasInvited(user.id)
		assert !sprint.hasReader(user.id)
		
		sprint.update([name:'New Name'])
		assert !sprint.hasInvited(user.id)
		assert sprint.hasReader(user.id)
		assert sprint.hasWriter(user.id)
		assert !sprint.hasAdmin(user.id)
		assert sprint.fetchUserGroup().fullName.equals("New Name Tracking Sprint")
		assert sprint.fetchTagName().equals("new name sprint")
	}
	
	@Test
	void testInviteSprintAdmin() {
		def sprint = Sprint.create(currentTime, user2, "Sprint", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Sprint"
		assert sprint.visibility == Visibility.PUBLIC
		
		assert sprint.hasAdmin(user2.id)
		
		sprint.addInvitedAdmin(user.id)
		assert sprint.hasInvitedAdmin(user.id)
		assert !sprint.hasInvited(user.id)
		assert !sprint.hasReader(user.id)
		
		sprint.update([name:'New Name'])
		assert !sprint.hasInvitedAdmin(user.id)
		assert !sprint.hasInvited(user.id)
		assert sprint.hasReader(user.id)
		assert sprint.hasWriter(user.id)
		assert sprint.hasAdmin(user.id)
		assert sprint.fetchUserGroup().fullName.equals("New Name Tracking Sprint")
		assert sprint.fetchTagName().equals("new name sprint")
	}
	
	@Test
	void testCreateSprintTagsDeleteRecreate() {
		Sprint sprint = Sprint.create(currentTime, user2, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar sprint"
		
		def entry1 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "coffee pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry2 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "sugar pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry3 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "aspirin 200mg 3pm repeat", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		
		sprint.addMember(user.id)
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		def list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		assert sprint.hasStarted(user.id, currentTime)
		assert sprint.hasStarted(user.id, tomorrowCurrentTime)
		
		boolean sugar = false, aspirin = false, coffee = false, sprintStart = false
		
		Long sugarEntryId = null
		Long aspirinEntryId = null
		
		for (record in list) {
			if (record.description == 'sugar' && (record.repeatType & RepeatType.CONTINUOUS_BIT)) {
				sugar = true
				sugarEntryId = record.id
			} else if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT)) {
				aspirin = true
				aspirinEntryId = record.id
			} else if (record.description == 'coffee' && (record.repeatType & RepeatType.CONTINUOUS_BIT))
				coffee = true
			else if (record.description == 'caffeine sugar sprint start')
				sprintStart = true
			else
				assert false
		}
		
		assert sugar && aspirin && coffee && sprintStart

		Entry.delete(Entry.get(sugarEntryId), new EntryStats())
		def newSugarEntry = Entry.create(user.id, entryParserService.parse(baseDate, timeZone, "sugar pinned", null, null, baseDate, true), new EntryStats())
		
		sprint.stop(user.id, tomorrowBaseDate, tomorrowCurrentTime, timeZone, new EntryStats())
		
		assert sprint.hasStarted(user.id, currentTime)
		assert !sprint.hasStarted(user.id, tomorrowCurrentTime)
		
		list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		sprintStart = false
		aspirin = false
		
		for (record in list) {
			if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT))
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
		Sprint sprint = Sprint.create(currentTime, user2, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar sprint"
		
		def entry1 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "coffee pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry2 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "sugar pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry3 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "aspirin 200mg 3pm repeat", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		
		sprint.addMember(user.id)
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		def list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		int sugar = 0, aspirin = 0, coffee = 0, sprintStart = 0
		
		for (record in list) {
			if (record.description == 'sugar' && (record.repeatType & RepeatType.CONTINUOUS_BIT))
				sugar++
			else if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT))
				aspirin++
			else if (record.description == 'coffee' && (record.repeatType & RepeatType.CONTINUOUS_BIT))
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
			if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT))
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
	
	@Test
	void testCreateSprintTagsDuplicatePinned() {
		Sprint sprint = Sprint.create(currentTime, user2, "Caffeine + Sugar", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Caffeine + Sugar"
		assert sprint.visibility == Visibility.PUBLIC
		assert sprint.fetchTagName() == "caffeine sugar sprint"
		
		def entry1 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "coffee pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry2 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "sugar pinned", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry3 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "aspirin 200mg 3pm repeat", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		def entry4 = Entry.create(sprint.getVirtualUserId(), entryParserService.parse(Sprint.getSprintBaseDate(), "UTC", "exercise 1pm", null, null, Sprint.getSprintBaseDate(), true), new EntryStats())
		
		def entryP = Entry.create(user.id, entryParserService.parse(baseDate, "UTC", "coffee pinned", null, null, currentTime, true), new EntryStats())
		
		sprint.addMember(user.id)
		
		sprint.start(user.id, baseDate, currentTime, timeZone, new EntryStats())
		
		def list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		assert sprint.hasStarted(user.id, currentTime)
		assert sprint.hasStarted(user.id, tomorrowCurrentTime)
		
		int sugar = 0, aspirin = 0, coffee = 0, sprintStart = 0
		
		for (record in list) {
			if (record.description == 'sugar' && (record.repeatType & RepeatType.CONTINUOUS_BIT))
				sugar++
			else if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT))
				aspirin++
			else if (record.description == 'coffee' && (record.repeatType & RepeatType.CONTINUOUS_BIT))
				coffee++
			else if (record.description == 'caffeine sugar sprint start')
				sprintStart++
			else if (record.description == 'exercise')
				assert false
			else
				assert false
		}
		
		assert sugar == 1 && aspirin == 1 && coffee == 1 && sprintStart == 1
		
		sprint.stop(user.id, tomorrowBaseDate, tomorrowCurrentTime, timeZone, new EntryStats())
		
		assert sprint.hasStarted(user.id, currentTime)
		assert !sprint.hasStarted(user.id, tomorrowCurrentTime)
		
		list = Entry.fetchListData(user, timeZone, baseDate, currentTime)
		
		sprintStart = 0
		aspirin = 0
		
		for (record in list) {
			if (record.description == 'aspirin' && (record.repeatType & RepeatType.DAILY_BIT))
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
	
	Sprint dummySprint1, dummySprint2, dummySprint3
	void mockSprintData() {
		dummySprint1 = Sprint.create(currentTime, user, "demo1", Visibility.PRIVATE)
		dummySprint2 = Sprint.create(currentTime, user, "demo2", Visibility.PRIVATE)
		dummySprint3 = Sprint.create(currentTime, user, "demo3", Visibility.PRIVATE)
		dummySprint1.addMember(user2.getId())
		dummySprint2.addAdmin(user2.getId())
	}
	
	@Test
	void "Test getSprintListForUser when no userId is passed"() {
		mockSprintData()
		List<Sprint> sprintList = Sprint.getSprintListForUser(null, 5, 0)
		assert sprintList == []
	}
	
	@Test
	void "Test getSprintListForUser when a user has no sprint"() {
		List<Sprint> sprintList = Sprint.getSprintListForUser(user2.getId(), 5, 0)
		assert sprintList == []
	}
	
	@Test
	void testGetSprintListForUser() {
		mockSprintData()
		List<Sprint> sprintList = Sprint.getSprintListForUser(user2.getId(), 5, 0)
		assert sprintList.size() == 2
		assert sprintList[0].id == dummySprint1.id || dummySprint2.id
		assert sprintList[1].id == dummySprint2.id || dummySprint1.id
		assert sprintList[0].id != sprintList[1].id
	}

	@Test
	void testDelete() {
		mockSprintData()
		dummySprint1.addAdmin(user2.getId())
		assert dummySprint1.hasAdmin(user2.getId())
		assert dummySprint1.hasAdmin(user.getId())
		assert dummySprint1.hasMember(user2.getId())
		assert dummySprint1.hasMember(user.getId())
		assert dummySprint1.userId == userId
		
		Sprint.delete(dummySprint1)
		
		assert !dummySprint1.hasAdmin(user2.getId())
		assert !dummySprint1.hasAdmin(user.getId())
		assert !dummySprint1.hasMember(user2.getId())
		assert !dummySprint1.hasMember(user.getId())
		assert dummySprint1.userId == 0l
	}

	@Test
	void "Test getParticipants when sprint has no participant"() {
		mockSprintData()
		dummySprint1.removeMember(user.id)
		dummySprint1.removeMember(user2.id)
		int max = 5
		int offset = 0

		List participantsList = dummySprint1.getParticipants(max, offset)

		assert !participantsList
	}

	@Test
	void "Test getParticipants"() {
		mockSprintData()

		int max = 5
		int offset = 0
		List participantsList = dummySprint1.getParticipants(max, offset)

		assert participantsList.size() == 2
		assert participantsList[0].id == user.id
	}

	@Test
	void "Test getParticipants when offset is more than 0"() {
		mockSprintData()

		Map params = new HashMap()
		params.put("username", "testuser3")
		params.put("email", "test3@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first3")
		params.put("last", "last3")
		params.put("sex", "M")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1991")

		User user3 = User.create(params)
		Utils.save(user3, true)

		params.put("username", "testuser4")
		params.put("email", "test4@test.com")

		User user4 = User.create(params)
		Utils.save(user4, true)
		
		params.put("username", "testuser5")
		params.put("email", "test5@test.com")

		User user5 = User.create(params)
		Utils.save(user5, true)
	
		dummySprint1.addMember(user3.id)
		dummySprint1.addMember(user4.id)
		dummySprint1.addMember(user5.id)

		int max = 4
		int offset = 3
		List participantsList = dummySprint1.getParticipants(max, offset)

		assert participantsList.size() == 2
		def userIds = [user4.id, user5.id]
		assert participantsList[0].id in userIds
		assert participantsList[1].id in userIds
		assert participantsList[0].id != participantsList[1].id
	}

/**/
}

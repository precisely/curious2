package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec

import java.text.DateFormat
import java.util.concurrent.atomic.AtomicInteger
import java.util.Date;

import us.wearecurio.model.Entry
import us.wearecurio.model.Sprint
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User;
import us.wearecurio.model.UserActivity
import us.wearecurio.model.Model.Visibility;
import us.wearecurio.model.Tag
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils

class UserActivityIntegrationSpec extends IntegrationSpec {
	
	static transactional = true
	
	Date currentTime
	User user
	
	static AtomicInteger nameCount = new AtomicInteger(0)
	static String getUniqueName() {
		return "UserActivityIntegrationSpec name" + nameCount.getAndIncrement()
	}

    def setup() {
		user = User.create(
			[	username:'shane',
				sex:'F',
				name:'shane macgowen',
				email:'shane@pogues.com',
				birthdate:'01/01/1960',
				password:'shanexyz',
				action:'doregister',
				controller:'home'	]
		)

		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		def dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
    }
	
	def cleanup() {
		User.executeUpdate("delete User u")
	}
	
	//@spock.lang.Ignore
    void "test UserActivity created with new Sprint"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		
		when: "a UserActivity is found"
		def userActivity = UserActivity.findByUserId(user.id)
		
		then: "a UserActivity is also created"
		userActivity.userId == user.id
		userActivity.activityType == UserActivity.ActivityType.CREATE
		userActivity.objectType == UserActivity.ObjectType.SPRINT
		userActivity.objectId == sprint.id
		userActivity.otherType == null
		userActivity.otherId == null
    }
	
	//@spock.lang.Ignore
	void "test UserActivity created with deleted Sprint"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		
		when: "the sprint is deleted"
		Sprint.delete(sprint)
		
		and: "UserActivitys for user are found"
		def userActivities = UserActivity.findAllByUserId(user.id)
		
		and: "a DELETE activity type is included"
		def userActivity = userActivities.find{ it -> it.activityType == UserActivity.ActivityType.DELETE }
		
		then: "a UserActivity is created"
		userActivity.userId == user.id
		userActivity.activityType == UserActivity.ActivityType.DELETE
		userActivity.objectType == UserActivity.ObjectType.SPRINT
		userActivity.objectId == sprint.id
		userActivity.otherType == null
		userActivity.otherId == null
	}

	//@spock.lang.Ignore
	void "test UserActivity created with started Sprint"() {
		given: "a new sprint is created"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		
		when: "the sprint is started"
		Date baseDate = Utils.getStartOfDay(currentTime)
		sprint.start(user.id, baseDate,  currentTime, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate), new EntryStats())
		
		and: "UserActivitys for user are found"
		def userActivities = UserActivity.findAllByUserId(user.id)
		
		and: "a START activity type is included"
		def userActivity = userActivities.find{ it -> it.activityType == UserActivity.ActivityType.START }
		
		then: "a UserActivity is also created"
		userActivity.userId == user.id
		userActivity.activityType == UserActivity.ActivityType.START
		userActivity.objectType == UserActivity.ObjectType.SPRINT
		userActivity.objectId == sprint.id
		userActivity.otherType == null
		userActivity.otherId == null
		
		cleanup:
		sprint.stop(user.id, baseDate+1,  currentTime+1, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate+1), new EntryStats())
		Entry.executeUpdate("delete Entry e")
		Tag.executeUpdate("delete Tag t")
		Sprint.executeUpdate("delete Sprint s")
		synchronized(Tag.tagCache) {
			Tag.tagCache.clear()
			Tag.tagIdCache.clear()
		}
	}
	
	//@spock.lang.Ignore
	void "test UserActivity created with stopped Sprint"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		
		and: "the sprint is started"
		Date baseDate = Utils.getStartOfDay(currentTime)
		sprint.start(user.id, baseDate,  currentTime, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate), new EntryStats())
		
		when: "the sprint is stopped"
		sprint.stop(user.id, baseDate+1,  currentTime+1, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate+1), new EntryStats())
		
		and: "the user is queried for its UserActivity data" 
		def userActivities = UserActivity.findAllByUserId(user.id)
		
		and: "the results are queried for a STOP activity"
		def userActivity = userActivities.find{ it -> it.activityType == UserActivity.ActivityType.STOP }
		
		then: "a STOP UserActivity is found"
		userActivity != null
		userActivity.userId == user.id
		userActivity.activityType == UserActivity.ActivityType.STOP
		userActivity.objectType == UserActivity.ObjectType.SPRINT
		userActivity.objectId == sprint.id
		userActivity.otherType == null
		userActivity.otherId == null
		
		cleanup:
		Entry.executeUpdate("delete Entry e")
		Tag.executeUpdate("delete Tag t")
		Sprint.executeUpdate("delete Sprint s")
		synchronized(Tag.tagCache) {
			Tag.tagCache.clear()
			Tag.tagIdCache.clear()
		}
	}	
}

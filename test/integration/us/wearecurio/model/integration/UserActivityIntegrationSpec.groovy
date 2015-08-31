package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec

import java.text.DateFormat
import java.util.concurrent.atomic.AtomicInteger
import java.util.Date;

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Entry
import us.wearecurio.model.Sprint
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User;
import us.wearecurio.model.UserActivity
import us.wearecurio.model.UserActivity.ActivityType
import us.wearecurio.model.UserActivity.ObjectType
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
		//User.executeUpdate("delete User u")
		synchronized(Tag.tagCache) {
			Tag.tagCache.clear()
			Tag.tagIdCache.clear()
		}
	}
	
	UserActivity findUserActivity(ActivityType activityType, ObjectType objectType) {
		return UserActivity.getAll().find{
			it -> (
				(it.activityType == activityType) &&
				(it.objectType == objectType)
			)
		}
	}
	
	//@spock.lang.Ignore
    void "test CREATE UserActivity created when SPRINT created"() {
		when: "a sprint is created"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		
		then: "a CREATE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.SPRINT)
		userActivity 			!= 	null
		userActivity.userId 	== 	user.id
		userActivity.objectId 	== 	sprint.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
    }
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when SPRINT deleted"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		
		when: "the sprint is deleted"
		Sprint.delete(sprint)
		
		then: "a DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.SPRINT)
		userActivity 			!= 	null
		userActivity.userId 	== 	user.id
		userActivity.objectId 	== 	sprint.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}

	@spock.lang.Ignore
	void "test FOLLOW UserActivity created when USER follows SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test UNFOLLOW UserActivity created when USER unfollows SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test ADD UserActivity created when DISCUSSION added to SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test REMOVE UserActivity created when DISCUSSION removed from SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test ADD UserActivity created when ADMIN added to SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test REMOVE UserActivity created when ADMIN removed from SPRINT"() {
	}
		
	@spock.lang.Ignore
	void "test ADD UserActivity created when READER added to SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test REMOVE UserActivity created when READER removed from SPRINT"() {
	}
		
	//@spock.lang.Ignore
	void "test START UserActivity created when SPRINT started"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		
		when: "the sprint is started"
		Date baseDate = Utils.getStartOfDay(currentTime)
		sprint.start(user.id, baseDate,  currentTime, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate), new EntryStats())
		
		then: "a START UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.START, ObjectType.SPRINT)
		userActivity 			!= 	null
		userActivity.userId 	== 	user.id
		userActivity.objectId 	== 	sprint.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test STOP UserActivity created when SPRINT stopped"() {
		given: "a started sprint"
		def sprint = Sprint.create(currentTime, user, getUniqueName(), Visibility.PUBLIC)
		Date baseDate = Utils.getStartOfDay(currentTime)
		sprint.start(user.id, baseDate,  currentTime, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate), new EntryStats())
		
		when: "the sprint is stopped"
		sprint.stop(user.id, baseDate+1,  currentTime+1, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate+1), new EntryStats())
		
		then: "a STOP UserActivity is created" 
		UserActivity userActivity = findUserActivity(ActivityType.STOP, ObjectType.SPRINT)
		userActivity 			!= 	null
		userActivity.userId 	== 	user.id
		userActivity.objectId 	== 	sprint.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	@spock.lang.Ignore
	void "test INVITE UserActivity created when USER invited to SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test UNINVITE UserActivity created when USER uninvited to SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test INVITE UserActivity created when ADMIN invited to SPRINT"() {
	}
	
	@spock.lang.Ignore
	void "test UNINVITE UserActivity created when ADMIN uninvited from SPRINT"() {
	}
	
	//@spock.lang.Ignore
	void "test CREATE UserActivity created when DISCUSSION_POST created"() {
		given: "a new discussion"
		def discussion = Discussion.create(user, getUniqueName())

		when: "a discussion post is creqted"
		def post = discussion.createPost(user, getUniqueName())
		
		then: "a CREATE UserActivity is created for the DISCUSSION_POST"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.DISCUSSION_POST)
		userActivity 			!= 	null
		userActivity.userId 	== 	user.id
		userActivity.objectId 	== 	post.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when DISCUSSION_POST deleted"() {
		given: "a new discussion post"
		def discussion = Discussion.create(user, getUniqueName())
		def post = discussion.createPost(user, getUniqueName())

		when: "the post is deleted"
		DiscussionPost.delete(post)

		def userActivities = UserActivity.getAll()
		if (userActivities == null) {
			println "userActivities are null"
		} else {
			for (def a : userActivities) {
				println "userActivity: " + a.activityType + ", " + a.objectType
			}
		}
		
		then: "a new DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.DISCUSSION_POST)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.objectId 	== 	post.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	@spock.lang.Ignore
	void "test COMMENT UserActivity created when DISCUSSION_POST added to DISCUSSION"() {
	}
	
	@spock.lang.Ignore
	void "test UNCOMMENT UserActivity created when DISCUSSION_POST removed from DISCUSSION"() {
	}
	
	//@spock.lang.Ignore
	void "test CREATE UserActivity created when DISCUSSION created"() {
		when: "a dicussion is created"
		def discussion = Discussion.create(user, getUniqueName())
		
		then: "a CREATE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.DISCUSSION)
		userActivity 			!= 	null
		userActivity.userId 	== 	user.id
		userActivity.objectId 	== 	discussion.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when DISCUSSION deleted"() {
		given: "a new discussion"
		def discussion = Discussion.create(user, getUniqueName())
		
		when: "the discussion is deleted"
		Discussion.delete(discussion)
		
		then: "a DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.DISCUSSION)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.objectId 	== 	discussion.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	@spock.lang.Ignore
	void "test ADD UserActivity created when ADMIN added to DISCUSSION"() {
	}
	
	@spock.lang.Ignore
	void "test REMOVE UserActivity created when ADMIN removed from DISCUSSION"() {
	}
	
	@spock.lang.Ignore
	void "test ADD UserActivity created when READER added to DISCUSSION"() {
	}
	
	@spock.lang.Ignore
	void "test REMOVE UserActivity created when READER removed from DISCUSSION"() {
	}
		
	//@spock.lang.Ignore
	void "test CREATE UserActivity created when USER created"() {
		when: "a user is created"
		//user created in class setup
		
		then: "a CREATE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.USER)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.objectId 	== 	user.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when USER deleted"() {
		when: "the user created in class setup is deleted"
		User.delete(user)
		
		then: "a DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.USER)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.objectId 	== 	user.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	@spock.lang.Ignore
	void "test FOLLOW UserActivity is created when USER follows USER"() {
	}
	
	@spock.lang.Ignore
	void "test FOLLOW UserActivity is created when USER unfollows USER"() {
	}

}

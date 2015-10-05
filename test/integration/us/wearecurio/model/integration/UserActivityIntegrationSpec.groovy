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
	User user1
	User user2
	User user3
	
	static AtomicInteger nameCount = new AtomicInteger(0)
	static String getUniqueName() {
		return "UserActivityIntegrationSpec name" + nameCount.getAndIncrement()
	}

    def setup() {
		user1 = User.create(
			[	username:'shane',
				sex:'F',
				name:'shane macgowen',
				email:'shane@pogues.com',
				birthdate:'01/01/1960',
				password:'shanexyz',
				action:'doregister',
				controller:'home'	]
		)
		user2 = User.create(
			[	username:'spider',
				sex:'F',
				name:'spider stacy',
				email:'spider@pogues.com',
				birthdate:'01/01/1961',
				password:'spiderxyz',
				action:'doregister',
				controller:'home'	]
		)
		user3 = User.create(
			[	username:'jem',
				sex:'F',
				name:'jem finer',
				email:'jem@pogues.com',
				birthdate:'01/01/1963',
				password:'jemxyz',
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
	
	UserActivity findUserActivity(ActivityType activityType, ObjectType objectType, Long objectId = null) {
		return UserActivity.getAll().find{
			it -> (
				(it.activityType == activityType) &&
				(it.objectType == objectType) &&
				(objectId == null || it.objectId == objectId)
			)
		}
	}
	
	//@spock.lang.Ignore
    void "test CREATE UserActivity created when SPRINT created"() {
		when: "a sprint is created"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		then: "a CREATE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.SPRINT, sprint.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
    }
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when SPRINT deleted"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "the sprint is deleted"
		Sprint.delete(sprint)
		
		then: "a DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.SPRINT, sprint.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}

	//@spock.lang.Ignore
	void "test FOLLOW UserActivity created when USER follows SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "a user follows sprint"
		sprint.addReader(user1.id)
		
		then: "a FOLLOW UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.FOLLOW, ObjectType.SPRINT, sprint.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	ObjectType.USER
		userActivity.otherId	==	user1.id
	}
	
	//@spock.lang.Ignore
	void "test UNFOLLOW UserActivity created when USER unfollows SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "a sprint reader"
		sprint.addReader(user1.id)
		
		when: "the sprint reader is removed"
		sprint.removeReader(user1.id)
		
		then: "an UNFOLLOW UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.UNFOLLOW, ObjectType.SPRINT, sprint.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	ObjectType.USER
		userActivity.otherId	==	user1.id
	}
	
	//@spock.lang.Ignore
	void "test ADD UserActivity created when DISCUSSION added to SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "a new discussion"
		def discussion = Discussion.create(user2, getUniqueName())
		
		when: "the discussion is added to sprint"
		sprint.addDiscussion(discussion)
		
		then: "an ADD UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.ADD, ObjectType.DISCUSSION, discussion.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
	
	//@spock.lang.Ignore
	void "test REMOVE UserActivity created when DISCUSSION removed from SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())

		and: "the discussion is added to sprint"				
		sprint.addDiscussion(discussion)
		
		when: "the discussion is removed from sprint"
		sprint.removeDiscussion(discussion)
		
		then: "an REMOVE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.REMOVE, ObjectType.DISCUSSION, discussion.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
	
	//@spock.lang.Ignore
	void "test ADD UserActivity created when ADMIN added to SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "a sprint admin is added"
		sprint.addAdmin(user2.id)
		
		then: "an ADD UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.ADD, ObjectType.ADMIN, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user2.id
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
	
	//@spock.lang.Ignore
	void "test REMOVE UserActivity created when ADMIN removed from SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "a sprint admin is added"
		sprint.addAdmin(user2.id)
		
		when: "the sprint admin is removed"
		sprint.removeAdmin(user2.id)

		then: "a REMOVE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.REMOVE, ObjectType.ADMIN, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user2.id
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
		
	//@spock.lang.Ignore
	void "test ADD UserActivity created when READER added to SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "a sprint reader is added"
		sprint.addReader(user2.id)
		
		then: "an ADD UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.ADD, ObjectType.READER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user2.id
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
	
	//@spock.lang.Ignore
	void "test REMOVE UserActivity created when READER removed from SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "a sprint reader is added"
		sprint.addReader(user2.id)
		
		when: "the sprint reader is removed"
		sprint.removeReader(user2.id)

		then: "a REMOVE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.REMOVE, ObjectType.READER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user2.id
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
		
	//@spock.lang.Ignore
	void "test START UserActivity created when SPRINT started"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "the sprint is started"
		Date baseDate = Utils.getStartOfDay(currentTime)
		sprint.start(user1.id, baseDate,  currentTime, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate), new EntryStats())
		
		then: "a START UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.START, ObjectType.SPRINT, sprint.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test STOP UserActivity created when SPRINT stopped"() {
		given: "a started sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		Date baseDate = Utils.getStartOfDay(currentTime)
		sprint.start(user1.id, baseDate,  currentTime, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate), new EntryStats())
		
		when: "the sprint is stopped"
		sprint.stop(user1.id, baseDate+1,  currentTime+1, TimeZoneId.guessTimeZoneNameFromBaseDate(baseDate+1), new EntryStats())
		
		then: "a STOP UserActivity is created" 
		UserActivity userActivity = findUserActivity(ActivityType.STOP, ObjectType.SPRINT, sprint.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test INVITE UserActivity created when USER invited to SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "another user is invited to sprint"
		sprint.addInvited(user2.id)
		
		then: "an INVITE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.INVITE, ObjectType.USER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
	
	//@spock.lang.Ignore
	void "test UNINVITE UserActivity created when USER uninvited to SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "another user is invited to sprint"
		sprint.addInvited(user2.id)
		
		when: "the other user is uninvited from sprint"
		sprint.removeInvited(user2.id)
		
		then: "an UNINVITE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.UNINVITE, ObjectType.USER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}

	//@spock.lang.Ignore
	void "test INVITE UserActivity created when ADMIN invited to SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "admin is invited to sprint"
		sprint.addInvitedAdmin(user2.id)
		
		then: "an INVITE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.INVITE, ObjectType.ADMIN, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
	
	//@spock.lang.Ignore
	void "test UNINVITE UserActivity created when ADMIN uninvited from SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "admin is invited to sprint"
		sprint.addInvitedAdmin(user2.id)
		
		when: "the admin is uninvited from sprint"
		sprint.removeInvitedAdmin(user2.id)
		
		then: "an UNINVITE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.UNINVITE, ObjectType.ADMIN, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.SPRINT
		userActivity.otherId	==	sprint.id
	}
	
	void "test UNINVITE UserActivitys created when clearInvited called on SPRINT"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "another user is invited to sprint"
		sprint.addInvited(user2.id)
		
		and: "admin is invited to sprint"
		sprint.addInvitedAdmin(user3.id)
		
		when: "clearInvited is called on the sprint"
		sprint.clearInvited()
		
		then: "an UNINVITE UserActivity is created for USER"
		UserActivity userActivityUser = findUserActivity(ActivityType.UNINVITE, ObjectType.USER, user2.id)
		userActivityUser 			!= 	null
		userActivityUser.userId 	== 	null
		userActivityUser.otherType 	== 	ObjectType.SPRINT
		userActivityUser.otherId	==	sprint.id

		and: "an UNINVITE UserActivity is created for ADMIN"
		UserActivity userActivityAdmin = findUserActivity(ActivityType.UNINVITE, ObjectType.ADMIN, user3.id)
		userActivityAdmin 			!= 	null
		userActivityAdmin.userId 	== 	null
		userActivityAdmin.otherType == 	ObjectType.SPRINT
		userActivityAdmin.otherId	==	sprint.id
	}
	
	//@spock.lang.Ignore
	void "test CREATE UserActivity created when DISCUSSION_POST created"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())

		when: "a discussion post is created"
		def post = discussion.createPost(user1, getUniqueName())
		
		then: "a CREATE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.DISCUSSION_POST, post.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when DISCUSSION_POST deleted"() {
		given: "a new discussion post"
		def discussion = Discussion.create(user1, getUniqueName())
		def post = discussion.createPost(user2, getUniqueName())

		when: "the post is deleted"
		DiscussionPost.delete(post)
		
		then: "a DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.DISCUSSION_POST, post.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test COMMENT UserActivity created when DISCUSSION_POST added to DISCUSSION"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())

		when: "a discussion post is created"
		def post = discussion.createPost(user2, getUniqueName())
		
		then: "a COMMENT UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.COMMENT, ObjectType.DISCUSSION, discussion.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user2.id
		userActivity.otherType 	== 	ObjectType.DISCUSSION_POST
		userActivity.otherId	==	post.id
	}
	
	//@spock.lang.Ignore
	void "test UNCOMMENT UserActivity created when DISCUSSION_POST removed from DISCUSSION"() {
		given: "a new discussion post"
		def discussion = Discussion.create(user1, getUniqueName())
		def post = discussion.createPost(user2, getUniqueName())

		when: "the post is deleted"
		DiscussionPost.delete(post)
		
		then: "an UNCOMMENT UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.UNCOMMENT, ObjectType.DISCUSSION, discussion.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.DISCUSSION_POST
		userActivity.otherId	==	post.id
	}
	
	//@spock.lang.Ignore
	void "test CREATE UserActivity created when DISCUSSION created"() {
		when: "a dicussion is created"
		def discussion = Discussion.create(user1, getUniqueName())
		
		then: "a CREATE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.DISCUSSION, discussion.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user1.id
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when DISCUSSION deleted"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "the discussion is deleted"
		Discussion.delete(discussion)
		
		then: "a DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.DISCUSSION, discussion.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test ADD UserActivity created when ADMIN added to DISCUSSION"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "a discussion admin is added"
		def groups = discussion.getGroups()
		if (groups != null) {
			for(def group : groups) {
				group.addAdmin(user2)
			}
		}
		
		then: "an ADD UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.ADD, ObjectType.ADMIN, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.DISCUSSION
		userActivity.otherId	==	discussion.id
	}
	
	//@spock.lang.Ignore
	void "test REMOVE UserActivity created when ADMIN removed from DISCUSSION"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		
		and: "a discussion admin is added"
		def groups = discussion.getGroups()
		if (groups != null) {
			for(def group : groups) {
				group.addAdmin(user2)
			}
		}
		
		when: "the discussion admin is removed"
		if (groups != null) {
			for(def group : groups) {
				group.removeAdmin(user2)
			}
		}

		then: "a REMOVE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.REMOVE, ObjectType.ADMIN, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.DISCUSSION
		userActivity.otherId	==	discussion.id
	}
	
	//@spock.lang.Ignore
	void "test ADD UserActivity created when READER added to DISCUSSION"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "a discussion reader is added"
		def groups = discussion.getGroups()
		if (groups != null) {
			for(def group : groups) {
				group.addReader(user2)
			}
		}
		
		then: "an ADD UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.ADD, ObjectType.READER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.DISCUSSION
		userActivity.otherId	==	discussion.id
	}
	
	//@spock.lang.Ignore
	void "test REMOVE UserActivity created when READER removed from DISCUSSION"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		
		and: "a discussion reader is added"
		def groups = discussion.getGroups()
		if (groups != null) {
			for(def group : groups) {
				group.addReader(user2)
			}
		}
		
		when: "the discussion reader is removed"
		if (groups != null) {
			for(def group : groups) {
				group.removeReader(user2)
			}
		}

		then: "a REMOVE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.REMOVE, ObjectType.READER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	ObjectType.DISCUSSION
		userActivity.otherId	==	discussion.id
	}
		
	//@spock.lang.Ignore
	void "test CREATE UserActivity created when USER created"() {
		when: "a user is created"
		//user created in class setup
		
		then: "a CREATE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.CREATE, ObjectType.USER, user1.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test DELETE UserActivity created when USER deleted"() {
		when: "a user created in class setup is deleted"
		User.delete(user1)
		
		then: "a DELETE UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.DELETE, ObjectType.USER, user1.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	null
		userActivity.otherType 	== 	null
		userActivity.otherId	==	null
	}
	
	//@spock.lang.Ignore
	void "test FOLLOW UserActivity is created when USER follows USER"() {
		when: "one user follows another"
		user1.follow(user2)
		
		then: "a FOLLOW UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.FOLLOW, ObjectType.USER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user2.id
		userActivity.otherType 	== 	ObjectType.USER
		userActivity.otherId	==	user1.id
	}
	
	//@spock.lang.Ignore
	void "test UNFOLLOW UserActivity is created when USER unfollows USER"() {
		given: "one user following another"
		user1.follow(user2)
		
		when: "user unfollows other user"
		user1.unFollow(user2)
		
		then: "a FOLLOW UserActivity is created"
		UserActivity userActivity = findUserActivity(ActivityType.UNFOLLOW, ObjectType.USER, user2.id)
		userActivity 			!= 	null
		userActivity.userId 	== 	user2.id
		userActivity.otherId	==	user1.id
	}
}

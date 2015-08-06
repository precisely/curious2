package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.GroupMemberDiscussion
import us.wearecurio.model.GroupMemberReader
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils

class UserIntegrationSpec extends IntegrationSpec {

	def user1Params = [username:'shane', sex:'F', \
			name:'shane macgowen', email:'shane@pogues.com', birthdate:'01/01/1960', \
			password:'shanexyz', action:'doregister', \
			controller:'home']
	def user2Params = [username:'spider', sex:'F', \
			name:'spider stacy', email:'spider@pogues.com', birthdate:'01/01/1961', \
			password:'spiderxyz', action:'doregister', \
			controller:'home']
	
    def setup() {
		
    }

    def cleanup() {
    }

    void "Saving a new user"() {
		when: "given a newly created user"
		def shane = User.create(user1Params)
		
		then: "user is saved successfully and can be found in the database"
		User.get(shane.id).username == shane.username
    }
	
	void "Virtual user group is created for followers"() {
		when: "given a newly created user"
		def shane = User.create(user1Params)
		
		then: "user's virtual UserGroup for followers is successfully created and can be found in the database"
		def forFollowers = UserGroup.get(shane.virtualUserGroupIdFollowers)
		forFollowers.isVirtual
	}

	void "Virtual user created for discussions"() {
		when: "given a newly created user"
		def shane = User.create(user1Params)
		
		then: "user's virtual UserGroup for discussions is successfully created and can be found in the database"
		def forDiscussions = UserGroup.get(shane.virtualUserGroupIdDiscussions)
		forDiscussions.isVirtual
	}

	void "User can be followed"() {
		given: "users, shane and spider"
		def shane = User.create(user1Params)
		def spider = User.create(user2Params)
		
		when: "spider follows shane"
		shane.follow(spider)
		Utils.save(shane, true)
		
		then: "in the database, spider is a reader of shane's virtual followers group"
		GroupMemberReader.lookup(shane.virtualUserGroupIdFollowers, spider.id)
	}
	
	void "User can be unfollowed"() {
		given: "users, shane and spider"
		def shane = User.create(user1Params)
		def spider = User.create(user2Params)
		
		when: "spider follows shane"
		shane.follow(spider)
		Utils.save(shane, true)
		
		then: "in the database, spider is a reader of shane's virtual followers group"
		GroupMemberReader.lookup(shane.virtualUserGroupIdFollowers, spider.id)
		
		when: "spider stops following shane"
		shane.unFollow(spider)
		Utils.save(shane, true)
		
		then: "in the database, spider is not a reader of shane's virtual followers group"
		GroupMemberReader.lookup(shane.virtualUserGroupIdFollowers, spider.id) == null
	}
	
	void "User does not automatically follow themselves"() {
		when: "given a newly created user"
		def shane = User.create(user1Params)
		
		then: "in the database, user is not reader of its own virtual followers group"
		GroupMemberReader.lookup(shane.virtualUserGroupIdFollowers, shane.id) == null
	}
	
	void "Users can follow each other"() {
		given: "users, shane and spider"
		def shane = User.create(user1Params)
		def spider = User.create(user2Params)
		
		when: "spider follows shane and shane follows spider"
		shane.follow(spider)
		Utils.save(shane, true)
		spider.follow(shane)
		Utils.save(spider, true)
		
		then: "in the database, spider and shane are readers of each other's virtual followers group"
		GroupMemberReader.lookup(shane.virtualUserGroupIdFollowers, spider.id)
		GroupMemberReader.lookup(spider.virtualUserGroupIdFollowers, shane.id)
	}

	void "User can read discussion that it created"() {
		given: "A newly created user"
		def shane = User.create(user1Params)
		
		when: "a discussion is created by user"
		def discussion = Discussion.create(shane)
		
		then: "user is reader of virtual group that is member of discussion group and the information is in the database"
		GroupMemberReader.lookup(shane.virtualUserGroupIdDiscussions, shane.id)
		GroupMemberDiscussion.lookup(shane.virtualUserGroupIdDiscussions, discussion.id)		
	}
}

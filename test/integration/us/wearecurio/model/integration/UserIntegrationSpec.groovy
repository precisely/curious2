package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
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
	
//	void "Virtual user group is created for followers"() {
//		when: "given a newly created user"
//		def shane = User.create(user1Params)
//		
//		then: "user's virtual UserGroup for followers is successfully created and can be found in the database"
//		def forFollowers = UserGroup.get(shane.virtualUserGroupIdFollowers)
//		forFollowers.isVirtual
//	}
//
//	void "Virtual user created for discussions"() {
//		when: "given a newly created user"
//		def shane = User.create(user1Params)
//		
//		then: "user's virtual UserGroup for discussions is successfully created and can be found in the database"
//		def forDiscussions = UserGroup.get(shane.virtualUserGroupIdDiscussions)
//		forDiscussions.isVirtual
//	}
//
//	void "User can be followed"() {
//		given: "users, shane and spider"
//		def shane = User.create(user1Params)
//		def spider = User.create(user2Params)
//		
//		when: "spider follows shane"
//		shane.FollowMe(spider)
//		Utils.save(shane, true)
//		
//		then: "the following relationship is stored in the database"
//		UserGroup.get(User.get(shane.id).virtualUserGroupIdFollowers).find{ it -> it.id == spider.id }
//	}
//	
//	void "Users can follow each other"() {
//		given: "users, shane and spider"
//		def shane = User.create(user1Params)
//		def spider = User.create(user2Params)
//		
//		when: "spider follows shane and shane follows spider"
//		shane.FollowMe(spider)
//		Utils.save(shane, true)
//		spider.FollowMe(shane)
//		Utils.save(spider, true)
//		
//		then: "both following relationships are stored in the database"
//		UserGroup.get(User.get(shane.id).virtualUserGroupIdFollowers).find{ it -> it.id == spider.id }
//		UserGroup.get(User.get(spider.id).virtualUserGroupIdFollowers).find{ it -> it.id == shane.id }
//	}
//
//	void "User's virtual group can read discussion created by user"() {
//		given: "A newly created user"
//		def shane = User.create(user1Params)
//		
//		when: "a discussion is created by user"
//		def discussion = Discussion.create(shane)
//		
//		then: "the user's virtual group has reader permissions for the discussion and the information is in the database"
//		GroupMemberReader.lookupGroupIds(shane.id).find{ it -> it.memberId == discussion.id && it.groupId == shane.virtualUserGroupIdDiscussions }
//	}
}

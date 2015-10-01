package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec
import groovy.lang.Closure;
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.GroupMemberDiscussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.model.UserActivity
import us.wearecurio.utility.Utils

class GetDiscussionActivityIntegrationSpec extends SearchServiceIntegrationSpecBase {
	static Closure addAdmin = {
		discussion, admin, user = null ->
			def userGroup = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(), [isOpen : true])
			userGroup.addWriter(discussion.userId)
			userGroup.addDiscussion(discussion)
			userGroup.addAdmin(admin)
			userGroup
	}
	
	static Closure addReader = {
		discussion, reader, user = null ->
			def userGroup = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(), [isOpen : true])
			userGroup.addWriter(discussion.userId)
			userGroup.addDiscussion(discussion)
			userGroup.addReader(reader)
			userGroup
	}
	
	static Closure addFollower = {
		discussion = null, follower, user ->
			user.follow(follower)
			user
	}
	
	static Closure addSprintReader = {
		discussion, reader, user ->
			def sprint = Sprint.create(user)
			sprint.addReader(reader.id)
			sprint.fetchUserGroup()?.addWriter(discussion.userId)
			sprint.fetchUserGroup()?.addDiscussion(discussion)
			sprint
	}
	
	static Closure addUserTags = {
		discussion, unrelated, user = null ->
			//def userGroup = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(), null)
			def tag = Tag.create(discussion.name)
			unrelated.addInterestTag(tag)
			tag
	}
	
	static Closure removeFollower = {
		follower, user ->
			user.unFollow(follower)
	}
	
	static Closure removeSprintReader = {
		reader, sprint ->
			sprint.removeReader(reader.id)
	}
	
	static Closure removeUserTags = {
		unrelated, tag ->
			unrelated.deleteInterestTag(tag)
	}
	
	static Closure removeSprintDiscussion = {
		user = null, sprint ->
			def discussionIds = GroupMemberDiscussion.lookupMemberIds(sprint.virtualGroupId)
			for (Long discussionId in discussionIds) {
				GroupMemberDiscussion.delete(sprint.virtualGroupId, discussionId)
			}
	}

	static def data = [
		//		visibility	otherUser	expectedResult					otherUserCode	
		[Visibility.PUBLIC 	, 	1	, 	"self sees"						,	{d, u2, u1 ->}],
		[Visibility.PRIVATE	, 	1	,	"self sees"						,	{d, u2, u1 ->}],
		[Visibility.UNLISTED,	1	,	"self sees"						, 	{d, u2, u1 ->}],
		[Visibility.NEW		, 	1	,	"self sees"						,	{d, u2, u1 ->}],
		[Visibility.PUBLIC 	, 	2	, 	"admin sees"					,	addAdmin],
		[Visibility.PRIVATE	, 	2	,	"admin does not see"			,	addAdmin],
		[Visibility.UNLISTED,	2	,	"admin sees"					,	addAdmin],
		[Visibility.NEW		, 	2	,	"admin sees"					,	addAdmin],
		[Visibility.PUBLIC 	, 	2	, 	"reader sees"					,	addReader],
		[Visibility.PRIVATE	, 	2	,	"reader does not see"			,	addReader],
		[Visibility.UNLISTED,	2	,	"reader sees"					,	addReader],
		[Visibility.NEW		, 	2	,	"reader does not see"			,	addReader],
		[Visibility.PUBLIC 	, 	2	, 	"user follower sees"			,	addFollower],
		[Visibility.PRIVATE	, 	2	,	"user follower does not see"	,	addFollower],
		[Visibility.UNLISTED,	2	,	"user follower does not see"	,	addFollower],
		[Visibility.NEW		, 	2	,	"user follower does not see"	,	addFollower],
		[Visibility.PUBLIC 	, 	2	, 	"sprint reader sees"			,	addSprintReader],
		[Visibility.PRIVATE	, 	2	,	"sprint reader does not see"	,	addSprintReader],
		[Visibility.UNLISTED,	2	,	"sprint reader does not see"	,	addSprintReader],
		[Visibility.NEW		, 	2	,	"sprint reader does not see"	,	addSprintReader],
		[Visibility.PUBLIC 	, 	2	, 	"tags user sees"				,	addUserTags],
		[Visibility.PRIVATE	, 	2	,	"tags user does not see"		,	addUserTags],
		[Visibility.UNLISTED,	2	,	"tags user does not see"		,	addUserTags],
		[Visibility.NEW		, 	2	,	"tags user does not see"		,	addUserTags]
	]
	
	//@spock.lang.Ignore
	void "Test getDiscussionActivity"() {
		given: "a new user group"
		UserGroup groupA = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(),
			[isReadOnly:false, defaultNotify:false])
		groupA.addAdmin(user1)
		groupA.addAdmin(user2)
		
		and: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName(), groupA)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getActivity for other user returns 0 or 1 discussion created activities"
		def results = searchService.getDiscussionActivity(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].userId == user1.id
		results.listItems[0].userName == user1.name
		results.listItems[0].type == "act-created-dis"
		results.listItems[0].objectId == discussion.id
		results.listItems[0].objectDescription == discussion.name
		results.listItems[0].otherId == null
		results.listItems[0].otherDescription == null
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility discussion created activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility
		
		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)
	
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "getActivity for other user returns 0 or 1 discussion created activities"
		def results = searchService.getDiscussionActivity(other)
		results.success
		expectedSize == 0 || results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].userId == user1.id
		expectedSize == 0 || results.listItems[0].userName == user1.name
		expectedSize == 0 || results.listItems[0].type == "act-created-dis"
		expectedSize == 0 || results.listItems[0].objectId == discussion.id
		expectedSize == 0 || results.listItems[0].objectDescription == discussion.name
		expectedSize == 0 || results.listItems[0].otherId == null
		expectedSize == 0 || results.listItems[0].otherDescription == null

		and: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.CREATE, 
				UserActivity.ObjectType.DISCUSSION), 
			discussion.id
		)
		ua.activityType == UserActivity.ActivityType.CREATE
		ua.objectType == UserActivity.ObjectType.DISCUSSION
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == discussion.groupIds
		ua.objectDescription == discussion.name
		ua.objectUserId == discussion.userId
		ua.objectVisibility == discussion.visibility
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility discussion deleted activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "the discussion is deleted"
		Discussion.delete(discussion)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.DELETE,
				UserActivity.ObjectType.DISCUSSION),
			discussion.id
		)
		ua.activityType == UserActivity.ActivityType.DELETE
		ua.objectType == UserActivity.ObjectType.DISCUSSION
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == null
		ua.objectUserId == null
		ua.objectVisibility == null

		and: "getActivity for other user returns 0 discussion deleted activities for user"
		def results = searchService.getDiscussionActivity(other)
		results.success
		results.listItems.size() == 0
				
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult admin added to #visibility discussion activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)

		when: "an admin is added"
		addAdmin(discussion, user3)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "getActivity for other user returns 0 or 1 admin added to discussion activities at top of list"
		def results = searchService.getDiscussionActivity(other)
		results.success
		results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].type == "act-dis-admin-add"
		expectedSize == 0 || results.listItems[0].activity == user3.name + " added as admin to discussion '" + discussion.name + "'"
		expectedSize == 0 || results.listItems[0].id == discussion.id
		expectedSize == 0 || results.listItems[0].name == discussion.name
		expectedSize == 0 || results.listItems[0].activityDetail == null || results.listItems[0].activityDetail.trim().isEmpty()
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult admin removed from #visibility discussion activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)

		when: "an admin is added"
		def group = addAdmin(discussion, user3)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "an admin is removed"
		group.removeAdmin(user3.id)
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "getActivity for other user returns 0 or 1 admin removed from discussion activities at top of list"
		def results = searchService.getDiscussionActivity(other)
		results.success
		results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].type == "act-dis-admin-del"
		expectedSize == 0 || results.listItems[0].activity == user3.name + " removed as admin to discussion '" + discussion.name + "'"
		expectedSize == 0 || results.listItems[0].id == discussion.id
		expectedSize == 0 || results.listItems[0].name == discussion.name
		expectedSize == 0 || results.listItems[0].activityDetail == null || results.listItems[0].activityDetail.trim().isEmpty()
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult reader added to #visibility discussion activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)

		when: "an reader is added"
		addReader(discussion, user3)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "getActivity for other user returns 0 or 1 reader added to discussion activities at top of list"
		def results = searchService.getDiscussionActivity(other)
		results.success
		results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].type == "act-dis-reader-add"
		expectedSize == 0 || results.listItems[0].activity == user3.name + " added as reader to discussion '" + discussion.name + "'"
		expectedSize == 0 || results.listItems[0].id == discussion.id
		expectedSize == 0 || results.listItems[0].name == discussion.name
		expectedSize == 0 || results.listItems[0].activityDetail == null || results.listItems[0].activityDetail.trim().isEmpty()
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult reader removed from #visibility discussion activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)

		when: "an reader is added"
		def group = addReader(discussion, user3)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "an reader is removed"
		group.removeReader(user3.id)
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "getActivity for other user returns 0 or 1 reader removed from discussion activities at top of list"
		def results = searchService.getDiscussionActivity(other)
		results.success
		results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].type == "act-dis-reader-del"
		expectedSize == 0 || results.listItems[0].activity == user3.name + " removed as reader to discussion '" + discussion.name + "'"
		expectedSize == 0 || results.listItems[0].id == discussion.id
		expectedSize == 0 || results.listItems[0].name == discussion.name
		expectedSize == 0 || results.listItems[0].activityDetail == null || results.listItems[0].activityDetail.trim().isEmpty()
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult post added to #visibility discussion activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)

		when: "a post is added"
		def post = discussion.createPost(user1, getUniqueName())
//		println ""
//		println "other.id: " + other.id
//		println "user1.id: " + user1.id
//		println "user1.virtualUserGroupIdDiscussions: " + user1.virtualUserGroupIdDiscussions
//		println "user1.virtualUserGroupIdFollowers: " + user1.virtualUserGroupIdFollowers
//		println "user2.id: " + user2.id
//		println "user2.virtualUserGroupIdDiscussions: " + user2.virtualUserGroupIdDiscussions
//		println "user2.virtualUserGroupIdFollowers: " + user2.virtualUserGroupIdFollowers
//		println ""
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected result determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		and: "getActivity for other user returns 0 or 1 post added to discussion activities at top of list"
		def results = searchService.getDiscussionActivity(other)
		
		then: "result is successful"
		results.success
		
		and: "discussion post is found"
		def found = results.listItems.find {
			it.objectId == discussion.id && it.type == "act-commented-dis-post"
		}
		(expectedSize > 0 && found != null) || (expectedSize == 0 && found == null)
		found == null || found.userId == user1.id
		found == null || found.userName == user1.name
		found == null || found.objectDescription == discussion.name
		found == null || found.otherId == post.id
		found == null || found.otherDescription == post.message
				
		and: "userActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectIdAndOtherId(
			UserActivity.toType(
				UserActivity.ActivityType.COMMENT, 
				UserActivity.ObjectType.DISCUSSION,
				UserActivity.ObjectType.DISCUSSION_POST), 
			discussion.id,
			post.id
		)
		ua.activityType == UserActivity.ActivityType.COMMENT
		ua.objectType == UserActivity.ObjectType.DISCUSSION
		ua.otherType == UserActivity.ObjectType.DISCUSSION_POST
		ua.discussionGroupIds == discussion.groupIds
		ua.objectDescription == discussion.name
		ua.objectUserId == discussion.userId
		ua.objectVisibility == discussion.visibility
//		expectedSize == 0 || results.listItems[0].type == "act-dis-post"
//		expectedSize == 0 || results.listItems[0].id == discussion.id
//		expectedSize == 0 || results.listItems[0].name == discussion.name
//
//		expectedSize == 0 || results.listItems.size() == expectedSize
//		expectedSize == 0 || results.listItems[0].userId == user1.id
//		expectedSize == 0 || results.listItems[0].userName == user1.name
//		expectedSize == 0 || results.listItems[0].type == "act-created-dis"
//		expectedSize == 0 || results.listItems[0].objectId == discussion.id
//		expectedSize == 0 || results.listItems[0].objectDescription == discussion.name
//		expectedSize == 0 || results.listItems[0].otherId == null
//		expectedSize == 0 || results.listItems[0].otherDescription == null

		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
		
	@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult post removed from #visibility discussion activity"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)

		when: "a post is added"
		def post = discussion.createPost(user1, getUniqueName())
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "discussion has post"
		DiscussionPost.get(post.id).discussionId == discussion.id
		
		when: "post is removed"
		DiscussionPost.delete(post)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
				
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectIdAndOtherId(
			UserActivity.toType(
				UserActivity.ActivityType.UNCOMMENT, 
				UserActivity.ObjectType.DISCUSSION,
				UserActivity.ObjectType.DISCUSSION_POST), 
			discussion.id,
			post.id
		)
		ua.activityType == UserActivity.ActivityType.COMMENT
		ua.objectType == UserActivity.ObjectType.DISCUSSION
		ua.otherType == UserActivity.ObjectType.DISCUSSION_POST
		ua.discussionGroupIds == discussion.groupIds
		ua.objectDescription == discussion.name
		ua.objectUserId == discussion.userId
		ua.objectVisibility == discussion.visibility
		
//		and: "getActivity for other user returns 0 or 1 post removed from discussion activities at top of list"
//		def results = searchService.getDiscussionActivity(other)
//		results.success
//		results.listItems.size() == expectedSize
//		expectedSize == 0 || results.listItems[0].type == "act-dis-post-del"
//		expectedSize == 0 || results.listItems[0].activity == "A posted message was removed from discussion '" + discussion.name + "'"
//		expectedSize == 0 || results.listItems[0].id == discussion.id
//		expectedSize == 0 || results.listItems[0].name == discussion.name
//		expectedSize == 0 || results.listItems[0].activityDetail == post.message
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #followType does not see post to #visibility discussion activity after follow relationship removed"() {
		given: "a new discussion"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility

		and: "follow relationship added"
		def unFollowObj = followCode(discussion, user2, user1)

		when: "a post is added"
		def post = discussion.createPost(user1, getUniqueName())
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "discussion has post"
		DiscussionPost.get(post.id).discussionId == discussion.id
		
		when: "follow relationship removed"
		unFollowCode(user2, unFollowObj)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
				
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectIdAndOtherId(
			UserActivity.toType(
				UserActivity.ActivityType.COMMENT, 
				UserActivity.ObjectType.DISCUSSION,
				UserActivity.ObjectType.DISCUSSION_POST), 
			discussion.id,
			post.id
		)
		ua.activityType == UserActivity.ActivityType.COMMENT
		ua.objectType == UserActivity.ObjectType.DISCUSSION
		ua.otherType == UserActivity.ObjectType.DISCUSSION_POST
		ua.discussionGroupIds == discussion.groupIds
		ua.objectDescription == discussion.name
		ua.objectUserId == discussion.userId
		ua.objectVisibility == discussion.visibility

		and: "getActivity for user2 returns no discussion activities"
		def results = searchService.getDiscussionActivity(user2)
		results.success
		results.listItems.size() == 0
				
		where:
		visibility			|	followType						|	followCode		|	unFollowCode
		Visibility.PUBLIC	|	"user follower"					|	addFollower		|	removeFollower
		Visibility.PRIVATE	|	"user follower"					|	addFollower		|	removeFollower
		Visibility.UNLISTED	|	"user follower"					|	addFollower		|	removeFollower
		Visibility.NEW		|	"user follower"					|	addFollower		|	removeFollower
		Visibility.PUBLIC	|	"sprint reader"					|	addSprintReader	|	removeSprintReader
		Visibility.PRIVATE	|	"sprint reader"					|	addSprintReader	|	removeSprintReader
		Visibility.UNLISTED	|	"sprint reader"					|	addSprintReader	|	removeSprintReader
		Visibility.NEW		|	"sprint reader"					|	addSprintReader	|	removeSprintReader
		Visibility.PUBLIC	|	"user tags"						|	addUserTags		|	removeUserTags
		Visibility.PRIVATE	|	"user tags"						|	addUserTags		|	removeUserTags
		Visibility.UNLISTED	|	"user tags"						|	addUserTags		|	removeUserTags
		Visibility.NEW		|	"user tags"						|	addUserTags		|	removeUserTags
		Visibility.PUBLIC	|	"sprint reader w/o discussion"	|	addSprintReader	|	removeSprintDiscussion
		Visibility.PRIVATE	|	"sprint reader w/o discussion"	|	addSprintReader	|	removeSprintDiscussion
		Visibility.UNLISTED	|	"sprint reader w/o discussion"	|	addSprintReader	|	removeSprintDiscussion
		Visibility.NEW		|	"sprint reader w/o discussion"	|	addSprintReader	|	removeSprintDiscussion
	}
}

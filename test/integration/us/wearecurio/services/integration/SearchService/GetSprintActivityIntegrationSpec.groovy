package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec
import groovy.lang.Closure;

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.model.UserActivity
import us.wearecurio.model.UserGroup
import us.wearecurio.model.Tag
import us.wearecurio.utility.Utils

class GetSprintActivityIntegrationSpec extends SearchServiceIntegrationSpecBase {

	static Closure addAdmin = {
		sprint, admin, user = null ->
			sprint.fetchUserGroup()?.addAdmin(admin)
			sprint
	}
	
	static Closure addReader = {
		sprint, reader, user = null ->
			sprint.addReader(reader.id)
			sprint
	}
	
	static Closure addFollower = {
		sprint = null, follower, user ->
			user.follow(follower)
			user
	}
	
	static Closure addUserTags = {
		sprint, unrelated, user = null ->
			def tag = Tag.create(sprint.name)
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

	static def data = [
		//		visibility	otherUser	expectedResult					otherUserCode			deleteSprintExpectedResult
		[Visibility.PUBLIC 	, 	1	, 	"self sees"						,	{d, u2, u1 ->}	,	"self does not see"],
		[Visibility.PRIVATE	, 	1	,	"self sees"						,	{d, u2, u1 ->}	,	"self does not see"],
		[Visibility.UNLISTED,	1	,	"self sees"						, 	{d, u2, u1 ->}	,	"self does not see"],
		[Visibility.NEW		, 	1	,	"self sees"						,	{d, u2, u1 ->}	,	"self does not see"],
		[Visibility.PUBLIC 	, 	2	, 	"admin sees"					,	addAdmin		,	"admin does not see"],
		[Visibility.PRIVATE	, 	2	,	"admin does not see"			,	addAdmin		,	"admin does not see"],
		[Visibility.UNLISTED,	2	,	"admin sees"					,	addAdmin		,	"admin does not see"],
		[Visibility.NEW		, 	2	,	"admin sees"					,	addAdmin		,	"admin does not see"],
		[Visibility.PUBLIC 	, 	2	, 	"reader sees"					,	addReader		,	"reader does not see"],
		[Visibility.PRIVATE	, 	2	,	"reader does not see"			,	addReader		,	"reader does not see"],
		[Visibility.UNLISTED,	2	,	"reader sees"					,	addReader		,	"reader does not see"],
		[Visibility.NEW		, 	2	,	"reader does not see"			,	addReader		,	"reader not see"],
		[Visibility.PUBLIC 	, 	2	, 	"user follower sees"			,	addFollower		,	"user follower does not see"],
		[Visibility.PRIVATE	, 	2	,	"user follower does not see"	,	addFollower		,	"user follower does not see"],
		[Visibility.UNLISTED,	2	,	"user follower does not see"	,	addFollower		,	"user follower does not see"],
		[Visibility.NEW		, 	2	,	"user follower does not see"	,	addFollower		,	"user follower does not see"],
		[Visibility.PUBLIC 	, 	2	, 	"tags user sees"				,	addUserTags		,	"tags user sees"],
		[Visibility.PRIVATE	, 	2	,	"tags user does not see"		,	addUserTags		,	"tags user does not see"],
		[Visibility.UNLISTED,	2	,	"tags user does not see"		,	addUserTags		,	"tags user does not see"],
		[Visibility.NEW		, 	2	,	"tags user does not see"		,	addUserTags		,	"tags user does not see"]
	]
		
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility sprint created activity"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), visibility)
		sprint.description = getUniqueName()
		
		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint, other, user1)
	
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "getActivity for other user returns 0 or 1 sprint created activities"
		def results = searchService.getSprintActivity(other)
		results.success
		expectedSize == 0 || results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].userId == user1.id
		expectedSize == 0 || results.listItems[0].userName == user1.name
		expectedSize == 0 || results.listItems[0].type == "act-created-spr"
		expectedSize == 0 || results.listItems[0].objectId == sprint.id
		expectedSize == 0 || results.listItems[0].objectDescription == sprint.name
		expectedSize == 0 || results.listItems[0].otherId == null
		expectedSize == 0 || results.listItems[0].otherDescription == null

		and: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.CREATE, 
				UserActivity.ObjectType.SPRINT), 
			sprint.id
		)
		ua.userId == user1.id
		ua.activityType == UserActivity.ActivityType.CREATE
		ua.objectType == UserActivity.ObjectType.SPRINT
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == sprint.name
		ua.objectUserId == sprint.userId
		ua.objectVisibility == sprint.visibility
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}

	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility sprint deleted activity"() {
		given: "a new sprint"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), visibility)
		sprint.description = getUniqueName()
		
		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint, other, user1)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "the sprint is deleted"
		Sprint.delete(sprint)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 2 : 0
		
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.DELETE,
				UserActivity.ObjectType.SPRINT),
			sprint.id
		)
		ua.activityType == UserActivity.ActivityType.DELETE
		ua.objectType == UserActivity.ObjectType.SPRINT
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == sprint.name
		ua.sprintDescription == sprint.description
		ua.objectUserId == 0
		ua.objectVisibility == sprint.visibility

		then: "getActivity for other user returns 0 or 1 sprint deleted activities"
		def results = searchService.getSprintActivity(other)
		results.success
		expectedSize == 0 || results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].userId == user1.id
		expectedSize == 0 || results.listItems[0].userName == user1.name
		expectedSize == 0 || results.listItems[0].type == "act-deleted-spr"
		expectedSize == 0 || results.listItems[0].objectId == sprint.id
		expectedSize == 0 || results.listItems[0].objectDescription == sprint.name
		expectedSize == 0 || results.listItems[0].otherId == null
		expectedSize == 0 || results.listItems[0].otherDescription == null

		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		otherUserCode 	<< 	data.collect{ it[3] }
		expectedResult 	<< 	data.collect{ it[4] }
	}
		
	@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility sprint followed activity"() {
		given: "a new sprint"
		def sprint = Sprint.create(user1)
		sprint.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint, other, user1)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "the sprint is deleted"
		Sprint.delete(sprint)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.DELETE,
				UserActivity.ObjectType.SPRINT),
			sprint.id
		)
		ua.activityType == UserActivity.ActivityType.DELETE
		ua.objectType == UserActivity.ObjectType.SPRINT
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == null
		ua.objectUserId == null
		ua.objectVisibility == null

		and: "getActivity for other user returns 0 discussion deleted activities for user"
		def results = searchService.getSprintActivity(other)
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
	void "Test #expectedResult #visibility sprint unfollowed activity"() {
		given: "a new sprint"
		def sprint = Sprint.create(user1)
		sprint.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint, other, user1)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "the sprint is deleted"
		Sprint.delete(sprint)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.DELETE,
				UserActivity.ObjectType.SPRINT),
			sprint.id
		)
		ua.activityType == UserActivity.ActivityType.DELETE
		ua.objectType == UserActivity.ObjectType.SPRINT
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == null
		ua.objectUserId == null
		ua.objectVisibility == null

		and: "getActivity for other user returns 0 discussion deleted activities for user"
		def results = searchService.getSprintActivity(other)
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
	void "Test #expectedResult #visibility sprint started activity"() {
		given: "a new sprint"
		def sprint = Sprint.create(user1)
		sprint.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint, other, user1)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "the sprint is deleted"
		Sprint.delete(sprint)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.DELETE,
				UserActivity.ObjectType.SPRINT),
			sprint.id
		)
		ua.activityType == UserActivity.ActivityType.DELETE
		ua.objectType == UserActivity.ObjectType.SPRINT
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == null
		ua.objectUserId == null
		ua.objectVisibility == null

		and: "getActivity for other user returns 0 discussion deleted activities for user"
		def results = searchService.getSprintActivity(other)
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
	void "Test #expectedResult #visibility sprint stopped activity"() {
		given: "a new sprint"
		def sprint = Sprint.create(user1)
		sprint.visibility = visibility

		and: "another or same user"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint, other, user1)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "the sprint is deleted"
		Sprint.delete(sprint)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "UserActivity is recorded in the db"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.DELETE,
				UserActivity.ObjectType.SPRINT),
			sprint.id
		)
		ua.activityType == UserActivity.ActivityType.DELETE
		ua.objectType == UserActivity.ObjectType.SPRINT
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == null
		ua.objectUserId == null
		ua.objectVisibility == null

		and: "getActivity for other user returns 0 discussion deleted activities for user"
		def results = searchService.getSprintActivity(other)
		results.success
		results.listItems.size() == 0
				
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	void "Test public sprints matching interest tags show up in results"() {
		given: "user2 with interest tags"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user2.addInterestTag(tag)
		
		when: "a new sprint is created by user1 with name matching user2's interest tags"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		Utils.save(sprint1, true)
		
		and: "a new sprint is created by user1 with description matching user2's interest tags"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		sprint2.description = getUniqueName() + " " + tagText
		Utils.save(sprint2, true)
		
		and: "a new sprint is created by user1 with tagName matching user2's interest tags"
		def sprint3 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		sprint3.description = getUniqueName()
		sprint3.tagName = "tagname  " + tagText
		Utils.save(sprint3, true)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getSprintActivity returns 3 results"
		def results = searchService.getSprintActivity(user2)
		results.success
		results.listItems.size() == 3
		
		and: "one result is for the creation of sprint1"
		def s = results.listItems.find{ it.objectId == sprint1.id }
		s != null
		s.userId == user1.id
		s.userName == user1.name
		s.type == "act-created-spr"
		s.objectDescription == sprint1.name
		s.otherId == null
		s.otherDescription == null
		
		and: "one result is for the creation of sprint2"
		def s2 = results.listItems.find{ it.objectId == sprint2.id }
		s2.userId == user1.id
		s2.userName == user1.name
		s2.type == "act-created-spr"
		s2.objectDescription == sprint2.name
		s2.otherId == null
		s2.otherDescription == null

		and: "one result is for the creation of sprint3"
		def s3 = results.listItems.find{ it.objectId == sprint3.id }
		s3.userId == user1.id
		s3.userName == user1.name
		s3.type == "act-created-spr"
		s3.objectDescription == sprint3.name
		s3.otherId == null
		s3.otherDescription == null
	}	
	
	void "Test sprints with #visibility matching interest tags do not show up in results"() {
		given: "user2 with interest tags"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user2.addInterestTag(tag)
		
		when: "a new sprint is created by user1 with name matching user2's interest tags"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName() + " " + tagText, visibility)
		sprint1.description = getUniqueName()
		Utils.save(sprint1, true)
		
		and: "a new sprint is created by user1 with description matching user2's interest tags"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), visibility)
		sprint2.description = getUniqueName() + " " + tagText
		Utils.save(sprint2, true)
		
		and: "a new sprint is created by user1 with tagName matching user2's interest tags"
		def sprint3 = Sprint.create(currentTime, user1, getUniqueName(), visibility)
		sprint3.description = getUniqueName()
		sprint3.tagName += " " + tagText
		Utils.save(sprint3, true)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintActivity returns no results"
		def results = searchService.getSprintActivity(user2)
		results.success
		results.listItems.size() == 0
		
		and: "sprint created user activity is created for sprint1 in the database"
		def ua = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.CREATE,
				UserActivity.ObjectType.SPRINT),
			sprint1.id
		)
		ua.activityType == UserActivity.ActivityType.CREATE
		ua.objectType == UserActivity.ObjectType.SPRINT
		ua.otherType == null
		ua.otherId == null
		ua.discussionGroupIds == null
		ua.objectDescription == sprint1.name
		ua.sprintDescription == sprint1.description
		ua.objectUserId == sprint1.userId
		ua.objectVisibility == sprint1.visibility

		and: "sprint created user activity is created for sprint2 in the database"
		def ua2 = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.CREATE,
				UserActivity.ObjectType.SPRINT),
			sprint2.id
		)
		ua2.activityType == UserActivity.ActivityType.CREATE
		ua2.objectType == UserActivity.ObjectType.SPRINT
		ua2.otherType == null
		ua2.otherId == null
		ua2.discussionGroupIds == null
		ua2.objectDescription == sprint2.name
		ua2.sprintDescription == sprint2.description
		ua2.objectUserId == sprint2.userId
		ua2.objectVisibility == sprint2.visibility

		and: "sprint created user activity is created for sprint3 in the database"
		def ua3 = UserActivity.findByTypeIdAndObjectId(
			UserActivity.toType(
				UserActivity.ActivityType.CREATE,
				UserActivity.ObjectType.SPRINT),
			sprint3.id
		)
		ua3.activityType == UserActivity.ActivityType.CREATE
		ua3.objectType == UserActivity.ObjectType.SPRINT
		ua3.otherType == null
		ua3.otherId == null
		ua3.discussionGroupIds == null
		ua3.objectDescription == sprint3.name
		ua3.sprintDescription == sprint3.description
		ua3.objectUserId == sprint3.userId
		ua3.objectVisibility == sprint3.visibility

		where:
		visibility << [
			Visibility.PRIVATE,
			Visibility.UNLISTED,
			Visibility.NEW
		]
	}
}

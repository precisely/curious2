package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec
import groovy.lang.Closure;

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.GroupMemberDiscussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.UserGroup

import us.wearecurio.utility.Utils

class SearchDiscussionsIntegrationSpec extends SearchServiceIntegrationSpecBase {
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
	@spock.lang.Unroll
    void "Test #visibility discussion when search term in name"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term in name"
		def discussion = Discussion.create(user1, getUniqueName() + " " + term)
		discussion.visibility = visibility
		Utils.save(discussion, true)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term)
		results.success
		!sees || results.listItems.size() == 1
		!sees || results.listItems[0].type == "dis"
		!sees || results.listItems[0].id == discussion.id.toString()
		!sees || results.listItems[0].name == discussion.name
		
		where:
		visibility			|	sees
		Visibility.PUBLIC	|	true
		Visibility.PRIVATE	|	false
		Visibility.UNLISTED	|	false
		Visibility.NEW		|	false
    }
	
	//@spock.lang.Ignore
    void "Test only one discussion when search term in name and in post message"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term in name"
		def discussion = Discussion.create(user1, getUniqueName() + " " + term)
		discussion.visibility = Visibility.PUBLIC
		Utils.save(discussion, true)
		
		when: "user1 adds a post with search term in message"
		def post = discussion.createPost(user1, getUniqueName() + " " + term)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
    }
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #visibility discussion when search term in discussion post message"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new sprint with search term not in name"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility
		Utils.save(discussion, true)
		
		when: "user1 adds a post with search term in message"
		def post = discussion.createPost(user1, getUniqueName() + " " + term)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term)
		results.success
		!sees || results.listItems.size() == 1
		!sees || results.listItems[0].type == "dis"
		!sees || results.listItems[0].id == discussion.id.toString()
		!sees || results.listItems[0].name == discussion.name
				
		where:
		visibility			|	sees
		Visibility.PUBLIC	|	true
		Visibility.PRIVATE	|	false
		Visibility.UNLISTED	|	false
		Visibility.NEW		|	false
	}
	
	//@spock.lang.Ignore
	void "Test discussion returned when search term in multiple discussion post messages"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new sprint with search term not in name"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = Visibility.PUBLIC
		Utils.save(discussion, true)
		
		when: "user1 adds a post with search term in message"
		def post1 = discussion.createPost(user1, getUniqueName() + " " + term)
		
		and: "user1 adds a post with search term in message"
		def post2 = discussion.createPost(user1, getUniqueName() + " " + term)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility discussion when search term in name"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term in name"
		def discussion = Discussion.create(user1, getUniqueName() + " " + term)
		discussion.visibility = visibility
		Utils.save(discussion, true)
		
		when: "other code is called"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(other, term)
		results.success
		expectedSize == 0 || results.listItems.size() == 1
		expectedSize == 0 || results.listItems[0].type == "dis"
		expectedSize == 0 || results.listItems[0].id == discussion.id.toString()
		expectedSize == 0 || results.listItems[0].name == discussion.name

		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}	

	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility discussion when search term in discussion post message"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term not in name"
		def discussion = Discussion.create(user1, getUniqueName())
		discussion.visibility = visibility
		Utils.save(discussion, true)
		
		when: "user1 adds a post with search term in message"
		def post = discussion.createPost(user1, getUniqueName() + " " + term)
		
		and: "other code is called"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion, other, user1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(other, term)
		results.success
		expectedSize == 0 || results.listItems.size() == 1
		expectedSize == 0 || results.listItems[0].type == "dis"
		expectedSize == 0 || results.listItems[0].id == discussion.id.toString()
		expectedSize == 0 || results.listItems[0].name == discussion.name

		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}	

	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility discussions when search term in name"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = visibility
		Utils.save(discussion1, true)
		
		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion2.visibility = visibility
		Utils.save(discussion2, true)
		
		when: "other code is called"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion1, other, user1)
		otherUserCode(discussion2, other, user1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(other, term)
		results.success
		expectedSize == 0 || results.listItems.size() == 2
		expectedSize == 0 || results.listItems.find{
			it.type == "dis" &&
			it.id == discussion1.id.toString()
			it.name == discussion1.name
		} != null
		expectedSize == 0 || results.listItems.find{
			it.type == "dis" &&
			it.id == discussion1.id.toString()
			it.name == discussion1.name
		} != null

		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}	

	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "Test #expectedResult #visibility discussion when search term in discussion post messages"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term not in name"
		def discussion1 = Discussion.create(user1, getUniqueName())
		discussion1.visibility = visibility
		Utils.save(discussion1, true)
		
		and: "another discussion with search term not in name"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = visibility
		Utils.save(discussion2, true)
		
		when: "user1 adds a post with search term in message to discussion1"
		def post1 = discussion1.createPost(user1, getUniqueName() + " " + term)
		
		and: "user3 adds a post with search term in message to discussion2"
		def post2 = discussion2.createPost(user3, getUniqueName() + " " + term)
		
		and: "other code is called"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion1, other, user1)
		otherUserCode(discussion2, other, user1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(other, term)
		results.success
		expectedSize == 0 || results.listItems.size() == 2
		expectedSize == 0 || results.listItems.find{
			it.type == "dis" &&
			it.id == discussion1.id.toString()
			it.name == discussion1.name
		} != null
		expectedSize == 0 || results.listItems.find{
			it.type == "dis" &&
			it.id == discussion2.id.toString()
			it.name == discussion2.name
		} != null

		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}	

	@spock.lang.Unroll
	void "Test #expectedResult #visibility discussion when search term in discussion name and discussion post message"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = visibility
		Utils.save(discussion1, true)
		
		and: "another discussion with search term not in name"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = visibility
		Utils.save(discussion2, true)

		when: "user1 adds a post with search term not in message to discussion1"
		def post1 = discussion1.createPost(user1, getUniqueName())
		
		and: "user3 adds a post with search term in message to discussion2"
		def post2 = discussion1.createPost(user3, getUniqueName() + " " + term)
		
		and: "other code is called"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(discussion1, other, user1)
		otherUserCode(discussion2, other, user1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(other, term)
		results.success
		expectedSize == 0 || results.listItems.size() == 1
		expectedSize == 0 || results.listItems[0].type == "dis"
		expectedSize == 0 || results.listItems[0].id == discussion1.id.toString()
		expectedSize == 0 || results.listItems[0].name == discussion1.name

		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}	

	//@spock.lang.Ignore
	void "Test discussion returned when all search terms in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new discussion with both search terms in name"
		def discussion = Discussion.create(user1, getUniqueName() + " " + term1 + " " + term2)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
	}
	
	//@spock.lang.Ignore
	void "Test discussion returned when all search terms in discussion post message"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new discussion with neither search term in name"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "user1 adds a post with both search terms in message to discussion"
		def post = discussion.createPost(user1, getUniqueName() + " " + term1 + " " + term2)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
	}
		
	//@spock.lang.Ignore
	void "Test discussion not returned when only first search term in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new discussion with only first search term in name"
		def discussion = Discussion.create(user1, getUniqueName() + " " + term1)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}
	
	//@spock.lang.Ignore
	void "Test discussion not returned when only second search term in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new discussion with only second search term in name"
		def discussion = Discussion.create(user1, getUniqueName() + " " + term2)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}
	
	//@spock.lang.Ignore
	void "Test discussion not returned when only first search term in discussion post message"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new discussion with neither search term in name"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "user1 adds a post with first search term in message to discussion"
		def post = discussion.createPost(user1, getUniqueName() + " " + term1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}

	//@spock.lang.Ignore
	void "Test discussion not returned when only second search term in discussion post message"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new discussion with neither search term in name"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "user1 adds a post with second search term in message to discussion"
		def post = discussion.createPost(user1, getUniqueName() + " " + term2)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}
	
	//@spock.lang.Ignore
	void "Test max with searchDiscussions"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.created = curTime
		Utils.save(discussion1, true)
		
		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion2.created = curTime - 1
		Utils.save(discussion2, true)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchSprints returns sprint"
		def results = searchService.searchDiscussions(user1, term, 0, 1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion1.id.toString()
		results.listItems[0].name == discussion1.name
	}
	
	//@spock.lang.Ignore
	void "Test max with multiple discussion posts for single discussion"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new discussion with search term in name"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "user1 adds a post with search term in message to discussion"
		def post1 = discussion.createPost(user1, getUniqueName() + " " + term)
		
		and: "user1 adds a post with search term in message to discussion"
		def post2 = discussion.createPost(user1, getUniqueName() + " " + term)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns discussion"
		def results = searchService.searchDiscussions(user1, term, 0, 1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
	}
	
	//@spock.lang.Ignore
	void "Test offset with searchDiscussions"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.created = curTime
		Utils.save(discussion1, true)
		
		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion2.created = curTime - 1
		Utils.save(discussion2, true)
		
		and: "another discussion with search term in name"
		def discussion3 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion3.created = curTime - 2
		Utils.save(discussion3, true)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns first discussion for offset 0"
		def results1 = searchService.searchDiscussions(user1, term, 0)
		results1.success
		results1.listItems.size() == 3
		results1.listItems[0].type == "dis"
		results1.listItems[0].id == discussion1.id.toString()
		results1.listItems[0].name == discussion1.name
		
		and: "searchDiscussions returns second discussion for offset 1"
		def results2 = searchService.searchDiscussions(user1, term, 1)
		results2.success
		results2.listItems.size() == 2
		results2.listItems[0].type == "dis"
		results2.listItems[0].id == discussion2.id.toString()
		results2.listItems[0].name == discussion2.name

		and: "searchDiscussions returns third discussion for offset 2"
		def results3 = searchService.searchDiscussions(user1, term, 2)
		results3.success
		results3.listItems.size() == 1
		results3.listItems[0].type == "dis"
		results3.listItems[0].id == discussion3.id.toString()
		results3.listItems[0].name == discussion3.name
	}
	
	//@spock.lang.Ignore
	void "Test offset with multiple discussion posts for single discussion"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName())
		discussion1.created = curTime - 7
		Utils.save(discussion1, true)
		
		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.created = curTime - 8
		Utils.save(discussion2, true)
		
		and: "another discussion with search term in name"
		def discussion3 = Discussion.create(user1, getUniqueName())
		discussion3.created = curTime - 9
		Utils.save(discussion3, true)
		
		when: "user1 adds a post with search term in message to discussion"
		def post11 = discussion1.createPost(user1, getUniqueName() + " " + term)
		post11.created = curTime
		Utils.save(post11, true)
		
		and: "user1 adds a post with search term in message to discussion"
		def post12 = discussion1.createPost(user1, getUniqueName() + " " + term)
		post12.created = curTime - 2
		Utils.save(post12, true)
		
		and: "user1 adds a post with search term in message to discussion"
		def post21 = discussion2.createPost(user1, getUniqueName() + " " + term)
		post21.created = curTime - 3
		Utils.save(post21, true)
		
		and: "user1 adds a post with search term in message to discussion"
		def post22 = discussion2.createPost(user1, getUniqueName() + " " + term)
		post22.created = curTime - 4
		Utils.save(post22, true)
		
		and: "user1 adds a post with search term in message to discussion"
		def post31 = discussion3.createPost(user1, getUniqueName() + " " + term)
		post31.created = curTime - 5
		Utils.save(post31, true)
		
		and: "user1 adds a post with search term in message to discussion"
		def post32 = discussion3.createPost(user1, getUniqueName() + " " + term)
		post32.created = curTime - 6
		Utils.save(post32, true)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchDiscussions returns first discussion for offset 0"
		def results1 = searchService.searchDiscussions(user1, term, 0)
		results1.success
		results1.listItems.size() == 3
		results1.listItems[0].type == "dis"
		results1.listItems[0].id == discussion1.id.toString()
		results1.listItems[0].name == discussion1.name
		
		and: "searchDiscussions returns second discussion for offset 1"
		def results2 = searchService.searchDiscussions(user1, term, 1)
		results2.success
		results2.listItems.size() == 2
		results2.listItems[0].type == "dis"
		results2.listItems[0].id == discussion2.id.toString()
		results2.listItems[0].name == discussion2.name

		and: "searchDiscussions returns third discussion for offset 2"
		def results3 = searchService.searchDiscussions(user1, term, 2)
		results3.success
		results3.listItems.size() == 1
		results3.listItems[0].type == "dis"
		results3.listItems[0].id == discussion3.id.toString()
		results3.listItems[0].name == discussion3.name
	}
}

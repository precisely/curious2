package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec
import groovy.lang.Closure;

import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.Model.Visibility

class SearchSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {
	
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
		//		visibility	otherUser	expectedResult					otherUserCode
		[Visibility.PUBLIC 	, 	1	, 	"self sees"						,	{d, u2, u1 ->}],
		[Visibility.PRIVATE	, 	1	,	"self sees"						,	{d, u2, u1 ->}],
		[Visibility.UNLISTED,	1	,	"self sees"						, 	{d, u2, u1 ->}],
		[Visibility.NEW		, 	1	,	"self sees"						,	{d, u2, u1 ->}],
		[Visibility.PUBLIC 	, 	2	, 	"admin sees"					,	addAdmin],
		[Visibility.PRIVATE	, 	2	,	"admin sees"					,	addAdmin],
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
		[Visibility.PUBLIC 	, 	2	, 	"tags user sees"				,	addUserTags],
		[Visibility.PRIVATE	, 	2	,	"tags user does not see"		,	addUserTags],
		[Visibility.UNLISTED,	2	,	"tags user does not see"		,	addUserTags],
		[Visibility.NEW		, 	2	,	"tags user does not see"		,	addUserTags]
	]
	
	@spock.lang.Unroll
	void "Test #visibility sprint when search term in name"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + term, visibility)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchSprints returns sprint"
		def results = searchService.searchSprints(user1, term)
		results.success
		!sees || results.listItems.size() == 1
		!sees || results.listItems[0].type == "spr"
		!sees || results.listItems[0].id == sprint.id.toString()
		!sees || results.listItems[0].name == sprint.name
		
		where:
		visibility			|	sees
		Visibility.PUBLIC	|	true
		Visibility.PRIVATE	|	false
		Visibility.UNLISTED	|	false
		Visibility.NEW		|	false
	}
	
	@spock.lang.Unroll
	void "Test #expectedResult #visibility sprint when search term in name"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + term, visibility)
		
		when: "other code is called"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint, other, user1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 1 : 0
		
		then: "searchSprints returns sprint"
		def results = searchService.searchSprints(other, term)
		results.success
		results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems[0].type == "spr"
		expectedSize == 0 || results.listItems[0].id == sprint.id.toString()
		expectedSize == 0 || results.listItems[0].name == sprint.name
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	@spock.lang.Unroll
	void "Test #expectedResult #visibility sprints when search term in name"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName() + " " + term, visibility)
		
		and: "another sprint with search term in name"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName() + " " + term, visibility)
		
		when: "other code is called"
		def other = (otherUser == 1 ? user1 : user2)
		otherUserCode(sprint1, other, user1)
		otherUserCode(sprint2, other, user1)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "expected size is determined"
		def expectedSize = expectedResult.contains("sees") ? 2 : 0
		
		then: "searchSprints returns both sprints"
		def results = searchService.searchSprints(other, term)
		results.success
		results.listItems.size() == expectedSize
		expectedSize == 0 || results.listItems.find{ 
			it.type == "spr" &&
			it.id == sprint1.id.toString() &&
			it.name == sprint1.name
		} != null
		expectedSize == 0 || results.listItems.find{ 
			it.type == "spr" &&
			it.id == sprint2.id.toString() &&
			it.name == sprint2.name
		} != null
		
		where:
		visibility 		<<	data.collect{ it[0] }
		otherUser 		<< 	data.collect{ it[1] }
		expectedResult 	<< 	data.collect{ it[2] }
		otherUserCode 	<< 	data.collect{ it[3] }
	}
	
	@spock.lang.Unroll
	void "Test #visibility sprint when all search terms in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + term1 + " " + term2, visibility)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchSprints returns sprint or not"
		def results = searchService.searchSprints(user1, term1 + " " + term2)
		results.success
		!sees || results.listItems.size() == 1
		!sees || results.listItems[0].type == "spr"
		!sees || results.listItems[0].id == sprint.id.toString()
		!sees || results.listItems[0].name == sprint.name
		
		where:
		visibility			|	sees
		Visibility.PUBLIC	|	true
		Visibility.PRIVATE	|	false
		Visibility.UNLISTED	|	false
		Visibility.NEW		|	false
	}
	
	void "Test sprint not returned when only first search term in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + term1, , Visibility.PUBLIC)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchSprints does not return sprint"
		def results = searchService.searchSprints(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}
	
	void "Test sprint not returned when only second search term in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + term2, Visibility.PUBLIC)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchSprints does not return sprint"
		def results = searchService.searchSprints(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}
	
	void "Test max with searchSprints"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new sprint with search term in name"
		def sprint1 = Sprint.create(curTime, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "another sprint with search term in name"
		def sprint2 = Sprint.create(curTime - 1, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchSprints returns sprint"
		def results = searchService.searchSprints(user1, term, 0, 1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "spr"
		results.listItems[0].id == sprint1.id.toString()
		results.listItems[0].name == sprint1.name
	}
	
	void "Test offset with searchSprints"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new sprint with search term in name"
		def sprint1 = Sprint.create(curTime, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "another sprint with search term in name"
		def sprint2 = Sprint.create(curTime - 1, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "another sprint with search term in name"
		def sprint3 = Sprint.create(curTime - 2, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchSprints returns first sprint for offset 0"
		def results1 = searchService.searchSprints(user1, term, 0)
		results1.success
		results1.listItems.size() == 3
		results1.listItems[0].type == "spr"
		results1.listItems[0].id == sprint1.id.toString()
		results1.listItems[0].name == sprint1.name
		
		and: "searchSprints returns second sprint for offset 1"
		def results2 = searchService.searchSprints(user1, term, 1)
		results2.success
		results2.listItems.size() == 2
		results2.listItems[0].type == "spr"
		results2.listItems[0].id == sprint2.id.toString()
		results2.listItems[0].name == sprint2.name

		and: "searchSprints returns third sprint for offset 2"
		def results3 = searchService.searchSprints(user1, term, 2)
		results3.success
		results3.listItems.size() == 1
		results3.listItems[0].type == "spr"
		results3.listItems[0].id == sprint3.id.toString()
		results3.listItems[0].name == sprint3.name
	}
}

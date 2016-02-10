package us.wearecurio.search.integration

import us.wearecurio.model.Tag
import us.wearecurio.model.User

import us.wearecurio.services.SearchService

import us.wearecurio.utility.Utils

class GetSuggestionsUsersIntegrationSpec extends SearchServiceIntegrationSpecBase {

	void "Test getSuggestions for users"() {
	}
	
	boolean actualMatchesExpected(List actual, List expected) {
		return 	(actual[0].hash == expected[0].hash) &&
			(actual[1].hash == expected[1].hash) &&
			(actual[2].hash == expected[2].hash)
	}
	
	boolean successful(def results, def count) {
		return results.success &&
			(results.listItems.size == count) &&
			(results.listItems.count{ it.type == "usr" } == count)
	}
	
	List createUsers() {
		user2.created = new Date() - 10
		Utils.save(user2, true)
		
		user3.created = user2.created + 1
		Utils.save(user3, true)
		
		User user4 = User.create(
			[	username:'james',
				sex:'F',
				name:'james fearnley',
				email:'james@pogues.com',
				birthdate:'01/01/1960',
				password:'jamesxyz',
				action:'doregister',
				controller:'home'	]
		)
		user4.settings.makeNamePublic()
		user4.settings.makeBioPublic()
		user4.created = user3.created + 1
		Utils.save(user4, true)
		
		List users = []
		
		//results ordered newest first
		users << user4
		users << user3
		users << user2		

		return users
	}
	
	//@spock.lang.IgnoreRest
	void "Test different sessionId for getSuggestions for users"() {		
		given: "users with successive created dates"
		List users = createUsers()
				
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called multiple times with the same id"
		Random r = new Random()
		def results1 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(1))
		def results2 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(2))
		def results3 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(3))
		def results4 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(4))
		def results5 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(5))
		def results6 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(6))
		def results7 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(7))
		def results8 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(8))
		def results9 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(9))
		def results10 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, r.nextInt(10))
		
		then: "all results are successfully return correct number"
		successful(results1, users.size)
		successful(results2, users.size)
		successful(results3, users.size)
		successful(results4, users.size)
		successful(results5, users.size)
		successful(results6, users.size)
		successful(results7, users.size)
		successful(results8, users.size)
		successful(results9, users.size)
		successful(results10, users.size)
		
		and: "at least one of the results in different order"
		!actualMatchesExpected(results1.listItems, users) ||
		!actualMatchesExpected(results2.listItems, users) ||
		!actualMatchesExpected(results3.listItems, users) ||
		!actualMatchesExpected(results4.listItems, users) ||
		!actualMatchesExpected(results5.listItems, users) ||
		!actualMatchesExpected(results6.listItems, users) ||
		!actualMatchesExpected(results7.listItems, users) ||
		!actualMatchesExpected(results8.listItems, users) ||
		!actualMatchesExpected(results9.listItems, users) ||
		!actualMatchesExpected(results10.listItems, users)		
	}
	
	//@spock.lang.IgnoreRest
	void "Test same sessionId for getSuggestions for users"() {		
		given: "users with successive created dates"
		List users = createUsers()

		and: "a sessionId"
		Long sessionId = 395

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called multiple times with the same id"
		def results1 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results2 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results3 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results4 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results5 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results6 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results7 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results8 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results9 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)
		def results10 = searchService.getSuggestions(SearchService.USER_TYPE, user1, 0, 10, sessionId)		
		
		then: "all results are successfully return correct number"
		successful(results1, users.size)
		successful(results2, users.size)
		successful(results3, users.size)
		successful(results4, users.size)
		successful(results5, users.size)
		successful(results6, users.size)
		successful(results7, users.size)
		successful(results8, users.size)
		successful(results9, users.size)
		successful(results10, users.size)
		
		and: "all results are the same"
		actualMatchesExpected(results1.listItems, results2.listItems)
		actualMatchesExpected(results2.listItems, results3.listItems)
		actualMatchesExpected(results3.listItems, results4.listItems)
		actualMatchesExpected(results4.listItems, results5.listItems)
		actualMatchesExpected(results5.listItems, results6.listItems)
		actualMatchesExpected(results6.listItems, results7.listItems)
		actualMatchesExpected(results7.listItems, results8.listItems)
		actualMatchesExpected(results8.listItems, results9.listItems)
		actualMatchesExpected(results9.listItems, results10.listItems)
	}
	
	//@spock.lang.IgnoreRest
	void "Test default sessionId for getSuggestions for users"() {		
		given: "users with successive created dates"
		List users = createUsers()

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called multiple times with the default sessionId"
		def results1 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results2 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results3 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results4 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results5 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results6 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results7 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results8 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results9 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		def results10 = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		
		then: "all results are successfully return correct number"
		successful(results1, users.size)
		successful(results2, users.size)
		successful(results3, users.size)
		successful(results4, users.size)
		successful(results5, users.size)
		successful(results6, users.size)
		successful(results7, users.size)
		successful(results8, users.size)
		successful(results9, users.size)
		successful(results10, users.size)
		
		and: "all results are the same"
		actualMatchesExpected(results1.listItems, results2.listItems)
		actualMatchesExpected(results2.listItems, results3.listItems)
		actualMatchesExpected(results3.listItems, results4.listItems)
		actualMatchesExpected(results4.listItems, results5.listItems)
		actualMatchesExpected(results5.listItems, results6.listItems)
		actualMatchesExpected(results6.listItems, results7.listItems)
		actualMatchesExpected(results7.listItems, results8.listItems)
		actualMatchesExpected(results8.listItems, results9.listItems)
		actualMatchesExpected(results9.listItems, results10.listItems)
	}
	
	void "Test virtual user with tag not returned"() {
		given: "a new Tag"
		Tag tag = Tag.create("testTag")
		
		and: "interest tag is added to user1"
		user1.addInterestTag(tag)
		Utils.save(user1, true)
		
		and: "a new user with the same interest tag"
		User user = User.create(
			[	username:'cait',
				sex:'F',
				name:'cait oriordan',
				email:'cait@pogues.com',
				birthdate:'01/01/1962',
				password:'caitxyz',
				action:'doregister',
				controller:'home'	]
		)
		user.settings.makeNamePublic()
		user.settings.makeBioPublic()
		user.addInterestTag(tag)
		Utils.save(user, true)
		
		and: "user1 has matching interest tag"
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called"
		def results = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		
		then: "user is found"
		results.success
		results.listItems.size > 0
		results.listItems.find{it.type == "usr" && it.hash == user.hash}
		
		when: "virtual flag is set to true"
		user.virtual = true
		Utils.save(user, true)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called"
		results = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		
		then: "user is not found"
		results.success
		results.listItems.find{it.type == "usr" && it.hash == user.hash} == null		
	}
	
	void "Test offset for getSuggestions for users"() {
	}
	
	void "Test max for getSuggestions for users"() {
	}
	
	void "Test sessionId for getSuggestions for users"() {
	}

	void "Test sessionId with offset for getSuggestions for users"() {
	}
	
	void "Test sessionId with max for getSuggestions for users"() {
	}

}

package us.wearecurio.search.integration

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Tag
import us.wearecurio.model.User

import us.wearecurio.utility.Utils

class SearchUsersIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//@spock.lang.IgnoreRest
	void "Test non-followed user found"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "user2's name includes searchTerm"
		user2.name += " $searchTerm"
		Utils.save(user2, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "non-followed user is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == user2.hash
	}
	
	void "Test single user found when followed name match and tag match"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a tag matching search term matches"
		def tag = Tag.create(searchTerm)
		user2.addInterestTag(tag)
		
		and: "user2's name includes searchTerm"
		user2.name += " $searchTerm"
		Utils.save(user2, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single matched user is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == user2.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test search for user with partial word"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "user2's name includes searchTerm"
		user2.name += " $searchTerm"
		Utils.save(user2, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, "mag")
		
		then: "matched user is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == user2.hash
	}
	
    //@spock.lang.IgnoreRest
	void "Test search for users by username does finds self"() {
        given: "a new user"
		User user = User.create(
			[	username:'shaneMac1',
				sex:'F',
				name:'shane macgowen',
				email:'shane@pogues.com',
				birthdate:'01/01/1960',
				password:'shanexyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
		and: "search is performed by user on user's username"
		def results = searchService.search(user, user.username)
		
		then: "self is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == user.hash
	}
	
    //@spock.lang.IgnoreRest
	void "Test search for users by username finds other user"() {
        given: "a new user"
		User user = User.create(
			[	username:'shaneMac1',
				sex:'F',
				name:'shane macgowen',
				email:'shane@pogues.com',
				birthdate:'01/01/1960',
				password:'shanexyz',
				action:'doregister',
				controller:'home'	]
		)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
		and: "search is performed by user1 on user's username"
		def results = searchService.search(user1, user.username)
		
		then: "user1 is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == user.hash
	}
    
	//@spock.lang.IgnoreRest
	void "Test search for users by non-existing username returns no results"() {
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed for user2 on a non-existant username"
        def results = searchService.search(user2, "${user1.username}_$uniqueName")
		
		then: "user1 is not found"
		results.success
		results.listItems.size == 0
	}
	
	void "Test offset for search for users"() {
	}
	
	void "Test max for search for users"() {
	}
	
	void "Test offset and max for search for users"() {
	}

	void "Test match in name and tag ahead of match in name alone"() {
	}

	void "Test match in name and tag ahead of match in tag alone"() {
	}

	void "Test match in name ahead of match in tag"() {
	}
	
	void "Test followed users before non-followed users"() {
	}
}

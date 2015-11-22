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
	void "Test search for user with wildcard"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "user2's name includes searchTerm"
		user2.name += " $searchTerm"
		Utils.save(user2, true)
						
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, "mag*")
		
		then: "matched user is found"
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
	
	void "Test search for users"() {
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

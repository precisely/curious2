package us.wearecurio.search.integration

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint

import us.wearecurio.utility.Utils

class SearchSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//@spock.lang.IgnoreRest
	void "Test public sprint found"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public sprint"
		Sprint s = Sprint.create(new Date(), user2, "sprPublicRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single public sprint is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
	}
	
	void "Test public single sprint found when followed and public"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new sprint"
		Sprint s = Sprint.create(new Date(), user2, "sprFollowedRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
		
		and: "user1 follows the sprint"
		s.fetchUserGroup().addReader(user1)
		Utils.save(s, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single public and followed sprint is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
	}
	
	//@spock.lang.IgnoreRest
<<<<<<< HEAD
=======
	void "Test search for sprint with wildcard"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public sprint"
		Sprint s = Sprint.create(new Date(), user2, "sprPublicRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
				
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, "mag*")
		
		then: "matched sprint is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
	}
	
	//@spock.lang.IgnoreRest
>>>>>>> 7a097bda1ea75adefff88308beead1afc99fb1d3
	void "Test search for sprint with partial word"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public sprint"
		Sprint s = Sprint.create(new Date(), user2, "sprPublicRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
				
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, "mag")
		
		then: "matched sprint is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
	}
	
	void "Test search for sprints"() {
	}
	
	void "Test offset for search for sprints"() {
	}
	
	void "Test max for search for sprints"() {
	}
	
	void "Test offset and max for search for sprints"() {
	}
	
	void "Test old sprint with match in name behind recent sprint with match in name"(){
	}

	void "Test old sprint with match in name behind recent sprint with match in description"(){
	}

	void "Test old sprint with match in description behind recent sprint with match in name"(){
	}

	void "Test recent sprint with match in name behind older recent sprint with match in name"(){
	}

	void "Test recent sprint with match in description behind older recent sprint with match in description"(){
	}

	void "Test old sprint with match in name behind older old sprint with match in name"(){
	}

	void "Test old sprint with match in description behind older old sprint with match in description"(){
	}
	
	void "Test followed sprints before public sprints"() {
	}
	
	void "Test owned sprints before public sprints"() {
	}
	

}

package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint

import us.wearecurio.utility.Utils

class ExcludeDeletedItemsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    //@spock.lang.Ignore
    void "Test deleted discussions not included in search results"() {
		given: "a search phrase"
		def searchPhrase = "check magnesium levels"
		
		and: "a new public discussion with searchPhrase in name"
		Discussion d = Discussion.create(user2, "$uniqueTerm $searchPhrase", Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, /"$searchPhrase"/)
		
		then: "matched discussion is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == d.hash
        
        when: "discussion is deleted"
		Discussion.delete(d)
        
        and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "search is performed"
		results = searchService.search(user1, /"$searchPhrase"/)
        
        then: "no matching discussion is found"
		results.success
		results.listItems.size == 0        
    }
    
    //@spock.lang.Ignore
    void "Test deleted sprints not included in search results"() {
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
        
        when: "sprint is deleted"
        Sprint.delete(s)
        searchService.deindex(s)
  
		and: "elasticsearch service is indexed"
        elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "search is performed"
		results = searchService.search(user1, searchTerm)        
        
        then: "sprint not in results"
		results.success
		results.listItems.size == 0
    }
}

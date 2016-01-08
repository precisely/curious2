package us.wearecurio.search.integration

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class GetSuggestionsSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {

	void "Test getSuggestions for sprints"() {
	}
	
	void "Test offset for getSuggestions for sprints"() {
	}
	
	void "Test max for getSuggestions for sprints"() {
	}
	
	void "Test sessionId for getSuggestions for sprints"() {
	}

	void "Test sessionId with offset for getSuggestions for sprints"() {
	}
	
	void "Test sessionId with max for getSuggestions for sprints"() {
	}
	
    //@spock.lang.IgnoreRest
    void "Test getSuggestions does not return untitled sprint with empty name"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an untitled sprint with empty string for title, but tag match in description"
        def untitled = Sprint.create(new Date(), user2, "", Visibility.PUBLIC)         
        untitled.description = tagText
        
		when: "getSuggestions is called"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "untitled sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == untitled.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getSuggestions does not return untitled sprint with null name"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an untitled sprint with null for title, but tag match in description"
        def untitled = Sprint.create(new Date(), user2, null, Visibility.PUBLIC)         
        untitled.description = tagText
        
		when: "getSuggestions is called"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
        
        then: "untitled sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == untitled.hash} == null
	}
}

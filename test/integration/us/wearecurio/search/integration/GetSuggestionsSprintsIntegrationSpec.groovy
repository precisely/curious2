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
    void "Test getSuggestions does not return unedited sprint with empty description"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with empty string for description, but tag match in name"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = ""
        Utils.save(unedited)
        
		when: "getSuggestions is called"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getSuggestions does not return unedited sprint with null name"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with null description, but tag match in name"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = null
        Utils.save(unedited)
        
		when: "getSuggestions is called"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
        
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
}

package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class GetFeedSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {    
	void "Test getFeed for sprints"() {
	}
	
	void "Test offset for getFeed for sprints"() {
	}
	
	void "Test max for getFeed for sprints"() {
	}
	
	void "Test sessionId for getFeed for sprints"() {
	}

	void "Test suggestionOffset for getFeed for sprints"() {
	}
	
	void "Test offset and suggestionOffset for getFeed for sprints"() {
	}
	
	void "Test max and suggestionOffset for getFeed for sprints"() {
	}

    //@spock.lang.IgnoreRest
    void "Test getFeed does not return untitled sprint with empty name"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an untitled sprint with empty string for title, but tag match in description"
        def untitled = Sprint.create(new Date(), user2, "", Visibility.PUBLIC)         
        untitled.description = tagText
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "untitled sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == untitled.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getFeed does not return untitled sprint with null name"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an untitled sprint with null for title, but tag match in description"
        def untitled = Sprint.create(new Date(), user2, null, Visibility.PUBLIC)         
        untitled.description = tagText
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
        
        then: "untitled sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == untitled.hash} == null
	}

    //@spock.lang.IgnoreRest
    void "Test getFeed does not return untitled sprint with empty name"() {
        given: "an untitled sprint with empty string for title"
        def untitled = Sprint.create(new Date(), user1, "", Visibility.PUBLIC)         
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "untitled sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == untitled.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getFeed does not return untitled sprint with null name"() {
        given: "an untitled sprint with empty string for title"
        def untitled = Sprint.create(new Date(), user1, null, Visibility.PUBLIC)         
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "untitled sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == untitled.hash} == null
	}    
}

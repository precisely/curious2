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
    void "Test getFeed does not return unedited sprint with empty description but tag match"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with empty description, but tag match in title"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = ""
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getFeed does not return unedited sprint with null description but tag match"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with null description, but tag match in name"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = null
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
        
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}

    //@spock.lang.IgnoreRest
    void "Test getFeed does not return unedited sprint with empty description"() {
        given: "an untitled sprint with empty description"
        def unedited = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)         
        unedited.description = ""
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        
        println "printing results..."
        print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getFeed does not return unedited sprint with null description"() {
        given: "an untitled sprint with null description"
        def unedited = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC) 
        unedited.description = null
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}    
}

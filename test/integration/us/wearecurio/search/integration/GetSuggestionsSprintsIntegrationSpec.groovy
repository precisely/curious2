package us.wearecurio.search.integration

import us.wearecurio.model.Entry
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class GetSuggestionsSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {

	void "Test getSuggestions returns public sprint with no tag match"() {
		given: "a sprint owned by user1"
        Sprint sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		sprint.description = uniqueName
		Utils.save(sprint, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "sprint is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].type == "spr"
		results.listItems[0].hash == sprint.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test getSuggestions returns public sprint with tag match"() {
		given: "an interest tag for user2"
		String tagText = "MyInterestTag"
		Tag tag = Tag.create(tagText)
		user2.addInterestTag(tag)
		Utils.save(user2, true)
		
		and: "a sprint owned by user1 with tag match in name"
        Sprint sprint = Sprint.create(new Date(), user1, tagText, Visibility.PUBLIC)         
        sprint.description = uniqueName
        Utils.save(sprint, true)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "sprint is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].type == "spr"
		results.listItems[0].hash == sprint.hash
	}	
	
	void "Test getSuggestions returns public sprint with tag match before sprint without tag match"() {
		given: "an interest tag for user2"
		String tagText = "MyInterestTag"
		Tag tag = Tag.create(tagText)
		user2.addInterestTag(tag)
		Utils.save(user2, true)
		
		and: "a sprint owned by user1 with tag match in name"
        Sprint withTag = Sprint.create(new Date(), user1, tagText, Visibility.PUBLIC)         
        withTag.description = uniqueName
        Utils.save(withTag, true)
        
		and: "a sprint owned by user1 without tag match"
        Sprint withoutTag = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		withoutTag.description = uniqueName
		Utils.save(withoutTag, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "2 sprints are returned"
		results.success
		results.listItems.size == 2
		
		and: "the first sprint is the one with tag match"
		results.listItems[0].type == "spr"
		results.listItems[0].hash == withTag.hash
		
		and: "the second sprint is the one without tag match"
		results.listItems[1].type == "spr"
		results.listItems[1].hash == withoutTag.hash
	}	
	
	void "Test getSuggestions does not return private sprint with no tag match"() {
		given: "a private sprint owned by user1"
        Sprint sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PRIVATE)
		sprint.description = uniqueName
		Utils.save(sprint, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "no sprint is returned"
		results.success
		results.listItems.size == 0
	}	
	
	void "Test getSuggestions does not return private sprint with tag match"() {
		given: "an interest tag for user2"
		String tagText = "MyInterestTag"
		Tag tag = Tag.create(tagText)
		user2.addInterestTag(tag)
		Utils.save(user2, true)
		
		and: "a private sprint owned by user1 with tag match in name"
        Sprint sprint = Sprint.create(new Date(), user1, tagText, Visibility.PRIVATE)         
        sprint.description = uniqueName
        Utils.save(sprint, true)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "no sprint is returned"
		results.success
		results.listItems.size == 0
	}	
	
	void "Test getSuggestions does not return followed public sprint with no tag match"() {
		given: "a sprint owned by user1"
        Sprint sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		sprint.description = uniqueName
		Utils.save(sprint, true)
		
		and: "user2 follows the sprint"
		sprint.fetchUserGroup().addReader(user2)
		Utils.save(sprint, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "no sprint is returned"
		results.success
		results.listItems.size == 0
	}
	
	void "Test getSuggestions does not returns followed public sprint with tag match"() {
		given: "an interest tag for user2"
		String tagText = "MyInterestTag"
		Tag tag = Tag.create(tagText)
		user2.addInterestTag(tag)
		Utils.save(user2, true)
		
		and: "a sprint owned by user1 with tag match in name"
        Sprint sprint = Sprint.create(new Date(), user1, tagText, Visibility.PUBLIC)         
        sprint.description = uniqueName
        Utils.save(sprint, true)
		
		and: "user2 follows the sprint"
		sprint.fetchUserGroup().addReader(user2)
		Utils.save(sprint, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "no sprint is returned"
		results.success
		results.listItems.size == 0
	}	
	
	void "Test getSuggestions does not return followed private sprint with no tag match"() {
		given: "a sprint owned by user1"
        Sprint sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PRIVATE)
		sprint.description = uniqueName
		Utils.save(sprint, true)
		
		and: "user2 follows the sprint"
		sprint.fetchUserGroup().addReader(user2)
		Utils.save(sprint, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "no sprint is returned"
		results.success
		results.listItems.size == 0
	}
	
	void "Test getSuggestions does not returns followed private sprint with tag match"() {
		given: "an interest tag for user2"
		String tagText = "MyInterestTag"
		Tag tag = Tag.create(tagText)
		user2.addInterestTag(tag)
		Utils.save(user2, true)
		
		and: "a sprint owned by user1 with tag match in name"
        Sprint sprint = Sprint.create(new Date(), user1, tagText, Visibility.PUBLIC)         
        sprint.description = uniqueName
        Utils.save(sprint, true)
		
		and: "user2 follows the sprint"
		sprint.fetchUserGroup().addReader(user2)
		Utils.save(sprint, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getSuggestions is called for user2"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user2)
		
		then: "no sprint is returned"
		results.success
		results.listItems.size == 0
	}	
	
	void "Test getSuggestions does not return private sprint with no tag match"() {
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
	@spock.lang.Ignore
    void "Test getSuggestions does not return unedited sprint with empty description"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with empty string for description, but tag match in name"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = ""
        Utils.save(unedited, true)
        
		when: "getSuggestions is called"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
    
    //@spock.lang.IgnoreRest
	@spock.lang.Ignore
    void "Test getSuggestions does not return unedited sprint with null name"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with null description, but tag match in name"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = null
        Utils.save(unedited, true)
        
		when: "getSuggestions is called"
		def results = searchService.getSuggestions(SearchService.SPRINT_TYPE, user1)
        println "printing results..."
        print(results)
        
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
}

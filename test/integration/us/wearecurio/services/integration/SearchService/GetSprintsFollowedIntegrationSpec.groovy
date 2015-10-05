package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint

class GetSprintsFollowedIntegrationSpec extends SearchServiceIntegrationSpecBase {
	
    void "Test single sprint owned"() {
		when: "a new sprint is created"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsFollowed returns the sprint"
		def results = searchService.getSprintsFollowed(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "spr"
		results.listItems[0].id == sprint.id.toString()
		results.listItems[0].name == sprint.name
    }
	
    void "Test single sprint followed"() {
		given: "a new sprint created by user1"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "user2 follows that sprint"
		sprint.addReader(user2.id)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsFollowed returns the sprint"
		def results = searchService.getSprintsFollowed(user2)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "spr"
		results.listItems[0].id == sprint.id.toString()
		results.listItems[0].name == sprint.name
    }
	
	void "Test multiple sprints owned"() {
		when: "a new sprint is created"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "another sprint is created"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)

		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsFollowed returns both sprints" 
		def results = searchService.getSprintsFollowed(user1)
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "spr" && 
			it.id == sprint1.id.toString() && 
			it.name == sprint1.name 
		} != null
		results.listItems.find{ 
			it.type == "spr" && 
			it.id == sprint2.id.toString() && 
			it.name == sprint2.name 
		} != null
	}
	
	void "Test multiple sprints followed"() {
		given: "a new sprint created by user1"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "a new sprint created by user2"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "user3 follows both sprints"
		sprint1.addReader(user3.id)
		sprint2.addReader(user3.id)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsFollowed returns both sprints"
		def results = searchService.getSprintsFollowed(user3)
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "spr" && 
			it.id == sprint1.id.toString() && 
			it.name == sprint1.name 
		} != null
		results.listItems.find{ 
			it.type == "spr" && 
			it.id == sprint2.id.toString() && 
			it.name == sprint2.name 
		} != null
	}
	
	void "Test followed user's followed sprint is not returned"() {
		given: "a new sprint created by user1"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "user2 follows user1"
		user1.follow(user2)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsFollowed does not return any sprints for user2"
		def results1 = searchService.getSprintsFollowed(user2)
		results1.success
		results1.listItems.size() == 0
		
		and: "getSprintsFollowed returns sprint for user"
		def results2 = searchService.getSprintsFollowed(user1)
		results2.success
		results2.listItems.size() == 1
		results2.listItems[0].type == "spr"
		results2.listItems[0].id == sprint.id.toString()
		results2.listItems[0].name == sprint.name
	}
}

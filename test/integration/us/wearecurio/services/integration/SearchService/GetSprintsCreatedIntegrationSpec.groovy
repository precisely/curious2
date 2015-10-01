package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint

class GetSprintsCreatedIntegrationSpec extends SearchServiceIntegrationSpecBase {
	void "Test single"() {
		given: "a sprint created by user"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsCreated returns sprint created by user"
		def results = searchService.getSprintsCreated(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "act-created-spr"
		results.listItems[0].objectId == sprint.id
		results.listItems[0].objectDescription == sprint.name
	}
	
	void "Test multiple"() {
		given: "a sprint created by user"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		 
		and: "another sprint created by user"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsCreated returns sprint created by user"
		def results = searchService.getSprintsCreated(user1)
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "act-created-spr" && 
			it.objectId == sprint1.id && 
			it.objectDescription == sprint1.name 
		} != null
		results.listItems.find{ 
			it.type == "act-created-spr" &&
			it.objectId == sprint2.id && 
			it.objectDescription == sprint2.name 
		} != null
	}
	
	void "Test sprint created by followed user is not returned"() {
		given: "user2 following user1"
		user1.follow(user2)
		
		and: "a sprint created by user1"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		and: "a sprint created by user2"
		def sprint2 = Sprint.create(currentTime, user2, getUniqueName(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintsCreated only returns sprint created by user2"
		def results = searchService.getSprintsCreated(user2)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "act-created-spr"
		results.listItems[0].objectId == sprint2.id
		results.listItems[0].objectDescription == sprint2.name
	}
}

package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint

import us.wearecurio.services.SearchService

class GetActivitySprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    Sprint sprint1
    Sprint sprint2
    Sprint sprint3
    Discussion discussion1
    Discussion discussion2
    Discussion discussion3
    
	def setup() {
        sprint1 = Sprint.create(new Date(), user1, "$uniqueName", Visibility.PUBLIC)
        sprint2 = Sprint.create(new Date(), user1, "$uniqueName", Visibility.PUBLIC)
        sprint3 = Sprint.create(new Date(), user1, "$uniqueName", Visibility.PUBLIC)
        
        sprint1.addWriter(user2.id)
        sprint2.addWriter(user2.id)
        sprint3.addWriter(user2.id)
		
        discussion1 = Discussion.create(user2, "$uniqueName", sprint1.fetchUserGroup(), new Date(), Visibility.PUBLIC)
		discussion2 = Discussion.create(user2, "$uniqueName", sprint2.fetchUserGroup(), new Date(), Visibility.PUBLIC)
		discussion3 = Discussion.create(user2, "$uniqueName", sprint3.fetchUserGroup(), new Date(), Visibility.PUBLIC)
        
        discussion1.createPost(user2, "$uniqueName", (new Date()) - 2)
        discussion2.createPost(user2, "$uniqueName", new Date() - 1)
        discussion3.createPost(user2, "$uniqueName", new Date())
        
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")        
    }
    
	void "Test getActivity for sprints"() {
		when: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
		
        then: "all three sprints a returned"
        results.success
		results.listItems.size() == 3
        results.listItems.find{it.type == "spr" && it.hash == sprint1.hash}
        results.listItems.find{it.type == "spr" && it.hash == sprint2.hash}
        results.listItems.find{it.type == "spr" && it.hash == sprint3.hash}        
	}
	
	void "Test offset for getActivity for sprints"() {
		when: "getActivity is called with offset"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1, 1)

        then: "middle sprint (sprint2) is the first result"        
		results.success
		
        then: "all three sprints a returned"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint2.hash
	}
	
	void "Test max for getActivity for sprints"() {
		when: "getActivity is called with max"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1, 0, 1)

        then: "only most recent sprint (sprint3) is returned"        
		results.success
		results.listItems.size() == 1
		
		and: "single result is sprint3"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint3.hash
	}
	
	void "Test max and offset for getActivity for sprints"() {
		when: "getActivity is called with max and offset"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1, 1, 1)

        then: "only middle sprint (sprint2) is returned"        
		results.success
		results.listItems.size() == 1
		
		and: "single result is sprint2"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint2.hash
	}
	
	void "Test order for getActivity for sprints"() {
		when: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)

        then: "middle sprint (sprint2) is the first result"        
		results.success
		results.listItems.size() == 3
		
		and: "results are in correct order"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint3.hash
		results.listItems[1].type == "spr"
        results.listItems[1].hash == sprint2.hash
		results.listItems[2].type == "spr"
        results.listItems[2].hash == sprint1.hash
	}
}

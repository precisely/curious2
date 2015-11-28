package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint

import us.wearecurio.services.SearchService

class GetActivitySprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {
	
	void "Test getActivity for sprints"() {
		given: "a sprint owned by user1"
        def sprint = Sprint.create(new Date(), user1, "$uniqueName", Visibility.PUBLIC)
        
        and: "user2 is a writer to the group"
        sprint.addWriter(user2.id)

		and: "a discussion created by user2 and associated with the sprint"
		def discussion = Discussion.create(user2, "$uniqueName", sprint.fetchUserGroup(), new Date(), Visibility.PUBLIC)
        
		and: "a post made to the dicussion"
        discussion.createPost(user2, "$uniqueName")
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity returns 1 result"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
		results.success
		results.listItems.size() == 1
		
		and: "results includes the sprint"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint.hash
	}
	
	void "Test offset for getActivity for sprints"() {
	}
	
	void "Test max for getActivity for sprints"() {
	}
	
	void "Test max and offset for getActivity for sprints"() {
	}
	
	void "Test order for getActivity for sprints"() {
	}
}

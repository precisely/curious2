package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint


class GetAllCreatedIntegrationSpec extends SearchServiceIntegrationSpecBase {
	void "Test discussion, discussion post and sprint created"() {
		given: "discussion created by user"
		def discussion = Discussion.create(user1, getUniqueName())

		and: "discussion post created by user"
		def post = discussion.createPost(user1, getUniqueName())
		
		and: "sprint created by user"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getDiscussionsCreated returns discussion created by user"
		def results = searchService.getAllCreated(user1)
		results.success
		results.listItems.size() == 3
		results.listItems.find{
			it.type == "act-created-dis" &&
			it.objectId == discussion.id && 
			it.objectDescription == discussion.name
		} != null
		results.listItems.find{
			it.type == "act-created-spr" &&
			it.objectId == sprint.id &&
			it.objectDescription == sprint.name
		} != null
		results.listItems.find{
			it.type == "act-created-post" &&
			it.objectId == post.id && 
			it.objectDescription == post.message
		} != null		
	}
}

package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.UserGroup
import us.wearecurio.model.Sprint

class GetAllFollowedIntegrationSpec extends SearchServiceIntegrationSpecBase {
	
	void "Test followed discussions, sprints and users are returned"() {
		given: "a new user group"
		def group = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(), null)
		
		and: "user1 is a writer to that group"
		group.addWriter(user1)
		
		and: "a new discussion created by user1 for that group"
		def discussion = Discussion.create(user1, getUniqueName(), group)
		
		and: "a new sprint created by user1"
		def sprint = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		
		when: "user2 added as a reader to discussion user group"
		group.addReader(user2)
		
		and: "user2 is added as reader to the sprint"
		sprint.addReader(user2.id)
		
		and: "user2 follows user3"
		user3.follow(user2)
		println ""
		println "user3.virtualUserGroupIdFollowers: " + user3.virtualUserGroupIdFollowers
		println "group.id: " + group.id
		println "sprint.virtualGroupId: " + sprint.virtualGroupId
		println ""
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getAllFollowed for user2 returns discussion, sprint and user3"
		def results = searchService.getAllFollowed(user2)
		results.success
		results.listItems.size() == 3
		results.listItems.find{
			it.type == "dis"
			it.id == discussion.id
			it.name == discussion.name
		} != null
		results.listItems.find{
			it.type == "spr"
			it.id == sprint.id
			it.name == sprint.name
		} != null
		results.listItems.find{
			it.type == "usr"
			it.id == user3.id
			it.name == user3.name
		} != null
	}
}

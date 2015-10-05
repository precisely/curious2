package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

class GetUsersFollowedIntegrationSpec extends SearchServiceIntegrationSpecBase {

    void "Test single user followed"() {
		when: "user2 follows user1"
		user1.follow(user2)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getUsersFollowed returns user1 for user2"
		def results = searchService.getUsersFollowed(user2)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "usr"
		results.listItems[0].id == user1.id.toString()
		results.listItems[0].name == user1.name
    }
	
	void "Test multiple users followed"() {
		when: "user3 follows user1"
		user1.follow(user3)
		
		and: "user3 follows user2"
		user2.follow(user3)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getUsersFollowed returns user1 and user2 for user3"
		def results = searchService.getUsersFollowed(user3)
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "usr" && 
			it.id == user1.id.toString() && 
			it.name == user1.name 
		} != null
		results.listItems.find{ 
			it.type == "usr" &&
			it.id == user2.id.toString() && 
			it.name == user2.name 
		} != null
	}
	
	void "Test user followed by followed user does not return user followed by followed user"() {
		when: "user3 follows user1"
		user1.follow(user3)
		
		and: "user1 follows user2"
		user2.follow(user1)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getUsersFollowed for user3 returns user1 but not user2"
		def results = searchService.getUsersFollowed(user3)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "usr"
		results.listItems[0].id == user1.id.toString()
		results.listItems[0].name == user1.name
	}
}

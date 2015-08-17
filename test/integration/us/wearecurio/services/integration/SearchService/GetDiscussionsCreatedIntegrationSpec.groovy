package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion

class GetDiscussionsCreatedIntegrationSpec extends SearchServiceIntegrationSpecBase {
	void "Test single"() {
		given: "discussion created by user"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getDiscussionsCreated returns discussion created by user"
		def results = searchService.getDiscussionsCreated(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].id == discussion.id
		results.listItems[0].name == discussion.name
	}
	
	void "Test multiple for multiple users"() {
		given: "discussion1 created by user1 and discussion2 created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user2, getUniqueName())
		
		when: "elasticsearch service is indexed and getDiscussionsCreated is called for user1"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsCreated(user1)
		
		then: "only discussion1 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems[0].id == discussion1.id
		results.listItems[0].name == discussion1.name

		when: "getDiscussionsCreated is called for user2"
		results = searchService.getDiscussionsCreated(user2)
		
		then: "only discussion2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems[0].id == discussion2.id
		results.listItems[0].name == discussion2.name
	}
	
	void "Test with offset"() {
		given: "two discussions created by user1 and one discussion created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user2, getUniqueName())

		when: "elasticsearch service is indexed and getDiscussionsCreated is called for user1 with offset 0 and max > 2"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsCreated(user1, 0, 3)
		
		then: "both discussion1 and discussion2 are returned"
		results.success
		results.listItems.size() == 2
		results.listItems.find{ it -> it.id == discussion1.id && it.name == discussion1.name }
		results.listItems.find{ it -> it.id == discussion2.id && it.name == discussion2.name }
		
		when: "getDiscussionsCreated is called for user1 with offset 1 and max > 2"
		results = searchService.getDiscussionsCreated(user1, 1, 3)
		
		then: "only discussion1 or discussion2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems.find{ it -> (it.id == discussion1.id && it.name == discussion1.name) || (it.id == discussion2.id && it.name == discussion2.name) }

		when: "getDiscussionsCreated is called for user1 with offset > 1 and max > 2"
		results = searchService.getDiscussionsCreated(user1, 2, 3)
		
		then: "no discussions are returned"
		results.success
		results.listItems.size() < 1
	}
	
	void "Test with max"() {
		given: "two discussions created by user1 and one discussion created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user2, getUniqueName())
		
		when: "elasticsearch service is indexed and getDiscussionsCreated for user1 is called with offset 0 and max 0"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsCreated(user1, 0, 0)
		
		then: "no dsicussions are returned"
		results.success
		results.listItems.size() < 1
		
		when: "getDiscussionsCreated is called for user1 with offset 0 and max 1"
		results = searchService.getDiscussionsCreated(user1, 0, 1)
		
		then: "only discussion1 or discussion2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems.find{ it -> (it.id == discussion1.id && it.name == discussion1.name) || (it.id == discussion2.id && it.name == discussion2.name) }

		when: "getDiscussionsCreated is called for user1 with offset 0 and max 2"
		results = searchService.getDiscussionsCreated(user1, 0, 2)
		
		then: "both discussion1 and discussion2 are returned"
		results.success
		results.listItems.size() == 2
		results.listItems.find{ it -> it.id == discussion1.id && it.name == discussion1.name }
		results.listItems.find{ it -> it.id == discussion2.id && it.name == discussion2.name }
		
		when: "getDiscussionsCreated is called for user1 with offset 0 and max > 2"
		results = searchService.getDiscussionsCreated(user1, 0, 3)
		
		then: "both discussion1 and discussion2 are returned"
		results.success
		results.listItems.size() == 2
		results.listItems.find{ it -> it.id == discussion1.id && it.name == discussion1.name }
		results.listItems.find{ it -> it.id == discussion2.id && it.name == discussion2.name }
	}
	
	void "Test with offset and max"() {
		given: "three discussions created by user1 and two discussions created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user1, getUniqueName())
		def discussion4 = Discussion.create(user2, getUniqueName())
		def discussion5 = Discussion.create(user2, getUniqueName())

		when: "elasticsearch service is indexed and getDiscussionsCreated is called for user1 with offset 1 and max 1"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsCreated(user1, 1, 1)
		
		then: "only discussion1 or discussion2 or discussion3 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems.find{ it -> 
			(it.id == discussion1.id && it.name == discussion1.name) || 
			(it.id == discussion2.id && it.name == discussion2.name) ||
			(it.id == discussion3.id && it.name == discussion3.name)
		}
	}
	
	void "Test unique for unique offset values with max 1"() {
		given: "three discussions created by user1 and two discussions created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user1, getUniqueName())
		def discussion4 = Discussion.create(user2, getUniqueName())
		def discussion5 = Discussion.create(user2, getUniqueName())

		when: "elasticsearch service is indexed and getDiscussionsCreated is called for user1 with different offset values"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results01 = searchService.getDiscussionsCreated(user1, 0, 1)
		def results11 = searchService.getDiscussionsCreated(user1, 1, 1)
		def results21 = searchService.getDiscussionsCreated(user1, 2, 1)
	
		then: "results01 is either discussion1, discussion2 or discussion3"
		results01.success
		results01.listItems.size() == 1
		results01.listItems.find{ it -> 
			(it.id == discussion1.id && it.name == discussion1.name) || 
			(it.id == discussion2.id && it.name == discussion2.name) ||
			(it.id == discussion3.id && it.name == discussion3.name)
		}
		
		and: "results11 is either discussion1, discussion2 or discussion3"
		results11.success
		results11.listItems.size() == 1
		results11.listItems.find{ it ->
			(it.id == discussion1.id && it.name == discussion1.name) ||
			(it.id == discussion2.id && it.name == discussion2.name) ||
			(it.id == discussion3.id && it.name == discussion3.name)
		}

		and: "results21 is either discussion1, discussion2 or discussion3"
		results21.success
		results21.listItems.size() == 1
		results21.listItems.find{ it ->
			(it.id == discussion1.id && it.name == discussion1.name) ||
			(it.id == discussion2.id && it.name == discussion2.name) ||
			(it.id == discussion3.id && it.name == discussion3.name)
		}

		and: "none of the discussions in results01, results11 and results21 are the same"
		results01[0].id != results11[0].id
		results01[0].id != results21[0].id
		results11[0].id != results21[0].id
	}	
}

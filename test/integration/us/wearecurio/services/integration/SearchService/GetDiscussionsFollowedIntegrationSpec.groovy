package us.wearecurio.services.integration.SearchService

import us.wearecurio.model.Discussion
import us.wearecurio.model.UserGroup

class GetDiscussionsFollowedIntegrationSpec extends SearchServiceIntegrationSpecBase {

	void "Test single"() {
		given: "discussion followed by owner"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getDiscussionsFollowed returns discussion followed by user1"
		def results = searchService.getDiscussionsFollowed(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
	}

	void "Test multiple users for single"() {
		given: "discussion followed by user1 and user2"
		def discussion = Discussion.create(user1, getUniqueName())
		def userGroup = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(), null)
		userGroup.addWriter(user1)
		userGroup.addDiscussion(discussion)
		userGroup.addReader(user2)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getDiscussionsFollowed called for user1 returns discussion followed by user1"
		def results = searchService.getDiscussionsFollowed(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name

		when: "getDiscussionsFollowed called for user2"
		results = searchService.getDiscussionsFollowed(user2)
		def readerGroups = UserGroup.getGroupsForReader(user2.id)
		
		then: "discussion followed by user2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
	}
	
	void "Test multiple for multiple users"() {
		given: "discussion1 followed by user1 and discussion2 followed by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user2, getUniqueName())

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getDiscussionsFollowed called for user1 only returns discussion1"
		def results = searchService.getDiscussionsFollowed(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion1.id.toString()
		results.listItems[0].name == discussion1.name
		
		when: "getDiscussionsFollowed called for user2"
		results = searchService.getDiscussionsFollowed(user2)
		
		then: "only discussion2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion2.id.toString()
		results.listItems[0].name == discussion2.name
	}
	
	void "Test removed user"() {
		given: "discussion followed by user1 and user2"
		def discussion = Discussion.create(user1, getUniqueName())
		def userGroup = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(), null)
		userGroup.addWriter(user1)
		userGroup.addDiscussion(discussion)
		userGroup.addReader(user2)
		
		when: "elasticsearch service is indexed and getDiscussionsFollowed called for user2"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsFollowed(user2)
				
		then: "discussion followed by user2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "dis"
		results.listItems[0].id == discussion.id.toString()
		results.listItems[0].name == discussion.name
		
		when: "user2 is removed from discussion and getDiscussionsFollowed called for user2"
		userGroup.removeReader(user2)
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		results = searchService.getDiscussionsFollowed(user2)

		then: "no discussions returned"
		results.success
		results.listItems.size() < 1
	}
	
	void "Test with offset"() {
		given: "two discussions followed by user1 and two discussions followed by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user2, getUniqueName())
		def discussion4 = Discussion.create(user2, getUniqueName())

		when: "elasticsearch service is indexed and getDiscussionsFollowed is called for user1 with offset 0 and max > 2"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsFollowed(user1, 0, 3)
		
		then: "both discussion1 and discussion2 are returned"
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "dis" && 
			it.id == discussion1.id.toString() && 
			it.name == discussion1.name
		} != null
		results.listItems.find{ 
			it.type == "dis" && 
			it.id == discussion2.id.toString() && 
			it.name == discussion2.name
		} != null
		
		when: "getDiscussionsFollowed is called for user1 with offset 1 and max > 2"
		results = searchService.getDiscussionsFollowed(user1, 1, 3)
		
		then: "discussion1 or discussion2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems.find{ 
			(it.type == "dis" && it.id == discussion1.id.toString() && it.name == discussion1.name) || 
			(it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name)
		} != null

		when: "getDiscussionsFollowed is called for user1 with offset > 1 and max > 2"
		results = searchService.getDiscussionsFollowed(user1, 2, 3)
		
		then: "no discusions are returned"
		results.success
		results.listItems.size() < 1
	}
	
	void "Test with max"() {
		given: "two discusions created by user1 and one discussion created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user2, getUniqueName())
		
		when: "elasticsearch service is indexed and getDiscussionsFollowed is called for user1 with offset 0 and max 0"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsFollowed(user1, 0, 0)
		
		then: "no discussions are returned"
		results.success
		results.listItems.size() < 1
		
		when: "getDiscussionsFollowed is called for user1 with offset 0 and max 1"
		results = searchService.getDiscussionsFollowed(user1, 0, 1)
		
		then: "only discussion1 or discussion2 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems.find{ 
			(it.type == "dis" && it.id == discussion1.id.toString() && it.name == discussion1.name) || 
			(it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name)
		} != null

		when: "getDiscussionsFollowed is called for user1 with offset 0 and max 2"
		results = searchService.getDiscussionsFollowed(user1, 0, 2)
		
		then: "both discussion1 and discussion2 are returned"
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "dis" && 
			it.id == discussion1.id.toString() && 
			it.name == discussion1.name
		} != null
		results.listItems.find{ 
			it.type == "dis" && 
			it.id == discussion2.id.toString() && 
			it.name == discussion2.name 
		} != null
		
		when: "getDiscussionsFollowed is called for user1 with offset 0 and max > 2"
		results = searchService.getDiscussionsFollowed(user1, 0, 3)
		
		then: "both discussion1 and discussion2 are returned"
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "dis" && 
			it.id == discussion1.id.toString() && 
			it.name == discussion1.name 
		} != null
		results.listItems.find{ 
			it.type == "dis" && 
			it.id == discussion2.id.toString()	 && 
			it.name == discussion2.name 
		} != null
	}

	void "Test with offset and max"() {
		given: "three discussions created by user1 and two discussions created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user1, getUniqueName())
		def discussion4 = Discussion.create(user2, getUniqueName())
		def discussion5 = Discussion.create(user2, getUniqueName())

		when: "elasticsearch service is indexed and getDiscussionsFollowed is called for user1 with offset 1 and max 1"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results = searchService.getDiscussionsFollowed(user1, 1, 1)
		
		then: "only discussion1 or discussion2 or discussion3 is returned"
		results.success
		results.listItems.size() == 1
		results.listItems.find{
			(it.type == "dis" && it.id == discussion1.id.toString() && it.name == discussion1.name) ||
			(it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name) ||
			(it.type == "dis" && it.id == discussion3.id.toString() && it.name == discussion3.name)
		} != null
	}
	
	void "Test unique for unique offset values with max 1"() {
		given: "three discussions created by user1 and two discussions created by user2"
		def discussion1 = Discussion.create(user1, getUniqueName())
		def discussion2 = Discussion.create(user1, getUniqueName())
		def discussion3 = Discussion.create(user1, getUniqueName())
		def discussion4 = Discussion.create(user2, getUniqueName())
		def discussion5 = Discussion.create(user2, getUniqueName())

		when: "elasticsearch service is indexed and getDiscussionsFollowed is called for user1 with different offset values"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		def results01 = searchService.getDiscussionsFollowed(user1, 0, 1)
		def results11 = searchService.getDiscussionsFollowed(user1, 1, 1)
		def results21 = searchService.getDiscussionsFollowed(user1, 2, 1)
	
		then: "results01 is either discussion1, discussion2 or discussion3"
		results01.success
		results01.listItems.size() == 1
		results01.listItems.find{
			(it.type == "dis" && it.id == discussion1.id.toString() && it.name == discussion1.name) ||
			(it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name) ||
			(it.type == "dis" && it.id == discussion3.id.toString() && it.name == discussion3.name)
		} != null
		
		and: "results11 is either discussion1, discussion2 or discussion3"
		results11.success
		results11.listItems.size() == 1
		results11.listItems.find{
			(it.type == "dis" && it.id == discussion1.id.toString() && it.name == discussion1.name) ||
			(it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name) ||
			(it.type == "dis" && it.id == discussion3.id.toString() && it.name == discussion3.name)
		} != null

		and: "results21 is either discussion1, discussion2 or discussion3"
		results21.success
		results21.listItems.size() == 1
		results21.listItems.find{
			(it.type == "dis" && it.id == discussion1.id.toString() && it.name == discussion1.name) ||
			(it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name) ||
			(it.type == "dis" && it.id == discussion3.id.toString() && it.name == discussion3.name)
		} != null

		and: "none of the discussions in results01, results11 and results21 are the same"
		results01.listItems[0].id != results11.listItems[0].id
		results01.listItems[0].id != results21.listItems[0].id
		results11.listItems[0].id != results21.listItems[0].id
	}
}
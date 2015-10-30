package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Tag
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class GetActivityDiscussionsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    void "Test getActivity for discussions"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)

		and: "user2 with matching interest tags"
		def tag2 = Tag.create(tagText)
		user2.addInterestTag(tag2)
		Utils.save(user2, true)
		
		and: "discussion1 with matching interest tags"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		Utils.save(discussion1, true)

		and: "discussion2 owned by user without matching interest tags"
		def discussion2 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion2, true)

		and: "post made to discussion2"
		def post = discussion2.createPost(user3, getUniqueName())
		Utils.save(post, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity returns 1 result"
		def results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1)
		results.success
		results.listItems.size() == 1
		
		and: "results includes discussion with post (discussion2)"
		results.listItems.find{ it.type == "dis" && it.id == discussion2.id && it.name == discussion2.name }
    }
	
	void "Test offset for getActivity for discussions"() {
		given: "discussion1"
		def now = new Date()
		def discussion1 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion1, true)

		and: "discussion2"
		def discussion2 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion2, true)

		and: "discussion3"
		def discussion3 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion3, true)
		println "discussion1.hash: $discussion1.hash, discussion2.hash: $discussion2.hash, discussion3.hash: $discussion3.hash"
		
		and: "post1 made to discussion1"
		def post1 = discussion1.createPost(user1, getUniqueName(), now)
		Utils.save(post1, true)

		and: "post2 made to discussion2"
		def post2 = discussion2.createPost(user2, getUniqueName(), now -  1)
		Utils.save(post2, true)

		and: "post3 made to discussion1"
		def post3 = discussion3.createPost(user3, getUniqueName(), now - 2)
		Utils.save(post3, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity with offset 0 returns 3 results "
		def results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 0)
		results.success
		results.listItems.size() == 3
		
		and: "first result is discussion with most recent post (discussion1)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion1.hash
		results.listItems[0].name == discussion1.name
		
		and: "second result is discussion with second most recent post (discussion2)"
		results.listItems[1].type == "dis"
		results.listItems[1].hash == discussion2.hash
		results.listItems[1].name == discussion2.name
		
		and: "third result is discussion with third most recent post (discussion3)"
		results.listItems[2].type == "dis"
		results.listItems[2].hash == discussion3.hash
		results.listItems[2].name == discussion3.name
		
		when: "getActivity is called with offset 1 returns 2 results "
		results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 1)
		results.success
		results.listItems.size() == 2
		
		then: "first result is discussion with second most recent post (discussion2)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion2.hash
		results.listItems[0].name == discussion2.name
		
		and: "second result is discussion with third most recent post (discussion3)"
		results.listItems[1].type == "dis"
		results.listItems[1].hash == discussion3.hash
		results.listItems[1].name == discussion3.name
		
		when: "getActivity is called with offset 2 returns 1 results "
		results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 2)
		results.success
		results.listItems.size() == 1
		
		then: "first result is discussion with third most recent post (discussion2)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion3.hash
		results.listItems[0].name == discussion3.name
		
		when: "getActivity is called with offset 3"
		results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 3)
		
		then: "0 results are returned"
		results.success
		results.listItems.size() == 0
	}
	
	void "Test max for getActivity for discussions"() {
		given: "discussion1"
		def now = new Date()
		def discussion1 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion1, true)

		and: "discussion2"
		def discussion2 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion2, true)

		and: "discussion3"
		def discussion3 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion3, true)
		println "discussion1.hash: $discussion1.hash, discussion2.hash: $discussion2.hash, discussion3.hash: $discussion3.hash"
		
		and: "post1 made to discussion1"
		def post1 = discussion1.createPost(user1, getUniqueName(), now)
		Utils.save(post1, true)

		and: "post2 made to discussion2"
		def post2 = discussion2.createPost(user2, getUniqueName(), now -  1)
		Utils.save(post2, true)

		and: "post3 made to discussion1"
		def post3 = discussion3.createPost(user3, getUniqueName(), now - 2)
		Utils.save(post3, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity with max 10 returns all discussions (3)"
		def results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 0, 10)
		results.success
		results.listItems.size() == 3
		
		and: "first result is discussion with most recent post (discussion1)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion1.hash
		results.listItems[0].name == discussion1.name
		
		and: "second result is discussion with second most recent post (discussion2)"
		results.listItems[1].type == "dis"
		results.listItems[1].hash == discussion2.hash
		results.listItems[1].name == discussion2.name
		
		and: "third result is discussion with third most recent post (discussion3)"
		results.listItems[2].type == "dis"
		results.listItems[2].hash == discussion3.hash
		results.listItems[2].name == discussion3.name
		
		when: "getActivity with max 3 returns all discussions (3)"
		results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 0, 3)
		results.success
		results.listItems.size() == 3
		
		then: "first result is discussion with most recent post (discussion1)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion1.hash
		results.listItems[0].name == discussion1.name
		
		and: "second result is discussion with second most recent post (discussion2)"
		results.listItems[1].type == "dis"
		results.listItems[1].hash == discussion2.hash
		results.listItems[1].name == discussion2.name
		
		and: "third result is discussion with third most recent post (discussion3)"
		results.listItems[2].type == "dis"
		results.listItems[2].hash == discussion3.hash
		results.listItems[2].name == discussion3.name
		
		when: "getActivity is called with max 2 returns 2 results"
		results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 0, 2)
		results.success
		results.listItems.size() == 2
		
		then: "first result is discussion with most recent post (discussion1)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion1.hash
		results.listItems[0].name == discussion1.name
		
		and: "second result is discussion with second most recent post (discussion2)"
		results.listItems[1].type == "dis"
		results.listItems[1].hash == discussion2.hash
		results.listItems[1].name == discussion2.name
		
		when: "getActivity is called with max 1 returns 1 result"
		results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 0, 1)
		results.success
		results.listItems.size() == 1
		
		then: "first result is discussion with third most recent post (discussion1)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion1.hash
		results.listItems[0].name == discussion1.name
		
		when: "getActivity is called with max 0"
		results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 0, 0)
		
		then: "0 results are returned"
		results.success
		results.listItems.size() == 0
	}
	
	void "Test offset and max for getActivity for discussions"() {
		given: "discussion1"
		def now = new Date()
		def discussion1 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion1, true)

		and: "discussion2"
		def discussion2 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion2, true)

		and: "discussion3"
		def discussion3 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion3, true)
		println "discussion1.hash: $discussion1.hash, discussion2.hash: $discussion2.hash, discussion3.hash: $discussion3.hash"
		
		and: "post1 made to discussion1"
		def post1 = discussion1.createPost(user1, getUniqueName(), now)
		Utils.save(post1, true)

		and: "post2 made to discussion2"
		def post2 = discussion2.createPost(user2, getUniqueName(), now -  1)
		Utils.save(post2, true)

		and: "post3 made to discussion1"
		def post3 = discussion3.createPost(user3, getUniqueName(), now - 2)
		Utils.save(post3, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity with offset 1 and max 1 returns one discussion"
		def results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1, 1, 1)
		results.success
		results.listItems.size() == 1
		
		and: "first result is discussion with second most recent post (discussion2)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion2.hash
		results.listItems[0].name == discussion2.name
	}
	
	void "Test order for getActivity for discussions"() {
		given: "discussion1"
		def now = new Date()
		def discussion1 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion1, true)

		and: "discussion2"
		def discussion2 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion2, true)

		and: "discussion3"
		def discussion3 = Discussion.create(user1, getUniqueName())
		Utils.save(discussion3, true)
		println "discussion1.hash: $discussion1.hash, discussion2.hash: $discussion2.hash, discussion3.hash: $discussion3.hash"
		
		and: "post1 made to discussion1"
		def post1 = discussion1.createPost(user1, getUniqueName(), now)
		Utils.save(post1, true)

		and: "post2 made to discussion2"
		def post2 = discussion2.createPost(user2, getUniqueName(), now -  1)
		Utils.save(post2, true)

		and: "post3 made to discussion1"
		def post3 = discussion3.createPost(user3, getUniqueName(), now - 2)
		Utils.save(post3, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity returns 3 results "
		def results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1)
		results.success
		results.listItems.size() == 3
		
		and: "first result is discussion with most recent post (discussion1)"
		results.listItems[0].type == "dis"
		results.listItems[0].hash == discussion1.hash
		results.listItems[0].name == discussion1.name
		
		and: "second result is discussion with second most recent post (discussion2)"
		results.listItems[1].type == "dis"
		results.listItems[1].hash == discussion2.hash
		results.listItems[1].name == discussion2.name
		
		and: "third result is discussion with third most recent post (discussion3)"
		results.listItems[2].type == "dis"
		results.listItems[2].hash == discussion3.hash
		results.listItems[2].name == discussion3.name
	}
}

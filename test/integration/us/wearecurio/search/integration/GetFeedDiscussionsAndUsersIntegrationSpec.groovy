package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class GetFeedDiscussionsAndUsersIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//@spock.lang.IgnoreRest
	void "Test getFeed for discussions and users"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user1.addInterestTag(tag)
		Utils.save(user1, true)
		
		and: "a discusison (d1) with name matching tag"
		def d1 = Discussion.create(user2, "$tagText $uniqueName")

		and: "another discussion (d2) without matching tag"
		def d2 = Discussion.create(user2, uniqueName)
		
		and: "post with matching tag made to d2"
        def post = d2.createPost(user3, "$tagText $uniqueName")
        Utils.save(post, true)
		
		and: "another discussion (d3) without tag match"
		def d3 = Discussion.create(user3, uniqueName)
		
		and: "user1 follows d3"
		d3.addFollower(user1.id)
		
		and: "post without tag match made to d3"
        post = d3.createPost(user3, uniqueName)
        Utils.save(post, true)
		
		and: "another discussion (d4) without tag match"
		def d4 = Discussion.create(user3, uniqueName, null, new Date(), Visibility.PRIVATE)
		
		and: "post without tag match made to d4"
        post = d4.createPost(user3, uniqueName)
        Utils.save(post, true)
				
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1"
		def results = searchService.getFeed(SearchService.USER_TYPE | SearchService.DISCUSSION_TYPE, user1)
		
		then: "5 results returned"
		results.success
		results.listItems.size == 5
		
		and: "discussion with tag match suggested (d1)"
		results.listItems.find { it.type == "dis" && it.hash == d1.hash }
								
		and: "discussion with post with tag match suggested (d2)"
		results.listItems.find { it.type == "dis" && it.hash == d2.hash }
								
		and: "discussion followed by no tag match has post activity (d3)"
		results.listItems.find { it.type == "dis" && it.hash == d3.hash }
								
		and: "discussion not followed and now tag match is not returned"
		results.listItems.find { it.type == "dis" && it.hash == d4.hash } == null
								
		and: "other users are returned (user1 and user2 created in setup)"
		results.listItems.find{ it.type == "usr" && it.hash == user2.hash }
		results.listItems.find{ it.type == "usr" && it.hash == user3.hash }
	}
	
	//@spock.lang.IgnoreRest
	void "Test offset for getFeed for discussions and users"() {
		given: "a discusison"
		def d = Discussion.create(user2, uniqueName)
		
		and: "user1 follows discussion"
		d.addFollower(user1.id)
		
		and: "post with matching tag made to followed discussion"
        def post = d.createPost(user3, uniqueName)
        Utils.save(post, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1 with offset"
		def results = searchService.getFeed(SearchService.USER_TYPE | SearchService.DISCUSSION_TYPE, user1, 1)
		
		then: "2 results returned"
		results.success
		results.listItems.size == 2
				
		and: "discussion is not in results (activity goes before suggestions)"
		results.listItems.find{it.type == "dis" && it.hash == d1.hash} == null
		
		and: "user2 or user3 (both suggestions)"
		results.listItems.find{it.type == "usr" && it.hash == user2.hash}
		results.listItems.find{it.type == "usr" && it.hash == user3.hash}			
	}
	
	void "Test max for getFeed for discussions and users"() {
	}
	
	//@spock.lang.IgnoreRest
	void "Test 3 suggestions included when activity meets max"() {
		given: "3 discussions"
		def d1 = Discussion.create(user2, uniqueName)
		def d2 = Discussion.create(user2, uniqueName)
		def d3 = Discussion.create(user2, uniqueName)
		
		and: "user1 follows the discussions"
		d1.addFollower(user1.id)
		d2.addFollower(user1.id)
		d3.addFollower(user1.id)
		
		and: "the discussions have activity"
        def post = d1.createPost(user3, uniqueName)
        Utils.save(post, true)
        post = d2.createPost(user3, uniqueName)
        Utils.save(post, true)
        post = d3.createPost(user3, uniqueName)
        Utils.save(post, true)
		
		and: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user1.addInterestTag(tag)
		Utils.save(user1, true)
		
		and: "two more discussions with tag match"
		def d_suggestion1 = Discussion.create(user2, tagText, null, new Date(), Visibility.PUBLIC)
		def d_suggestion2 = Discussion.create(user2, tagText, null, new Date(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1 with max of 3"
		def results = searchService.getFeed(SearchService.USER_TYPE | SearchService.DISCUSSION_TYPE, user1, 0, 3)
		
		then: "6 results returned"
		results.success
		results.listItems.size == 6
		
		and: "3 results are discussions with activity"
		results.listItems.count{
			it.type == "dis" &&
				(it.hash == d1.hash) ||
				(it.hash == d2.hash) ||
				(it.hash == d3.hash) } == 3
		
		and: "3 results are discussions with activity or other users"
		results.listItems.count{
			(
				it.type == "dis" &&
				(it.hash == d_suggestion1.hash) ||
				(it.hash == d_suggestion2.hash)
			) || (
				it.type == "usr" &&
				  (it.hash == user2.hash) ||
				  (it.hash == user3.hash)
			)
		} == 3		
	}
	
	//@spock.lang.IgnoreRest
	void "Test suggestions fill to max when not enough activity"() {
		given: "a discusison"
		def d = Discussion.create(user2, uniqueName)
		
		and: "user1 follows discussion"
		d.addFollower(user1.id)
		
		and: "post made to discussion"
        Utils.save(d.createPost(user3, uniqueName), true)
		
		and: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user1.addInterestTag(tag)
		Utils.save(user1, true)
		
		and: "a discussion with tag match"
		def d_suggestion = Discussion.create(user2, tagText, null, new Date(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1 with offset and max"
		def results = searchService.getFeed(SearchService.USER_TYPE | SearchService.DISCUSSION_TYPE, user1, 1, 3)
		
		then: "3 results returned"
		results.success
		results.listItems.size == 3
		
		and: "all results are suggestions"
		results.listItems.count{
			((it.type == "dis") && (it.hash == d_suggestion.hash)) || (
			(it.type == "usr") &&
				(it.hash == user2.hash) ||
				(it.hash == user3.hash))
		} == 3
	}
	
	//@spock.lang.IgnoreRest
	void "Test 3 suggestions included even when that brings total beyond max"() {
		given: "a discusison"
		def d = Discussion.create(user2, uniqueName)
		
		and: "user1 follows discussion"
		d.addFollower(user1.id)
		
		and: "post made to discussion"
        Utils.save(d.createPost(user3, uniqueName), true)
		
		and: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user1.addInterestTag(tag)
		Utils.save(user1, true)
		
		and: "two more discussions with tag match"
		def d_suggestion1 = Discussion.create(user2, tagText, null, new Date(), Visibility.PUBLIC)
		def d_suggestion2 = Discussion.create(user2, tagText, null, new Date(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1 with offset and max"
		def results = searchService.getFeed(SearchService.USER_TYPE | SearchService.DISCUSSION_TYPE, user1, 1, 1)
		
		then: "3 results returned"
		results.success
		results.listItems.size == 3
		
		and: "all results are suggestions"
		results.listItems.count{
			(
				(it.type == "dis") && 
			 		(it.hash == d_suggestion1.hash) ||
			 		(it.hash == d_suggestion2.hash)
			 ) || (
				(it.type == "usr") &&
				(it.hash == user2.hash) ||
				(it.hash == user3.hash)
			)
		} == 3
	}
	
	void "Test sessionId for getFeed for discussions and users"() {
	}

	void "Test suggestionOffset for getFeed for discussions and users"() {
	}
	
	void "Test offset and suggestionOffset for getFeed for discussions and users"() {
	}
	
	void "Test max and suggestionOffset for getFeed for discussions and users"() {
	}
	
}

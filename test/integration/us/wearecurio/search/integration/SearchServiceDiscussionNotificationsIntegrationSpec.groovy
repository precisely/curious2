package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost

import us.wearecurio.services.SearchService

class SearchServiceDiscussionNotificationsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    def setup() {
    }

    def cleanup() {
    }

    void "Test notification count returns 0 for no posts on owned discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for owner"
        def count = searchService.getNotificationCount(user1, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 0
    }
    
    void "Test notification count returns 1 for single post on owned discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "a new post"
        discussion.createPost(user2, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for owner"
        def count = searchService.getNotificationCount(user1, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 1
    }
    
    void "Test notification count returns 1 for two posts on owned discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "a new post"
        discussion.createPost(user2, uniqueName)
        
        and: "another new post"
        discussion.createPost(user3, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for user"
        def count = searchService.getNotificationCount(user1, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 1
    }
    
    void "Test notification count returns 0 for no posts on followed discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called by follower"
        def count = searchService.getNotificationCount(user2, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 0
    }
    
    void "Test notification count returns 1 for single post on followed discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "a new post"
        discussion.createPost(user1, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def count = searchService.getNotificationCount(user2, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 1
    }
    
    void "Test notification count returns 1 for two posts on followed discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "a new post"
        discussion.createPost(user1, uniqueName)
        
        and: "another new post"
        discussion.createPost(user3, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def count = searchService.getNotificationCount(user2, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 1
    }
    
    void "Test notification count returns 0 for no posts on unfollowed discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "user2 stops following discussion"
        discussion.removeFollower(user2.id)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called by follower"
        def count = searchService.getNotificationCount(user2, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 0
    }
    
    void "Test notification count returns 0 for single post on unfollowed discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "user2 stops following discussion"
        discussion.removeFollower(user2.id)
        
        and: "a new post"
        discussion.createPost(user1, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def count = searchService.getNotificationCount(user2, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 0
    }
    
    void "Test notification count returns 0 for two posts on unfollowed discussion"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "user2 stops following discussion"
        discussion.removeFollower(user2.id)
        
        and: "a new post"
        discussion.createPost(user1, uniqueName)
        
        and: "another new post"
        discussion.createPost(user3, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def count = searchService.getNotificationCount(user2, SearchService.DISCUSSION_TYPE)
        
        then: "the result is 1"
        count == 0
    }
    
	//@spock.lang.IgnoreRest
    void "Test get notification does not return followed discussion without post"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 0
    }
        
    void "Test get notification returns followed discussion with one post"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)

        and: "a new post"
        discussion.createPost(user1, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion.hash
    }
        
    void "Test get notification returns followed discussion with two posts"() {
        given: "a new discussion"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "a new post"
        discussion.createPost(user1, uniqueName)
        
        and: "another new post"
        discussion.createPost(user3, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion.hash
    }
        
    void "Test get notification returns followed discussions with posts but not followed discussion without post"() {
        given: "a new discussion"
        Discussion discussion1 = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion1.addFollower(user2.id)
        
        and: "a new discussion owned by user2"
        Discussion discussion2 = Discussion.create(user2, uniqueName)        
        
        and: "a new post"
        discussion1.createPost(user1, uniqueName)
        
        and: "another new post"
        discussion1.createPost(user3, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion1.hash
    }
        
    void "Test get notification returns multiple followed and owned discussions with posts"() {
        given: "discussions owned by user1 and user3"
        Discussion discussion1 = Discussion.create(user1, uniqueName)
        Discussion discussion2 = Discussion.create(user3, uniqueName)
        
        and: "discussions owned by user2"
        Discussion discussion3 = Discussion.create(user2, uniqueName)
        Discussion discussion4 = Discussion.create(user2, uniqueName)
        
        and: "user2 follows discussion1 and discussion2"
        discussion1.addFollower(user2.id)
        discussion2.addFollower(user2.id)
        
        and: "a new posts to all discussions"
        discussion1.createPost(user2, uniqueName)
        discussion2.createPost(user3, uniqueName)
        discussion3.createPost(user1, uniqueName)
        discussion4.createPost(user3, uniqueName)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 4
        result.listItems.find{ it.type == "dis" && it.hash == discussion1.hash}
        result.listItems.find{ it.type == "dis" && it.hash == discussion2.hash}
        result.listItems.find{ it.type == "dis" && it.hash == discussion3.hash}
        result.listItems.find{ it.type == "dis" && it.hash == discussion4.hash}
    }
}

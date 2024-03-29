package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility

import us.wearecurio.services.SearchService

class SearchServiceDiscussionNotificationsIntegrationSpec extends SearchServiceIntegrationSpecBase {

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
        
        and: "getNotifications is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 0
    }
     
    //@spock.lang.IgnoreRest
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
        
        and: "getNotifications is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion.hash
    }
     
    //@spock.lang.IgnoreRest
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
        
        and: "getNotifications is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion.hash
    }
    
    //@spock.lang.IgnoreRest
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
        
        and: "getNotifications is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion is the only result"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion1.hash
    }
    
    //@spock.lang.IgnoreRest
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
        
        and: "new posts to all discussions"
        discussion1.createPost(user2, uniqueName)
        discussion2.createPost(user3, uniqueName)
        discussion3.createPost(user1, uniqueName)
        discussion4.createPost(user3, uniqueName)
    
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "discussion's 1 through 4 are all in results"
        result.success
        result.listItems != null
        result.listItems.size == 4
        result.listItems.find{ it.type == "dis" && it.hash == discussion1.hash}
        result.listItems.find{ it.type == "dis" && it.hash == discussion2.hash}
        result.listItems.find{ it.type == "dis" && it.hash == discussion3.hash}
        result.listItems.find{ it.type == "dis" && it.hash == discussion4.hash}
    }
    
    //@spock.lang.IgnoreRest
    void "Test followed discussions with post returned from get discussions marked as new"() {
        given: "discussion owned by user1"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "a new posts to all discussions"
        discussion.createPost(user3, uniqueName)
    
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for follower"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE)
        
        then: "isNew is true for discussion"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion.hash
        result.listItems[0].isNew == true
    }
    
    //@spock.lang.IgnoreRest
    void "Test owned discussions with post returned from get discussions marked as new"() {
        given: "discussion owned by user1"
        Discussion discussion = Discussion.create(user1, uniqueName)
        
        and: "a new post to discussion"
        discussion.createPost(user3, uniqueName)
    
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotificationCount is called for owner"
        def result = searchService.getNotifications(user1, SearchService.DISCUSSION_TYPE)
        
        then: "isNew is true for discussion"
        result.success
        result.listItems != null
        result.listItems.size == 1
        result.listItems[0].type == "dis"
        result.listItems[0].hash == discussion.hash
        result.listItems[0].isNew == true
    }
    
    //@spock.lang.IgnoreRest
    void "Test isNew flag off for old and on for new after reset notification check date to current"() {
        given: "discussions owned by user1"
        Discussion discussionWithOldPost = Discussion.create(user1, uniqueName)
        Discussion discussionWithNewPost = Discussion.create(user1, uniqueName)
        
        when: "a new post is created before checkdate is reset"
        discussionWithOldPost.createPost(user2, uniqueName)
        sleep(2000)
        
        and: "resetNotificationsCheckDate called with default of current date"
        searchService.resetNotificationsCheckDate(user1)
        sleep(2000)
        
        and: "a new post is created after checkdate is reset"
        discussionWithNewPost.createPost(user3, uniqueName)
    
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for owner"
        def result = searchService.getNotifications(user1, SearchService.DISCUSSION_TYPE)
        
        then: "isNew is false for discussionWithOldPost"
        result.success
        result.listItems != null
        result.listItems.size == 2
        result.listItems.find{
            it.type == "dis" && 
            it.hash == discussionWithOldPost.hash && 
            it.isNew==false
        }
        
        and: "isNew is true for discussionWithNewPost"
        result.listItems.find{
            it.type == "dis" && 
            it.hash == discussionWithNewPost.hash && 
            it.isNew==true
        }
    }
    
    //@spock.lang.IgnoreRest
    void "Test isNew flag off for old and on for new after reset notification check date to specific date"() {
        given: "a specified checkDate"
        Date checkDate = (new Date()) - 4
        
        and: "discussions owned by user1"
        Discussion discussionWithOldPost = Discussion.create(user1, uniqueName)
        Discussion discussionWithNewPost = Discussion.create(user1, uniqueName)
        
        when: "resetNotificationsCheckDate called for specified checkDate"
        searchService.resetNotificationsCheckDate(user1, SearchService.DISCUSSION_TYPE, checkDate)
        
        and: "a new post is created before specified checkDate"
        discussionWithOldPost.createPost(user2, uniqueName, checkDate - 1)
        
        and: "a new post is created after specified checkDate"
        discussionWithNewPost.createPost(user3, uniqueName, checkDate + 1)
    
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for owner"
        def result = searchService.getNotifications(user1, SearchService.DISCUSSION_TYPE)
        
        then: "isNew is false for discussionWithOldPost"
        result.success
        result.listItems != null
        result.listItems.size == 2
        result.listItems.find{
            it.type == "dis" && 
            it.hash == discussionWithOldPost.hash && 
            it.isNew==false
        }
        
        and: "isNew is true for discussionWithNewPost"
        result.listItems.find{
            it.type == "dis" && 
            it.hash == discussionWithNewPost.hash && 
            it.isNew==true
        }
    }
    
    //@spock.lang.IgnoreRest
    void "Test checkDate for one user does not affect check date for other user"() {
        given: "a specified checkDate"
        Date checkDate = (new Date()) - 4
        
        and: "discussions owned by user1"
        Discussion discussionWithOldPost = Discussion.create(user1, uniqueName)
        Discussion discussionWithNewPost = Discussion.create(user1, uniqueName)
        
        when: "resetNotificationsCheckDate called for other user with specified checkDate"
        searchService.resetNotificationsCheckDate(user2, SearchService.DISCUSSION_TYPE, checkDate)
        
        and: "a new post is created before specified checkdate"
        discussionWithOldPost.createPost(user2, uniqueName, checkDate - 1)
        
        and: "a new post is created after specified checkdate"
        discussionWithNewPost.createPost(user3, uniqueName, checkDate + 1)
    
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for owner"
        def result = searchService.getNotifications(user1, SearchService.DISCUSSION_TYPE)
        
        then: "isNew is true for discussionWithOldPost and discussionWithNewPost"
        result.success
        result.listItems != null
        result.listItems.size == 2
        result.listItems.find{
            it.type == "dis" && 
            it.hash == discussionWithOldPost.hash && 
            it.isNew==true
        }
        result.listItems.find{
            it.type == "dis" && 
            it.hash == discussionWithNewPost.hash && 
            it.isNew==true
        }
    }
	
	//@spock.lang.IgnoreRest
	void "Test getNewNotificationCount for single user"() {
        given: "a specified checkDate"
        Date checkDate = (new Date()) - 4
        
        and: "discussions owned by user1"
        Discussion discussionWithOldPost = Discussion.create(user1, uniqueName)
        Discussion discussionWithNewPost = Discussion.create(user1, uniqueName)
        
        when: "resetNotificationsCheckDate called for specified checkDate"
        searchService.resetNotificationsCheckDate(user1, SearchService.DISCUSSION_TYPE, checkDate)
        
        and: "a new post is created before specified checkDate"
        discussionWithOldPost.createPost(user2, uniqueName, checkDate - 1)
        
        and: "a new post is created after specified checkDate"
        discussionWithNewPost.createPost(user3, uniqueName, checkDate + 1)
    
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        then: "getNewNotificationCount returns 1 for user1"
        searchService.getNewNotificationCount(user1, SearchService.DISCUSSION_TYPE) == 1        
	}
	
	//@spock.lang.IgnoreRest
    void "Test checkDate for one user does not affect new notification count for other user"() {
        given: "a specified checkDate"
        Date checkDate = (new Date()) - 4
        
        and: "discussions owned by user1"
        Discussion discussionWithOldPost = Discussion.create(user1, uniqueName)
        Discussion discussionWithNewPost = Discussion.create(user1, uniqueName)
        
        when: "resetNotificationsCheckDate called for other user with specified checkDate"
        searchService.resetNotificationsCheckDate(user2, SearchService.DISCUSSION_TYPE, checkDate)
        
        and: "a new post is created before specified checkdate"
        discussionWithOldPost.createPost(user2, uniqueName, checkDate - 1)
        
        and: "a new post is created after specified checkdate"
        discussionWithNewPost.createPost(user3, uniqueName, checkDate + 1)
    
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        then: "getNewNotificationCount returns 2 for owner"
        searchService.getNewNotificationCount(user1, SearchService.DISCUSSION_TYPE) == 2        
    }
	
	//@spock.lang.IgnoreRest
	void "Test getNewNotificationCount for multiple users"() {
        given: "a specified checkDate for user1"
        Date checkDateUser1 = (new Date()) - 8
        
        and: "a specified checkDate for user2"
        Date checkDateUser2 = (new Date()) - 4
		
        and: "discussions owned by user1"
        Discussion discussionWithOldPostUser1 = Discussion.create(user1, uniqueName)
        Discussion discussionWithNewPostUser1 = Discussion.create(user1, uniqueName)
        
        and: "discussions owned by user2"
        Discussion discussionWithOldPostUser2 = Discussion.create(user2, uniqueName)
        Discussion discussionWithNewPostUser2 = Discussion.create(user2, uniqueName)
		
        when: "resetNotificationsCheckDate called for user1's checkDate"
        searchService.resetNotificationsCheckDate(user1, SearchService.DISCUSSION_TYPE, checkDateUser1)
        
        and: "resetNotificationsCheckDate called for user2's checkDate"
        searchService.resetNotificationsCheckDate(user2, SearchService.DISCUSSION_TYPE, checkDateUser2)
        
        and: "a new post is created before user1's checkDate"
        discussionWithOldPostUser1.createPost(user2, uniqueName, checkDateUser1 - 1)
        
        and: "a new post is created after user1's checkDate but before user2's checkdate"
        discussionWithNewPostUser1.createPost(user3, uniqueName, checkDateUser1 + 1)
    
        and: "a new post is created before user2's checkDate but after user1's checkdate"
        discussionWithOldPostUser2.createPost(user1, uniqueName, checkDateUser2 - 1)
        
        and: "a new post is created after user2's checkDate"
        discussionWithNewPostUser2.createPost(user3, uniqueName, checkDateUser2 + 1)
    
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        then: "getNewNotificationCount returns 1 for user1"
        searchService.getNewNotificationCount(user1, SearchService.DISCUSSION_TYPE) == 1
		
        and: "getNewNotificationCount returns 1 for user2"
        searchService.getNewNotificationCount(user2, SearchService.DISCUSSION_TYPE) == 1
	}
	
	//@spock.lang.IgnoreRest
	void "Test getNewNotification with offset"() {
		given: "a date in the past"
		Date d = (new Date()) - 7
		
        and: "discussions owned by user1 and user3"
        Discussion discussion1 = Discussion.create(user1, uniqueName, null, d, Visibility.PUBLIC)
        Discussion discussion2 = Discussion.create(user3, uniqueName, null, d, Visibility.PUBLIC)
        
        and: "discussions owned by user2"
        Discussion discussion3 = Discussion.create(user2, uniqueName, null, d, Visibility.PUBLIC)
        Discussion discussion4 = Discussion.create(user2, uniqueName, null, d, Visibility.PUBLIC)
        
        and: "user2 follows discussion1 and discussion2"
        discussion1.addFollower(user2.id)
        discussion2.addFollower(user2.id)
        
        and: "new posts to all discussions"
        discussion1.createPost(user2, uniqueName, d + 1)
        discussion2.createPost(user3, uniqueName, d + 2)
        discussion3.createPost(user1, uniqueName, d + 3)
        discussion4.createPost(user3, uniqueName, d + 4)
    
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for follower, with offset of 1"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE, 1)

		then: "first result is discussion with second most recent post (discussion3)"
        result.success
        result.listItems != null
        result.listItems.size == 3
        result.listItems[0].hash == discussion3.hash
        result.listItems[1].hash == discussion2.hash
        result.listItems[2].hash == discussion1.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test getNewNotification with max"() {
		given: "a date in the past"
		Date d = (new Date()) - 7
		
        and: "discussions owned by user1 and user3"
        Discussion discussion1 = Discussion.create(user1, uniqueName, null, d, Visibility.PUBLIC)
        Discussion discussion2 = Discussion.create(user3, uniqueName, null, d, Visibility.PUBLIC)
        
        and: "discussions owned by user2"
        Discussion discussion3 = Discussion.create(user2, uniqueName, null, d, Visibility.PUBLIC)
        Discussion discussion4 = Discussion.create(user2, uniqueName, null, d, Visibility.PUBLIC)
        
        and: "user2 follows discussion1 and discussion2"
        discussion1.addFollower(user2.id)
        discussion2.addFollower(user2.id)
        
        and: "new posts to all discussions"
        discussion1.createPost(user2, uniqueName, d + 1)
        discussion2.createPost(user3, uniqueName, d + 2)
        discussion3.createPost(user1, uniqueName, d + 3)
        discussion4.createPost(user3, uniqueName, d + 4)
    
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for follower, with max of 2"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE, 0, 2)

		then: "two most recent discussions are returned (discussion4 and discussion3)"
        result.success
        result.listItems != null
        result.listItems.size == 2
        result.listItems[0].hash == discussion4.hash
        result.listItems[1].hash == discussion3.hash
	}

	//@spock.lang.IgnoreRest
	void "Test getNewNotification with offset and max"() {
		given: "a date in the past"
		Date d = (new Date()) - 7
		
        and: "discussions owned by user1 and user3"
        Discussion discussion1 = Discussion.create(user1, uniqueName, null, d, Visibility.PUBLIC)
        Discussion discussion2 = Discussion.create(user3, uniqueName, null, d, Visibility.PUBLIC)
        
        and: "discussions owned by user2"
        Discussion discussion3 = Discussion.create(user2, uniqueName, null, d, Visibility.PUBLIC)
        Discussion discussion4 = Discussion.create(user2, uniqueName, null, d, Visibility.PUBLIC)
        
        and: "user2 follows discussion1 and discussion2"
        discussion1.addFollower(user2.id)
        discussion2.addFollower(user2.id)
        
        and: "new posts to all discussions"
        discussion1.createPost(user2, uniqueName, d + 1)
        discussion2.createPost(user3, uniqueName, d + 2)
        discussion3.createPost(user1, uniqueName, d + 3)
        discussion4.createPost(user3, uniqueName, d + 4)
    
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getNotifications is called for follower, with offset of 1"
        def result = searchService.getNotifications(user2, SearchService.DISCUSSION_TYPE, 1, 2)

		then: "first result is discussion with second and third most recent posts (discussion3 and discussion2)"
        result.success
        result.listItems != null
        result.listItems.size == 2
        result.listItems[0].hash == discussion3.hash
        result.listItems[1].hash == discussion2.hash
	}
}

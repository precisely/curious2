package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.UserGroup

class GetDiscussionPostsCreatedIntegrationSpec extends SearchServiceIntegrationSpecBase {

    void "Test single post to owned discussion"() {
		given: "a new discussion created by user1"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "user1 adds a post"
		def post = discussion.createPost(user1, getUniqueName())
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionPostCreated returns the post"
		def results = searchService.getDiscussionPostsCreated(user1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "act-created-post"
		results.listItems[0].objectId == post.id
		results.listItems[0].objectDescription == post.message
    }
	
	void "Test single post to followed discussion"() {
		given: "a new user group"
		UserGroup group = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(),
			[isReadOnly:false, defaultNotify:false])

		and: "user1 is a writer of the group"
		group.addWriter(user1)
		
		and: "a new discussion created by user1"
		def discussion = Discussion.create(user1, getUniqueName(), group)
				
		and: "user2 is a reader and writer of discussion group"
		group.addReader(user2)
		group.addWriter(user2)
		
		when: "user2 adds a post"
		def post = discussion.createPost(user2, getUniqueName())
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionPostCreated returns post"
		def results = searchService.getDiscussionPostsCreated(user2)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "act-created-post"
		results.listItems[0].objectId == post.id
		results.listItems[0].objectDescription == post.message
	}
	
	void "Test multiple posts to owned discussion"() {
		given: "a new discussion created by user1"
		def discussion = Discussion.create(user1, getUniqueName())
		
		when: "user1 adds a post"
		def post1 = discussion.createPost(user1, getUniqueName())
		
		and: "user1 adds a second post"
		def post2 = discussion.createPost(user1, getUniqueName())

		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionPostCreated returns both posts"
		def results = searchService.getDiscussionPostsCreated(user1)
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "act-created-post" && 
			it.objectId == post1.id && 
			it.objectDescription == post1.message 
		} != null
		results.listItems.find{ 
			it.type == "act-created-post" && 
			it.objectId == post2.id && 
			it.objectDescription == post2.message 
		} != null		
	}
	
	void "Test multiple posts to followed discussion"() {
		given: "a new user group"
		UserGroup group = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(),
			[isReadOnly:false, defaultNotify:false])

		and: "user1 is a writer of the group"
		group.addWriter(user1)
		
		and: "a new discussion created by user1"
		def discussion = Discussion.create(user1, getUniqueName(), group)
				
		and: "user2 is a reader and writer of discussion group"
		group.addReader(user2)
		group.addWriter(user2)
		
		when: "user2 adds a post"
		def post1 = discussion.createPost(user2, getUniqueName())
		
		and: "user2 adds a second post"
		def post2 = discussion.createPost(user2, getUniqueName())

		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionPostCreated returns both posts"
		def results = searchService.getDiscussionPostsCreated(user2)
		results.success
		results.listItems.size() == 2
		results.listItems.find{ 
			it.type == "act-created-post" && 
			it.objectId == post1.id && 
			it.objectDescription == post1.message 
		} != null
		results.listItems.find{ 
			it.type == "act-created-post" && 
			it.objectId == post2.id && 
			it.objectDescription == post2.message 
		} != null		
	}
	
	void "Test user3 does not see posts by user1 and user2"() {
		given: "a new user group"
		UserGroup group = UserGroup.create(getUniqueName(), getUniqueName(), getUniqueName(),
			[isReadOnly:false, defaultNotify:false])

		and: "user1 is a writer of the group"
		group.addWriter(user1)
		
		and: "a new discussion created by user1"
		def discussion = Discussion.create(user1, getUniqueName(), group)
				
		and: "user2 is a reader and writer of discussion group"
		group.addReader(user2)
		group.addWriter(user2)
		
		when: "user1 posts to discussion"
		def post1 = discussion.createPost(user1, getUniqueName())
		
		and: "user2 posts to discussion"
		def post2 = discussion.createPost(user2, getUniqueName())
		
		and: "user3 posts to discussion"
		def post3 = discussion.createPost(user3, getUniqueName())
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionPost for user3 only returns posts by user3"
		def results = searchService.getDiscussionPostsCreated(user3)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "act-created-post"
		results.listItems[0].objectId == post3.id
		results.listItems[0].objectDescription == post3.message
	}
}

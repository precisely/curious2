package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint

import us.wearecurio.services.SearchService

import us.wearecurio.utility.Utils

class SprintDiscussionsInFeedIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//@spock.lang.IgnoreRest
    void "Test feed includes followed sprint's non-followed discussion without post"() {
        given: "a new sprint"
        def sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)

        and: "the sprint usergroup"
        def group = sprint.fetchUserGroup()
        
        and: "user2 is a writer to sprint group"
        group.addWriter(user2)
        
        and: "a new discussion associated with sprint"
        def discussion = Discussion.create(user2, uniqueName, group, new Date(), Visibility.PUBLIC)
        
        when: "user3 follows sprint"
        sprint.addReader(user3.id)
        Utils.save(sprint, true)
        
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getFeed is called for user3"
        def results = searchService.getFeed(SearchService.DISCUSSION_TYPE, user3)
        
        then: "discussion shows up in result"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == discussion.hash}
    }
    
    void "Test feed includes followed sprint's non-followed discussion with post shows based on creation date"() {
        given: "a new sprint"
        def sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
        
        and: "the sprint usergroup"
        def group = sprint.fetchUserGroup()
        
        and: "user2 is a writer to sprint group"
        group.addWriter(user2)
        
        and: "a new discussion associated with sprint"
        Date old = (new Date()) - 3
        def discussion1 = Discussion.create(user2, uniqueName, group, old, Visibility.PUBLIC)
        
        and: "another new discussion associated with sprint created at a later date than first discussion"
        def discussion2 = Discussion.create(user2, uniqueName, group, old + 1, Visibility.PUBLIC)
        
        when: "user3 follows sprint"
        sprint.addReader(user3.id)
        Utils.save(sprint, true)
        
        and: "another user posts to the first discussion at later date than second discussion"
        discussion1.createPost(user1, uniqueName, old + 2)
         
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getFeed is called for user3"
        def results = searchService.getFeed(SearchService.DISCUSSION_TYPE, user3)
        println "discussion1.score: ${results.listItems.find{it.type == "dis" && it.hash == discussion1.hash}.score}"
        println "discussion2.score: ${results.listItems.find{it.type == "dis" && it.hash == discussion2.hash}.score}"
        
        then: "more recent second discussion shows has lower index than first discussion"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == discussion1.hash}
        results.listItems.find{it.type == "dis" && it.hash == discussion2.hash}
        results.listItems.findIndexOf{it.type == "dis" && it.hash == discussion1.hash} > 
            results.listItems.findIndexOf{it.type == "dis" && it.hash == discussion2.hash}
    }
    
    void "Test feed includes followed sprint's followed discussion with post based on post date"() {
        given: "a new sprint"
        def sprint = Sprint.create(new Date(), user1, "$uniqueName", Visibility.PUBLIC)
        
        and: "the sprint usergroup"
        def group = sprint.fetchUserGroup()
        
        and: "user2 is a writer to sprint group"
        group.addWriter(user2)
        
        and: "a new discussion associated with sprint"
        Date old = (new Date()) - 3
        def discussion1 = Discussion.create(user2, uniqueName, group, old, Visibility.PUBLIC)
        
        and: "another new discussion associated with sprint created at a later date than first discussion"
        def discussion2 = Discussion.create(user2, uniqueName, group, old + 1, Visibility.PUBLIC)
        
        when: "user3 follows sprint"
        sprint.addReader(user3.id)
        Utils.save(sprint, true)
        
        and: "user3 follows discussion1"
        discussion1.addFollower(user3.id)
        Utils.save(discussion1, true)
        
        and: "another user posts to the first discussion at later date than second discussion"
        discussion1.createPost(user1, uniqueName, old + 2)
         
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
//        println "user1.id: ${user1.id}"
//        println "user2.id: ${user2.id}"
//        println "user3.id: ${user3.id}"
//        println "discussion1.hasRecentPost: ${discussion1.hasRecentPost}"
//        println "discussion1.followers: ${discussion1.followers}"
//        println "discussion1.recentPostCreated: ${discussion1.recentPostCreated}"
//        println "discussion1.created: ${discussion1.created}"
//        println "discussion2.hasRecentPost: ${discussion2.hasRecentPost}"
//        println "discussion2.followers: ${discussion2.followers}"
//        println "discussion2.recentPostCreated: ${discussion2.recentPostCreated}"
//        println "discussion2.created: ${discussion2.created}"
        
        and: "getFeed is called for user3"
        def results = searchService.getFeed(SearchService.DISCUSSION_TYPE, user3)
        
        println "discussion1.score: ${results.listItems.find{it.type == "dis" && it.hash == discussion1.hash}.score}"
        println "discussion2.score: ${results.listItems.find{it.type == "dis" && it.hash == discussion2.hash}.score}"
        then: "more recently posted to first discussion, shows lower index than second discussion"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == discussion1.hash}
        results.listItems.find{it.type == "dis" && it.hash == discussion2.hash}
        results.listItems.findIndexOf{it.type == "dis" && it.hash == discussion1.hash} < 
            results.listItems.findIndexOf{it.type == "dis" && it.hash == discussion2.hash}
    }
}

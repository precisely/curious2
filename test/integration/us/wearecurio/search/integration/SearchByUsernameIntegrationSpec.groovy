package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup

import us.wearecurio.utility.Utils

class SearchByUsernameIntegrationSpec extends SearchServiceIntegrationSpecBase {

    void "Test user"() {
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "user2 is returned"
        results.success
        results.listItems.size == 1
        results.listItems[0].type == "usr"
        results.listItems[0].hash == user2.hash
    }
    
    void "Test public sprint"() {
        given: "a sprint created by user2"
        def s = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "sprint is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "spr" && it.hash == s.hash}
    }

    void "Test public discussion"() {
        given: "a discussion created by user2"
        def d = Discussion.create(user2, uniqueName, Visibility.PUBLIC)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "discussion is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == d.hash}
    }

    void "Test discussion post"() {
        given: "a discussion created by user3"
        def d = Discussion.create(user3, uniqueName, Visibility.PUBLIC)
        
        and: "a post by user2"
        d.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "discussion is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == d.hash}
    }
    
    void "Test discussion post to public, non-owned, non-followed sprint"() {
        given: "a public sprint created by user3"
        def s = Sprint.create(new Date(), user3, uniqueName, Visibility.PUBLIC)
        
        and: "user3 has write access"
        s.addWriter(user3.id)

        and: "a non-public dicussion created by user3 associated with the sprint"
        def d = Discussion.create(user3, uniqueName, s.fetchUserGroup(), new Date(), Visibility.UNLISTED)
        
        and: "a post by user2"
        d.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "sprint is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "spr" && it.hash == s.hash}
    }
    
    void "Test discussion post to non-public, non-owned, followed sprint"() {
        given: "a non-public sprint created by user3"
        def s = Sprint.create(new Date(), user3, uniqueName, Visibility.UNLISTED)
        
        and: "user1 follows sprint"
		s.fetchUserGroup().addReader(user1)
		Utils.save(s, true)
        
        and: "user3 has write access"
        s.addWriter(user3.id)
        
        and: "a non-public discussion associated with sprint"
        def d = Discussion.create(user3, uniqueName, s.fetchUserGroup(), new Date(), Visibility.UNLISTED)
        
        and: "a post by user2"
        d.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)        
        
        then: "sprint is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "spr" && it.hash == s.hash}
    }
    
    void "Test discussion post to non-public, owned, non-followed sprint"() {
        given: "a non-public sprint created by user1"
        def s = Sprint.create(new Date(), user1, uniqueName, Visibility.UNLISTED)

        and: "user3 has write access"
        s.addWriter(user3.id)
        
        and: "a non-public discussion associated with sprint"
        def d = Discussion.create(user3, uniqueName, s.fetchUserGroup(), new Date(), Visibility.UNLISTED)
        
        and: "a post by user2"
        d.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")

        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
                
        then: "sprint is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "spr" && it.hash == s.hash}
    }
    
    void "Test discussion post to public, non-owned, non-followed discussion"() {
        given: "a public discussion created by user3"
        def d = Discussion.create(user3, uniqueName, Visibility.PUBLIC)
        
        and: "a post by user2"
        d.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")

        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
                        
        then: "discussion is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == d.hash}
    }
    
    void "Test discussion post to non-public, non-owned, followed discussion"() {
        given: "a user group"
		def g = UserGroup.create(uniqueName, uniqueName, uniqueName, [isReadOnly:false, defaultNotify:false])
        
        and: "user1 is reader of group"
		g.addReader(user1)
        
        and: "user3 is writer of group"
		g.addWriter(user3)
		Utils.save(g, true)
        
        and: "a discussion created by user3 and followed by user1"
        def d = Discussion.create(user3, uniqueName, g, new Date(), Visibility.PUBLIC)
                
        and: "a post by user2"
        d.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")

        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "discussion is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == d.hash}
    }
    
    void "Test discussion post to non-public, owned, non-followed discussion"() {
        given: "a non-public discussion owned by user1"
        def d = Discussion.create(user1, uniqueName, Visibility.UNLISTED)
        
        and: "a post by user2"
        d.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")

        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "discussion is returned"
        results.success
        results.listItems.size > 0
        results.listItems.find{it.type == "dis" && it.hash == d.hash}
    }
    
    void "Test user, discussion, sprint, post to discussion"() {
        given: "a public sprint created by user2"
        def s = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)

        and: "a public discussion created by user2"
        def dUser2 = Discussion.create(user2, uniqueName, Visibility.PUBLIC)
        
        and: "a public discussion created by user3"
        def dUser3 = Discussion.create(user3, uniqueName, Visibility.PUBLIC)
        
        and: "a post made by user2 to user3 discussion"
        dUser3.createPost(user2, uniqueName)
        
        when: "elasticsearch service is indexed"
        elasticSearchService.index()
        elasticSearchAdminService.refresh("us.wearecurio.model_v0")

        and: "searched for user2.username"
        def results = searchService.search(user1, user2.username)
        
        then: "results are returned"
        results.success
        results.listItems.size > 0
        
        then: "user2 is returned"
        results.listItems.find{it.type == "usr" && it.hash == user2.hash}
        
        and: "user2 discussion is returned"
        results.listItems.find{it.type == "dis" && it.hash == dUser2.hash}
        
        and: "sprint is returned"
        results.listItems.find{it.type == "spr" && it.hash == s.hash}
        
        and: "user3 discussion is returned"
        results.listItems.find{it.type == "dis" && it.hash == dUser3.hash}        
    }  
}

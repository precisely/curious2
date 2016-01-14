package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.User

import us.wearecurio.utility.Utils

class ExcludeDeletedItemsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    //@spock.lang.IgnoreRest
    void "Test deindex discussion"() {
		given: "a new discussion"
		Discussion d = Discussion.create(user2, uniqueName, Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = Discussion.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$d.id")
		}
				
		then: "matched discussion is found"
		results.searchResults.size > 0
		results.searchResults[0].hash == d.hash
        
        when: "discussion is deindexed"
		searchService.deindex(d)
        
        and: "search is performed"
		results = Discussion.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$d.id")
		}
        
        then: "no matching discussion is found"
		results.searchResults.size == 0
    }
	
    //@spock.lang.IgnoreRest
    void "Test deindex sprint"() {
		given: "a new sprint"
		Sprint s = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = Sprint.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$s.id")
		}
				
		then: "matched sprint is found"
		results.searchResults.size > 0
		results.searchResults[0].hash == s.hash
        
        when: "sprint is deindexed"
		searchService.deindex(s)
        
        and: "search is performed"
		results = Sprint.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$s.id")
		}
        
        then: "no matching sprint is found"
		results.searchResults.size == 0
    }
	
    //@spock.lang.IgnoreRest
    void "Test deindex user"() {
		given: "a new user"
		User u = User.create(
			[	username:'mckee',
				sex:'M',
				name:'robert mckee',
				email:'robert@story.com',
				birthdate:'01/01/1940',
				password:'robxyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = User.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$u.id")
		}
				
		then: "matched user is found"
		results.searchResults.size > 0
		results.searchResults[0].hash == u.hash
        
        when: "user is deindexed"
		searchService.deindex(u)
        
        and: "search is performed"
		results = User.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$u.id")
		}
        
        then: "no matching users found"
		results.searchResults.size == 0
    }
	
    void "Test deindex discussion does not stick after elasticsearch index"() {
		given: "a new discussion"
		Discussion d = Discussion.create(user2, uniqueName, Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = Discussion.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$d.id")
		}
				
		then: "matched discussion is found"
		results.searchResults.size > 0
		results.searchResults[0].hash == d.hash
        
        when: "discussion is deindexed"
		searchService.deindex(d)
        
		and: "elasticsearch service is indexed without deleting discussion"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
        and: "search is performed"
		results = Discussion.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$d.id")
		}
        
		then: "discussion is reindexed and found"
		results.searchResults.size > 0
		results.searchResults[0].hash == d.hash
    }
	
    //@spock.lang.IgnoreRest
    void "Test deindex sprint does not stick after elasticsearch index"() {
		given: "a new sprint"
		Sprint s = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = Sprint.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$s.id")
		}
				
		then: "matched sprint is found"
		results.searchResults.size > 0
		results.searchResults[0].hash == s.hash
        
        when: "sprint is deindexed"
		searchService.deindex(s)
        
		and: "elasticsearch service is indexed without deleting sprint"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
        and: "search is performed"
		results = Sprint.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$s.id")
		}
        
		then: "sprint is reindexed and found"
		results.searchResults.size > 0
		results.searchResults[0].hash == s.hash
    }
	
    //@spock.lang.IgnoreRest
    void "Test deindex user does not stick after elasticsearch index"() {
		given: "a new user"
		User u = User.create(
			[	username:'mckee',
				sex:'M',
				name:'robert mckee',
				email:'robert@story.com',
				birthdate:'01/01/1940',
				password:'robxyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = User.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$u.id")
		}
				
		then: "matched user is found"
		results.searchResults.size > 0
		results.searchResults[0].hash == u.hash
        
        when: "user is deindexed"
		searchService.deindex(u)
        
		and: "elasticsearch service is indexed without deleting user"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
        and: "search is performed"
		results = User.search(searchType:'query_and_fetch') {
			query_string(query: "_id:$u.id")
		}
                
		then: "user is reindexed and found"
		results.searchResults.size > 0
		results.searchResults[0].hash == u.hash
    }
	
    //@spock.lang.Ignore
    void "Test deleted discussions not included in search results when elastic search not indexed"() {
		given: "a search phrase"
		def searchPhrase = "check magnesium levels"
		
		and: "a new public discussion with searchPhrase in name"
		Discussion d = Discussion.create(user2, "$uniqueTerm $searchPhrase", Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, /"$searchPhrase"/)
		
		then: "matched discussion is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == d.hash
        
        when: "discussion is deleted"
		Discussion.delete(d)
        
        and: "search is performed"
		results = searchService.search(user1, /"$searchPhrase"/)
        
        then: "no matching discussion is found"
		results.success
		results.listItems.size == 0        
    }
    
    //@spock.lang.Ignore
    void "Test deleted sprints not included in search results when elastic search not indexed"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public sprint"
		Sprint s = Sprint.create(new Date(), user2, "sprPublicRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single public sprint is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
        
        when: "sprint is deleted"
        Sprint.delete(s)
  
        and: "search is performed"
		results = searchService.search(user1, searchTerm)        
        
        then: "sprint not in results"
		results.success
		results.listItems.size == 0
    }
	
    //@spock.lang.Ignore
    void "Test deleted users not included in search results when elastic search not indexed"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public sprint"
		User u = User.create(
			[	username:'magnesium44',
				sex:'M',
				name:'robert mckee',
				email:'robert@story.com',
				birthdate:'01/01/1940',
				password:'robxyz',
				action:'doregister',
				controller:'home'	]
		)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single user is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == u.hash
        
        when: "sprint is deleted"
        User.delete(u)
  
        and: "search is performed"
		results = searchService.search(user1, searchTerm)        
        
        then: "user not in results"
		results.success
		results.listItems.size == 0
    }
	
    //@spock.lang.Ignore
    void "Test deleted discussions not included in search results after elastic search indexed"() {
		given: "a search phrase"
		def searchPhrase = "check magnesium levels"
		
		and: "a new public discussion with searchPhrase in name"
		Discussion d = Discussion.create(user2, "$uniqueTerm $searchPhrase", Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, /"$searchPhrase"/)
		
		then: "matched discussion is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == d.hash
        
        when: "discussion is deleted"
		Discussion.delete(d)
  
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "search is performed"
		results = searchService.search(user1, /"$searchPhrase"/)
        
        then: "no matching discussion is found"
		results.success
		results.listItems.size == 0        
    }
    
    //@spock.lang.Ignore
    void "Test deleted sprints not included in search results after elastic search indexed"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public sprint"
		Sprint s = Sprint.create(new Date(), user2, "sprPublicRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single public sprint is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
        
        when: "sprint is deleted"
        Sprint.delete(s)
  
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
  
        and: "search is performed"
		results = searchService.search(user1, searchTerm)        
        
        then: "sprint not in results"
		results.success
		results.listItems.size == 0
    }
	
    //@spock.lang.Ignore
    void "Test deleted users not included in search results after elastic search indexed"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public sprint"
		User u = User.create(
			[	username:'magnesium44',
				sex:'M',
				name:'robert mckee',
				email:'robert@story.com',
				birthdate:'01/01/1940',
				password:'robxyz',
				action:'doregister',
				controller:'home'	]
		)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single user is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == u.hash
        
        when: "sprint is deleted"
        User.delete(u)
  
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

        and: "search is performed"
		results = searchService.search(user1, searchTerm)        
        
        then: "user not in results"
		results.success
		results.listItems.size == 0
    }
}

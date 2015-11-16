package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup1Level2GTLevel3IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {
	
	void "Test owned discussion recent non-first post message match ahead of user name match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentNonFirstPostMessageMatch, CreateType.UserFollowedNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
	}
	
	void "Test owned sprint recent description match ahead of user name match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedRecentDescriptionMatch, CreateType.UserFollowedNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
	}
	
	void "Test followed discussion recent non-first post message match ahead of user name match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentNonFirstPostMessageMatch, CreateType.UserFollowedNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
	}
	
	void "Test followed sprint recent description match ahead of user name match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedRecentDescriptionMatch, CreateType.UserFollowedNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
	}
	
}

package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup1Level4GTLevel5IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	void "Test user tag match ahead of owned discussion old name match"() {	
		given: "two objects"
		createObjectPair(CreateType.UserFollowedTagMatch, CreateType.DisOwnedOldNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash }
	}
	
	void "Test user tag match ahead of followed discussion old name match"() {
		given: "two objects"
		createObjectPair(CreateType.UserFollowedTagMatch, CreateType.DisFollowedOldNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash }
	}
	
	void "Test user tag match ahead of owned discussion old first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.UserFollowedTagMatch, CreateType.DisOwnedOldFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
	}
	
	void "Test user tag match ahead of followed discussion old first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.UserFollowedTagMatch, CreateType.DisFollowedOldFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
	}
	
	void "Test user tag match ahead of owned sprint old name match"() {
		given: "two objects"
		createObjectPair(CreateType.UserFollowedTagMatch, CreateType.SprOwnedOldNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
	}
	
	void "Test user tag match ahead of followed sprint old name match"() {
		given: "two objects"
		createObjectPair(CreateType.UserFollowedTagMatch, CreateType.SprFollowedOldNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
	}
	
}

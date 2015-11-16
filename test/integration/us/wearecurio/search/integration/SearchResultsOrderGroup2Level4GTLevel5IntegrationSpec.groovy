package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup2Level4GTLevel5IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	void "Test user tag match ahead of discussion old name match"() {
		given: "two objects"
		createObjectPair(CreateType.UserNonFollowedTagMatch, CreateType.DisPublicOldNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldNameMatch.hash }
	}
	
	void "Test user tag match ahead of discussion old first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.UserNonFollowedTagMatch, CreateType.DisPublicOldFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldFirstPostMessageMatch.hash }
	}
	
	void "Test user tag match ahead of sprint old name match"() {
		given: "two objects"
		createObjectPair(CreateType.UserNonFollowedTagMatch, CreateType.SprPublicOldNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash }
	}
	
}

package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup2Level2GTLevel3IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	void "Test discussion recent non-first post message match ahead of user name match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicRecentNonFirstPostMessageMatch, CreateType.UserNonFollowedNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
	}
	
	void "Test sprint recent description match ahead of user name match"() {		
		given: "two objects"
		createObjectPair(CreateType.SprPublicRecentDescriptionMatch, CreateType.UserNonFollowedNameMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
	}
	
}

package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup2Level1GTLevel2IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	void "Test discussion recent name match ahead of discussion recent non-first post message match"() {	
		given: "two objects"
		createObjectPair(CreateType.DisPublicRecentNameMatch, CreateType.DisPublicRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test discussion recent name match ahead of sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicRecentNameMatch, CreateType.SprPublicRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
	}
	
	void "Test discussion recent first post message match ahead of discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicRecentFirstPostMessageMatch, CreateType.DisPublicRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test discussion recent first post message match ahead of sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicRecentFirstPostMessageMatch, CreateType.SprPublicRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
	}
	
	void "Test sprint recent name match ahead of discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprPublicRecentNameMatch, CreateType.DisPublicRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test sprint recent name match ahead of sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprPublicRecentNameMatch, CreateType.SprPublicRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
	}	
}

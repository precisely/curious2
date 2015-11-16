package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup2Level5GTLevel6IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	void "Test discussion old name match ahead of discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicOldNameMatch, CreateType.DisPublicOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldNonFirstPostMessageMatch.hash }
	}

	void "Test discussion old name match ahead of sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicOldNameMatch, CreateType.SprPublicOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash }
	}

	void "Test discussion old first post message match ahead of discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicOldFirstPostMessageMatch, CreateType.DisPublicOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldNonFirstPostMessageMatch.hash }
	}

	void "Test discussion old first post message match ahead of sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisPublicOldFirstPostMessageMatch, CreateType.SprPublicOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash }
	}

	void "Test sprint old name match ahead of discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprPublicOldNameMatch, CreateType.DisPublicOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disPublicOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicOldNonFirstPostMessageMatch.hash }
	}

	void "Test sprint old name match ahead of sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprPublicOldNameMatch, CreateType.SprPublicOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash }
	}
}

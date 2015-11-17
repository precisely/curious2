package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup1Level5GTLevel6IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	void "Test owned discussion old name match ahead of owned discussion old non-first post message match"() {	
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldNameMatch, CreateType.DisOwnedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion old name match ahead of followed discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldNameMatch, CreateType.DisFollowedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion old name match ahead of owned sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldNameMatch, CreateType.SprOwnedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
	}
	
	void "Test owned discussion old name match ahead of followed sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldNameMatch, CreateType.SprFollowedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
	}
	
	void "Test owned discussion old first post message match ahead of owned discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldFirstPostMessageMatch, CreateType.DisOwnedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion old first post message match ahead of followed discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldFirstPostMessageMatch, CreateType.DisFollowedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion old first post message match ahead of owned sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldFirstPostMessageMatch, CreateType.SprOwnedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
	}
	
	void "Test owned discussion old first post message match ahead of followed sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedOldFirstPostMessageMatch, CreateType.SprFollowedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
	}
	
	void "Test owned sprint old name match ahead of owned discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedOldNameMatch, CreateType.DisOwnedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned sprint old name match ahead of followed discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedOldNameMatch, CreateType.DisFollowedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned sprint old name match ahead of owned sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedOldNameMatch, CreateType.SprOwnedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
	}
	
	void "Test owned sprint old name match ahead of followed sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedOldNameMatch, CreateType.SprFollowedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
	}
	
	void "Test followed discussion old name match ahead of owned discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldNameMatch, CreateType.DisOwnedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion old name match ahead of followed discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldNameMatch, CreateType.DisFollowedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion old name match ahead of owned sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldNameMatch, CreateType.SprOwnedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
	}
	
	void "Test followed discussion old name match ahead of followed sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldNameMatch, CreateType.SprFollowedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
	}
	
	void "Test followed discussion old first post message match ahead of owned discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldFirstPostMessageMatch, CreateType.DisOwnedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion old first post message match ahead of followed discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldFirstPostMessageMatch, CreateType.DisFollowedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion old first post message match ahead of owned sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldFirstPostMessageMatch, CreateType.SprOwnedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
	}
	
	void "Test followed discussion old first post message match ahead of followed sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedOldFirstPostMessageMatch, CreateType.SprFollowedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
	}
	
	void "Test followed sprint old name match ahead of owned discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedOldNameMatch, CreateType.DisOwnedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed sprint old name match ahead of followed discussion old non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedOldNameMatch, CreateType.DisFollowedOldNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed sprint old name match ahead of owned sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedOldNameMatch, CreateType.SprOwnedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldDescriptionMatch.hash }
	}
	
	void "Test followed sprint old name match ahead of followed sprint old description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedOldNameMatch, CreateType.SprFollowedOldDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
	}	
}

package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup1Level1GTLevel2IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	//@spock.lang.IgnoreRest
	void "Test owned discussion recent name match ahead of owned discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentNameMatch, CreateType.DisOwnedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent name match ahead of followed discussion recent non-first post message match"() {	
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentNameMatch, CreateType.DisFollowedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent name match ahead of owned sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentNameMatch, CreateType.SprOwnedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test owned discussion recent name match ahead of followed sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentNameMatch, CreateType.SprFollowedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of owned discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentFirstPostMessageMatch, CreateType.DisOwnedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of followed discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentFirstPostMessageMatch, CreateType.DisFollowedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of owned sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentFirstPostMessageMatch, CreateType.SprOwnedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of followed sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisOwnedRecentFirstPostMessageMatch, CreateType.SprFollowedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of owned discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedRecentNameMatch, CreateType.DisOwnedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of followed discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedRecentNameMatch, CreateType.DisFollowedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of owned sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedRecentNameMatch, CreateType.SprOwnedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of followed sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprOwnedRecentNameMatch, CreateType.SprFollowedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of owned discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentNameMatch, CreateType.DisOwnedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of followed discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentNameMatch, CreateType.DisFollowedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of owned sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentNameMatch, CreateType.SprOwnedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of followed sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentNameMatch, CreateType.SprFollowedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of owned discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentFirstPostMessageMatch, CreateType.DisOwnedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of followed discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentFirstPostMessageMatch, CreateType.DisFollowedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of owned sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentFirstPostMessageMatch, CreateType.SprOwnedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of followed sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.DisFollowedRecentFirstPostMessageMatch, CreateType.SprFollowedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of owned discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedRecentNameMatch, CreateType.DisOwnedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of followed discussion recent non-first post message match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedRecentNameMatch, CreateType.DisFollowedRecentNonFirstPostMessageMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of owned sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedRecentNameMatch, CreateType.SprOwnedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of followed sprint recent description match"() {
		given: "two objects"
		createObjectPair(CreateType.SprFollowedRecentNameMatch, CreateType.SprFollowedRecentDescriptionMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}	
}

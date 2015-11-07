package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup1Level1GTLevel2IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	def setup() {
		setupGroup1Level1()
		setupGroup1Level2()
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
	}
	
	void "Test owned discussion recent name match ahead of owned discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		
		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } > 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent name match ahead of followed discussion recent non-first post message match"() {	
	}
	
	void "Test owned discussion recent name match ahead of owned sprint recent description match"() {
	}
	
	void "Test owned discussion recent name match ahead of followed sprint recent description match"() {
	}
	
	void "Test owned discussion recent first post message match ahead of owned discussion recent non-first post message match"() {
	}
	
	void "Test owned discussion recent first post message match ahead of followed discussion recent non-first post message match"() {
	}
	
	void "Test owned discussion recent first post message match ahead of owned sprint recent description match"() {
	}
	
	void "Test owned discussion recent first post message match ahead of followed sprint recent description match"() {
	}
	
	void "Test owned sprint recent name match ahead of owned discussion recent non-first post message match"() {
	}
	
	void "Test owned sprint recent name match ahead of followed discussion recent non-first post message match"() {
	}
	
	void "Test owned sprint recent name match ahead of owned sprint recent description match"() {
	}
	
	void "Test owned sprint recent name match ahead of followed sprint recent description match"() {
	}
	
	void "Test followed discussion recent name match ahead of owned discussion recent non-first post message match"() {
	}
	
	void "Test followed discussion recent name match ahead of followed discussion recent non-first post message match"() {
	}
	
	void "Test followed discussion recent name match ahead of owned sprint recent description match"() {
	}
	
	void "Test followed discussion recent name match ahead of followed sprint recent description match"() {
	}
	
	void "Test followed discussion recent first post message match ahead of owned discussion recent non-first post message match"() {
	}
	
	void "Test followed discussion recent first post message match ahead of followed discussion recent non-first post message match"() {
	}
	
	void "Test followed discussion recent first post message match ahead of owned sprint recent description match"() {
	}
	
	void "Test followed discussion recent first post message match ahead of followed sprint recent description match"() {
	}
	
	void "Test followed sprint recent name match ahead of owned discussion recent non-first post message match"() {
	}
	
	void "Test followed sprint recent name match ahead of followed discussion recent non-first post message match"() {
	}
	
	void "Test followed sprint recent name match ahead of owned sprint recent description match"() {
	}
	
	void "Test followed sprint recent name match ahead of followed sprint recent description match"() {
	}	
}

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
		if (results.success) {
			println ""
			println "disOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }?.score}"
			println "disOwnedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
			println "disOwnedRecentNonFirstPostMessageMatch.firstPostMessage: $disOwnedRecentNonFirstPostMessageMatch.firstPostMessage"
			println ""
			println "disOwnedRecentNonFirstPostMessageMatch.posts: $disOwnedRecentNonFirstPostMessageMatch.posts"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent name match ahead of followed discussion recent non-first post message match"() {	
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }?.score}"
			println "disFollowedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent name match ahead of owned sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }?.score}"
			println "sprOwnedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test owned discussion recent name match ahead of followed sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }?.score}"
			println "sprFollowedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of owned discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disOwnedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }?.score}"
			println "disOwnedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}
			
		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of followed discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disOwnedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }?.score}"
			println "disFollowedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of owned sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disOwnedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }?.score}"
			println "sprOwnedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test owned discussion recent first post message match ahead of followed sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disOwnedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }?.score}"
			println "sprFollowedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of owned discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }?.score}"
			println "disOwnedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of followed discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }?.score}"
			println "disFollowedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of owned sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }?.score}"
			println "sprOwnedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test owned sprint recent name match ahead of followed sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprOwnedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }?.score}"
			println "sprFollowedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of owned discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }?.score}"
			println "disOwnedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of followed discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }?.score}"
			println "disFollowedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of owned sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }?.score}"
			println "sprOwnedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent name match ahead of followed sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }?.score}"
			println "sprFollowedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of owned discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }?.score}"
			println "disOwnedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of followed discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }?.score}"
			println "disFollowedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of owned sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }?.score}"
			println "sprOwnedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test followed discussion recent first post message match ahead of followed sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "disFollowedRecentFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }?.score}"
			println "sprFollowedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentFirstPostMessageMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of owned discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }?.score}"
			println "disOwnedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of followed discussion recent non-first post message match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }?.score}"
			println "disFollowedRecentNonFirstPostMessageMatch score: ${results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of non-first post match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedRecentNonFirstPostMessageMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of owned sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }?.score}"
			println "sprOwnedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedRecentDescriptionMatch.hash }
	}
	
	void "Test followed sprint recent name match ahead of followed sprint recent description match"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		if (results.success) {
			println ""
			println "sprFollowedRecentNameMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }?.score}"
			println "sprFollowedRecentDescriptionMatch score: ${results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }?.score}"
			println ""
		}

		then: "name match is ahead of description match"
		results.success
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedRecentDescriptionMatch.hash }
	}	
}

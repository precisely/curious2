package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderGroup2Level3GTLevel4IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

   void "Test user name match ahead of user tag match"() {
		given: "two objects"
		createObjectPair(CreateType.UserNonFollowedNameMatch, CreateType.UserNonFollowedTagMatch)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm)
		printScores(results)
		
		then: "order is correct"
		results.success
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash }
		results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash } < 
				results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedTagMatch.hash }
    }
	
}

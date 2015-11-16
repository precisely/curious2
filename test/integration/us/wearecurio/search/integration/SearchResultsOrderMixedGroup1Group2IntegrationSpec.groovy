package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderMixedGroup1Group2IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	//@spock.lang.IgnoreRest
	void "Test order all first results are group1"() {
		given: "2 new group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)

		and: "5 old group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisOwnedOldNonFirstPostMessageMatch, false)
		createObject(CreateType.SprOwnedOldNameMatch, false)
		createObject(CreateType.SprFollowedOldNameMatch, true)		

		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		printScores(results)
		
		then: "order is correct"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }

		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } >= 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } >= 5
		
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } < 5		
	}
	
	void "Test order all first results are group2 when no group1"() {
		given: "5 new group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentDescriptionMatch, false)
		createObject(CreateType.SprPublicOldNameMatch, false)
		createObject(CreateType.SprPublicOldDescriptionMatch, true)		

		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		printScores(results)
		
		then: "order is correct"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash }
		
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldNameMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicOldDescriptionMatch.hash } < 5
	}
	
	void "Test order all first results are group1 when no group2"() {
		given: "5 group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisOwnedOldNonFirstPostMessageMatch, false)
		createObject(CreateType.SprOwnedOldNameMatch, false)
		createObject(CreateType.SprFollowedOldNameMatch, true)		

		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		printScores(results)
		
		then: "order is correct"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } < 5
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } < 5		
	}

	void "Test order some first results are group1, then group2 when all group1 used"() {
		given: "2 new group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)

		and: "2 old group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, true)

		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		printScores(results)
		
		then: "order is correct"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }

		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } >= 2
		results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } >= 2
		
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } < 2
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } < 2
	}
		
	//@spock.lang.IgnoreRest
	void "Test order alternates between group1 and group2 after first results"() {
		given: "3 group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)
		createObject(CreateType.UserNonFollowedNameMatch, false)
		
		and: "7 group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisOwnedOldNonFirstPostMessageMatch, false)
		createObject(CreateType.SprOwnedOldNameMatch, false)
		createObject(CreateType.SprFollowedOldNameMatch, false)		
		createObject(CreateType.UserFollowedNameMatch, false)
		createObject(CreateType.UserFollowedTagMatch, true)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		printScores(results)

		then: "order is correct"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
		
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }

		[5,7,9].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } )
		[5,7,9].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } )
		[5,7,9].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash } )
		
		[0,1,2,3,4,6,8].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6,8].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6,8].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6,8].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } )
		[0,1,2,3,4,6,8].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } )		
		[0,1,2,3,4,6,8].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedNameMatch.hash } )
		[0,1,2,3,4,6,8].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } )		
	}
	
	void "Test order alternates between group1 and group2 after first results then all group1 when group2 runs out"() {
		given: "2 new group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)
		
		and: "9 group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisOwnedOldNonFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.SprOwnedOldNameMatch, false)
		createObject(CreateType.SprFollowedOldNameMatch, false)		
		createObject(CreateType.SprFollowedOldDescriptionMatch, false)		
		createObject(CreateType.UserFollowedNameMatch, false)
		createObject(CreateType.UserFollowedTagMatch, true)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 11)
		printScores(results)
		
		then: "order is correct"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedNameMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }

		[5,7].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } )
		[5,7].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } )
		
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } )
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } )		
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash } )		
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedNameMatch.hash } )
		[0,1,2,3,4,6,8,9,10].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } )		
	}
	
	void "Test order alternates between group1 and group2 after first results then all group2 when group1 runs out"() {
		given: "4 new group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentDescriptionMatch, false)
		createObject(CreateType.UserNonFollowedNameMatch, false)
		
		and: "6 group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisOwnedOldNonFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.SprOwnedOldNameMatch, false)
		createObject(CreateType.SprFollowedOldNameMatch, true)		
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		printScores(results)
		
		then: "order is correct"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
		
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }

		[5,7,8,9].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } )
		[5,7,8,9].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } )
		[5,7,8,9].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash } )
		[5,7,8,9].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash } )
		
		[0,1,2,3,4,6].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } )
		[0,1,2,3,4,6].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } )
		[0,1,2,3,4,6].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } )		
	}
	
	void "Test no results returned when no group1 or group2 with no offset"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		
		then: "no results are returned"
		results.success
		results.listItems.size == 0
	}
	
	void "Test no results returned when no group1 or group2 with offset"() {
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 11, 10)
		
		then: "no results are returned"
		results.success
		results.listItems.size == 0
	}
	
	void "Test orderList size is smaller than max when not enough group1 and group2"() {
		given: "2 new group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)
		
		and: "2 group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, true)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 0, 10)
		printScores(results)
		
		then: "only 4 results returned"
		results.success
		
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash }
		results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash }

		[2,3].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } )
		[2,3].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } )
		
		[0,1].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } )
		[0,1].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } )
	}
	
	void "Test orderList size is smaller than max and order is correct when not enough group1 and group2 with offset"() {
		given: "4 new group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentDescriptionMatch, false)
		createObject(CreateType.UserNonFollowedNameMatch, false)
		
		and: "6 group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisOwnedOldNonFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.SprOwnedOldNameMatch, false)
		createObject(CreateType.SprFollowedOldNameMatch, true)		
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 5, 10)
		printScores(results)
		
		then: "size is correct"
		results.success
		results.listItems.size == 5
		
		and: "order is correct"
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }
		
		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } || 
			results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } || 
			results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } ||
			results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } ||
			results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } ||
			results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash }

		[0,2,3,4].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } )
		[0,2,3,4].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } )
		[0,2,3,4].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash } )
		[0,2,3,4].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash } )
		
		results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } == 1 ||
			results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } == 1 ||
			results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } == 1 ||
			results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } == 1 ||
			results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } == 1 ||
			results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } == 1		
	}
	
	void "Test alternate group1 and group2 items when search with offset"() {
		given: "4 group2 items"
		createObject(CreateType.DisPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentNameMatch, false)
		createObject(CreateType.SprPublicRecentDescriptionMatch, false)
		createObject(CreateType.UserNonFollowedNameMatch, false)
		
		and: "9 group1 items"
		createObject(CreateType.DisOwnedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.DisOwnedOldNonFirstPostMessageMatch, false)
		createObject(CreateType.DisFollowedOldFirstPostMessageMatch, false)
		createObject(CreateType.SprOwnedOldNameMatch, false)
		createObject(CreateType.SprFollowedOldNameMatch, false)		
		createObject(CreateType.SprFollowedOldDescriptionMatch, false)		
		createObject(CreateType.UserFollowedNameMatch, false)
		createObject(CreateType.UserFollowedTagMatch, true)
		
		when: "searched is called"
		def results = searchService.search(user1, searchTerm, 5, 10)
		printScores(results)
		
		then: "size is correct"
		results.success
		results.listItems.size == 8
		
		and: "order is correct"
		results.listItems.find{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash }
		results.listItems.find{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash }
		results.listItems.find{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash }

		results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } ||
			results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } ||
			results.listItems.find{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } ||
			results.listItems.find{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } ||
			results.listItems.find{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } ||
			results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } ||
			results.listItems.find{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash } ||
			results.listItems.find{ it.type == "usr" && it.hash == userFollowedNameMatch.hash } ||
			results.listItems.find{ it.type == "usr" && it.hash == userFollowedTagMatch.hash }

		[0,2,4,6].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disPublicRecentNameMatch.hash } )
		[0,2,4,6].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentNameMatch.hash } )
		[0,2,4,6].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprPublicRecentDescriptionMatch.hash } )
		[0,2,4,6].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userNonFollowedNameMatch.hash } )

		[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldFirstPostMessageMatch.hash } ) ||
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } ) ||
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disOwnedOldNonFirstPostMessageMatch.hash } ) ||
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "dis" && it.hash == disFollowedOldFirstPostMessageMatch.hash } ) ||
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprOwnedOldNameMatch.hash } ) ||
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldNameMatch.hash } ) ||	
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprFollowedOldDescriptionMatch.hash } ) ||		
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedNameMatch.hash } ) ||
			[1,3,5,7].contains( results.listItems.findIndexOf{ it.type == "usr" && it.hash == userFollowedTagMatch.hash } )		
	}
}

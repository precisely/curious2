package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderingIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//users
	  //	user followed name match
	  //	user followed tag match
	  //	user non-followed name match
	  //	user non-followed tag match
	//discussions
	  //	discussion owned/followed recent name match
	  //	discussion owned/followed recent first post message match
	  //	discussion owned/followed recent non-first post message match
	  //	discussion owned/followed old name match
	  //	discussion owned/followed old first post message match
	  //	discussion owned/followed old non-first post message match
	  //	discussion public recent name match
	  //	discussion public recent first post message match
	  //	discussion public recent non-first post message match
	  //	discussion public old name match
	  //	discussion public old first post message match
	  //	discussion public old non-first post message match
	//sprints
	  //	sprint owned/followed recent name match
	  //	sprint owned/followed recent description match
	  //	sprint owned/followed old name match
	  //	sprint owned/followed old description match
	  //	sprint public recent name match
	  //	sprint public recent description match
	  //	sprint public old name match
	  //	sprint public old description match
	
	//order
	//level 1
	//	discussion owned/followed recent name match
	//	discussion owned/followed recent first post message match
	//	sprint owned/followed recent name match
	//level 2
	//	discussion owned/followed recent non-first post message match
	//	sprint owned/followed recent description match
	//level 3
	//	user followed name match
	//	discussion public recent name match
	//	discussion public recent first post message match
	//	sprint public recent name match
	//level 4
	//	user followed tag match
	//	discussion public recent non-first post message match
	//	sprint public recent description match
	//level 5
	//	user non-followed name match
	//	discussion owned/followed old name match
	//	discussion owned/followed old first post message match
	//	sprint owned/followed old name match
	//level 6
	//	user non-followed tag match
	//	discussion public old name match
	//	discussion public old first post message match
	//	sprint public old name match
	//level 7
	//	discussion old/followed old non-first post message match
	//	sprint owned/followed old description match
	//level 8
	//	discussion public old non-first post message match
	//	sprint public old description match
	
	//group 1
	// followed/owned discussions
	// followed/owned sprints
	// followed users
	
	//group 2
	// public/non-followed/non-owned discussions
	// public/non-followed/non-owned sprints
	// non-followed users
	
	//group 1 order
	//level 1
	//	discussion owned/followed recent name match
	//	discussion owned/followed recent first post message match
	//	sprint owned/followed recent name match
	//level 2
	//	discussion owned/followed recent non-first post message match
	//	sprint owned/followed recent description match
	//level 3
	//	user followed name match
	//level 4
	//	user followed tag match
	//level 5
	//	discussion owned/followed old name match
	//	discussion owned/followed old first post message match
	//	sprint owned/followed old name match
	//level 6
	//	discussion owned/followed old non-first post message match
	//	sprint owned/followed old description match

	//group 2 order
	//level 1
	//	discussion public recent name match
	//	discussion public recent first post message match
	//	sprint public recent name match
	//level 2
	//	discussion public recent non-first post message match
	//	sprint public recent description match
	//level 3
	//	user non-followed name match
	//level 4
	//	user non-followed tag match
	//level 5
	//	discussion public old name match
	//	discussion public old first post message match
	//	sprint public old name match
	//level 6
	//	discussion public old non-first post message match
	//	sprint public old description match
	
	//group 1 order tests
	//	level 1 ahead of level 2 (SearchResultsOrderGroup1Level1GTLevel2)
	// 		owned discussion recent name match ahead of owned discussion recent non-first post message match
	// 		owned discussion recent name match ahead of followed discussion recent non-first post message match
	// 		owned discussion recent name match ahead of owned sprint recent description match
	// 		owned discussion recent name match ahead of followed sprint recent description match
	// 		owned discussion recent first post message match ahead of owned discussion recent non-first post message match
	// 		owned discussion recent first post message match ahead of followed discussion recent non-first post message match
	// 		owned discussion recent first post message match ahead of owned sprint recent description match
	// 		owned discussion recent first post message match ahead of followed sprint recent description match
	// 		owned sprint recent name match ahead of owned discussion recent non-first post message match
	// 		owned sprint recent name match ahead of followed discussion recent non-first post message match
	// 		owned sprint recent name match ahead of owned sprint recent description match
	// 		owned sprint recent name match ahead of followed sprint recent description match
	// 		followed discussion recent name match ahead of owned discussion recent non-first post message match
	// 		followed discussion recent name match ahead of followed discussion recent non-first post message match
	// 		followed discussion recent name match ahead of owned sprint recent description match
	// 		followed discussion recent name match ahead of followed sprint recent description match
	// 		followed discussion recent first post message match ahead of owned discussion recent non-first post message match
	// 		followed discussion recent first post message match ahead of followed discussion recent non-first post message match
	// 		followed discussion recent first post message match ahead of owned sprint recent description match
	// 		followed discussion recent first post message match ahead of followed sprint recent description match
	// 		followed sprint recent name match ahead of owned discussion recent non-first post message match
	// 		followed sprint recent name match ahead of followed discussion recent non-first post message match
	// 		followed sprint recent name match ahead of owned sprint recent description match
	// 		followed sprint recent name match ahead of followed sprint recent description match
	//	level 2 ahead of level 3 (SearchResultsOrderGroup1Level2GTLevel3)
	// 		owned discussion recent non-first post message match ahead of user name match
	// 		owned sprint recent description match ahead of user name match
	// 		followed discussion recent non-first post message match ahead of user name match
	// 		followed sprint recent description match ahead of user name match
	//	level 3 ahead of level 4 (SearchResultsOrderGroup1Level3GTLevel4)
	//		user name match ahead of user tag match
	//	level 4 ahead of level 5 (SearchResultsOrderGroup1Level4GTLevel5)
	//		user tag match ahead of owned discussion old name match
	//		user tag match ahead of followed discussion old name match
	//		user tag match ahead of owned discussion old first post message match
	//		user tag match ahead of followed discussion old first post message match
	//		user tag match ahead of owned sprint old name match
	//		user tag match ahead of followed sprint old name match
	//	level 5 ahead of level 6 (SearchResultsOrderGroup1Level5GTLevel6)
	//		owned discussion old name match ahead of owned discussion old non-first post message match
	//		owned discussion old name match ahead of followed discussion old non-first post message match
	//		owned discussion old name match ahead of owned sprint old description match
	//		owned discussion old name match ahead of followed sprint old description match
	//		owned discussion old first post message match ahead of owned discussion old non-first post message match
	//		owned discussion old first post message match ahead of followed discussion old non-first post message match
	//		owned discussion old first post message match ahead of owned sprint old description match
	//		owned discussion old first post message match ahead of followed sprint old description match
	//		owned sprint old name match ahead of owned discussion old non-first post message match
	//		owned sprint old name match ahead of followed discussion old non-first post message match
	//		owned sprint old name match ahead of owned sprint old description match
	//		owned sprint old name match ahead of followed sprint old description match
	//		followed discussion old name match ahead of owned discussion old non-first post message match
	//		followed discussion old name match ahead of followed discussion old non-first post message match
	//		followed discussion old name match ahead of owned sprint old description match
	//		followed discussion old name match ahead of followed sprint old description match
	//		followed discussion old first post message match ahead of owned discussion old non-first post message match
	//		followed discussion old first post message match ahead of followed discussion old non-first post message match
	//		followed discussion old first post message match ahead of owned sprint old description match
	//		followed discussion old first post message match ahead of followed sprint old description match
	//		followed sprint old name match ahead of owned discussion old non-first post message match
	//		followed sprint old name match ahead of followed discussion old non-first post message match
	//		followed sprint old name match ahead of owned sprint old description match
	//		followed sprint old name match ahead of followed sprint old description match

	//group 2 order tests
	//	level 1 ahead of level 2 (SearchResultsOrderGroup2Level1GTLevel2)
	// 		discussion recent name match ahead of discussion recent non-first post message match
	// 		discussion recent name match ahead of sprint recent description match
	// 		discussion recent first post message match ahead of discussion recent non-first post message match
	// 		discussion recent first post message match ahead of sprint recent description match
	// 		sprint recent name match ahead of discussion recent non-first post message match
	// 		sprint recent name match ahead of sprint recent description match
	//	level 2 ahead of level 3 (SearchResultsOrderGroup2Level2GTLevel3)
	// 		discussion recent non-first post message match ahead of user name match
	// 		sprint recent description match ahead of user name match
	//	level 3 ahead of level 4 (SearchResultsOrderGroup1Level3GTLevel4)
	//		user name match ahead of user tag match
	//	level 4 ahead of level 5 (SearchResultsOrderGroup2Level4GTLevel5)
	//		user tag match ahead of discussion old name match
	//		user tag match ahead of discussion old first post message match
	//		user tag match ahead of sprint old name match
	//	level 5 ahead of level 6 (SearchResultsOrderGroup2Level5GTLevel6)
	//		discussion old name match ahead of discussion old non-first post message match
	//		discussion old name match ahead of sprint old description match
	//		discussion old first post message match ahead of discussion old non-first post message match
	//		discussion old first post message match ahead of sprint old description match
	//		sprint old name match ahead of discussion old non-first post message match
	//		sprint old name match ahead of sprint old description match
	
	//mixed group 1 and group 2 order tests (SearchResultsOrderMixedGroup1Group2)
	//	only group 1 in first 5 results
	//	group 2 and group 1 alternate after 5 results
	
	//MAKE BASE TEST CLASS WITH AT LEAST ONE TYPE OF EACH OBJECT FOR REUSE (e.g., disOwnedRecentName)
	
    void "test level 1 results before level 2 results"() {
    }
}

package us.wearecurio.services.unit

//import grails.test.mixin.TestMixin
//import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.services.SearchQueryService

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
//@TestMixin(GrailsUnitTestMixin)
class SearchQueryServiceGroup2Spec extends Specification {
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getDiscussionSearchGroup2QueryString(#userId, #query, #readerGroupIds, #adminGroupIds, #followedUsersGroupIds, #followedSprintsGroupIds, #ownedSprintsGroupIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String searchedQuery = SearchQueryService.getDiscussionSearchGroup2QueryString(
			userId,
			query,
			readerGroupIds,
			adminGroupIds,
			followedUsersGroupIds,
			followedSprintsGroupIds,
			ownedSprintsGroupIds
		)
		
		then: "valid normalized query is produced"
		searchedQuery == expected
		
		where:
		userId	| query	| readerGroupIds	| adminGroupIds	| followedUsersGroupIds	| followedSprintsGroupIds	| ownedSprintsGroupIds	| 
		expected
		34		| "foo"	| []				| []			| []					| []						| []					| 
		"((NOT ((userId:34))) AND visibility:PUBLIC AND (name:(foo) OR posts:(foo) OR firstPostMessage:(foo)) AND _type:discussion)"
		
		578		| "foo"	| [3]				| [4]			| [378]					| [3]						| [6]					| 
		"((NOT (((userId:578) OR (groupIds:(4 OR 378 OR 3 OR 6))))) AND visibility:PUBLIC AND (name:(foo) OR posts:(foo) OR firstPostMessage:(foo)) AND _type:discussion)"
		
		4637	| "foo" | [6]				| [8]			| [5]					| []						| []					| 
		"((NOT (((userId:4637) OR (groupIds:(6 OR 8 OR 5))))) AND visibility:PUBLIC AND (name:(foo) OR posts:(foo) OR firstPostMessage:(foo)) AND _type:discussion)"
	}
	
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getSprintSearchGroup2QueryString(#userId, #query, #readerGroupIds, #adminGroupIds, #followedUsersIds, #followedSprintsGroupIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String searchedQuery = SearchQueryService.getSprintSearchGroup2QueryString(
			userId,
			query,
			readerGroupIds,
			adminGroupIds,
			followedUsersIds,
			followedSprintsGroupIds
		)
		
		then: "valid normalized query is produced"
		searchedQuery == expected
		
		where:
		userId	| query	| readerGroupIds	| adminGroupIds	| followedUsersIds	| followedSprintsGroupIds	|
		expected
		34		| "foo"	| []				| []			| []					| []					| 
		"(visibility:PUBLIC AND (name:(foo) OR description:(foo)) AND _type:sprint)"
		
		578		| "foo"	| [3]				| [4]			| [378]					| [3]					| 
		"((NOT (((virtualGroupId:3) OR (userId:378)))) AND visibility:PUBLIC AND (name:(foo) OR description:(foo)) AND _type:sprint)"
		
		4637	| "foo" | [6]				| [8]			| [5]					| []					| 
		"((NOT ((userId:5))) AND visibility:PUBLIC AND (name:(foo) OR description:(foo)) AND _type:sprint)"
	}
	
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getUserSearchGroup2QueryString(#userId, #query, #followedUsersIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String searchedQuery = SearchQueryService.getUserSearchGroup2QueryString(
			userId,
			query,
			followedUsersIds
		)
		
		then: "valid normalized query is produced"
		searchedQuery == expected
		
		where:
		userId	| query	| followedUsersIds	| 
		expected
		34		| "foo"	| []				|
		"(((publicName:(foo)) OR (publicBio:(foo)) OR (interestTagsString:(foo)))  AND virtual:false AND _type:user AND NOT (_id:34))"
		
		578		| "foo"	| [3]				|
		"(((publicName:(foo)) OR (publicBio:(foo)) OR (interestTagsString:(foo)))  AND virtual:false AND _type:user AND NOT (_id:(3 OR 578)))"
		
		4637	| "foo" | [6]				|
		"(((publicName:(foo)) OR (publicBio:(foo)) OR (interestTagsString:(foo)))  AND virtual:false AND _type:user AND NOT (_id:(6 OR 4637)))"
	}	
}

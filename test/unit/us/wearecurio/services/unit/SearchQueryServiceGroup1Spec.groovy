package us.wearecurio.services.unit

//import grails.test.mixin.TestMixin
//import grails.test.mixin.support.GrailsUnitTestMixin
import java.util.List;

import spock.lang.Specification
import us.wearecurio.services.SearchQueryService

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
//@TestMixin(GrailsUnitTestMixin)
class SearchQueryServiceGroup1Spec extends Specification {

	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getDiscussionSearchGroup1QueryString(#userId, #query, #readerGroupIds, #adminGroupIds, #followedUsersGroupIds, #followedSprintsGroupIds, #ownedSprintsGroupIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String searchedQuery = SearchQueryService.getDiscussionSearchGroup1QueryString(
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
		"(((userId:34 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW))) AND (name:(foo) OR posts:(foo) OR firstPostMessage:(foo) OR username:(foo) OR postUsernames:(foo)) AND _type:discussion)"
		
		578		| "foo"	| [3]				| [4]			| [378]					| [3]						| [6]					| 
		"((((userId:578 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) OR (groupIds:4 AND visibility:(PUBLIC OR UNLISTED OR NEW)) OR (groupIds:378 AND visibility:PUBLIC) OR (groupIds:3 AND visibility:PUBLIC) OR (groupIds:6 AND visibility:PUBLIC))) AND (name:(foo) OR posts:(foo) OR firstPostMessage:(foo) OR username:(foo) OR postUsernames:(foo)) AND _type:discussion)"
		
		4637	| "foo" | [6]				| [8]			| [5]					| []						| []					| 
		"((((userId:4637 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) OR (groupIds:6 AND visibility:(PUBLIC OR UNLISTED)) OR (groupIds:8 AND visibility:(PUBLIC OR UNLISTED OR NEW)) OR (groupIds:5 AND visibility:PUBLIC))) AND (name:(foo) OR posts:(foo) OR firstPostMessage:(foo) OR username:(foo) OR postUsernames:(foo)) AND _type:discussion)"
	}
	
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getSprintSearchGroup1QueryString(#userId, #query, #readerGroupIds, #adminGroupIds, #followedUsersIds, #followedSprintsGroupIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String searchedQuery = SearchQueryService.getSprintSearchGroup1QueryString(
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
		null
		
		578		| "foo"	| [3]				| [4]			| [378]					| [3]					| 
		"((((virtualGroupId:3 AND visibility:(PUBLIC OR UNLISTED)) OR (userId:378 AND visibility:PUBLIC))) AND (name:(foo) OR description:(foo) OR username:(foo) OR discussionsUsernames:(foo)) AND _type:sprint AND ((NOT _exists_:deleted) OR deleted:false))"
		
		4637	| "foo" | [6]				| [8]			| [5]					| []					| 
		"(((userId:5 AND visibility:PUBLIC)) AND (name:(foo) OR description:(foo) OR username:(foo) OR discussionsUsernames:(foo)) AND _type:sprint AND ((NOT _exists_:deleted) OR deleted:false))"
	}
	
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getUserSearchGroup1QueryString(#userId, #query, #followedUsersIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String searchedQuery = SearchQueryService.getUserSearchGroup1QueryString(
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
		"(((publicName:(foo)) OR (publicBio:(foo)) OR (interestTagsString:(foo)) OR (username:(foo))) AND _type:user AND _id:34 AND virtual:false)"
		578		| "foo"	| [3]				|
		"(((publicName:(foo)) OR (publicBio:(foo)) OR (interestTagsString:(foo)) OR (username:(foo))) AND _type:user AND _id:(3 OR 578) AND virtual:false)"
		
		4637	| "foo" | [6]				|
		"(((publicName:(foo)) OR (publicBio:(foo)) OR (interestTagsString:(foo)) OR (username:(foo))) AND _type:user AND _id:(6 OR 4637) AND virtual:false)"
	}	
}

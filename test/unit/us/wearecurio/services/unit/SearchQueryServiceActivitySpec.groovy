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
class SearchQueryServiceActivitySpec extends Specification {
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getDiscussionActivityQueryString(#userId, #readerGroupIds, #adminGroupIds, #followedUsersGroupIds, #followedSprintsGroupIds, #ownedSprintsGroupIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String query = SearchQueryService.getDiscussionActivityQueryString(
			userId,
			readerGroupIds,
			adminGroupIds,
			followedUsersGroupIds,
			followedSprintsGroupIds,
			ownedSprintsGroupIds
		)
		
		then: "valid normalized query is produced"
		query == expected
		
		where:
		userId	| readerGroupIds	| adminGroupIds	| followedUsersGroupIds	| followedSprintsGroupIds	| ownedSprintsGroupIds	| 
		expected
		34		| []				| []			| []					| []						| []					| 
		"((userId:34 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) AND _type:discussion)"
		
		578		| [3]				| [4]			| [378]					| [3]						| [6]					| 
		"(((userId:578 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) OR (groupIds:4 AND visibility:(PUBLIC OR UNLISTED OR NEW)) OR (groupIds:378 AND visibility:PUBLIC) OR (groupIds:3 AND visibility:PUBLIC) OR (groupIds:6 AND visibility:PUBLIC)) AND _type:discussion)"
		
		4637	| [6]				| [8]			| [5]					| []						| []					| 
		"(((userId:4637 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) OR (groupIds:6 AND visibility:(PUBLIC OR UNLISTED)) OR (groupIds:8 AND visibility:(PUBLIC OR UNLISTED OR NEW)) OR (groupIds:5 AND visibility:PUBLIC)) AND _type:discussion)"
	}
	
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test getSprintActivityQueryString(#userId, #readerGroupIds, #adminGroupIds, #followedUsersIds, #followedSprintsGroupIds) returns '#expected'"() {
		when: "SearchQueryService.getDiscussionActivityQueryString is called"
		String query = SearchQueryService.getSprintActivityQueryString(
			userId, 
			readerGroupIds, 
			adminGroupIds, 
			followedUsersIds, 
			followedSprintsGroupIds		
		)
		
		then: "valid normalized query is produced"
		query == expected
		
		where:
		userId	| readerGroupIds	| adminGroupIds	| followedUsersIds	| followedSprintsGroupIds	|
		expected
		34		| []				| []			| []				| []						|
		"((userId:34 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) AND _type:sprint AND _exists_:description AND ((NOT _exists_:deleted) OR deleted:false))"
		
		578		| [3]				| [4]			| [378]				| [3]						|
		"(((userId:578 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) OR (virtualGroupId:3 AND visibility:(PUBLIC OR UNLISTED)) OR (userId:378 AND visibility:PUBLIC)) AND _type:sprint AND _exists_:description AND ((NOT _exists_:deleted) OR deleted:false))"
		
		4637	| [6]				| [8]			| [5]				| []						|
		"(((userId:4637 AND visibility:(PUBLIC OR PRIVATE OR UNLISTED OR NEW)) OR (userId:5 AND visibility:PUBLIC)) AND _type:sprint AND _exists_:description AND ((NOT _exists_:deleted) OR deleted:false))"
	}
}

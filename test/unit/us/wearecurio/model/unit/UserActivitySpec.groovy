package us.wearecurio.model.unit

//import grails.test.mixin.TestMixin
//import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import grails.test.spock.*

import us.wearecurio.model.UserActivity

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
//@TestMixin(GrailsUnitTestMixin)
class UserActivitySpec extends Specification {

	static def typePermutations = []
	static def validTypes = [
		[UserActivity.ActivityType.CREATE, UserActivity.ObjectType.SPRINT, null],
		[UserActivity.ActivityType.CREATE, UserActivity.ObjectType.DISCUSSION, null],
		[UserActivity.ActivityType.DELETE, UserActivity.ObjectType.SPRINT, null],
		[UserActivity.ActivityType.DELETE, UserActivity.ObjectType.DISCUSSION, null],
		[UserActivity.ActivityType.FOLLOW, UserActivity.ObjectType.SPRINT, null],
		[UserActivity.ActivityType.FOLLOW, UserActivity.ObjectType.USER, null],
		[UserActivity.ActivityType.UNFOLLOW, UserActivity.ObjectType.SPRINT, null],
		[UserActivity.ActivityType.UNFOLLOW, UserActivity.ObjectType.USER, null],
		[UserActivity.ActivityType.ADD, UserActivity.ObjectType.DISCUSSION, UserActivity.ObjectType.SPRINT],
		[UserActivity.ActivityType.ADD, UserActivity.ObjectType.ADMIN, UserActivity.ObjectType.DISCUSSION],
		[UserActivity.ActivityType.ADD, UserActivity.ObjectType.READER, UserActivity.ObjectType.DISCUSSION],
		[UserActivity.ActivityType.REMOVE, UserActivity.ObjectType.DISCUSSION, UserActivity.ObjectType.SPRINT],
		[UserActivity.ActivityType.REMOVE, UserActivity.ObjectType.ADMIN, UserActivity.ObjectType.DISCUSSION],
		[UserActivity.ActivityType.REMOVE, UserActivity.ObjectType.READER, UserActivity.ObjectType.DISCUSSION],
		[UserActivity.ActivityType.START, UserActivity.ObjectType.SPRINT, null],
		[UserActivity.ActivityType.END, UserActivity.ObjectType.SPRINT, null],
		[UserActivity.ActivityType.STOP, UserActivity.ObjectType.SPRINT, null],
		[UserActivity.ActivityType.COMMENT, UserActivity.ObjectType.DISCUSSION_POST, UserActivity.ObjectType.DISCUSSION],
		[UserActivity.ActivityType.UNCOMMENT, UserActivity.ObjectType.DISCUSSION_POST, UserActivity.ObjectType.DISCUSSION],
		[UserActivity.ActivityType.INVITE, UserActivity.ObjectType.USER, UserActivity.ObjectType.SPRINT],
		[UserActivity.ActivityType.UNINVITE, UserActivity.ObjectType.USER, UserActivity.ObjectType.SPRINT]
	]
	static String expectedTypeStringPart = "act"
	static String expectedDelimiter = "-"
	static String expectedInvalidString = ""
	
	static def expectedActivityTypeStringPart = [
		0 : "created",
		1 : "deleted",
		2 : "followed",
		3 : "unfollowed",
		4 : "added",
		5 : "removed",
		6 : "started",
		7 : "ended",
		8 : "stopped",
		9 : "commented",
		10 : "uncommented",
		11 : "invited",
		12 : "uninvited"
	]
	
	static def expectedObjectTypeStringPart = [
		0 : "spr",
		1 : "post",
		2 : "dis",
		3 : "usr",
		4 : "admin",
		5 : "reader"
	]
	
    def setupSpec() {
		String expectedTypeString = expectedInvalidString
		boolean expectedValidity = validTypes.find{ it -> it[0] == null && it[1] == null && it[2] == null } != null
		if (expectedValidity) {
			expectedTypeString = expectedTypeStringPart
		}
		typePermutations << [
			null,
			null,
			null,
			expectedTypeString,
			expectedValidity
		]

		for (i in 0..12) {
			def activityType = UserActivity.ActivityType.get(i)
			expectedTypeString = expectedInvalidString
			expectedValidity = validTypes.find{ it -> it[0] == activityType && it[1] == null && it[2] == null } != null
			if (expectedValidity) {
				expectedTypeString = 	expectedTypeStringPart
				expectedTypeString += 	expectedDelimiter
				expectedTypeString += 	expectedActivityTypeStringPart[i]
			}
			typePermutations << [
				activityType,
				null,
				null,
				expectedTypeString,
				expectedValidity
			]
			for (j in 0..5) {
				def primaryObjType = UserActivity.ObjectType.get(j)
				expectedValidity = validTypes.find{ it -> it[0] == activityType && it[1] == primaryObjType && it[2] == null } != null
				if (UserActivity.isValid(activityType, primaryObjType)) {
					expectedTypeString = 	expectedTypeStringPart 
					expectedTypeString += 	expectedDelimiter
					expectedTypeString += 	expectedActivityTypeStringPart[i]
					expectedTypeString += 	expectedDelimiter
					expectedTypeString += 	expectedObjectTypeStringPart[j]
				}
				typePermutations << [
					activityType,
					primaryObjType,
					null,
					expectedTypeString,
					expectedValidity
				]				
				for (k in 0..5) {
					expectedTypeString = expectedInvalidString
					def secondaryObjType = UserActivity.ObjectType.get(k)
					expectedValidity = validTypes.find{ it -> it[0] == activityType && it[1] == primaryObjType && it[2] == secondaryObjType } != null
					if( UserActivity.isValid(activityType, primaryObjType, secondaryObjType) ) {
						expectedTypeString = 	expectedTypeStringPart
						expectedTypeString += 	expectedDelimiter
						expectedTypeString += 	expectedActivityTypeStringPart[i]
						expectedTypeString += 	expectedDelimiter
						expectedTypeString += 	expectedObjectTypeStringPart[j]
						expectedTypeString += 	expectedDelimiter
						expectedTypeString += 	expectedObjectTypeStringPart[k]
					}
					typePermutations << [
						activityType,
						primaryObjType,
						secondaryObjType,
						expectedTypeString,
						expectedValidity
					]
				}
			}
		}
    }

	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test ActivityType.toString for '#activityType' returns '#expected'"() {
		when: "isValidSecondaryType is called"
		def result = activityType != null ? activityType.toString() : ""
		
		then: "result matches expected value"
		result == expected
		
		where:
		activityType						|	expected
		UserActivity.ActivityType.CREATE	|	"created"
		UserActivity.ActivityType.DELETE	|	"deleted"
		UserActivity.ActivityType.FOLLOW	|	"followed"
		UserActivity.ActivityType.UNFOLLOW	|	"unfollowed"
		UserActivity.ActivityType.ADD		|	"added"
		UserActivity.ActivityType.REMOVE	|	"removed"
		UserActivity.ActivityType.START		|	"started"
		UserActivity.ActivityType.END		|	"ended"
		UserActivity.ActivityType.STOP		|	"stopped"
		UserActivity.ActivityType.COMMENT	|	"commented"
		UserActivity.ActivityType.UNCOMMENT	|	"uncommented"
		UserActivity.ActivityType.INVITE	|	"invited"
		UserActivity.ActivityType.UNINVITE	|	"uninvited"
		null								| 	""
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test ObjectType.toString for '#objectType' returns '#expected'"() {
		when: "isValidSecondaryType is called"
		def result = objectType != null ? objectType.toString() : ""
		
		then: "result matches expected value"
		result == expected
		
		where:
		objectType								|	expected
		UserActivity.ObjectType.SPRINT			|	"spr"
		UserActivity.ObjectType.DISCUSSION		|	"dis"
		UserActivity.ObjectType.DISCUSSION_POST	|	"post"
		UserActivity.ObjectType.USER			|	"usr"
		UserActivity.ObjectType.ADMIN			|	"admin"
		UserActivity.ObjectType.READER			|	"reader"
		null									| 	""
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test isValid returns #expectedValidity for activity '#activityType', primary object '#primaryObjectType' and secondary object '#secondaryObjectType'"() {
		when: "getTypeString is called"
		def valid = UserActivity.isValid(activityType, primaryObjectType, secondaryObjectType)
		
		then: "expected results are returned"
		valid == expectedValidity
		
		where:
		activityType << typePermutations.collect{ it[0] }
		primaryObjectType << typePermutations.collect{ it[1] }
		secondaryObjectType << typePermutations.collect{ it[2] }
		expectedValidity << typePermutations.collect{ it[4] }
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test getTypeString returns '#expectedTypeString' for activity '#activityType', primary object '#primaryObjectType' and secondary object '#secondaryObjectType'"() {
		when: "getTypeString is called"
		def typeString = UserActivity.getTypeString(activityType, primaryObjectType, secondaryObjectType)
		
		then: "expected results are returned"
		typeString == expectedTypeString
		
		where:
		activityType << typePermutations.collect{ it[0] }
		primaryObjectType << typePermutations.collect{ it[1] }
		secondaryObjectType << typePermutations.collect{ it[2] }
		expectedTypeString << typePermutations.collect{ it[3] }
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test toActivityType and toObjectType returns correct values using typeId returned from toTypeId(#activityType, #primaryObjectType, #secondaryObjectType)"() {
		given: "typeId returned from UserActivity.toType"
		Long typeId = UserActivity.toType(activityType, primaryObjectType, secondaryObjectType)
		boolean isValid = (typeId != UserActivity.INVALID_TYPE)
		
		when: "toActivityType is called"
		UserActivity.ActivityType activityTypeReturned = UserActivity.toActivityType(typeId)
		
		and: "toObjectType is called" 
		UserActivity.ObjectType objectTypeReturned = UserActivity.toObjectType(typeId)
		
		and: "toOtherType is called"
		UserActivity.ObjectType otherTypeReturned = UserActivity.toOtherType(typeId)
		println "typeId: " + typeId
		println "activityTypeReturned: " + activityTypeReturned
		println "objectTypeReturned: " + objectTypeReturned
		println "otherTypeReturned: " + otherTypeReturned
		
		then: "expected results are returned"
		isValid == expectedValidity
		(!isValid && activityTypeReturned == null) || (isValid && activityTypeReturned == activityType)
		(!isValid && objectTypeReturned == null) || (isValid && objectTypeReturned == primaryObjectType)
		(!isValid && otherTypeReturned == null) || (isValid && otherTypeReturned == secondaryObjectType)
		
		where:
		activityType << typePermutations.collect{ it[0] }
		primaryObjectType << typePermutations.collect{ it[1] }
		secondaryObjectType << typePermutations.collect{ it[2] }
		expectedValidity << typePermutations.collect{ it[4] }
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test toActivityType returns '#activityType' using typeId returned from toType(#activityType)"() {
		given: "typeId returned from UserActivity.toType"
		Long typeId = UserActivity.toType(activityType)
		boolean isValid = (typeId != UserActivity.INVALID_TYPE)
		
		when: "toActivityType is called"
		UserActivity.ActivityType activityTypeReturned = UserActivity.toActivityType(typeId)
		
		then: "expected result are returned"
		(activityType == null && !isValid) || (activityType != null && isValid && activityType == activityTypeReturned)
		
		where:
		activityType << [
		UserActivity.ActivityType.CREATE,
		UserActivity.ActivityType.DELETE,
		UserActivity.ActivityType.FOLLOW,
		UserActivity.ActivityType.UNFOLLOW,
		UserActivity.ActivityType.ADD,
		UserActivity.ActivityType.REMOVE,
		UserActivity.ActivityType.START,
		UserActivity.ActivityType.END,
		UserActivity.ActivityType.STOP,
		UserActivity.ActivityType.COMMENT,
		UserActivity.ActivityType.UNCOMMENT,
		UserActivity.ActivityType.INVITE,
		UserActivity.ActivityType.UNINVITE,
		null
		]
	}

	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test toTypeFromObject returns '#objectType' using typeId returned from toTypeFromObject(#objectType)"() {
		given: "typeId returned from UserActivity.toTypeFromObject"
		Long typeId = UserActivity.toTypeFromObject(objectType)
		boolean isValid = (typeId != UserActivity.INVALID_TYPE)
		
		when: "toObjectType is called"
		UserActivity.ObjectType objectTypeReturned = UserActivity.toObjectType(typeId)
		
		then: "expected result are returned"
		(objectType == null && !isValid) || (objectType != null && isValid && objectType == objectTypeReturned)
		
		where:
		objectType << [
		UserActivity.ObjectType.SPRINT,
		UserActivity.ObjectType.DISCUSSION_POST,
		UserActivity.ObjectType.DISCUSSION,
		UserActivity.ObjectType.USER,
		UserActivity.ObjectType.ADMIN,
		UserActivity.ObjectType.READER,
		null
		]
	}

	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test toTypeFromOther returns '#otherType' using typeId returned from toTypeFromOther(#otherType)"() {
		given: "typeId returned from UserActivity.toTypeFromOther"
		Long typeId = UserActivity.toTypeFromOther(otherType)
		boolean isValid = (typeId != UserActivity.INVALID_TYPE)
		
		when: "toOtherType is called"
		UserActivity.ObjectType otherTypeReturned = UserActivity.toOtherType(typeId)
		
		then: "expected result are returned"
		(otherType == null && typeId == 0) || (otherType != null && otherType == otherTypeReturned)
		
		where:
		otherType << [
		UserActivity.ObjectType.SPRINT,
		UserActivity.ObjectType.DISCUSSION_POST,
		UserActivity.ObjectType.DISCUSSION,
		UserActivity.ObjectType.USER,
		UserActivity.ObjectType.ADMIN,
		UserActivity.ObjectType.READER,
		null
		]
	}
}

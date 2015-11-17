package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils

//
//	I prefer to have CreateType enum as a static enum of SearchResultsOrderIntegrationSpecBase, but for reasons beyond my comprehension, 
// 	I am unable to get that to work, and was forced to moving the enum outside the class instead
//  That is, if I try to define the following inside of SearchResultsOrderIntegrationSpecBase:
//	static enum CreateType2 {
//		UserFollowedNameMatch,
//		UserFollowedTagMatch,
//		...
//	}
//	I get this error:
//	compilation error compiling [integration] tests: (class:
//		us/wearecurio/search/integration/SearchResultsOrderIntegrationSpecBase$CreateType,
//		method: destoryIntegrationTest signature: ()V) Incompatible object argument for function call
//	HUH?????
//
enum CreateType {
	UserFollowedNameMatch,
	UserFollowedTagMatch,
	UserNonFollowedNameMatch,
	UserNonFollowedTagMatch,
	DisOwnedRecentNameMatch,
	DisFollowedRecentNameMatch,
	DisOwnedRecentFirstPostMessageMatch,
	DisFollowedRecentFirstPostMessageMatch,
	DisOwnedRecentNonFirstPostMessageMatch,
	DisFollowedRecentNonFirstPostMessageMatch,
	DisOwnedOldNameMatch,
	DisFollowedOldNameMatch,
	DisOwnedOldFirstPostMessageMatch,
	DisFollowedOldFirstPostMessageMatch,
	DisOwnedOldNonFirstPostMessageMatch,
	DisFollowedOldNonFirstPostMessageMatch,
	DisPublicRecentNameMatch,
	DisPublicRecentFirstPostMessageMatch,
	DisPublicRecentNonFirstPostMessageMatch,
	DisPublicOldNameMatch,
	DisPublicOldFirstPostMessageMatch,
	DisPublicOldNonFirstPostMessageMatch,
	SprOwnedRecentNameMatch,
	SprFollowedRecentNameMatch,
	SprOwnedRecentDescriptionMatch,
	SprFollowedRecentDescriptionMatch,
	SprOwnedOldNameMatch,
	SprFollowedOldNameMatch,
	SprOwnedOldDescriptionMatch,
	SprFollowedOldDescriptionMatch,
	SprPublicRecentNameMatch,
	SprPublicRecentDescriptionMatch,
	SprPublicOldNameMatch,
	SprPublicOldDescriptionMatch
}

class SearchResultsOrderIntegrationSpecBase extends SearchServiceIntegrationSpecBase {
	
	User userFollowedNameMatch
	User userFollowedTagMatch
	User userNonFollowedNameMatch
	User userNonFollowedTagMatch
	Discussion disOwnedRecentNameMatch
	Discussion disFollowedRecentNameMatch
	Discussion disOwnedRecentFirstPostMessageMatch
	Discussion disFollowedRecentFirstPostMessageMatch
	Discussion disOwnedRecentNonFirstPostMessageMatch
	Discussion disFollowedRecentNonFirstPostMessageMatch
	Discussion disOwnedOldNameMatch
	Discussion disFollowedOldNameMatch
	Discussion disOwnedOldFirstPostMessageMatch
	Discussion disFollowedOldFirstPostMessageMatch
	Discussion disOwnedOldNonFirstPostMessageMatch
	Discussion disFollowedOldNonFirstPostMessageMatch
	Discussion disPublicRecentNameMatch
	Discussion disPublicRecentFirstPostMessageMatch
	Discussion disPublicRecentNonFirstPostMessageMatch
	Discussion disPublicOldNameMatch
	Discussion disPublicOldFirstPostMessageMatch
	Discussion disPublicOldNonFirstPostMessageMatch
	Sprint sprOwnedRecentNameMatch
	Sprint sprFollowedRecentNameMatch
	Sprint sprOwnedRecentDescriptionMatch
	Sprint sprFollowedRecentDescriptionMatch
	Sprint sprOwnedOldNameMatch
	Sprint sprFollowedOldNameMatch
	Sprint sprOwnedOldDescriptionMatch
	Sprint sprFollowedOldDescriptionMatch
	Sprint sprPublicRecentNameMatch
	Sprint sprPublicRecentDescriptionMatch
	Sprint sprPublicOldNameMatch
	Sprint sprPublicOldDescriptionMatch
 	
	String searchTerm = uniqueTerm
	def recentDate = new Date()
	def oldDate = recentDate - 365
	def irrelevantDate = oldDate - 365	
	
	def printScores(def result) {
		if (result == null || !result.success || result.listItems == null || result.listItems.size == 0) {
			println ""
			println "no results to display scores for"
			println ""
			return
		}
		
		result.listItems.each {
			println "=============================================="
			println "=============================================="
			println "name: '$it.name'"
			println "hash: '$it.hash'"
			println "score: '$it.score'"
			println "explanation:"
			println "$it.explanation"
			println "=============================================="
			println "=============================================="
		}
	}

	def createObject(CreateType type, boolean index = true) {
		switch(type) {
			case CreateType.UserFollowedNameMatch:
				userFollowedNameMatch = User.create(
					[	username:'andrew',
						sex:'F',
						name:"userFollowedNameMatch andrew ranken $searchTerm",
						email:'andrew@pogues.com',
						birthdate:'03/01/1960',
						password:'andrewxyz',
						action:'doregister',
						controller:'home'	]
				)
				userFollowedNameMatch.settings.makeNamePublic()
				userFollowedNameMatch.settings.makeBioPublic()
				userFollowedNameMatch.follow(user1)
				Utils.save(userFollowedNameMatch, true)
				break;
			case CreateType.UserFollowedTagMatch:
				def tag = Tag.create(searchTerm)
				
				userFollowedTagMatch = User.create(
					[	username:'philip',
						sex:'M',
						name:'userFollowedTagMatch philip',
						email:'philip@pogues.com',
						birthdate:'04/01/1960',
						password:'philipxyz',
						action:'doregister',
						controller:'home'	]
				)
				userFollowedTagMatch.settings.makeNamePublic()
				userFollowedTagMatch.settings.makeBioPublic()
				userFollowedTagMatch.follow(user1)
				userFollowedTagMatch.addInterestTag(tag)
				Utils.save(userFollowedTagMatch, true)
				break;
			case CreateType.UserNonFollowedNameMatch:
				userNonFollowedNameMatch = User.create(
					[	username:'james',
						sex:'M',
						name:"userNonFollowedNameMatch james fearnley $searchTerm",
						email:'james@pogues.com',
						birthdate:'03/15/1960',
						password:'jamesxyz',
						action:'doregister',
						controller:'home'	]
				)
				userNonFollowedNameMatch.settings.makeNamePublic()
				userNonFollowedNameMatch.settings.makeBioPublic()
				Utils.save(userNonFollowedNameMatch, true)
				break;
			case CreateType.UserNonFollowedTagMatch:
				def tag = Tag.create(searchTerm)
				userNonFollowedTagMatch = User.create(
					[	username:'terry',
						sex:'F',
						name:'userNonFollowedTagMatch terry woods',
						email:'terry@pogues.com',
						birthdate:'08/24/1957',
						password:'terryxyz',
						action:'doregister',
						controller:'home'	]
				)
				userNonFollowedTagMatch.settings.makeNamePublic()
				userNonFollowedTagMatch.settings.makeBioPublic()
				userNonFollowedTagMatch.addInterestTag(tag)
				Utils.save(userNonFollowedTagMatch, true)
				break;
			case CreateType.DisOwnedRecentNameMatch:
				disOwnedRecentNameMatch = Discussion.create(user1, "disOwnedRecentNameMatch $uniqueName $searchTerm", null, recentDate, Visibility.PRIVATE)
				break;
			case CreateType.DisFollowedRecentNameMatch:
				def group = UserGroup.create("curious follower", "Following Group Level 1", "Group for following Discussions",
					[isReadOnly:false, defaultNotify:false])
				group.addMember(user1)
				group.addMember(user2)
				
				disFollowedRecentNameMatch = Discussion.create(user2, "disFollowedRecentNameMatch $uniqueName $searchTerm", group, recentDate, Visibility.UNLISTED)
				break;
			case CreateType.DisOwnedRecentFirstPostMessageMatch:
				disOwnedRecentFirstPostMessageMatch = Discussion.create(user1, "disOwnedRecentFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
				disOwnedRecentFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
				break;
			case CreateType.DisFollowedRecentFirstPostMessageMatch:
				def group = UserGroup.create("curious follower", "Following Group Level 1", "Group for following Discussions",
					[isReadOnly:false, defaultNotify:false])
				group.addMember(user1)
				group.addMember(user2)
				
				disFollowedRecentFirstPostMessageMatch = Discussion.create(user2, "disFollowedRecentFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
				disFollowedRecentFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
				break;
			case CreateType.DisOwnedRecentNonFirstPostMessageMatch:
				disOwnedRecentNonFirstPostMessageMatch = Discussion.create(user1, "disOwnedRecentNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
				disOwnedRecentNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
				disOwnedRecentNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
				break;
			case CreateType.DisFollowedRecentNonFirstPostMessageMatch:
				def group = UserGroup.create("curious follower", "Following Group Level 2", "Group for following Discussions",
					[isReadOnly:false, defaultNotify:false])
				group.addMember(user1)
				group.addMember(user2)
				
				disFollowedRecentNonFirstPostMessageMatch = Discussion.create(user2, "disFollowedRecentNonFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
				disFollowedRecentNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
				disFollowedRecentNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
				break;
			case CreateType.DisOwnedOldNameMatch:
				disOwnedOldNameMatch = Discussion.create(user1, "disOwnedOldNameMatch $uniqueName $searchTerm", null, oldDate, Visibility.PRIVATE)
				break;
			case CreateType.DisFollowedOldNameMatch:
				def group = UserGroup.create("curious follower", "Following Group Level 5", "Group for following Discussions",
					[isReadOnly:false, defaultNotify:false])
				group.addMember(user1)
				group.addMember(user2)
				
				disFollowedOldNameMatch = Discussion.create(user2, "disFollowedOldNameMatch $uniqueName $searchTerm", group, oldDate, Visibility.UNLISTED)
				break;
			case CreateType.DisOwnedOldFirstPostMessageMatch:
				disOwnedOldFirstPostMessageMatch = Discussion.create(user1, "disOwnedOldFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
				disOwnedOldFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
				break;
			case CreateType.DisFollowedOldFirstPostMessageMatch:
				def group = UserGroup.create("curious follower", "Following Group Level 5", "Group for following Discussions",
					[isReadOnly:false, defaultNotify:false])
				group.addMember(user1)
				group.addMember(user2)
				
				disFollowedOldFirstPostMessageMatch = Discussion.create(user2, "disFollowedOldFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
				disFollowedOldFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
				break;
			case CreateType.DisOwnedOldNonFirstPostMessageMatch:
				disOwnedOldNonFirstPostMessageMatch = Discussion.create(user1, "disOwnedOldNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
				disOwnedOldNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
				disOwnedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
				break;
			case CreateType.DisFollowedOldNonFirstPostMessageMatch:
				def group = UserGroup.create("curious follower", "Following Group Level 6", "Group for following Discussions",
					[isReadOnly:false, defaultNotify:false])
				group.addMember(user1)
				group.addMember(user2)
		
				disFollowedOldNonFirstPostMessageMatch = Discussion.create(user2, "disFollowedOldNonFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
				disFollowedOldNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
				disFollowedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
				break;
			case CreateType.DisPublicRecentNameMatch:
				disPublicRecentNameMatch = Discussion.create(user2, "disPublicRecentNameMatch $uniqueName $searchTerm", null, recentDate, Visibility.PUBLIC)
				break;
			case CreateType.DisPublicRecentFirstPostMessageMatch:
				disPublicRecentFirstPostMessageMatch = Discussion.create(user2, "disPublicRecentFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
				disPublicRecentFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
				break;
			case CreateType.DisPublicRecentNonFirstPostMessageMatch:
				disPublicRecentNonFirstPostMessageMatch = Discussion.create(user2, "disPublicRecentNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
				disPublicRecentNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
				disPublicRecentNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
				break;
			case CreateType.DisPublicOldNameMatch:
				disPublicOldNameMatch = Discussion.create(user2, "disPublicOldNameMatch $uniqueName $searchTerm", null, oldDate, Visibility.PUBLIC)
				break;
			case CreateType.DisPublicOldFirstPostMessageMatch:
				disPublicOldFirstPostMessageMatch = Discussion.create(user2, "disPublicOldFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
				disPublicOldFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
				break;
			case CreateType.DisPublicOldNonFirstPostMessageMatch:
				disPublicOldNonFirstPostMessageMatch = Discussion.create(user2, "disPublicOldNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
				disPublicOldNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
				disPublicOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
				break;
			case CreateType.SprOwnedRecentNameMatch:
				sprOwnedRecentNameMatch = Sprint.create(recentDate, user1, "sprOwnedRecentNameMatch $uniqueName $searchTerm", Visibility.PRIVATE)
				break;
			case CreateType.SprFollowedRecentNameMatch:
				sprFollowedRecentNameMatch = Sprint.create(recentDate, user2, "sprFollowedRecentNameMatch $uniqueName $searchTerm", Visibility.UNLISTED)
				sprFollowedRecentNameMatch.fetchUserGroup().addReader(user1)
				Utils.save(sprFollowedRecentNameMatch, true)
				break;
			case CreateType.SprOwnedRecentDescriptionMatch:
				sprOwnedRecentDescriptionMatch = Sprint.create(recentDate, user1, "sprOwnedRecentDescriptionMatch $uniqueName", Visibility.PRIVATE)
				sprOwnedRecentDescriptionMatch.description = "$uniqueName $searchTerm"
				Utils.save(sprOwnedRecentDescriptionMatch, true)
				break;
			case CreateType.SprFollowedRecentDescriptionMatch:
				sprFollowedRecentDescriptionMatch = Sprint.create(recentDate, user2, "sprFollowedRecentDescriptionMatch $uniqueName", Visibility.UNLISTED)
				sprFollowedRecentDescriptionMatch.description = "$uniqueName $searchTerm"
				sprFollowedRecentDescriptionMatch.fetchUserGroup().addReader(user1)
				Utils.save(sprFollowedRecentDescriptionMatch, true)
				break;
			case CreateType.SprOwnedOldNameMatch:
				sprOwnedOldNameMatch = Sprint.create(oldDate, user1, "sprOwnedOldNameMatch $uniqueName $searchTerm", Visibility.PRIVATE)
				break;
			case CreateType.SprFollowedOldNameMatch:
				sprFollowedOldNameMatch = Sprint.create(oldDate, user2, "sprFollowedOldNameMatch $uniqueName $searchTerm", Visibility.UNLISTED)
				sprFollowedOldNameMatch.fetchUserGroup().addReader(user1)
				Utils.save(sprFollowedOldNameMatch, true)
				break;
			case CreateType.SprOwnedOldDescriptionMatch:
				sprOwnedOldDescriptionMatch = Sprint.create(oldDate, user1, "sprOwnedOldDescriptionMatch $uniqueName", Visibility.PRIVATE)
				sprOwnedOldDescriptionMatch.description = "$uniqueName $searchTerm"
				Utils.save(sprOwnedOldDescriptionMatch, true)
				break;
			case CreateType.SprFollowedOldDescriptionMatch:
				sprFollowedOldDescriptionMatch = Sprint.create(oldDate, user2, "sprFollowedOldDescriptionMatch $uniqueName", Visibility.UNLISTED)
				sprFollowedOldDescriptionMatch.description = "$uniqueName $searchTerm"
				sprFollowedOldDescriptionMatch.fetchUserGroup().addReader(user1)
				Utils.save(sprFollowedOldDescriptionMatch, true)
				break;
			case CreateType.SprPublicRecentNameMatch:
				sprPublicRecentNameMatch = Sprint.create(recentDate, user2, "sprPublicRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
				break;
			case CreateType.SprPublicRecentDescriptionMatch:
				sprPublicRecentDescriptionMatch = Sprint.create(recentDate, user2, "sprPublicRecentDescriptionMatch $uniqueName", Visibility.PUBLIC)
				sprPublicRecentDescriptionMatch.description = "$uniqueName $searchTerm"
				Utils.save(sprPublicRecentDescriptionMatch, true)
				break;
			case CreateType.SprPublicOldNameMatch:
				sprPublicOldNameMatch = Sprint.create(oldDate, user2, "sprPublicOldNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
				break;
			case CreateType.SprPublicOldDescriptionMatch:
				sprPublicOldDescriptionMatch = Sprint.create(oldDate, user2, "sprPublicOldDescriptionMatch $uniqueName", Visibility.PUBLIC)
				sprPublicOldDescriptionMatch.description = "$uniqueName $searchTerm"
				Utils.save(sprPublicOldDescriptionMatch, true)
				break;
		}
		
		if (index) {
			elasticSearchService.index()
			elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		}
	}
	
	def createObjectPair(CreateType type1, CreateType type2) {
		createObject(type1,false)
		createObject(type2,true)
	}
	
	def setupGroup1Level1() {
		def group = UserGroup.create("curious follower", "Following Group Level 1", "Group for following Discussions",
			[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		group.addMember(user2) 
		
		disOwnedRecentNameMatch = Discussion.create(user1, "disOwnedRecentNameMatch $uniqueName $searchTerm", null, recentDate, Visibility.PRIVATE)
		disFollowedRecentNameMatch = Discussion.create(user2, "disFollowedRecentNameMatch $uniqueName $searchTerm", group, recentDate, Visibility.UNLISTED)
		disOwnedRecentFirstPostMessageMatch = Discussion.create(user1, "disOwnedRecentFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
		disOwnedRecentFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
		disFollowedRecentFirstPostMessageMatch = Discussion.create(user2, "disFollowedRecentFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
		disFollowedRecentFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
		
		sprOwnedRecentNameMatch = Sprint.create(recentDate, user1, "sprOwnedRecentNameMatch $uniqueName $searchTerm", Visibility.PRIVATE)
		sprFollowedRecentNameMatch = Sprint.create(recentDate, user2, "sprFollowedRecentNameMatch $uniqueName $searchTerm", Visibility.UNLISTED)
		sprFollowedRecentNameMatch.fetchUserGroup().addReader(user1)
		Utils.save(sprFollowedRecentNameMatch, true)
	}
	
	def setupGroup1Level2() {
		def group = UserGroup.create("curious follower", "Following Group Level 2", "Group for following Discussions",
			[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		group.addMember(user2)
		
		disOwnedRecentNonFirstPostMessageMatch = Discussion.create(user1, "disOwnedRecentNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
		disOwnedRecentNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
		disOwnedRecentNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
		disFollowedRecentNonFirstPostMessageMatch = Discussion.create(user2, "disFollowedRecentNonFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
		disFollowedRecentNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
		disFollowedRecentNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)

		sprOwnedRecentDescriptionMatch = Sprint.create(recentDate, user1, "sprOwnedRecentDescriptionMatch $uniqueName", Visibility.PRIVATE)
		sprOwnedRecentDescriptionMatch.description = "$uniqueName $searchTerm"
		Utils.save(sprOwnedRecentDescriptionMatch, true)
		sprFollowedRecentDescriptionMatch = Sprint.create(recentDate, user2, "sprFollowedRecentDescriptionMatch $uniqueName", Visibility.UNLISTED)
		sprFollowedRecentDescriptionMatch.description = "$uniqueName $searchTerm"
		sprFollowedRecentDescriptionMatch.fetchUserGroup().addReader(user1)
		Utils.save(sprFollowedRecentDescriptionMatch, true)
	}
	
	def setupGroup1Level3() {
		userFollowedNameMatch = User.create(
			[	username:'andrew',
				sex:'F',
				name:'userFollowedNameMatch andrew ranken $searchTerm',
				email:'andrew@pogues.com',
				birthdate:'03/01/1960',
				password:'andrewxyz',
				action:'doregister',
				controller:'home'	]
		)
		userFollowedNameMatch.settings.makeNamePublic()
		userFollowedNameMatch.settings.makeBioPublic()
		userFollowedNameMatch.follow(user1)
		Utils.save(userFollowedNameMatch, true)
	}
	
	def setupGroup1Level4() {
		def tag = Tag.create(searchTerm)
		
		userFollowedTagMatch = User.create(
			[	username:'philip',
				sex:'M',
				name:'userFollowedTagMatch philip',
				email:'philip@pogues.com',
				birthdate:'04/01/1960',
				password:'philipxyz',
				action:'doregister',
				controller:'home'	]
		)
		userFollowedTagMatch.settings.makeNamePublic()
		userFollowedTagMatch.settings.makeBioPublic()
		userFollowedTagMatch.follow(user1)
		userFollowedTagMatch.addInterestTag(tag)
		Utils.save(userFollowedTagMatch, true)
	}
	
	def setupGroup1Level5() {
		def group = UserGroup.create("curious follower", "Following Group Level 5", "Group for following Discussions",
			[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		group.addMember(user2)
		
		disOwnedOldNameMatch = Discussion.create(user1, "disOwnedOldNameMatch $uniqueName $searchTerm", null, oldDate, Visibility.PRIVATE)
		disFollowedOldNameMatch = Discussion.create(user2, "disFollowedOldNameMatch $uniqueName $searchTerm", group, oldDate, Visibility.UNLISTED)
		disOwnedOldFirstPostMessageMatch = Discussion.create(user1, "disOwnedOldFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
		disOwnedOldFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		disFollowedOldFirstPostMessageMatch = Discussion.create(user2, "disFollowedOldFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
		disFollowedOldFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		sprOwnedOldNameMatch = Sprint.create(oldDate, user1, "sprOwnedOldNameMatch $uniqueName $searchTerm", Visibility.PRIVATE)
		sprFollowedOldNameMatch = Sprint.create(oldDate, user2, "sprFollowedOldNameMatch $uniqueName $searchTerm", Visibility.UNLISTED)
		sprFollowedOldNameMatch.fetchUserGroup().addReader(user1)
		Utils.save(sprFollowedOldNameMatch, true)
	}
	
	def setupGroup1Level6() {
		def group = UserGroup.create("curious follower", "Following Group Level 6", "Group for following Discussions",
			[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		group.addMember(user2)

		disOwnedOldNonFirstPostMessageMatch = Discussion.create(user1, "disOwnedOldNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PRIVATE)
		disOwnedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", irrelevantDate)
		disOwnedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		disFollowedOldNonFirstPostMessageMatch = Discussion.create(user2, "disFollowedOldNonFirstPostMessageMatch $uniqueName", group, irrelevantDate, Visibility.UNLISTED)
		disFollowedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", irrelevantDate)
		disFollowedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		sprOwnedOldDescriptionMatch = Sprint.create(oldDate, user1, "sprOwnedOldDescriptionMatch $uniqueName", Visibility.PRIVATE)
		sprOwnedOldDescriptionMatch.description = "$uniqueName $searchTerm"
		Utils.save(sprOwnedOldDescriptionMatch, true)
		sprFollowedOldDescriptionMatch = Sprint.create(oldDate, user2, "sprFollowedOldDescriptionMatch $uniqueName", Visibility.UNLISTED)
		sprFollowedOldDescriptionMatch.description = "$uniqueName $searchTerm"
		sprFollowedOldDescriptionMatch.fetchUserGroup().addReader(user1)
		Utils.save(sprFollowedOldDescriptionMatch, true)
	}
	
	def setupGroup2Level1() {
		disPublicRecentNameMatch = Discussion.create(user2, "disPublicRecentNameMatch $uniqueName $searchTerm", null, recentDate, Visibility.PUBLIC)
		disPublicRecentFirstPostMessageMatch = Discussion.create(user2, "disPublicRecentFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
		sprPublicRecentNameMatch = Sprint.create(recentDate, user2, "sprPublicRecentNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
	}
	
	def setupGroup2Level2() {
		disPublicRecentNonFirstPostMessageMatch = Discussion.create(user2, "disPublicRecentNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
		sprPublicRecentDescriptionMatch = Sprint.create(recentDate, user2, "sprPublicRecentDescriptionMatch $uniqueName", Visibility.PUBLIC)
		sprPublicRecentDescriptionMatch.description = "$uniqueName $searchTerm"
		Utils.save(sprPublicRecentDescriptionMatch, true)
	}
	
	def setupGroup2Level3() {
		userNonFollowedNameMatch = User.create(
			[	username:'james',
				sex:'M',
				name:'userNonFollowedNameMatch james fearnley $searchTerm',
				email:'james@pogues.com',
				birthdate:'03/15/1960',
				password:'jamesxyz',
				action:'doregister',
				controller:'home'	]
		)
		userNonFollowedNameMatch.settings.makeNamePublic()
		userNonFollowedNameMatch.settings.makeBioPublic()
		Utils.save(userNonFollowedNameMatch, true)
	}
	
	def setupGroup2Level4() {
		def tag = Tag.create(searchTerm)
		userNonFollowedTagMatch = User.create(
			[	username:'terry',
				sex:'F',
				name:'userNonFollowedTagMatch terry woods',
				email:'terry@pogues.com',
				birthdate:'08/24/1957',
				password:'terryxyz',
				action:'doregister',
				controller:'home'	]
		)
		userNonFollowedTagMatch.settings.makeNamePublic()
		userNonFollowedTagMatch.settings.makeBioPublic()
		userNonFollowedTagMatch.addInterestTag(tag)
		Utils.save(userNonFollowedTagMatch, true)
	}
	
	def setupGroup2Level5() {
		disPublicOldNameMatch = Discussion.create(user2, "disPublicOldNameMatch $uniqueName $searchTerm", null, oldDate, Visibility.PUBLIC)
		disPublicOldFirstPostMessageMatch = Discussion.create(user2, "disPublicOldFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
		sprPublicOldNameMatch = Sprint.create(oldDate, user2, "sprPublicOldNameMatch $uniqueName $searchTerm", Visibility.PUBLIC)
	}
	
	def setupGroup2Level6() {
		disPublicOldNonFirstPostMessageMatch = Discussion.create(user2, "disPublicOldNonFirstPostMessageMatch $uniqueName", null, irrelevantDate, Visibility.PUBLIC)
		sprPublicOldDescriptionMatch = Sprint.create(oldDate, user2, "sprPublicOldDescriptionMatch $uniqueName", Visibility.PUBLIC)
		sprPublicOldDescriptionMatch.description = "$uniqueName $searchTerm"
		Utils.save(sprPublicOldDescriptionMatch, true)
	}
	

    def setup() {
//		searchTerm = uniqueTerm
//		recentDate = new Date()
//		oldDate = recentDate - 365
//		irrelevantDate = oldDate - 365 // date for the discussion, which is not relevant when looking at posts
    }
}

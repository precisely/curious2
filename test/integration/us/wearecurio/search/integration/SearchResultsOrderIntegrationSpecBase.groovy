package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup

import us.wearecurio.utility.Utils

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
	 
	def setupGroup1Level1() {
		def group = UserGroup.create("curious follower", "Following Group Level 1", "Group for following Discussions",
			[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		group.addMember(user2) 
		
		disOwnedRecentNameMatch = Discussion.create(user1, "$uniqueName $searchTerm", null, recentDate, Visibility.PRIVATE)
		disFollowedRecentNameMatch = Discussion.create(user2, "$uniqueName $searchTerm", group, recentDate, Visibility.UNLISTED)
		disOwnedRecentFirstPostMessageMatch = Discussion.create(user1, uniqueName, null, irrelevantDate, Visibility.PRIVATE)
		disOwnedRecentFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
		disFollowedRecentFirstPostMessageMatch = Discussion.create(user2, uniqueName, group, irrelevantDate, Visibility.UNLISTED)
		disFollowedRecentFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
		
		sprOwnedRecentNameMatch = Sprint.create(recentDate, user1, "$uniqueName $searchTerm", Visibility.PRIVATE)
		sprFollowedRecentNameMatch = Sprint.create(recentDate, user2, "$uniqueName $searchTerm", Visibility.UNLISTED)
		sprFollowedRecentNameMatch.fetchUserGroup().addReader(user1)
		Utils.save(sprFollowedRecentNameMatch, true)
	}
	
	def setupGroup1Level2() {
		def group = UserGroup.create("curious follower", "Following Group Level 2", "Group for following Discussions",
			[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		group.addMember(user2)
		
		disOwnedRecentNonFirstPostMessageMatch = Discussion.create(user1, uniqueName, null, irrelevantDate, Visibility.PRIVATE)
		disOwnedRecentNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
		disOwnedRecentNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)
		disFollowedRecentNonFirstPostMessageMatch = Discussion.create(user2, uniqueName, group, irrelevantDate, Visibility.UNLISTED)
		disFollowedRecentNonFirstPostMessageMatch.createPost(user3, uniqueName, irrelevantDate)
		disFollowedRecentNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", recentDate)

		sprOwnedRecentDescriptionMatch = Sprint.create(recentDate, user1, uniqueName, Visibility.PRIVATE)
		sprOwnedRecentDescriptionMatch.description = "$uniqueName $searchTerm"
		Utils.save(sprOwnedRecentDescriptionMatch, true)
		sprFollowedRecentDescriptionMatch = Sprint.create(recentDate, user2, uniqueName, Visibility.UNLISTED)
		sprFollowedRecentDescriptionMatch.description = "$uniqueName $searchTerm"
		sprFollowedRecentDescriptionMatch.fetchUserGroup().addReader(user1)
		Utils.save(sprFollowedRecentDescriptionMatch, true)
	}
	
	def setupGroup1Level3() {
		userFollowedNameMatch = User.create(
			[	username:'andrew',
				sex:'F',
				name:'andrew ranken $searchTerm',
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
				name:'philip',
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
		
		disOwnedOldNameMatch = Discussion.create(user1, "$uniqueName $searchTerm", null, oldDate, Visibility.PRIVATE)
		disFollowedOldNameMatch = Discussion.create(user2, "$uniqueName $searchTerm", group, oldDate, Visibility.UNLISTED)
		disOwnedOldFirstPostMessageMatch = Discussion.create(user1, uniqueName, null, irrelevantDate, Visibility.PRIVATE)
		disOwnedOldFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		disFollowedOldFirstPostMessageMatch = Discussion.create(user2, uniqueName, group, irrelevantDate, Visibility.UNLISTED)
		disFollowedOldFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		sprOwnedOldNameMatch = Sprint.create(oldDate, user1, "$uniqueName $searchTerm", Visibility.PRIVATE)
		sprFollowedOldNameMatch = Sprint.create(oldDate, user2, "$uniqueName $searchTerm", Visibility.UNLISTED)
	}
	
	def setupGroup1Level6() {
		def group = UserGroup.create("curious follower", "Following Group Level 6", "Group for following Discussions",
			[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)

		disOwnedOldNonFirstPostMessageMatch = Discussion.create(user1, uniqueName, null, irrelevantDate, Visibility.PRIVATE)
		disOwnedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", irrelevantDate)
		disOwnedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		disFollowedOldNonFirstPostMessageMatch = Discussion.create(user2, uniqueName, group, irrelevantDate, Visibility.UNLISTED)
		disFollowedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", irrelevantDate)
		disFollowedOldNonFirstPostMessageMatch.createPost(user3, "$uniqueName $searchTerm", oldDate)
		sprOwnedOldDescriptionMatch = Sprint.create(oldDate, user1, uniqueName, Visibility.PRIVATE)
		sprOwnedOldDescriptionMatch.description = "$uniqueName $searchTerm"
		Utils.save(sprOwnedOldDescriptionMatch, true)
		sprFollowedOldDescriptionMatch = Sprint.create(oldDate, user2, uniqueName, Visibility.UNLISTED)
		sprFollowedOldDescriptionMatch.description = "$uniqueName $searchTerm"
		sprFollowedOldDescriptionMatch.fetchUserGroup().addReader(user1)
		Utils.save(sprFollowedOldDescriptionMatch, true)
	}
	
	def setupGroup2Level1() {
		disPublicRecentNameMatch = Discussion.create(user2, "$uniqueName $searchTerm", null, recentDate, Visibility.PUBLIC)
		disPublicRecentFirstPostMessageMatch = Discussion.create(user2, uniqueName, null, irrelevantDate, Visibility.PUBLIC)
		sprPublicRecentNameMatch = Sprint.create(recentDate, user2, "$uniqueName $searchTerm", Visibility.PUBLIC)
	}
	
	def setupGroup2Level2() {
		disPublicRecentNonFirstPostMessageMatch = Discussion.create(user2, uniqueName, null, irrelevantDate, Visibility.PUBLIC)
		sprPublicRecentDescriptionMatch = Sprint.create(recentDate, user2, uniqueName, Visibility.PUBLIC)
		sprPublicRecentDescriptionMatch.description = "$uniqueName $searchTerm"
		Utils.save(sprPublicRecentDescriptionMatch, true)
	}
	
	def setupGroup2Level3() {
		userNonFollowedNameMatch = User.create(
			[	username:'james',
				sex:'M',
				name:'james fearnley $searchTerm',
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
				name:'terry woods',
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
		disPublicOldNameMatch = Discussion.create(user2, "$uniqueName $searchTerm", null, oldDate, Visibility.PUBLIC)
		disPublicOldFirstPostMessageMatch = Discussion.create(user2, uniqueName, null, irrelevantDate, Visibility.PUBLIC)
		sprPublicOldNameMatch = Sprint.create(oldDate, user2, "$uniqueName $searchTerm", Visibility.PUBLIC)
	}
	
	def setupGroup2Level6() {
		disPublicOldNonFirstPostMessageMatch = Discussion.create(user2, uniqueName, null, irrelevantDate, Visibility.PUBLIC)
		sprPublicOldDescriptionMatch = Sprint.create(oldDate, user2, uniqueName, Visibility.PUBLIC)
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

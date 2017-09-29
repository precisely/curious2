package us.wearecurio.controller

import com.causecode.fileuploader.FileUploaderService
import com.causecode.fileuploader.UFile
import com.causecode.fileuploader.UFileMoveHistory
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.PlotData
import us.wearecurio.model.PushNotificationDevice
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.model.profiletags.ProfileTag
import us.wearecurio.services.SearchService
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils

class UserController extends LoginController {

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

	FileUploaderService fileUploaderService
	SearchService searchService

	// This method updates the user settings bits based on the method name passed as string.
	protected updateUserSettings(String methodName) {
		User currentUser = sessionUser()
		if (!currentUser) {
			debug "auth failure"
			renderJSONGet([success: false])
			return
		}

		currentUser.settings?."$methodName"()

		// Saving user as UserSettings is an embedded domain
		if (Utils.save(currentUser, true)) {
			renderJSONGet([success: true])
		} else {
			renderJSONGet([success: false, message: g.message(code: "default.not.updated.message", args: ["User", "preferences"])])
		}
	}

	def index() {

	}

	def show() {
		debug ("UserController.show() params:" + params)
		User user = User.findByHash(params.id)

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}

		Map userDetails
		User sessionUser = sessionUser()
		if (user.id != sessionUser.id) {
			userDetails = user.getPublicJSONDesc()
		} else {
			userDetails = user.getPeopleJSONDesc()
		}
		userDetails.followed = sessionUser.follows(user);
		renderJSONGet([success: true, user: userDetails])
	}

	def follow() {
		debug ("UserController.follow() params:" + params)
		User followed = User.findByHash(params.id)
		boolean unfollow = params.unfollow ? true : false

		if (!followed) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}

		User follower = sessionUser()

		boolean retVal
		
		if (unfollow)
			retVal = follower.unFollow(followed)
		else
			retVal = follower.follow(followed)

		renderJSONGet([success: retVal])
	}

	/**
	 * Used to update the avatar for current logged in user.
	 */
	def saveAvatar() {
		debug ("UserController.saveAvatar() params:" + params)
		UFile avatar
		try {
			avatar = fileUploaderService.saveFile("avatar", params.avatar)
		} catch (Exception e) {
			Utils.reportError("Error while saving avatar", e)
			renderJSONPost([success: false, message: g.message(code: "default.not.updated.message",
					args: ["Profile", "image"])])
			return
		}

		if (!avatar) {
			renderJSONPost([success: false, message: g.message(code: "default.not.updated.message",
					args: ["Profile", "image"])])
			return
		}

		User currentUserInstance = sessionUser()

		UFile.withTransaction {
			try {
				UFileMoveHistory.findByUfile(currentUserInstance.avatar)?.delete()
				currentUserInstance.avatar?.delete()
				currentUserInstance.avatar = avatar
				Utils.save(currentUserInstance, true)
				renderJSONPost([success: true, avatarURL: avatar.path])
				currentUserInstance.reindexAssociations()
			} catch (Exception e) {
				Utils.reportError("Unable to change or add avatar for ${currentUserInstance}", e)
				renderJSONPost([success: false], message: g.message(code: "default.not.updated.message",
						args: ["Profile", "image"]))
			}
		}
	}

	def update() {
		debug ("UserController.update() params:" + params)
		Map requestData = request.JSON
		User user = User.findByHash(params.id)

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}

		if (user.id != sessionUser().id) {
			renderJSONGet([success: false, message: g.message(code: "default.permission.denied")])
			return
		}
		Map validate = user.validateUserPreferences(requestData, user)

		if (!validate.status) {
			renderJSONGet([success: false, message: validate.message])
		} else {
			renderJSONGet([success: true, message: validate.message, hash: validate.hash])
		}
	}

	def closeExplanationCardCuriosity() {
		debug "UserController.closeExplanationCardCuriosity()"
		updateUserSettings("closeCuriositiesExplanation")
	}

	def closeExplanationCardTrackathon() {
		debug "UserController.closeExplanationCardTrackathon()"
		updateUserSettings("closeTrackathonExplanation")
	}

	def markTrackathonVisited() {
		debug "UserController.closeExplanationCardTrackathon()"
		updateUserSettings("markTrackathonVisited")
	}

	def markFirstChartPlotted() {
		debug "UserController.markFirstChartPlotted()"
		updateUserSettings("markFirstChartPlot")
	}

	def addTutorialTags() {
		User user = sessionUser()
		if (!user) {
			debug "auth failure"
			renderJSONGet([success: false])
			return
		}
		
		log.debug "Saving tutorial tags, params: " + params['tags[]']
		
		Long userId = user.id
		
		EntryStats stats = new EntryStats(userId)
		
		def tags = params['tags[]']
		
		if (tags instanceof String)
			tags = [tags]
		
		for (String tag in tags) {
			log.debug "Tag: " + tag
			if (tag == 'sleep') {
				Entry.createBookmark(userId, "sleep", stats)
				Entry.createBookmark(userId, "sleep quality", stats)
				user.addInterestTag(Tag.look("sleep"))
			} else if (tag == 'mood') {
				Entry.createBookmark(userId, "mood", stats)
				Entry.createBookmark(userId, "stress level", stats)
				user.addInterestTag(Tag.look("mood"))
				user.addInterestTag(Tag.look("stress"))
			} else if (tag == 'fitness') {
				Entry.createBookmark(userId, "exercise", stats)
				Entry.createBookmark(userId, "bike", stats)
				Entry.createBookmark(userId, "walk", stats)
				Entry.createBookmark(userId, "run", stats)
				user.addInterestTag(Tag.look("exercise"))
				user.addInterestTag(Tag.look("biking"))
				user.addInterestTag(Tag.look("walking"))
				user.addInterestTag(Tag.look("running"))
			} else if (tag == 'food') {
				Entry.createBookmark(userId, "weight", stats)
				Entry.createBookmark(userId, "coffee", stats)
				Entry.createBookmark(userId, "carbs", stats)
				Entry.createBookmark(userId, "vegetables", stats)
				Entry.createBookmark(userId, "fruits", stats)
				user.addInterestTag(Tag.look("food"))
				user.addInterestTag(Tag.look("diet"))
			} else if (tag == 'supplements') {
				Entry.createBookmark(userId, "fish oil", stats)
				Entry.createBookmark(userId, "vitamin c", stats)
				Entry.createBookmark(userId, "magnesium", stats)
				Entry.createBookmark(userId, "probiotics", stats)
				user.addInterestTag(Tag.look("supplements"))
			}
		}
		
		stats.finish()
		
		renderJSONGet([success: true])
	}

	def getGroupsToShare() {
		debug "UserController.getGroupsToShare()"
		User user = sessionUser()
		
		UserGroup defaultGroup = UserGroup.getDefaultGroupForUser(user)
		
		List groups = UserGroup.getConcreteGroupsForWriter(user)
		
		renderJSONGet([groups: groups.findAll { it.id != defaultGroup.id }, success: true])
	}

	def deleteAccount() {
		User user = sessionUser()
		debug "Delete User account with username ${user.username}"

		User.withTransaction { status ->
			try {
				OAuthAccount oAuthAccount = OAuthAccount.findByUserIdAndTypeId(user.id, ThirdParty.TWENTY_THREE_AND_ME)

				// Remove Twenty3AndMeData.
				Twenty3AndMeData.executeUpdate("DELETE FROM Twenty3AndMeData twenty3AndMeData where" +
						" twenty3AndMeData.account = :account", [account: oAuthAccount])

				// To disable polling.
				OAuthAccount.executeUpdate("DELETE FROM OAuthAccount account where account.userId = :userId",
						[userId: user.id]);

				// Remove user devices for Push notificaiton.
				PushNotificationDevice.executeUpdate("DELETE FROM PushNotificationDevice device where " +
						"device.userId = :userId", [userId: user.id])

				// Remove User's profile tags
				ProfileTag.executeUpdate("DELETE FROM ProfileTag profileTag where profileTag.userId = :userId",
						[userId: user.id])

				PlotData.executeUpdate("DELETE FROM PlotData plot where plot.userId = :userId and " +
						"plot.isSnapshot = false", [userId: user.id])

				// Delete all trackathons created by User.
				List sprintList = Sprint.findAllByUserId(user.id)

				sprintList.each { Sprint sprint ->
					Sprint.delete(sprint)
				}

				// Delete Discussions started by User
				List discussionList = Discussion.findAllByUserId(user.id)

				discussionList.each { Discussion discussion ->
					Discussion.delete(discussion)
				}

				// Delete discussion posts by the User
				DiscussionPost.executeUpdate("DELETE FROM DiscussionPost p where p.authorUserId = :userId",
						[userId: user.id]);

				// Delete Entry.
				Entry.executeUpdate("DELETE FROM Entry entry where entry.userId = :userId", [userId: user.id]);

				User.delete(user)
			} catch (Exception e) {
				log.error "Error while deleting user account", e

				status.setRollbackOnly()

				renderJSONGet([success: false])
				return
			}
		}

		SearchService.get().deindex(user)
		renderJSONGet([success: true])
	}
}

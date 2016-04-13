package us.wearecurio.controller

import com.lucastex.grails.fileuploader.FileUploaderService
import com.lucastex.grails.fileuploader.UFile
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.services.SearchService
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils

class UserController extends LoginController {

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

	FileUploaderService fileUploaderService
	SearchService searchService

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
		} catch (FileNotFoundException | IOException e) {		// https://docs.oracle.com/javase/tutorial/essential/exceptions/catch.html
			renderJSONPost([success: false, message: g.message(code: "default.not.updated.message", args: ["Avatar"])])
			return
		}

		if (!avatar) {
			renderJSONPost([success: false, message: g.message(code: "default.not.updated.message", args: ["Avatar"])])
			return
		}
		User currentUserInstance = sessionUser()

		try {
			currentUserInstance.avatar?.delete()
			currentUserInstance.avatar = avatar
			Utils.save(currentUserInstance, true)
			renderJSONPost([success: true, avatarURL: avatar.path])
			currentUserInstance.reindexAssociations()
		} catch (Exception e) {
			log.error "Unable to change or add avatar for ${currentUserInstance}", e
			renderJSONPost([success: false], message: g.message(code: "default.not.updated.message", args: ["Avatar"]))
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
		String methodName = "closeCuriositiesExplanation"
		updateUserSettings(methodName)
	}

	def closeExplanationCardTrackathon() {
		debug "UserController.closeExplanationCardTrackathon()"
		String methodName = "closeTrackathonExplanation"
		updateUserSettings(methodName)
	}

	def markTrackathonVisited() {
		debug "UserController.closeExplanationCardTrackathon()"
		String methodName = "markTrackathonVisited"
		updateUserSettings(methodName)
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
}

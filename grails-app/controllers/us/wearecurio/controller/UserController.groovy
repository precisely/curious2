package us.wearecurio.controller
import com.lucastex.grails.fileuploader.FileUploaderService
import com.lucastex.grails.fileuploader.UFile
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

class UserController extends LoginController {

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

	FileUploaderService fileUploaderService

	def index() {

	}

	def show() {
		debug ("UserController.show() params:" + params)
		User user = User.findByHash(params.id)

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}
		Map userDetails = [:]
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
		def validate = [:]
		User user = User.findByHash(params.id)

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}

		if (user.id != sessionUser().id) {
			renderJSONGet([success: false, message: g.message(code: "default.permission.denied")])
			return
		}
		validate = user.validateUserPreferences(requestData, user)

		if (!validate.status) {
			renderJSONGet([success: false, message: validate.message])
		} else {
			renderJSONGet([success: true, message: validate.message, hash: validate.hash])
		}
	}

	def closeExplanationCardCuriosity() {
		User currentUser = sessionUser()
		if (!currentUser) {
			debug "auth failure"
			renderJSONGet([success: false])
			return
		}

		currentUser.settings?.closeCuriositiesExplanation()

		// Saving user as UserSettings is an embedded domain
		if (Utils.save(currentUser, true)) {
			renderJSONGet([success: true])
		} else {
			renderJSONGet([success: false, message: g.message(code: "default.not.updated.message", args: ["User", "preferences"])])
		}
	}

	def closeExplanationCardTrackathon() {
		User currentUser = sessionUser()
		if (!currentUser) {
			debug "auth failure"
			renderJSONGet([success: false])
			return
		}

		currentUser.settings?.closeTrackathonExplanation()

		// Saving user as UserSettings is an embedded domain
		if (Utils.save(currentUser, true)) {
			renderJSONGet([success: true])
		} else {
			renderJSONGet([success: false, message: g.message(code: "default.not.updated.message", args: ["User", "preferences"])])
		}
	}
	
	def addTutorialTags() {
		User currentUser = sessionUser()
		if (!currentUser) {
			debug "auth failure"
			renderJSONGet([success: false])
			return
		}

		renderJSONGet([success: true])
	}
}

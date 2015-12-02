package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.*
import us.wearecurio.utility.*
import com.lucastex.grails.fileuploader.UFile
import com.lucastex.grails.fileuploader.FileUploaderService

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
		if (user.id != sessionUser().id) {
			userDetails = user.getPublicJSONDesc()
		} else {
			userDetails = user.getPeopleJSONDesc()
		}
		renderJSONGet([success: true, user: userDetails])
	}

	def follow() {
		debug ("UserController.follow() params:" + params)
		User followed = User.findByHash(params.id)

		if (!followed) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}

		User follower = sessionUser()

		boolean retVal = follower.follow(followed)

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
			return
		}

		if (!validate.status) {
			renderJSONGet([success: false, message: validate.message])
		} else {
			renderJSONGet([success: true, message: validate.message, hash: validate.hash])
		}
	}
}

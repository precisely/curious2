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
		User user = User.findByHash(params.id)

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}
		renderJSONGet([success: true, user: user.getPeopleJSONDesc()])
	}

	/**
	 * Used to update the avatar for current logged in user.
	 */
	def saveAvatar() {
		debug ("HomeController.saveAvatar() params:" + params)
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
}

package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.*
import us.wearecurio.utility.*

class UserController extends LoginController {

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

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
}

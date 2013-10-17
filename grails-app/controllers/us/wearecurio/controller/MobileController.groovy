package us.wearecurio.controller

import grails.util.Environment

import org.apache.commons.logging.LogFactory

class MobileController extends SessionController {

	private static def log = LogFactory.getLog(this)

	static debug(str) {
		log.debug(str)
	}

	def MobileController() {
	}

	def cachemanifest() {
		if(Environment.current == Environment.DEVELOPMENT) {
			response.sendError(500)	// Skipping cache on development.
		}
		debug "MobileController.cachemanifest()"
		render(contentType:"text/cache-manifest", view:"/mobile/cachemanifest", model:[templateVer:urlService.template(request)])
	}

	def index() {
		debug "MobileController.index()"

		def user = sessionUser()
		if (user != null) {
			debug "auth user:" + user
			def entryId = -1
			if (params.entryId) {
				entryId = Long.parseLong(params.entryId)
			}
			[prefs:user.getPreferences(), login:0, templateVer:urlService.template(request), entryId:entryId]
		} else {
			debug "auth failure"
			[prefs:null, login:0, templateVer:urlService.template(request), entryId:-1]
		}
	}

	def login() {
		debug "MobileController.login()"

		render(view: "/mobile/index", model: [login:1,templateVer:urlService.template(request)])
	}
}

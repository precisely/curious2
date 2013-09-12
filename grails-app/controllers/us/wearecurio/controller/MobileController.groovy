package us.wearecurio.controller

import grails.converters.*
import us.wearecurio.model.*
import us.wearecurio.exceptions.*
import us.wearecurio.utility.Utils
import us.wearecurio.services.TwitterDataService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.SimpleTimeZone
import us.wearecurio.server.Session
import org.apache.commons.logging.LogFactory

class MobileController extends SessionController {
	
	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}
	
	def MobileController() {
	}

	def cachemanifest() {
		debug "MobileController.cachemanifest()"
		render(contentType:"text/cache-manifest", view:"/mobile/cachemanifest", model:[templateVer:urlService.template(request)])
	}

	def index() {
		debug "MobileController.index()"
		
		def user = sessionUser()
		if (user != null) {
			debug "auth user:" + user
			[prefs:user.getPreferences(),login:0,templateVer:urlService.template(request)]
		} else {
			debug "auth failure"
			[prefs:null,login:0,templateVer:urlService.template(request)]
		}
	}

	def login() {
		debug "MobileController.login()"
		
		render(view:"/mobile/index",
				model:[login:1,templateVer:urlService.template(request)])
	}
}

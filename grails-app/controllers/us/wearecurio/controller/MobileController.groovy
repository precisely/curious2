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
	
	def index() {
		def htmlContent = new File('./web-app/mobile/main.html').text
		render text: htmlContent, contentType:"text/html", encoding:"UTF-8"
	}
}

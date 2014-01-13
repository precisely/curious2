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
		redirect(uri:urlService.make([controller:"mobile",action:"main.html"]))
	}
}

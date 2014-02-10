package us.wearecurio.controller

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.exceptions.*
import us.wearecurio.model.*
import us.wearecurio.utility.Utils

class PublicController {
	
	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}
	
	def PublicController() {
		debug "PublicController()"
	}

	def lgmd2iproject() {
		def model = []
		render(view:"/public/lgmd2iproject", model:model)
	}
}

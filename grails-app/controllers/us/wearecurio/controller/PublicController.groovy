package us.wearecurio.controller

import grails.converters.JSON
import org.apache.commons.logging.LogFactory

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

	def getSurveyOptions() {
		debug "PublicController.getSurveyOptions()"
		Map surveyOptions = ["Sleep": "sleep", "Mood": "mood", "Fitness": "fitness", 
				"Productivity": "productivity", "Food": "food", "Supplements": "supplements"]
		render "${params.callback}(${new JSON(surveyOptions)})", contentType: "application/javascript"
	}
}

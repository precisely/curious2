package us.wearecurio.jobs

import us.wearecurio.services.*
import grails.util.Environment

class AlertGenerationJob extends us.wearecurio.utility.TimerJob {
	static transactional = false
	AlertGenerationService alertGenerationService

	static triggers = {
		simple startDelay: 0, repeatInterval: 120 * MINUTE
	}

	def execute() {
		log.debug "Started executing AlertGenerationJob..."
		alertGenerationService.generateAlerts(new Date())
		log.debug "Finished executing AlertGenerationJob..."
	}
}

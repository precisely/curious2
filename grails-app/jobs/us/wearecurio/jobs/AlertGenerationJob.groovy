package us.wearecurio.jobs

import us.wearecurio.services.*
import grails.util.Environment

class AlertGenerationJob extends us.wearecurio.utility.TimerJob {
	static transactional = false
	RemindEmailService remindEmailService

	static triggers = {
		simple startDelay: 0, repeatInterval: 120 * MINUTE
	}

	def execute() {
		log.debug "Started executing AlertGenerationJob..."
		remindEmailService.sendReminders()
		log.debug "Finished executing AlertGenerationJob..."
	}
}

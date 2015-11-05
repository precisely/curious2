package us.wearecurio.jobs

import us.wearecurio.services.*
import grails.util.Environment

class RemindEmailJob extends us.wearecurio.utility.TimerJob {
	static transactional = false
	RemindEmailService remindEmailService

	static triggers = {
		simple startDelay: 1 * MINUTE, repeatInterval: 2 * MINUTE
	}

	def execute() {
		if (Environment.current != Environment.PRODUCTION)
			return
		log.debug "Started executing RemindEmailJob..."
		remindEmailService.sendReminders(new Date())
		log.debug "Finished executing RemindEmailJob..."
	}
}

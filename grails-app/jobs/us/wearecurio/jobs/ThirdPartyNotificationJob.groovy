package us.wearecurio.jobs

import grails.util.Environment
import us.wearecurio.services.OuraDataService

class ThirdPartyNotificationJob extends us.wearecurio.utility.TimerJob {
	static transactional = false

	static triggers = {
		simple startDelay: 10 * MINUTE, repeatInterval: 5 * MINUTE
	}

	def group = "curious"
	def concurrent = false

	def fitBitDataService
	def withingsDataService
	OuraDataService ouraDataService

	def execute() {
		log.debug "Started executing ThirdPartyNotificationJob.."
		if (Environment.current != Environment.PRODUCTION) {
			log.debug "Aborted ThirdPartyNotificationJob.."
			return // don't send reminders in test or development mode
		}
		fitBitDataService.notificationProcessor()
		withingsDataService.notificationProcessor()
		ouraDataService.notificationProcessor()
		log.debug "Finished executing ThirdPartyNotificationJob."
	}
}

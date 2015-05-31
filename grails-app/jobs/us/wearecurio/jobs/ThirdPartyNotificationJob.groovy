package us.wearecurio.jobs

import us.wearecurio.model.ThirdPartyNotification

import grails.util.Environment

class ThirdPartyNotificationJob extends us.wearecurio.utility.TimerJob {
	static transactional = false

	static triggers = {
		simple startDelay: 10 * MINUTE, repeatInterval: 5 * MINUTE
	}

	def group = "curious"

	def fitBitDataService
	def withingsDataService

	def execute() {
		log.debug "Started executing ThirdPartyNotificationJob.."
		if (Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST) {
			log.debug "Aborted ThirdPartyNotificationJob.."
			return // don't send reminders in test or development mode
		}
		fitBitDataService.notificationProcessor()
		withingsDataService.notificationProcessor()
		log.debug "Finished executing ThirdPartyNotificationJob."
	}
}

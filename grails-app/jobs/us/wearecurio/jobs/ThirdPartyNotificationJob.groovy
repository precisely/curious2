package us.wearecurio.jobs

import us.wearecurio.model.ThirdPartyNotification

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
		fitBitDataService.notificationProcessor()
		withingsDataService.notificationProcessor()
		log.debug "Finished executing ThirdPartyNotificationJob."
	}
}

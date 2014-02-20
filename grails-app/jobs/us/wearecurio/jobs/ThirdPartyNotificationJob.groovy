package us.wearecurio.jobs

import us.wearecurio.model.ThirdPartyNotification

class ThirdPartyNotificationJob extends us.wearecurio.utility.TimerJob {

	static triggers = {
		simple startDelay: 2 * MINUTE, repeatInterval: 2 * MINUTE
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

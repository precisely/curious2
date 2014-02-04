package us.wearecurio.jobs

import us.wearecurio.model.ThirdPartyNotification

class FitbitNotificationJob extends us.wearecurio.utility.TimerJob {

	static triggers = {
		simple startDelay: 2 * MINUTE, repeatInterval: 15 * MINUTE
	}

	def group = "curious"
	def fitBitDataService

	def execute() {
		def c = ThirdPartyNotification.createCriteria()

		/*List<ThirdPartyNotification> results = ThirdPartyNotification.createCriteria().list() {
			eq("status", NotificationStatus.UNPROCESSED)
			maxResults(100)
			order ("date", "asc")
		}

		results.each { notification ->
			if (fitBitDataService.poll(notification)) {
				notification.status = NotificationStatus.PROCESSED
				notification.save()
			}
		}*/
	}
}

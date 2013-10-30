package us.wearecurio.jobs

import us.wearecurio.model.FitbitNotification;
import us.wearecurio.services.FitBitDataService;

class FitbitNotificationJob extends us.wearecurio.utility.TimerJob {
    static triggers = {
		simple startDelay: 2 * MINUTE, repeatInterval: 15 * MINUTE
    }

	def group = "curious"
	FitBitDataService fitBitDataService

	def execute() {
		def c = FitbitNotification.createCriteria()

		def results = c {
			eq("status", FitbitNotification.Status.UNPROCESSED)
			maxResults(100)
			order ("date", "asc")
		}

		results.each { notification ->
			if (fitBitDataService.poll(notification)) {
				notification.status = FitbitNotification.Status.PROCESSED
				notification.save()
			}			
		}
	}
}

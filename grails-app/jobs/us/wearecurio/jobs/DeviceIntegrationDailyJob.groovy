package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob

class DeviceIntegrationDailyJob extends TimerJob {
	static transactional = false

	def oauthAccountService
	def withingsDataService

	static triggers = {
		simple repeatInterval: DAY
	}

	def execute() {
		log.debug "Started executing Daily basis job.."
		oauthAccountService.refreshAllToken()
		//withingsDataService.refreshSubscriptions()
		log.debug "Finished executing Daily basis job.."
	}

}
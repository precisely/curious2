package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob
import us.wearecurio.model.UniqueTimedToken

class DeviceIntegrationDailyJob extends TimerJob {
	static transactional = false

	def OAuthAccountService
	def withingsDataService

	static triggers = {
		simple repeatInterval: DAY
	}

	def execute() {
		log.debug "Started executing Daily basis job.."
		OAuthAccountService.refreshAllToken()
		withingsDataService.refreshSubscriptions()
		UniqueTimedToken.clearExpiredTokens()
		log.debug "Finished executing Daily basis job.."
	}

}
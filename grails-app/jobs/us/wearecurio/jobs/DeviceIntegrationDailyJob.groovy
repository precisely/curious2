package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob

class DeviceIntegrationDailyJob extends TimerJob {
	static transactional = false

	def oauthAccountService
	def withingsDataService

	static triggers = {
		cron name:'deviceIntegrationTrigger', startDelay: 2 * HOUR, cronExpression: '0 30 2 * * ? *' //2:30 AM 
	}

	def execute() {
		log.debug "Started executing Daily basis job.."
		oauthAccountService.refreshAllToken()
		withingsDataService.refreshSubscriptions()
		log.debug "Finished executing Daily basis job.."
	}

}

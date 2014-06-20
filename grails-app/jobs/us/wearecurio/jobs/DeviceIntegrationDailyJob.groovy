package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob

class DeviceIntegrationDailyJob extends TimerJob {
	static transactional = false

	def oauthAccountService
	def withingsDataService

	static triggers = {
		cron name:'cronTrigger', cronExpression: '0 15 2 * * ? *' //2:15 AM 
	}

	def execute() {
		log.debug "Started executing Daily basis job.."
		oauthAccountService.refreshAllToken()
		withingsDataService.refreshSubscriptions()
		log.debug "Finished executing Daily basis job.."
	}

}

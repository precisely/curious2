package us.wearecurio.jobs

import us.wearecurio.utility.TimerJob
import grails.util.Environment

class DeviceIntegrationDailyJob extends TimerJob {
	static transactional = false

	def oauthAccountService
	def withingsDataService

	static triggers = {
		cron name:'deviceIntegrationTrigger', startDelay: 2 * HOUR, cronExpression: '0 30 2 * * ? *' //2:30 AM 
	}

	def execute() {
		log.debug "Started executing Daily basis job.."
		if (Environment.current != Environment.PRODUCTION) {
			log.debug "Aborted executing Daily basis job.."
			return
		}
		oauthAccountService.refreshAllToken()
		withingsDataService.refreshSubscriptions()
		log.debug "Finished executing Daily basis job.."
	}

}

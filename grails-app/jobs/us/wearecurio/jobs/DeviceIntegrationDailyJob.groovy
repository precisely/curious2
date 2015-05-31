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
		if (Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST) {
			log.debug "Aborted executing Daily basis job.."
			return // don't send reminders in test or development mode
		}
		oauthAccountService.refreshAllToken()
		withingsDataService.refreshSubscriptions()
		log.debug "Finished executing Daily basis job.."
	}

}

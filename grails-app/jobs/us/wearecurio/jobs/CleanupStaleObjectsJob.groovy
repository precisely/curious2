package us.wearecurio.jobs

import us.wearecurio.model.PasswordRecovery
import us.wearecurio.server.Session

class CleanupStaleObjectsJob extends us.wearecurio.utility.TimerJob {
	static triggers = {
		cron name:'cleanupTrigger', startDelay: DAY, cronExpression: '0 15 2 * * ? *' //2:15 AM
	}
	
	def grailsApplication

	def execute() {
		if (!grailsApplication.config.wearecurious.runImportJobs) {
			log.debug "Not job server, aborting..."
			return
		}
		
		PasswordRecovery.deleteStale()
		Session.deleteStale()
	}
}

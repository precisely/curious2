package us.wearecurio.jobs

import us.wearecurio.services.AnalyticsService
import org.apache.commons.logging.LogFactory

class AnalyticsJob extends us.wearecurio.utility.TimerJob {
		AnalyticsService analyticsService
		private static def log = LogFactory.getLog(this)

		static triggers = {
			//cron name:'analyticsTrigger', startDelay: 2 * HOUR, cronExpression: '0 45 2 * * ? *' //2:45 AM
			cron name:'analyticsTrigger', startDelay: 1 * MINUTE, cronExpression: '* * * * * ? *' //2:45 AM
		}

		def execute() {
			analyticsService.processUsers()
			log "analyticsJob launched."
		}
}

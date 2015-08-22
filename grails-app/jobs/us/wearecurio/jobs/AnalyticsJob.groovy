package us.wearecurio.jobs

import us.wearecurio.services.AnalyticsService
import org.apache.commons.logging.LogFactory
import grails.util.Environment

class AnalyticsJob extends us.wearecurio.utility.TimerJob {
		AnalyticsService analyticsService
		private static def log = LogFactory.getLog(this)

		static triggers = {
			cron name:'analyticsTrigger', startDelay: 1 * MINUTE, cronExpression: '0 30 2 * * ? *' // 2:30 a.m.
		}

		def execute() {
			if (Environment.current != Environment.DEVELOPMENT) {
				analyticsService.processUsers()
				log "analyticsJob launched."
			}
		}
}

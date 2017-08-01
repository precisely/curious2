package us.wearecurio.jobs

import us.wearecurio.services.AnalyticsService
import org.apache.commons.logging.LogFactory
import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsApplication

class AnalyticsJob extends us.wearecurio.utility.TimerJob {
	AnalyticsService analyticsService
	GrailsApplication grailsApplication
	
	private static def log = LogFactory.getLog(this)
	
	static triggers = {
		cron name:'analyticsTrigger', startDelay: 5 * MINUTE, cronExpression: '0 30 2 * * ? *' // 2:30 a.m.
	}
	
	def execute() {
		if (!grailsApplication.config.wearecurious.runImportJobs) {
			log.debug "Not job server, aborting..."
			return
		}
		if (Environment.current != Environment.DEVELOPMENT) {
			log.debug "analyticsJob launched."
//			analyticsService.processUsers()
		}
	}
}

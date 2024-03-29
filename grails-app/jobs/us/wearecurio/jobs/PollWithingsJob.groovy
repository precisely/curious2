package us.wearecurio.jobs

import us.wearecurio.services.*

import org.apache.commons.logging.LogFactory

import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsApplication

class PollWithingsJob extends us.wearecurio.utility.TimerJob {

	private static def log = LogFactory.getLog(this)

	static triggers = {
		cron name:'pollWithingsCron', startDelay: 2 * HOUR, cronExpression: '0 0 3 * * ? *' //3 AM 
	}

	WithingsDataService withingsDataService
	GrailsApplication grailsApplication
	
	def execute() {
		def timestamp = System.currentTimeMillis()
		log.debug "PollWithingsJob: Started at ${timestamp}"
		
		if (!grailsApplication.config.wearecurious.runImportJobs) {
			log.debug "Not job server, aborting..."
			return
		}
		
		if (Environment.current != Environment.PRODUCTION) {
			log.debug "Aborted DeviceIntegrationHourlyJob.."
			return // don't send reminders in test or development mode
		}
		
		withingsDataService.pollAll()
		log.debug "PollWithingsJob: Job started at ${timestamp} ended"
	}
}

package us.wearecurio.jobs

import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.services.OauthAccountService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.services.OuraDataService
import us.wearecurio.services.DataService
import us.wearecurio.utility.TimerJob
import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsApplication

class DeviceIntegrationDailyJob extends TimerJob {
	static transactional = false

	OauthAccountService oauthAccountService
	WithingsDataService withingsDataService
	OuraDataService ouraDataService
	JawboneUpDataService jawboneUpDataService
	GrailsApplication grailsApplication

	static triggers = {
		cron name:'deviceIntegrationTrigger', startDelay: HOUR, cronExpression: '0 30 2,12 * * ? *' //2:30 AM, 12:30 PM
	}

	def execute() {
		log.debug "Started executing Daily job.."
		if (!grailsApplication.config.wearecurious.runImportJobs) {
			log.debug "Not job server, aborting..."
			return
		}
			
		if (Environment.current != Environment.PRODUCTION) {
			log.debug "Aborted executing Daily basis job.."
			return
		}
		oauthAccountService.refreshAll()
		DataService.pollAllDataServices()
		
		ouraDataService.checkSyncHealth()
		log.debug "Finished executing Daily basis job.."
	}

}

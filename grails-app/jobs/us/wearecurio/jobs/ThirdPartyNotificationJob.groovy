package us.wearecurio.jobs

import grails.util.Environment
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.services.LegacyOuraDataService

class ThirdPartyNotificationJob extends us.wearecurio.utility.TimerJob {
	static transactional = false

	static triggers = {
		simple startDelay: 10 * MINUTE, repeatInterval: 5 * MINUTE
	}

	def group = "curious"
	def concurrent = false

	def fitBitDataService
	def withingsDataService
	LegacyOuraDataService legacyOuraDataService
	JawboneUpDataService jawboneUpDataService
	def grailsApplication

	def execute() {
		log.debug "Started executing ThirdPartyNotificationJob.."
		if (!grailsApplication.config.wearecurious.runImportJobs) {
			log.debug "Not job server, aborting..."
			return
		}
		if (Environment.current != Environment.PRODUCTION) {
			log.debug "Aborted ThirdPartyNotificationJob.."
			return // don't send reminders in test or development mode
		}
		fitBitDataService.notificationProcessor()
		withingsDataService.notificationProcessor()
		legacyOuraDataService.notificationProcessor()
		jawboneUpDataService.notificationProcessor()
		log.debug "Finished executing ThirdPartyNotificationJob."
	}
}

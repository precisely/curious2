package us.wearecurio.jobs

import grails.util.Environment
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.services.OuraDataService

class ThirdPartyNotificationJob extends us.wearecurio.utility.TimerJob {
	static transactional = false

	static triggers = {
		simple startDelay: 10 * MINUTE, repeatInterval: 5 * MINUTE
	}

	def group = "precise.ly"
	def concurrent = false

	def fitBitDataService
	def withingsDataService
	OuraDataService ouraDataService
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
		ouraDataService.notificationProcessor()
		jawboneUpDataService.notificationProcessor()
		log.debug "Finished executing ThirdPartyNotificationJob."
	}
}

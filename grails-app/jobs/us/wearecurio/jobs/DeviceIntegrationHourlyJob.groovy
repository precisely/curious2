package us.wearecurio.jobs

import org.codehaus.groovy.grails.commons.GrailsApplication
import grails.util.Environment
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.services.OuraDataService
import us.wearecurio.utility.TimerJob

class DeviceIntegrationHourlyJob extends TimerJob {
	static transactional = false

	def movesDataService
	def withingsDataService
	OuraDataService ouraDataService
	def concurrent = false
	GrailsApplication grailsApplication
	
	static triggers = {
		simple startDelay: 30 * MINUTE, repeatInterval: HOUR
	}

	def execute() {
		//humanDataService.poll()
		log.debug("Started DeviceIntegrationHourlyJob...")
		
		if (!grailsApplication.config.wearecurious.runImportJobs) {
			log.debug "Not job server, aborting..."
			return
		}
		
		if (Environment.current != Environment.PRODUCTION) {
			log.debug "Aborted DeviceIntegrationHourlyJob.."
			return // don't send reminders in test or development mode
		}

		movesDataService.pollAll()
		ouraDataService.pollAll()

		List<Integer> results = OAuthAccount.createCriteria().list {
			projections {
				groupProperty('timeZoneId')
			}
		}

		println results

		results.each { timeZoneId ->
			if (timeZoneId) {
				DateTimeZone timezone = TimeZoneId.fromId(timeZoneId).toDateTimeZone()
				DateTime now = new DateTime().withZone(timezone)
				LocalDateTime localTime = now.toLocalDateTime()

				if (localTime.getHourOfDay() % 3 == 0) { // change to polling every 3 hours
					List<OAuthAccount> withingsAccounts = OAuthAccount.findAllByTimeZoneIdAndTypeId(timeZoneId, ThirdParty.WITHINGS)
					withingsAccounts.each { account ->
						log.debug "DeviceIntegrationHourlyJob.execute() calling getDataForWithings " + account
						withingsDataService.poll(account)
					}
				}
			}
		}

		log.debug("Finished DeviceIntegrationHourlyJob...")
	}

}

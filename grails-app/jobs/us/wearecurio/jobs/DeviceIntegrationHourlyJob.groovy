package us.wearecurio.jobs

import grails.util.Environment

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.utility.TimerJob
import org.codehaus.groovy.grails.commons.GrailsApplication

class DeviceIntegrationHourlyJob extends TimerJob {
	static transactional = false

	def movesDataService
	def withingsDataService
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
				log.debug "DeviceIntegrationHourlyJob.execute() Local Hour of the day: " + localTime.getHourOfDay()

				if (localTime.getHourOfDay() == 0) {
					OAuthAccount.findAllByTimeZoneIdAndTypeId(timeZoneId, ThirdParty.WITHINGS).each { account ->
						log.debug "DeviceIntegrationHourlyJob.execute() calling getDataForWithings " + account
						try {
							withingsDataService.getDataDefault(account, account.fetchLastDataDate(), null, false)
						} catch (InvalidAccessTokenException e) {
							log.warn "Token expired while polling account: [$account] for Withings."
						}

					}
				}
			}
		}

		log.debug("Finished DeviceIntegrationHourlyJob...")
	}

}

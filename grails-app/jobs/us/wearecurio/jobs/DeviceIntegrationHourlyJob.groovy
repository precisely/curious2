package us.wearecurio.jobs

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import us.wearecurio.model.OAuthAccount;
import us.wearecurio.model.ThirdParty;
import us.wearecurio.model.TimeZoneId;
import us.wearecurio.model.User;
import us.wearecurio.thirdparty.InvalidAccessTokenException;
import us.wearecurio.utility.TimerJob

class DeviceIntegrationHourlyJob extends TimerJob {
	static transactional = false

	def humanDataService
	def movesDataService
	def withingsDataService

	static triggers = {
		simple startDelay: 30 * MINUTE, repeatInterval: HOUR
	}

	def execute() {
		//humanDataService.poll()
		movesDataService.pollAll()
		def c = OAuthAccount.createCriteria()
		def results = c.list {
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
							withingsDataService.getDataDefault(account, null, false)
						} catch (InvalidAccessTokenException e) {
							log.warn "Token expired while polling account: [$account] for Withings."
						}
						
					}
				}
			}
		}
		
	}

}

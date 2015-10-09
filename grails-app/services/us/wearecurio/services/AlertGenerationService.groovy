package us.wearecurio.services

import org.apache.commons.logging.LogFactory
import org.springframework.transaction.annotation.Transactional

import grails.util.Environment
import us.wearecurio.server.DateRecord
import us.wearecurio.utility.Utils
import us.wearecurio.model.*
import us.wearecurio.data.RepeatType

import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.*

class AlertGenerationService {
	static transactional = true

	private static def log = LogFactory.getLog(this)

	static AlertGenerationService service
	
	static def set(AlertGenerationService s) { service = s }

	static AlertGenerationService get() { return service }
	
	def generateAlerts(Date now) {
		log.debug "generateAlerts()"
		
		DateRecord rec = DateRecord.lookup(DateRecord.ALERT_GENERATION)
		
		Date startDate = rec.getDate()
		
		Date endDate = now + 1
		
		if (!startDate)
			startDate = new Date(now.getTime() - 2L*60L*60000L)
			
		def alertUsers = User.executeQuery("SELECT u.id FROM User u")
		log.debug "Users to generate alerts for " + alertUsers.size()

		for (def userId in alertUsers) {
			AlertNotification.generate(userId, startDate, endDate)
		}
		
		rec.setDate(endDate)
		
		Utils.save(rec, true)
	}
	
	def regenerateAlerts(Long userId, Date now) {
		AlertNotification.deleteforUser(userId)
		Date startDate = new Date(now.getTime() - 1*60000L)
		Date endDate = now + 1
		AlertNotification.generate(userId, startDate, endDate)
	}
}

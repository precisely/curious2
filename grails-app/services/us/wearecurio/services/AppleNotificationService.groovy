package us.wearecurio.services
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.annotation.Transactional
import grails.util.*
import com.notnoop.apns.*
import us.wearecurio.utility.Utils

import grails.util.Environment

class AppleNotificationService {
	
	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}

	def grailsApplication
	
    def sendMessage(def messageTxt, def devices = [], def collapseKey = 'Curious',def customPayload = [:]) {
		if (Environment.current == Environment.DEVELOPMENT) {
			return // don't send notifications in development mode
		}
		
		if (devices.size() == 0) {
			debug ("Need at least one device token to send")
			return false
		}
		
		def apnsConfig = [:]
		
		apnsConfig.pathToCertificate = Holders.getFlatConfig()['pushNotification.apns.pathToCertificate']
		apnsConfig.environment = Holders.getFlatConfig()['pushNotification.apns.environment']
		apnsConfig.password = Holders.getFlatConfig()['pushNotification.apns.password']
		debug "APNS Config " + apnsConfig.dump()
		try {
			ApnsService service 
			
			FileInputStream certStream = new FileInputStream(apnsConfig.pathToCertificate)
		
			if (apnsConfig.environment?.equals("sandbox")) {
				service = APNS.newService()
					.withCert(certStream, apnsConfig.password)
					.withSandboxDestination()
					.build()
			} else {
				service = APNS.newService()
					.withCert(certStream, apnsConfig.password)
					.withProductionDestination()
					.build()
			}
			
			String payload = APNS.newPayload()
				.alertBody(messageTxt)
				.customFields(customPayload)
				.build();
					
			devices.each { token ->
				service.push(token, payload);
				debug("Sent push notification message '" + messageTxt + "' to " + token)
			}
		} catch (Exception e) {
			Utils.reportError("Exception while trying to send APNS message", e)
			return false
		}
		return true
    }
}

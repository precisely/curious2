package us.wearecurio.services
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.annotation.Transactional
import grails.util.*
import com.notnoop.apns.*

class AppleNotificationService {
	
	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}

	def grailsApplication
	
    def sendMessage(def messageTxt, def devices = [], def collapseKey = 'Curious',def customPayload = [:]) {
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
			}
		} catch(Exception e) {
			debug("Exception occured while trying to send APNS message")
			e.printStackTrace()
			return false
		}
		return true
    }
}

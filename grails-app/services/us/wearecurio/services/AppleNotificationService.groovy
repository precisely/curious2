package us.wearecurio.services
import org.apache.commons.logging.LogFactory;

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
		
		def apnsConfig = grailsApplication.config.pushNotification.apns
		debug "APNS Certificate" + apnsConfig?.pathToCertificate
		debug "APNS Environment" + apnsConfig?.environment
		try {
			ApnsService service 
			
			FileInputStream certStream = new FileInputStream(grailsApplication.getMainContext().getResource(apnsConfig.pathToCertificate).getFile())
		
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

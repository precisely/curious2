package us.wearecurio.services

import com.google.android.gcm.server.Message
import com.google.android.gcm.server.MulticastResult
import com.google.android.gcm.server.Sender
import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

class GoogleMessageService {
	private static def log = LogFactory.getLog(this)

	static debug(str) {
		log.debug(str)
	}

	def grailsApplication

	def sendMessage(def messageTxt, def devices = [], def collapseKey = 'Curious', Map customPayLoad = [:]) {
		def gcmConfig = grailsApplication.config.pushNotification.gcm
		if (devices.size() == 0) {
			debug ("Need at least one device token to send")
			return false
		}
		// Instance of com.android.gcm.server.Sender, that does the
		// transmission of a Message to the Google Cloud Messaging service.
		Sender sender = new Sender(gcmConfig.senderID)

		customPayLoad.message = messageTxt
		customPayLoad.title = "We Are Curious"
		customPayLoad.image = 'www/content/images/notification-icon.png'
		customPayLoad["content-available"] = "1"

		// This Message object will hold the data that is being transmitted
		// to the Android client devices.  For this demo, it is a simple text
		// string, but could certainly be a JSON object.
		Message message = new Message.Builder()

		// If multiple messages are sent using the same .collapseKey()
		// the android target device, if it was offline during earlier message
		// transmissions, will only receive the latest message for that key when
		// it goes back on-line.
				.collapseKey(collapseKey)
				.timeToLive(30)
				.delayWhileIdle(true)
				.setData(customPayLoad)
				.build()

		try {
			// use this for multicast messages.  The second parameter
			// of sender.send() will need to be an array of register ids.
			MulticastResult result = sender.send(message, devices, 1)
			 
			if (result.getResults() != null) {
				int canonicalRegId = result.getCanonicalIds()
				if (canonicalRegId != 0) {
					 
				}
				return true
			} else {
				int error = result.getFailure()
				System.out.println("Broadcast failure: " + error)
			}

		} catch (Exception e) {
			debug("Exception occured while trying to send GCM message")
			Utils.reportError("Exception while sending GoogleMessageService", e)
			return false
		}
	}
}

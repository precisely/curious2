package us.wearecurio.services

import us.wearecurio.server.BackgroundTask
import org.apache.commons.logging.LogFactory
import grails.plugin.mail.MailService
import grails.util.Environment

class EmailService {

	private static def log = LogFactory.getLog(this)

	static transactional = false

	static def service

	static def set(s) { service = s }

	static EmailService get() { return service }

	MailService mailService

	void sendEmail(Closure callable) {
		if (Environment.current != Environment.PRODUCTION) {
			return
		}

		BackgroundTask.launch {
			try {
				log.debug "Calling mail service sendEmail"
				mailService.sendMail(callable)
			} catch (Exception e) {
				log.error "Error while sending email to: "
				e.printStackTrace()
			}
		}
	}

	void send(String toString, String subjectString, String bodyString) {
		log.debug "Sending email: " + toString + ", " + subjectString

		sendEmail {
			from "curious@wearecurio.us"
			to toString
			subject subjectString
			body bodyString
		}
	}

	void send(String fromString, String toString, String subjectString, String bodyString) {
		log.debug "Sending email: " + fromString + ", " + toString + ", " + subjectString

		sendEmail {
			from fromString
			to toString
			subject subjectString
			body bodyString
		}
	}

	def getMailConfig() {
		return mailService.getMailConfig()
	}

	boolean isDisabled() {
		return mailService.isDisabled()
	}
}

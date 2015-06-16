package us.wearecurio.services

import org.springframework.mail.MailMessage

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
	
    MailMessage sendMail(Closure callable) {
		return mailService.sendMail(callable)
    }

    MailMessage send(String toString, String subjectString, String bodyString) {
		sendMail {
			from "curious@wearecurio.us"
			to toString
			subject subjectString
			body bodyString
		}
    }

    MailMessage send(String fromString, String toString, String subjectString, String bodyString) {
		sendMail {
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

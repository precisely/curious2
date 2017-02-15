package us.wearecurio.controller

import grails.util.Environment
import grails.util.Holders
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

/**
 * A dummy controller to provide some utility methods for development purpose. This file will be removed from
 * production WAR files. See "BuildConfig.groovy"
 */
class DummyController {

	def beforeInterceptor = [action: this.&onlyDevelopment]

	// This is just an edge condition. This controller will not be available in any WAR files.
	private onlyDevelopment() {
		if (!Environment.isDevelopmentMode()) {
			throw new Exception("Trying to run an application in non-development environment.")
		}
	}

	/**
	 * Useful for development environment to test any service method. Directly use the service name (without
	 * injecting the service using "def xyzService") and this method will autoinject that service using Groovy's
	 * propertyMissing logic to do the same.
	 */
	def propertyMissing(String name) {
		if (name.endsWith("Service")) {
			return grailsApplication.mainContext[name]
		}
		if (name == "currentUser") {
			return securityService.getCurrentUser()
		}
	}

	def index() {
		log.debug "send apple notification"
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZ").withZone(DateTimeZone.UTC)
		def appleNotificationService = Holders.grailsApplication.mainContext.getBean 'appleNotificationService'

		appleNotificationService.sendMessage("Test notification message",
				['54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd', '7582136e4079d0b8521d3c2215c244049b0392290dd4d64cee4e25fa3e2b74fe'], "Curious",
				[entryId: 593838, entryDate: dateTimeFormatter.print(new Date().getTime())])
	}

	def runMigrations() {
		migrationService.doMigrations()
	}

	def test() {
	}
}

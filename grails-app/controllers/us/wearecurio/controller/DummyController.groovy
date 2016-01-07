package us.wearecurio.controller

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import us.wearecurio.model.*
import us.wearecurio.model.Entry
import grails.util.*

class DummyController {

	def ouraDataService

	def index() {
		log.debug "send apple notification"
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZ").withZone(DateTimeZone.UTC)
		def appleNotificationService = Holders.grailsApplication.mainContext.getBean 'appleNotificationService'
		appleNotificationService.sendMessage("Test notification message",
				['54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd', '7582136e4079d0b8521d3c2215c244049b0392290dd4d64cee4e25fa3e2b74fe'],"Curious",
				['entryId': 593838,'entryDate':dateTimeFormatter.print(new Date().getTime())])
	}

	def handleNotify() {
		log.debug "handle oura notification"
		ouraDataService.notificationProcessor()
	}
}

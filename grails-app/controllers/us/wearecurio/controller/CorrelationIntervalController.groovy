package us.wearecurio.controller

import us.wearecurio.model.AnalyticsCorrelationInterval
import grails.converters.JSON

class CorrelationIntervalController {

	static allowedMethods = [index: "POST"]
	def securityService

	def index() {
		def currentUser = securityService.currentUser
		if (currentUser == null) {
			renderNotLoggedIn()
			return
		}
		def correlationIntervals = AnalyticsCorrelationInterval.list(params.id.toLong())
		response.status = correlationIntervals ? 200 : 500
		render correlationIntervals.collect { it.asJson() } as JSON
	}

	private isOwner(id) {
		def obj = AnalyticsCorrelation.get(id)
		def currentUser = securityService.currentUser
		currentUser && obj && (obj.userId == currentUser.id)
	}

	private renderNotLoggedIn() {
		response.status = 500
		render(contentType: "text/json") {['message': 'not logged in']}
	}

	private renderOk(errorFree) {
		if (errorFree) {
			response.status = 200
			render(contentType: "text/json") {['message': "ok" ]}
		} else {
			response.status = 500
			render(contentType: "text/json") {['message': 'an error occurred']}
		}
	}

}

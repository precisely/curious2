package us.wearecurio.controller

import us.wearecurio.model.AnalyticsCorrelation
import grails.converters.JSON

class CorrelationController {

	static allowedMethods = [markViewed: "PATCH", markSaved: "PATCH", updateSignalLevel: "PATCH", index: "GET"]
	def securityService

	def index() {
		def currentUser = securityService.currentUser
		if (currentUser == null) {
			renderNotLoggedIn()
			return
		}
		def correlations = AnalyticsCorrelation.userCorrelations(currentUser.id, 20, params.flavor)
		response.status = correlations ? 200 : 500
		render correlations as JSON
	}

	def updateSignalLevel() {
		def id = params.id.toLong()
		def signalLevel = params.signalLevel.toDouble()
		println "updateSignalLevel ${id} ${signalLevel}"
		renderOk(isOwner(id) && AnalyticsCorrelation.updateSignalLevel(id, signalLevel))
	}

	def markViewed() {
		def id = params.id.toLong()
		renderOk(isOwner(id) && AnalyticsCorrelation.markViewed(id))
	}

	def markNoise() {
		def id = params.id.toLong()
		renderOk(isOwner(id) && AnalyticsCorrelation.markNoise(id))
	}

	def markSaved() {
		def id = params.id.toLong()
		renderOk(isOwner(id) && AnalyticsCorrelation.markSaved(id))
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

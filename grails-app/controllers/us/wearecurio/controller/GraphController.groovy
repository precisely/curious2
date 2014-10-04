package us.wearecurio.controller

import us.wearecurio.model.Entry
import grails.converters.JSON

class GraphController {
	def securityService

	def index() {
		def currentUser = securityService.currentUser
		if (currentUser == null) {
			renderNotLoggedIn()
			return
		}
		def correlations = Correlation.userCorrelations(currentUser.id, 20, params.flavor)
		response.status = correlations ? 200 : 500
		render correlations as JSON
	}

	private isOwner(obj) {
		def currentUser = securityService.currentUser
		currentUser && obj && obj.userId == currentUser.id
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

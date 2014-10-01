package us.wearecurio.controller

import us.wearecurio.model.Correlation

class CorrelationController {
	static allowedMethods = [viewed: "PATCH"]
	def securityService

	def index() {
		def currentUser = securityService.currentUser
		if (currentUser != null) {
			renderNotLoggedIn()
			return
		}
		response.status = topCorrelations ? 200 : 500
		render(contentType: "text/json") {
			Correlation.userCorrelations(currentUser.id, 20, params.flavor)
		}
	}

	def markViewed() {
		def id = params.id.toLong()
		renderOk isOwner(id) && Correlation.markViewed(id)
	}

	def markNoise() {
		def id = params.id.toLong()
		renderOk isOwner(id) && Correlation.markNoise(id)
	}

	def markSaved() {
		def id = params.id.toLong()
		renderOk isOwner(id) && Correlation.markSaved(id)
	}

	private isOwner(id) {
		def obj = Correlation.get(id)
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

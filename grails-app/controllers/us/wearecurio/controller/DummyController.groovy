package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.Discussion
import us.wearecurio.model.UserGroup

class DummyController extends DataController {

	//static responseFormats = ['xml', 'json']
	def index() { }
	
	def getMyThreads(Long discussionId) {
		debug "DummyController.getMyThreads()"
		redirect (uri: "home/feed", params: discussionId)
	}

	def getSearchResults() {
		log.debug "params: ${params}"
		redirect (action: "getMyThreads") 
	}

}

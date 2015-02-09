package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.Discussion
import us.wearecurio.model.UserGroup

class FeedController extends DataController {

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

	def getRecentSearches() {
		List searchKeywords = ['Good sleep', 'Fast Sprint', 'Alcohol', 'Snapshot', 'Dream']
		if (request.xhr) {
			log.debug "sending requested search items"
			renderJSONGet([terms: searchKeywords])
			return
		}
		render searchKeywords
	}

	def createSprint(String details, String tags, String participants) {
		log.debug "parameters recieved to save sprint: ${params}"
		log.debug "Details: ${details}"
		log.debug "Tags: ${tags}"
		log.debug "Participants: ${participants}"
		return
	}

	def getSprintsForUser() {
		List sprintsList = ['Eat Halthier', 'Runners challange', 'Sprint3', 'Sprint4', 'Sprint5']
		// If is a ajax call
		if (request.xhr) {
			log.debug "sending requested search items"
			renderJSONGet([terms: sprintsList])
			return
		}
		render sprintsList
	}

	/**
	 * 
	 * @param id Id of Sprint
	 * @param userId Id of {@link us.wearecurio.model.User User}
	 * @return
	 */
	def sprint(Long id, Long userId) {
		Map sprint = [
			id: 1,
			title: "Demo Sprint",
			description: "At the professional level, sprinters begin the race by assuming a crouching position" + 
			"in the starting blocks before leaning forward and gradually moving into an upright position as" + 
			"the race progresses and momentum is gained. The set position differs depending on the start." + 
			"Body alignment is of key importance in producing the optimal amount of force. Ideally the athlete should begin in a" + 
			"4-point stance and push off using both legs for maximum force production",
			tags: ["Tag1","Tag2", "Tag3"],
			participants: [[id: 1, name: "Johan"], [id: 2, name: "Ron"], [id: 3, name: "Rambo"], [id: 4, name: "Don"]],
			totalParticipants: 5,
			userId: 1
		]
		render(view: "/home/sprint", model: sprint)
	}
}

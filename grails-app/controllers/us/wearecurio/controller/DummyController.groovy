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

	def createNewBlankSprint() {
		renderJSONGet([id: 1, name: "New Sprint", userId: 1, groupId: 1, success: true])
		return
	}

	def createSprint(String details, boolean error) {
		log.debug "parameters recieved to save sprint: ${params}"
		log.debug "Details: ${details}"
		log.debug "error: ${error}"
		
		if (error) {
			renderJSONPost([error: true])
			return
		} else {
			renderJSONPost([success: true, id: 1])
			return
		}
	}

	/**
	 * 
	 * @param id Id of Sprint
	 * @param userId Id of {@link us.wearecurio.model.User User}
	 * @return
	 * We need to send tagList so that it is available to create sprint modal.
	 * 
	 */
	def sprint(Long id, Long userId) {
		
		// This is dummy sprint and recent searches data.
		List sprintList = [[title: 'Eat Halthier', id: 1], [title: 'Runners challange', id: 2], [title: 'Sprint3', id: 3]]
		List searchKeywords = [[userId: 1, searchString: 'Good sleep'], [userId: 1, searchString: 'Fast Sprint'], 
			[userId: 1, searchString: 'Alcohol'], [userId: 1, searchString: 'Snapshot']]
		Map sprint = [
			id: 1,
			title: "Demo Sprint",
			description: "At the professional level, sprinters begin the race by assuming a crouching position" + 
			"in the starting blocks before leaning forward and gradually moving into an upright position as" + 
			"the race progresses and momentum is gained. The set position differs depending on the start." + 
			"Body alignment is of key importance in producing the optimal amount of force. Ideally the athlete should begin in a" + 
			"4-point stance and push off using both legs for maximum force production",
			tags: [[id: 1, description: "Tag1"], [id: 2, description: "Tag2"], [id: 3, description: "Tag3"]],
			tagList: [[id:1, description: "run miles", repeatType: "pinned"], [id:2, description: "max heart rate", repeatType: "remind"]
				, [id:3, description: "Repeat Me", repeatType: "repeat"], [id:4, description: "track coffee", repeatType: "pinned"]
				, [id:5, description: "Take pills", repeatType: "remind"]],
			participants: [[id: 1, name: "Johan"], [id: 2, name: "Ron"], [id: 3, name: "Rambo"], [id: 4, name: "Don"]],
			totalParticipants: 5,
			userId: 1,
			sprintList: sprintList,
			searchKeywords: searchKeywords
		]
		render(view: "/home/sprint", model: sprint)
	}

	def addSprintParticipants(String username, long virtualGroupId, boolean error) {
		log.debug "DummyController.addParticipants(): parameters recieved- $params"
		if (error) {
			renderJSONPost([error: true, errorMessage: "Participant Already Invited!"])
			return
		} else {
			renderJSONPost([success: true])
			return
		}
	}

	def addSprintAdmins(String username, long virtualGroupId, boolean error) {
		log.debug "DummyController.addAdmins(): parameters recieved- $params"
		if (error) {
			renderJSONPost([error: true, errorMessage: "Admin Already Invited!"])
			return
		} else {
			renderJSONPost([success: true])
			return
		}
	}

	def deleteSprintParticipant(String username, long virtualGroupId, boolean error) {
		log.debug "DummyController.deleteSprintParticipant(): parameters recieved- $params"
		if (error) {
			renderJSONPost([error: true, errorMessage: "No such participant!"])
			return
		} else {
			renderJSONPost([success: true])
			return
		}
	}

	def deleteSprintAdmin(String username, long virtualGroupId, boolean error) {
		log.debug "DummyController.deleteSprintAdmin(): parameters recieved- $params"
		if (error) {
			renderJSONPost([error: true, errorMessage: "No such admin!"])
			return
		} else {
			renderJSONPost([success: true])
			return
		}
	}
}

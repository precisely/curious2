package us.wearecurio.controller
import us.wearecurio.model.User
import us.wearecurio.services.SearchService

class SearchController extends LoginController {

	def searchService

	static debug(str) {
		log.debug(str)
	}

	SearchController() {
		debug "SearchController constructor()"
	}

	User getCurrentUser() {
		User user = sessionUser()
		
		if (!user) {
			debug "sessionUser returns null"
			renderJSONGet([success: false, message: g.message(code: "auth.error.message")])
			return null
		}

		return user
	}
	
	def getSocialNotifications(int offset, int max) {
		log.debug "SearchController.getSocialNotifications $params"
		User user = sessionUser()
		params.max = Math.min(max ?: 10, 100)
		params.offset = offset ?: 0
		params.type = params.type ?: SearchService.DISCUSSION_TYPE
		Map result = searchService.getNotifications(user, params.type, new Date(), params.offset, params.max)
		if (result.success) {
			searchService.resetNotificationsCheckDate(user, params.type)
		}
		renderJSONGet(result)
	}

	def getTotalNotificationsCount() {
		User user = sessionUser()
		if (!user) {
			renderJSONGet([success: false])
		} else {
			renderJSONGet([success: true, totalNotificationCount: searchService.getNewNotificationCount(user)])
		}
	}

	/**
	 * AJAX endpoint which is called for the "ALL" sub-tab in the "SOCIAL" view
	 */
	def getAllSocialData(int offset, int max, int nextSuggestionOffset) {
		log.debug "SearchController.getAllSocialData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.nextSuggestionOffset = nextSuggestionOffset ?: 0
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0

		renderJSONGet(
			searchService.getFeed(
				SearchService.DISCUSSION_TYPE | SearchService.USER_TYPE,
				user,
				params.offset,
				params.max,
				nextSuggestionOffset//,
				//session.id
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "DISCUSSIONS" sub-tab in the "SOCIAL" view
	 */
	def getDiscussionSocialData(int offset, int max) {
		log.debug "SearchController.getDiscussionSocialData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.getActivity(
				SearchService.DISCUSSION_TYPE,
				user, 
				params.offset, 
				params.max
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "PEOPLE" sub-tab in the "SOCIAL" view
	 */
	def getPeopleSocialData(int offset, int max) {
		log.debug "SearchController.getPeopleSocialData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}

		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0

//		renderJSONGet(
//			searchService.getSuggestions(
//				SearchService.USER_TYPE,
//				user, 
//				params.offset, 
//				params.max,
//				session.id
//			)
//		)
		
		renderJSONGet(
			searchService.getSuggestions(
				SearchService.USER_TYPE,
				user,
				params.offset,
				params.max
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "OWNED" sub-tab in the "SOCIAL" view
	 */
	def getOwnedSocialData(int offset, int max) {
		log.debug "SearchController.getOwnedSocialData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.getOwned(
				SearchService.DISCUSSION_TYPE,
				user, 
				params.offset, 
				params.max
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "ALL" sub-tab in the "SPRINTS" view
	 */
	def getAllSprintData(int offset, int max, int nextSuggestionOffset) {
		log.debug "SearchController.getAllSprintData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}

		params.nextSuggestionOffset = nextSuggestionOffset ?: 0
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0

		renderJSONGet(
			searchService.getFeed(
				SearchService.SPRINT_TYPE,
				user,
				params.offset,
				params.max,
				nextSuggestionOffset//,
				//session.id
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "OWNED" sub-tab in the "SPRINTS" view
	 */
	def getOwnedSprintData(int offset, int max) {
		log.debug "SearchController.getOwnedSprintData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.getOwned(
				SearchService.SPRINT_TYPE,
				user,
				params.offset, 
				params.max
			)
		)
	}

	/**
	 * AJAX endpoint which is called for the "ALL" (default) sub-tab in the "SEARCH" view
	 */
	def searchAllData(int offset, int max, String q) {
		log.debug "SearchController.searchData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.search(
				user,
				q,
				params.offset,
				params.max
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "DISCUSSIONS" sub-tab in the "SEARCH" view
	 */
	def searchDiscussionData(int offset, int max, String q) {
		log.debug "SearchController.searchData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.search(
				user,
				q,
				params.offset,
				params.max,
				SearchService.DISCUSSION_TYPE
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "SPRINTS" sub-tab in the "SEARCH" view
	 */
	def searchSprintData(int offset, int max, String q) {
		log.debug "SearchController.searchData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.search(
				user,
				q,
				params.offset,
				params.max,
				SearchService.SPRINT_TYPE
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "PEOPLE" sub-tab in the "SEARCH" view
	 */
	def searchPeopleData(int offset, int max, String q) {
		log.debug "SearchController.searchData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.search(
				user,
				q,
				params.offset,
				params.max,
				SearchService.USER_TYPE
			)
		)
	}
	
	/**
	 * AJAX endpoint which is called for the "OWNED" sub-tab in the "SEARCH" view
	 */
	def searchAllOwnedData(int offset, int max, String q) {
		log.debug "SearchController.searchData $params"
		
		User user = currentUser
		if (user == null) {
			return
		}
		
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(
			searchService.searchOwned(
				user,
				q,
				params.offset,
				params.max
			)
		)
	}
	
	/**
	 * Ajax endpoint used for "STARTED" subtab in the Trackathon
	 */
	def getStartedSprintData(int offset, int max) {
		log.debug "SearchController.getStartedSprintData $params"
		User user = currentUser

		if (!user) {
			return
		}

		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		renderJSONGet(searchService.getStartedSprints(user.id, offset, max))
	}
	
	/**
	 * Action used only for rendering the search GSP
	 */
	def index() {
	}
}

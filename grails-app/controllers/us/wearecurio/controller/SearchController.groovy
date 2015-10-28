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
	 * Action used only for rendering the search GSP
	 */
	def index() {
	}

	/**
	 * AJAX endpoint which is called when the user searches anything from the search bar. This endpoint is also
	 * called when any of the tab is selected in the search page results.
	 */
	def searchData(Long type, int max, int offset, Boolean ownedFeed, String q) {
		log.debug "Search with $params"

		User user = sessionUser()
		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "auth.error.message")])
			return
		}

		if (!type) {
			renderJSONGet([success: false, message: g.message(code: "default.blank.message", args: ["Type"])])
			return
		}

		// If no query is entered or empty string is passed
		if (!q || !q.trim()) {
			renderJSONGet([success: false, message: g.message(code: "search.query.empty")])
			return
		}
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0
		
		if (ownedFeed) {
			// TODO Replace with actual method for search + owned feed. Temporarily using only the owned feed method
			renderJSONGet(searchService.getOwned(type, user, params.offset, params.max))
			return
		}

		renderJSONGet(searchService.search(user, q, params.offset, params.max, type))
	}

	/**
	 * AJAX endpoint for getting any feed data of any type.
	 */
	def feedData(Long type, int max, int offset, int nextSuggestionOffset, Boolean ownedFeed) {
		log.debug "Get feeds with $params"

		User user = sessionUser()
		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "auth.error.message")])
			return
		}

		if (!type) {
			renderJSONGet([success: false, message: g.message(code: "default.blank.message", args: ["Type"])])
			return
		}

		params.nextSuggestionOffset = nextSuggestionOffset ?: 0
		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0

		if (ownedFeed) {
			renderJSONGet(searchService.getOwned(type, user, params.offset, params.max))
			return
		}

		// TODO Implement this and pass a actual sessionId for current user
		def randomSessionId = "44";

		if (type == SearchService.USER_TYPE) {
			renderJSONGet(searchService.getSuggestions(type, user, params.offset, params.max, randomSessionId))
		} else {
			renderJSONGet(searchService.getFeed(type, user, params.offset, params.max, params.nextSuggestionOffset,
					randomSessionId))
		}
	}
}
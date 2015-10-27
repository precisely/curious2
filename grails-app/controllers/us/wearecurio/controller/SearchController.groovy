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
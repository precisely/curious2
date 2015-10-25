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

	def indexData(Long type, int max, int offset, String query, int nextSuggestionOffset) {
		User user = sessionUser()

		log.debug "Get feeds $params"
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

		if (query) {
			renderJSONGet(searchService.search(user, query, params.offset, params.max, type))
		} else if (type == SearchService.USER_TYPE) {
			renderJSONGet(searchService.getSuggestions(type, user, params.offset, params.max))
		} else {
			renderJSONGet(searchService.getFeed(type, user, params.offset, params.max, params.nextSuggestionOffset,
					params.randomSessionId))
		}
	}
}

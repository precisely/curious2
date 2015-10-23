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

	def indexData(Long type, int max, int offset) {
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

		params.max = Math.min(max ?: 5, 100)
		params.offset = offset ?: 0

		if (type != SearchService.ALL_TYPE) {
			renderJSONGet(searchService.getFeed(type, user, params.offset, params.max))
		} else {
			List listItems = []

			[SearchService.SPRINT_TYPE, SearchService.DISCUSSION_TYPE, SearchService.USER_TYPE].each { feedType ->
				Map result = searchService.getFeed(feedType, user, params.offset, params.max)
				if (result.listItems) {
					listItems.addAll(result.listItems)
				}
			}

			renderJSONGet([listItems: listItems, success: true])
		}
	}
}

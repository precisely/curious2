package us.wearecurio.controller

import us.wearecurio.model.User

class SearchController extends LoginController {

	def searchService

	static debug(str) {
		log.debug(str)
	}

	SearchController() {
		debug "SearchController constructor()"
	}

	def indexData(String type, int max, int offset) {
		User user = sessionUser()

		log.debug "params recieved: $params"
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

		if (type.equalsIgnoreCase("people")) {
			renderJSONGet(searchService.getPeopleList(user, params.offset, params.max))
		} else if (type.equalsIgnoreCase("discussions")) {
			renderJSONGet(searchService.getDiscussionsList(user, params.offset, params.max))
		} else if (type.equalsIgnoreCase("sprints")) {
			renderJSONGet(searchService.getSprintsList(user, params.offset, params.max))
		} else if (type.equalsIgnoreCase("all")) {
			List listItems = []

			Map sprints = searchService.getSprintsList(user, params.offset, params.max)
			if (sprints.listItems) {
				listItems.addAll(sprints.listItems)
			}

			Map discussions = searchService.getDiscussionsList(user, params.offset, params.max)
			if (discussions.listItems) {
				listItems.addAll(discussions.listItems)
			}

			Map peoples = searchService.getPeopleList(user, params.offset, params.max)
			if (peoples.listItems) {
				listItems.addAll(peoples.listItems)
			}

			if (!listItems) {
				renderJSONGet([success: false, listItems: false])
				return
			}
			renderJSONGet([listItems: listItems, success: true])
		} else {
			renderJSONGet([success: false, message: g.message(code: "default.blank.message", args: ["Type"])])
		}
	}
}

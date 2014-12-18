package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.Discussion
import us.wearecurio.model.UserGroup

class FeedController extends DataController {

	//static responseFormats = ['xml', 'json']
	def index() { }
	
	def getMyThreads(Long discussionId) {
		debug "DummyController.getMyThreads()"
		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			flash.message = "Must be logged in"
			redirect(url:toUrl(action:'index'))
			return
		}

		def groupMemberships = UserGroup.getGroupsForReader(user)
		List associatedGroups = UserGroup.getGroupsForWriter(user)
		def groupName
		def groupFullname = "Community Feed"

		groupMemberships.each { group ->
			if (group[0]?.name.equals(params.userGroupNames)) {
				groupFullname = group[0].fullName ?: group[0].name
				groupName = group[0].name
			}
		}

		params.max = params.max ?: 5
		params.offset = params.offset ?: 0

		List groupNameList = params.userGroupNames ? params.list("userGroupNames") : []
		debug "Trying to load list of discussions for " + user.getId() + " and list:" + groupMemberships.dump()

		Map discussionData = groupNameList ? UserGroup.getDiscussionsInfoForGroupNameList(user, groupNameList, params) :
				UserGroup.getDiscussionsInfoForUser(user, true, params)

		log.debug("HomeController.feed: User has read memberships for :" + groupMemberships.dump())

		Map model = [prefs: user.getPreferences(), userId: user.getId(), templateVer: urlService.template(request),
			groupMemberships: groupMemberships, associatedGroups: associatedGroups, groupName: groupName, groupFullname: groupFullname,
			discussionList: discussionData["dataList"], discussionPostData: discussionData["discussionPostData"], totalDiscussionCount: discussionData["totalCount"]]

		if (request.xhr) {
			if( !model.discussionList ){
				// render false if there are no more discussions to show.
				render false
			} else {
				render template: "/feed/discussions", model: model
			}
			return
		}

		model
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
}

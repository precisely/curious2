package us.wearecurio.services

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.UserGroup

class SearchService {
	
	static transactional = true
	
	static SearchService service
	
	static set(SearchService s) { service = s }
	
	static SearchService get() { service }

	// params has the structure:
	// [ key:[offset:<offset>, max:<max>], ... ]
	//
	// returns:
	// [ key:[<json description of item>, ... ], ... ]
	
	def list(Long userId, String searchString, Map params) {
		def retVal = [:]
		for (e in params) {
			String k = e.key
			Map v = e.value
			int offset = v['offset']
			int max = v['max']
			
			def results
			
			if (k.equals('discussions')) {
				retVal[k] = this.listDiscussions(searchString, offset, max)
			} else if (k.equals('users')) {
				retVal[k] = this.listUsers(searchString, offset, max)
			} else if (k.equals('sprints')) {
				retVal[k] = this.listSprints(searchString, offset, max)
			} else if (k.equals('groups')) {
				// not searching groups for now
				// retVal[k] = this.listGroups(searchString, offset, max)
			}
		}
	}
	
	def listDiscussions(User user, String searchString, int offset, int max) {
		
	}
	
	def listUsers(User user, String searchString, int offset, int max) {
		
	}
	
	def listSprints(User user, String searchString, int offset, int max) {
		
	}
	
	def listGroups(User user, String searchString, int offset, int max) {
		
	}

	Map getSprintsList(User user, int offset, int max) {
		List sprintList = Sprint.getSprintListForUser(user.id, max, offset)
		if (!sprintList) {
			return [listItems: false, success: true]
		}
		Map model = [sprintList: sprintList]
		return [listItems: model, success: true]
	}

	Map getDiscussionsList(User user, int offset, int max, List groupNameList = []) {
		def groupMemberships = UserGroup.getGroupsForReader(user)
		Map discussionData = groupNameList ? UserGroup.getDiscussionsInfoForGroupNameList(user, groupNameList, [offset: offset, max: max]) :
			UserGroup.getDiscussionsInfoForUser(user, true, false, [offset: offset, max: max])

		Map discussionPostData = discussionData["discussionPostData"]
		discussionData["dataList"].each {data ->
				if (data) {
					data.totalComments = discussionPostData[data.id].totalPosts
				}
			}

		Map model = [userId: user.getId(), groupMemberships: groupMemberships, totalDiscussionCount: discussionData["totalCount"], 
			discussionList: discussionData["dataList"], discussionPostData: discussionData["discussionPostData"]]

		if (!model.discussionList) {
			return [listItems: false, success: true]
		} else {
			return [listItems: model, success: true]
		}
	}

	Map getPeopleList(User user, int offset, int max) {
		List usersList = User.getUsersList(max, offset, user.id)
		if (usersList) {
			 return [listItems: usersList, success: true]
		}
		return [listItems: false, success: true]
	}
}

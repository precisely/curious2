package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionController extends LoginController {

	static allowedMethods = [create: "POST", update: "POST", delete: "POST"]


	private def loadGroup(def params, User user) {
		debug "DiscussionController.loadGroup()"
		def p = params

		UserGroup group = p.group ? UserGroup.lookup(p.group) : UserGroup.getDefaultGroupForUser(user)
		if (group && !group.hasWriter(user)) {
			return false
		}
		return group
	}

	private def loadDiscussion(def params) {
		def discussion
		if (params.id) {
			discussion = Discussion.get(params.id)
		} else if (params.plotDataId) {
			discussion = Discussion.getDiscussionForPlotDataId(params.plotDataId)
			debug "Discussion for plotDataId not found: " + params.plotDataId
		}
		return discussion
	}

	def create() {
		def user = sessionUser()
		def group = loadGroup(params, user)
		def p = params
		debug "DiscussionController.create to group: " + group?.dump()
		if (!group) {
			flash.message = "Failed to create new discussion topic: can't post to this group"
		} else {
			Discussion discussion = Discussion.create(user, p.name)
			if (discussion != null)
				Utils.save(discussion, true)
				flash.message = "Created new discussion: " + params.name
				if (group) group.addDiscussion(discussion)
			else {
				debug "DiscussionId not found: " + discussionId
				flash.message = "Failed to create new discussion topic: internal error"
			}
		}

		redirect(url:toUrl(controller:'home', action:'community'))
		return
	}

}

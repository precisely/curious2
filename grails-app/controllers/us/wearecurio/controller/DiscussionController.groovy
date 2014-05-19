package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionController extends LoginController {

	static allowedMethods = [create: "POST", update: "POST", delete: "POST"]


	private def loadGroup(def params, User user) {
		def p = params

		UserGroup group = p.group ? UserGroup.lookup(p.group) : UserGroup.getDefaultGroupForUser(user)
		debug "DiscussionController.loadGroup() " + group?.dump()
		if (group && !group.hasWriter(user)) {
			return false
		}
		return group
	}

	private def loadDiscussion(def p, def user) {
		def discussion
		if (p.id) {
			discussion = Discussion.get(p.id)
		} else if (p.plotDataId) {
			discussion = Discussion.getDiscussionForPlotDataId(p.plotDataId)
			debug "Discussion for plotDataId not found: " + p.plotDataId
		} 

		
		return discussion
	}

	private def createDiscussion(def p, def group, def user) {
		def discussion
		if (group.hasWriter(user)) {
			discussion = Discussion.create(user, p.name)
			group.addDiscussion(discussion)
		}
		return discussion
	}


	def create() {
		def p = params
		def user = sessionUser()
		def group = loadGroup(p, user)
		debug "DiscussionController.create to group: " + group?.dump()
		if (!group) {
			flash.message = "Failed to create new discussion topic: can't post to this group"
		} else {
			def discussion = loadDiscussion(p, user)
			discussion = discussion ?: createDiscussion(p, group, user)
			if (discussion != null) {
				Utils.save(discussion, true)
				flash.message = "Created new discussion: " + params.name
				discussion.createPost(user, p.discussionPost)
			} else {
				debug "DiscussionId not found: " + discussionId
				flash.message = "Failed to create new discussion topic: internal error"
			}
		}

		redirect(url:toUrl(controller:'home', action:'community'))
		return
	}

}

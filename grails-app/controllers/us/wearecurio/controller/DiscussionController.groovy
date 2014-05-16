package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionController extends LoginController {

	static allowedMethods = [create: "POST", update: "POST", delete: "POST"]


	private def loadGroup(def params) {
		debug "DiscussionController.loadGroup()"
		def user = sessionUser()
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

	def index() {
		redirect(action: "list", params: params)
	}

	def list(Integer max) {
		params.max = Math.min(max ?: 10, 100)
		[discussionInstanceList: Discussion.list(params), discussionInstanceTotal: Discussion.count()]
	}

	def create() {
		def user = sessionUser()
		def group = loadGroup(params)
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


	def show(Long id) {
		def discussionInstance = Discussion.get(id)
		if (!discussionInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'discussion.label', default: 'Discussion'), id])
			redirect(action: "list")
			return
		}

		[discussionInstance: discussionInstance]
	}

	def updateData(Long id) {
		def discussionInstance = Discussion.get(id)
		// TODO update a discussion
		if (p.id) {
			discussion = Discussion.get(p.id)
			discussion ?: (message = "That discussion topic no longer exists.")
		} else if (plotDataId) {
			discussion = Discussion.getDiscussionForPlotDataId(plotDataId)
			debug "Discussion for plotDataId not found: " + plotDataId
			discussion ?: (message = "That shared graph discussion no longer exists.")
		}
	}

	def deleteData(Long id) {
		def discussionInstance = Discussion.get(id)

		try {
			discussionInstance.delete(flush: true)
			renderJSONPost(['success', [message: message]])
			return
		}
		catch (DataIntegrityViolationException e) {
			renderJSONPost(['error', [message: message]])
			return
		}
	}
}

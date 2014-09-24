package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionController extends LoginController {

	static allowedMethods = [create: "POST", update: "POST", delete: "POST"]

	def create(Long plotDataId, String name, Long id, String discussionPost) {
		def user = sessionUser()
		UserGroup group = Discussion.loadGroup(params.group, user)

		if (!group) {
			flash.message = "Failed to create new discussion topic: can't post to this group"
		} else {
			Discussion discussion = Discussion.loadDiscussion(id, plotDataId, user)
			discussion = discussion ?: Discussion.create(user, name, group)

			if (discussion != null) {
				Utils.save(discussion, true)
				flash.message = "Created new discussion: " + name
				discussion.createPost(user, discussionPost)
			} else {
				flash.message = "Failed to create new discussion topic: internal error"
			}
		}

		redirect(url:toUrl(controller:'home', action:'community'))
		return
	}

}

package us.wearecurio.controller

import org.springframework.transaction.annotation.Transactional	 
import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionController extends LoginController {

	static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

	// Not being used right now as all discussion lists are comming form feed
	def index() {
	}

	// Deprecated method
	@Deprecated
	def createTopic() {
		redirect(url: toUrl(action: "save", params: params))
	}

	def save(Long plotDataId, String name, Long id, String discussionPost) {
		def user = sessionUser()
		UserGroup group = Discussion.loadGroup(params.group, user)

		if (!group) {
			flash.message = "Failed to create new discussion topic: can't post to this group"
			redirect(url: toUrl(controller: "home", action: "social"))
		} else {
			Discussion discussion = Discussion.loadDiscussion(id, plotDataId, user)
			discussion = discussion ?: Discussion.create(user, name, group)

			if (discussion != null) {
				Utils.save(discussion, true)

				if(discussionPost) {
					discussion.createPost(user, discussionPost)
				}

				redirect(url: toUrl(action: "show", params: ["id": discussion.hash]))
			} else {
				flash.message = "Failed to create new discussion topic: internal error"
				redirect(url: toUrl(controller: "home", action: "social"))
			}
		}

	}

	def show() {
		User user = sessionUser()

		Discussion discussion = Discussion.findByHash(params.id)
		if (!discussion){
			flash.message = g.message(code: "default.blank.message", args: ["Discussion"])
			redirect(url: toUrl(controller: "home", action: "index"))
			return
		} else if (!discussion.getIsPublic() && !user) {
			flash.message = g.message(code: "default.login.message")
			redirect(url: toUrl(controller: "home", action: "index"))
			return
		}
		params.max = params.max ?: 5
		params.offset = params.offset ?: 0

		Map model = discussion.getJSONModel(params)
		model = model << [notLoggedIn: user ? false : true, userId: user?.getId(),
				username: user ? user.getUsername() : '(anonymous)', isAdmin: UserGroup.canAdminDiscussion(user, discussion),
				templateVer: urlService.template(request), discussionHash: discussion.hash]

		// If used for pagination
		if (request.xhr) {
			if (!model.posts ) {
				// render false if there are no more comments to show.
				render false
			} else {
				render (template: "/discussion/posts", model: model)
			}
			return
		}
		if (user == null) {
			model.put("associatedGroups", []) // public discussion
		} else {
			List associatedGroups = UserGroup.getGroupsForWriter(user)
			List alreadySharedGroups = [], otherGroups = []

			associatedGroups.each { userGroup ->
				if (UserGroup.hasDiscussion(userGroup["id"], discussion.id)) {
					alreadySharedGroups << userGroup.plus([shared: true])
				} else {
					otherGroups << userGroup
				}
			}
			associatedGroups = alreadySharedGroups.sort { it.name }
			associatedGroups.addAll(otherGroups.sort { it.name })
			model.put("associatedGroups", associatedGroups)
		}
		render(view: "/home/discuss", model: model)
	}

	// Used to edit the discussion
	def edit() {
	
	}

	// This method will be called to update already created discussion
	def update() {
	
	}

	def delete() {
		User user = sessionUser()
		Discussion discussion = Discussion.findByHash(params.id)
		if (!discussion) {
			log.warn "DiscussionId not found: " + params.id
			renderJSONGet([success: false, message: g.message(code: "default.not.found.message", args: ["Discussion"])])
		} else {
			Map result = Discussion.delete(discussion, sessionUser())
			renderJSONGet(result)
		}
	}

	def publish(Discussion discussion) {
		User user = sessionUser()

		if (!discussion) {
			flash.message = g.message(code: "default.blank.message", args: ["Discussion"]) 
			redirect(url: toUrl(controller: "home", action: "index"))
			return
		}
		def discussionUserId = discussion.getUserId()
		if ((user?.id == discussionUserId) || !discussionUserId) {
			discussion.setIsPublic(true)
			Utils.save(discussion, true)
			flash.message = g.message(code: "default.updated.message", args: ["Discussion"]) 
			redirect(url: toUrl(action: "show", params: ["id": discussion.id]))
		} else {
			flash.message = g.message(code: "default.permission.denied") 
			redirect(url: toUrl(action: "show", params: ["id": discussion.id]))
		}
	}
}

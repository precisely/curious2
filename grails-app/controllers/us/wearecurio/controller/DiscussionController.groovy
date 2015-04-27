package us.wearecurio.controller

import org.springframework.transaction.annotation.Transactional	 
import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionController extends LoginController {

	static allowedMethods = [create: "POST", update: "POST", delete: "DELETE"]

	def index() {
		redirect(action: "list", params: params)
	}

	def create() {
		redirect(action: "createTopic", params: params)
	}

	def createTopic(Long plotDataId, String name, Long id, String discussionPost) {
		def user = sessionUser()
		UserGroup group = Discussion.loadGroup(params.group, user)

		if (!group) {
			flash.message = "Failed to create new discussion topic: can't post to this group"
		} else {
			Discussion discussion = Discussion.loadDiscussion(id, plotDataId, user)
			discussion = discussion ?: Discussion.create(user, name, group)

			if (discussion != null) {
				Utils.save(discussion, true)
				//flash.message = "Created new discussion: " + name

				if(discussionPost) {
					discussion.createPost(user, discussionPost)
				}

				Map model = discussion.getJSONModel(params)

				model = model << [notLoggedIn: user ? false : true, userId: user?.getId(),
					username: user ? user.getUsername() : '(anonymous)', isAdmin: UserGroup.canAdminDiscussion(user, discussion),
					templateVer: urlService.template(request)]

				// If used for pagination
				if (request.xhr) {
					render (template: "/discussion/posts", model: model)
					return
				}

				// see duplicated code in HomeController.discuss
				// edits here should be duplicated there
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

				render(view: "/home/discuss", model: model)
				// redirect(url:toUrl(controller:'home', action:'feed'))
				return
			} else {
				flash.message = "Failed to create new discussion topic: internal error"
			}
		}

	}

	def save() {
	
	}

	def show(Discussion discussion) {
		User user = sessionUser()

		if (!discussion){
			flash.message = g.message(code: "default.blank.message", args: ["Discussion"])
			redirect(url:toUrl(controller: "home", action: "index"))
			return
		} else if (!discussion.getIsPublic() && !user) {
			flash.message = "Must be logged in"
			redirect(url:toUrl(controller: "home", action: "index"))
			return
		}
		params.max = params.max ?: 5
		params.offset = params.offset ?: 0

		// see duplicated code in DiscussionController.createTopic
		// edits here should be duplicated there
		Map model = discussion.getJSONModel(params)
		model = model << [notLoggedIn: user ? false : true, userId: user?.getId(),
				username: user ? user.getUsername() : '(anonymous)', isAdmin: UserGroup.canAdminDiscussion(user, discussion),
				templateVer: urlService.template(request)]

		// If used for pagination
		if (request.xhr) {
			if (!model.posts ){
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

	def edit() {
	
	}

	def udate() {
	
	}

	def delete(Discussion discussion) {
		User user = sessionUser()
		if (!discussion) {
			log.warn "DiscussionId not found: " + params.id
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["That discussion"])])
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
		if ((user != null && user.getId() == discussionUserId) || (discussionUserId == null)) {
			discussion.setIsPublic(true)
			Utils.save(discussion, true)
			flash.message = g.message(code: "default.updated.message", args: ["Discussion"]) 
			redirect(url: toUrl(action: "show", params: ["id": discussion.id]))
		} else {
			flash.message = g.message(code: "default.permission.denied") 
			redirect(url: toUrl(action: "show", params: ["id": discussion.id]))
		}
	}

	def deletePost(Long deletePostId, Long discussionId) {
		debug "Attemtping to delete post id " + deletePostId
		DiscussionPost post = DiscussionPost.get(deletePostId)
		Discussion discussion = Discussion.get(discussionId)
		User user = sessionUser()

		if (!discussion) {
			flash.message = g.message(code: "default.blank.message", args: ["Discussion"])
		} else if (post != null && post.getDiscussionId() != discussionId) {
			flash.message = "Can't delete that post --- mismatching discussion id"
		} else {
			if (post != null && (user == null || (post.getUserId() != user.getId() && (!UserGroup.canAdminDiscussion(user, discussion))))) {
				flash.message = g.message(code: "default.permission.denied")
			} else {
				discussion.setUpdated(new Date())
				DiscussionPost.delete(post)
				Utils.save(discussion, true)
			}
		}
		redirect(url: toUrl(action: "show", params: ["id": discussionId]))
	}

	@Transactional(readOnly = true)
	def addComment(Discussion discussion) {
		debug "Attemping to add comment '" + params.message + "', plotIdMessage: " + params.plotIdMessage
		if (!discussion){
			if (request.xhr) {
				renderJSONPost([success: false, message: g.message(code: "default.blank.message", args: ["Discussion"])])
			} else {
				flash.message = g.message(code: "default.blank.message", args: ["Discussion"])
				redirect(url:toUrl(controller: "home", action:"feed"))
			}
			return
		}

		User user = sessionUser()
		def comment = DiscussionPost.createComment(params.message, user, discussion, 
				params.plotIdMessage, params)
		if (comment instanceof String) {
			if (request.xhr) {
				renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
			} else {
				flash.message = message(code: "default.permission.denied")
				redirect(url:toUrl(action:"show", params: [id: discussion.id]))
			}
			return
		}

		if (comment == null) {
			if (request.xhr) {
				renderJSONPost([success: false, message: g.message(code: "not.created.message", args: ["Comment"])])
				return
			}
			flash.message = g.message(code: "not.created.message", args: ["Comment"])
			redirect(url:toUrl(controller: "home", action: "index"))
		} else {
			if (request.xhr) {
				renderJSONPost([success: true])
				return
			}
			redirect(url:toUrl(action: "show", params:[id: discussion.id]))
		}
	}

}

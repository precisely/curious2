package us.wearecurio.controller

import us.wearecurio.model.*
import grails.gsp.PageRenderer
import us.wearecurio.utility.*

class DiscussionPostController extends LoginController{

	PageRenderer groovyPageRenderer
	static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

	def index() {
		Discussion discussion = Discussion.get(params.discussionId)
		if (!discussion) {
			renderJSONGet([posts: false])
			return
		}

		params.max = Math.min(Integer.parseInt(params.max) ?: 5, 100)
		params.offset = params.offset ?: 0
		List posts = discussion.getFollowupPosts([max: params.max, offset: params.offset])

		if (!posts) {
			renderJSONGet([posts: false])
		} else {
			renderJSONGet([posts: groovyPageRenderer.render(template: "/discussion/posts", model: [posts: posts, userId: sessionUser().id, 
				isAdmin: UserGroup.canAdminDiscussion(sessionUser(), discussion)])])
		}
	}

	def save() {
		debug "Attemping to add comment '" + params.message + "', plotIdMessage: " + params.plotIdMessage
		Discussion discussion = Discussion.get(params.id)
		if (!discussion){
			if (request.xhr) {
				renderJSONPost([success: false, message: g.message(code: "default.blank.message", args: ["Discussion"])])
			} else {
				flash.message = g.message(code: "default.blank.message", args: ["Discussion"])
				redirect(url: toUrl(controller: "home", action: "feed"))
			}
			return
		}

		User user = sessionUser()
		def comment = DiscussionPost.createComment(params.message, user, discussion, 
			params.plotIdMessage, params)

		// If user does not have permission to add comment response will be 'false'
		if (comment instanceof String) {
			if (request.xhr) {
				renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
			} else {
				flash.message = message(code: "default.permission.denied")
				redirect(url: toUrl(controller: "discussion", action: "show", params: ["id": discussion.id]))
			}
			return
		}

		if (!comment) {
			if (request.xhr) {
				renderJSONPost([success: false, message: g.message(code: "not.created.message", args: ["Comment"])])
				return
			}
			flash.message = g.message(code: "not.created.message", args: ["Comment"])
			redirect(url: toUrl(controller: "home", action: "index"))
		} else {
			if (request.xhr) {
				renderJSONPost([success: true])
				return
			}
			redirect(url: toUrl(controller: "discussion", action: "show", params: [id: discussion.id]))
		}
	}

	def delete(DiscussionPost post) {
		debug "Attemtping to delete post id " + params.id
		Discussion discussion
		User user = sessionUser()
		if (!post) {
			renderJSONPost([success: false, message: g.message(code: "default.blank.message", args: ["Post"])])
		} else {
			discussion = Discussion.get(post.discussionId)
			if (post && (!user || (post.getUserId() != user.id && (!UserGroup.canAdminDiscussion(user, discussion))))) {
				renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
			} else {
				discussion.setUpdated(new Date())
				DiscussionPost.delete(post)
				Utils.save(discussion, true)
				renderJSONPost([success: true, message: g.message(code: "default.deleted.message", args: ["Discussion", "post"])])
			}
		}
	}

	def update() {
	}
}

package us.wearecurio.controller

import us.wearecurio.model.*
import grails.gsp.PageRenderer
import us.wearecurio.utility.*

class DiscussionPostController extends LoginController{

	PageRenderer groovyPageRenderer
	static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

	def index(Integer offset) {
		Discussion discussion = Discussion.findByHash(params.discussionHash)
		if (!discussion) {
			renderJSONGet([posts: false, success: false, message: g.message(code: "default.blank.message", args: ["Discussion"])])
			return
		}

		params.max = Math.min(Integer.parseInt(params.max) ?: 4, 100)
		params.offset = offset ?: 0

		List<DiscussionPost> posts = discussion.getFollowupPosts(params)

		Map discussionDetails = [isAdmin: UserGroup.canAdminDiscussion(sessionUser(), discussion)]

		// Discussion details are only needed for the first page
		if (params.offset == 0) {
			discussionDetails.putAll(discussion.getJSONDesc())
		}

		JSON.use("jsonDate") {
			renderJSONGet([posts: posts ? posts*.getJSONDesc() : false, userId: sessionUser().id, success: true,
					discussionDetails: discussionDetails])
		}
	}

	def save() {
		debug "Attemping to add comment '" + params.message + "', plotIdMessage: " + params.plotIdMessage + 
		"for discussion with hash: ${params.discussionHash}"
		Discussion discussion = Discussion.findByHash(params.discussionHash)
		if (!discussion){
			renderJSONPost([success: false, message: g.message(code: "default.blank.message", args: ["Discussion"])])
			return
		}

		User user = sessionUser()
		def comment = DiscussionPost.createComment(params.message, user, discussion, 
		params.plotIdMessage, params)

		// If user does not have permission to add comment response will be 'false'
		if (comment instanceof String) {
			renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
			return
		}

		if (!comment) {
			renderJSONPost([success: false, message: g.message(code: "not.created.message", args: ["Comment"])])
			return
		} else {
			renderJSONPost([success: true])
			return
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

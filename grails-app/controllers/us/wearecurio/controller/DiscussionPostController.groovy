package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionPostController extends LoginController{

	static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

	def index() {
		Discussion discussion = Discussion.findByHash(params.discussionHash)
		if (!discussion) {
			renderJSONGet([posts: false, success: false, message: g.message(code: "default.blank.message", args: ["Discussion"])])
			return
		}

		params.max = Math.min(Integer.parseInt(params.max) ?: 4, 100)
		params.offset = params.offset ?: 0

		DiscussionPost firstPostInstance = discussion.getFirstPost()
		boolean isFollowUp = firstPostInstance?.getPlotDataId() != null

		List<DiscussionPost> posts = isFollowUp ? discussion.getFollowupPosts(params) : discussion.getPosts(params)

		Map discussionDetails 
		if (Integer.parseInt(params.offset + "") == 0) {
			discussionDetails = discussion.getJSONModel(params)
			discussionDetails["isAdmin"] = UserGroup.canAdminDiscussion(sessionUser(), discussion)
		} else {
			discussionDetails = [:]
		}

		JSON.use("jsonDate") {
			renderJSONGet([posts: posts ? posts*.getJSONDesc() : false, userId: sessionUser().id, success: true,
					isAdmin:UserGroup.canAdminDiscussion(sessionUser(), discussion),
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
		def comment = DiscussionPost.createComment(params.message, user, discussion, params.plotIdMessage, params)

		// If user does not have permission to add comment response will be 'false'
		if (comment instanceof String) {
			renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
			return
		}

		if (!comment) {
			renderJSONPost([success: false, message: g.message(code: "not.created.message", args: ["Comment"])])
			return
		}

		JSON.use("jsonDate") {
			renderJSONPost([success: true, post: comment.getJSONDesc(), userId: sessionUser().id, idAdmin:
					UserGroup.canAdminDiscussion(sessionUser(), discussion)])
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

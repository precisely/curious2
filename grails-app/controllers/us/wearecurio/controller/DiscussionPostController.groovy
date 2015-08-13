package us.wearecurio.controller

import grails.gsp.PageRenderer
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionPostController extends LoginController{

	PageRenderer groovyPageRenderer
	static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

	def index() {
		Discussion discussion = Discussion.findByHash(params.discussionHash)
		if (!discussion) {
			renderJSONGet([posts: false])
			return
		}

		params.max = Math.min(Integer.parseInt(params.max) ?: 4, 100)
		params.offset = params.offset ?: 0

		DiscussionPost firstPostInstance = discussion.getFirstPost()
		boolean isFollowUp = firstPostInstance?.getPlotDataId() != null

		List<DiscussionPost> posts = isFollowUp ? discussion.getFollowupPosts([max: params.max, offset: params.offset]) : 
				discussion.getPosts([max: params.max, offset: params.offset])

		if (!posts) {
			renderJSONGet([posts: false])
		} else {
			renderJSONGet([posts: posts*.getJSONDesc(), userId: sessionUser().id, isAdmin:
					UserGroup.canAdminDiscussion(sessionUser(), discussion)])
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
package us.wearecurio.controller

import grails.converters.JSON
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionPostController extends LoginController{

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

	def index(Integer offset, Integer max) {
		Discussion discussion = Discussion.findByHash(params.discussionHash)
		if (!discussion) {
			renderJSONGet([posts: false, success: false, message: g.message(code: "not.found.message", args: ["Discussion"])])
			return
		}

		params.max = Math.min(max ?: 4, 100)
		params.offset = offset ?: 0

		List<DiscussionPost> posts = discussion.getFollowupPosts(params)
		User currentUser = sessionUser()

		Map discussionDetails = [isAdmin: UserGroup.canAdminDiscussion(currentUser, discussion), 
				canWrite: UserGroup.canWriteDiscussion(currentUser, discussion), isFollowing: discussion.isFollower(currentUser.id)]

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
			renderJSONPost([success: false, message: g.message(code: "not.found.message", args: ["Discussion"])])
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
			renderJSONPost([success: false, message: g.message(code: "not.found.message", args: ["Post"])])
		} else {
			discussion = Discussion.get(post.discussionId)
			if (post && (!user || (post.getUserId() != user.id && (!UserGroup.canAdminDiscussion(user, discussion))))) {
				renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
			} else {
				discussion.setUpdated(new Date())
				DiscussionPost.delete(post)
				Utils.save(discussion, true)
				Map nextFollowupPost = discussion.getFollowupPosts([offset: params.offset ?: 0, max: 1, order: 'desc'])[0]?.getJSONDesc()
				boolean isAdmin = UserGroup.canAdminDiscussion(sessionUser(), discussion)
				JSON.use("jsonDate") {
					renderJSONPost([success: true, message: g.message(code: "default.deleted.message", args: ["Discussion", "comment"]), 
							nextFollowupPost: nextFollowupPost, isAdmin: isAdmin, discussionHash: discussion.hash])
				}
			}
		}
	}

	def update() {
		debug ("UserController.update() params:" + params)
		Map requestData = request.JSON
		User user = sessionUser()
		DiscussionPost discussionPost = DiscussionPost.get(params.id)
		if (!discussionPost) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["Post"])])
			return
		}

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}

		if (user.id == discussionPost.authorUserId) {
			discussionPost.message = requestData.message
			Utils.save(discussionPost, true)
			renderJSONGet([success: true])
		} else {
			renderJSONGet([success: false, message: g.message(code: "default.permission.denied")])
			return
		}
	}
}

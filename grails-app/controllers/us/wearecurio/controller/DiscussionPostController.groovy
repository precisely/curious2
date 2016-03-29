package us.wearecurio.controller

import grails.converters.JSON
import org.springframework.http.HttpStatus
import us.wearecurio.annotations.EmailVerificationRequired
import us.wearecurio.exception.AccessDeniedException
import us.wearecurio.exception.CreationNotAllowedException
import us.wearecurio.exception.InstanceNotFoundException
import us.wearecurio.exception.OperationNotAllowedException
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils

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

	@EmailVerificationRequired
	def save() {
		debug "Attempting to add comment '" + params.message +
				"for discussion with hash: ${params.discussionHash}"

		Discussion discussion = Discussion.findByHash(params.discussionHash)
		if (!discussion) {
			renderJSONPost([success: false, message: g.message(code: "not.found.message", args: ["Discussion"])])
			return
		}

		User user = sessionUser()
		def comment
		try {
			comment = DiscussionPost.createComment(params.message, user, discussion, params)
		} catch (CreationNotAllowedException e) {
			respond([message: g.message(code: "discussion.comment.disabled")], HttpStatus.NOT_ACCEPTABLE)
			return
		} catch (AccessDeniedException e) {
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

	@EmailVerificationRequired
	def delete(Long id) {
		User user = sessionUser()
		debug "$user attempting to delete post $params"

		DiscussionPost post
		try {
			post = DiscussionPost.deleteComment(id, user, null)
		} catch (InstanceNotFoundException e) {
			renderJSONPost([success: false, message: g.message(code: "not.found.message", args: ["Post"])])
			return
		} catch (AccessDeniedException e) {
			renderJSONPost([success: false, message: g.message(code: "default.permission.denied")])
			return
		} catch (OperationNotAllowedException e) {
			renderJSONPost([success: false, message: g.message(code: "discussion.description.can.not.deleted")])
			return
		}

		Discussion discussion = post.getDiscussion()

		Map nextFollowupPost = discussion.getFollowupPosts([offset: params.offset ?: 0, max: 1, order: 'desc'])[0]?.getJSONDesc()
		boolean isAdmin = UserGroup.canAdminDiscussion(sessionUser(), discussion)

		JSON.use("jsonDate") {
			renderJSONPost([success: true, message: g.message(code: "default.deleted.message", args: ["Discussion", "comment"]),
					nextFollowupPost: nextFollowupPost, isAdmin: isAdmin, discussionHash: discussion.hash])
		}
	}

	def update() {
		Map requestData = request.JSON
		debug ("UserController.update() params:" + requestData)
		User user = sessionUser()
		DiscussionPost discussionPost = DiscussionPost.get(requestData.id)
		if (!discussionPost) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["Post"])])
			return
		}

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		}

		// If message of the comment is empty or missing
		if (!requestData.message) {
			renderJSONGet([success: false, message: g.message(code: "discussion.comment.empty")])
			return
		}

		if (user.id == discussionPost.authorUserId) {
			discussionPost.message = requestData.message ?: ""
			discussionPost.updated = new Date()
			Utils.save(discussionPost, true)
			renderJSONGet([success: true])
		} else {
			renderJSONGet([success: false, message: g.message(code: "default.permission.denied")])
		}
	}
}

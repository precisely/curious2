package us.wearecurio.controller

import org.springframework.http.HttpStatus
import us.wearecurio.annotations.EmailVerificationRequired
import us.wearecurio.exception.CreationNotAllowedException
import us.wearecurio.security.NoAuth
import java.text.SimpleDateFormat
import java.text.DateFormat
import grails.converters.JSON
import us.wearecurio.model.*
import us.wearecurio.utility.*
import us.wearecurio.model.Model.Visibility

class DiscussionController extends LoginController {

	static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

	// Not being used right now as all discussion lists are comming form feed
	def index() {
	}

	@EmailVerificationRequired
	def save(Long plotDataId, String name, Long id, String discussionPost, String visibility) {
		log.debug "saving plotDataId:" + plotDataId + ", name:" + name + ", id:" + id + ", visibility:" + visibility
		def user = sessionUser()
		UserGroup group = Discussion.loadGroup(params.group, user)

		if (!group) {
			log.debug "Failure: cannot post to this group"
			renderJSONPost([success: false, message: "Failed to create new discussion topic: can't post to this group"])
			return
		}
		Discussion discussion = Discussion.loadDiscussion(id, plotDataId, user)
		Visibility discussionVisibility = visibility ? visibility.toUpperCase() : Visibility.PUBLIC

		if (!discussion) {
			try {
				discussion = Discussion.create(user, name, group, null, discussionVisibility)
			} catch (CreationNotAllowedException e) {
				respond([message: g.message(code: "discussion.creation.disabled")], HttpStatus.NOT_ACCEPTABLE)
				return
			}
		}

		if (discussion != null) {
			Utils.save(discussion, true)

			/*
			 * Always create a first post (i.e. DiscussionPost) which will be used as the description of the discussion.
			 * https://github.com/syntheticzero/curious2/issues/924
			 */
			DiscussionPost.createFirstPost(discussionPost ?: "", user, discussion, null)

			Map model = discussion.getJSONDesc()
			DateFormat df = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ssZ");
			
			log.debug "Successfully created new discussion: " + discussion.id

			renderJSONPost([discussion: [name: model.discussionTitle, hash: model.hash, created: df.format(model.discussionCreatedOn), 
					type: "dis", userAvatarURL: model.discussionOwnerAvatarURL, userName: model.discussionOwner, userHash: model.discussionOwnerHash, 
				isAdmin: true, totalComments: model.totalPostCount, groupName: model.groupName, id: model.discussionId], success: true])
		} else {
			log.debug "Failed to create discussion"

			renderJSONPost([success: false, message: "Failed to create new discussion topic: internal error"])
		}
	}

	@NoAuth
	def show() {
		User user = sessionUser()

		log.debug "Showing discussion:" + params.id

		Discussion discussion = Discussion.findByHash(params.id)
		if (!discussion){
			log.debug "Discussion not found"
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["Discussion"])])
			return
		}

		if (!discussion.getIsPublic() && !user) {
			log.debug "Cannot show non-public discussion to anonymous user"
			renderJSONGet([success: false, message: g.message(code: "default.login.message")])
			return
		}

		Map model = discussion.getJSONDesc()
		model.putAll([notLoggedIn: user ? false : true, userId: user?.id, associatedGroups: [],		// Public discussion
				username: user ? user.getUsername() : '(anonymous)', isAdmin: UserGroup.canAdminDiscussion(user, discussion), isFollowing: discussion.isFollower(user?.id),
				templateVer: urlService.template(request), discussionHash: discussion.hash, canWrite: UserGroup.canWriteDiscussion(user, discussion)])

		/*if (user) {
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
		}*/

		JSON.use("jsonDate") {
			log.debug "Rendering discussion:" + model
			renderJSONGet([success: true, discussionDetails: model])
		}
	}
	
	def disableComments(boolean disable, String id) {
		debug("Attempting to disable comments on trackathon $params")

		Discussion discussion = Discussion.findByHash(id)
		if (!discussion) {
			log.warn "discussionId not found: " + id
			renderJSONGet([success: false, message: g.message(code: "default.not.found.message", args: ["Discussion"])])
		} else {
			Map result = Discussion.disableComments(discussion, sessionUser(), disable)
			renderJSONGet(result)
		}
	}

	// Used to edit the discussion
	def edit() {

	}

	// This method will be called to update already created discussion
	def update() {
		Map requestData = request.JSON
		debug "Update discussion data $requestData"

		User user = sessionUser()

		if (!user) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		Discussion discussion = Discussion.findByHash(requestData.discussionHash)

		if (!discussion) {
			renderJSONGet([success: false, message: "No such discussion found"])
			return
		}

		if (Discussion.update(discussion, requestData, user)) {
			DiscussionPost firstPost = discussion.getFirstPost()
			if (firstPost) {
				DiscussionPost.update(user, discussion, firstPost, requestData.message?.toString())
			}

			renderJSONGet([success: true])
		} else {
			renderJSONGet([success: false, message: "Failed to update discussion name"])
		}
	}

	def delete() {
		User user = sessionUser()
		debug("$user attempting to delete discussion $params")

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
		log.debug "Publishing discussion: " + discussion?.id
		User user = sessionUser()

		if (!discussion) {
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["Discussion"])]) 
			return
		}
		def discussionUserId = discussion.getUserId()
		if ((user?.id == discussionUserId) || !discussionUserId) {
			discussion.setIsPublic(true)
			Utils.save(discussion, true)
			renderJSONGet([success: true, message: g.message(code: "default.updated.message", args: ["Discussion"])]) 
		} else {
			log.debug "Failed to publish discussion: user ID does not match owner"
			renderJSONGet([success: false, message: g.message(code: "default.permission.denied", args: ["Discussion"])]) 
		}
	}

	def follow() {
		debug("DiscussionController.follow: with params ${params}")
		User user = sessionUser()
		Discussion discussion = Discussion.findByHash(params.id)
		boolean unfollow = params.unfollow ? true : false
		
		if (!user) {
			log.debug "Failed to follow discussion: not logged in"
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["User"])])
			return
		} else if (!discussion) {
			log.debug "Failed to follow discussion: discussion does not exist"
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["Discussion"])])
			return
		}

		if (!unfollow) {
			if (discussion.addFollower(user.id)) {
				renderJSONGet([success: true])
			} else {
				log.debug "Failed to follow discussion: internal error"
				renderJSONGet([success: false, message: g.message(code: "default.operation.failed")])
			}
		} else {
			discussion.removeFollower(user.id)
			log.debug "Unfollowed discussion"
			renderJSONGet([success: true])
		}
	}
}

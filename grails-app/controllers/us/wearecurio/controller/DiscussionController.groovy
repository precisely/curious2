package us.wearecurio.controller

import java.text.SimpleDateFormat
import java.text.DateFormat
import grails.converters.JSON
import org.springframework.transaction.annotation.Transactional	 
import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*
import us.wearecurio.model.Model.Visibility

class DiscussionController extends LoginController {

	static allowedMethods = [save: "POST", update: "POST", delete: "DELETE"]

	// Not being used right now as all discussion lists are comming form feed
	def index() {
	}

	def save(Long plotDataId, String name, Long id, String discussionPost, String visibility) {
		def user = sessionUser()
		UserGroup group = null
		
		if (params.group) {
			group = Discussion.loadGroup(params.group, user)

			if (!group) {
				renderJSONPost([success: false, message: "Failed to create new discussion topic: can't post to this group"])
				return
			}
		}
		Discussion discussion = Discussion.loadDiscussion(id, plotDataId, user)
		Visibility discussionVisibility = visibility ? visibility.toUpperCase() : Visibility.PUBLIC
		discussion = discussion ?: Discussion.create(user, name, group, null, discussionVisibility)

		if (discussion != null) {
			Utils.save(discussion, true)

			if (discussionPost) {
				discussion.createPost(user, discussionPost)
			}

			Map model = discussion.getJSONDesc()
			DateFormat df = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ssZ");

			renderJSONPost([discussion: [name: model.discussionTitle, hash: model.hash, created: df.format(model.discussionCreatedOn), 
					type: "dis", userAvatarURL: model.discussionOwnerAvatarURL, userName: model.discussionOwner, userHash: model.discussionOwnerHash, 
				isAdmin: true, totalComments: model.totalPostCount, groupName: model.groupName, id: model.discussionId], success: true])
		} else {
			renderJSONPost([success: false, message: "Failed to create new discussion topic: internal error"])
		}
	}

	def show() {
		User user = sessionUser()

		Discussion discussion = Discussion.findByHash(params.id)
		if (!discussion){
			renderJSONGet([success: false, message: g.message(code: "not.exist.message", args: ["Discussion"])])
			return
		}

		if (!discussion.getIsPublic() && !user) {
			renderJSONGet([success: false, message: g.message(code: "default.login.message")])
			return
		}

		Map model = discussion.getJSONDesc()
		model.putAll([notLoggedIn: user ? false : true, userId: user?.id, associatedGroups: [],		// Public discussion
				username: user ? user.getUsername() : '(anonymous)', isAdmin: UserGroup.canAdminDiscussion(user, discussion),
				templateVer: urlService.template(request), discussionHash: discussion.hash])

		if (user) {
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

		JSON.use("jsonDate") {
			renderJSONGet([success: true, discussionDetails: model])
		}
	}

	// Used to edit the discussion
	def edit() {

	}

	// This method will be called to update already created discussion
	def update() {

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
			renderJSONGet([success: false, message: g.message(code: "default.permission.denied", args: ["Discussion"])]) 
		}
	}
}

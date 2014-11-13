package us.wearecurio.controller

import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.model.*
import us.wearecurio.utility.*

class DiscussionController extends LoginController {

	static allowedMethods = [create: "POST", update: "POST", delete: "POST"]

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

}

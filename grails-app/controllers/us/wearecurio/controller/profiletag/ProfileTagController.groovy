package us.wearecurio.controller.profiletag

import us.wearecurio.controller.LoginController
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.profiletags.ProfileTag
import us.wearecurio.model.profiletags.ProfileTagStatus
import us.wearecurio.services.SearchService

/**
 * A controller for handling all ProfileTag related requests like getting all interest tags, adding interest tags and
 * deleting interest tags.
 */
class ProfileTagController extends LoginController {

	def getInterestTags() {
		debug "ProfileTagController.getInterestTags() params: " + params

		User user = sessionUser()
		if (!user) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		renderJSONGet([publicInterestTags: ProfileTag.getPublicInterestTags(user.id),
				privateInterestTags: ProfileTag.getPrivateInterestTags(user.id)])
	}

	def addInterestTag() {
		debug "ProfileTagController.addInterestTag() params: " + params

		User user = sessionUser()

		String message = !user ? AUTH_ERROR_MESSAGE : (!params.tagNames ? 'No tag names specified' :
				(!params.tagStatus ? 'No tag status specified' : ''))

		if (message) {
			debug message
			renderStringGet(message)

			return
		}

		ProfileTagStatus status = params.tagStatus as ProfileTagStatus
		List<String> errorTagsList = []
		List<ProfileTag> newProfileTags = []
		List<String> tagNames = params.tagNames.split(',')

		tagNames.each { tagName ->
			Tag newTag = Tag.look(tagName.toLowerCase().trim())

			ProfileTag newProfileTag = ProfileTag.addInterestTag(newTag, user.id, status, true)
			if (newProfileTag)  {
				debug "Successfully added interest profile tag - ${tagName} for userId ${user.id}."
				newProfileTags.add(newProfileTag)
			} else {
				debug "Failure adding interest profile tag - ${tagName} for uesrId ${user.id}."
				errorTagsList.add(tagName)
			}
		}


		if (errorTagsList)  {
			debug "Failure adding profile tag"
			renderStringGet("Error adding interest tag for tags ${errorTagsList}")
		} else {
			debug "Successfully added profile tag"
			renderJSONGet([success: true, profileTag: newProfileTags])
		}
	}

	def deleteInterestTag(ProfileTag profileTagInstance) {
		debug "ProfileTagController.deleteInterestTag() params: ${params}"

		User user = sessionUser()
		String message = !user ? AUTH_ERROR_MESSAGE : (!profileTagInstance ? 'No profile tag id specified' : '')

		if (message) {
			debug message
			renderStringGet(message)

			return
		}

		try {
			profileTagInstance.delete(flush: true)
			SearchService.get()?.index(user)

			debug "Successfully removed ProfileTag"
			renderJSONGet([success: true])
		} catch(Exception e) {
			debug "Failure removing profile tag", e
			renderStringGet("Error deleting profile tag")
		}
	}
}

package us.wearecurio.controller.profiletag

import grails.converters.JSON
import us.wearecurio.controller.LoginController
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.profiletags.ProfileTag
import us.wearecurio.profiletags.ProfileTagStatus
import us.wearecurio.utility.Utils

class ProfileTagController extends LoginController {

	def getInterestTags() {
		debug "ProfileTagController.getInterestTags() userId: " + params.userId

		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		renderJSONGet([publicInterestTags: ProfileTag.getPublicInterestTags(user.id),
				privateInterestTags: ProfileTag.getPrivateInterestTags(user.id)])
	}

	def addInterestTag() {
		debug "ProfileTagController.addInterestTag() params " + params

		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		if (!params.tagName) {
			debug "no tag name specified"
			renderStringGet("No tag name specified")
			return
		}

		if (!params.tagStatus) {
			debug "no tag status specified"
			renderStringGet("No tag status specified")
			return
		}

		ProfileTagStatus status = params.tagStatus as ProfileTagStatus
		Tag newTag = Tag.look(params.tagName.toLowerCase().trim())

		boolean flush = true
		ProfileTag newProfileTag = ProfileTag.addInterestTag(newTag, user.id, status, flush)

		if (newProfileTag)  {
			debug "Successfully added profile tag"
			renderJSONGet([success: true, profileTag: newProfileTag])
		} else {
			debug "Failure adding profile tag"
			renderStringGet("Error adding interest tag")
		}
	}

	def deleteInterestTag(ProfileTag profileTagInstance) {
		debug "ProfileTagController.deleteInterestTag() params: ${params}"

		User user = sessionUser()
		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		if (!profileTagInstance) {
			debug "no profile tag id specified"
			renderStringGet("No profile tag id specified")

			return
		}

		try {
			profileTagInstance.delete(flush: true)

			debug "Successfully removed ProfileTag"
			renderJSONGet([success: true])
		} catch(Exception e) {
			debug "Failure removing profile tag", e
			renderStringGet("Error deleting profile tag")
		}
	}
}

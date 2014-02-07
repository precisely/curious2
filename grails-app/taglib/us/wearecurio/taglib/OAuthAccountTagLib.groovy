package us.wearecurio.taglib

import us.wearecurio.model.ThirdParty

class OAuthAccountTagLib {

	static namespace = "oauth"

	def OAuthAccountService

	/**
	 * Renders body if particular type of OAuthAccount is linked with
	 * current logged in user or not.
	 * 
	 * @attr typeId REQUIRED Type of OAuthAccount to check
	 * @aatr userId REQUIRED UserId of the current user
	 */
	def isLinked = { attrs, body ->
		if (!attrs.typeId || !attrs.userId) {
			throwTagError("Tag [oauth:isLinked] requires two missing parameters: typeId & userId.")
		}
		ThirdParty type = ThirdParty[attrs.typeId]

		out << body(OAuthAccountService.isLinked(type, attrs.userId))
	}

}
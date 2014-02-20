package us.wearecurio.taglib

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty

class OAuthAccountTagLib {

	static namespace = "oauth"

	/**
	 * Renders body if particular type of OAuthAccount is linked with
	 * current logged in user or not.
	 * 
	 * @attr typeId REQUIRED Type of OAuthAccount to check
	 * @attr userId REQUIRED UserId of the current user
	 */
	def checkSubscription = { attrs, body ->
		if (!attrs.typeId || !attrs.userId) {
			throwTagError("Tag [oauth:checkSubscription] requires two missing parameters: typeId & userId.")
		}
		ThirdParty type = ThirdParty[attrs.typeId]

		out << body(OAuthAccount.findByTypeIdAndUserId(type, attrs.userId))
	}

}
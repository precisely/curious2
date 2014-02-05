package us.wearecurio.taglib

import us.wearecurio.model.ThirdParty

class OAuthAccountTagLib {

	static namespace = "oauth"

	def OAuthAccountService
	def securityService

	/**
	 * Renders body if particular type of oauth account is linked with
	 * current logged in user or not.
	 * 
	 * @attr typeId REQUIRED Type of oauth account to check
	 */
	def isLinked = { attrs, body ->
		ThirdParty type = ThirdParty[attrs.typeId]

		out << body(OAuthAccountService.isLinked(type))
	}

}

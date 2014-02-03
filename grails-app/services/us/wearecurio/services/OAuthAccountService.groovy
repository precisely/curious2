package us.wearecurio.services

import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User

class OAuthAccountService {

	def securityService

	/**
	 * Used to create OAuthAccount instance for given provider/type for current loggedIn user.
	 * @param typeId Type of the provider. See constants in OAuthAccount.groovy.
	 * @param accountId Account Identity for current provider.
	 * @param accessToken Access Token after authorization.
	 * @param accessSeret Access Secret after authorization.
	 * @return OAuthAccount instance for given parameters.
	 */
	OAuthAccount createOrUpdate(int typeId, String accountId, String accessToken, String accessSeret) {
		User currentUser = securityService.currentUser

		OAuthAccount.createOrUpdate(typeId, currentUser.id, accountId, accessToken, accessSeret ?: "")
	}

	OAuthAccount createOrUpdate(int typeId, String accountId, Token tokenInstance) {
		createOrUpdate(typeId, accountId, tokenInstance.token, tokenInstance.secret)
	}

	boolean isLinked(int typeId) {
		User currentUser = securityService.currentUser

		if (OAuthAccount.findByTypeIdAndUserId(typeId, currentUser.id)) {
			return true
		}
		return false
	}

}
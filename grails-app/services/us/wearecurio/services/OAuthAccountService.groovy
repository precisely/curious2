package us.wearecurio.services

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User

class OAuthAccountService {

	def securityService

	boolean isLinked(int typeId) {
		User currentUser = securityService.currentUser

		if (OAuthAccount.findByTypeIdAndUserId(typeId, currentUser.id)) {
			return true
		}
		return false
	}

}
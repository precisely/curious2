package us.wearecurio.services

import static us.wearecurio.model.OAuthAccount.*
import us.wearecurio.model.OAuthAccount

class MovesDataService {

	Map unSubscribe(Long userId) {
		log.debug "unSubscribe() account:" + userId

		OAuthAccount account = OAuthAccount.findByUserIdAndTypeId(userId, MOVES_ID)
		if (!account) {
			log.info "No moves subscription found for userId [$userId]"
			return [success: false, message: "No subscription found"]
		}

		account.delete()
		[success: true]
	}

}
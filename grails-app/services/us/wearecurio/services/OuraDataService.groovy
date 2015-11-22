package us.wearecurio.services

import us.wearecurio.model.OAuthAccount
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException

/**
 * Created by causecode on 22/11/15.
 */
class OuraDataService extends DataService {
	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		return null
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		return null
	}

	@Override
	void notificationHandler(String notificationData) {

	}
}

package us.wearecurio.services

import org.codehaus.groovy.grails.web.json.JSONElement
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.jawbone.JawboneUpTagUnitMap

class JawboneUpDataService extends DataService {

	static final String BASE_URL = "https://jawbone.com/nudge/api%s"
	static final String COMMENT = "(Jawbone Up)"
	static final String SET_NAME = "JUP"

	JawboneUpTagUnitMap tagUnitMap = new JawboneUpTagUnitMap()

	JawboneUpDataService() {
		provider = "jawboneup"
		typeId = ThirdParty.JAWBONE
		profileURL = String.format(BASE_URL, "/users/@me")
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		return null;
	}

	/**
	 * Overriding default implementation so to send accept header to all requests
	 */
	@Override
	JSONElement getResponse(Token tokenInstance, String requestUrl, String method = "get", Map queryParams = [:],
			Map requestHeaders = [:]) {
		requestHeaders << ["Accept": "application/json"]
		// TODO Remove this. This should work automatically.
		requestHeaders << ["Authorization": "Bearer ${tokenInstance?.token}"]
		super.getResponse(tokenInstance, requestUrl, method, queryParams, requestHeaders)
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void notificationHandler(String notificationData) {
		// TODO Auto-generated method stub
	}
}
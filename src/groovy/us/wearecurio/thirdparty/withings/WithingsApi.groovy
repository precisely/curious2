package us.wearecurio.thirdparty.withings

import org.scribe.builder.api.DefaultApi10a
import org.scribe.model.Token
import org.scribe.model.Verb

class WithingsApi extends DefaultApi10a {

	@Override
	String getRequestTokenEndpoint() {
		return "https://oauth.withings.com/account/request_token"
	}

	@Override
	Verb getRequestTokenVerb() {
		return Verb.GET
	}

	@Override
	String getAuthorizationUrl(Token requestToken) {
		return "https://oauth.withings.com/account/authorize?oauth_token=" + requestToken.getToken()
	}

	@Override
	String getAccessTokenEndpoint() {
		return "https://oauth.withings.com/account/access_token"
	}

}
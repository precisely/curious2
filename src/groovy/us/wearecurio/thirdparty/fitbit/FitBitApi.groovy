package us.wearecurio.thirdparty.fitbit

import org.scribe.builder.api.DefaultApi10a
import org.scribe.model.Token
import org.scribe.model.Verb

class FitBitApi extends DefaultApi10a {

	@Override
	String getRequestTokenEndpoint() {
		return "https://www.fitbit.com/oauth/request_token"
	}

	@Override
	Verb getRequestTokenVerb() {
		return Verb.POST
	}

	@Override
	String getAuthorizationUrl(Token requestToken) {
		return "https://www.fitbit.com/oauth/authorize?oauth_token=" + requestToken.getToken()
	}

	@Override
	String getAccessTokenEndpoint() {
		return "https://www.fitbit.com/oauth/access_token"
	}

}
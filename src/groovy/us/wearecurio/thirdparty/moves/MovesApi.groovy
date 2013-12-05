package us.wearecurio.thirdparty.moves

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import org.scribe.oauth.OAuthService

class MovesApi extends DefaultApi20 {

	private static final String AUTHORIZE_URL = "https://api.moves-app.com/oauth/v1/authorize?response_type=code&client_id=%s&scope=%s&redirect_uri=%s";

	@Override
	String getAccessTokenEndpoint() {
		"https://api.moves-app.com/oauth/v1/access_token"
	}

	@Override
	Verb getAccessTokenVerb() {
		return Verb.POST
	}

	@Override
	String getAuthorizationUrl(OAuthConfig config) {
		String.format(AUTHORIZE_URL, config.apiKey, config.scope, config.callback)
	}

	@Override
	OAuthService createService(OAuthConfig config) {
		return new MovesOAuth20ServiceImpl(this, config)
	}

}
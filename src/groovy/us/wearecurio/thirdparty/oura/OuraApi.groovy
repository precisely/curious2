package us.wearecurio.thirdparty.oura

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import org.scribe.utils.OAuthEncoder

class OuraApi extends DefaultApi20 {

	private static final String AUTHORIZE_URL = "https://cloud.ouraring.com/oauth/authorize?response_type=code&client_id=%s&scope=%s&redirect_uri=%s";

	@Override
	String getAccessTokenEndpoint() {
		"https://cloud.ouraring.com/oauth/token"
	}

	@Override
	Verb getAccessTokenVerb() {
		return Verb.POST
	}

	@Override
	String getAuthorizationUrl(OAuthConfig config) {
		println "$config.callback"
		String.format(AUTHORIZE_URL, config.apiKey, config.scope, OAuthEncoder.encode(config.callback))
	}

	/*@Override
	OAuthService createService(OAuthConfig config) {
		return new MovesOAuth20ServiceImpl(this, config)
	}*/

}
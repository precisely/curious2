package us.wearecurio.thirdparty.oura

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import org.scribe.utils.OAuthEncoder
import us.wearecurio.thirdparty.PayloadTypeOAuth20ServiceImpl

class OuraApi extends DefaultApi20 {

	static final String BASE_URL = "https://api.ouraring.com"

	private static final String AUTHORIZE_URL = "$BASE_URL/oauth/authorize?response_type=code&client_id=%s&scope=%s&redirect_uri=%s";

	@Override
	String getAccessTokenEndpoint() {
		"$BASE_URL/oauth/token"
	}

	@Override
	Verb getAccessTokenVerb() {
		return Verb.POST
	}

	@Override
	String getAuthorizationUrl(OAuthConfig config) {
		return String.format(AUTHORIZE_URL, config.apiKey, config.scope, OAuthEncoder.encode(config.callback))
	}

	@Override
	PayloadTypeOAuth20ServiceImpl createService(OAuthConfig config) {
		return new PayloadTypeOAuth20ServiceImpl(this, config)
	}
}

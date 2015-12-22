package us.wearecurio.thirdparty.oura

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import org.scribe.utils.OAuthEncoder
import us.wearecurio.thirdparty.PayloadTypeOAuth20ServiceImpl
import us.wearecurio.thirdparty.QueryParamTypeOAuth20ServiceImpl

class OuraApi extends DefaultApi20 {

	private static final String AUTHORIZE_URL = "https://ouracloud.ouraring.com/oauth/authorize?response_type=code&client_id=%s&scope=%s&redirect_uri=%s";

	@Override
	String getAccessTokenEndpoint() {
		"https://ouracloud.ouraring.com/oauth/token"
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

	@Override
	PayloadTypeOAuth20ServiceImpl createService(OAuthConfig config) {
		return new PayloadTypeOAuth20ServiceImpl(this, config)
	}
}

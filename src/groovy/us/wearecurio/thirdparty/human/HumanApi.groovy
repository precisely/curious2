package us.wearecurio.thirdparty.human

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import org.scribe.oauth.OAuthService
import org.scribe.utils.OAuthEncoder

import us.wearecurio.thirdparty.PayloadTypeOAuth20ServiceImpl

class HumanApi extends DefaultApi20 {

	private static final String AUTHORIZE_URL = "https://user.humanapi.co/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s";

	@Override
	String getAccessTokenEndpoint() {
		"https://user.humanapi.co/oauth/token"
	}

	@Override
	Verb getAccessTokenVerb() {
		return Verb.POST
	}

	@Override
	String getAuthorizationUrl(OAuthConfig config) {
		String.format(AUTHORIZE_URL, config.apiKey, OAuthEncoder.encode(config.callback))
	}

	@Override
	OAuthService createService(OAuthConfig config) {
		new PayloadTypeOAuth20ServiceImpl(this, config)
	}

}
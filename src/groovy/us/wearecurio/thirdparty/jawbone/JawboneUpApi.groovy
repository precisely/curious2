package us.wearecurio.thirdparty.jawbone

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.oauth.OAuthService

import us.wearecurio.thirdparty.QueryParamTypeOAuth20ServiceImpl

class JawboneUpApi extends DefaultApi20 {

	private static final String API_BASE_URL = "https://jawbone.com/auth/oauth2"
	private static final String AUTHORIZE_URL = "$API_BASE_URL/auth?response_type=code&client_id=%s&scope=%s&redirect_uri=%s"

	@Override
	OAuthService createService(OAuthConfig config) {
		return new QueryParamTypeOAuth20ServiceImpl(this, config)
	}

	@Override
	String getAccessTokenEndpoint() {
		return "$API_BASE_URL/token"
	}

	@Override
	String getAuthorizationUrl(OAuthConfig config) {
		String.format(AUTHORIZE_URL, config.apiKey, config.scope, config.callback)
	}
}
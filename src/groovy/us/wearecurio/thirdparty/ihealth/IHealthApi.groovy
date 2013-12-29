package us.wearecurio.thirdparty.ihealth

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import org.scribe.oauth.OAuthService
import org.scribe.utils.OAuthEncoder

class IHealthApi extends DefaultApi20 {

	private static final String AUTHORIZE_URL = "http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization?response_type=code&client_id=%s&APIName=%s&redirect_uri=%s";

	@Override
	String getAccessTokenEndpoint() {
		"http://sandboxapi.ihealthlabs.com/OpenApiV2/OAuthv2/userauthorization/"
	}

	@Override
	Verb getAccessTokenVerb() {
		return Verb.POST
	}

	@Override
	String getAuthorizationUrl(OAuthConfig config) {
		String.format(AUTHORIZE_URL, OAuthEncoder.encode(config.apiKey), OAuthEncoder.encode(config.scope), OAuthEncoder.encode(config.callback))
	}

}
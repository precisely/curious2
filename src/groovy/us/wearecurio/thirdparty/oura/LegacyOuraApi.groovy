package us.wearecurio.thirdparty.oura

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import us.wearecurio.thirdparty.PayloadTypeOAuth20ServiceImpl

class LegacyOuraApi extends DefaultApi20 {

	static final String BASE_URL = "https://cloud.ouraring.com"

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
		/**
		 * Spring Security OAuth2 release version 2.0.6.RELEASE has a bug which was double encoding the "redirect_uri"
		 * parameter. That has been fixed in 2.0.7.RELEASE but upgrading that in Ouracloud application requires
		 * various changes in the Grails plugin of Spring Security OAuth2 provider.
		 *
		 * So, removing the encoding from here temporally to fix that problem until the actual problem is fixed at
		 * the Ouracloud application.
		 *
		 * https://github.com/syntheticzero/curious2/issues/808
		 * https://github.com/spring-projects/spring-security-oauth/compare/2.0.6.RELEASE...2.0.7.RELEASE
		 * https://github.com/spring-projects/spring-security-oauth/issues/430
		 */
		//String.format(AUTHORIZE_URL, config.apiKey, config.scope, OAuthEncoder.encode(config.callback))
		return String.format(AUTHORIZE_URL, config.apiKey, config.scope, config.callback)
	}

	@Override
	PayloadTypeOAuth20ServiceImpl createService(OAuthConfig config) {
		return new PayloadTypeOAuth20ServiceImpl(this, config)
	}
}

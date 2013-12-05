package us.wearecurio.thirdparty.ttandme

import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.Verb
import org.scribe.oauth.OAuthService

class Twenty3AndMeApi extends DefaultApi20 {

	private static final String AUTHORIZE_URL = "https://api.23andme.com/authorize?response_type=code&redirect_uri=%s&client_id=%s&scope=%s";

	@Override
	String getAccessTokenEndpoint() {
		"https://api.23andme.com/token/"
	}

	@Override
	public Verb getAccessTokenVerb() {
		return Verb.POST
	}

	@Override
	String getAuthorizationUrl(OAuthConfig config) {
		String.format(AUTHORIZE_URL, config.callback, config.apiKey, config.scope)
	}

	/**
	 * This is needed to override because default implementation for this (i.e. org.scribe.oauth.Default20ServiceImpl)
	 * sends parameter using queryString technique but according to 23AndMe API, query parameters needs to passed as
	 * payload. So writing our custom service implementation will overcome this problem.
	 * 
	 * @see https://github.com/fernandezpablo85/scribe-java/blob/master/src/main/java/org/scribe/oauth/OAuth20ServiceImpl.java 
	 */
	@Override
	OAuthService createService(OAuthConfig config) {
		return new Twenty3AndMeOAuth20ServiceImpl(this, config)
	}

}

package us.wearecurio.thirdparty

import grails.converters.JSON
import grails.util.Environment

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.OAuthConstants
import org.scribe.model.OAuthRequest
import org.scribe.model.Response
import org.scribe.model.Token
import org.scribe.model.Verifier
import org.scribe.oauth.OAuth20ServiceImpl

/**
 * By default scribe library sends parameter as query string. But some API's
 * like Twenty3AndMe, Human etc. accepts access token parameters as request
 * header payload. This class fulfills the same requirement.
 *
 */
class PayloadTypeOAuth20ServiceImpl extends OAuth20ServiceImpl {

	DefaultApi20 api
	OAuthConfig config

	PayloadTypeOAuth20ServiceImpl(DefaultApi20 api, OAuthConfig config) {
		super(api, config)
		this.api = api
		this.config = config
	}

	@Override
	Token getAccessToken(Token requestToken, Verifier verifier) {
		OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint())

		Map queryParams = [:]

		queryParams.put(OAuthConstants.CLIENT_ID, config.getApiKey())
		queryParams.put(OAuthConstants.CLIENT_SECRET, config.getApiSecret())
		queryParams.put(OAuthConstants.REDIRECT_URI, config.getCallback())

		if (verifier instanceof RefreshTokenVerifier) {
			queryParams.put(RefreshTokenVerifier.REFRESH_TOKEN, verifier.getValue())
			queryParams.put("grant_type", RefreshTokenVerifier.REFRESH_TOKEN)
		} else {
			queryParams.put(OAuthConstants.CODE, verifier.getValue())
			queryParams.put("grant_type", "authorization_code")
		}

		if(config.hasScope()) queryParams.put(OAuthConstants.SCOPE, config.getScope())

		request.addPayload(queryParams.collect { key, value -> "$key=$value" }.join("&"))

		Response response = request.send()

		JSONObject responseJSON = JSON.parse(response.body)
		if (Environment.current == Environment.DEVELOPMENT) {
			println "Received response from ${api.class.simpleName}: $response.body"
		}

		return new Token(responseJSON.access_token, "", responseJSON.toString())
	}

	@Override
	void signRequest(Token accessToken, OAuthRequest request) {
		request.addQuerystringParameter(OAuthConstants.ACCESS_TOKEN, accessToken.getToken());
		request.addHeader("Authorization", "Bearer $accessToken.token")
	}

}
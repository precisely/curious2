package us.wearecurio.thirdparty.moves

import grails.converters.JSON
import grails.util.Environment

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.builder.api.DefaultApi20
import org.scribe.exceptions.OAuthException
import org.scribe.model.OAuthConfig
import org.scribe.model.OAuthConstants
import org.scribe.model.OAuthRequest
import org.scribe.model.Response
import org.scribe.model.Token
import org.scribe.model.Verifier
import org.scribe.oauth.OAuth20ServiceImpl

import us.wearecurio.thirdparty.RefreshTokenVerifier

/**
 * @deprecated Use {@link us.wearecurio.thirdparty.QueryParamTypeOAuth20ServiceImpl QueryParamTypeOAuth20ServiceImpl }
 * @author Shashank
 */
@Deprecated
class MovesOAuth20ServiceImpl extends OAuth20ServiceImpl {

	DefaultApi20 api
	OAuthConfig config

	MovesOAuth20ServiceImpl(DefaultApi20 api, OAuthConfig config) {
		super(api, config)
		this.api = api
		this.config = config
	}

	@Override
	Token getAccessToken(Token requestToken, Verifier verifier) {
		Map queryParams = [:]

		/**
		 * Checking if current request is for getting refresh token or for access token.
		 * If verifier is an instance of RefreshTokenVerifier then setting grant_type as
		 * refresh_token.
		 */
		if (verifier instanceof RefreshTokenVerifier) {
			queryParams.put(RefreshTokenVerifier.REFRESH_TOKEN, verifier.getValue())
			queryParams.put("grant_type", RefreshTokenVerifier.REFRESH_TOKEN)
		} else {
			queryParams.put(OAuthConstants.CODE, verifier.getValue())
			queryParams.put("grant_type", "authorization_code")
		}

		queryParams.put(OAuthConstants.CLIENT_ID, config.apiKey)
		queryParams.put(OAuthConstants.REDIRECT_URI, config.callback)
		queryParams.put(OAuthConstants.CLIENT_SECRET, config.apiSecret)

		StringBuilder accessTokenEndpoint = new StringBuilder(api.accessTokenEndpoint)
		accessTokenEndpoint.append("?")
		accessTokenEndpoint.append(queryParams.collect { key, value -> "$key=$value" }.join("&"))

		OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), accessTokenEndpoint.toString())

		Response response = request.send()

		String responseBody = response.body

		if (Environment.current != Environment.PRODUCTION) {
			println "Received response from moves api: $responseBody"
		}
		JSONObject responseJSON = JSON.parse(responseBody)

		if(!responseJSON.access_token) {
			throw new OAuthException("Response body is incorrect. Can't extract a token from this: $responseBody", null);
		}

		return new Token(responseJSON.access_token, "", responseBody)
	}

}
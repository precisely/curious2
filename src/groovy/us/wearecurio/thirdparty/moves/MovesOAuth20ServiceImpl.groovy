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
		queryParams.put("grant_type", "authorization_code")
		queryParams.put(OAuthConstants.CODE, verifier.value)
		queryParams.put(OAuthConstants.CLIENT_ID, config.apiKey)
		queryParams.put(OAuthConstants.REDIRECT_URI, config.callback)
		queryParams.put(OAuthConstants.CLIENT_SECRET, config.apiSecret)

		StringBuilder accessTokenEndpoint = new StringBuilder(api.accessTokenEndpoint)
		accessTokenEndpoint.append("?")
		accessTokenEndpoint.append(queryParams.collect { key, value -> "$key=$value" }.join("&"))

		OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), accessTokenEndpoint.toString())

		Response response = request.send()

		String responseBody = response.body

		if (Environment.current == Environment.DEVELOPMENT) {
			println "Received response from moves api: $responseBody"
		}
		JSONObject responseJSON = JSON.parse(responseBody)

		if(!responseJSON.access_token) {
			throw new OAuthException("Response body is incorrect. Can't extract a token from this: $responseBody", null);
		}

		return new Token(responseJSON.access_token, "", responseBody)
	}

}
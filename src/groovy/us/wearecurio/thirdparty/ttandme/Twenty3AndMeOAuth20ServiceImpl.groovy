package us.wearecurio.thirdparty.ttandme

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.builder.api.DefaultApi20
import org.scribe.model.OAuthConfig
import org.scribe.model.OAuthConstants
import org.scribe.model.OAuthRequest
import org.scribe.model.Response
import org.scribe.model.Token
import org.scribe.model.Verifier
import org.scribe.oauth.OAuth20ServiceImpl

class Twenty3AndMeOAuth20ServiceImpl extends OAuth20ServiceImpl {

	DefaultApi20 api
	OAuthConfig config

	Twenty3AndMeOAuth20ServiceImpl(DefaultApi20 api, OAuthConfig config) {
		super(api, config)
		this.api = api
		this.config = config
	}

	Token getAccessToken(Token requestToken, Verifier verifier) {
		OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint())

		Map queryParams = [:]
		queryParams.put(OAuthConstants.CLIENT_ID, config.getApiKey())
		queryParams.put(OAuthConstants.CLIENT_SECRET, config.getApiSecret())
		queryParams.put(OAuthConstants.CODE, verifier.getValue())
		queryParams.put(OAuthConstants.REDIRECT_URI, config.getCallback())
		queryParams.put("grant_type", "authorization_code")
		queryParams.put(OAuthConstants.SCOPE, config.getScope())

		request.addPayload(queryParams.collect { key, value -> "$key=$value" }.join("&"))

		Response response = request.send()

		JSONObject responseJSON = JSON.parse(response.body)

		return new Token(responseJSON.access_token, "", responseJSON.toString())
	}

}

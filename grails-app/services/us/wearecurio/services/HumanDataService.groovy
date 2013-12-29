package us.wearecurio.services

import static us.wearecurio.model.OAuthAccount.*
import grails.converters.JSON
import grails.util.Environment

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount

class HumanDataService {

	static final BASE_URL = "https://api.humanapi.co/v1/human"
	static final String SET_NAME = "moves import"

	def oauthService

	JSONObject doWithURL(Token tokenInstance, String url, String type) {
		Response apiResponse = oauthService.getHumanResource(tokenInstance, url)

		log.debug "Received [$type] response from Human API with code [$apiResponse.code]"
		if (Environment.current == Environment.DEVELOPMENT) {
			log.debug "Received [$type] response from Human API with body [$apiResponse.body]"
		}

		JSON.parse(apiResponse.body)
	}

	JSONObject getUserProfile(OAuthAccount account) {
		getUserProfile(account.tokenInstance)
	}

	JSONObject getUserProfile(Token tokenInstance) {
		doWithURL(tokenInstance, BASE_URL, "User Profile")
	}

}
package us.wearecurio.controller

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User

/**
 * A generic controller to handle all authentication made by oauth plugin.
 * Actions of this controller redirects client browser according to given
 * session. This is useful where authentication may require at any part of
 * the application and needs to redirect again at the same portion.
 */
class AuthenticationController extends SessionController {

	def beforeInterceptor = [action: this.&authRedirect]

	def oauthService	// From OAuth Plugin

	private String provider
	private Token tokenInstance

	private boolean authRedirect() {
		provider = params.provider
		tokenInstance = session[oauthService.findSessionKeyForAccessToken(provider)]
		if (!tokenInstance) {
			log.error "No token found after authentication with [$provider]."
			redirect controller: "login", action: "auth"
			return false
		}
		if (session.returnURIWithToken) {
			log.debug "Redirecting user to [$session.returnURIWithToken]"
			redirect uri: session.returnURIWithToken
			session.returnURIWithToken = null
		}
		return true
	}

	def twenty3andmeAuth() {
		User currentUserInstance = sessionUser()

		if(currentUserInstance) {
			Response apiResponse = oauthService.getTwenty3AndMeResource(tokenInstance, "https://api.23andme.com/1/names/")
			JSONObject parsedResponse = JSON.parse(apiResponse.body)

			if(parsedResponse.id) {
				OAuthAccount.createOrUpdate(OAuthAccount.TWENTY_3_AND_ME_ID, currentUserInstance.id, parsedResponse.id,
						tokenInstance.token, tokenInstance.secret ?: "")
			}
		}
	}

}
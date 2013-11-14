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

	def beforeInterceptor = [action: this.&authRedirect, except: ["authenticateProvider"]]

	def oauthService	// From OAuth Plugin
	def twenty3AndMeDataService

	private String provider
	private Token tokenInstance

	private boolean authRedirect() {
		provider = params.provider
		tokenInstance = session[oauthService.findSessionKeyForAccessToken(provider)]

		if (params.status == "fail" || !tokenInstance) {
			if (params.status == "fail") {
				log.info "User denied to authenticate with [$provider]."
			} else {
				log.error "No token found after authentication with [$provider]."
			}
			if (session.deniedURI) {
				redirect uri: session.deniedURI
				session.deniedURI = null
				return false
			}
			redirect url: toUrl([controller: "login", action: "auth"])
			return false
		}

		return true
	}

	/**
	 * @see Declarative Error handling in http://grails.org/doc/latest/guide/theWebLayer.html#mappingToResponseCodes
	 */
	def authenticateProvider() {
		String provider = request.exception.cause.provider
		String returnURI = request.forwardURI
		if (request.queryString) {
			returnURI += "?" + request.queryString
		}
		session.returnURIWithToken = returnURI.substring(1)	// Removing "/" from beginning since serverURL is configured with "/" at last.
		redirect(url: toUrl(action: "authenticate", controller: "oauth", params: [provider: provider]))	// redirecting to oauth plugin controller
		return
	}

	def twenty3andmeAuth() {
		User currentUserInstance = sessionUser()

		if (currentUserInstance) {
			JSONObject parsedResponse = twenty3AndMeDataService.getUserProfiles(tokenInstance)

			if (parsedResponse.id) {
				OAuthAccount.createOrUpdate(OAuthAccount.TWENTY_3_AND_ME_ID, currentUserInstance.id, parsedResponse.id,
						tokenInstance.token, tokenInstance.secret ?: "")
			}
		}
		if (session.returnURIWithToken) {
			log.debug "Redirecting user to [$session.returnURIWithToken]"
			redirect uri: session.returnURIWithToken
			session.returnURIWithToken = null
		}
		return
	}

}
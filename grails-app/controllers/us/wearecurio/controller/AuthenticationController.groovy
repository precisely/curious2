package us.wearecurio.controller

import static us.wearecurio.model.OAuthAccount.*
import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount

/**
 * A generic controller to handle all authentication made by oauth plugin.
 * Actions of this controller redirects client browser according to given
 * session. This is useful where authentication may require at any part of
 * the application and needs to redirect again at the same portion.
 */
class AuthenticationController extends SessionController {

	def afterInterceptor = [action: this.&afterAuthRedirect, only: ["humanAuth", "twenty3andmeAuth", "fitbitAuth", "movesAuth", "withingsAuth"]]
	def beforeInterceptor = [action: this.&authRedirect, except: ["authenticateProvider", "withingCallback"]]

	def fitBitDataService
	def humanDataService
	def oauthService	// From OAuth Plugin
	def oAuthAccountService
	def twenty3AndMeDataService

	private String provider
	private Token tokenInstance

	private boolean afterAuthRedirect(model) {
		if (session.returnURIWithToken) {
			log.debug "Redirecting user to [$session.returnURIWithToken]"
			redirect uri: session.returnURIWithToken
			session.returnURIWithToken = null
			return false
		}
	}

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
			redirect url: toUrl([controller: "home", action: "login"])
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
		// Redirecting to oauth plugin controller
		redirect(url: toUrl(action: "authenticate", controller: "oauth", params: [provider: provider]))
		return
	}

	def twenty3andmeAuth() {
		JSONObject userInfo = twenty3AndMeDataService.getUserProfiles(tokenInstance)

		oAuthAccountService.createOrUpdate(TWENTY_3_AND_ME_ID, userInfo.id, tokenInstance)
	}

	def fitbitAuth() {
		// Since FitBit doesn't return user info in response to an authentication we explicitly ask for it
		JSONObject userInfo =  fitBitDataService.getUserInfo(tokenInstance)

		oAuthAccountService.createOrUpdate(FITBIT_ID, userInfo.user.encodedId, tokenInstance)
	}

	def humanAuth() {
		JSONObject userInfo = humanDataService.getUserProfile(tokenInstance)

		oAuthAccountService.createOrUpdate(HUMAN_ID,  userInfo.userId, tokenInstance.token, "")
	}

	def movesAuth() {
		// Moves sends user_id while getting the access token.
		JSONObject userInfo = JSON.parse(tokenInstance.rawResponse)

		oAuthAccountService.createOrUpdate(MOVES_ID, userInfo.user_id.toString(), tokenInstance)
	}

	def withingsAuth() {
	}

	def withingCallback(String userid) {
		params.provider = "withings"
		session.withingsUserId = userid
		redirect(url: toUrl(action: "callback", controller: "oauth", params: params))	// redirecting to oauth plugin controller
	}

}
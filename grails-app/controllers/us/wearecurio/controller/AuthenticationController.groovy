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

	private String provider
	private Token tokenInstance

	def afterInterceptor = [action: this.&afterAuthentication, except: ["authenticateProvider", "withingCallback"]]
	def beforeInterceptor = [action: this.&checkAuthentication, except: ["authenticateProvider", "withingCallback"]]

	def fitBitDataService
	def humanDataService
	def OAuthAccountService
	def oauthService	// From OAuth Plugin
	def twenty3AndMeDataService

	private boolean afterAuthentication(model) {
		if (session.returnURIWithToken) {
			log.debug "Redirecting user to [$session.returnURIWithToken]"
			redirect uri: session.returnURIWithToken
			session.returnURIWithToken = null
			return false
		}
	}

	private boolean checkAuthentication() {
		provider = params.provider
		tokenInstance = session[oauthService.findSessionKeyForAccessToken(provider)]

		if (params.status == "fail" || !tokenInstance) {
			log.info "Either user denied or no token found after authentication with [$provider]. Status: [$params.status]"
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

	def fitbitAuth() {
		// Since FitBit doesn't return user info in response to an authentication we explicitly ask for it
		JSONObject userInfo =  fitBitDataService.getUserInfo(tokenInstance)

		OAuthAccountService.createOrUpdate(FITBIT_ID, userInfo.user.encodedId, tokenInstance)
	}

	def humanAuth() {
		JSONObject userInfo = humanDataService.getUserProfile(tokenInstance)

		OAuthAccountService.createOrUpdate(HUMAN_ID,  userInfo.userId, tokenInstance.token, "")
	}

	def movesAuth() {
		// Moves sends user_id while getting the access token.
		JSONObject userInfo = JSON.parse(tokenInstance.rawResponse)

		OAuthAccountService.createOrUpdate(MOVES_ID, userInfo.user_id.toString(), tokenInstance)
	}

	def twenty3andmeAuth() {
		JSONObject userInfo = twenty3AndMeDataService.getUserProfiles(tokenInstance)

		OAuthAccountService.createOrUpdate(TWENTY_3_AND_ME_ID, userInfo.id, tokenInstance)
	}

	/**
	 * Special case for Withings callback, since Withings sends user id as request parameter.
	 */
	def withingCallback(String userid) {
		params.provider = "withings"
		session.withingsUserId = userid
		redirect(url: toUrl(action: "callback", controller: "oauth", params: params))	// redirecting to oauth plugin controller
	}

	def withingsAuth() {
	}

}
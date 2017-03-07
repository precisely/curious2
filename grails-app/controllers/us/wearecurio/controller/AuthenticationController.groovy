package us.wearecurio.controller

import groovyx.net.http.URIBuilder
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Token
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.security.NoAuth
import us.wearecurio.services.OuraDataService

/**
 * A generic controller to handle all authentication made by oauth plugin.
 * Actions of this controller redirects client browser according to given
 * session. This is useful where authentication may require at any part of
 * the application and needs to redirect again at the same portion.
 */
class AuthenticationController extends SessionController {

	private String provider
	private Token tokenInstance

	/**
	 * The session key used to distinguish the reason for which the third party authentication is being performed.
	 */
	static final String AUTH_REASON_KEY = "thirdPartyAuthFor"
	static final Integer SIGN_UP_AUTH = 1
	static final Integer SIGN_IN_AUTH = 2

	def afterInterceptor = [action: this.&afterAuthentication, except: ["authenticateProvider", "withingCallback",
			"thirdPartySignUp", "thirdPartySignIn"]]
	def beforeInterceptor = [action: this.&checkAuthentication, except: ["authenticateProvider", "withingCallback",
			"thirdPartySignUp", "thirdPartySignIn"]]

	def fitBitDataService
	def humanDataService
	def jawboneUpDataService
	def movesDataService
	def oauthAccountService
	def oauthService	// From OAuth Plugin
	def twenty3AndMeDataService
	def withingsDataService
	OuraDataService ouraDataService

	User currentUser
	Long userId

	private boolean afterAuthentication(model) {
		if (session.returnURIWithToken) {
			log.debug "Redirecting user with id: [$userId] to [$session.returnURIWithToken]"
			redirect uri: session.returnURIWithToken
			session.returnURIWithToken = null
			return false
		}
	}

	private boolean checkAuthentication() {
		provider = params.provider
		currentUser = sessionUser()
		tokenInstance = session[oauthService.findSessionKeyForAccessToken(provider)]

		if (params.status == "fail" || !tokenInstance) {
			log.warn "Either user denied or no token found after authentication with [$provider]. Status: [$params.status]"

			if (session.deniedURI) {
				redirect uri: session.deniedURI
				session.deniedURI = null
				return false
			}

			redirect url: toUrl([controller: "home", action: "login"])
			return false
		}

		if (session[AUTH_REASON_KEY] == SIGN_UP_AUTH || session[AUTH_REASON_KEY] == SIGN_IN_AUTH) {
			return true
		}

		if (!currentUser) {
			log.debug "Session expired after callback from $provider's authentication."
			redirect url: toUrl([controller: "home", action: "login"])
			return false
		}
		userId = currentUser.id

		return true
	}


	/**
	 * @see Declarative Error handling in http://grails.org/doc/latest/guide/theWebLayer.html#mappingToResponseCodes
	 */
	@NoAuth
	def authenticateProvider() {
		String provider = request.exception.cause.provider
		String returnURI = request.forwardURI
		if (request.queryString) {
			returnURI += "?" + request.queryString
		}

		session.returnURIWithToken = returnURI.substring(1)	// Removing "/" from beginning since serverURL is configured with "/" at last.
		// Redirecting to oauth plugin controller
		redirect(url: toUrl(action: "authenticate", controller: "oauth", params: [provider: provider.toLowerCase()]))
		return
	}

	def fitbitAuth() {
		// Since FitBit doesn't return user info in response to an authentication we explicitly ask for it
		JSONObject userInfo =  fitBitDataService.getUserProfile(tokenInstance).user
		Integer timeZoneId = TimeZoneId.look(userInfo.timezone).id

		oauthAccountService.createOrUpdate(ThirdParty.FITBIT, userInfo.encodedId, tokenInstance, userId, timeZoneId)
	}

	def twitterAuth() {
		URIBuilder builder = new URIBuilder("https://api.twitter.com?" + tokenInstance.getRawResponse())
		String twitterId = builder.getQuery().user_id
		oauthAccountService.createOrUpdate(ThirdParty.TWITTER, twitterId, tokenInstance, userId)
	}

	def humanAuth() {
		JSONObject userInfo = humanDataService.getUserProfile(tokenInstance)
		Integer timeZoneId = TimeZoneId.look(userInfo.defaultTimeZone.name).id

		oauthAccountService.createOrUpdate(ThirdParty.HUMAN, userInfo.userId, tokenInstance, userId, timeZoneId)
	}

	def jawboneupAuth() {
		JSONObject userInfo = jawboneUpDataService.getUserProfile(tokenInstance)
		String timezone = jawboneUpDataService.getTimeZoneName(tokenInstance)
		Integer timeZoneId = TimeZoneId.look(timezone).id

		oauthAccountService.createOrUpdate(ThirdParty.JAWBONE, userInfo.data.xid, tokenInstance, userId, timeZoneId)
	}

	def movesAuth() {
		JSONObject userInfo = movesDataService.getUserProfile(tokenInstance)
		Integer timeZoneId = TimeZoneId.look(userInfo.profile.currentTimeZone.id).id

		oauthAccountService.createOrUpdate(ThirdParty.MOVES, userInfo.user_id.toString(), tokenInstance, userId, timeZoneId)
	}

	def ouraAuth() {
		JSONObject userInfo = ouraDataService.getUserProfile(tokenInstance)

		// If authentication was for user signup/signin with Oura
		if (session[AUTH_REASON_KEY] == SIGN_UP_AUTH || session[AUTH_REASON_KEY] == SIGN_IN_AUTH) {
			String username = userInfo["username"].toLowerCase()
			User user = User.lookup(username)

			if (user) {
				log.debug "$user associated with Oura"
				securityService.setLoginUser(user)
			} else {
				log.debug "No user found associated with Oura username $userInfo.username"

				if (session[AUTH_REASON_KEY] == SIGN_IN_AUTH) {
					session.returnURIWithToken = null
					flash.message = g.message(code: "thirdparty.account.not.exists")
					redirect(url: toUrl(action: "login", controller: "home"))
					return
				}

				// Adding a random password
				Map signupData = [username: username, email: username, name: "", password:
						new DefaultHashIDGenerator().generate(12)]

				List<String> groups
				// TODO Make this group names DRY with "register.gsp"
				if (urlService.template(request) == "lhp") {
					groups = ["announce", "lhp", "lhp announce"]
				} else {
					groups = ["announce", "curious", "curious announce"]
				}

				user = User.create(signupData, groups)

				if (user.hasErrors()) {
					flash.message = "Error registering the user. Please try again later."
					return
				}

				session.showHelp = true
				securityService.setLoginUser(user)
			}

			userId = user.id
		}

		oauthAccountService.createOrUpdate(ThirdParty.OURA, userInfo['user_id']?.toString(), tokenInstance, userId)
	}

	def twenty3andmeAuth() {
		JSONObject userInfo = twenty3AndMeDataService.getUserProfile(tokenInstance)

		oauthAccountService.createOrUpdate(ThirdParty.TWENTY_THREE_AND_ME, userInfo.id, tokenInstance, userId)
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
		String timezoneFromActivityData = withingsDataService.getUsersTimeZone(tokenInstance, session.withingsUserId)
		Integer timeZoneId = TimeZoneId.look(timezoneFromActivityData).id
		oauthAccountService.createOrUpdate(ThirdParty.WITHINGS, session.withingsUserId, tokenInstance, userId, timeZoneId)
	}

	@NoAuth
	def thirdPartySignUp() {
		handleSignUpSignInAuth(SIGN_UP_AUTH)
	}

	@NoAuth
	def thirdPartySignIn() {
		handleSignUpSignInAuth(SIGN_IN_AUTH)
	}

	private void handleSignUpSignInAuth(Integer type) {
		User user = sessionUser()

		if (user) {
			flash.message = g.message(code: "already.loggedin")
			redirect(url: toUrl(controller: "home", action: "index"))
			return
		}

		session[AUTH_REASON_KEY] = type
		session.returnURIWithToken = "home/index"
		redirect(url: toUrl(action: "authenticate", controller: "oauth", params:
				[provider: params.provider.toLowerCase()]))
	}
}
package us.wearecurio.controller

import org.scribe.model.Token

/**
 * A generic controller to handle all authentication made by oauth plugin.
 * Actions of this controller redirects client browser according to given
 * session. This is useful where authentication may require at any part of
 * the application and needs to redirect again at the same portion.
 */
class AuthenticationController {

	def beforeInterceptor = [action: this.&authRedirect]

	def oauthService	// From OAuth Plugin

	private boolean authRedirect() {
		String provider = params.provider
		Token tokenInstance = session[oauthService.findSessionKeyForAccessToken(provider)]
		if (!tokenInstance) {
			log.error "No token found after authentication with [$provider]."
			redirect controller: "login", action: "auth"
			return false
		}
		if (session.returnURIWithToken) {
			log.debug "Redirecting user to [$session.returnURIWithToken]"
			redirect uri: session.returnURIWithToken
			session.returnURIWithToken = null
			return false
		}
		return true
	}

	def ttandmeAuth() {
	}

}
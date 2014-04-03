package us.wearecurio.services

import javax.servlet.http.HttpServletRequest

import org.springframework.transaction.annotation.Transactional

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsHttpSession
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.web.context.request.RequestContextHolder

import us.wearecurio.model.User
import us.wearecurio.server.Session

class SecurityService {

	static transactional = false

	/**
	 * Checks whether the token in the request is valid.
	 */
	boolean isTokenValid(HttpServletRequest request, params, def session, User user, boolean resetToken = false) {
		if (params.mobileSessionId) {
			User mobileUser = Session.lookupSessionUser(params.mobileSessionId)
			if (mobileUser != null) {
				if (user == null) {
					println "Opening mobile session with user " + user
					session.userId = mobileUser.getId()
					return true
				} else if (user.getId() != mobileUser.getId()) {
					println "Mobile user id " + mobileUser.getId() + " does not conform to current user session with user " + user
					return false
				} else {
					return true
				}
			} else {
				println "Failed to find mobile session with id " + params.mobileSessionId
				return false
			}
		}

		SynchronizerTokensHolder tokenInSession = request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
		String tokenInRequest = params[SynchronizerTokensHolder.TOKEN_KEY]
		String URIInRequest = params[SynchronizerTokensHolder.TOKEN_URI]

		if (!tokenInRequest || !URIInRequest || !tokenInSession) {
			log.debug "Invalid token due to missing parameter or session token"
			return false
		}

		try {
			return tokenInSession.isValid(URIInRequest, tokenInRequest)
		} catch (IllegalArgumentException e) {
			log.debug "Invalid token"
			return false
		} finally {
			if (resetToken) {
				tokenInSession.resetToken(URIInRequest)
			}
		}
	}

	User getCurrentUser() {
		GrailsHttpSession session = RequestContextHolder.requestAttributes.session
		
		if (session.userId) {
			User user = User.get(session.userId)
			log.debug "userId " + session.userId
			session.setMaxInactiveInterval(60*60*24*7) // one week session timeout by default
			return user
		}
		log.debug "No session user"
		return null
	}

}
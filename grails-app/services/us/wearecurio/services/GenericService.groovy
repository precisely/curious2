package us.wearecurio.services

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.transaction.annotation.Transactional

class GenericService {

	static transactional = false

	/**
	 * Checks whether the token in the request is valid.
	 */
	boolean isTokenValid(HttpServletRequest request, params, boolean resetToken = false) {
		SynchronizerTokensHolder tokenInSession = request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
		String tokenInRequest = params[SynchronizerTokensHolder.TOKEN_KEY]
		String URIInRequest = params[SynchronizerTokensHolder.TOKEN_URI]

		if(!tokenInRequest || !URIInRequest || !tokenInSession) {
			log.debug "Invalid token due to missing paramter or session token"
			return false
		}

		try {
			return tokenInSession.isValid(URIInRequest, tokenInRequest)
		} catch(IllegalArgumentException e) {
			log.debug "Invalid token"
			return false
		} finally {
			if(resetToken) {
				tokenInSession.resetToken(URIInRequest)
			}
		}
	}

	@Transactional
	void someTransactionalMethod() {

	}

}
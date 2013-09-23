package us.wearecurio.services

import javax.servlet.http.HttpServletRequest

import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder

class GenericService {

	/**
	 * Checks whether the token in the request is valid.
	 */
	synchronized boolean isTokenValid(HttpServletRequest request, params) {
		SynchronizerTokensHolder tokenInSession = request.getSession(false)?.getAttribute(SynchronizerTokensHolder.HOLDER)
		String tokenInRequest = params[SynchronizerTokensHolder.TOKEN_KEY]
		String URIInRequest = params[SynchronizerTokensHolder.TOKEN_URI]

		if(!tokenInRequest || !URIInRequest || !tokenInSession) {
			return false
		}

		try {
			return tokenInSession.isValid(URIInRequest, tokenInRequest)
		} catch(IllegalArgumentException e) {
			return false
		}
	}

}
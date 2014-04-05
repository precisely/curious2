package us.wearecurio.services

import org.apache.commons.logging.LogFactory

class TokenService {

	private static def log = LogFactory.getLog(this)

	static transactional = false
	
	def acquire(def session, def token) {
		synchronized(session) {
			if (session['tokenKey:' + token])
				return false
			session['tokenKey:' + token] = true
			
			return true
		}
	}
}

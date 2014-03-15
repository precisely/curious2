package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

class UniqueTimedToken {
	private static def log = LogFactory.getLog(this)
	public static final int MAXLENGTH = 100
	
	static constraints = { token(maxSize:MAXLENGTH, unique:true) }
	
	static mapping = {
		table 'unique_timed_token'
		token column:'token', index:'token_idx'
	}
	
	String token
	Date date
	Date expires
	
	public UniqueTimedToken(String token, Date date, long timeout) {
		this.token = token
		this.date = date
		this.expires = new Date(date.getTime() + timeout)
	}
	
	public boolean trySave() {
		return Utils.save(this, true)
	}
	
	public static delete(UniqueTimedToken token) {
		log.debug "UniqueTimedToken.delete(): " + token
		token.delete()
	}
	
	public static clearExpiredTokens() {
		UniqueTimedToken.executeUpdate("delete UniqueTimedToken t where t.expires < :nowTime",
			[nowTime: new Date()])
	}
	
	public updateExpiration(Date now, long timeout) {
		this.date = now
		this.expires = new Date(date.getTime() + timeout)
	}
	
	// acquire lock on token
	public static boolean acquire(String token, Date now, long timeout = 120000) {
		log.debug "Attempt to acquire token " + token + " at " + now
		
		UniqueTimedToken already = UniqueTimedToken.findByToken(token, [lock:true])
		
		if (already) {
			if (now.getTime() - already.getDate().getTime() < timeout) {
				log.debug "Failed to acquire token, existing token still fresh"
				return false
			}
			log.debug "Found expired token, updating"
			
			already.updateExpiration(now, timeout)
			
			boolean retVal = already.trySave()
			
			log.debug "Success of token update: " + retVal
			
			return retVal
		} else {
			log.debug "Attempting to acquire token"

			UniqueTimedToken newToken = new UniqueTimedToken(token, now, timeout)
			
			boolean retVal = newToken.trySave()
			
			log.debug "Success of token acquisition: " + retVal
			
			return retVal
		}
	}
	
	public String toString() {
		return "UniqueTimedToken('" + token +"', " + date + ")"
	}
}

package us.wearecurio.server

import org.apache.commons.logging.LogFactory
import us.wearecurio.services.DatabaseService

import java.text.SimpleDateFormat
import java.text.ParseException
import us.wearecurio.utility.Utils
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

/**
 * Session = class to store manually-created session IDs initially for use in simulating session
 * persistence via HTML5 localStorage. Storing local sessionIDs to re-log-in user after leaving
 * web app context.
 */
class Session {

	private static def log = LogFactory.getLog(this)

	String uuid
	Long userId
	long expires
	boolean disabled

	public static final long EXPIRE_TIME = 60000L * 60L * 24L * 7L // one week

	static constraints = { uuid(maxSize:50) }
	static mapping = {
		version false
		userId column: 'user_id', index: 'user_id_index'
		uuid column:'uuid', index:'uuid_index'
	}

	private static String newUUIDString() {
		def uuid = UUID.randomUUID()
		Long hi = uuid.getMostSignificantBits()
		Long low = uuid.getLeastSignificantBits()

		return hi.toString() + "-" + low.toString()
	}

	public static Session createOrGetSession(User user) {
		log.debug "Session.createOrGetSession() userId:" + user.getId()
		
		def session = Session.findByUserId(user.getId())

		def nowTime = new Date().getTime()

		if (session != null && (!session.isDisabled())) {
			def result = DatabaseService.retry(session) {
				log.debug "Session found for user " + user.getId() + " uuid: " + session.getUuid()
				if (session.getExpires() > nowTime) {
					if (nowTime + EXPIRE_TIME > session.getExpires() + 60000L * 30L) {
						session.setExpires(nowTime + EXPIRE_TIME) // refresh session
						session.save(flush:true)
					}
					return session
				} else {
					log.debug ("Session expired, deleting");
					delete(session)
					return null
				}
			}
			if (result != null)
				return result
		}

		log.debug "Session not found for user " + user.getId()

		session = new Session()
		session.setUuid(newUUIDString())
		session.setExpires(nowTime + EXPIRE_TIME)
		session.setUserId(user.getId())
		
		session.save()

		return session
	}
	
	public static delete(Session session) {
		session.delete(flush: true)
	}

	public static User lookupSessionUser(String sessionId) {
		log.debug "Session.lookupSessionUser() sessionId:" + sessionId
		
		def session = Session.findByUuid(sessionId)

		def nowTime = new Date().getTime()

		if (session != null && (!session.isDisabled())) {
			if (session.getExpires() > nowTime) {
				log.debug "Session found for user " + session.getUserId() + ", still active, expires at " + new Date(session.getExpires())
				return User.get(session.getUserId())
			} else {
				log.debug "Session found for user " + session.getUserId() + ", expired at " + new Date(session.getExpires()) + ", deleting"
				delete(session)
			}
		}

		return null
	}

	public boolean equals(Object other) {
		if (other == null) return false
		return this.uuid == other.uuid && this.userId == other.userId && this.expires == other.expires
	}

	public String fetchUuid() {
		if (disabled) return null
		else return uuid;
	}

	public String toString() {
		return "Session(" + uuid + ", userId:" + userId + ", expires:" + expires + ", disabled:" + disabled + ")"
	}
}

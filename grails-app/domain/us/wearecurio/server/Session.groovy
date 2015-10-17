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

	static final long EXPIRE_TIME = 60000L * 60L * 24L * 7L // one week

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

	static Session createOrGetSession(User user, Date now) {
		log.debug "Session.createOrGetSession() userId:" + user.getId()
		
		def session = Session.findByUserId(user.getId())

		long nowTime = now.getTime()

		if (session != null && (!session.isDisabled())) {
			log.debug "Session found for user " + user.getId() + " uuid: " + session.getUuid()
			if (session.getExpires() > nowTime) {
				if (nowTime + EXPIRE_TIME > session.getExpires() + 60000L * 30L) {
					session.setExpires(nowTime + EXPIRE_TIME) // refresh session
					Utils.save(session, true)
				}
				return session
			} else {
				log.debug ("Session expired, disable");
				session.disabled = true
				Utils.save(session, true)
			}
		}

		log.debug "Session not found or expired for user, create new session " + user.getId()

		session = new Session()
		session.setUuid(newUUIDString())
		session.setExpires(nowTime + EXPIRE_TIME)
		session.setUserId(user.getId())
		
		Utils.save(session, true)

		return session
	}
	
	static delete(Session session) {
		// disable instead of delete
		session.disabled = true
		Utils.save(session)
	}

	static User lookupSessionUser(String sessionId, Date now) {
		log.debug "Session.lookupSessionUser() sessionId:" + sessionId
		
		def session = Session.findByUuid(sessionId)

		def nowTime = now.getTime()

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

	boolean equals(Object other) {
		if (other == null) return false
		return this.uuid == other.uuid && this.userId == other.userId && this.expires == other.expires
	}

	String fetchUuid() {
		if (disabled) return null
		else return uuid;
	}

	String toString() {
		return "Session(" + uuid + ", userId:" + userId + ", expires:" + expires + ", disabled:" + disabled + ")"
	}

	static deleteStale() {
		def c = Session.createCriteria()
		
		Long twoWeeksAgoTicks = new Date(new Date().getTime() - 60000L * 60L * 24L * 14L).getTime()
		
		Session.executeUpdate("delete Session s where s.expires < :twoWeeksAgoTicks",
				[twoWeeksAgoTicks:twoWeeksAgoTicks])
	}
}

package us.wearecurio.controller
import us.wearecurio.model.*

import java.lang.Thread
import grails.util.GrailsNameUtils
import org.apache.commons.logging.LogFactory
import us.wearecurio.server.Session
import us.wearecurio.utility.Utils
import us.wearecurio.services.UrlService

class SessionController {
	UrlService urlService
	def name

	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}
	
	def SessionController() {
		debug "SessionController()"
		name = GrailsNameUtils.getLogicalName(getClass(), "Controller").toLowerCase()
	}
	
	protected def toUrl(map) {
		map.controller = map.controller ? map.controller : name
		return urlService.make(map, request)
	}

	protected def execLogout() {
		debug "SessionController.execLogout()"
		
		setLoginUser(null)
		if (session.mobileSession != null && !session.mobileSession.isDisabled()) {
			session.mobileSession.setDisabled(true)
			Session.delete(session.mobileSession)
		}
		session.mobileSession = null
		session.sessionCache = null
	}

	protected def setLoginUser(user) {
		debug "SessionController.setLoginUser()"

		session.tags = null
		if (user == null) {
			debug "Logoff: login user cleared"
			session.userId = null
			clearTags()
		} else {
			debug "Login: setting login user " + user.getId()
			session.userId = user.getId()
		}
		session.setMaxInactiveInterval(60*60*24*7) // one week session timeout by default
	}

	protected def sessionUser() {
		debug "SessionController.sessionUser()"

		if (session.userId != null) {
			def user = User.get(session.userId)
			debug "userId " + session.userId
			session.setMaxInactiveInterval(60*60*24*7) // one week session timeout by default
			return user
		}
		
		debug "no session user"

		return null
	}
	
	public static class SessionCacheValue {
		def value
		def expiresTime
		
		public SessionCacheValue(value, long expiresTimeMillis) {
			this.value = value
			this.expiresTime = expiresTimeMillis
		}
	}
	
	protected def setSessionCache(String key, value, int expiresSeconds = 600) {
		if (!session.sessionCache) {
			session.sessionCache = [:]
		}
		
		session.sessionCache[key] =
			new SessionCacheValue(value, new Date().getTime() + 1000L * expiresSeconds)
				
		return value
	}

	protected def getSessionCache(String key, Closure newValueClosure, int expiresSeconds = 600) {
		debug "SessionController.getSessionCache() key:" + key

		if (!session.sessionCache) {
			session.sessionCache = [:]
		}
		def value = session.sessionCache[key]
		if (value) {
			long now = new Date().getTime()
			if (value.getExpiresTime() < now) { // value expired
				debug "getSessionCache expired at " + now + " > " + value.getExpiresTime()
				return setSessionCache(key, newValueClosure(), expiresSeconds)
			}

			def retVal = value.getValue()
			debug "Called closure to get value: " + retVal
			
			return retVal
		}
		
		def newValue = newValueClosure()
		
		debug "Setting value to:" + newValue
		
		return setSessionCache(key, newValue, expiresSeconds)
	}

	protected def clearTags() {
		debug "SessionController.clearTags()"
		
		session.tags = null
	}
	
	protected def userFromId(Long userId) {
		User sessionUser = sessionUser()
		if (sessionUser != null && sessionUser.getId() == userId)
			return sessionUser
		return null
	}

	protected def userFromIdStr(String userIdStr) {
		if (userIdStr == "undefined") {
			return sessionUser();
		}
		return userFromId(Long.parseLong(userIdStr))
	}

	def static parseLong(numStr) {
		return Long.parseLong(numStr)
	}

	protected def usersFromIds(def userIds) {
		def users = []
		for (userId in userIds) {
			users.add(userFromId(userId))
		}
		return users
	}

	protected def parseDate(String dateStr) {
		return Date.parse("EEE, dd MMM yyyy HH:mm:ss z", dateStr)
	}

	protected def name() {
		return name
	}
}

package us.wearecurio.controller

import java.text.SimpleDateFormat;

import grails.util.GrailsNameUtils

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.User
import us.wearecurio.server.Session
import us.wearecurio.services.SecurityService
import us.wearecurio.services.UrlService
import us.wearecurio.services.SecurityService.AuthenticationStatus

class SessionController {
	
	SimpleDateFormat systemFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

	SecurityService securityService
	UrlService urlService
	def name

	private static def log = LogFactory.getLog(this)
	
	static final String AUTH_ERROR_MESSAGE = 'You have logged into a different account in another window. Please refresh the browser window.'
	static final String SYNTAX_ERROR_MESSAGE = 'Internal error in application. Please update your application or contact precise.ly.'

	static debug(str) {
		log.debug(str)
	}
	
	def SessionController() {
		debug "SessionController()"
		systemFormat.setTimeZone(TimeZone.getDefault())
		name = GrailsNameUtils.getLogicalName(getClass(), "Controller").toLowerCase()
	}
	
	protected def toUrl(map) {
		map.controller = map.controller ? map.controller : name
		return urlService.make(map, request)
	}
	
	protected def execLogin() {
		securityService.login(session.username, session.password)
	}

	protected def execLogout() {
		debug "SessionController.execLogout()"
		
		securityService.logout()
	}

	protected User sessionUser() {
		debug "SessionController.sessionUser()"
		return securityService.currentUser
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
		debug "clearTags()"
		
		securityService.clearTags()
	}
	
	protected User userFromId(Long userId) {
		return authFromUserId(userId).user
	}

	protected AuthenticationStatus authFromUserId(Long userId) {
		return securityService.authFromUserId(userId)
	}

	protected def userFromIdStr(String userIdStr) {
		if (userIdStr == "undefined") {
			return sessionUser()
		}
		if (!userIdStr)
			return null
		try {
			return userFromId(Long.parseLong(userIdStr))
		} catch (NumberFormatException e) {
			log.error("userIdStr passed in has a format exception ")
			e.printStackTrace()
			return null
		}
	}

	protected def authFromUserIdStr(String userIdStr) {
		if (userIdStr == "undefined") {
			return authFromUserId(sessionUser()?.id)
		}
		if (!userIdStr)
			return authFromUserId(null)
		try {
			return authFromUserId(Long.parseLong(userIdStr))
		} catch (NumberFormatException e) {
			log.error("userIdStr passed in has a format exception ")
			e.printStackTrace()
			return authFromUserId(null)
		}
	}

	static Long parseLong(numStr) {
		if (!numStr) return null
		return Long.parseLong(numStr)
	}

	protected def usersFromIds(def userIds) {
		def users = []
		for (userId in userIds) {
			users.add(userFromId(userId))
		}
		return users
	}

	static Date parseDate(String dateStr) {
		if (dateStr == null) return null
		return Date.parse("EEE, dd MMM yyyy HH:mm:ss z", dateStr)
	}

	protected def name() {
		return name
	}
}

package us.wearecurio.services

import grails.converters.JSON

import javax.servlet.http.HttpServletRequest

import org.springframework.transaction.annotation.Transactional
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsHttpSession
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.web.context.request.RequestContextHolder

import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.server.Session
import grails.compiler.GrailsTypeChecked

class SecurityService {

	static transactional = true
	
	static SecurityService service
	
	public static def set(s) { service = s }

	public static SecurityService get() { return service }
		
	// list of actions that should be cross-site checked
	static def csrfActions = [
		"addEntrySData",
		"listTagsAndTagGroups",
		"autocompleteData",
		"getListData",
		"getEntriesData",
		"getTagProperties",
		"getPlotData",
		"getSumPlotData",
		"createTagGroup",
		"addTagGroupToTagGroup",
		"addTagToTagGroup",
		"removeTagGroupFromTagGroup",
		"removeTagFromTagGroup",
		"showTagGroup",
		"deleteTagGroup",
		"setTagPropertiesData",
		"updateEntrySData",
		"deleteEntrySData",
		"activateGhostEntryData",
		"deleteGhostEntryData",
		"activateGhostEntry",
		"deleteGhostEntry",
		"registerForPushNotificationData",
		"registerForPushNotification",
		"unregisterPushNotificationData",
		"unregisterPushNotification"
	] as Set

	// list of actions that are allowed without authentication
	static def noauthActions = [
		'login',
		'authenticateProvider',
		'dologin',
		'register',
		'doregister',
		'forgot',
		'doforgot',
		'doforgotData',
		'dologinData',
		'doregisterData',
		'discuss',
		'loadSnapshotDataId',
		'recover',
		'dorecover',
		'notifywithings',
		'termsofservice',
		'notifyfitbit',
		'homepage'
	] as Set

	/**
	 * Checks whether the CSRF token in the request is valid.
	 */
	protected boolean checkCSRF(HttpServletRequest request, params, def session, def user, boolean resetToken = false) {
		if (params.mobileSessionId) {
			User mobileUser = Session.lookupSessionUser(params.mobileSessionId)
			if (mobileUser != null) {
				if (user == null) {
					println "Opening mobile session with user " + mobileUser
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

	protected def checkLogin(actionName, HttpServletRequest request, params, flash, def session) {
		println "login security filter: " + actionName
		if (params.mobileSessionId != null)
			params.persistentSessionId = params.mobileSessionId
		if (!session.userId && !noauthActions.contains(actionName)) {
			if (params.persistentSessionId != null) {
				User user = Session.lookupSessionUser(params.persistentSessionId)
				if (user != null) {
					println "Opening persistent session with user " + user
					session.userId = user.getId()
					return [true, user]
				} else {
					println "Failed to find persistent session with id " + params.persistentSessionId
				}
			} else {
				println "No persistent session id"
			}
			return [false, null]
		}
		if (params.survey) {
			session.survey = params.survey
		}
		return [true, null]
	}

	/**
	 * checks login and CSRF token
	 * TODO: Write test for this method
	 */
	boolean isAuthorized(actionName, HttpServletRequest request, params, flash, def session) {
		def (boolean authorized, User user) = checkLogin(actionName, request, params, flash, session)
		if (!authorized)
			return false
		if (csrfActions.contains(actionName)) {
			if (!checkCSRF(request, params, session, user))
				return false
		}
		
		return true
	}
	
	Long getCurrentUserId() {
		GrailsHttpSession session = RequestContextHolder.requestAttributes.session
		
		return session.userId
	}
	
	User getCurrentUser() {
		GrailsHttpSession session = RequestContextHolder.requestAttributes.session
		
		Long userId = getCurrentUserId()
		
		if (userId) {
			User user = User.get(userId)
			log.debug "userId " + userId
			session.setMaxInactiveInterval(60*60*24*7) // one week session timeout by default
			if (session.persistentSession != null && !session.persistentSession.isDisabled()) {
				if (session.persistentSession.getUserId() != user.getId()) {
					session.persistentSession = Session.createOrGetSession(user)
					return user
				}
			}
			session.persistentSession = Session.createOrGetSession(user)
			return user
		}
		log.debug "No session user"
		return null
	}
	
	/**
	 * Returns true if given userId is writable by currently logged-in user
	 * 
	 * At present, this is true if userId == currently logged in user id or
	 * if userId is a virtualUserId of a sprint that is writable by the currently logged-in user
	 * 
	 * @param userId
	 * @return
	 */
	boolean userIdIsAccessible(Long userId) {
		Long currentUserId = getCurrentUserId()
		
		if (!currentUserId) return false
		
		if (userId == currentUserId) return true
		
		Sprint sprint = Sprint.findByVirtualUserId(userId)
		
		if (sprint) {
			return sprint.hasWriter(userId)
		}
		
		return false
	}

	protected def clearTags() {
		log.debug "clearTags()"
		
		GrailsHttpSession session = RequestContextHolder.requestAttributes.session
		
		session.tags = null
	}
	
	User login(String username, String password) {
		log.debug "login() username: " + username
		
		username = username.toLowerCase()
		
		boolean authorized = false
		
		def user = User.findByUsername(username)
		if (user != null) {
			// if admin sets the password to '', let the next person to log in set the password for
			// the account (should never be used in production, only for local sandboxes)
			if (user.getPassword().length() == 0) {
				log.debug "password cleared by admin: reset password: WARNING SHOULD NEVER OCCUR ON PRODUCTION SYSTEM"
				user.encodePassword(password)
				authorized = true
			} else if (user.checkPassword(password)) {
				authorized = true
			}
		}
		if (authorized) {
			log.debug "auth success user:" + user
			setLoginUser(user)
			return user
		} else {
			log.debug "auth failure"
			return null
		}
	}
	
	def setLoginUser(user) {
		log.debug "setLoginUser() " + user

		GrailsHttpSession session = RequestContextHolder.requestAttributes.session
		
		if (user != null) {
			Session persistentSession = Session.createOrGetSession(user)
			log.debug "Login: persistent session " + persistentSession.fetchUuid()
			session.persistentSession = persistentSession
		}
		session.tags = null
		if (user == null) {
			log.debug "Logoff: login user cleared"
			session.userId = null
			clearTags()
		} else {
			log.debug "Login: setting login user " + user.getId()
			session.userId = user.getId()
		}
		session.setMaxInactiveInterval(60*60*24*7) // one week session timeout by default
	}
	
	def logout() {
		GrailsHttpSession session = RequestContextHolder.requestAttributes.session
		
		setLoginUser(null)
		if (session.persistentSession != null && !session.persistentSession.isDisabled()) {
			session.persistentSession.setDisabled(true)
			Session.delete(session.persistentSession)
		}
		session.persistentSession = null
		session.sessionCache = null

	}
}
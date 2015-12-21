package us.wearecurio.services

import grails.converters.JSON
import us.wearecurio.security.NoAuth

import javax.servlet.http.HttpServletRequest

import org.springframework.transaction.annotation.Transactional
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsHttpSession
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.springframework.web.context.request.RequestContextHolder

import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.server.Session
import grails.compiler.GrailsTypeChecked

import org.apache.commons.logging.LogFactory

import java.lang.reflect.Method

class SecurityService {

	static transactional = true
	
	private static def log = LogFactory.getLog(this)
	
	static SecurityService service
	DefaultGrailsApplication grailsApplication

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
		"deleteGhostEntryData",
		"deleteGhostEntry",
		"registerForPushNotificationData",
		"registerForPushNotification",
		"unregisterPushNotificationData",
		"unregisterPushNotification"
	] as Set

	// list of actions that are allowed without authentication, this will be populated by populateNoAuthMethods action which is being called in Bootstrap.groovy
	static Map noauthActions = [:]

	/**
	 * Checks whether the CSRF token in the request is valid.
	 */
	protected boolean checkCSRF(HttpServletRequest request, params, def session, def user, boolean resetToken = false) {
		log.debug "checkCSRF() params:" + params
		if (params.mobileSessionId) {
			User mobileUser = Session.lookupSessionUser(params.mobileSessionId, new Date())
			if (mobileUser != null) {
				if (user == null) {
					log.debug "Opening mobile session with user " + mobileUser
					session.userId = mobileUser.getId()
					return true
				} else if (user.getId() != mobileUser.getId()) {
					log.debug "Mobile user id " + mobileUser.getId() + " does not conform to current user session with user " + user
					return false
				} else {
					return true
				}
			} else {
				log.debug "Failed to find mobile session with id " + params.mobileSessionId
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
		log.debug "login security filter: " + actionName
		if (params.mobileSessionId != null)
			params.persistentSessionId = params.mobileSessionId
		if (!session.userId && (!noauthActions[params.controller] == '*' || !noauthActions[params.controller]?.contains(actionName))) {
			if (params.persistentSessionId != null) {
				User user = Session.lookupSessionUser(params.persistentSessionId, new Date())
				if (user != null) {
					log.debug "Opening persistent session with user " + user
					session.userId = user.getId()
					return [true, user]
				} else {
					log.debug "Failed to find persistent session with id " + params.persistentSessionId
				}
			} else {
				log.debug "No persistent session id"
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
		log.debug "isAuthorized() actionName:" + actionName
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
		
		Date now = new Date()
		
		Long userId = getCurrentUserId()
		
		if (userId) {
			User user = User.get(userId)
			log.debug "userId " + userId
			session.setMaxInactiveInterval(60*60*24*7) // one week session timeout by default
			if (session.persistentSession != null && !session.persistentSession.isDisabled()) {
				if (session.persistentSession.getUserId() != user.getId()) {
					session.persistentSession = Session.createOrGetSession(user, now)
					return user
				}
			}
			session.persistentSession = Session.createOrGetSession(user, now)
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
		return authFromUserId(userId) ? true : false
	}
	
	// In the future, may return more complex authorization levels
	static class AuthenticationStatus {
		boolean authorized
		boolean primary
		Sprint sprint
		Long userId
		User user
		
		User getUser() {
			if (user) return user
			if (userId) {
				user = User.get(userId)
				return user
			}
			return null
		}
	}

	AuthenticationStatus authFromUserId(Long userId) {
		Long currentUserId = getCurrentUserId()
		
		if (!currentUserId) return new AuthenticationStatus(authorized:false, primary:false)
		
		if (userId == currentUserId) return new AuthenticationStatus(authorized:true, primary:true, userId:userId)
		
		Sprint sprint = Sprint.findByVirtualUserId(userId)
		
		if (sprint) {
			if (sprint.hasWriter(userId) || sprint.hasAdmin(userId)) {
				return new AuthenticationStatus(authorized:true, primary:false, sprint:sprint, userId:userId)
			}
		}
		
		return new AuthenticationStatus(authorized:false, primary:false)
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
		
		Date now = new Date()
		
		if (user != null) {
			Session persistentSession = Session.createOrGetSession(user, now)
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
		log.debug "logout()"
		GrailsHttpSession session = RequestContextHolder.requestAttributes.session
		
		setLoginUser(null)
		if (session.persistentSession != null && !session.persistentSession.isDisabled()) {
			session.persistentSession.setDisabled(true)
			Session.delete(session.persistentSession)
		}
		session.persistentSession = null
		session.sessionCache = null
	}

	/*
	 * This method iterates over all the controllersandmethods to search for 
	 * NoAuth annotation and populates noauthActions list based on above
	 * search
	 */
	void populateNoAuthMethods() {
		grailsApplication.controllerClasses.each { controllerArtefact ->
			Class controllerClass = controllerArtefact.getClazz()
			String controllerName = controllerArtefact.getLogicalPropertyName();
			if (controllerClass.isAnnotationPresent(NoAuth)) {
				noauthActions[controllerName] = '*'
			}

			controllerClass.methods.each { Method method ->
				if (method.isAnnotationPresent(NoAuth)) {
					if (noauthActions[controllerName] && !(noauthActions[controllerName] == '*')) {
						noauthActions[controllerName].push(method.name)
					} else {
						noauthActions[controllerName] = [method.name]
					}
				}
			}
		}
		log.debug "No auth actions map: ${noauthActions.dump()}"
	}
}

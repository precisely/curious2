import grails.converters.JSON
import us.wearecurio.model.User
import us.wearecurio.server.Session
import us.wearecurio.services.UrlService
import org.codehaus.groovy.grails.web.util.WebUtils
import us.wearecurio.controller.HomeController

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * SecurityFilters used for authentication: servlet sessions and mobile persistent HTML5 localStorage-sessions
 * 
 * @author mitsu
 */
class SecurityFilters {
	UrlService urlService
	
	static List noauthActions = [
		'login',
		'authenticate',	/** For /oauth/$provider/authenticate OAuth Plugin	*/
		'callback',	/** For /oauth/$provider/callback of OAuth Plugin	*/
		'ttandme',	/** For /oauth/ttandme/authenticate OAuth Plugin	*/
		'ttandmeAuth',	/** For /authenticate/ttandmeAuth	*/
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
		'notifyfitbit'
	]
	def filters = {
		loginCheck(controller:'home', action:'*') {
			before = {
				def a = actionName
				if (params.controller == null) {
					flash.precontroller = 'home'
					flash.preaction = 'index'
					flash.parm = ''
					redirect(url:urlService.base(request) + 'home/login')
					return true
				}
				if(!session.userId && !noauthActions.contains(actionName)) {
					if (actionName.endsWith('Data')) {
						println "No session for data action " + actionName
						render "${params.callback}('login')"
					} else {
						def parm = params.clone()
						parm.remove('action')
						parm.remove('controller')
						flash.precontroller = params.controller
						flash.preaction = actionName
						flash.parm = new JSON(parm).toString()
						redirect(url:urlService.base(request) + params.controller + '/login')
					}
					return false
				}
			}
		}
		mobileCheck(controller:'mobile', action:'*') {
			before = {
				if (params.controller == null && (!params.action.equals('index'))) {
					flash.precontroller = 'mobile'
					flash.preaction = 'index'
					flash.parm = new JSON(params).toString()
					println "serverURL:" + UrlService.base(request)
					redirect(url:urlService.base(request) + 'mobile/index')
					return true
				}
			}
		}
		mobiledataCheck(controller:'mobiledata', action:'*') {
			before = {
				if(!session.userId && !noauthActions.contains(actionName)) {
					if (params.mobileSessionId != null) {
						User user = Session.lookupSessionUser(params.mobileSessionId)
						if (user != null) {
							println "Opening mobile session with user " + user
							session.userId = user.getId()
							return true
						} else {
							println "Failed to find mobile session with id " + params.mobileSessionId
						}
					} else {
						println "No mobile session id"
					}
					if (actionName.endsWith('Data')) {
						println "Attempting to call " + actionName + " from mobile controller, fail"
						render "${params.callback}('login')"
					} else {
						flash.precontroller = params.controller
						flash.preaction = actionName
						flash.parm = new JSON(params).toString()
						println "Attempting to redirect to login page"
						redirect(url:urlService.base(request) + params.controller + '/login')
					}
					return false
				}
			}
		}
		trialCheck(controller:'*', action:'*') {
			before = {
				if (params.controller == null) {
					flash.precontroller = UrlService.template(request).equals("lhp") ? 'home' : 'trial'
					flash.preaction = 'index'
					flash.parm = new JSON(params).toString()
					redirect(url:urlService.base(request) + (urlService.template(request).equals("lhp") ? 'home/login' : 'trial/login'))
					return true
				}
				if (params.controller.equals('mobile') || params.controller.equals('mobiledata'))
					return true
				if(!session.userId && !noauthActions.contains(actionName)) {
					println "Redirecting to login page"
					if (actionName.endsWith('Data')) {
						render "${params.callback}('login')"
					} else {
						flash.precontroller = params.controller
						flash.preaction = actionName
						flash.parm = new JSON(params).toString()
						redirect(url:urlService.base(request) + params.controller + '/login')
					}
					return false
				}
			}
		}
	}
}

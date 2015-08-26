import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsApplication
import us.wearecurio.model.UserGroup
import us.wearecurio.services.SecurityService
import us.wearecurio.services.TokenService
import us.wearecurio.services.UrlService
import org.apache.commons.logging.LogFactory

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
	private static def log = LogFactory.getLog(this)
	
	UrlService urlService
	TokenService tokenService
	SecurityService securityService
	GrailsApplication grailsApplication
	
	// list of actions that do not need to be duplicate checked
	static def idempotentActions = [
		'getPeopleData',
		'getEntriesData',
		'getListData',
		'getPlotData',
		'getSumPlotData',
		'getPlotDescData',
		'getSumPlotDescData',
		'getTagsData',
		'setTagPropertiesData',
		'autocompleteData',
		'listPlotData',
		'loadPlotDataId',
		'listSnapshotData',
		'listDiscussionData',
		'loadSnapshotDataId',
		'getTagProperties',
		'createDiscussionData'
	] as Set

	def filters = {
		preCheck(controller:'(home|tag|mobiledata|data|correlation|search)', action:'*') {
			before = {
				def a = actionName
				if (params.controller == null) {
					flash.precontroller = 'home'
					flash.preaction = 'index'
					flash.parm = ''
					redirect(url:urlService.base(request) + 'home/login')
					return true
				}
				if (!actionName)
					return false
				if (!securityService.isAuthorized(actionName, request, params, flash, session)) {
					if (actionName.endsWith('Data') || actionName.endsWith('DataId')) {
						log.debug "Unauthorized data action " + actionName
						render "${params.callback}('login')"
					} else {
						def parm = params.clone()
						parm.remove('action')
						parm.remove('controller')
						flash.precontroller = params.controller
						flash.preaction = actionName
						flash.parm = new JSON(parm).toString()
						redirect(url:urlService.base(request) + 'home/login')
					}
					return false
				}
				return true
			}
		}
		apiFilter(controller: '(sprint|user|discussion|discussionPost)', action: '*') {
			before = {
				if (!securityService.isAuthorized(actionName, request, params, flash, session)) {
					log.debug "Unauthorized data action " + actionName
					render "${params.callback}('login')"
					return false
				}
				return true
			}
		}
		duplicateCheck(controller:'*', action:'*') {
			before = {
				if (actionName && (actionName.endsWith('Data') || actionName.endsWith('DataId') || request.forwardURI.contains('/api/')) && !idempotentActions.contains(actionName)) {
					log.debug "duplicate filter: " + actionName
					def p = new TreeMap(params)
					if (params.date || params.dateToken || params.currentTime) {
						p.remove('_')
						p.remove('callback')
						
						def token = p.toString()
		
						if (!tokenService.acquire(session, token)) {
							log.debug "token not acquired"
							render "${params.callback}('refresh')"
							return false
						} else {
							log.debug "token acquired"
							return true
						}
					}
				} else
					return true
			}
		}
		mobileCheck(controller:'mobile', action:'*') {
			before = {
				log.debug "mobile security filter: " + actionName
				if (params.controller == null && (!params.action.equals('index'))) {
					flash.precontroller = 'mobile'
					flash.preaction = 'index'
					flash.parm = new JSON(params).toString()
					log.debug "serverURL:" + UrlService.base(request)
					redirect(url:urlService.base(request) + 'mobile/index')
					return true
				}
			}
		}
		analyticsTaskCheck(controller:'analyticsTask', action:'*') {
			before = {
				log.debug "Checking analytics task"
				def a = actionName
				if (!actionName)
					return false
				String adminKey
				try {
					adminKey = grailsApplication.config.wearecurious.adminKey
				} catch (Throwable t) {
					log.debug "No admin key in config file"
					adminKey = null
				}
				if (!adminKey) {
					grailsApplication.config.wearecurious = [:]
					grailsApplication.config.wearecurious.adminKey = "nethgoau9er8gih5q78q9u3iyq84f98q38gq7t9qthqethqtj" // only used for dev deploys
					adminKey = grailsApplication.config.wearecurious.adminKey
				}
				if (adminKey != null && params.key && params.key == adminKey) {
					log.debug "Admin key does match"
					return true
				}
				log.debug "Admin key does not match"
				return false
			}
		}
		adminPages(controller: "(admin|sharedTagGroup|userGroup)") {
			before = {
				def a = actionName
				if (params.controller == null) {
					flash.precontroller = 'home'
					flash.preaction = 'index'
					flash.parm = ''
					redirect(url:urlService.base(request) + 'home/login')
					return true
				}
				if (!actionName)
					return false
				if ((!securityService.isAuthorized(actionName, request, params, flash, session))
						|| (!UserGroup.hasAdmin(UserGroup.lookup(UserGroup.SYSTEM_USER_GROUP_NAME).id, session.userId))) {
					log.debug "Unauthorized admin page data action " + actionName
					if (actionName.endsWith('Data') || actionName.endsWith('DataId')) {
						render "${params.callback}('login')"
					} else {
						def parm = params.clone()
						parm.remove('action')
						parm.remove('controller')
						flash.precontroller = params.controller
						flash.preaction = actionName
						flash.parm = new JSON(parm).toString()
						redirect(url:urlService.base(request) + 'home/login')
					}
					log.debug "Authorized admin page action"
					return false
				}
				return true
			}
		}
		/* trialCheck(controller:'trial', action:'*') {
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
		} */
	}
}

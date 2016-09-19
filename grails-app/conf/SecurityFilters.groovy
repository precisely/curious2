import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsApplication
import us.wearecurio.model.UserGroup
import us.wearecurio.services.SecurityService
import us.wearecurio.services.TokenService
import us.wearecurio.services.UrlService
import us.wearecurio.utility.Utils
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
		'getTagProperties'
	] as Set

	def filters = {
		preCheck(controller:'(home|tag|mobiledata|data|correlation|search)', action:'*') {
			before = {
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
						render text: "${params.callback}('login')", contentType: "application/javascript"
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
					render text: "${params.callback}('login')", contentType: "application/javascript"
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
					if (params.date || params.dateToken || params.currentTime || params.currentDate) {
						p.remove('_')
						p.remove('callback')
						
						def token = p.toString()
		
						if (!tokenService.acquire(session, token)) {
							log.debug "token not acquired"
							render text: "${params.callback}('refresh')", contentType: "application/javascript"
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
		adminPages(controller: "(admin|sharedTagGroup|userGroup|analyticsTask)", action:'*') {
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
				if (params.controller == 'analyticsTask') {
					log.debug "Checking analytics task"
					String adminKey
					try {
						adminKey = grailsApplication.config.wearecurious.adminKey
					} catch (Throwable t) {
						log.debug "No admin key in config file"
						Utils.reportError("SecurityFilters: no admin key in config file", "")
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
				}
				if ((!securityService.isAuthorized(actionName, request, params, flash, session))
						|| (!UserGroup.hasAdmin(UserGroup.lookup(UserGroup.SYSTEM_USER_GROUP_NAME).id, session.userId))) {
					log.debug "Unauthorized admin page data action " + actionName
					if (actionName.endsWith('Data') || actionName.endsWith('DataId')) {
						render text: "${params.callback}('login')", contentType: "application/javascript"
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
	}
}

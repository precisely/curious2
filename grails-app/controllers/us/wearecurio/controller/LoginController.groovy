package us.wearecurio.controller

import grails.converters.*
import us.wearecurio.model.*
import grails.util.GrailsNameUtils
import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

/**
 * LoginController
 * 
 * @author mitsu
 */
class LoginController extends SessionController {

	def beforeInterceptor = [action: this.&validateToken, only: [/*"getPeopleData", */"addEntrySData", "listTagsAndTagGroups",
		"autocompleteData", "getListData", "getEntriesData", "getTagProperties", "getPlotData", "getSumPlotData", "createTagGroup", "addTagGroupToTagGroup",
		"addTagToTagGroup", "removeTagGroupFromTagGroup", "removeTagFromTagGroup", "showTagGroup", "deleteTagGroup",
		"setTagPropertiesData", "updateEntrySData", "deleteEntrySData", "activateGhostEntry", "deleteGhostEntry"]]

	private boolean validateToken() {
		if (!securityService.isTokenValid(request, params, session, sessionUser())) {
			response.sendError 401
			return false
		}
		return true
	}

	static debug(str) {
		log.debug(str)
	}
	
	def LoginController() {
	}

	protected def forgotView() {
		return "/" + name() + "/forgot"
	}
	protected def registerView() {
		return "/" + name() + "/register"
	}
	protected def loginView() {
		return "/" + name() + "/login"
	}
	protected def name() {
		return name;
	}
	protected User execLogin() {
		debug "LoginController.execLogin()"
		
		params.username = params.username.toLowerCase()
		
		boolean authorized = false
		
		def user = User.findByUsername(params.username)
		if (user != null) {
			// if admin sets the password to '', let the next person to log in set the password for
			// the account (should never be used in production, only for local sandboxes)
			if (user.getPassword().length() == 0) {
				debug "password cleared by admin: reset password: WARNING SHOULD NEVER OCCUR ON PRODUCTION SYSTEM"
				user.encodePassword(params.password)
				authorized = true
			} else if (user.checkPassword(params.password)) {
				authorized = true
			}
		}
		if (authorized) {
			debug "auth success user:" + user
			setLoginUser(user)
			return user
		} else {
			debug "auth failure"
			return null
		}
	}
	protected def renderJSONPost(data) {
		render "${new JSON(data)}"
	}
	protected def renderJSONGet(data) {
		//debug "${params.callback}(${new JSON(data)})"
		render "${params.callback}(${new JSON(data)})"
	}
	protected def renderDataGet(data) {
		render "${params.callback}(${data})"
	}
	protected def renderStringGet(str) {
		def repl = str.replaceAll("'", "\\\\'")
		render "${params.callback}('${repl}')"
	}
	protected def renderStringPost(str) {
		def repl = str.replaceAll("'", "\\\\'")
		render "'${repl}'"
	}
	def login() {
		debug "LoginController.login()"
		
		def message = flash.message
		
		execLogout()
		render(view:loginView(),
				model:[precontroller:flash.precontroller ?: name(), preaction:flash.preaction ?: 'index', parm:flash.parm ?: [:], message:flash.message, templateVer:urlService.template(request)])
	}
	def dologin() {
		debug "LoginController.dologin()"
		
		def user = execLogin()
		def parm = params.parm
		if (user) {
			flash.message = ""
			redirect(url:toUrl(controller:params.precontroller, action:params.preaction, params:params.parm ? JSON.parse(params.parm) : [:]))
			return
		} else {
			flash.message = "Wrong user name or password. Please try again."
			flash.precontroller = params.precontroller
			flash.preaction = params.preaction
			flash.parm = params.parm
			redirect(url:toUrl(controller:"home", action:"login"))
			return
		}
	}
	def dologinData() {
		debug "LoginController.dologinData()"
		
		def user = execLogin()
		if (user) {
			debug "auth success user:" + user
			renderJSONGet([user:user,success:true])
			return
		} else {
			debug "auth failure"
			renderJSONGet([success:false])
			return
		}
	}
	def forgot() {
		debug "LoginController.forgot()"

		render(view:forgotView(),
				model:[precontroller:params.precontroller, preaction:params.preaction])
	}
	protected static final int FORGOT_SUCCESS = 0
	protected static final int FORGOT_ERROR_EMAIL_NOT_RECOGNIZED = 1

	protected def execForgot(def user) {
		debug "Sending password recovery email for: " + user.getEmail()
		
		def recovery = PasswordRecovery.create(user.getId())
		
		def recoveryLink = toUrl(controller:'home', action:'recover', params:[code:recovery.getCode()])
		
		sendMail {
			to user.getEmail()
			from "contact@wearecurio.us"
			subject "Password reset instructions"
			body "Click here to reset the password for username '" + user.getUsername() + "': " + recoveryLink
		}
		
		debug "Password recovery link:" + recoveryLink
		
		return FORGOT_SUCCESS
	}
	
	protected def execForgotEmail(String email) {
		email = email?.toLowerCase()
		def user = User.findByEmail(email)
		if (user == null) {
			debug "Error recovering user email '" + email + "' not found'"
			return FORGOT_ERROR_EMAIL_NOT_RECOGNIZED
		}
		return execForgot(user)
	}
	protected def execForgotUsername(String username) {
		username = username?.toLowerCase()
		def user = User.findByUsername(username)
		if (user == null) {
			debug "Error recovering user name '" + username + "' not found'"
			return FORGOT_ERROR_EMAIL_NOT_RECOGNIZED
		}
		return execForgot(user)
	}
	def doforgot() {
		debug "LoginController.doforgot()"
		
		if (params.cancel) {
			debug "cancel"
			redirect(url:toUrl(controller:params.precontroller ?: name(), action:params.preaction ?: 'login'))
			return
		}
		if (params.email && execForgotEmail(params.email) == FORGOT_SUCCESS) {
			flash.message = "Password recovery email sent; please check your email"
			
			redirect(url:toUrl(controller:name(), action:'login'))
		} else if (params.username && execForgotUsername(params.username) == FORGOT_SUCCESS) {
			flash.message = "Password recovery email sent; please check your email"
			
			redirect(url:toUrl(controller:name(), action:'login'))
		} else {
			flash.message = "No user found"
			redirect(url:toUrl(action:"forgot",
					model:[precontroller:params.precontroller, preaction:params.preaction]))
		}
	}
	def doforgotData() {
		debug "LoginController.doForgotData()"
		
		if (params.email && execForgotEmail(params.email) == FORGOT_SUCCESS) {
			renderJSONGet([success:true])
		} else if (params.username && execForgotUsername(params.username) == FORGOT_SUCCESS) {
			renderJSONGet([success:true])
		} else {
			renderJSONGet([message:"We don't recognize that user.",success:false])
		}
	}
	def recover() {
		debug "LoginController.recover()"
		
		PasswordRecovery recovery = PasswordRecovery.look(params.code)
		
		if (recovery == null) {
			flash.message = "Invalid or expired password recovery link, please try again"
		
			redirect(url:toUrl(controller:name(), action:'forgot'))
			return
		}
		
		recovery.reset()
		Utils.save(recovery, true)

		render(view:"/" + name() + "/recover",
				model:[precontroller:params.precontroller, preaction:params.preaction, code:params.code, templateVer:urlService.template(request)])
	}
	def dorecover() {
		debug "LoginController.dorecover()"
		
		PasswordRecovery recovery = PasswordRecovery.look(params.code)
		
		if (recovery == null) {
			flash.message = "Invalid or expired password recovery link, please try again"
		
			redirect(url:toUrl(controller:name(), action:'recover'))
			return
		}
		if (params.password == null || params.password.length() == 0) {
			flash.message = "Please enter password"
		
			redirect(url:toUrl(controller:name(), action:'recover'))
			return
		}
		
		def user = User.get(recovery.getUserId())
		
		PasswordRecovery.delete(recovery)
		
		if (user == null) {
			flash.message = "Invalid password recovery link, please try again"
			
			redirect(url:toUrl(controller:name(), action:'recover'))			
			return
		}
		user.setParameters([password:params.password])
		Utils.save(user, true)
		
		if (!user.validate()) {
			flash.message = "Error saving new password, please email contact@wearecurio.us to investigate"
			
			redirect(url:toUrl(controller:name(), action:'recover'))			
			return
		}

		setLoginUser(user)
		
		redirect(url:toUrl(controller:'home', action:'index'))
	}
	def register() {
		debug "LoginController.register()"

		render(view:registerView(),
				model:[precontroller:params.precontroller, preaction:params.preaction, templateVer:urlService.template(request)])
	}
	protected static final int REGISTER_ERROR_USER_ALREADY_EXISTS = 1
	protected static final int REGISTER_MISSING_FIELDS = 2
	protected static final int REGISTER_DUPLICATE_FIELDS = 3
	
	protected def execRegister(params) {
		def retVal = [:]
		
		params.username = params.username?.toLowerCase()
		if (params.username == null) {
			retVal['errorCode'] = REGISTER_MISSING_FIELDS
			retVal['validateErrors'] = 'Missing username'
			debug "Error creating user: " + retVal['validateErrors']
			return retVal
		}
		
		if (User.findByUsername(params.username) != null) {
			retVal['errorCode'] = REGISTER_ERROR_USER_ALREADY_EXISTS
			return retVal
		}

		debug "Creating user with params: " + params
		
		if (!params.sex) {
			params.sex = 'N'
		}

		def user = User.create(params)
		
		retVal['user'] = user
		if (!user.validate()) {
			retVal['errorCode'] = REGISTER_MISSING_FIELDS
			retVal['validateErrors'] = ''
		    user.errors.allErrors.each {
				retVal['validateErrors'] += it
		    }
			debug "Error creating user: " + retVal['validateErrors']
			return retVal
		} else {
			debug "Successful creation of new user: " + user
			Utils.save(user, true)
			
			def groups
			
			if (params.groups) {
				groups = JSON.parse(params.groups)
	
				for (groupName in groups) {
					def group = UserGroup.lookup(groupName)
					
					group?.addMember(user)
				}
			}
					
			if (params.metaTagName1 && params.metaTagValue1) {
				user.addMetaTag(params.metaTagName1, params.metaTagValue1)
			}
			if (params.metaTagName2 && params.metaTagValue2) {
				user.addMetaTag(params.metaTagName2, params.metaTagValue2)
			}
			if (params.metaTagName3 && params.metaTagValue3) {
				user.addMetaTag(params.metaTagName3, params.metaTagValue3)
			}
			setLoginUser(user)
			retVal['success'] = true
			return retVal
		}
	}
	def doregister() {
		debug "LoginController.doregister()"
		
		if (params.cancel) {
			debug "cancel"
			redirect(url:toUrl(controller:params.precontroller ?: 'home', action:params.preaction ?: 'index'))
			return
		}
		def retVal = execRegister(params)
		if (retVal['success']) {
			redirect(url:toUrl(controller:params.precontroller ?: 'home', action:params.preaction ?: 'index'))
		} else if (retVal['errorCode'] == REGISTER_ERROR_USER_ALREADY_EXISTS) {
			flash.message = "User " + params.username + " already exists"
			redirect(url:toUrl(action:"register",
					model:[precontroller:params.precontroller, preaction:params.preaction]))
			return
		} else if (retVal['errorCode'] == REGISTER_MISSING_FIELDS) {
			flash.message = "Error registering user - highlighted fields required or email address already registered"
			flash.user = retVal['user']
			redirect(url:toUrl(action:"register"))
		}
	}
	def doregisterData() {
		debug "LoginController.doregisterData()"
		
		def retVal = execRegister(params)
		
		if (retVal['success']) {
			renderJSONPost([success:true])
		} else if (retVal['errorCode'] == REGISTER_ERROR_USER_ALREADY_EXISTS) {
			renderJSONPost([success:false, message:"User " + params.username + " already exists"])
		} else if (retVal['errorCode'] == REGISTER_MISSING_FIELDS) {
			renderJSONPost([success:false, message:"All fields are required."])
		}
	}
	def logout() {
		debug "LoginController.logout()"
		
		def user = sessionUser()
		
		if (user == null) {
			debug "not logged in"
			redirect(url:toUrl(action:'index'))
			return
		}
		
		flash.message = "Goodbye ${user.getName()}"
		execLogout()
		redirect(url:toUrl(controller:name(), action:"index"))
	}
}

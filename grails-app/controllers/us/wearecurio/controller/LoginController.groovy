package us.wearecurio.controller
import grails.converters.JSON
import us.wearecurio.model.PasswordRecovery
import us.wearecurio.model.PushNotificationDevice
import us.wearecurio.model.User
import us.wearecurio.services.EmailService
import us.wearecurio.utility.Utils
/**
 * LoginController
 * 
 * @author mitsu
 */
class LoginController extends SessionController {

	def HTTPBuilderService
	EmailService emailService

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
		
		return securityService.login(params.username, params.password)
	}
	
	protected def execLogout() {
		debug "LoginController.execLogout()"
		
		return securityService.logout()
	}
	
	protected def renderJSONPost(data) {
		render "${new JSON(data)}"
	}

	protected def renderJSONPost(data, status) {
		render text: "${new JSON(data)}", status: status
	}
	
	protected def renderJSONGet(data) {
		//debug "${params.callback}(${new JSON(data)})"
		if (params.callback)
			render "${params.callback}(${new JSON(data)})"
		else
			render "${new JSON(data)}"
	}
	
	protected def renderDataGet(data) {
		if (params.callback)
			render "${params.callback}(${data})"
		else
			render "${data}"
	}
	
	protected def renderStringGet(str) {
		def repl = str.replaceAll("'", "\\\\'")
		if (params.callback)
			render "${params.callback}('${repl}')"
		else
			render "'${repl}'"
	}
	
	protected def renderStringPost(str) {
		def repl = str.replaceAll("'", "\\\\'")
		render "'${repl}'"
	}
	
	def login() {
		debug "LoginController.login()"
		
		def message = flash.message
		/*if (Environment.current == Environment.DEVELOPMENT && !session.myip) {
			// Fetching public IP to be used in withings/fitbit/facebook/linkedin.
			try {
				session.myip = HTTPBuilderService.performRestRequest("http://gt-tests.appspot.com/ip", "GET").readLines()[0]
				grailsApplication.config.grails.other.serverURL = "http://${session.myip }:8080/"
				log.info "My public ip: $session.myip"
			} catch (Exception e) {
				log.error "Fetching public ip exception: ", e
			}
		}*/
		
		execLogout()
		render(view:loginView(),
				model:[precontroller:flash.precontroller ?: name(), preaction:flash.preaction ?: 'index', parm:flash.parm ?: [:], message:flash.message, templateVer:urlService.template(request)])
	}
	
	def dologin() {
		debug "LoginController.dologin()"
		
		def user = execLogin()
		String parm = params.parm
		if (user) {
			flash.message = ""
			def uuid = session.persistentSession.fetchUuid()
			def parmMap = parm ? JSON.parse(parm) : [:]
			//parmMap.persistentSessionId = uuid
			redirect(url:toUrl(controller:params.precontroller, action:params.preaction, params:parmMap))
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
			def uuid = session.persistentSession.fetchUuid()
			debug "Logged in, persistent session ID " + uuid
			// TODO: mobileSessionId is deprecated, will be removed eventually
			renderJSONGet([user: user.getJSONDesc(), success:true, persistentSessionId:uuid, mobileSessionId:uuid])
		} else {
			debug "auth failure"
			renderJSONGet([success:false])
		}
	}
	
	def forgot() {
		debug "LoginController.forgot()"

		render(view:forgotView(),
				model:[precontroller:params.precontroller, preaction:params.preaction])
	}
	
	protected static final int EMAIL_CODE_SUCCESS = 0
	protected static final int EMAIL_CODE_ERROR_EMAIL_NOT_RECOGNIZED = 1

	protected def execForgotUser(User user) {
		debug "Sending password recovery email for: " + user.getEmail()
		
		def recovery = PasswordRecovery.create(user.getId())
		
		def recoveryLink = toUrl(controller:'home', action:'recover', params:[code:recovery.getCode()])
		
		emailService.send(user.getEmail(), "We Are Curious: password reset instructions",
				"Someone requested a password reset on your account. If this wasn't you, you can ignore this email. Otherwise, please click here to reset the password for username '" + user.getUsername() + "': " + recoveryLink)
		
		debug "Recovery link: " + recoveryLink
		
		return EMAIL_CODE_SUCCESS
	}
	
	protected def execVerifyUser(User user) {
		debug "Sending account recovery email for: " + user.getEmail()
		
		def verification = PasswordRecovery.createVerification(user.getId())
		
		def verificationLink = toUrl(controller:'home', action:'verify', params:[code:verification.getCode()])
		
		emailService.send(user.getEmail(), "We Are Curious: account verification instructions",
				"Welcome to We Are Curious! Please verify your email address before using the social features of our app. Click here to verify your account: " + verificationLink)
		
		debug "Verification link: " + verificationLink
		
		return EMAIL_CODE_SUCCESS
	}
	
	protected def execForgotPassword(String emailOrUsername) {
		emailOrUsername = emailOrUsername?.toLowerCase()
		User user = User.findByUsername(emailOrUsername)
		if (user == null) {
			user = User.findByEmailAndVirtual(emailOrUsername, false)
			if (user == null) {
				debug "Error recovering password '" + emailOrUsername + "' not found'"
				return EMAIL_CODE_ERROR_EMAIL_NOT_RECOGNIZED
			}
		}
		return execForgotUser(user)
	}
	
	def doforgot() {
		debug "LoginController.doforgot()"
		
		if (params.cancel) {
			debug "cancel"
			redirect(url:toUrl(controller:params.precontroller ?: name(), action:params.preaction ?: 'login'))
			return
		}
		def forgotKey = params.username ?: params.email
		if (forgotKey && execForgotPassword(forgotKey) == EMAIL_CODE_SUCCESS) {
			flash.message = "Password recovery email sent. Please check your email; and be sure to check your spam folder"
			
			redirect(url:toUrl(controller:name(), action:'login'))
		} else {
			flash.message = "Cannot find user '" + forgotKey + "'"
			redirect(url:toUrl(action:"forgot",
					model:[precontroller:params.precontroller, preaction:params.preaction]))
		}
	}
	
	def doforgotData() {
		debug "LoginController.doForgotData()"
		
		def forgotKey = params.username ?: params.email
		if (forgotKey && execForgotPassword(forgotKey) == EMAIL_CODE_SUCCESS) {
			renderJSONGet([success:true])
		} else {
			renderJSONGet([message:"We don't recognize that user.",success:false])
		}
	}
	
	def dosendverify() {
		debug "LoginController.dosendverify()"
		
		User user = sessionUser()
		if (!user) {
			debug "auth failure"
			return
		}
		
		Long userId = user.id
		
		if (execVerifyUser(user) == EMAIL_CODE_SUCCESS) {
			flash.message = "Account verification email sent. Please check your email; be sure to check your spam folder"
			
			render(view:"/home/userpreferences",
					model:[precontroller:flash.precontroller ?: 'home', preaction:flash.preaction ?: 'index', user:user,
						prefs:user.getPreferences(), templateVer:urlService.template(request)])
		} else {
			flash.message = "Error sending verification email."
			render(view:"/home/userpreferences",
					model:[precontroller:flash.precontroller ?: 'home', preaction:flash.preaction ?: 'index', user:user,
						prefs:user.getPreferences(), templateVer:urlService.template(request)])
		}
	}
	
	def dosendverifyData() {
		debug "LoginController.dosendverifyData()"
		
		User user = sessionUser()
		if (!user) {
			debug "auth failure"
			return
		}
		
		if (execVerifyUser(user) == EMAIL_CODE_SUCCESS) {
			renderJSONGet([success:true])
		} else {
			renderJSONGet([message:"Error sending verification email.",success:false])
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
	
	def verify() {
		debug "LoginController.verify()"
		
		PasswordRecovery verification = PasswordRecovery.lookVerification(params.code)
		
		if (verification == null) {
			flash.message = "Invalid or expired email verification link, please try again"
		
			redirect(url:toUrl(controller:'home', action:'index'))
			return
		}
		
		User primeUser = User.get(verification.getUserId())
		
		if (primeUser == null) {
			flash.message = "Invalid email verification link, please try again"
			
			redirect(url:toUrl(controller:'home', action:'index'))
			return
		}
		
		PasswordRecovery.delete(verification)
		
		primeUser.isVerified = true
		Utils.save(user, true)
		
		flash.message = "Account email verified: " + primeUser.getEmail() + " for username: " + user.username
			
		redirect(url:toUrl(controller:'home', action:'index'))
	}
	
	def setLoginUser(user) {
		securityService.setLoginUser(user)
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
		
		User primeUser = User.get(recovery.getUserId())
		
		if (primeUser == null) {
			flash.message = "Invalid password recovery link, please try again"
			
			redirect(url:toUrl(controller:name(), action:'recover'))
			return
		}
		
		PasswordRecovery.delete(recovery)
		
		def users = User.findAllByEmailAndVirtual(primeUser.getEmail(), false)
		
		for (User user in users) {
			user.update([password:params.password])
			Utils.save(user, true)
			
			if (!user.validate()) {
				flash.message = "Error saving new password, please email curious@wearecurio.us to investigate"
				
				redirect(url:toUrl(controller:name(), action:'recover'))
				return
			}
		}
		
		setLoginUser(primeUser)
		
		redirect(url:toUrl(controller:'home', action:'index'))
	}
	
	def register() {
		debug "LoginController.register()"

		render(view:registerView(),
				model:[precontroller:params.precontroller, preaction:params.preaction, templateVer:urlService.template(request)])
	}
	
	protected static final int REGISTER_ERROR_USER_ALREADY_EXISTS = 1
	protected static final int REGISTER_MISSING_FIELDS = 2
	protected static final int REGISTER_DUPLICATE_EMAIL = 3
	protected static final int REGISTER_EMAIL_AND_CONFIRM_EMAIL_DIFFERENT = 4
	
	protected Map execRegister(Map<String, Object> params) {
		Map retVal = [:], p = [:]
		
		params.each { k, v ->
			if (k.endsWith("_profile"))
				p[k.substring(0, k.length() - 8)] = v
			else
				p[k] = v
		}
		
		p.username = p.username?.toLowerCase()
		if (p.username == null) {
			retVal['errorCode'] = REGISTER_MISSING_FIELDS
			retVal['validateErrors'] = 'Missing username'
			debug "Error creating user: " + retVal['validateErrors']
			return retVal
		}
		
		if (User.findByUsername(p.username) != null) {
			retVal['errorCode'] = REGISTER_ERROR_USER_ALREADY_EXISTS
			return retVal
		}
		
		if (User.findByEmailAndVirtual(p.email, false) != null) {
			retVal['errorCode'] = REGISTER_DUPLICATE_EMAIL
			return retVal
		}
		
		if (!(p.email.equals(p.confirm_email))) {
			retVal['errorCode'] = REGISTER_EMAIL_AND_CONFIRM_EMAIL_DIFFERENT
			return retVal
		}

		debug "Creating user with params: " + p

		List<String> groups
		if (p.groups) {
			groups = JSON.parse(p.groups)
		}

		User user = User.create(p, groups)
		
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
			
			if (p.metaTagName1 && p.metaTagValue1) {
				user.addMetaTag(p.metaTagName1, p.metaTagValue1)
			}
			if (p.metaTagName2 && p.metaTagValue2) {
				user.addMetaTag(p.metaTagName2, p.metaTagValue2)
			}
			if (p.metaTagName3 && p.metaTagValue3) {
				user.addMetaTag(p.metaTagName3, p.metaTagValue3)
			}
			setLoginUser(user)
			execVerifyUser(user)
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
			session.showHelp = true
			redirect(url:toUrl(controller: params.precontroller ?: 'home', action: params.preaction ?: 'index'))
		} else if (retVal['errorCode'] == REGISTER_ERROR_USER_ALREADY_EXISTS) {
			flash.message = "User " + params.username + " already exists"
			redirect(url:toUrl(action:"register",
					model:[precontroller:params.precontroller, preaction:params.preaction]))
			return
		} else if (retVal['errorCode'] == REGISTER_MISSING_FIELDS) {
			flash.message = "Error registering user - highlighted fields required"
			flash.user = retVal['user']
			redirect(url:toUrl(action:"register"))
		} else if (retVal['errorCode'] == REGISTER_DUPLICATE_EMAIL) {
			flash.message = "Error registering user - email address already registered"
			flash.user = retVal['user']
			redirect(url:toUrl(action:"register"))
		} else if (retVal['errorCode'] == REGISTER_EMAIL_AND_CONFIRM_EMAIL_DIFFERENT) {
			flash.message = "Error registering user - email and confirm email fields have different values"
			flash.user = retVal['user']
			redirect(url: toUrl(action: "register"))
		} else {
			flash.message = "Error registering user - try different values"
			flash.user = retVal['user']
			redirect(url:toUrl(action:"register"))
		}
	}
	
	def doregisterData() {
		debug "LoginController.doregisterData()"
		
		def p = params
		
		p.first = p.first ?: ''
		p.last = p.last ?: ''
		if (!p.name) {
			p.name = p.first + ' ' + p.last
		}
		p.sex = p.sex ?: 'N'
		p.birthdate = p.birthdate ?: '1/1/1901'
		
		def retVal = execRegister(params)
		
		if (retVal['success']) {
			def uuid = session.persistentSession.fetchUuid()
			renderJSONPost([success:true, persistentSessionId:uuid, mobileSessionId:uuid])
		} else if (retVal['errorCode'] == REGISTER_ERROR_USER_ALREADY_EXISTS) {
			renderJSONPost([success:false, message:"User " + params.username + " already exists"])
		} else if (retVal['errorCode'] == REGISTER_MISSING_FIELDS) {
			renderJSONPost([success:false, message:"All fields are required."])
		} else if (retVal['errorCode'] == REGISTER_DUPLICATE_EMAIL) {
			renderJSONPost([success:false, message:"Email '" + params.email + "' already in use"])
		} else if (retVal['errorCode'] == REGISTER_EMAIL_AND_CONFIRM_EMAIL_DIFFERENT) {
			renderJSONPost([success:false, message:"Email and confirm email fields do not match"])
		} else {
			renderJSONPost([success:false, message:"Error registering user. Try different values."])
		}
	}
	
	// TODO: registerForPushNotfication is deprecated
	def registerForPushNotification() {
		registerForPushNotificationData()
	}
	
	def registerForPushNotificationData() {
		def user = sessionUser()
		debug "LoginController.registerForPushNotificationData()"
		PushNotificationDevice pushNotificationDeviceInstance = 
			PushNotificationDevice.findByUserIdAndToken(user.id,params.token)
		if (!pushNotificationDeviceInstance) {
			pushNotificationDeviceInstance = new PushNotificationDevice()
			pushNotificationDeviceInstance.token = params.token
			pushNotificationDeviceInstance.deviceType = params.int('deviceType')
			pushNotificationDeviceInstance.userId = user.id
			if (pushNotificationDeviceInstance.validate()) {
				Utils.save(pushNotificationDeviceInstance, true)
				debug "Registering device for push notification"
			}
			else {
				pushNotificationDeviceInstance.errors.allErrors.each {
					println it
				}
			}
		} else {
			debug "Device already registered to be notified."
		}
			
		debug params.dump()
		
		renderJSONGet([success:true])
	}
	
	// TODO: unregisterPushNotification is deprecated
	def unregisterPushNotification() {
		unregisterPushNotificationData()
	}
	
	def unregisterPushNotificationData() {
		def user = sessionUser()
		debug "LoginController.unregisterPushNotificationData"
		PushNotificationDevice pushNotificationDeviceInstance =
			PushNotificationDevice.findByUserIdAndToken(user.id,params.token)
		if (!pushNotificationDeviceInstance) {
			debug "Unable to unregister no push notification found."
		} else {
			debug "Unregistering the current device push notification"
			pushNotificationDeviceInstance.delete(flush:true)
		}
			
		debug params.dump()
		
		renderJSONGet([success:true])
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

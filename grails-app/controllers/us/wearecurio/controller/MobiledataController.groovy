package us.wearecurio.controller
import grails.converters.*
import us.wearecurio.model.*;
import us.wearecurio.exceptions.*;
import us.wearecurio.utility.Utils;
import us.wearecurio.services.TwitterDataService;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.SimpleTimeZone;
import us.wearecurio.server.Session
import org.apache.commons.logging.LogFactory

class MobiledataController extends DataController {

	private static def log = LogFactory.getLog(this)
	
	static debug(str) {
		log.debug(str)
	}
	
	def MobiledataController() {
	}

	protected def setLoginUser(user) {
		debug "MobiledataController.setLoginUser() user:" + user
		
		if (user != null) {
			Session mobileSession = Session.createOrGetSession(user)
			debug "Login: mobile session " + mobileSession.fetchUuid()
			session.mobileSession = mobileSession
		}
		super.setLoginUser(user)
	}

	protected def sessionUser() {
		User user = super.sessionUser()
		if (user != null) {
			if (session.mobileSession != null && !session.mobileSession.isDisabled()) {
				if (session.mobileSession.getUserId() != user.getId()) {
					session.mobileSession = Session.createOrGetSession(user)
					return user
				}
			}
			session.mobileSession = Session.createOrGetSession(user)
		}

		return user
	}

	def dologinData() {
		debug "MobiledataController.dologinData()"
		
		def user = execLogin()
		if (user) {
			debug "Logged in, mobile session ID " + session.mobileSession.fetchUuid()
			renderJSONGet([user:user, success:true, mobileSessionId:session.mobileSession.fetchUuid()])
		} else {
			debug "auth failure"
			renderJSONGet([success:false])
		}
	}

	def doregisterData() {
		debug "LoginController.doregisterData()"
		
		def p = params
		
		params.first = ''
		params.last = ''
		params.sex = 'N'
		params.birthdate = '1/1/1901'
		
		def retVal = execRegister(params)

		if (retVal['success']) {
			renderJSONPost([success:true, mobileSessionId:session.mobileSession.fetchUuid()])
		} else if (retVal['errorCode'] == REGISTER_ERROR_USER_ALREADY_EXISTS) {
			renderJSONPost([success:false, message:"User " + params.username + " already exists"])
		} else if (retVal['errorCode'] == REGISTER_MISSING_FIELDS) {
			renderJSONPost([success:false, message:"All fields are required."])
		}
	}
		
	def registerForPushNotification() {
		def user = sessionUser()
		debug user.dump()
		PushNotificationDevice pushNotificationDeviceInstance = 
			PushNotificationDevice.findByUserIdAndToken(user.id,params.token)
		if (!pushNotificationDeviceInstance) {
			pushNotificationDeviceInstance = new PushNotificationDevice()
			pushNotificationDeviceInstance.token = params.token
			pushNotificationDeviceInstance.deviceType = params.int('deviceType')
			pushNotificationDeviceInstance.userId = user.id
			if (pushNotificationDeviceInstance.validate()) {
				pushNotificationDeviceInstance.save()
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
}

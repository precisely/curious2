package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import org.springframework.transaction.annotation.Transactional

import grails.util.Environment

import us.wearecurio.server.DateRecord
import us.wearecurio.utility.Utils
import us.wearecurio.model.*
import us.wearecurio.data.RepeatType
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.*

class RemindEmailService {
	EmailService emailService
	def googleMessageService
	def appleNotificationService
	AlertGenerationService alertGenerationService

	private static def log = LogFactory.getLog(this)

	@Transactional(readOnly = true)
	boolean sendReminderForEvent(long userId, String email, AlertNotification alert, def devices) {
		boolean actuallySend = Environment.current == Environment.PRODUCTION
		
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZ").withZone(DateTimeZone.UTC)
		if (alert != null) {
			if (!actuallySend)
				return true
			if (email != null && email.length() > 1) {
				try {
					log.debug "Trying to send reminder email " + alert + " to " + email
					def messageBody = url + "?entryId=" + alert.objectId
					def messageSubject = "Reminder to track: " + alert.text
					emailService.send(email, messageSubject, messageBody)
				} catch (Throwable t) {
					log.debug "Error while sending email: " + t
				}
			}
			def notificationMessage = "Reminder to track:" + alert.text
			devices.each { userDevice ->
				if (userDevice && userDevice.deviceType == PushNotificationDevice.ANDROID_DEVICE) {
					googleMessageService.sendMessage(notificationMessage, [userDevice.token])
					log.debug "Notifying Android device for user "+ alert.userId
				} else if (userDevice && userDevice.deviceType == PushNotificationDevice.IOS_DEVICE) {
					//TODO Send APN message for reminder
					log.debug "Notifying iOS device for user "+ alert.userId
					appleNotificationService.sendMessage(notificationMessage, [userDevice.token],"Curious",
						['entryId':alert.objectId,'entryDate':dateTimeFormatter.print(alert.date.getTime())])
				}
			}
			
			return true
		}
		
		return false
	}
	
	/**
	 * Returns number of reminders sent
	 */
	int sendReminders(Date now) {
		log.debug "RemindEmailService.sendReminders()"
		
		alertGenerationService.generate(now)
		
		DateRecord rec = DateRecord.lookup(DateRecord.REMIND_EMAILS)
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZ").withZone(DateTimeZone.UTC)
		
		Date oldDate = rec.getDate()
		
		if (!oldDate)
			oldDate = new Date(now.getTime() - 5*60000L)
			
		def remindUsers = User.executeQuery("SELECT u.id, u.remindEmail FROM User u")
		log.debug "Users to remind " + remindUsers.size()
		//appleNotificationService.sendMessage("This is a test reminder with data",
			//["54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd"],"Curious", 
			//['entryId':"5229",'entryDate':dateTimeFormatter.print(new DateTime(now).plusDays(3).getMillis())])
		
		int numReminders = 0
		
		for (def user in remindUsers) {
			def c = Entry.createCriteria()
			def userId = user[0]
			def email = user[1]
			def u = User.get(userId)
			def devices = PushNotificationDevice.findAllByUserId(userId)
			def url = "https://dev.wearecurio.us/home/index"
			
			if (userId == 1L)
				userId = userId

			def remindEvents = Entry.fetchReminders(u, oldDate, (long)(now.getTime() - oldDate.getTime()) / 1000L)
			
			if (remindEvents.size() > 0) {
				log.debug "Reminder for user " + userId + " " + u?.username
				log.debug "User devices registered for notification " + devices.size()
				log.debug "Number of remind events found "+remindEvents.size()
			}
			for (AlertNotification alert in remindEvents) {
				if (sendReminderForEvent(userId, email, alert, devices))
					++numReminders
			}
			
		}
		
		rec.setDate(now)
		
		Utils.save(rec, true)
		
		return numReminders
	}
}

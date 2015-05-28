package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import org.springframework.transaction.annotation.Transactional

import us.wearecurio.server.DateRecord
import us.wearecurio.utility.Utils
import us.wearecurio.model.*
import us.wearecurio.model.Entry.RepeatType
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.*

class RemindEmailService {
	def mailService
	def googleMessageService
	def appleNotificationService

	private static def log = LogFactory.getLog(this)

	static Tag lhpMemberTag
	
	@Transactional(readOnly = true)
	def sendReminderForEvent(long userId, String email, def entryId, def devices) {
		def event = Entry.get(entryId)
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZ").withZone(DateTimeZone.UTC)
		if (event != null) {
			if (email != null && email.length() > 1) {
				try {
					log.debug "Trying to send reminder email " + event + " to " + email
					def messageBody = url + "?entryId=" + event.getId()
					def messageSubject = "Reminder to track:" + event.getTag().getDescription()
					mailService.sendMail {
						to email
						from "contact@wearecurio.us"
						subject messageSubject
						body messageBody
					}
				} catch (Throwable t) {
					log.debug "Error while sending email: " + t
				}
			}
			def notificationMessage = "Reminder to track:" + event.getTag().getDescription() + " " + event.getComment()
			devices.each { userDevice ->
				if (userDevice && userDevice.deviceType == PushNotificationDevice.ANDROID_DEVICE) {
					googleMessageService.sendMessage(notificationMessage, [userDevice.token])
					log.debug "Notifying Android device for user "+userId
				} else if (userDevice && userDevice.deviceType == PushNotificationDevice.IOS_DEVICE) {
					//TODO Send APN message for reminder
					log.debug "Notifying iOS device for user "+userId
					appleNotificationService.sendMessage(notificationMessage, [userDevice.token],"Curious",
						['entryId':event.getId(),'entryDate':dateTimeFormatter.print(event.getDate().getTime())])
				}
			}
		}
	}
	
	def sendReminders() {
		log.debug "RemindEmailService.sendReminders()"
		
		if (!lhpMemberTag)
			lhpMemberTag = User.lookupMetaTag("lhpmember", "true")

		DateRecord rec = DateRecord.lookup(DateRecord.REMIND_EMAILS)
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZ").withZone(DateTimeZone.UTC)
		
		Date oldDate = rec.getDate()
		
		Date now = new Date()
		
		if (!oldDate)
			oldDate = new Date(now.getTime() - 5*60000L)
			
		def remindUsers = User.executeQuery("SELECT u.id, u.remindEmail FROM User u")
		log.debug "Users to remind " + remindUsers.size()
		//appleNotificationService.sendMessage("This is a test reminder with data",
			//["54f8158bbe5bd3fc0031c4fde5c6cfdc42e43b6a2fa67762c8d0bf1bd000e2fd"],"Curious", 
			//['entryId':"5229",'entryDate':dateTimeFormatter.print(new DateTime(now).plusDays(3).getMillis())])
		for (def user in remindUsers) {
			def c = Entry.createCriteria()
			def userId = user[0]
			def email = user[1]
			def u = User.get(userId)
			def devices = PushNotificationDevice.findAllByUserId(userId)
			def lhp = u.hasMetaTag(lhpMemberTag)
			def url = lhp ? "https://lamhealth.wearecurio.us/mobile/index" : "https://dev.wearecurio.us/mobile/index"

			def remindEvents = Entry.fetchReminders(u, oldDate, (long)(now.getTime() - oldDate.getTime()) / 1000L)
			
			if (remindEvents.size() > 0) {
				log.debug "Reminder for user " + userId + " " + u?.username
				log.debug "User devices registered for notification " + devices.size()
				log.debug "Number of remind events found "+remindEvents.size()
			}
			for (def eventIdRecord in remindEvents) {
				sendReminderForEvent(userId, email, eventIdRecord['id'], devices)
			}
			
		}
		
		rec.setDate(now)
		
		Utils.save(rec, true)
	}
}

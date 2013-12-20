package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import us.wearecurio.server.DateRecord
import us.wearecurio.utility.Utils
import us.wearecurio.model.*
import us.wearecurio.model.Entry.RepeatType

class RemindEmailService {
	
	def mailService
	def GCMService
	def APNSService

	private static def log = LogFactory.getLog(this)

	static transactional = true
	
	static Tag lhpMemberTag
	
	def sendReminders() {
		log.debug "RemindEmailService.sendReminders()"
		
		if (!lhpMemberTag)
			lhpMemberTag = User.lookupMetaTag("lhpmember","true")

		DateRecord rec = DateRecord.lookup(DateRecord.REMIND_EMAILS)
		
		Date oldDate = rec.getDate()
		
		Date now = new Date()
		
		if (!oldDate)
			oldDate = new Date(now.getTime() - 5*60000L)
			
		def remindUsers = User.executeQuery("SELECT u.id, u.remindEmail FROM User u WHERE u.remindEmail is not null")
		log.debug "Users to remind "+remindUsers.size()
		for (def user in remindUsers) {
			def c = Entry.createCriteria()
			def userId = user[0]
			def email = user[1]
			def u = User.get(userId)
			log.debug "Reminder for user "+userId+" "+u?.username
			def devices = PushNotificationDevice.findAllByUserId(userId)
			log.debug "User devices registered for notification "+devices.size()
			def lhp = u.hasMetaTag(lhpMemberTag)
			def url = lhp ? "https://lamhealth.wearecurio.us/mobile/index" : "https://dev.wearecurio.us/mobile/index"

			def remindEvents = Entry.fetchReminders(u, oldDate, (long)(now.getTime() - oldDate.getTime()) / 1000L)
			
			log.debug "Number of remind events found "+remindEvents.size()
			for (def eventIdRecord in remindEvents) {
				def event = Entry.get(eventIdRecord['id'])
				if (event != null) {
					log.debug "Trying to send reminder email " + event + " to " + email
					def messageBody = url + "?entryId=" + event.getId()
					def messageSubject = "Reminder to track:" + event.getTag().getDescription()
					mailService.sendMail {
						to email
						from "contact@wearecurio.us"
						subject messageSubject
						body messageBody
					}
					devices.each { userDevice ->
						if (userDevice && userDevice.deviceType == PushNotificationDevice.ANDROID_DEVICE) {
							GCMService.sendMessage(messageBody, [userDevice.token])
							log.debug "Notifying Android device for user "+userId
						} else if (userDevice && userDevice.deviceType == PushNotificationDevice.IOS_DEVICE) {
							//TODO Send APN message for reminder
							log.debug "Notifying iOS device for user "+userId
							APNSService.sendMessage(messageBody, [userDevice.token])
						}
					}
				}
			}
		}
		
		rec.setDate(now)
		
		Utils.save(rec, true)
	}
}

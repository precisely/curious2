package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import us.wearecurio.server.DateRecord
import us.wearecurio.utility.Utils
import us.wearecurio.model.*
import us.wearecurio.model.Entry.RepeatType

class RemindEmailService {
	
	def mailService

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
		
		for (def user in remindUsers) {
			def c = Entry.createCriteria()
			def userId = user[0]
			def email = user[1]
			def u = User.get(userId)
			PushNotificationDevice userDevice = PushNotificationDevice.findByUserId(userId)
			
			def lhp = u.hasMetaTag(lhpMemberTag)
			def url = lhp ? "https://lamhealth.wearecurio.us/mobile/index" : "https://dev.wearecurio.us/mobile/index"

			def remindEvents = Entry.fetchReminders(u, oldDate, (long)(now.getTime() - oldDate.getTime()) / 1000L)
			
			for (def eventIdRecord in remindEvents) {
				def event = Entry.get(eventIdRecord['id'])
				if (event != null) {
					log.debug "Trying to send reminder email " + event + " to " + email
					mailService.sendMail {
						to email
						from "contact@wearecurio.us"
						subject "Reminder to track:" + event.getTag().getDescription()
						body url + "?entryId=" + event.getId()
					}
					
					if (userDevice && userDevice.deviceType == PushNotificationDevice.ANDROID_DEVICE) {
						//TODO Send GCM message for reminder
						log.debug "Notifying Android device for user "+userId
					} else if (userDevice && userDevice.deviceType == PushNotificationDevice.IOS_DEVICE) {
						//TODO Send APN message for reminder
						log.debug "Notifying iOS device for user "+userId
					}
				}
			}
		}
		
		rec.setDate(now)
		
		Utils.save(rec, true)
	}
}

import grails.plugin.mail.MailService

import org.joda.time.DateTimeZone

import us.wearecurio.services.*
import us.wearecurio.utility.Utils

class BootStrap {

	MigrationService migrationService
	WithingsDataService withingsDataService
	RemindEmailService remindEmailService
	DatabaseService databaseService
	MailService mailService

	def init = { servletContext ->
		log.debug "Curious bootstrap started executing.."
		log.info "Default TimeZone: " + TimeZone.getDefault().getID()
		log.info "Default JodaTimeZone (DateTimeZone): " + DateTimeZone.getDefault()

		DatabaseService.set(databaseService)
		Utils.setMailService(mailService)
		migrationService.doMigrations()
		//withingsDataService.refreshSubscriptions()
		log.debug "Curious bootstrap finished executing."
	}

	def destroy = {
	}

}
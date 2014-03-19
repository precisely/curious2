import grails.plugin.mail.MailService
import grails.util.Environment;

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
		DatabaseService.set(databaseService)
		Utils.setMailService(mailService)
		migrationService.doMigrations()
		//withingsDataService.refreshSubscriptions()
		log.debug "Curious bootstrap finished executing."
	}

	def destroy = {
	}

}
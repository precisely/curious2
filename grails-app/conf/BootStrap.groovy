import grails.plugin.mail.MailService
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
		//withingsDataService.poll()
		//withingsDataService.refreshSubscriptions()
	}

	def destroy = {
	}

}
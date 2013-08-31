import grails.plugin.mail.MailService
import us.wearecurio.services.*
import us.wearecurio.model.PasswordRecovery
import us.wearecurio.utility.Utils

class BootStrap {
	MigrationService migrationService
	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	RemindEmailService remindEmailService
	DatabaseService databaseService
	
    def init = { servletContext ->
		DatabaseService.set(databaseService)
		migrationService.doMigrations()
		withingsDataService.initialize()
		withingsDataService.poll()
		withingsDataService.refreshSubscriptions()
		fitBitDataService.initialize()
		remindEmailService.sendReminders()
    }
    def destroy = {
    }
}

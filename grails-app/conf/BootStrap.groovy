import grails.plugin.mail.MailService
import us.wearecurio.services.*
import us.wearecurio.model.PasswordRecovery
import us.wearecurio.utility.Utils
import org.hibernate.dialect.function.SQLFunctionTemplate
import org.hibernate.Hibernate

class BootStrap {
	MigrationService migrationService
	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	RemindEmailService remindEmailService
	DatabaseService databaseService
	MailService mailService
	
    def init = { servletContext ->
		DatabaseService.set(databaseService)
		Utils.setMailService(mailService)
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

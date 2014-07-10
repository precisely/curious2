import grails.converters.JSON
import grails.plugin.mail.MailService
import grails.util.Environment
import us.wearecurio.marshaller.EnumMarshaller
import us.wearecurio.model.UserGroup
import us.wearecurio.services.*
import us.wearecurio.thirdparty.withings.*
import us.wearecurio.utility.Utils

class BootStrap {

	MigrationService migrationService
	WithingsDataService withingsDataService
	RemindEmailService remindEmailService
	DatabaseService databaseService
	MailService mailService

	def init = { servletContext ->
		log.debug "Curious bootstrap started executing."
		def current = Environment.current
		DatabaseService.set(databaseService)
		Utils.setMailService(mailService)
		migrationService.doMigrations()

		JSON.registerObjectMarshaller(new EnumMarshaller())

		//withingsDataService.refreshSubscriptions()
		/*if (current != Environment.TEST) {
			try {
				new IntraDayDataThread().start()
			} catch(IllegalStateException ie) {
				log.debug "Bootstrap: Could not start IntraDayDataThread"
			}
		}*/
		log.debug "Curious bootstrap finished executing."
	}

	def destroy = {
	}

}

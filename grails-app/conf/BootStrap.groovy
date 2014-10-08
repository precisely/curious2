import grails.converters.JSON
import grails.util.Environment

import org.joda.time.DateTimeZone
import us.wearecurio.thirdparty.withings.*

import us.wearecurio.server.BackgroundTask

import us.wearecurio.marshaller.EnumMarshaller
import us.wearecurio.marshaller.CorrelationMarshaller
import us.wearecurio.model.UserGroup
import us.wearecurio.services.*
import us.wearecurio.thirdparty.withings.*
import us.wearecurio.utility.Utils
import org.springframework.web.context.support.WebApplicationContextUtils


class BootStrap {

	MigrationService migrationService
	WithingsDataService withingsDataService
	RemindEmailService remindEmailService
	DatabaseService databaseService
	TagService tagService
	EmailService emailService

	def init = { servletContext ->
		log.debug "Curious bootstrap started executing."
		def current = Environment.current
		DatabaseService.set(databaseService)
		TagService.set(tagService)
		EmailService.set(emailService)
		migrationService.doMigrations()
		JSON.registerObjectMarshaller(new EnumMarshaller())
		def springContext = WebApplicationContextUtils.getWebApplicationContext( servletContext )
		springContext.getBean( "customObjectMarshallers" ).register()

		BackgroundTask.launch {
			migrationService.doBackgroundMigrations()
		}
		withingsDataService.refreshSubscriptions()
		if (current != Environment.TEST) {
			try {
				new IntraDayDataThread().start()
			} catch(IllegalStateException ie) {
				log.debug "Bootstrap: Could not start IntraDayDataThread"
			}
		}
		log.debug "Curious bootstrap finished executing."
	}

	def destroy = {
	}

}

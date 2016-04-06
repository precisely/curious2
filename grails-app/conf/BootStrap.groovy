import grails.converters.JSON
import grails.util.Environment

import org.joda.time.DateTimeZone
import us.wearecurio.filters.EmailVerificationCheckFilters
import us.wearecurio.thirdparty.withings.IntraDayDataThread

import us.wearecurio.server.BackgroundTask

import us.wearecurio.marshaller.EnumMarshaller
import us.wearecurio.model.UserGroup
import us.wearecurio.services.*
import us.wearecurio.utility.Utils
import us.wearecurio.data.DataRetriever
import org.springframework.web.context.support.WebApplicationContextUtils

import us.wearecurio.data.UnitGroupMap

import us.wearecurio.model.TagStats

class BootStrap {

	MigrationService migrationService
	WithingsDataService withingsDataService
	RemindEmailService remindEmailService
	DatabaseService databaseService
	EmailService emailService
	AnalyticsService analyticsService
	SecurityService securityService
	SearchService searchService
	EntryParserService entryParserService
	AlertGenerationService alertGenerationService
	
	static protected initClosures = []
	
	def init = { servletContext ->
		log.debug "Curious bootstrap started executing."
		def current = Environment.current
		
		DatabaseService.set(databaseService)
		EmailService.set(emailService)
		SecurityService.set(securityService)
		SearchService.set(searchService)
		EntryParserService.set(entryParserService)
		AnalyticsService.set(analyticsService)
		AlertGenerationService.set(alertGenerationService)
		
		DataRetriever.setDatabaseService(databaseService)
		
		UnitGroupMap.initialize()

		EmailVerificationCheckFilters.populateEmailVerificationEndpoints()
		
		migrationService.doMigrations()
		JSON.registerObjectMarshaller(new EnumMarshaller())
		def springContext = WebApplicationContextUtils.getWebApplicationContext( servletContext )
		springContext.getBean( "customObjectMarshallers" ).register()
		BackgroundTask.launch {
			migrationService.doBackgroundMigrations()
		}

		TagStats.initializeSharedTags()
		
		Utils.registerTestReset({ DataRetriever.resetCache() })

		/**
		 * This marshaller is implemented to parse date into javascript date format
		 * when rendering response for a POST request. Default Date format in config is javascript this marshaller will override it.
		 * Usage: 
		 *  JSON.use("jsonDate") {
		 *		sampleInstance as JSON
		 *  }
		 */

		JSON.createNamedConfig("jsonDate") {
			it.registerObjectMarshaller(Date) {
				return it.format("yyyy-MM-dd'T'HH:mm:ssZ")
			}
		}

		log.debug "Populating no auth actions."
		securityService.populateNoAuthMethods()

		withingsDataService.refreshSubscriptions()
		if (current != Environment.TEST && current != Environment.DEVELOPMENT) {
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

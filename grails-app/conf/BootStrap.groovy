import grails.converters.JSON
import grails.util.Environment
import org.springframework.web.context.support.WebApplicationContextUtils
import us.wearecurio.data.DataRetriever
import us.wearecurio.data.UnitGroupMap
import us.wearecurio.filters.EmailVerificationCheckFilters
import us.wearecurio.marshaller.EnumMarshaller
import us.wearecurio.marshaller.QuestionMarshaller
import us.wearecurio.model.TagInputType
import us.wearecurio.marshaller.ProfileTagMarshaller
import us.wearecurio.model.TagStats
import us.wearecurio.server.BackgroundTask
import us.wearecurio.services.AlertGenerationService
import us.wearecurio.services.AnalyticsService
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EmailService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.MigrationService
import us.wearecurio.services.SearchService
import us.wearecurio.services.SecurityService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.thirdparty.withings.IntraDayDataThread
import us.wearecurio.utility.Utils

class BootStrap {

	MigrationService migrationService
	WithingsDataService withingsDataService
	DatabaseService databaseService
	EmailService emailService
	AnalyticsService analyticsService
	SecurityService securityService
	SearchService searchService
	EntryParserService entryParserService
	AlertGenerationService alertGenerationService

	def grailsApplication
	
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
		JSON.registerObjectMarshaller(new ProfileTagMarshaller())
		JSON.registerObjectMarshaller(new QuestionMarshaller())

		def springContext = WebApplicationContextUtils.getWebApplicationContext( servletContext )
		springContext.getBean( "customObjectMarshallers" ).register()
		BackgroundTask.launch {
			migrationService.doBackgroundMigrations()
			/*def userList = User.list()
			for (user in userList) {
				DataService.pollAllForUserId(user.id)
			}*/
		}

		TagStats.initializeSharedTags()
		TagInputType.initializeCachedTagWithInputTypes()

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

		if (grailsApplication.config.wearecurious.runImportJobs)
			withingsDataService.refreshSubscriptions()

		log.debug "Populating no auth actions."
		securityService.populateNoAuthMethods()

		if (current != Environment.TEST && current != Environment.DEVELOPMENT) {
			try {
				new IntraDayDataThread().start()
			} catch(IllegalStateException ie) {
				log.debug "Bootstrap: Could not start IntraDayDataThread"
				Utils.reportError("Bootstrap: Could not start IntraDayDataThread","")
			}
		}

		log.debug "Curious bootstrap finished executing."
	}

	def destroy = {
	}

}
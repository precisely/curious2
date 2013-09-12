package us.wearecurio.services

class WeatherService {
	def grailsApplication
	HTTPBuilderService HTTPBuilderService
	
	def fetchConditions(def city, def state, def date = new Date()) {
		def key = grailsApplication.config.grails.api.weatherunderground.key
		def url = "http://${key}/CA/LosAngeles.json"
		
	}
}

package us.wearecurio.services

class WeatherService {
	def grailsApplication
	HTTPBuilderService HTTPBuilderService
	
	def fetchConditions(def city="San_Francisco", def state ="CA", def date = new Date()) {
		def key = grailsApplication.config.api.weatherunderground.key
		def url = "http://api.wunderground.com/api/${key}/conditions/q/${state}/${city}.json"
		
		def result = HTTPBuilderService.performRestRequest(url)
		println result.dump()
		
	}
}

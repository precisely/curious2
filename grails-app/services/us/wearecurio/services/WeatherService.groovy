package us.wearecurio.services
import us.wearecurio.http.*

class WeatherService {
	def grailsApplication
	
	
	def fetchConditions(def city="San_Francisco", def state ="CA", def date = new Date()) {
		def key = grailsApplication.config.api.weatherunderground.key
		def url = "http://api.wunderground.com/api/${key}/conditions/q/${state}/${city}.json"
		HttpConnection connection = new HttpConnection(url)
		def result = connection.sendGet()
		println result.dump()
		return result
	}
}

package us.wearecurio.services

import org.springframework.transaction.annotation.Transactional

import java.text.SimpleDateFormat;

import grails.converters.JSON;
import us.wearecurio.http.*

class WeatherService {
	def grailsApplication
	
	
	def fetchConditions(def city="San_Francisco", def state ="CA") {
		def key = grailsApplication.config.api.weatherunderground.key
		def url = "http://api.wunderground.com/api/${key}/conditions/q/${state}/${city}.json"
		HttpConnection connection = new HttpConnection(url)
		def result = connection.sendGet()
		result = JSON.parse(result)
		return result
	}
	
	def fetchHistory(def city="San_Francisco", def state ="CA", def date = new Date()) {
		def key = grailsApplication.config.api.weatherunderground.key
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd")
		def url = "http://api.wunderground.com/api/${key}/history_${format.format(date)}/q/${state}/${city}.json"
		
		if (date >= new Date().clearTime()) {
		//	return fetchConditions(city,state)
		}
		
		HttpConnection connection = new HttpConnection(url)
		def result = connection.sendGet()
		result = JSON.parse(result)
		return result
	}
}

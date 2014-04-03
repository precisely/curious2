package us.wearecurio.services

import org.springframework.transaction.annotation.Transactional

class LocationService {

	static transactional = false    // Required, if the geocodeAddress method is called from beforeUpdate event

	def grailsApplication
	def HTTPBuilderService

	/**
	 * A generic method to geocode address by giving either query or other arguments.
	 * @param query any address query like: <pre>1 Microsoft way, Redmond WA 98052</pre>
	 * @param args OPTIONAL a map containing other address parameters like: country, postalCode
	 * etc. <b>Note:</b> <em>country</em> is required if <em>postalCode</em> is given.
	 * @return a map containing latitude or longitude or empty map if no result found.
	 */
	Map geocodeAddress(String query, Map args = [:]) {
		StringBuilder geocodeURL = new StringBuilder("http://dev.virtualearth.net/REST/v1/Locations")
		geocodeURL.append("?key=${grailsApplication.config.api.bingMapKey.key}")
		if(query) {
			geocodeURL.append("&q=${query.encodeAsURL()}")
		}
		if(args.postalCode) {
			geocodeURL.append("&postalCode=$args.postalCode")
			geocodeURL.append("&countryRegion=$args.country")
		} else if(args.country) {
			geocodeURL.append("&countryRegion=$args.country")
		}

		def apiResponse = HTTPBuilderService.performRestRequest(geocodeURL.toString(), "GET")
		if(!apiResponse.isSuccess()) {
			return [:]
		}
		if(apiResponse.statusDescription != "OK") {
			log.warn apiResponse
			return [:]
		}
		def location = apiResponse.resourceSets[0]?.resources[0]?.point?.coordinates
		if(location) {
			return [latitude: location[0], longitude: location[1]]
		}
		return [:]
	}

}
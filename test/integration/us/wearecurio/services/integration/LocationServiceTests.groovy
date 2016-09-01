package us.wearecurio.services.integration

import us.wearecurio.services.LocationService
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class LocationServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	LocationService locationService

	void setup() {
	}

	void cleanup() {
	}

	void testQueryString() {
		when:
		def result = locationService.geocodeAddress("Pune, Maharashtra, India")

		then:
		assert result instanceof Map
		assert result.latitude
		assert isClose(18.50419044494629, result.latitude, 0.0001)
		assert isClose(73.85308837890625, result.longitude, 0.0001)

		when:
		result = locationService.geocodeAddress("1 Microsoft way, Redmond WA 98052")

		then:
		assert result.latitude
		assert isClose(47.64013051986694, result.latitude, 0.0001)
		assert isClose(-122.12973184883595, result.longitude, 0.001)

		when:
		result = locationService.geocodeAddress("Bilaspur, Chhattisgarh")

		then:
		result.latitude
		assert isClose(22.01, result.latitude, 0.01)
		assert isClose(82.09, result.longitude, 0.01)
	}

	void testOnlyPostalCode() {
		when:
		def result = locationService.geocodeAddress("", [postalCode: 411045])

		then:
		assert result instanceof Map
		assert null == result.latitude
		assert null == result.longitude
	}

	void testPostalCodeWithCountryCode() {
		when:
		def result = locationService.geocodeAddress("", [postalCode: 411045, country: "IN"])

		then:
		assert result instanceof Map
		assert result.latitude
		assert isClose(23.4, result.latitude, 0.01)
		assert isClose(79.45, result.longitude, 0.01)
	}
}
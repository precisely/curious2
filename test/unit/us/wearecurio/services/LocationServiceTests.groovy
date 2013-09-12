package us.wearecurio.services

import grails.test.mixin.*

import org.codehaus.groovy.grails.plugins.codecs.URLCodec
import org.junit.*

import us.wearecurio.services.HTTPBuilderService

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(LocationService)
class LocationServiceTests {

	void setUp() {
		mockCodec(URLCodec)
		defineBeans {
			HTTPBuilderService(HTTPBuilderService)
		}
	}

	void tearDown() {

	}

	void testQueryString() {
		def result = service.geocodeAddress("Pune, Maharashtra, India")
		assert result instanceof Map
		assertEquals 18.50419044494629, result.latitude
		assertEquals 73.85308837890625, result.longitude

		result = service.geocodeAddress("1 Microsoft way, Redmond WA 98052")
		assertEquals 47.64013051986694, result.latitude
		assertEquals (-122.12973184883595, result.longitude)

		result = service.geocodeAddress("Bilaspur, Chhattisgarh")
		assertEquals 22.0693302154541, result.latitude
		assertEquals 82.15856170654297, result.longitude
	}

	void testOnlyPostalCode() {
		def result = service.geocodeAddress("", [postalCode: 411045])
		assert result instanceof Map
		assertEquals null, result.latitude
		assertEquals null, result.longitude
	}

	void testPostalCodeWithCountryCode() {
		def result = service.geocodeAddress("", [postalCode: 411045, country: "IN"])
		assert result instanceof Map
		assertEquals 22.67099952697754, result.latitude
		assertEquals 79.14399719238281, result.longitude
	}

}
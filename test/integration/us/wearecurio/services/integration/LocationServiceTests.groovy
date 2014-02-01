package us.wearecurio.services.integration

import grails.test.mixin.*

import org.codehaus.groovy.grails.plugins.codecs.URLCodec
import org.junit.*

import java.lang.Math

import us.wearecurio.services.HTTPBuilderService
import us.wearecurio.services.LocationService;

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
		if(result.latitude) {
			assertEquals 18.50419044494629, result.latitude
			assertEquals 73.85308837890625, result.longitude
		}

		result = service.geocodeAddress("1 Microsoft way, Redmond WA 98052")
		if(result.latitude) {
			assert Math.abs(47.64013051986694 - result.latitude) < 0.001
			assert Math.abs(-122.12973184883595 - result.longitude) < 0.001
		}

		result = service.geocodeAddress("Bilaspur, Chhattisgarh")
		if(result.latitude) {
			assert Math.abs(22.08 - result.latitude) < 0.01
			assert Math.abs(82.15 - result.longitude) < 0.01
		}
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
		if(result.latitude) {
			assert Math.abs(23.4 - result.latitude) < 0.01
			assert Math.abs(79.45 - result.longitude) < 0.01
		}
	}

}
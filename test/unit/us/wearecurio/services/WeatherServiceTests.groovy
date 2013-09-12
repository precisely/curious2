package us.wearecurio.services
import grails.test.mixin.*

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(WeatherService)
class WeatherServiceTests {
	
	void setUp() {
		
	}
	
	void tearDown() {
		
	}
	
	void testHTTPRequestWithNoInput() {
		def result = service.fetchConditions()
		assert result.temp_c != null, "Invalid result"
	}
	
	void testHTTPRequest() {
		def result = service.fetchConditions("New_York","NY")
		assert result.temp_c != null, "Invalid result"
	}
}

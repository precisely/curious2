package us.wearecurio.services
import grails.test.mixin.*
import groovy.json.JsonOutput;

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(WeatherService)
class WeatherServiceTests {
	
	void setUp() {
		
	}
	
	void tearDown() {
		
	}
	
	void testFetchConditionsWithNoInput() {
		def result = service.fetchConditions()
		assert result.response != null, "Invalid result"
	}
	
	void testFetchConditions() {
		def result = service.fetchConditions("New_York","NY")
		assert result.response != null, "Invalid result"
	}
	
	void testFetchHistoryWithNoInput() {
		def result = service.fetchHistory()
		println JsonOutput.prettyPrint(result.toString())
		assert result.response != null, "Invalid result"
	}
}

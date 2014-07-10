package us.wearecurio.services.integration

import org.junit.*
import us.wearecurio.services.WeatherService;
import grails.test.mixin.*
import groovy.json.JsonOutput;

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class WeatherServiceTests extends CuriousServiceTestCase {
	
	WeatherService weatherService
	
	@Before
	void setUp() {
		super.setUp()
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}
	
	void testFetchConditionsWithNoInput() {
		def result = weatherService.fetchConditions()
		assert result.response != null, "Invalid result"
	}
	
	void testFetchConditions() {
		def result = weatherService.fetchConditions("New_York","NY")
		assert result.response != null, "Invalid result"
	}
	
	void testFetchHistoryWithNoInput() {
		def result = weatherService.fetchHistory()
		println JsonOutput.prettyPrint(result.toString())
		assert result.response != null, "Invalid result"
	}
}

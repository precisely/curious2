package us.wearecurio.services.integration

import groovy.json.JsonOutput
import us.wearecurio.services.WeatherService
/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
class WeatherServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	WeatherService weatherService
	
	void setup() {
	}
	
	void cleanup() {
	}

	void testFetchConditionsWithNoInput() {
		when:
		def result = weatherService.fetchConditions()

		then:
		assert result.response != null, "Invalid result"
	}

	void testFetchConditions() {
		when:
		def result = weatherService.fetchConditions("New_York","NY")

		then:
		assert result.response != null, "Invalid result"
	}

	void testFetchHistoryWithNoInput() {
		when:
		def result = weatherService.fetchHistory()
		println JsonOutput.prettyPrint(result.toString())

		then:
		assert result.response != null, "Invalid result"
	}
}

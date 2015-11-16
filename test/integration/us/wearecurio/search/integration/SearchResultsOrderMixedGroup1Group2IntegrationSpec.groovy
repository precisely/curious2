package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

class SearchResultsOrderMixedGroup1Group2IntegrationSpec extends SearchResultsOrderIntegrationSpecBase {

	void "Test order all first results are group1"() {
	}
	
	void "Test order all first results are group2 when no group1"() {
	}
	
	void "Test order some first results are group1, then group2 when all group1 used"() {
	}
	
	void "Test order all results are group1 when no group2"() {
	}
	
	void "Test order all results are group2 when no group1"() {
	}
	
	void "Test order alternates between group1 and group2 after first results"() {
	}
	
	void "Test order alternates between group1 and group2 after first results then all group1 when group2 runs out"() {
	}
	
	void "Test order alternates between group1 and group2 after first results then all group2 when group1 runs out"() {
	}
	
	void "Test no results returned when no group1 or group2 with no offset"() {
	}
	
	void "Test no results returned when no group1 or group2 with offset"() {
	}
	
	void "Test orderList size is smaller than max when not enough group1 and group2"() {
	}
	
	void "Test orderList size is smaller than max when not enough group1 and group2 with offset"() {
	}
}

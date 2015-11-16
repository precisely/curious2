package us.wearecurio.services.unit

//import grails.test.mixin.TestMixin
//import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import grails.test.spock.*

import us.wearecurio.services.SearchService

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
//@TestMixin(GrailsUnitTestMixin)
class SearchServiceSearchOrderSpec extends Specification {

    void "test order all first results are group1"() {
		given: "offset, max and totals"
		int offset = 0
		int max = SearchService.SHOW_FIRST_COUNT
		int group1Tot = SearchService.SHOW_FIRST_COUNT + 10
		int group2Tot = group1Tot
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "all results are group1"
		results.offset1 == offset
		results.max1 == max
		results.offset2 == 0
		results.max2 == 0
		for (int i = 0; i < max; ++i) {
			results.orderList[i] == 1
		}
    }
	
	void "test order all first results are group2 when no group1"() {
		given: "offset, max and totals"
		int offset = 0
		int max = SearchService.SHOW_FIRST_COUNT
		int group1Tot = 0
		int group2Tot = SearchService.SHOW_FIRST_COUNT + 10
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "all results are group2"
		results.offset1 == offset
		results.max1 == 0
		results.offset2 == 0
		results.max2 == max
		for (int i = 0; i < max; ++i) {
			results.orderList[i] == 2
		}
	}
	
	void "test order some first results are group1, then group2 when all group1 used"() {
		given: "offset, max and totals"
		int offset = 0
		int max = SearchService.SHOW_FIRST_COUNT
		int group1Tot = 2
		int group2Tot = SearchService.SHOW_FIRST_COUNT + 10
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "results start with group1 and finish with group2"
		results.offset1 == 0
		results.max1 == 2
		results.offset2 == 0
		results.max2 == max - results.max1
		results.orderList[0] == 1
		results.orderList[1] == 1
		for (int i = 2; i < max; ++i) {
			results.orderList[i] == 2
		}
	}
	
	void "test order all results are group1 when no group2"() {
		given: "offset, max and totals"
		int offset = 0
		int max = SearchService.SHOW_FIRST_COUNT * 2
		int group1Tot = SearchService.SHOW_FIRST_COUNT * 2 + 10
		int group2Tot = 0
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "all results are group1"
		results.offset1 == offset
		results.max1 == max
		results.offset2 == 0
		results.max2 == 0
		for (int i = 0; i < max; ++i) {
			results.orderList[i] == 1
		}
	}
	
	void "test order all results are group2 when no group1"() {
		given: "offset, max and totals"
		int offset = 0
		int max = SearchService.SHOW_FIRST_COUNT * 2
		int group1Tot = 0
		int group2Tot = SearchService.SHOW_FIRST_COUNT * 2 + 10
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "all results are group2"
		results.offset1 == 0
		results.max1 == 0
		results.offset2 == offset
		results.max2 == max
		for (int i = 0; i < max; ++i) {
			results.orderList[i] == 2
		}
	}
	
	void "test order alternates between group1 and group2 after first results"() {
		given: "offset, max and totals"
		int offset = SearchService.SHOW_FIRST_COUNT
		int max = SearchService.SHOW_FIRST_COUNT * 2
		int group1Tot = SearchService.SHOW_FIRST_COUNT * 3
		int group2Tot = group1Tot
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "results alternate between group1 and group2"
		results.offset1 == SearchService.SHOW_FIRST_COUNT
		results.max1 == max / 2
		results.offset2 == 0
		results.max2 == max / 2
		for (int i = 0; i < max; ++i) {
			results.orderList[i] == ((i%2) == 0) ? 1 : 2
		}
	}
	
	void "test order alternates between group1 and group2 after first results then all group1 when group2 runs out"() {
		given: "offset, max and totals"
		int offset = SearchService.SHOW_FIRST_COUNT
		int max = SearchService.SHOW_FIRST_COUNT * 2
		int group1Tot = SearchService.SHOW_FIRST_COUNT * 3
		int group2Tot = 2
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "results alternate between group1 and group2, but finish with group1 when group2 runs out"
		results.offset1 == SearchService.SHOW_FIRST_COUNT
		results.max1 == max - group2Tot
		results.offset2 == 0
		results.max2 == group2Tot
		results.orderList[0] == 2
		results.orderList[1] == 1
		results.orderList[2] == 2
		for (int i = 3; i < max; ++i) {
			results.orderList[i] == 1
		}
	}
	
	void "test order alternates between group1 and group2 after first results then all group2 when group1 runs out"() {
		given: "offset, max and totals"
		int offset = SearchService.SHOW_FIRST_COUNT
		int max = SearchService.SHOW_FIRST_COUNT * 2
		int group1Tot = SearchService.SHOW_FIRST_COUNT + 2
		int group2Tot = SearchService.SHOW_FIRST_COUNT * 3
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "results alternate between group1 and group2, but finish with group2 when group1 runs out"
		results.offset1 == SearchService.SHOW_FIRST_COUNT
		results.max1 == 2
		results.offset2 == 0
		results.max2 == max - results.max1
		results.orderList[0] == 2
		results.orderList[1] == 1
		results.orderList[2] == 2
		results.orderList[3] == 1
		for (int i = 4; i < max; ++i) {
			results.orderList[i] == 2
		}
	}
	
	void "test no results returned when no group1 or group2 with no offset"() {
		given: "offset, max and totals"
		int offset = 0
		int max = SearchService.SHOW_FIRST_COUNT * 2
		int group1Tot = 0
		int group2Tot = 0
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "empty results returned"
		results.offset1 == 0
		results.max1 == 0
		results.offset2 == 0
		results.max2 == 0
		results.orderList.size == 0
	}
	
	void "test no results returned when no group1 or group2 with offset"() {
		given: "offset, max and totals"
		int offset = SearchService.SHOW_FIRST_COUNT
		int max = SearchService.SHOW_FIRST_COUNT * 2
		int group1Tot = 0
		int group2Tot = 0
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "empty results returned"
		results.offset1 == 0
		results.max1 == 0
		results.offset2 == 0
		results.max2 == 0
		results.orderList.size == 0
	}
	
	void "test orderList size is smaller than max when not enough group1 and group2"() {
		given: "offset, max and totals"
		int offset = 0
		int max = SearchService.SHOW_FIRST_COUNT + 10
		int group1Tot = SearchService.SHOW_FIRST_COUNT + 2
		int group2Tot = 2
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "results are less than max"
		results.offset1 == 0
		results.max1 == group1Tot
		results.offset2 == 0
		results.max2 == group2Tot
		max > (group1Tot + group2Tot)
		results.orderList.size == group1Tot + group2Tot
	}
	
	void "test orderList size is smaller than max when not enough group1 and group2 with offset"() {
		given: "offset, max and totals"
		int offset = SearchService.SHOW_FIRST_COUNT
		int max = SearchService.SHOW_FIRST_COUNT + 10
		int group1Tot = SearchService.SHOW_FIRST_COUNT + 2
		int group2Tot = 2
		
		when: "getSearchOrder is called"
		def results = SearchService.getSearchOrder(offset, max, group1Tot, group2Tot)
		
		then: "results are less than max"
		results.offset1 == SearchService.SHOW_FIRST_COUNT
		results.max1 == 2
		results.offset2 == 0
		results.max2 == group2Tot
		max > (2 + group2Tot)
		results.orderList.size == 2 + group2Tot
	}

}

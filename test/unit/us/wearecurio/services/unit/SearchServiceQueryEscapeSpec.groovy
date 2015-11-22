package us.wearecurio.services.unit

//import grails.test.mixin.TestMixin
//import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification
import grails.test.spock.*

import us.wearecurio.services.SearchQueryService

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
//@TestMixin(GrailsUnitTestMixin)
class SearchServiceQueryEscapeSpec extends Specification {

    void "test + is escaped"() {
		given: "a query string"
		String query = "+"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\+* OR #\+*)/
    }
	
	void "test - is escaped"() {
		given: "a query string"
		String query = "-"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\-* OR #\-*)/
	}

	void "test = is escaped"() {
		given: "a query string"
		String query = "="
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\=* OR #\=*)/
	}

	void "test > is escaped"() {
		given: "a query string"
		String query = ">"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\>* OR #\>*)/
	}

	void "test < is escaped"() {
		given: "a query string"
		String query = "<"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\<* OR #\<*)/
	}

	void "test ! is escaped"() {
		given: "a query string"
		String query = "!"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\!* OR #\!*)/
	}

	void "test & is escaped"() {
		given: "a query string"
		String query = "&"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\&* OR #\&*)/
	}

	void "test | is escaped"() {
		given: "a query string"
		String query = "|"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\|* OR #\|*)/
	}

	void "test \\ is escaped"() {
		given: "a query string"
		String query = "\\"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == "(\\* OR #\\*)"
	}

	void "test \" is escaped"() {
		given: "a query string"
		String query = "\""
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\"* OR #\"*)/
	}

	void "test ) is escaped"() {
		given: "a query string"
		String query = ")"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\)* OR #\)*)/
	}

	void "test { is escaped"() {
		given: "a query string"
		String query = "{"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\{* OR #\{*)/
	}

	void "test } is escaped"() {
		given: "a query string"
		String query = "}"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\}* OR #\}*)/
	}

	void "test [ is escaped"() {
		given: "a query string"
		String query = "["
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\[* OR #\[*)/
	}

	void "test ] is escaped"() {
		given: "a query string"
		String query = "]"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\]* OR #\]*)/
	}

	void "test ^ is escaped"() {
		given: "a query string"
		String query = "^"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\^* OR #\^*)/
	}

	void "test ~ is escaped"() {
		given: "a query string"
		String query = "~"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\~* OR #\~*)/
	}

	void "test * is escaped"() {
		given: "a query string"
		String query = "*"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\** OR #\**)/
	}

	void "test ? is escaped"() {
		given: "a query string"
		String query = "?"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\?* OR #\?*)/
	}

	void "test : is escaped"() {
		given: "a query string"
		String query = ":"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == /(\:* OR #\:*)/
	}

	void "test / is escaped"() {
		given: "a query string"
		String query = "/"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid andified query is produced"
		normalizedQuery == "(\\/* OR #\\/*)"
	}

}

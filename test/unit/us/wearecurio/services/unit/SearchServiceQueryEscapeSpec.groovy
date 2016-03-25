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

    @spock.lang.Unroll
    void "test '#query' is escaped"() {
        when: "query is normalized"
        String normalizedQuery = SearchQueryService.normalizeQuery(query)
        println normalizedQuery
        
        then: "normalized query is valid"
        normalizedQuery == expected
        
        where:
        query   |   expected
        "+"     |   /(\+* OR #\+*)/
        "-"     |   /(\-* OR #\-*)/
    }
    
    void "test + is escaped"() {
		given: "a query string"
		String query = "+"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\+* OR #\+*)/
    }
	
	void "test - is escaped"() {
		given: "a query string"
		String query = "-"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\-* OR #\-*)/
	}

	void "test = is escaped"() {
		given: "a query string"
		String query = "="
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\=* OR #\=*)/
	}

	void "test > is escaped"() {
		given: "a query string"
		String query = ">"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\>* OR #\>*)/
	}

	void "test < is escaped"() {
		given: "a query string"
		String query = "<"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\<* OR #\<*)/
	}

	void "test ! is escaped"() {
		given: "a query string"
		String query = "!"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\!* OR #\!*)/
	}

	void "test & is escaped"() {
		given: "a query string"
		String query = "&"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\&* OR #\&*)/
	}

	void "test | is escaped"() {
		given: "a query string"
		String query = "|"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\|* OR #\|*)/
	}

	void "test \\ is escaped"() {
		given: "a query string"
		String query = "\\"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == "(\\* OR #\\*)"
	}

	void "test \" is escaped"() {
		given: "a query string"
		String query = /"/
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\"* OR #\"*)/
	}

	void "test ) is escaped"() {
		given: "a query string"
		String query = ")"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\)* OR #\)*)/
	}

	void "test { is escaped"() {
		given: "a query string"
		String query = "{"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\{* OR #\{*)/
	}

	void "test } is escaped"() {
		given: "a query string"
		String query = "}"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\}* OR #\}*)/
	}

	void "test [ is escaped"() {
		given: "a query string"
		String query = "["
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\[* OR #\[*)/
	}

	void "test ] is escaped"() {
		given: "a query string"
		String query = "]"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\]* OR #\]*)/
	}

	void "test ^ is escaped"() {
		given: "a query string"
		String query = "^"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\^* OR #\^*)/
	}

	void "test ~ is escaped"() {
		given: "a query string"
		String query = "~"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\~* OR #\~*)/
	}

	void "test * is escaped"() {
		given: "a query string"
		String query = "*"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\** OR #\**)/
	}

	void "test ? is escaped"() {
		given: "a query string"
		String query = "?"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\?* OR #\?*)/
	}

	void "test : is escaped"() {
		given: "a query string"
		String query = ":"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == /(\:* OR #\:*)/
	}

	void "test / is escaped"() {
		given: "a query string"
		String query = "/"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "valid orified query is produced"
		normalizedQuery == "(\\/* OR #\\/*)"
	}

	@spock.lang.Unroll
	void "test escapeString('#inputString') returns '#expected'"() {
		when: "escapeString is called"
		String result = SearchQueryService.escapeString(inputString)
		
		then: "resulting string is escaped"
		result == expected
		
		where:
		inputString 	| expected
		"+"				| 	/\+/
		"-"				|	/\-/
		"="				|	/\=/
		">"				|	/\>/
		"<"				|	/\</
		"!"				|	/\!/
		"&"				|	/\&/
		"|"				|	/\|/
		"("				|	/\(/
		")"				|	/\)/
		"{"				|	/\{/
		"}"				|	/\}/
		"["				|	/\[/
		"]"				|	/\]/
		"^"				|	/\^/
		"~"				|	/\~/
		"*"				|	/\*/
		"?"				|	/\?/
		":"				|	/\:/
		"/"				|	"\\/"
		"\""			|	"\\\""
		"chronic granulomatous disease"	| "chronic granulomatous disease"
		"me/cfs"		|	"me\\/cfs"
		"aynaud's"		|	"aynaud's"
	}
	
}

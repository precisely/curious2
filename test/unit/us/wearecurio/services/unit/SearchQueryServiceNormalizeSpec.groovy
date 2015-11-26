package us.wearecurio.services.unit

//import grails.test.mixin.TestMixin
//import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.services.SearchQueryService

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
//@TestMixin(GrailsUnitTestMixin)
class SearchQueryServiceNormalizeSpec extends Specification {
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test '#query' returns '#expected'"() {
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == expected
		
		where:
		query   			|   expected
		"foo"  				| 	"(foo* OR #foo*)"
		"#foo" 				|   "#foo*"
		"#foo*" 			|   /#foo\**/
		"foo foo bar foo"	|	"((foo* OR #foo*) AND (bar* OR #bar*))"
		"foo bar"			| 	"((foo* OR #foo*) AND (bar* OR #bar*))"
		'foo bar baz'		|	"((foo* OR #foo*) AND (bar* OR #bar*) AND (baz* OR #baz*))"
		'#foo bar baz'		|	"(#foo* AND (bar* OR #bar*) AND (baz* OR #baz*))"
		'foo #bar baz'		|	"((foo* OR #foo*) AND #bar* AND (baz* OR #baz*))"
		'foo bar #baz'		|	"((foo* OR #foo*) AND (bar* OR #bar*) AND #baz*)"
		'#foo bar #baz'		|	"(#foo* AND (bar* OR #bar*) AND #baz*)"
		'#foo #bar baz'		|	"(#foo* AND #bar* AND (baz* OR #baz*))"
		'foo #bar #baz'		|	"((foo* OR #foo*) AND #bar* AND #baz*)"
		'#foo #bar #baz'	|	"(#foo* AND #bar* AND #baz*)"
		'"foo #bar baz"'	| 	"\"foo #bar baz\""
		'foo "#bar baz"'	| 	/((foo* OR #foo*) AND (\"#bar* OR #\"#bar*) AND (baz\"* OR #baz\"*))/
	}
	
	//@spock.lang.IgnoreRest
	@spock.lang.Unroll
	void "test '#query' returns empty string"() {
		when: "query is normalized"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		println normalizedQuery
		
		then: "normalized query is valid"
		normalizedQuery == ""
		
		where:
		query << [
			null,
			"",
			" ",
			"       "
		]
	}

	
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
		"="		|	/(\=* OR #\=*)/
		">"		|	/(\>* OR #\>*)/
		"<"		|	/(\<* OR #\<*)/
		"!"		|	/(\!* OR #\!*)/
		"&"		|	/(\&* OR #\&*)/
		"|"		|	/(\|* OR #\|*)/
		"\\"	|	"(\\* OR #\\*)"
		"\""	|	/(\"* OR #\"*)/
		"("		|	/(\(* OR #\(*)/
		")"		|	/(\)* OR #\)*)/
		"{"		|	/(\{* OR #\{*)/
		"}"		|	/(\}* OR #\}*)/
		"["		|	/(\[* OR #\[*)/
		"]"		|	/(\]* OR #\]*)/
		"^"		|	/(\^* OR #\^*)/
		"~"		|	/(\~* OR #\~*)/
		"*"		|	/(\** OR #\**)/
		"?"		|	/(\?* OR #\?*)/
		":"		|	/(\:* OR #\:*)/
		"/"		|	"(\\/* OR #\\/*)"
		'f*o'	|	/(f\*o* OR #f\*o*)/
	}
}

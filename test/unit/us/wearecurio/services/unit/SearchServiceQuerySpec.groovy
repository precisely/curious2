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
class SearchServiceQuerySpec extends Specification {

	@spock.lang.IgnoreRest
	void "test simple query generates wildcard and hash query string"(){
		given: "a query string"
		String query = "foo"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "(foo* OR #foo*)"
	}
	
	@spock.lang.IgnoreRest
	void "test hash query generates only generates hash query string"(){
		given: "a query string"
		String query = "#foo"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "#foo*"
	}
	
	@spock.lang.IgnoreRest
	void "test duplicates generates only generates single query string"(){
		given: "a query string"
		String query = "foo foo bar foo"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
        
		then: "valid normalized query is produced"
		normalizedQuery == "((foo* OR #foo*) AND (bar* OR #bar*))"
	}
	
	void "test null query returns empty string"() {
		given: "a query string"
		String query = null
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

    void "test empty string returns empty string"() {
		given: "a query string"
		String query = ""
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
    }

    void "test single word with and in name"() {
		given: "a query string"
		String query = "bandaid"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "bandaid"
    }

    void "test single word with AND in name"() {
		given: "a query string"
		String query = "bANDaid"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "bANDaid"
    }

    void "test single word with aNd in name"() {
		given: "a query string"
		String query = "baNdaid"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "baNdaid"
    }

    void "test single word beginning with and"() {
		given: "a query string"
		String query = "anderson"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "anderson"
    }

    void "test single word beginning with AND"() {
		given: "a query string"
		String query = "ANDerson"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "ANDerson"
    }

    void "test single word beginning with aNd"() {
		given: "a query string"
		String query = "aNderson"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "aNderson"
    }

    void "test single word ending with and"() {
		given: "a query string"
		String query = "band"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "band"
    }

    void "test single word ending with AND"() {
		given: "a query string"
		String query = "bAND"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "bAND"
    }

    void "test single word ending with aNd"() {
		given: "a query string"
		String query = "baNd"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "baNd"
    }

     void "test single word with or in name"() {
		given: "a query string"
		String query = "bore"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "bore"
    }

    void "test single word with OR in name"() {
		given: "a query string"
		String query = "bORe"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "bORe"
    }

    void "test single word with oR in name"() {
		given: "a query string"
		String query = "boRe"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "boRe"
    }

    void "test single word beginning with or"() {
		given: "a query string"
		String query = "orson"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "orson"
    }

    void "test single word beginning with OR"() {
		given: "a query string"
		String query = "ORson"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "ORson"
    }

    void "test single word beginning with oR"() {
		given: "a query string"
		String query = "oRson"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "oRson"
    }

    void "test single word ending with or"() {
		given: "a query string"
		String query = "evaluator"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "evaluator"
    }

    void "test single word ending with OR"() {
		given: "a query string"
		String query = "evaluatOR"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "evaluatOR"
    }

    void "test single word ending with oR"() {
		given: "a query string"
		String query = "evaluatoR"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "evaluatoR"
    }

  void "test single word with andor in name"() {
		given: "a query string"
		String query = "blahandorblah"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "blahandorblah"
    }

   void "test single word with andorand in name"() {
		given: "a query string"
		String query = "blahandorandblah"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "blahandorandblah"
    }

   void "test single word with orandor in name"() {
		given: "a query string"
		String query = "blahorandorblah"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "blahorandorblah"
    }

   void "test multiple words with first ending in or"() {
		given: "a query string"
		String query = "evaluator in training"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "evaluator AND in AND training"
    }

   void "test multiple words with first beginning in or"() {
		given: "a query string"
		String query = "orson welles"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "orson AND welles"
    }

   void "test multiple words with last ending in or"() {
		given: "a query string"
		String query = "music evaluator"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "music AND evaluator"
    }

   void "test multiple words with first beginning in and"() {
		given: "a query string"
		String query = "anderson cooper"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "anderson AND cooper"
    }

   void "test multiple words with last ending in and"() {
		given: "a query string"
		String query = "big band"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "big AND band"
    }

   void "test multiple words with first beginning in or and last ending in and"() {
		given: "a query string"
		String query = "orson welles big band"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "orson AND welles AND big AND band"
    }

    void "test single word"() {
		given: "a query string"
		String query = "simple"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "simple"
    }

    void "test two word query string"() {
		given: "a query string"
		String query = "word1 word2"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2"
    }

    void "test three word query string"() {
		given: "a query string"
		String query = "word1 word2 word3"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2 AND word3"
    }

    void "test query with or in string"() {
		given: "a query string"
		String query = "word1 or word2"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2"
    }

    void "test query with OR in string"() {
		given: "a query string"
		String query = "word1 OR word2"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2"
    }

    void "test query with oR in string"() {
		given: "a query string"
		String query = "word1 oR word2"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2"
    }

    void "test query with and in string"() {
		given: "a query string"
		String query = "word1 and word2"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2"
    }

    void "test query with AND in string"() {
		given: "a query string"
		String query = "word1 AND word2"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2"
    }

    void "test query with aNd in string"() {
		given: "a query string"
		String query = "word1 aNd word2"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2"
    }

    void "test multiple words with trailing spaces"() {
		given: "a query string"
		String query = "word1 word2 word3            "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with leading spaces"() {
		given: "a query string"
		String query = "             word1 word2 word3"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2 AND word3"
    }

    void "test single word with trailing spaces"() {
		given: "a query string"
		String query = "simple             "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "simple"
    }

    void "test single word with leading spaces"() {
		given: "a query string"
		String query = "            simple"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "simple"
    }

    void "test multiple words with trailing, middle and trailing spaces"() {
		given: "a query string"
		String query = "            word1          word2      word3               "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with trailing, middle and trailing spaces with or in between"() {
		given: "a query string"
		String query = "            word1     or     word2      word3               "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with trailing, middle and trailing spaces with and in between"() {
		given: "a query string"
		String query = "            word1   and       word2      word3               "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with trailing, middle and trailing spaces with ors and ands in beginning, middle and end"() {
		given: "a query string"
		String query = "   and or or         word1    or      word2    and  word3       or and        "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == "word1 AND word2 AND word3"
    }
	
	void "test query of and returns empty string"() {
		given: "a query string"
		String query = "and"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of and with leading and trailing spaces returns empty string"() {
		given: "a query string"
		String query = "            and        "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of or returns empty string"() {
		given: "a query string"
		String query = "or"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of or with leading and trailing spaces returns empty string"() {
		given: "a query string"
		String query = "            or        "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of with multiple ands and ors returns empty string"() {
		given: "a query string"
		String query = "and or or and and or"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of with all ors separated by spaces returns empty string"() {
		given: "a query string"
		String query = "or or or"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of with all ors separated by spaces with space at beginning returns empty string"() {
		given: "a query string"
		String query = " or or or"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of with all ors separated by spaces with space at end returns empty string"() {
		given: "a query string"
		String query = "or or or "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}
	
	void "test query of with all ands separated by spaces returns empty string"() {
		given: "a query string"
		String query = "and and and"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of with all ands separated by spaces with space at beginning returns empty string"() {
		given: "a query string"
		String query = " and and and"
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}

	void "test query of with all ands separated by spaces with space at end returns empty string"() {
		given: "a query string"
		String query = "and and and "
		
		when: "SearchQueryService.normalizeQuery is called"
		String normalizedQuery = SearchQueryService.normalizeQuery(query)
		
		then: "valid normalized query is produced"
		normalizedQuery == ""
	}
}

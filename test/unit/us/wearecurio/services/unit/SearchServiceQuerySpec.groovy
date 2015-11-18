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
class SearchServiceQuerySpec extends Specification {

	void "test null query returns empty string"() {
		given: "a query string"
		String query = null
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

    void "test empty string returns empty string"() {
		given: "a query string"
		String query = ""
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
    }

    void "test single word with and in name"() {
		given: "a query string"
		String query = "bandaid"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "bandaid"
    }

    void "test single word with AND in name"() {
		given: "a query string"
		String query = "bANDaid"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "bANDaid"
    }

    void "test single word with aNd in name"() {
		given: "a query string"
		String query = "baNdaid"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "baNdaid"
    }

    void "test single word beginning with and"() {
		given: "a query string"
		String query = "anderson"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "anderson"
    }

    void "test single word beginning with AND"() {
		given: "a query string"
		String query = "ANDerson"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "ANDerson"
    }

    void "test single word beginning with aNd"() {
		given: "a query string"
		String query = "aNderson"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "aNderson"
    }

    void "test single word ending with and"() {
		given: "a query string"
		String query = "band"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "band"
    }

    void "test single word ending with AND"() {
		given: "a query string"
		String query = "bAND"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "bAND"
    }

    void "test single word ending with aNd"() {
		given: "a query string"
		String query = "baNd"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "baNd"
    }

     void "test single word with or in name"() {
		given: "a query string"
		String query = "bore"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "bore"
    }

    void "test single word with OR in name"() {
		given: "a query string"
		String query = "bORe"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "bORe"
    }

    void "test single word with oR in name"() {
		given: "a query string"
		String query = "boRe"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "boRe"
    }

    void "test single word beginning with or"() {
		given: "a query string"
		String query = "orson"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "orson"
    }

    void "test single word beginning with OR"() {
		given: "a query string"
		String query = "ORson"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "ORson"
    }

    void "test single word beginning with oR"() {
		given: "a query string"
		String query = "oRson"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "oRson"
    }

    void "test single word ending with or"() {
		given: "a query string"
		String query = "evaluator"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "evaluator"
    }

    void "test single word ending with OR"() {
		given: "a query string"
		String query = "evaluatOR"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "evaluatOR"
    }

    void "test single word ending with oR"() {
		given: "a query string"
		String query = "evaluatoR"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "evaluatoR"
    }

  void "test single word with andor in name"() {
		given: "a query string"
		String query = "blahandorblah"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "blahandorblah"
    }

   void "test single word with andorand in name"() {
		given: "a query string"
		String query = "blahandorandblah"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "blahandorandblah"
    }

   void "test single word with orandor in name"() {
		given: "a query string"
		String query = "blahorandorblah"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "blahorandorblah"
    }

   void "test multiple words with first ending in or"() {
		given: "a query string"
		String query = "evaluator in training"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "evaluator AND in AND training"
    }

   void "test multiple words with first beginning in or"() {
		given: "a query string"
		String query = "orson welles"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "orson AND welles"
    }

   void "test multiple words with last ending in or"() {
		given: "a query string"
		String query = "music evaluator"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "music AND evaluator"
    }

   void "test multiple words with first beginning in and"() {
		given: "a query string"
		String query = "anderson cooper"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "anderson AND cooper"
    }

   void "test multiple words with last ending in and"() {
		given: "a query string"
		String query = "big band"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "big AND band"
    }

   void "test multiple words with first beginning in or and last ending in and"() {
		given: "a query string"
		String query = "orson welles big band"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "orson AND welles AND big AND band"
    }

    void "test single word"() {
		given: "a query string"
		String query = "simple"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "simple"
    }

    void "test two word query string"() {
		given: "a query string"
		String query = "word1 word2"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2"
    }

    void "test three word query string"() {
		given: "a query string"
		String query = "word1 word2 word3"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2 AND word3"
    }

    void "test query with or in string"() {
		given: "a query string"
		String query = "word1 or word2"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2"
    }

    void "test query with OR in string"() {
		given: "a query string"
		String query = "word1 OR word2"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2"
    }

    void "test query with oR in string"() {
		given: "a query string"
		String query = "word1 oR word2"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2"
    }

    void "test query with and in string"() {
		given: "a query string"
		String query = "word1 and word2"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2"
    }

    void "test query with AND in string"() {
		given: "a query string"
		String query = "word1 AND word2"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2"
    }

    void "test query with aNd in string"() {
		given: "a query string"
		String query = "word1 aNd word2"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2"
    }

    void "test multiple words with trailing spaces"() {
		given: "a query string"
		String query = "word1 word2 word3            "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with leading spaces"() {
		given: "a query string"
		String query = "             word1 word2 word3"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2 AND word3"
    }

    void "test single word with trailing spaces"() {
		given: "a query string"
		String query = "simple             "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "simple"
    }

    void "test single word with leading spaces"() {
		given: "a query string"
		String query = "            simple"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "simple"
    }

    void "test multiple words with trailing, middle and trailing spaces"() {
		given: "a query string"
		String query = "            word1          word2      word3               "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with trailing, middle and trailing spaces with or in between"() {
		given: "a query string"
		String query = "            word1     or     word2      word3               "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with trailing, middle and trailing spaces with and in between"() {
		given: "a query string"
		String query = "            word1   and       word2      word3               "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2 AND word3"
    }

    void "test multiple words with trailing, middle and trailing spaces with ors and ands in beginning, middle and end"() {
		given: "a query string"
		String query = "   and or or         word1    or      word2    and  word3       or and        "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == "word1 AND word2 AND word3"
    }
	
	void "test query of and returns empty string"() {
		given: "a query string"
		String query = "and"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of and with leading and trailing spaces returns empty string"() {
		given: "a query string"
		String query = "            and        "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of or returns empty string"() {
		given: "a query string"
		String query = "or"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of or with leading and trailing spaces returns empty string"() {
		given: "a query string"
		String query = "            or        "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of with multiple ands and ors returns empty string"() {
		given: "a query string"
		String query = "and or or and and or"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of with all ors separated by spaces returns empty string"() {
		given: "a query string"
		String query = "or or or"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of with all ors separated by spaces with space at beginning returns empty string"() {
		given: "a query string"
		String query = " or or or"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of with all ors separated by spaces with space at end returns empty string"() {
		given: "a query string"
		String query = "or or or "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}
	
	void "test query of with all ands separated by spaces returns empty string"() {
		given: "a query string"
		String query = "and and and"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of with all ands separated by spaces with space at beginning returns empty string"() {
		given: "a query string"
		String query = " and and and"
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}

	void "test query of with all ands separated by spaces with space at end returns empty string"() {
		given: "a query string"
		String query = "and and and "
		
		when: "SearchService.normalizeQuery is called"
		String andifiedQuery = SearchService.normalizeQuery(query)
		
		then: "valid andified query is produced"
		andifiedQuery == ""
	}
}

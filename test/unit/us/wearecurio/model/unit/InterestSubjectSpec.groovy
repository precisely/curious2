package us.wearecurio.model.unit

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.model.InterestSubject

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock(InterestSubject)
class InterestSubjectSpec extends Specification {
	static String name = "subject"
	static String description = "description"
	static List interestTags = ["tag1", "tag2"]
	static List bookmarks = ["sleep", "coffee"]
	
	@spock.lang.Unroll
	void "test scrubList(#input) returns empty list"() {
		when: "scrubList is called"
		def result = InterestSubject.scrubList(input)

		then: "empty list is returned"
		result != null
		result.size == 0
		
		where:
		input	<< [
		null,
		[],
		[36],
		[null],
		[""],
		[null,"",1237]
		]
	}
	
	@spock.lang.Unroll
	void "test scrubList(#input) returns '#expected'"() {
		when: "scrubList is called"
		def result = InterestSubject.scrubList(input)
		
		then: "valid list is returned"
		result
		result.size == expected.size
		result.equals(expected)
		
		where:
		input									|		expected
		["item"]								|		["item"]
		["item1","item2"]						|		["item1","item2"]
		["item1","",null,"item2",123,"item3"]	|		["item1","item2","item3"]
	}

	@spock.lang.Unroll
    void "test createInterestSubject(#name, #tags, #bkmarks, #desc, #save) return null"() {
		when: "createInterestSubject is called"
		InterestSubject s = InterestSubject.createInterestSubject(
			null,
			interestTags,
			bookmarks,
			"",
			false
		)
		
		then: "a null object is returned"
		s == null
		
		where:
		name	| tags			| bkmarks	| desc	| save
		null	| interestTags	| bookmarks	| ""	| false
		""		| interestTags	| bookmarks | ""	| false
		name	| null			| null		| ""	| false
		name	| null			| [null]	| ""	| false
		name	| null			| [""]		| ""	| false
		name	| [null]		| null		| ""	| false
		name	| [""]			| null		| ""	| false
		name	| [""]			| [""]		| ""	| false
    }

    void "test createInterestSubject with existing name"() {
		given: "a subject"
		InterestSubject.createInterestSubject(
			name, 
			interestTags,
			bookmarks,
			description,
			true)
		
		when: "createInterestSubject is called with same name"
		InterestSubject s = InterestSubject.createInterestSubject(
			name,
			interestTags,
			bookmarks
		)
		
		then: "a null object is returned"
		s == null
    }

    void "test createInterestSubject with valid name"() {
		when: "createInterestSubject is called"
		InterestSubject s = InterestSubject.createInterestSubject(
			name,
			interestTags,
			bookmarks,
			"",
			false
		)
		
		then: "a valid object is returned"
		s
		s.name == name
		s.interestTags.size() == 2
		s.interestTags.find{interestTags[0]}
		s.interestTags.find{interestTags[1]}
		s.bookmarks.size() == 2
		s.bookmarks.find{bookmarks[0]}
		s.bookmarks.find{bookmarks[1]}
    }

    void "test createInterestSubject with null interestTags and valid bookmarks"() {
		when: "createInterestSubject is called"
		InterestSubject s = InterestSubject.createInterestSubject(
			name,
			null,
			bookmarks,
			"",
			false
		)
		
		then: "a valid object is returned"
		s
		s.name == name
		s.interestTags == null
		s.bookmarks.size() == 2
		s.bookmarks.find{bookmarks[0]}
		s.bookmarks.find{bookmarks[1]}
    }

    void "test createInterestSubject with valid interestTags and null bookmarks"() {
		when: "createInterestSubject is called"
		InterestSubject s = InterestSubject.createInterestSubject(
			name,
			interestTags,
			null,
			"",
			false
		)
		
		then: "a valid object is returned"
		s
		s.name == name
		s.interestTags.size() == 2
		s.interestTags.find{it == interestTags[0]}
		s.interestTags.find{it == interestTags[1]}
		s.bookmarks == null
    }

    void "test createInterestSubject with description"() {
		when: "createInterestSubject is called"
		InterestSubject s = InterestSubject.createInterestSubject(
			name,
			interestTags,
			bookmarks,
			description,
			false
		)
		
		then: "a valid object is returned"
		s
		s.name == name
		s.description == description
		s.interestTags.size() == 2
		s.interestTags.find{it == interestTags[0]}
		s.interestTags.find{it == interestTags[1]}
		s.bookmarks.size() == 2
		s.bookmarks.find{it == bookmarks[0]}
		s.bookmarks.find{it == bookmarks[1]}
    }

    void "test createInterestSubject with save = false"() {
		when: "createInterestSubject is called"
		InterestSubject.createInterestSubject(
			name,
			interestTags,
			bookmarks,
			description,
			false
		)
		
		and: "InterestSubject searched by name"
		InterestSubject s = InterestSubject.findByName(name)
		
		then: "a null object is returned"
		s == null
    }

    void "test createInterestSubject with save = true"() {
		when: "createInterestSubject is called"
		InterestSubject.createInterestSubject(
			name,
			interestTags,
			bookmarks,
			description,
			true
		)
		
		and: "InterestSubject searched by name"
		InterestSubject s = InterestSubject.findByName(name)
		
		then: "a valid object is returned"
		s
		s.name == name
		s.description == description
		s.interestTags.size() == 2
		s.interestTags.find{it == interestTags[0]}
		s.interestTags.find{it == interestTags[1]}
		s.bookmarks.size() == 2
		s.bookmarks.find{it == bookmarks[0]}
		s.bookmarks.find{it == bookmarks[1]}
    }
}

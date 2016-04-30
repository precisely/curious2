package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.InterestSubject

class InterestSubjectIntegrationSpec extends IntegrationSpec {

    void "test createInterestSubject"() {
		given: "a name"
			String name = "testSubject"
		
		and: "a description"
			String description = "tags and bookmarks related to sleep"
		
		and: "interest tags"
		List interestTags = ["sleep", "caffeine"]
		
		and: "bookmarks"
		List bookmarks = ["sleep 8 hours"]
		
		when: "createInterestSubject is called"
		InterestSubject subject = InterestSubject.createInterestSubject(
			name,
			interestTags,
			bookmarks,
			description,
			true
		)
		
		and: "interest subject is searched by name"
		InterestSubject result = InterestSubject.findByName(name)
		
		then: "result matches parameters"
		result
		result.name == name
		result.description == description
		result.interestTags.size() == 2
		result.interestTags.find{interestTags[0]} != null
		result.interestTags.find{interestTags[1]} != null
		result.bookmarks.size() == 1
		result.bookmarks[0] == bookmarks[0]
    }
}

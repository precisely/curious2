package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.utility.Utils
import us.wearecurio.model.InterestArea
import us.wearecurio.model.InterestSubject
import us.wearecurio.model.InterestSubjectBoolean

class InterestAreaIntegrationSpec extends IntegrationSpec {

    void "test addToInterestSubjects"() {
		given: "a name"
		String name = "testArea"
		
		and: "a new InterestArea"
		InterestArea interest = InterestArea.create()
		interest.name = name
		Utils.save(interest,true)
		
		and: "a new InterestSubject"
		InterestSubject subject = InterestSubject.createInterestSubject(
			"testSubject",
			["sleep"],
			["sleep 8 hours"],
			"testing sleep",
			true
		)
		
		when: "addToInterestSubjects is called"
		interest.addToInterestSubjects(
			new InterestSubjectBoolean(
				subject,
				true
			)
		)
		
		and: "interest area is queried by name"
		InterestArea result = InterestArea.findByName(name)
		
		then: "result has an interest subject"
		result
		result.interestSubjects.size() == 1
		result.interestSubjects[0].subject.name == subject.name
		result.interestSubjects[0].subject.description == subject.description
		result.interestSubjects[0].subject.interestTags.size() == 1
		result.interestSubjects[0].subject.name == subject.name
		result.interestSubjects[0].subject.description == subject.description
		result.interestSubjects[0].subject.interestTags[0] == "sleep"
		result.interestSubjects[0].subject.bookmarks[0] == "sleep 8 hours"
		result.interestSubjects[0].preSelect
    }
}

package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.utility.Utils
import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.InterestArea
import us.wearecurio.model.InterestAreaBoolean
import us.wearecurio.model.InterestSubject
import us.wearecurio.model.InterestSubjectBoolean

class InitialLoginConfigurationIntegrationSpec extends IntegrationSpec {

    void "test addToInterestAreas"() {
		given: "a promo code"
		String promoCode = "testPromoCode"
		
		and: "an interest area name"
		String name = "sleep"
		
		and: "a new InitialLoginConfiguration based on default"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.create()
		ilc.promoCode = promoCode
		
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
		
		and: "the subject is added to the interestArea"
		interest.addToInterestSubjects(
			new InterestSubjectBoolean(
				subject,
				true
			)
		)
		
		when: "interestArea is added to the InitialLoginConfiguration"
		ilc.addToInterestAreas(
			new InterestAreaBoolean(
				interest,
				true
			)
		)
		
		and: "InitialLoginConfiguration is saved"
		Utils.save(ilc,true)
		
		and: "InitialLoginConfiguration is queried by name"
		InitialLoginConfiguration ilcFound = InitialLoginConfiguration.findByPromoCode(promoCode)
		
		then: "result has an interest area"
		ilcFound
		ilcFound.interestAreas.size() == 1
		ilcFound.interestAreas[0].interest.name == interest.name
		ilcFound.interestAreas[0].preSelect
		ilcFound.interestAreas[0].interest.interestSubjects[0].subject.interestTags.size() == 1
		ilcFound.interestAreas[0].interest.interestSubjects[0].subject.name == interest.interestSubjects[0].subject.name
		ilcFound.interestAreas[0].interest.interestSubjects[0].subject.description == interest.interestSubjects[0].subject.description
		ilcFound.interestAreas[0].interest.interestSubjects[0].subject.interestTags[0] == interest.interestSubjects[0].subject.interestTags[0]
		ilcFound.interestAreas[0].interest.interestSubjects[0].subject.bookmarks[0] == interest.interestSubjects[0].subject.bookmarks[0]
		ilcFound.interestAreas[0].interest.interestSubjects[0].preSelect
    }
}

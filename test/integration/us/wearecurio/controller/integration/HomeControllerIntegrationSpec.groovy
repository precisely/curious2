package us.wearecurio.controller.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.controller.LoginController
import us.wearecurio.utility.Utils

//NOTE: These are spock tests (my preferred form) and will focus on promo code stuff
class HomeControllerIntegrationSpec extends IntegrationSpec {

    def setup() {
    }

    def cleanup() {
    }

    void "test promo code found"() {
		given: "a promo code"
		String code = "test-promo"
		
		and: "text for a few fields"
		String trackExample1 = "trackExample1 test"
		String sampleQuestionRatingExampleAnswer1 = "sampleQuestionRatingExampleAnswer1 test"
		
		when: "that promo code is specified for new registered user"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.createFromDefault(code)

		and: "fields are set"
		ifc.trackExample1 = trackExample1
		ifc.sampleQuestionRatingExampleAnswer1 = sampleQuestionRatingExampleAnswer1
		
		and: "ifc is saved"
		Utils.save(ifc, flush=true)
		
		and: "controller is created"
		LoginController controller = new LoginController()
		
		and: "parameters are set"
		
		
		then: "appropriate message is returned in flash"
		and: "user is done"
    }
	
	void "test no promo code specified uses default"() {
	}
	
	void "test default promo code specified and default data returned"(){
	}
	
	void "test invalid promo code returns default with message in flash"(){
	}
}

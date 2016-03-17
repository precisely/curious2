package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User

import us.wearecurio.services.UserRegistrationService
import us.wearecurio.services.UserRegistrationService.PromoCodeLookupResult

class UserRegistrationServiceIntegrationSpec extends IntegrationSpec {
	User user
	UserRegistrationService userRegistrationService
	
    def setup() {
		user = User.create(
			[	username:'shane',
				sex:'F',
				name:'shane macgowen',
				email:'shane@pogues.com',
				birthdate:'01/01/1960',
				password:'shanexyz',
				action:'doregister',
				controller:'home'	]
		)
    }

    def cleanup() {
    }

    void "test getInitialLoginConfiguration with no promo code"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		when: "setUserPromoCode is called"
		Map results = userRegistrationService.getInitialLoginConfiguration(user)
		
		then: "results match the default promo object"
		results
		results.success
		results.initialLoginConfig.promoCode == defaultILC.promoCode 
		results.initialLoginConfig.customQuestion1 == defaultILC.customQuestion1
		results.initialLoginConfig.customQuestion2 == defaultILC.customQuestion2
		results.initialLoginConfig.trackExample1 == defaultILC.trackExample1
		results.initialLoginConfig.trackExample2 == defaultILC.trackExample2
		results.initialLoginConfig.trackExample3 == defaultILC.trackExample3
		results.initialLoginConfig.deviceExample == defaultILC.deviceExample
		results.initialLoginConfig.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		results.initialLoginConfig.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		results.initialLoginConfig.sampleQuestionRating == defaultILC.sampleQuestionRating
		results.initialLoginConfig.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		results.initialLoginConfig.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		results.initialLoginConfig.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		results.initialLoginConfig.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		results.initialLoginConfig.today1 == defaultILC.today1
		results.initialLoginConfig.today1Example == defaultILC.today1Example
		results.initialLoginConfig.today2 == defaultILC.today2
		results.initialLoginConfig.today2Example == defaultILC.today2Example
		results.initialLoginConfig.today3 == defaultILC.today3
		results.initialLoginConfig.today3Example == defaultILC.today3Example
		results.initialLoginConfig.today4 == defaultILC.today4
		results.initialLoginConfig.today4Example == defaultILC.today4Example
		results.initialLoginConfig.interestTags.equals(defaultILC.interestTags)
		results.initialLoginConfig.bookmarks.equals(defaultILC.bookmarks)
		results.promoCodeLookupResult == PromoCodeLookupResult.UNSPECIFIED
    }
	
    void "test getInitialLoginConfiguration with null promo code"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		when: "setUserPromoCode is called"
		Map results = userRegistrationService.getInitialLoginConfiguration(user, null)
		
		then: "results match the default promo object"
		results
		results.success
		results.initialLoginConfig.promoCode == defaultILC.promoCode 
		results.initialLoginConfig.customQuestion1 == defaultILC.customQuestion1
		results.initialLoginConfig.customQuestion2 == defaultILC.customQuestion2
		results.initialLoginConfig.trackExample1 == defaultILC.trackExample1
		results.initialLoginConfig.trackExample2 == defaultILC.trackExample2
		results.initialLoginConfig.trackExample3 == defaultILC.trackExample3
		results.initialLoginConfig.deviceExample == defaultILC.deviceExample
		results.initialLoginConfig.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		results.initialLoginConfig.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		results.initialLoginConfig.sampleQuestionRating == defaultILC.sampleQuestionRating
		results.initialLoginConfig.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		results.initialLoginConfig.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		results.initialLoginConfig.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		results.initialLoginConfig.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		results.initialLoginConfig.today1 == defaultILC.today1
		results.initialLoginConfig.today1Example == defaultILC.today1Example
		results.initialLoginConfig.today2 == defaultILC.today2
		results.initialLoginConfig.today2Example == defaultILC.today2Example
		results.initialLoginConfig.today3 == defaultILC.today3
		results.initialLoginConfig.today3Example == defaultILC.today3Example
		results.initialLoginConfig.today4 == defaultILC.today4
		results.initialLoginConfig.today4Example == defaultILC.today4Example
		results.initialLoginConfig.interestTags.equals(defaultILC.interestTags)
		results.initialLoginConfig.bookmarks.equals(defaultILC.bookmarks)
		results.promoCodeLookupResult == PromoCodeLookupResult.UNSPECIFIED		
    }

}

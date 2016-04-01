package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User

import us.wearecurio.services.UserRegistrationService
import us.wearecurio.services.UserRegistrationService.PromoCodeLookupResult

import us.wearecurio.utility.Utils

class UserRegistrationServiceIntegrationSpec extends IntegrationSpec {
	User user
	InitialLoginConfiguration defaultILC
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
		defaultILC = InitialLoginConfiguration.defaultConfiguration()
    }

    void "test register no promo code specified"() {
		when: "setUserPromoCode is called"
		Map results = userRegistrationService.register(user)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == PromoCodeLookupResult.UNSPECIFIED	

		and: "the default promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig		
		ilcResult.promoCode == defaultILC.promoCode 
		ilcResult.customQuestion1 == defaultILC.customQuestion1
		ilcResult.customQuestion2 == defaultILC.customQuestion2
		ilcResult.trackExample1 == defaultILC.trackExample1
		ilcResult.trackExample2 == defaultILC.trackExample2
		ilcResult.trackExample3 == defaultILC.trackExample3
		ilcResult.deviceExample == defaultILC.deviceExample
		ilcResult.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		ilcResult.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		ilcResult.sampleQuestionRating == defaultILC.sampleQuestionRating
		ilcResult.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		ilcResult.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		ilcResult.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		ilcResult.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		ilcResult.today1 == defaultILC.today1
		ilcResult.today1Example == defaultILC.today1Example
		ilcResult.today2 == defaultILC.today2
		ilcResult.today2Example == defaultILC.today2Example
		ilcResult.today3 == defaultILC.today3
		ilcResult.today3Example == defaultILC.today3Example
		ilcResult.today4 == defaultILC.today4
		ilcResult.today4Example == defaultILC.today4Example
		ilcResult.interestTags.equals(defaultILC.interestTags)
		ilcResult.bookmarks.equals(defaultILC.bookmarks)
		
		and: "user interest tags are set"
		defaultILC.interestTags.each{ user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set"
		def tags = User.getTags(user.id)
		tags
		tags.size == 1
		tags.find{ it.description == "sleep [time]" }
    }
	
    void "test register null promo code specified"() {
		when: "setUserPromoCode is called"
		Map results = userRegistrationService.register(user, null)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == PromoCodeLookupResult.UNSPECIFIED	

		and: "the default promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig		
		ilcResult.promoCode == defaultILC.promoCode 
		ilcResult.customQuestion1 == defaultILC.customQuestion1
		ilcResult.customQuestion2 == defaultILC.customQuestion2
		ilcResult.trackExample1 == defaultILC.trackExample1
		ilcResult.trackExample2 == defaultILC.trackExample2
		ilcResult.trackExample3 == defaultILC.trackExample3
		ilcResult.deviceExample == defaultILC.deviceExample
		ilcResult.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		ilcResult.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		ilcResult.sampleQuestionRating == defaultILC.sampleQuestionRating
		ilcResult.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		ilcResult.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		ilcResult.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		ilcResult.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		ilcResult.today1 == defaultILC.today1
		ilcResult.today1Example == defaultILC.today1Example
		ilcResult.today2 == defaultILC.today2
		ilcResult.today2Example == defaultILC.today2Example
		ilcResult.today3 == defaultILC.today3
		ilcResult.today3Example == defaultILC.today3Example
		ilcResult.today4 == defaultILC.today4
		ilcResult.today4Example == defaultILC.today4Example
		ilcResult.interestTags.equals(defaultILC.interestTags)
		ilcResult.bookmarks.equals(defaultILC.bookmarks)
		
		and: "user interest tags are set"
		defaultILC.interestTags.each{ user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set"
		def tags = User.getTags(user.id)
		tags
		tags.size == 1
		tags.find{ it.description == "sleep [time]" }
    }

    void "test register default promo code specified"() {
		when: "setUserPromoCode is called"
		Map results = userRegistrationService.register(user, InitialLoginConfiguration.DEFAULT_PROMO_CODE)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == PromoCodeLookupResult.FOUND	

		and: "the default promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig		
		ilcResult.promoCode == defaultILC.promoCode 
		ilcResult.customQuestion1 == defaultILC.customQuestion1
		ilcResult.customQuestion2 == defaultILC.customQuestion2
		ilcResult.trackExample1 == defaultILC.trackExample1
		ilcResult.trackExample2 == defaultILC.trackExample2
		ilcResult.trackExample3 == defaultILC.trackExample3
		ilcResult.deviceExample == defaultILC.deviceExample
		ilcResult.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		ilcResult.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		ilcResult.sampleQuestionRating == defaultILC.sampleQuestionRating
		ilcResult.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		ilcResult.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		ilcResult.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		ilcResult.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		ilcResult.today1 == defaultILC.today1
		ilcResult.today1Example == defaultILC.today1Example
		ilcResult.today2 == defaultILC.today2
		ilcResult.today2Example == defaultILC.today2Example
		ilcResult.today3 == defaultILC.today3
		ilcResult.today3Example == defaultILC.today3Example
		ilcResult.today4 == defaultILC.today4
		ilcResult.today4Example == defaultILC.today4Example
		ilcResult.interestTags.equals(defaultILC.interestTags)
		ilcResult.bookmarks.equals(defaultILC.bookmarks)
		
		and: "user interest tags are set"
		defaultILC.interestTags.each{ user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set"
		def tags = User.getTags(user.id)
		tags
		tags.size == 1
		tags.find{ it.description == "sleep [time]" }
    }

    //@spock.lang.IgnoreRest
	void "test register valid promo code specified"() {
		given: "a promo code"
		String code = "test-promo"
		
		and: "an InitialLoginConfiguration for that promo code"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.createFromDefault(code, false)
		
		and: "some non-default login configuration data"
		String trackExample1 = "trackExample1 test"
		String sampleQuestionRatingExampleAnswer1 = "sampleQuestionRatingExampleAnswer1 test"
		List interestTags = ["testInterestTag1", "testInterestTag2"]
		List bookmarks = ["run 100 miles", "eat 2000 calories"]
				
		and: "set non-default data on the new InitialLoginConfiguration"
		ilc.trackExample1 = trackExample1
		ilc.sampleQuestionRatingExampleAnswer1 = sampleQuestionRatingExampleAnswer1
		ilc.interestTags = interestTags
		ilc.bookmarks = bookmarks
		Utils.save(ilc, true)
		
		when: "register is called"
		Map results = userRegistrationService.register(user, code)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == PromoCodeLookupResult.FOUND	
		and: "the saved promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig		
		ilcResult.promoCode == ilc.promoCode 
		ilcResult.customQuestion1 == ilc.customQuestion1
		ilcResult.customQuestion2 == ilc.customQuestion2
		ilcResult.trackExample1 == ilc.trackExample1
		ilcResult.trackExample1 == trackExample1
		ilcResult.trackExample2 == ilc.trackExample2
		ilcResult.trackExample3 == ilc.trackExample3
		ilcResult.deviceExample == ilc.deviceExample
		ilcResult.sampleQuestionDuration == ilc.sampleQuestionDuration
		ilcResult.sampleQuestionDurationExampleAnswers == ilc.sampleQuestionDurationExampleAnswers
		ilcResult.sampleQuestionRating == ilc.sampleQuestionRating
		ilcResult.sampleQuestionRatingRange == ilc.sampleQuestionRatingRange
		ilcResult.sampleQuestionRatingExampleAnswer1 == ilc.sampleQuestionRatingExampleAnswer1
		ilcResult.sampleQuestionRatingExampleAnswer1 == sampleQuestionRatingExampleAnswer1
		ilcResult.sampleQuestionRatingExampleAnswer2 == ilc.sampleQuestionRatingExampleAnswer2
		ilcResult.sampleQuestionRatingExampleAnswer3 == ilc.sampleQuestionRatingExampleAnswer3
		ilcResult.today1 == ilc.today1
		ilcResult.today1Example == ilc.today1Example
		ilcResult.today2 == ilc.today2
		ilcResult.today2Example == ilc.today2Example
		ilcResult.today3 == ilc.today3
		ilcResult.today3Example == ilc.today3Example
		ilcResult.today4 == ilc.today4
		ilcResult.today4Example == ilc.today4Example
		ilcResult.interestTags.equals(ilc.interestTags)
		ilcResult.bookmarks.equals(ilc.bookmarks)
		
		and: "user interest tags are set"
		ilc.interestTags.each{ user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set"
		def tags = User.getTags(user.id)
		tags
		tags.size == 2
		tags.find{ it.description == "eat [calories]" }
		tags.find{ it.description == "run [distance]" }
    }

    void "test register invalid promo code specified"() {
		given: "an invalid promo code"
		String code = "invalid"
		
		when: "register is called"
		Map results = userRegistrationService.register(user, code)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == PromoCodeLookupResult.INVALID	
		
		and: "the default promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig		
		ilcResult.promoCode == defaultILC.promoCode 
		ilcResult.customQuestion1 == defaultILC.customQuestion1
		ilcResult.customQuestion2 == defaultILC.customQuestion2
		ilcResult.trackExample1 == defaultILC.trackExample1
		ilcResult.trackExample2 == defaultILC.trackExample2
		ilcResult.trackExample3 == defaultILC.trackExample3
		ilcResult.deviceExample == defaultILC.deviceExample
		ilcResult.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		ilcResult.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		ilcResult.sampleQuestionRating == defaultILC.sampleQuestionRating
		ilcResult.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		ilcResult.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		ilcResult.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		ilcResult.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		ilcResult.today1 == defaultILC.today1
		ilcResult.today1Example == defaultILC.today1Example
		ilcResult.today2 == defaultILC.today2
		ilcResult.today2Example == defaultILC.today2Example
		ilcResult.today3 == defaultILC.today3
		ilcResult.today3Example == defaultILC.today3Example
		ilcResult.today4 == defaultILC.today4
		ilcResult.today4Example == defaultILC.today4Example
		ilcResult.interestTags.equals(defaultILC.interestTags)
		ilcResult.bookmarks.equals(defaultILC.bookmarks)
		
		and: "user interest tags are set"
		defaultILC.interestTags.each{ user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set"
		def tags = User.getTags(user.id)
		tags
		tags.size == 1
		tags.find{ it.description == "sleep [time]" }
    }

    //@spock.lang.IgnoreRest
	void "test register new default data"() {
		given: "some non-default login configuration data"
		String trackExample2 = "new default trackExample2 test"
		String sampleQuestionRatingExampleAnswer2 = "new default sampleQuestionRatingExampleAnswer2 test"
		List interestTags = ["testInterestTag1", "testInterestTag2", "testInterestTag3"]
		List bookmarks = ["run 100 miles", "eat 2000 calories", "walk 20 miles"]
		
		and: "set non-default data on the default InitialLoginConfiguration"
		defaultILC.trackExample2 = trackExample2
		defaultILC.sampleQuestionRatingExampleAnswer2 = sampleQuestionRatingExampleAnswer2
		defaultILC.interestTags = interestTags
		defaultILC.bookmarks = bookmarks
		Utils.save(defaultILC, true)
		
		when: "register is called"
		Map results = userRegistrationService.register(user)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == PromoCodeLookupResult.UNSPECIFIED
		
		and: "the saved promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig		
		ilcResult.promoCode == defaultILC.promoCode 
		ilcResult.customQuestion1 == defaultILC.customQuestion1
		ilcResult.customQuestion2 == defaultILC.customQuestion2
		ilcResult.trackExample1 == defaultILC.trackExample1
		ilcResult.trackExample2 == defaultILC.trackExample2
		ilcResult.trackExample2 == trackExample2
		ilcResult.trackExample3 == defaultILC.trackExample3
		ilcResult.deviceExample == defaultILC.deviceExample
		ilcResult.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		ilcResult.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		ilcResult.sampleQuestionRating == defaultILC.sampleQuestionRating
		ilcResult.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		ilcResult.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		ilcResult.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		ilcResult.sampleQuestionRatingExampleAnswer2 == sampleQuestionRatingExampleAnswer2
		ilcResult.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		ilcResult.today1 == defaultILC.today1
		ilcResult.today1Example == defaultILC.today1Example
		ilcResult.today2 == defaultILC.today2
		ilcResult.today2Example == defaultILC.today2Example
		ilcResult.today3 == defaultILC.today3
		ilcResult.today3Example == defaultILC.today3Example
		ilcResult.today4 == defaultILC.today4
		ilcResult.today4Example == defaultILC.today4Example
		ilcResult.interestTags.equals(defaultILC.interestTags)
		ilcResult.bookmarks.equals(defaultILC.bookmarks)
		
		and: "user interest tags are set"
		defaultILC.interestTags.each{ user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set"
		def tags = User.getTags(user.id)
		tags
		tags.size == 3
		tags.find{ it.description == "eat [calories]" }
		tags.find{ it.description == "run [distance]" }
		tags.find{ it.description == "walk [distance]" }
    }
}

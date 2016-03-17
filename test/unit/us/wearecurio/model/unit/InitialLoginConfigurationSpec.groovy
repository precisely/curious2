package us.wearecurio.model.unit

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.model.InitialLoginConfiguration

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock(InitialLoginConfiguration)
class InitialLoginConfigurationSpec extends Specification {
    void "test default"() {
		when: "given the default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		then: "defaultILC properties set to hard-coded values"
		defaultILC.promoCode == InitialLoginConfiguration.DEFAULT_PROMO_CODE
		defaultILC.customQuestion1 == "Does caffeine affect my sleep?"
		defaultILC.customQuestion2 == "Does exercise really affect my mood?"
		defaultILC.trackExample1 == "your mood"
		defaultILC.trackExample2 == "how much sleep you get"
		defaultILC.trackExample3 == "the coffee you drink"
		defaultILC.deviceExample == "the Oura ring (via your user profile)"
		defaultILC.sampleQuestionDuration == "How many hours did you sleep last night?"
		defaultILC.sampleQuestionDurationExampleAnswers == "e.g. 8 hours 10 minutes or 8hrs 10 mins"
		defaultILC.sampleQuestionRating == "How's your mood right now?"
		defaultILC.sampleQuestionRatingRange == "a number from 1 to 10"
		defaultILC.sampleQuestionRatingExampleAnswer1 == "1 would mean 'Not the best day'"
		defaultILC.sampleQuestionRatingExampleAnswer2 == "2>5 would mean 'Just so-so'"
		defaultILC.sampleQuestionRatingExampleAnswer3 == "3>10 would mean 'Super stoked'"
		defaultILC.today1 == "DRINK"
		defaultILC.today1Example == "e.g. coffee 1 cup 8am"
		defaultILC.today2 == "EXERCISE"
		defaultILC.today2Example == "e.g. walk 9500 steps"
		defaultILC.today3 == "WORK"
		defaultILC.today3Example == "e.g. work 7 hours 30 minutes"
		defaultILC.today4 == "SUPPLEMENTS"
		defaultILC.today4Example == "e.g. aspirin 400 mg, or vitamin c 200 mg"
		defaultILC.interestTags.equals(["sleep"])
		defaultILC.bookmarks.equals(["sleep 8 hours"])
    }

	void "test default returns saved modified values"() {
		given: "the default InitialLoginConfiguration object"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		and: "new values for defaultILC properties"
		String customQuestion1 = "Testing customQuestion1"
		String customQuestion2 = "Testing customQuestion2"
		String trackExample1 = "Testing trackExample1"
		String trackExample2 = "Testing trackExample2"
		String trackExample3 = "Testing trackExample3"
		String deviceExample = "Testing deviceExample"
		String sampleQuestionDuration = "Testing sampleQuestionDuration"
		String sampleQuestionDurationExampleAnswers = "Testing sampleQuestionDurationExampleAnswers"
		String sampleQuestionRating = "Testing sampleQuestionRating"
		String sampleQuestionRatingRange = "Testing sampleQuestionRatingRange"
		String sampleQuestionRatingExampleAnswer1 = "Testing sampleQuestionRatingExampleAnswer1"
		String sampleQuestionRatingExampleAnswer2 = "Testing sampleQuestionRatingExampleAnswer2"
		String sampleQuestionRatingExampleAnswer3 = "Testing sampleQuestionRatingExampleAnswer3"
		String today1 = "Testing today1"
		String today1Example = "Testing today1Example"
		String today2 = "Testing today2"
		String today2Example = "Testing today2Example"
		String today3 = "Testing today3"
		String today3Example = "Testing today3Example"
		String today4 = "Testing today4"
		String today4Example = "Testing today4Example"
		List interestTags = ["testing-interest-tag"]
		List bookmarks = ["testing-bookmark-text"]
		
		when: "default object is modified"
		defaultILC.customQuestion1 = customQuestion1
		defaultILC.customQuestion2 = customQuestion2
		defaultILC.trackExample1 = trackExample1
		defaultILC.trackExample2 = trackExample2
		defaultILC.trackExample3 = trackExample3
		defaultILC.deviceExample = deviceExample
		defaultILC.sampleQuestionDuration = sampleQuestionDuration
		defaultILC.sampleQuestionDurationExampleAnswers = sampleQuestionDurationExampleAnswers
		defaultILC.sampleQuestionRating = sampleQuestionRating
		defaultILC.sampleQuestionRatingRange = sampleQuestionRatingRange
		defaultILC.sampleQuestionRatingExampleAnswer1 = sampleQuestionRatingExampleAnswer1
		defaultILC.sampleQuestionRatingExampleAnswer2 = sampleQuestionRatingExampleAnswer2
		defaultILC.sampleQuestionRatingExampleAnswer3 = sampleQuestionRatingExampleAnswer3
		defaultILC.today1 = today1
		defaultILC.today1Example = today1Example
		defaultILC.today2 = today2
		defaultILC.today2Example = today2Example
		defaultILC.today3 = today3
		defaultILC.today3Example = today3Example
		defaultILC.today4 = today4
		defaultILC.today4Example = today4Example
		defaultILC.interestTags = interestTags
		defaultILC.bookmarks = bookmarks
		
		and: "defaultILC object is saved"
		defaultILC.save(true)
		
		and: "default is requested"
		InitialLoginConfiguration newDefault = InitialLoginConfiguration.defaultConfiguration()
		
		then: "returned InitialLoginConfiguration has default promoCode"
		newDefault.promoCode == InitialLoginConfiguration.DEFAULT_PROMO_CODE
		
		and: "other values reflect the saved values"
		newDefault.customQuestion1 == customQuestion1
		newDefault.customQuestion2 == customQuestion2
		newDefault.trackExample1 == trackExample1
		newDefault.trackExample2 == trackExample2
		newDefault.trackExample3 == trackExample3
		newDefault.deviceExample == deviceExample
		newDefault.sampleQuestionDuration == sampleQuestionDuration
		newDefault.sampleQuestionDurationExampleAnswers == sampleQuestionDurationExampleAnswers
		newDefault.sampleQuestionRating == sampleQuestionRating
		newDefault.sampleQuestionRatingRange == sampleQuestionRatingRange
		newDefault.sampleQuestionRatingExampleAnswer1 == sampleQuestionRatingExampleAnswer1
		newDefault.sampleQuestionRatingExampleAnswer2 == sampleQuestionRatingExampleAnswer2
		newDefault.sampleQuestionRatingExampleAnswer3 == sampleQuestionRatingExampleAnswer3
		newDefault.today1 == today1
		newDefault.today1Example == today1Example
		newDefault.today2 == today2
		newDefault.today2Example == today2Example
		newDefault.today3 == today3
		newDefault.today3Example == today3Example
		newDefault.today4 == today4
		newDefault.today4Example == today4Example
		newDefault.interestTags.equals(interestTags)
		newDefault.bookmarks.equals(bookmarks)
	}
	
	void "test createFromDefault without promoCode"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		when: "a new ilc created from default InitialLoginConfiguration"
		InitialLoginConfiguration newILC = InitialLoginConfiguration.createFromDefault()
		
		then: "new ilc promo code is blank"
		(newILC.promoCode == null) || (newILC.promoCode == "")
		
		and: "rest of fields of new ilc are identical to default ilc"
		newILC.customQuestion1 == defaultILC.customQuestion1
		newILC.customQuestion2 == defaultILC.customQuestion2
		newILC.trackExample1 == defaultILC.trackExample1
		newILC.trackExample2 == defaultILC.trackExample2
		newILC.trackExample3 == defaultILC.trackExample3
		newILC.deviceExample == defaultILC.deviceExample
		newILC.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		newILC.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		newILC.sampleQuestionRating == defaultILC.sampleQuestionRating
		newILC.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		newILC.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		newILC.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		newILC.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		newILC.today1 == defaultILC.today1
		newILC.today1Example == defaultILC.today1Example
		newILC.today2 == defaultILC.today2
		newILC.today2Example == defaultILC.today2Example
		newILC.today3 == defaultILC.today3
		newILC.today3Example == defaultILC.today3Example
		newILC.today4 == defaultILC.today4
		newILC.today4Example == defaultILC.today4Example
		newILC.interestTags.equals(defaultILC.interestTags)
		newILC.bookmarks.equals(defaultILC.bookmarks)
    }
	
    void "test createFromDefault with promoCode with save == false"() {
		given: "a promoCode"
		String promoCode = "testNonSavePromoCode"
		
		and: "a default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		when: "a new ilc created from default InitialLoginConfiguration"
		InitialLoginConfiguration newILC = InitialLoginConfiguration.createFromDefault(promoCode, false)
		
		then: "new ilc promo code is set to promo code passed in"
		newILC.promoCode == promoCode
		
		and: "rest of fields of new ilc are identical to default ilc"
		newILC.customQuestion1 == defaultILC.customQuestion1
		newILC.customQuestion2 == defaultILC.customQuestion2
		newILC.trackExample1 == defaultILC.trackExample1
		newILC.trackExample2 == defaultILC.trackExample2
		newILC.trackExample3 == defaultILC.trackExample3
		newILC.deviceExample == defaultILC.deviceExample
		newILC.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		newILC.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		newILC.sampleQuestionRating == defaultILC.sampleQuestionRating
		newILC.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		newILC.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		newILC.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		newILC.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		newILC.today1 == defaultILC.today1
		newILC.today1Example == defaultILC.today1Example
		newILC.today2 == defaultILC.today2
		newILC.today2Example == defaultILC.today2Example
		newILC.today3 == defaultILC.today3
		newILC.today3Example == defaultILC.today3Example
		newILC.today4 == defaultILC.today4
		newILC.today4Example == defaultILC.today4Example
		newILC.interestTags.equals(defaultILC.interestTags)
		newILC.bookmarks.equals(defaultILC.bookmarks)
		
		when: "customLogin searched by promoCode"
		InitialLoginConfiguration result = InitialLoginConfiguration.findByPromoCode(promoCode)
		
		then: "null is returned since new ILC is unsaved"
		result == null
	}
	
    void "test createFromDefault with promoCode with save == true"() {
		given: "a promoCode"
		String promoCode = "testNonSavePromoCode"
		
		and: "a default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		when: "a new ilc created from default InitialLoginConfiguration"
		InitialLoginConfiguration newILC = InitialLoginConfiguration.createFromDefault(promoCode, true)
		
		then: "new ilc promo code is set to promo code passed in"
		newILC.promoCode == promoCode
		
		and: "rest of fields of new ilc are identical to default ilc"
		newILC.customQuestion1 == defaultILC.customQuestion1
		newILC.customQuestion2 == defaultILC.customQuestion2
		newILC.trackExample1 == defaultILC.trackExample1
		newILC.trackExample2 == defaultILC.trackExample2
		newILC.trackExample3 == defaultILC.trackExample3
		newILC.deviceExample == defaultILC.deviceExample
		newILC.sampleQuestionDuration == defaultILC.sampleQuestionDuration
		newILC.sampleQuestionDurationExampleAnswers == defaultILC.sampleQuestionDurationExampleAnswers
		newILC.sampleQuestionRating == defaultILC.sampleQuestionRating
		newILC.sampleQuestionRatingRange == defaultILC.sampleQuestionRatingRange
		newILC.sampleQuestionRatingExampleAnswer1 == defaultILC.sampleQuestionRatingExampleAnswer1
		newILC.sampleQuestionRatingExampleAnswer2 == defaultILC.sampleQuestionRatingExampleAnswer2
		newILC.sampleQuestionRatingExampleAnswer3 == defaultILC.sampleQuestionRatingExampleAnswer3
		newILC.today1 == defaultILC.today1
		newILC.today1Example == defaultILC.today1Example
		newILC.today2 == defaultILC.today2
		newILC.today2Example == defaultILC.today2Example
		newILC.today3 == defaultILC.today3
		newILC.today3Example == defaultILC.today3Example
		newILC.today4 == defaultILC.today4
		newILC.today4Example == defaultILC.today4Example
		newILC.interestTags.equals(defaultILC.interestTags)
		newILC.bookmarks.equals(defaultILC.bookmarks)
		
		when: "customLogin searched by promoCode"
		InitialLoginConfiguration result = InitialLoginConfiguration.findByPromoCode(promoCode)
		
		then: "new ILC is saved, so result matches new ilc promo code"
		result.promoCode == newILC.promoCode
		
		and: "result matches rest of new ilc fields"
		result.customQuestion1 == newILC.customQuestion1
		result.customQuestion2 == newILC.customQuestion2
		result.trackExample1 == newILC.trackExample1
		result.trackExample2 == newILC.trackExample2
		result.trackExample3 == newILC.trackExample3
		result.deviceExample == newILC.deviceExample
		result.sampleQuestionDuration == newILC.sampleQuestionDuration
		result.sampleQuestionDurationExampleAnswers == newILC.sampleQuestionDurationExampleAnswers
		result.sampleQuestionRating == newILC.sampleQuestionRating
		result.sampleQuestionRatingRange == newILC.sampleQuestionRatingRange
		result.sampleQuestionRatingExampleAnswer1 == newILC.sampleQuestionRatingExampleAnswer1
		result.sampleQuestionRatingExampleAnswer2 == newILC.sampleQuestionRatingExampleAnswer2
		result.sampleQuestionRatingExampleAnswer3 == newILC.sampleQuestionRatingExampleAnswer3
		result.today1 == newILC.today1
		result.today1Example == newILC.today1Example
		result.today2 == newILC.today2
		result.today2Example == newILC.today2Example
		result.today3 == newILC.today3
		result.today3Example == newILC.today3Example
		result.today4 == newILC.today4
		result.today4Example == newILC.today4Example
		result.interestTags.equals(newILC.interestTags)
		result.bookmarks.equals(newILC.bookmarks)
	}		
}

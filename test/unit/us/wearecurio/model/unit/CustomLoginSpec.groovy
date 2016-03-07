package us.wearecurio.model.unit

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.model.CustomLogin

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock(CustomLogin)
class CustomLoginSpec extends Specification {
    void "test defaultCustomLogin"() {
		when: "given a default CustomLogin"
		CustomLogin login = CustomLogin.defaultCustomLogin()
		
		then: "defaults are set to hard-coded values since promo not in db"
		login.promoCode == CustomLogin.defaultPromoCode
		login.customQuestion1 == "Does caffeine affect my sleep?"
		login.customQuestion2 == "Does exercise really affect my mood?"
		login.trackExample1 == "your mood"
		login.trackExample2 == "how much sleep you get"
		login.trackExample3 == "the coffee you drink"
		login.deviceExample == "the Oura ring (via your user profile)"
		login.sampleQuestionDuration == "How many hours did you sleep last night?"
		login.sampleQuestionDurationExampleAnswers == "e.g. 8 hours 10 minutes or 8hrs 10 mins"
		login.sampleQuestionRating == "How's your mood right now?"
		login.sampleQuestionRatingRange == "a number from 1 to 10"
		login.sampleQuestionRatingExampleAnswer1 == "1 would mean 'Not the best day'"
		login.sampleQuestionRatingExampleAnswer2 == "2>5 would mean 'Just so-so'"
		login.sampleQuestionRatingExampleAnswer3 == "3>10 would mean 'Super stoked'"
		login.today1 == "DRINK"
		login.today1Example == "e.g. coffee 1 cup 8am"
		login.today2 == "EXERCISE"
		login.today2Example == "e.g. walk 9500 steps"
		login.today3 == "WORK"
		login.today3Example == "e.g. work 7 hours 30 minutes"
		login.today4 == "SUPPLEMENTS"
		login.today4Example == "e.g. aspirin 400 mg, or vitamin c 200 mg"
		login.interestTags.equals(["sleep"])
		login.bookmarks.equals(["sleep 8 hours"])
    }
	
	void "test createFromDefault without promoCode"() {
		given: "a default CustomLogin"
		CustomLogin defaultLogin = CustomLogin.defaultCustomLogin()
		
		when: "a new login created from default CustomLogin"
		CustomLogin newLogin = CustomLogin.createFromDefault()
		
		then: "new login promo code is blank"
		(newLogin.promoCode == null) || (newLogin.promoCode == "")
		
		and: "rest of fields of new login are identical to default login"
		newLogin.customQuestion1 == defaultLogin.customQuestion1
		newLogin.customQuestion2 == defaultLogin.customQuestion2
		newLogin.trackExample1 == defaultLogin.trackExample1
		newLogin.trackExample2 == defaultLogin.trackExample2
		newLogin.trackExample3 == defaultLogin.trackExample3
		newLogin.deviceExample == defaultLogin.deviceExample
		newLogin.sampleQuestionDuration == defaultLogin.sampleQuestionDuration
		newLogin.sampleQuestionDurationExampleAnswers == defaultLogin.sampleQuestionDurationExampleAnswers
		newLogin.sampleQuestionRating == defaultLogin.sampleQuestionRating
		newLogin.sampleQuestionRatingRange == defaultLogin.sampleQuestionRatingRange
		newLogin.sampleQuestionRatingExampleAnswer1 == defaultLogin.sampleQuestionRatingExampleAnswer1
		newLogin.sampleQuestionRatingExampleAnswer2 == defaultLogin.sampleQuestionRatingExampleAnswer2
		newLogin.sampleQuestionRatingExampleAnswer3 == defaultLogin.sampleQuestionRatingExampleAnswer3
		newLogin.today1 == defaultLogin.today1
		newLogin.today1Example == defaultLogin.today1Example
		newLogin.today2 == defaultLogin.today2
		newLogin.today2Example == defaultLogin.today2Example
		newLogin.today3 == defaultLogin.today3
		newLogin.today3Example == defaultLogin.today3Example
		newLogin.today4 == defaultLogin.today4
		newLogin.today4Example == defaultLogin.today4Example
		newLogin.interestTags.equals(defaultLogin.interestTags)
		newLogin.bookmarks.equals(defaultLogin.bookmarks)
    }
	
    void "test createFromDefault with promoCode with save == false"() {
		given: "a promoCode"
		String promoCode = "testNonSavePromoCode"
		
		and: "a default CustomLogin"
		CustomLogin defaultLogin = CustomLogin.defaultCustomLogin()
		
		when: "a new login created from default CustomLogin"
		CustomLogin newLogin = CustomLogin.createFromDefault(promoCode, false)
		
		then: "new login promo code is set"
		newLogin.promoCode == promoCode
		
		and: "rest of fields of new login are identical to default login"
		newLogin.customQuestion1 == defaultLogin.customQuestion1
		newLogin.customQuestion2 == defaultLogin.customQuestion2
		newLogin.trackExample1 == defaultLogin.trackExample1
		newLogin.trackExample2 == defaultLogin.trackExample2
		newLogin.trackExample3 == defaultLogin.trackExample3
		newLogin.deviceExample == defaultLogin.deviceExample
		newLogin.sampleQuestionDuration == defaultLogin.sampleQuestionDuration
		newLogin.sampleQuestionDurationExampleAnswers == defaultLogin.sampleQuestionDurationExampleAnswers
		newLogin.sampleQuestionRating == defaultLogin.sampleQuestionRating
		newLogin.sampleQuestionRatingRange == defaultLogin.sampleQuestionRatingRange
		newLogin.sampleQuestionRatingExampleAnswer1 == defaultLogin.sampleQuestionRatingExampleAnswer1
		newLogin.sampleQuestionRatingExampleAnswer2 == defaultLogin.sampleQuestionRatingExampleAnswer2
		newLogin.sampleQuestionRatingExampleAnswer3 == defaultLogin.sampleQuestionRatingExampleAnswer3
		newLogin.today1 == defaultLogin.today1
		newLogin.today1Example == defaultLogin.today1Example
		newLogin.today2 == defaultLogin.today2
		newLogin.today2Example == defaultLogin.today2Example
		newLogin.today3 == defaultLogin.today3
		newLogin.today3Example == defaultLogin.today3Example
		newLogin.today4 == defaultLogin.today4
		newLogin.today4Example == defaultLogin.today4Example
		newLogin.interestTags.equals(defaultLogin.interestTags)
		newLogin.bookmarks.equals(defaultLogin.bookmarks)
		
		when: "customLogin searched by promoCode"
		CustomLogin result = CustomLogin.findByPromoCode(promoCode)
		
		then: "result is null"
		result == null
	}
	
    void "test createFromDefault with promoCode with save == true"() {
		given: "a promoCode"
		String promoCode = "testNonSavePromoCode"
		
		and: "a default CustomLogin"
		CustomLogin defaultLogin = CustomLogin.defaultCustomLogin()
		
		when: "a new login created from default CustomLogin"
		CustomLogin newLogin = CustomLogin.createFromDefault(promoCode, true)
		
		then: "new login promo code is set"
		newLogin.promoCode == promoCode
		
		and: "rest of fields of new login are identical to default login"
		newLogin.customQuestion1 == defaultLogin.customQuestion1
		newLogin.customQuestion2 == defaultLogin.customQuestion2
		newLogin.trackExample1 == defaultLogin.trackExample1
		newLogin.trackExample2 == defaultLogin.trackExample2
		newLogin.trackExample3 == defaultLogin.trackExample3
		newLogin.deviceExample == defaultLogin.deviceExample
		newLogin.sampleQuestionDuration == defaultLogin.sampleQuestionDuration
		newLogin.sampleQuestionDurationExampleAnswers == defaultLogin.sampleQuestionDurationExampleAnswers
		newLogin.sampleQuestionRating == defaultLogin.sampleQuestionRating
		newLogin.sampleQuestionRatingRange == defaultLogin.sampleQuestionRatingRange
		newLogin.sampleQuestionRatingExampleAnswer1 == defaultLogin.sampleQuestionRatingExampleAnswer1
		newLogin.sampleQuestionRatingExampleAnswer2 == defaultLogin.sampleQuestionRatingExampleAnswer2
		newLogin.sampleQuestionRatingExampleAnswer3 == defaultLogin.sampleQuestionRatingExampleAnswer3
		newLogin.today1 == defaultLogin.today1
		newLogin.today1Example == defaultLogin.today1Example
		newLogin.today2 == defaultLogin.today2
		newLogin.today2Example == defaultLogin.today2Example
		newLogin.today3 == defaultLogin.today3
		newLogin.today3Example == defaultLogin.today3Example
		newLogin.today4 == defaultLogin.today4
		newLogin.today4Example == defaultLogin.today4Example
		newLogin.interestTags.equals(defaultLogin.interestTags)
		newLogin.bookmarks.equals(defaultLogin.bookmarks)
		
		when: "customLogin searched by promoCode"
		CustomLogin result = CustomLogin.findByPromoCode(promoCode)
		
		then: "result matches new login promo code"
		result.promoCode == newLogin.promoCode
		
		and: "result matches rest of new login fields"
		result.customQuestion1 == newLogin.customQuestion1
		result.customQuestion2 == newLogin.customQuestion2
		result.trackExample1 == newLogin.trackExample1
		result.trackExample2 == newLogin.trackExample2
		result.trackExample3 == newLogin.trackExample3
		result.deviceExample == newLogin.deviceExample
		result.sampleQuestionDuration == newLogin.sampleQuestionDuration
		result.sampleQuestionDurationExampleAnswers == newLogin.sampleQuestionDurationExampleAnswers
		result.sampleQuestionRating == newLogin.sampleQuestionRating
		result.sampleQuestionRatingRange == newLogin.sampleQuestionRatingRange
		result.sampleQuestionRatingExampleAnswer1 == newLogin.sampleQuestionRatingExampleAnswer1
		result.sampleQuestionRatingExampleAnswer2 == newLogin.sampleQuestionRatingExampleAnswer2
		result.sampleQuestionRatingExampleAnswer3 == newLogin.sampleQuestionRatingExampleAnswer3
		result.today1 == newLogin.today1
		result.today1Example == newLogin.today1Example
		result.today2 == newLogin.today2
		result.today2Example == newLogin.today2Example
		result.today3 == newLogin.today3
		result.today3Example == newLogin.today3Example
		result.today4 == newLogin.today4
		result.today4Example == newLogin.today4Example
		result.interestTags.equals(newLogin.interestTags)
		result.bookmarks.equals(newLogin.bookmarks)
	}	
}

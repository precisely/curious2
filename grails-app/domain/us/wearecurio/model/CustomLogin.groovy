package us.wearecurio.model

import groovy.transform.AutoClone

import us.wearecurio.utility.Utils

@AutoClone
class CustomLogin {

	static final String defaultPromoCode = "default"
	
	String promoCode
	String customQuestion1
	String customQuestion2
	String trackExample1
	String trackExample2
	String trackExample3
	String deviceExample
	String sampleQuestionDuration
	String sampleQuestionDurationExampleAnswers
	String sampleQuestionRating
	String sampleQuestionRatingRange
	String sampleQuestionRatingExampleAnswer1
	String sampleQuestionRatingExampleAnswer2
	String sampleQuestionRatingExampleAnswer3
	String today1
	String today1Example
	String today2
	String today2Example
	String today3
	String today3Example
	String today4
	String today4Example

	static hasMany = [interestTags: String, bookmarks: String]
	List interestTags
	List bookmarks
	
	static CustomLogin defaultCustomLogin() {
		CustomLogin defaultCustomization = CustomLogin.findByPromoCode(defaultPromoCode)
		
		if (defaultCustomization == null) {
			defaultCustomization = new CustomLogin()
			defaultCustomization.promoCode = defaultPromoCode
			defaultCustomization.customQuestion1 = "Does caffeine affect my sleep?"
			defaultCustomization.customQuestion2 = "Does exercise really affect my mood?"
			defaultCustomization.trackExample1 = "your mood"
			defaultCustomization.trackExample2 = "how much sleep you get"
			defaultCustomization.trackExample3 = "the coffee you drink"
			defaultCustomization.deviceExample = "the Oura ring (via your user profile)"
			defaultCustomization.sampleQuestionDuration = "How many hours did you sleep last night?"
			defaultCustomization.sampleQuestionDurationExampleAnswers = "e.g. 8 hours 10 minutes or 8hrs 10 mins"
			defaultCustomization.sampleQuestionRating = "How's your mood right now?"
			defaultCustomization.sampleQuestionRatingRange = "a number from 1 to 10"
			defaultCustomization.sampleQuestionRatingExampleAnswer1 = "1 would mean 'Not the best day'"
			defaultCustomization.sampleQuestionRatingExampleAnswer2 = "2>5 would mean 'Just so-so'"
			defaultCustomization.sampleQuestionRatingExampleAnswer3 = "3>10 would mean 'Super stoked'"
			defaultCustomization.today1 = "DRINK"
			defaultCustomization.today1Example = "e.g. coffee 1 cup 8am"
			defaultCustomization.today2 = "EXERCISE"
			defaultCustomization.today2Example = "e.g. walk 9500 steps"
			defaultCustomization.today3 = "WORK"
			defaultCustomization.today3Example = "e.g. work 7 hours 30 minutes"
			defaultCustomization.today4 = "SUPPLEMENTS"
			defaultCustomization.today4Example = "e.g. aspirin 400 mg, or vitamin c 200 mg"
			
			defaultCustomization.interestTags = ["sleep"]
			defaultCustomization.bookmarks = ["sleep 8 hours"]
			
			Utils.save(defaultCustomization, true)
		}
		
		return defaultCustomization
	}
	
	
	static CustomLogin createFromDefault() {
		CustomLogin ret = defaultCustomLogin().clone()
		
		if (ret == null) {
			ret = new CustomLogin()
		} else {
			ret.promoCode = ""
		}
		
		return ret
	}
	
	static CustomLogin createFromDefault(String promoCode, boolean save=false) {
		CustomLogin ret = createFromDefault()
		
		ret.promoCode = promoCode
		if (save) {
			Utils.save(ret, true)
		}
		
		return ret
	}
	
    static constraints = {
		promoCode(maxSize:32, unique: true, blank:false, nullable:false)
		customQuestion1(maxSize:64, unique:false, blank:false, nullable:false)
		customQuestion2(maxSize:64, unique:false, blank:false, nullable:false)
		trackExample1(maxSize:64, unique:false, blank:false, nullable:false)
		trackExample2(maxSize:64, unique:false, blank:false, nullable:false)
		trackExample3(maxSize:64, unique:false, blank:false, nullable:false)
		deviceExample(maxSize:64, unique:false, blank:false, nullable:false)
		sampleQuestionDuration(maxSize:64, unique:false, blank:false, nullable:false)
		sampleQuestionDurationExampleAnswers(maxSize:64, unique:false, blank:false, nullable:false)
		sampleQuestionRating(maxSize:64, unique:false, blank:false, nullable:false)
		sampleQuestionRatingRange(maxSize:64, unique:false, blank:false, nullable:false)
		sampleQuestionRatingExampleAnswer1(maxSize:64, unique:false, blank:false, nullable:false)
		sampleQuestionRatingExampleAnswer2(maxSize:64, unique:false, blank:false, nullable:false)
		sampleQuestionRatingExampleAnswer3(maxSize:64, unique:false, blank:false, nullable:false)
		today1(maxSize:32, unique:false, blank:false, nullable:false)
		today1Example(maxSize:64, unique:false, blank:false, nullable:false)
		today2(maxSize:32, unique:false, blank:false, nullable:false)
		today2Example(maxSize:64, unique:false, blank:false, nullable:false)
		today3(maxSize:32, unique:false, blank:false, nullable:false)
		today3Example(maxSize:64, unique:false, blank:false, nullable:false)
		today4(maxSize:32, unique:false, blank:false, nullable:false)
		today4Example(maxSize:64, unique:false, blank:false, nullable:false)
    }
}

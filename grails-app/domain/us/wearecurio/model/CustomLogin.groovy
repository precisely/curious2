package us.wearecurio.model

import us.wearecurio.utility.Utils

class CustomLogin {

	static final String defaultPromoCode = "default"
	
	String promoCode
	String customQuestion1 = "Does caffeine affect my sleep?"
	String customQuestion2 = "Does exercise really affect my mood?"
	String trackExample1 = "your mood"
	String trackExample2 = "how much sleep you get"
	String trackExample3 = "the coffee you drink"
	String deviceExample = "the Oura ring (via your user profile)"
	String sampleQuestionDuration = "How many hours did you sleep last night?"
	String sampleQuestionDurationExampleAnswers = "e.g. 8 hours 10 minutes or 8hrs 10 mins"
	String sampleQuestionRating = "How's your mood right now?"
	String sampleQuestionRatingRange = "a number from 1 to 10"
	String sampleQuestionRatingExampleAnswer1 = "1 would mean 'Not the best day'"
	String sampleQuestionRatingExampleAnswer2 = "2>5 would mean 'Just so-so'"
	String sampleQuestionRatingExampleAnswer3 = "3>10 would mean 'Super stoked'"
	String today1 = "DRINK"
	String today1Example = "e.g. coffee 1 cup 8am"
	String today2 = "EXERCISE"
	String today2Example = "e.g. walk 9500 steps"
	String today3 = "WORK"
	String today3Example = "e.g. work 7 hours 30 minutes"
	String today4 = "SUPPLEMENTS"
	String today4Example = "e.g. aspirin 400 mg, or vitamin c 200 mg"

	String[] interestTags = ["sleep", "coffee"]
	String[] bookmarks = ["sleep 8 hrs", "swim 30 mins"]
	
	static CustomLogin defaultCustomLogin() {
		CustomLogin defaultCustomization = CustomLogin.findByPromoCode(defaultPromoCode)
		
		if (defaultCustomization == null) {
			defaultCustomization = new CustomLogin()
			defaultCustomization.promoCode = defaultPromoCode
			Utils.save(defaultCustomization, true)
		}
		
		return defaultCustomization
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

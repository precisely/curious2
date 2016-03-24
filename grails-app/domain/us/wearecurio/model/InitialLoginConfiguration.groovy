package us.wearecurio.model

import groovy.transform.AutoClone

import us.wearecurio.utility.Utils

class InitialLoginConfiguration {

	static final String DEFAULT_PROMO_CODE = "default"
	
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
	
	private setDefaults() {
		customQuestion1 = "Does caffeine affect my sleep?"
		customQuestion2 = "Does exercise really affect my mood?"
		trackExample1 = "your mood"
		trackExample2 = "how much sleep you get"
		trackExample3 = "the coffee you drink"
		deviceExample = "the Oura ring (via your user profile)"
		sampleQuestionDuration = "How many hours did you sleep last night?"
		sampleQuestionDurationExampleAnswers = "e.g. 8 hours 10 minutes or 8hrs 10 mins"
		sampleQuestionRating = "How's your mood right now?"
		sampleQuestionRatingRange = "a number from 1 to 10"
		sampleQuestionRatingExampleAnswer1 = "1 would mean 'Not the best day'"
		sampleQuestionRatingExampleAnswer2 = "2>5 would mean 'Just so-so'"
		sampleQuestionRatingExampleAnswer3 = "3>10 would mean 'Super stoked'"
		today1 = "DRINK"
		today1Example = "e.g. coffee 1 cup 8am"
		today2 = "EXERCISE"
		today2Example = "e.g. walk 9500 steps"
		today3 = "WORK"
		today3Example = "e.g. work 7 hours 30 minutes"
		today4 = "SUPPLEMENTS"
		today4Example = "e.g. aspirin 400 mg, or vitamin c 200 mg"

		interestTags = ["sleep"]
		bookmarks = ["sleep 8 hours"]
	}
	
	static copy(InitialLoginConfiguration from, InitialLoginConfiguration to) {
		to.customQuestion1 = from.customQuestion1
		to.customQuestion2 = from.customQuestion2
		to.trackExample1 = from.trackExample1
		to.trackExample2 = from.trackExample2
		to.trackExample3 = from.trackExample3
		to.deviceExample = from.deviceExample
		to.sampleQuestionDuration = from.sampleQuestionDuration
		to.sampleQuestionDurationExampleAnswers = from.sampleQuestionDurationExampleAnswers
		to.sampleQuestionRating = from.sampleQuestionRating
		to.sampleQuestionRatingRange = from.sampleQuestionRatingRange
		to.sampleQuestionRatingExampleAnswer1 = from.sampleQuestionRatingExampleAnswer1
		to.sampleQuestionRatingExampleAnswer2 = from.sampleQuestionRatingExampleAnswer2
		to.sampleQuestionRatingExampleAnswer3 = from.sampleQuestionRatingExampleAnswer3
		to.today1 = from.today1
		to.today1Example = from.today1Example
		to.today2 = from.today2
		to.today2Example = from.today2Example
		to.today3 = from.today3
		to.today3Example = from.today3Example
		to.today4 = from.today4
		to.today4Example = from.today4Example

		to.interestTags = []
		from.interestTags.each{ to.interestTags << it }
		to.bookmarks = []
		from.bookmarks.each{ to.bookmarks << it }		
	}
	
	static InitialLoginConfiguration defaultConfiguration() {
		InitialLoginConfiguration ret = InitialLoginConfiguration.findByPromoCode(DEFAULT_PROMO_CODE)
		
		if (ret == null) {
			ret = InitialLoginConfiguration.create()
			ret.promoCode = DEFAULT_PROMO_CODE
			ret.setDefaults()
			Utils.save(ret, true)
		}
		
		return ret
	}
	
	static InitialLoginConfiguration createFromDefault() {
		InitialLoginConfiguration ret = InitialLoginConfiguration.create()
		ret.promoCode = ""
		
		InitialLoginConfiguration d = defaultConfiguration()
		if (d != null) {
			copy(d, ret)
		}
		
		return ret
	}
	
	static InitialLoginConfiguration createFromDefault(String promoCode, boolean save=false) {
		InitialLoginConfiguration ret = createFromDefault()
		if (ret != null) {
			ret.promoCode = promoCode
			
			if (save) {
				Utils.save(ret, true)
			}		
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

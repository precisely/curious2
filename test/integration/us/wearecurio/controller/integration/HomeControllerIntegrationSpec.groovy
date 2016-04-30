package us.wearecurio.controller.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.InterestArea
import us.wearecurio.model.InterestAreaBoolean
import us.wearecurio.model.InterestSubject
import us.wearecurio.model.InterestSubjectBoolean
import us.wearecurio.model.Tag
import us.wearecurio.model.TutorialInfo
import us.wearecurio.model.User
import us.wearecurio.controller.HomeController
import us.wearecurio.utility.Utils

//NOTE: These are spock tests (my preferred form) and will focus on promo code stuff
class HomeControllerIntegrationSpec extends IntegrationSpec {
	static transactional = true
	User user
	HomeController controller
	InitialLoginConfiguration defaultILC

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
		
		controller = new HomeController()
		controller.session.userId = user.id
		defaultILC = InitialLoginConfiguration.defaultConfiguration()
    }

	static Boolean hasSubjects(InitialLoginConfiguration ilc){
		return ( 
			ilc != null && 
			ilc.interestAreas != null &&
			ilc.interestAreas.size() > 0 && 
			ilc.interestAreas[0].interest != null &&
			ilc.interestAreas[0]
				.interest
				.interestSubjects != null &&
			ilc.interestAreas[0]
				.interest
				.interestSubjects.size() > 0 &&
			ilc.interestAreas[0]
				.interest
				.interestSubjects[0]
				.subject != null
		)
	}
	
	static Boolean hasInterestTags(InitialLoginConfiguration ilc){
		return (
			hasSubjects(ilc) &&
			ilc
				.interestAreas[0]
				.interest
				.interestSubjects[0]
				.subject
				.interestTags != null &&
			ilc
				.interestAreas[0]
				.interest
				.interestSubjects[0]
				.subject
				.interestTags.size() > 0
		)
	}
	
	static Boolean hasBookmarks(InitialLoginConfiguration ilc){
		return (
			hasSubjects(ilc) &&
			ilc
				.interestAreas[0]
				.interest
				.interestSubjects[0]
				.subject
				.bookmarks != null &&
			ilc
				.interestAreas[0]
				.interest
				.interestSubjects[0]
				.subject
				.bookmarks.size() > 0
		)		
	}
	
	static List interestTags(InitialLoginConfiguration ilc){
		List ret = []
		if (hasInterestTags(ilc)) {
			ilc
				.interestAreas[0]
				.interest
				.interestSubjects[0]
				.subject
				.interestTags
				.each{
					ret << it
				}
		}
		
		return ret
	}
	
	static List bookmarks(InitialLoginConfiguration ilc){
		List ret = []
		if (hasInterestTags(ilc)) {
			ilc
				.interestAreas[0]
				.interest
				.interestSubjects[0]
				.subject
				.bookmarks
				.each{
					ret << it
				}
		}
		
		return ret		
	}
	
	static Boolean compareTutorialInfo(
		InitialLoginConfiguration a, 
		InitialLoginConfiguration b ) {
		return (
			a.tutorialInfo.customQuestion1 == 
				b.tutorialInfo.customQuestion1 &&
			a.tutorialInfo.customQuestion2 == 
				b.tutorialInfo.customQuestion2 &&
			a.tutorialInfo.trackExample1 == 
				b.tutorialInfo.trackExample1 &&
			a.tutorialInfo.trackExample2 == 
				b.tutorialInfo.trackExample2 &&
			a.tutorialInfo.trackExample3 == 
				b.tutorialInfo.trackExample3 &&
			a.tutorialInfo.deviceExample == 
				b.tutorialInfo.deviceExample &&
			a.tutorialInfo.sampleQuestionDuration == 
				b.tutorialInfo.sampleQuestionDuration &&
			a.tutorialInfo.sampleQuestionDurationExampleAnswers == 
				b.tutorialInfo.sampleQuestionDurationExampleAnswers &&
			a.tutorialInfo.sampleQuestionRating == 
				b.tutorialInfo.sampleQuestionRating &&
			a.tutorialInfo.sampleQuestionRatingRange == 
				b.tutorialInfo.sampleQuestionRatingRange &&
			a.tutorialInfo.sampleQuestionRatingExampleAnswer1 == 
				b.tutorialInfo.sampleQuestionRatingExampleAnswer1 &&
			a.tutorialInfo.sampleQuestionRatingExampleAnswer2 == 
				b.tutorialInfo.sampleQuestionRatingExampleAnswer2 &&
			a.tutorialInfo.sampleQuestionRatingExampleAnswer3 == 
				b.tutorialInfo.sampleQuestionRatingExampleAnswer3 &&
			a.tutorialInfo.today1 == 
				b.tutorialInfo.today1 &&
			a.tutorialInfo.today1Example == 
				b.tutorialInfo.today1Example &&
			a.tutorialInfo.today2 == 
				b.tutorialInfo.today2 &&
			a.tutorialInfo.today2Example == 
				b.tutorialInfo.today2Example &&
			a.tutorialInfo.today3 == 
				b.tutorialInfo.today3 &&
			a.tutorialInfo.today3Example == 
				b.tutorialInfo.today3Example &&
			a.tutorialInfo.today4 == 
				b.tutorialInfo.today4 &&
			a.tutorialInfo.today4Example == 
				b.tutorialInfo.today4Example
		)
	}
	
	//@spock.lang.IgnoreRest
	void "test promo code found"() {
		given: "a promo code"
		String code = "test-promo-home"
		
		and: "an InitialLoginConfiguration for that promo code"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.createFromDefault(code, false)
		
		and: "some non-default login configuration data"
		String trackExample1 = 
			"trackExample1 test"
		String sampleQuestionRatingExampleAnswer1 = 
			"sampleQuestionRatingExampleAnswer1 test"
		List tags = 
			["testInterestTag1", "testInterestTag2"]
		List bkmrks = 
			["run 100 miles", "eat 2000 calories"]
				
		and: "set non-default tutorial info"
		TutorialInfo ti = TutorialInfo.createFromDefault(code)
		ti.trackExample1 = trackExample1
		ti.sampleQuestionRatingExampleAnswer1 = sampleQuestionRatingExampleAnswer1
		Utils.save(ti,true)
		
		and: "set non-default subject info"
		ilc
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.interestTags
			.clear()
		ilc
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.bookmarks
			.clear()
		tags
			.each {
				ilc
					.interestAreas[0]
					.interest
					.interestSubjects[0]
					.subject
					.addToInterestTags(it)}
		bkmrks
			.each {
				ilc
					.interestAreas[0]
					.interest
					.interestSubjects[0]
					.subject
					.addToBookmarks(it)}
		Utils.save(ilc, true)
				
		when: "params.promoCode is set"
		controller.params.clear()
		controller.params.putAll([
			promoCode: code
		])
		
		and: "index is called"
		def result = controller.index()
		
		then: "promo Code is correct"
		InitialLoginConfiguration ilcResult = result.initialConfig
		ilcResult.promoCode == ilc.promoCode
		
		and: "tutorial info matches"
		compareTutorialInfo(ilcResult, ilc)
		ilcResult.tutorialInfo.trackExample1 == 
			trackExample1
		ilcResult.tutorialInfo.sampleQuestionRatingExampleAnswer1 == 
			sampleQuestionRatingExampleAnswer1

		and: "interest Tags match"
		interestTags(ilcResult).sort().equals(interestTags(ilc).sort())
		
		and: "bookmarks match"
		bookmarks(ilcResult).sort().equals(bookmarks(ilc).sort())
	}
	
	@spock.lang.Unroll
	void "test promo code #promoCode returns default"(){
		when: "params.promoCode is set"
		controller.params.clear()
		controller.params.putAll([
			promoCode: promoCode
		])
		
		and: "index is called"
		def result = controller.index()

		then: "promo Code is correct"
		InitialLoginConfiguration ilcResult = result.initialConfig
		ilcResult.promoCode == 
			InitialLoginConfiguration.DEFAULT_PROMO_CODE
		
		and: "tutorial info matches"
		compareTutorialInfo(ilcResult, defaultILC)

		and: "interest Tags match"
		interestTags(ilcResult).sort().equals(interestTags(defaultILC).sort())
		
		and: "bookmarks match"
		bookmarks(ilcResult).sort().equals(bookmarks(defaultILC).sort())
				
		where:
		promoCode << [
		"",
		null,
		"invalid",
		InitialLoginConfiguration.DEFAULT_PROMO_CODE
		]
	}
	
	void "test new default data"() {
		given: "some non-default login configuration data"
		String trackExample2 = 
			"new default trackExample2 test"
		String sampleQuestionRatingExampleAnswer2 = 
			"new default sampleQuestionRatingExampleAnswer2 test"
		List tags = 
			["testInterestTag1", "testInterestTag2", "testInterestTag3"]
		List bkmrks = 
			["run 100 miles", "eat 2000 calories", "walk 20 miles"]
		
		and: "set non-default tutorial info"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		defaultTI.trackExample2 = trackExample2
		defaultTI.sampleQuestionRatingExampleAnswer2 = 
			sampleQuestionRatingExampleAnswer2
		Utils.save(defaultTI,true)
		
		and: "set non-default subject info"
		defaultILC
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.interestTags
			.clear()
		defaultILC
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.bookmarks
			.clear()
		tags
			.each {
				defaultILC
					.interestAreas[0]
					.interest
					.interestSubjects[0]
					.subject
					.addToInterestTags(it)}
		bkmrks
			.each {
				defaultILC
					.interestAreas[0]
					.interest
					.interestSubjects[0]
					.subject
					.addToBookmarks(it)}
		Utils.save(defaultILC, true)
		
		when: "index is called without parameters"
		def result = controller.index()
		
		then: "promo code is correct"
		InitialLoginConfiguration ilcResult = result.initialConfig
		ilcResult.promoCode == 
			InitialLoginConfiguration.DEFAULT_PROMO_CODE
		
		and: "tutorial info matches"
		compareTutorialInfo(ilcResult, defaultILC)

		and: "interest Tags match"
		interestTags(ilcResult).sort().equals(interestTags(defaultILC).sort())
		
		and: "bookmarks match"
		bookmarks(ilcResult).sort().equals(bookmarks(defaultILC).sort())
	}
}

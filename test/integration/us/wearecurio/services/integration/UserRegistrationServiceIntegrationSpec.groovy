package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Entry
import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.InterestArea
import us.wearecurio.model.InterestAreaBoolean
import us.wearecurio.model.InterestSubject
import us.wearecurio.model.InterestSubjectBoolean
import us.wearecurio.model.Tag
import us.wearecurio.model.TutorialInfo
import us.wearecurio.model.User

import us.wearecurio.services.UserRegistrationService
import us.wearecurio.services.UserRegistrationService.PromoCodeLookupResult

import us.wearecurio.utility.Utils

class UserRegistrationServiceIntegrationSpec extends IntegrationSpec {
	static transactional = true
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
		Utils.save(user,true)
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
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
    void "test register(user,#promoCode) returns #lookupResult"() {
		when: "setUserPromoCode is called"
		Map results
		if (promoCode == "none"){
			results = userRegistrationService.register(user)
		} else {
			results = userRegistrationService.register(user, promoCode)
		}
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == lookupResult	

		and: "the default promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig		
		ilcResult.promoCode == defaultILC.promoCode
		compareTutorialInfo(ilcResult, defaultILC)
		
		and: "interestArea is set to default"
		ilcResult.interestAreas
		ilcResult.interestAreas.size() == 
			defaultILC.interestAreas.size()
		ilcResult.interestAreas[0].preSelect == 
			defaultILC.interestAreas[0].preSelect
		ilcResult.interestAreas[0].interest
		ilcResult.interestAreas[0].interest.name == 
			defaultILC.interestAreas[0].interest.name
		
		and: "interestSubject is set to default"
		ilcResult.interestAreas[0].interest.interestSubjects
		ilcResult.interestAreas[0].interest.interestSubjects.size() == 
			defaultILC.interestAreas[0].interest.interestSubjects.size()
		ilcResult.interestAreas[0].interest.interestSubjects[0].subject
		ilcResult
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.name ==
				defaultILC
					.interestAreas[0]
					.interest
					.interestSubjects[0]
					.subject
					.name
		ilcResult
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.description ==
				defaultILC
					.interestAreas[0]
					.interest
					.interestSubjects[0]
					.subject
					.description
		
		and: "user interest tags are set to default"
		ilcResult
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.interestTags
		interestTags(ilcResult).size() == 1
		interestTags(ilcResult).find{interestTags(defaultILC)[0]}
		
		and: "user bookmarks are set to default"
		ilcResult
			.interestAreas[0]
			.interest
			.interestSubjects[0]
			.subject
			.bookmarks
		bookmarks(ilcResult).size() == 1
		bookmarks(ilcResult).find{bookmarks(defaultILC)[0]}
				
		where:
		promoCode								| lookupResult
		"none"									| PromoCodeLookupResult.UNSPECIFIED
		""										| PromoCodeLookupResult.UNSPECIFIED
		null									| PromoCodeLookupResult.UNSPECIFIED
		InitialLoginConfiguration.DEFAULT_PROMO_CODE | PromoCodeLookupResult.FOUND
		"invalid"								| PromoCodeLookupResult.INVALID
    }	

    //@spock.lang.Ignore
	void "test register valid promo code specified"() {
		given: "a promo code"
		String code = "test-promo"
		
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
		
		when: "register is called"
		Map results = userRegistrationService.register(user, code)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == 
			PromoCodeLookupResult.FOUND
		
		and: "the saved promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig
		compareTutorialInfo(ilcResult, ilc)
		ilcResult.tutorialInfo.trackExample1 == 
			trackExample1
		ilcResult.tutorialInfo.sampleQuestionRatingExampleAnswer1 == 
			sampleQuestionRatingExampleAnswer1
		interestTags(ilcResult).size() == 2
		interestTags(ilcResult).find{tags[0]}
		interestTags(ilcResult).find{tags[1]}
		bookmarks(ilcResult).size() == 2
		bookmarks(ilcResult).find{bkmrks[0]}
		bookmarks(ilcResult).find{bkmrks[1]}
    }

	//@spock.lang.Ignore
	void "test register new default data"() {
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
		
		when: "register is called"
		Map results = userRegistrationService.register(user)
		
		then: "results are successfully returned"
		results
		results.success
		
		and: "the promo code lookup result is correct"
		results.promoCodeLookupResult == 
			PromoCodeLookupResult.UNSPECIFIED
		
		and: "the saved promo object is returned"
		InitialLoginConfiguration ilcResult = results.initialLoginConfig	
		ilcResult.promoCode == defaultILC.promoCode
		compareTutorialInfo(ilcResult, defaultILC)
		ilcResult.tutorialInfo.trackExample2 == 
			trackExample2
		ilcResult.tutorialInfo.sampleQuestionRatingExampleAnswer2 == 
			sampleQuestionRatingExampleAnswer2
		
		interestTags(ilcResult).size() == 3
		interestTags(ilcResult).find{it == tags[0]}
		interestTags(ilcResult).find{it == tags[1]}
		interestTags(ilcResult).find{it == tags[2]}
		interestTags(defaultILC).size() == 3
		interestTags(ilcResult).find{interestTags(defaultILC)[0]}
		interestTags(ilcResult).find{interestTags(defaultILC)[1]}
		interestTags(ilcResult).find{interestTags(defaultILC)[2]}
		interestTags(ilcResult).size() == 3
		bookmarks(ilcResult).find{it == bkmrks[0]}
		bookmarks(ilcResult).find{it == bkmrks[1]}
		bookmarks(ilcResult).find{it == bkmrks[2]}
		bookmarks(defaultILC).size() == 3
		bookmarks(ilcResult.find{bookmarks(defaultILC)[0]})
		bookmarks(ilcResult.find{bookmarks(defaultILC)[1]})
		bookmarks(ilcResult.find{bookmarks(defaultILC)[2]})
    }
	
	//@spock.lang.IgnoreRest
	//@spock.lang.Ignore
	void "test bookmark"(){
		when: "bookmark is created"
		us.wearecurio.model.Entry.createBookmark(user.id,"sleep 8 hours")
		
		then: "user has bookmark tag"
		def tags = User.getTags(user.id)
		tags != null
		tags.size() == 1
		tags.find{ it.description == "sleep [time]" }
	}
	
	//@spock.lang.IgnoreRest
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test selectPromoCodeDefaults(user,#promoCode)"(){
		when: "selectPromoCodeDefaults is called"
		if (promoCode == "none"){
			userRegistrationService.selectPromoCodeDefaults(user)
		} else {
			userRegistrationService.selectPromoCodeDefaults(user, promoCode)
		}
			
		then: "user interest tags are set to default"
		interestTags(defaultILC).each{ 
			user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set to default"
		def tags = User.getTags(user.id)
		tags != null
		tags.size() == bookmarks(defaultILC).size()
		tags.find{ it.description == "sleep [time]" }

		where:
		promoCode << [
			"none",
			"",
			null,
			"invalid"
		]
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test selectInitialLoginConfigurationDefaults with #description"(){
		when: "selectInitialLoginConfigurationDefaults is called"
		userRegistrationService.selectInitialLoginConfigurationDefaults(
			user, 
			configOb
		)
			
		then: "user interest tags are set to default"
		interestTags(defaultILC).each{ 
			user.hasInterestTag(Tag.create(it)) }
		
		and: "user bookmarks are set to default"
		def tags = User.getTags(user.id)
		tags != null
		tags.size() == bookmarks(defaultILC).size()
		tags.find{ it.description == "sleep [time]" }

		where:
		configOb		| description
		null			| "null object"
		InitialLoginConfiguration.defaultConfiguration() | "default configuration"
	}
	
	//@spock.lang.IgnoreRest
	//@spock.lang.Ignore
	void "test selectInitialLoginConfigurationDefaults with custom config"(){
		given: "a custom InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.create()
		ilc.promoCode = "test-promo-code-2"
		Utils.save(ilc,true)
		
		and: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"test-subject-custom",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area name" 
		String name = "test-interest-area"
		
		and: "an interest area with that name"
		InterestArea area = InterestArea.create()
		area.name = name
		area.addToInterestSubjects(new InterestSubjectBoolean(subject,true))
		Utils.save(area,true)
		
		and: "test area is added to configuration"
		ilc.addToInterestAreas(new InterestAreaBoolean(area,true))
		
		when: "selectInitialLoginConfigurationDefaults is called"
		userRegistrationService.selectInitialLoginConfigurationDefaults(user, ilc)
			
		then: "user interest tags are set to custom"
		user.hasInterestTag(Tag.create("running"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "run [distance]"}
	}
	
	//@spock.lang.Ignore
	void "test selectInitialLoginConfigurationDefaults with custom config with preselect area off"(){
		given: "a custom InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.create()
		ilc.promoCode = "test-promo-code-custom-1"
		Utils.save(ilc,true)
				
		and: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"test-subject-custom-preselect-1",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"test-subject-custom-preselect-2",
				["eating"],
				["eat 1000 calories"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject3 = 
			InterestSubject.createInterestSubject(
				"test-subject-custom-preselect-3",
				["walking"],
				["walk 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "three interest area names" 
		String name1 = "test-interest-area-preselect-1"
		String name2 = "test-interest-area-preselect-2"
		String name3 = "test-interest-area-preselect-3"
		
		and: "an interest areas"
		InterestArea area1 = InterestArea.create()
		area1.name = name1
		area1.addToInterestSubjects(new InterestSubjectBoolean(subject1,true))
		Utils.save(area1,true)
		
		and: "another interest areas"
		InterestArea area2 = InterestArea.create()
		area2.name = name2
		area2.addToInterestSubjects(new InterestSubjectBoolean(subject2,true))
		Utils.save(area2,true)
		
		and: "another interest areas"
		InterestArea area3 = InterestArea.create()
		area3.name = name3
		area3.addToInterestSubjects(new InterestSubjectBoolean(subject3,true))
		Utils.save(area3,true)
		
		and: "two test areas ares added to configuration with preselect on"
		ilc.addToInterestAreas(new InterestAreaBoolean(area1,true))
		ilc.addToInterestAreas(new InterestAreaBoolean(area3,true))
		
		and: "test areas ares added to configuration with preselect off"
		ilc.addToInterestAreas(new InterestAreaBoolean(area2,false))
		
		when: "selectInitialLoginConfigurationDefaults is called"
		userRegistrationService.selectInitialLoginConfigurationDefaults(user, ilc)
			
		then: "user interest tags are set to custom with preselect true"
		user.hasInterestTag(Tag.create("running"))
		user.hasInterestTag(Tag.create("walking"))

		and: "user does not have interest tag, despite preselect false"
		!user.hasInterestTag(Tag.create("eating"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 2
		resultBkmrks.find {it.description == "run [distance]"}
		resultBkmrks.find {it.description == "walk [distance]"}
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test selectInterestAreaDefaults with '#interestArea'"(){
		when: "selectInterestAreaDefaults is called"
		userRegistrationService.selectInterestAreaDefaults(user, interestArea)
			
		then: "user has no interest tags"
		user.interestTags == null || 
			user.interestTags.size() == 0
		
		and: "user has no bookmarks"
		def tags = User.getTags(user.id)
		tags == null || tags.size() == 0
		
		where:
		interestArea << [
			"",
			null,
			"invalid"
		]
	}
	
	//@spock.lang.Ignore
	void "test selectInterestAreaDefaults with with valid interest area"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"test-subject",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area name" 
		String name = "test-interest-area"
		
		and: "an interest area with that name"
		InterestArea area = InterestArea.create()
		area.name = name
		area.addToInterestSubjects(new InterestSubjectBoolean(subject,true))
		Utils.save(area,true)
				
		when: "selectInterestAreaDefaults is called"
		userRegistrationService.selectInterestAreaDefaults(user, name)
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("running"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "run [distance]"}
	}	
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test selectInterestAreasDefaultsByStrings with '#interestArea'"(){
		when: "selectInterestAreasDefaultsByStrings is called"
		userRegistrationService.selectInterestAreasDefaultsByStrings(user, interestArea)
			
		then: "user has no interest tags"
		user.interestTags == null || 
			user.interestTags.size() == 0
		
		and: "user has no bookmarks"
		def tags = User.getTags(user.id)
		tags == null || tags.size() == 0
		
		where:
		interestArea << [
			null,
			[""],
			[null],
			["invalid"],
			[null,"","invalid"]
		]
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test selectInterestAreasDefaultsByStrings with #description"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"goldman",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area"
		InterestArea area = InterestArea.create()
		area.name = name
		area.addToInterestSubjects(new InterestSubjectBoolean(subject,true))
		Utils.save(area,true)
				
		when: "selectInterestAreasDefaultsByStrings is called"
		userRegistrationService.selectInterestAreasDefaultsByStrings(user, interestAreas)
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("running"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "run [distance]"}
		
		where:
		name					| interestAreas	| description
		"test-interest-area-1"	| ["test-interest-area-1"]		| "single valid interest area"
		"test-interest-area-2"	| ["test-interest-area-2",null]	| "valid and null interest area"
		"test-interest-area-3"	| ["test-interest-area-3",""]		| "valid and empty interest area"
		"test-interest-area-4"	| ["test-interest-area-4","invalid"] | "valid and invalid interest area"
		"test-interest-area-5"	| [null,"test-interest-area-5"]	| "null and valid interest area"
		"test-interest-area-6"	| ["","test-interest-area-6"]		| "empty and valid interest area"
		"test-interest-area-7"	| ["invalid","test-interest-area-7"]	| "invalid and valid interest area"
		"test-interest-area-8"	| [null,"test-interest-area-8","","invalid"]	| "valid and several invalid interest areas"
		"test-interest-area-9"	| ["test-interest-area-9","test-interest-area-9"] | "duplicate valid interest areas"
		"test-interest-area-10" | [null,"test-interest-area-10","","test-interest-area-10","invalid"] | "duplicate valid with invalid interest areas"
	}
	
	//@spock.lang.Ignore
	@spock.lang.Unroll
	void "test selectInterestAreasDefaultsByStrings with #description"(){
		given: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"madness",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"mayhem",
				["jogging"],
				["run 8 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area"
		InterestArea area1 = InterestArea.create()
		area1.name = nameA
		area1.addToInterestSubjects(new InterestSubjectBoolean(subject1,true))
		Utils.save(area1,true)
				
		and: "another interest area"
		InterestArea area2 = InterestArea.create()
		area2.name = nameB
		area2.addToInterestSubjects(new InterestSubjectBoolean(subject2,true))
		Utils.save(area2,true)
		
		when: "selectInterestAreasDefaultsByStrings is called"
		userRegistrationService.selectInterestAreasDefaultsByStrings(user, interestAreas)
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 2
		user.hasInterestTag(Tag.create("running"))
		user.hasInterestTag(Tag.create("jogging"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "run [distance]"}
		
		where:
		nameA		| nameB		| interestAreas			| description
		"name1-a"	| "name1-b"	| ["name1-a","name1-b"]	| "multiple valid interests"
		"name2-a"	| "name2-b"	| ["name2-a",null,"name2-b","invalid"]	| "multiple valid and invalid interests"
	}
	
	//@spock.lang.Ignore
	void "test selectInterestAreasDefaultsByStrings preSelect off"(){
		given: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"peanut",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"pinapple",
				["jogging"],
				["run 8 miles"],
				"test-subject-description",
				true
			)
		
		and: "interest area names"
		String name1 = "subject-preselect-off-test-1"
		String name2 = "subject-preselect-off-test-2"

		and: "an interest area"
		InterestArea area1 = InterestArea.create()
		area1.name = name1
		area1.addToInterestSubjects(new InterestSubjectBoolean(subject1,true))
		Utils.save(area1,true)
				
		and: "another interest area"
		InterestArea area2 = InterestArea.create()
		area2.name = name2
		area2.addToInterestSubjects(new InterestSubjectBoolean(subject2,false))
		Utils.save(area2,true)
		
		when: "selectInterestAreasDefaultsByStrings is called"
		userRegistrationService.selectInterestAreasDefaultsByStrings(user, [name1,name2])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("running"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "run [distance]"}
	}

	//@spock.lang.Ignore
	void "test selectInterestAreasDefaultsByObjects with one object"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"pogues",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area"
		InterestArea area = InterestArea.create()
		area.name = "hemingway"
		area.addToInterestSubjects(new InterestSubjectBoolean(subject,true))
		Utils.save(area,true)
				
		when: "selectInterestAreasDefaultsByObjects is called"
		userRegistrationService.selectInterestAreasDefaultsByObjects(user, [area])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("running"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "run [distance]"}
	}
	
	//@spock.lang.Ignore
	void "test selectInterestAreasDefaultsByObjects with two objects"(){
		given: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"nick",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"cave",
				["walking"],
				["walk 8 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area"
		InterestArea area1 = InterestArea.create()
		area1.name = "schulz"
		area1.addToInterestSubjects(new InterestSubjectBoolean(subject1,true))
		Utils.save(area1,true)
				
		and: "another interest area"
		InterestArea area2 = InterestArea.create()
		area2.name = "alexie"
		area2.addToInterestSubjects(new InterestSubjectBoolean(subject2,true))
		Utils.save(area2,true)
				
		when: "selectInterestAreasDefaultsByObjects is called"
		userRegistrationService.selectInterestAreasDefaultsByObjects(user, [area1,area2])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 2
		user.hasInterestTag(Tag.create("running"))
		user.hasInterestTag(Tag.create("walking"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 2
		resultBkmrks.find {it.description == "run [distance]"}
		resultBkmrks.find {it.description == "walk [distance]"}
	}
		
	//@spock.lang.Ignore
	void "test selectInterestAreasDefaultsByObjects with same two objects"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"joe",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area"
		InterestArea area = InterestArea.create()
		area.name = "tolstoy"
		area.addToInterestSubjects(new InterestSubjectBoolean(subject,true))
		Utils.save(area,true)
				
		when: "selectInterestAreasDefaultsByObjects is called"
		userRegistrationService.selectInterestAreasDefaultsByObjects(user, [area,area])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("running"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "run [distance]"}
	} 
		
	//@spock.lang.Ignore
	void "test selectInterestAreasDefaultsByObjects with two objects"(){
		given: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"bela",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"blixer",
				["walking"],
				["walk 8 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area"
		InterestArea area1 = InterestArea.create()
		area1.name = "barron"
		area1.addToInterestSubjects(new InterestSubjectBoolean(subject1,true))
		Utils.save(area1,true)
				
		and: "another interest area"
		InterestArea area2 = InterestArea.create()
		area2.name = "murakami"
		area2.addToInterestSubjects(new InterestSubjectBoolean(subject2,true))
		Utils.save(area2,true)
				
		when: "selectInterestAreasDefaultsByObjects is called"
		userRegistrationService.selectInterestAreasDefaultsByObjects(user, [area1,area2])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 2
		user.hasInterestTag(Tag.create("running"))
		user.hasInterestTag(Tag.create("walking"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 2
		resultBkmrks.find {it.description == "run [distance]"}
		resultBkmrks.find {it.description == "walk [distance]"}
	}
	
	//@spock.lang.Ignore
	void "test selectInterestAreasDefaultsByObjects with multiple objects"(){
		given: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"mick",
				["running"],
				["run 2 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"jones",
				["walking"],
				["walk 8 miles"],
				"test-subject-description",
				true
			)
		
		and: "an interest area"
		InterestArea area1 = InterestArea.create()
		area1.name = "harry"
		area1.addToInterestSubjects(new InterestSubjectBoolean(subject1,true))
		Utils.save(area1,true)
				
		and: "another interest area"
		InterestArea area2 = InterestArea.create()
		area2.name = "lime"
		area2.addToInterestSubjects(new InterestSubjectBoolean(subject2,true))
		Utils.save(area2,true)
				
		when: "selectInterestAreasDefaultsByObjects is called"
		userRegistrationService.selectInterestAreasDefaultsByObjects(user, [area1,null,area2,area1,null,null])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 2
		user.hasInterestTag(Tag.create("running"))
		user.hasInterestTag(Tag.create("walking"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 2
		resultBkmrks.find {it.description == "run [distance]"}
		resultBkmrks.find {it.description == "walk [distance]"}
	}
	
	@spock.lang.Unroll
	void "test selectInterestAreasDefaultsByObjects with #description"(){
		when: "selectInterestAreasDefaultsByObjects is called with null"
		userRegistrationService.selectInterestAreasDefaultsByObjects(user,interestAreas)
		
		then: "no interest tags are set"
		user.interestTags == null ||
			user.interestTags.size() == 0
		
		and: "no bookmarks are set"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks == null ||
			resultBkmrks.size() == 0
		
		where:
		interestAreas		| description
		null				| "null"
		[]					| "empty list"
		[null]				| "list with one null"
		[null,null,null]	| "list with multiple nulls"
	}
	
	@spock.lang.Unroll
	void "test selectInterestSubject with #subject"(){
		when: "selectInterestSubject is called"
		userRegistrationService.selectInterestSubject(user,subject)
		
		then: "no interest tags are set"
		user.interestTags == null ||
			user.interestTags.size() == 0
		
		and: "no bookmarks are set"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks == null ||
			resultBkmrks.size() == 0
		
		where:
		subject << [
		"",
		"invalid"
		]
	}

	//@spock.lang.IgnoreRest
	void "test selectInterestSubject with null string"(){
		when: "selectInterestSubject is called"
		userRegistrationService.selectInterestSubject(user,(String)null)
		
		then: "no interest tags are set"
		user.interestTags == null ||
			user.interestTags.size() == 0
		
		and: "no bookmarks are set"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks == null ||
			resultBkmrks.size() == 0
	}
	
	//@spock.lang.IgnoreRest
	void "test selectInterestSubject with null object"(){
		when: "selectInterestSubject is called"
		userRegistrationService.selectInterestSubject(user,(InterestSubject)null)
		
		then: "no interest tags are set"
		user.interestTags == null ||
			user.interestTags.size() == 0
		
		and: "no bookmarks are set"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks == null ||
			resultBkmrks.size() == 0
	}
	
	void "test selectInterestSubject with valid subject string"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"joey",
				["eating"],
				["eat 1500 calories"],
				"test-subject-description",
				true
			)
		
		when: "selectInterestSubject is called"
		userRegistrationService.selectInterestSubject(user,subject.name)
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("eating"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "eat [calories]"}
	}
	
	void "test selectInterestSubject with valid subject object"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"flannery",
				["swimming"],
				["swim 1 miles"],
				"test-subject-description",
				true
			)
		
		when: "selectInterestSubject is called"
		userRegistrationService.selectInterestSubject(user,subject)
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("swimming"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "swim [distance]"}
	}

	@spock.lang.Unroll
	void "test selectInterestSubjectsByStrings with #subjects"(){
		when: "selectInterestSubjectsByStrings is called"
		userRegistrationService.selectInterestSubjectsByStrings(user, subjects)
		
		then: "no interest tags are set"
		user.interestTags == null ||
			user.interestTags.size() == 0
		
		and: "no bookmarks are set"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks == null ||
			resultBkmrks.size() == 0
		
		where:
		subjects << [
		null,
		[null],
		[""],
		["invalid"],
		[null,"","invalid","",null,"invalid"]
		]		
	}
	
	void "test selectInterestSubjectsByStrings with single valid string"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"fantagraphic",
				["biking"],
				["bike 10 miles"],
				"test-subject-description",
				true
			)
		
		when: "selectInterestSubjectsByStrings is called"
		userRegistrationService.selectInterestSubjectsByStrings(user, [subject.name])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("biking"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "bike [distance]"}
	}
	
	void "test selectInterestSubjectsByStrings with multiple strings"(){
		given: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"chicago",
				["jogging"],
				["run 3 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"detroit",
				["skating"],
				["skate 3 miles"],
				"test-subject-description",
				true
			)
		
		when: "selectInterestSubjectsByStrings is called"
		userRegistrationService.selectInterestSubjectsByStrings(user, [subject1.name,null,subject2.name,subject1.name,"","invalid",null])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 2
		user.hasInterestTag(Tag.create("jogging"))
		user.hasInterestTag(Tag.create("skating"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 2
		resultBkmrks.find {it.description == "run [distance]"}
		resultBkmrks.find {it.description == "skate [distance]"}
	}
	
	@spock.lang.Unroll
	void "test selectInterestSubjectsByObjects with #subjects"(){
		when: "selectInterestSubjectsByObjects is called"
		userRegistrationService.selectInterestSubjectsByObjects(user, subjects)
		
		then: "no interest tags are set"
		user.interestTags == null ||
			user.interestTags.size() == 0
		
		and: "no bookmarks are set"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks == null ||
			resultBkmrks.size() == 0
		
		where:
		subjects << [
		null,
		[null],
		[null,null,null]
		]		
	}
	
	void "test selectInterestSubjectsByObjects with single object"(){
		given: "an interest subject"
		InterestSubject subject = 
			InterestSubject.createInterestSubject(
				"books",
				["biking"],
				["bike 10 miles"],
				"test-subject-description",
				true
			)
		
		when: "selectInterestSubjectsByObjects is called"
		userRegistrationService.selectInterestSubjectsByObjects(user, [subject])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 1
		user.hasInterestTag(Tag.create("biking"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 1
		resultBkmrks.find {it.description == "bike [distance]"}
	}
	
	void "test selectInterestSubjectsByObjects with multiple objects"(){
		given: "an interest subject"
		InterestSubject subject1 = 
			InterestSubject.createInterestSubject(
				"la",
				["running"],
				["run 4 miles"],
				"test-subject-description",
				true
			)
		
		and: "another interest subject"
		InterestSubject subject2 = 
			InterestSubject.createInterestSubject(
				"nyc",
				["walking"],
				["walk 2 miles"],
				"test-subject-description",
				true
			)
		
		when: "selectInterestSubjectsByObjects is called"
		userRegistrationService.selectInterestSubjectsByObjects(user, [subject1,null,subject2,subject1,null,null,null])
			
		then: "user interest tags are set to custom"
		user.interestTags.size() == 2
		user.hasInterestTag(Tag.create("running"))
		user.hasInterestTag(Tag.create("walking"))
		
		and: "user bookmarks are set to default"
		def resultBkmrks = User.getTags(user.id)
		resultBkmrks != null
		resultBkmrks.size() == 2
		resultBkmrks.find {it.description == "run [distance]"}
		resultBkmrks.find {it.description == "walk [distance]"}
	}
}

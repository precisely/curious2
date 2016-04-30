package us.wearecurio.model.unit

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.model.InitialLoginConfiguration
import us.wearecurio.model.InterestArea
import us.wearecurio.model.InterestAreaBoolean
import us.wearecurio.model.InterestSubject
import us.wearecurio.model.InterestSubjectBoolean
import us.wearecurio.model.TutorialInfo

import us.wearecurio.utility.Utils
/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([InitialLoginConfiguration, TutorialInfo])
class InitialLoginConfigurationSpec extends Specification {
    void "test default"() {
		when: "given the default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		and: "given the default tutorialInfo"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		then: "defaultILC properties set to hard-coded values"
		defaultILC.promoCode == InitialLoginConfiguration.DEFAULT_PROMO_CODE
		defaultILC.tutorialInfo.customQuestion1 == defaultTI.customQuestion1
		defaultILC.tutorialInfo.customQuestion2 == defaultTI.customQuestion2
		defaultILC.tutorialInfo.trackExample1 == defaultTI.trackExample1
		defaultILC.tutorialInfo.trackExample2 == defaultTI.trackExample2
		defaultILC.tutorialInfo.trackExample3 == defaultTI.trackExample3
		defaultILC.tutorialInfo.trackExample4 == defaultTI.trackExample4
		defaultILC.tutorialInfo.deviceExample == defaultTI.deviceExample
		defaultILC.tutorialInfo.sampleQuestionDuration == defaultTI.sampleQuestionDuration
		defaultILC.tutorialInfo.sampleQuestionDurationExampleAnswers == defaultTI.sampleQuestionDurationExampleAnswers
		defaultILC.tutorialInfo.sampleQuestionRating == defaultTI.sampleQuestionRating
		defaultILC.tutorialInfo.sampleQuestionRatingRange == defaultTI.sampleQuestionRatingRange
		defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer1 == defaultTI.sampleQuestionRatingExampleAnswer1
		defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer2 == defaultTI.sampleQuestionRatingExampleAnswer2
		defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer3 == defaultTI.sampleQuestionRatingExampleAnswer3
		defaultILC.tutorialInfo.today1 == defaultTI.today1
		defaultILC.tutorialInfo.today1Example == defaultTI.today1Example
		defaultILC.tutorialInfo.today2 == defaultTI.today2
		defaultILC.tutorialInfo.today2Example == defaultTI.today2Example
		defaultILC.tutorialInfo.today3 == defaultTI.today3
		defaultILC.tutorialInfo.today3Example == defaultTI.today3Example
		defaultILC.tutorialInfo.today4 == defaultTI.today4
		defaultILC.tutorialInfo.today4Example == defaultTI.today4Example
		defaultILC.interestAreas.size() == 1
		defaultILC.interestAreas[0].interest.name == "sleep"
		defaultILC.interestAreas[0].preSelect
		defaultILC.interestAreas[0].interest.interestSubjects.size() == 1
		defaultILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags.find{it == "sleep"}
		defaultILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks.find{it == "sleep 8 hours"}
		defaultILC.interestAreas[0].interest.interestSubjects[0].preSelect
    }

	void "test default returns saved modified values"() {
		given: "the default InitialLoginConfiguration object"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()

		and: "the default tutorial info"
		TutorialInfo ti = TutorialInfo.defaultTutorialInfo()
		
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
		ti.customQuestion1 = customQuestion1
		ti.customQuestion2 = customQuestion2
		ti.trackExample1 = trackExample1
		ti.trackExample2 = trackExample2
		ti.trackExample3 = trackExample3
		ti.deviceExample = deviceExample
		ti.sampleQuestionDuration = sampleQuestionDuration
		ti.sampleQuestionDurationExampleAnswers = sampleQuestionDurationExampleAnswers
		ti.sampleQuestionRating = sampleQuestionRating
		ti.sampleQuestionRatingRange = sampleQuestionRatingRange
		ti.sampleQuestionRatingExampleAnswer1 = sampleQuestionRatingExampleAnswer1
		ti.sampleQuestionRatingExampleAnswer2 = sampleQuestionRatingExampleAnswer2
		ti.sampleQuestionRatingExampleAnswer3 = sampleQuestionRatingExampleAnswer3
		ti.today1 = today1
		ti.today1Example = today1Example
		ti.today2 = today2
		ti.today2Example = today2Example
		ti.today3 = today3
		ti.today3Example = today3Example
		ti.today4 = today4
		ti.today4Example = today4Example
		Utils.save(ti,true)
		defaultILC.interestAreas[0].interest.interestSubjects[0].subject.addToInterestTags(interestTags[0])
		defaultILC.interestAreas[0].interest.interestSubjects[0].subject.addToBookmarks(bookmarks[0])
		
		and: "defaultILC object is saved"
		Utils.save(defaultILC,true)
		
		and: "default is requested"
		InitialLoginConfiguration newDefault = InitialLoginConfiguration.defaultConfiguration()
		
		then: "returned InitialLoginConfiguration has default promoCode"
		newDefault.promoCode == InitialLoginConfiguration.DEFAULT_PROMO_CODE
		
		and: "other values reflect the saved values"
		newDefault.tutorialInfo.customQuestion1 == customQuestion1
		newDefault.tutorialInfo.customQuestion2 == customQuestion2
		newDefault.tutorialInfo.trackExample1 == trackExample1
		newDefault.tutorialInfo.trackExample2 == trackExample2
		newDefault.tutorialInfo.trackExample3 == trackExample3
		newDefault.tutorialInfo.deviceExample == deviceExample
		newDefault.tutorialInfo.sampleQuestionDuration == sampleQuestionDuration
		newDefault.tutorialInfo.sampleQuestionDurationExampleAnswers == sampleQuestionDurationExampleAnswers
		newDefault.tutorialInfo.sampleQuestionRating == sampleQuestionRating
		newDefault.tutorialInfo.sampleQuestionRatingRange == sampleQuestionRatingRange
		newDefault.tutorialInfo.sampleQuestionRatingExampleAnswer1 == sampleQuestionRatingExampleAnswer1
		newDefault.tutorialInfo.sampleQuestionRatingExampleAnswer2 == sampleQuestionRatingExampleAnswer2
		newDefault.tutorialInfo.sampleQuestionRatingExampleAnswer3 == sampleQuestionRatingExampleAnswer3
		newDefault.tutorialInfo.today1 == today1
		newDefault.tutorialInfo.today1Example == today1Example
		newDefault.tutorialInfo.today2 == today2
		newDefault.tutorialInfo.today2Example == today2Example
		newDefault.tutorialInfo.today3 == today3
		newDefault.tutorialInfo.today3Example == today3Example
		newDefault.tutorialInfo.today4 == today4
		newDefault.tutorialInfo.today4Example == today4Example
		newDefault.interestAreas[0].interest.interestSubjects[0].subject.interestTags.find{it == interestTags[0]}
		newDefault.interestAreas[0].interest.interestSubjects[0].subject.bookmarks.find{it == bookmarks[0]}
	}
	
	void "test createFromDefault without promoCode"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration defaultILC = InitialLoginConfiguration.defaultConfiguration()
		
		when: "a new ilc created from default InitialLoginConfiguration"
		InitialLoginConfiguration newILC = InitialLoginConfiguration.createFromDefault()
		
		then: "new ilc promo code is blank"
		(newILC.promoCode == null) || (newILC.promoCode == "")
		
		and: "rest of fields of new ilc are identical to default ilc"
		newILC.tutorialInfo.customQuestion1 == defaultILC.tutorialInfo.customQuestion1
		newILC.tutorialInfo.customQuestion2 == defaultILC.tutorialInfo.customQuestion2
		newILC.tutorialInfo.trackExample1 == defaultILC.tutorialInfo.trackExample1
		newILC.tutorialInfo.trackExample2 == defaultILC.tutorialInfo.trackExample2
		newILC.tutorialInfo.trackExample3 == defaultILC.tutorialInfo.trackExample3
		newILC.tutorialInfo.deviceExample == defaultILC.tutorialInfo.deviceExample
		newILC.tutorialInfo.sampleQuestionDuration == defaultILC.tutorialInfo.sampleQuestionDuration
		newILC.tutorialInfo.sampleQuestionDurationExampleAnswers == defaultILC.tutorialInfo.sampleQuestionDurationExampleAnswers
		newILC.tutorialInfo.sampleQuestionRating == defaultILC.tutorialInfo.sampleQuestionRating
		newILC.tutorialInfo.sampleQuestionRatingRange == defaultILC.tutorialInfo.sampleQuestionRatingRange
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer1 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer1
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer2 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer2
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer3 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer3
		newILC.tutorialInfo.today1 == defaultILC.tutorialInfo.today1
		newILC.tutorialInfo.today1Example == defaultILC.tutorialInfo.today1Example
		newILC.tutorialInfo.today2 == defaultILC.tutorialInfo.today2
		newILC.tutorialInfo.today2Example == defaultILC.tutorialInfo.today2Example
		newILC.tutorialInfo.today3 == defaultILC.tutorialInfo.today3
		newILC.tutorialInfo.today3Example == defaultILC.tutorialInfo.today3Example
		newILC.tutorialInfo.today4 == defaultILC.tutorialInfo.today4
		newILC.tutorialInfo.today4Example == defaultILC.tutorialInfo.today4Example
		
		newILC.interestAreas
		newILC.interestAreas[0].interest.interestSubjects
		newILC.interestAreas[0].interest.interestSubjects[0]
		newILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags
		newILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks
		
		defaultILC.interestAreas
		defaultILC.interestAreas[0].interest.interestSubjects
		defaultILC.interestAreas[0].interest.interestSubjects[0]
		defaultILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags
		defaultILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks

		newILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags.find{it == defaultILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags[0]}
		newILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks.find{it == defaultILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks[0]}
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
		newILC.tutorialInfo.customQuestion1 == defaultILC.tutorialInfo.customQuestion1
		newILC.tutorialInfo.customQuestion2 == defaultILC.tutorialInfo.customQuestion2
		newILC.tutorialInfo.trackExample1 == defaultILC.tutorialInfo.trackExample1
		newILC.tutorialInfo.trackExample2 == defaultILC.tutorialInfo.trackExample2
		newILC.tutorialInfo.trackExample3 == defaultILC.tutorialInfo.trackExample3
		newILC.tutorialInfo.deviceExample == defaultILC.tutorialInfo.deviceExample
		newILC.tutorialInfo.sampleQuestionDuration == defaultILC.tutorialInfo.sampleQuestionDuration
		newILC.tutorialInfo.sampleQuestionDurationExampleAnswers == defaultILC.tutorialInfo.sampleQuestionDurationExampleAnswers
		newILC.tutorialInfo.sampleQuestionRating == defaultILC.tutorialInfo.sampleQuestionRating
		newILC.tutorialInfo.sampleQuestionRatingRange == defaultILC.tutorialInfo.sampleQuestionRatingRange
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer1 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer1
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer2 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer2
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer3 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer3
		newILC.tutorialInfo.today1 == defaultILC.tutorialInfo.today1
		newILC.tutorialInfo.today1Example == defaultILC.tutorialInfo.today1Example
		newILC.tutorialInfo.today2 == defaultILC.tutorialInfo.today2
		newILC.tutorialInfo.today2Example == defaultILC.tutorialInfo.today2Example
		newILC.tutorialInfo.today3 == defaultILC.tutorialInfo.today3
		newILC.tutorialInfo.today3Example == defaultILC.tutorialInfo.today3Example
		newILC.tutorialInfo.today4 == defaultILC.tutorialInfo.today4
		newILC.tutorialInfo.today4Example == defaultILC.tutorialInfo.today4Example
		newILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags.find{it == defaultILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags[0]}
		newILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks.find{it == defaultILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks[0]}
		
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
		newILC.tutorialInfo.customQuestion1 == defaultILC.tutorialInfo.customQuestion1
		newILC.tutorialInfo.customQuestion2 == defaultILC.tutorialInfo.customQuestion2
		newILC.tutorialInfo.trackExample1 == defaultILC.tutorialInfo.trackExample1
		newILC.tutorialInfo.trackExample2 == defaultILC.tutorialInfo.trackExample2
		newILC.tutorialInfo.trackExample3 == defaultILC.tutorialInfo.trackExample3
		newILC.tutorialInfo.deviceExample == defaultILC.tutorialInfo.deviceExample
		newILC.tutorialInfo.sampleQuestionDuration == defaultILC.tutorialInfo.sampleQuestionDuration
		newILC.tutorialInfo.sampleQuestionDurationExampleAnswers == defaultILC.tutorialInfo.sampleQuestionDurationExampleAnswers
		newILC.tutorialInfo.sampleQuestionRating == defaultILC.tutorialInfo.sampleQuestionRating
		newILC.tutorialInfo.sampleQuestionRatingRange == defaultILC.tutorialInfo.sampleQuestionRatingRange
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer1 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer1
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer2 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer2
		newILC.tutorialInfo.sampleQuestionRatingExampleAnswer3 == defaultILC.tutorialInfo.sampleQuestionRatingExampleAnswer3
		newILC.tutorialInfo.today1 == defaultILC.tutorialInfo.today1
		newILC.tutorialInfo.today1Example == defaultILC.tutorialInfo.today1Example
		newILC.tutorialInfo.today2 == defaultILC.tutorialInfo.today2
		newILC.tutorialInfo.today2Example == defaultILC.tutorialInfo.today2Example
		newILC.tutorialInfo.today3 == defaultILC.tutorialInfo.today3
		newILC.tutorialInfo.today3Example == defaultILC.tutorialInfo.today3Example
		newILC.tutorialInfo.today4 == defaultILC.tutorialInfo.today4
		newILC.tutorialInfo.today4Example == defaultILC.tutorialInfo.today4Example
		newILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags.find{defaultILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags[0]}
		newILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks.find{defaultILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks[0]}
		
		when: "customLogin searched by promoCode"
		InitialLoginConfiguration result = InitialLoginConfiguration.findByPromoCode(promoCode)
		
		then: "new ILC is saved, so result matches new ilc promo code"
		result.promoCode == newILC.promoCode
		
		and: "result matches rest of new ilc fields"
		result.tutorialInfo.customQuestion1 == newILC.tutorialInfo.customQuestion1
		result.tutorialInfo.customQuestion2 == newILC.tutorialInfo.customQuestion2
		result.tutorialInfo.trackExample1 == newILC.tutorialInfo.trackExample1
		result.tutorialInfo.trackExample2 == newILC.tutorialInfo.trackExample2
		result.tutorialInfo.trackExample3 == newILC.tutorialInfo.trackExample3
		result.tutorialInfo.deviceExample == newILC.tutorialInfo.deviceExample
		result.tutorialInfo.sampleQuestionDuration == newILC.tutorialInfo.sampleQuestionDuration
		result.tutorialInfo.sampleQuestionDurationExampleAnswers == newILC.tutorialInfo.sampleQuestionDurationExampleAnswers
		result.tutorialInfo.sampleQuestionRating == newILC.tutorialInfo.sampleQuestionRating
		result.tutorialInfo.sampleQuestionRatingRange == newILC.tutorialInfo.sampleQuestionRatingRange
		result.tutorialInfo.sampleQuestionRatingExampleAnswer1 == newILC.tutorialInfo.sampleQuestionRatingExampleAnswer1
		result.tutorialInfo.sampleQuestionRatingExampleAnswer2 == newILC.tutorialInfo.sampleQuestionRatingExampleAnswer2
		result.tutorialInfo.sampleQuestionRatingExampleAnswer3 == newILC.tutorialInfo.sampleQuestionRatingExampleAnswer3
		result.tutorialInfo.today1 == newILC.tutorialInfo.today1
		result.tutorialInfo.today1Example == newILC.tutorialInfo.today1Example
		result.tutorialInfo.today2 == newILC.tutorialInfo.today2
		result.tutorialInfo.today2Example == newILC.tutorialInfo.today2Example
		result.tutorialInfo.today3 == newILC.tutorialInfo.today3
		result.tutorialInfo.today3Example == newILC.tutorialInfo.today3Example
		result.tutorialInfo.today4 == newILC.tutorialInfo.today4
		result.tutorialInfo.today4Example == newILC.tutorialInfo.today4Example
		result.interestAreas[0].interest.interestSubjects[0].subject.interestTags.find{it == newILC.interestAreas[0].interest.interestSubjects[0].subject.interestTags[0]}
		result.interestAreas[0].interest.interestSubjects[0].subject.bookmarks.find{it == newILC.interestAreas[0].interest.interestSubjects[0].subject.bookmarks[0]}
	}
	
	void "test copy"() {
		given: "an InitialLoginConfiguration"
		InitialLoginConfiguration to = InitialLoginConfiguration.createFromDefault("to-promo-code",true)
		
		and: "another InitialLoginConfiguration"
		InitialLoginConfiguration from = InitialLoginConfiguration.createFromDefault("from-promo-code",true)
		
		when: "first InitialLoginConfiguration has properties assigned"
		to.interestAreas[0].interest.interestSubjects[0].subject.addToInterestTags("to")
		to.interestAreas[0].interest.interestSubjects[0].subject.addToBookmarks("to")
		
		and: "second InitialLoginConfiguration has different properties assigned"
		from.interestAreas[0].interest.interestSubjects[0].subject.addToInterestTags("from")
		from.interestAreas[0].interest.interestSubjects[0].subject.addToBookmarks("from")
		
		and: "second is copied to first"
		InitialLoginConfiguration.copy(from, to)
		
		then: "promo code is not copied"
		to.promoCode != from.promoCode
		
		and: "all other properties are copied"
		to.interestAreas[0].interest.interestSubjects[0].subject.interestTags.find{it == from.interestAreas[0].interest.interestSubjects[0].subject.interestTags[0]}
		to.interestAreas[0].interest.interestSubjects[0].subject.bookmarks.find{it == from.interestAreas[0].interest.interestSubjects[0].subject.bookmarks[0]}
		
		to.tutorialInfo.customQuestion1 == from.tutorialInfo.customQuestion1
		to.tutorialInfo.customQuestion2 == from.tutorialInfo.customQuestion2
		to.tutorialInfo.trackExample1 == from.tutorialInfo.trackExample1
		to.tutorialInfo.trackExample2 == from.tutorialInfo.trackExample2
		to.tutorialInfo.trackExample3 == from.tutorialInfo.trackExample3
		to.tutorialInfo.deviceExample == from.tutorialInfo.deviceExample
		to.tutorialInfo.sampleQuestionDuration == from.tutorialInfo.sampleQuestionDuration
		to.tutorialInfo.sampleQuestionDurationExampleAnswers == from.tutorialInfo.sampleQuestionDurationExampleAnswers
		to.tutorialInfo.sampleQuestionRating == from.tutorialInfo.sampleQuestionRating
		to.tutorialInfo.sampleQuestionRatingRange == from.tutorialInfo.sampleQuestionRatingRange
		to.tutorialInfo.sampleQuestionRatingExampleAnswer1 == from.tutorialInfo.sampleQuestionRatingExampleAnswer1
		to.tutorialInfo.sampleQuestionRatingExampleAnswer2 == from.tutorialInfo.sampleQuestionRatingExampleAnswer2
		to.tutorialInfo.sampleQuestionRatingExampleAnswer3 == from.tutorialInfo.sampleQuestionRatingExampleAnswer3
		to.tutorialInfo.today1 == from.tutorialInfo.today1
		to.tutorialInfo.today1Example == from.tutorialInfo.today1Example
		to.tutorialInfo.today2 == from.tutorialInfo.today2
		to.tutorialInfo.today2Example == from.tutorialInfo.today2Example
		to.tutorialInfo.today3 == from.tutorialInfo.today3
		to.tutorialInfo.today3Example == from.tutorialInfo.today3Example
		to.tutorialInfo.today4 == from.tutorialInfo.today4
		to.tutorialInfo.today4Example == from.tutorialInfo.today4Example		
	}
	
	void "test copy for default tutorial from custom tutorial"() {
		given: "promo codes"
		String toPromo = "to-promo-code"
		String fromPromo = "from-promo-code"
		
		and: "a to InitialLoginConfiguration"
		InitialLoginConfiguration to = InitialLoginConfiguration.createFromDefault(toPromo,true)
		
		and: "a from InitialLoginConfiguration"
		InitialLoginConfiguration from = InitialLoginConfiguration.createFromDefault(fromPromo,true)
		
		and: "the default tutorial"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		when: "a custom tutorial is created for the from configuration"
		TutorialInfo fromTI = TutorialInfo.createFromDefault(fromPromo, false)
		fromTI.customQuestion1 = "from customQuestion1"
		fromTI.sampleQuestionRatingRange = "from sampleQuestionRatingRange"
		fromTI.today4 = "from today4"
		Utils.save(fromTI,true)
		
		and: "from is copied to"
		InitialLoginConfiguration.copy(from, to)

		then: "to tutorial matches the from tutorial"
		to.tutorialInfo.customQuestion1 == from.tutorialInfo.customQuestion1
		to.tutorialInfo.customQuestion2 == from.tutorialInfo.customQuestion2
		to.tutorialInfo.trackExample1 == from.tutorialInfo.trackExample1
		to.tutorialInfo.trackExample2 == from.tutorialInfo.trackExample2
		to.tutorialInfo.trackExample3 == from.tutorialInfo.trackExample3
		to.tutorialInfo.deviceExample == from.tutorialInfo.deviceExample
		to.tutorialInfo.sampleQuestionDuration == from.tutorialInfo.sampleQuestionDuration
		to.tutorialInfo.sampleQuestionDurationExampleAnswers == from.tutorialInfo.sampleQuestionDurationExampleAnswers
		to.tutorialInfo.sampleQuestionRating == from.tutorialInfo.sampleQuestionRating
		to.tutorialInfo.sampleQuestionRatingRange == from.tutorialInfo.sampleQuestionRatingRange
		to.tutorialInfo.sampleQuestionRatingExampleAnswer1 == from.tutorialInfo.sampleQuestionRatingExampleAnswer1
		to.tutorialInfo.sampleQuestionRatingExampleAnswer2 == from.tutorialInfo.sampleQuestionRatingExampleAnswer2
		to.tutorialInfo.sampleQuestionRatingExampleAnswer3 == from.tutorialInfo.sampleQuestionRatingExampleAnswer3
		to.tutorialInfo.today1 == from.tutorialInfo.today1
		to.tutorialInfo.today1Example == from.tutorialInfo.today1Example
		to.tutorialInfo.today2 == from.tutorialInfo.today2
		to.tutorialInfo.today2Example == from.tutorialInfo.today2Example
		to.tutorialInfo.today3 == from.tutorialInfo.today3
		to.tutorialInfo.today3Example == from.tutorialInfo.today3Example
		to.tutorialInfo.today4 == from.tutorialInfo.today4
		to.tutorialInfo.today4Example == from.tutorialInfo.today4Example		
		
		and: "to tutorial does not match the default tutorial"
		to.tutorialInfo.name != defaultTI.name
		to.tutorialInfo.customQuestion1 != defaultTI.customQuestion1
		to.tutorialInfo.sampleQuestionRatingRange != defaultTI.sampleQuestionRatingRange
		to.tutorialInfo.today4 != defaultTI.today4
		
		and: "to tutorial matches the custom from tutorial"
		to.tutorialInfo.customQuestion1 == fromTI.customQuestion1
		to.tutorialInfo.sampleQuestionRatingRange == fromTI.sampleQuestionRatingRange
		to.tutorialInfo.today4 == fromTI.today4	
	}
	
	void "test copy for custom tutorial from default tutorial"() {
		given: "promo codes"
		String toPromo = "to-promo-code"
		String fromPromo = "from-promo-code"
		
		and: "a to InitialLoginConfiguration"
		InitialLoginConfiguration to = InitialLoginConfiguration.createFromDefault(toPromo,true)
		
		and: "a from InitialLoginConfiguration"
		InitialLoginConfiguration from = InitialLoginConfiguration.createFromDefault(fromPromo,true)
		
		and: "the default tutorial"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		when: "a custom tutorial is created for the to configuration"
		TutorialInfo toTI = TutorialInfo.createFromDefault(toPromo, false)
		toTI.customQuestion1 = "to customQuestion1"
		toTI.sampleQuestionRatingRange = "to sampleQuestionRatingRange"
		toTI.today4 = "to today4"
		Utils.save(toTI,true)
		
		and: "from is copied to"
		InitialLoginConfiguration.copy(from, to)

		then: "to tutorial matches the from tutorial"
		to.tutorialInfo.customQuestion1 == from.tutorialInfo.customQuestion1
		to.tutorialInfo.customQuestion2 == from.tutorialInfo.customQuestion2
		to.tutorialInfo.trackExample1 == from.tutorialInfo.trackExample1
		to.tutorialInfo.trackExample2 == from.tutorialInfo.trackExample2
		to.tutorialInfo.trackExample3 == from.tutorialInfo.trackExample3
		to.tutorialInfo.deviceExample == from.tutorialInfo.deviceExample
		to.tutorialInfo.sampleQuestionDuration == from.tutorialInfo.sampleQuestionDuration
		to.tutorialInfo.sampleQuestionDurationExampleAnswers == from.tutorialInfo.sampleQuestionDurationExampleAnswers
		to.tutorialInfo.sampleQuestionRating == from.tutorialInfo.sampleQuestionRating
		to.tutorialInfo.sampleQuestionRatingRange == from.tutorialInfo.sampleQuestionRatingRange
		to.tutorialInfo.sampleQuestionRatingExampleAnswer1 == from.tutorialInfo.sampleQuestionRatingExampleAnswer1
		to.tutorialInfo.sampleQuestionRatingExampleAnswer2 == from.tutorialInfo.sampleQuestionRatingExampleAnswer2
		to.tutorialInfo.sampleQuestionRatingExampleAnswer3 == from.tutorialInfo.sampleQuestionRatingExampleAnswer3
		to.tutorialInfo.today1 == from.tutorialInfo.today1
		to.tutorialInfo.today1Example == from.tutorialInfo.today1Example
		to.tutorialInfo.today2 == from.tutorialInfo.today2
		to.tutorialInfo.today2Example == from.tutorialInfo.today2Example
		to.tutorialInfo.today3 == from.tutorialInfo.today3
		to.tutorialInfo.today3Example == from.tutorialInfo.today3Example
		to.tutorialInfo.today4 == from.tutorialInfo.today4
		to.tutorialInfo.today4Example == from.tutorialInfo.today4Example
		
		and: "to tutorial does not match the original custom to tutorial"
		to.tutorialInfo.name != toTI.name
		to.tutorialInfo.customQuestion1 != toTI.customQuestion1
		to.tutorialInfo.sampleQuestionRatingRange != toTI.sampleQuestionRatingRange
		to.tutorialInfo.today4 != toTI.today4
		
		and: "to tutorial does matches the default tutorial"
		to.tutorialInfo.name == defaultTI.name
		to.tutorialInfo.customQuestion1 == defaultTI.customQuestion1
		to.tutorialInfo.sampleQuestionRatingRange == defaultTI.sampleQuestionRatingRange
		to.tutorialInfo.today4 == defaultTI.today4
	}
	
	void "test copy for custom tutorial from custom tutorial"() {
		given: "promo codes"
		String toPromo = "to-promo-code"
		String fromPromo = "from-promo-code"
		
		and: "a to InitialLoginConfiguration"
		InitialLoginConfiguration to = InitialLoginConfiguration.createFromDefault(toPromo,true)
		
		and: "a from InitialLoginConfiguration"
		InitialLoginConfiguration from = InitialLoginConfiguration.createFromDefault(fromPromo,true)
		
		when: "a custom tutorial is created for the from configuration"
		TutorialInfo fromTI = TutorialInfo.createFromDefault(fromPromo, false)
		fromTI.customQuestion1 = "from customQuestion1"
		fromTI.sampleQuestionRatingRange = "from sampleQuestionRatingRange"
		fromTI.today4 = "from today4"
		Utils.save(fromTI,true)
		
		and: "a custom tutorial is created for the to configuration"
		TutorialInfo toTI = TutorialInfo.createFromDefault(toPromo, false)
		String toCustomQuestion1 = "to customQuestion1"
		String toSampleQuestionRatingRange = "to sampleQuestionRatingRange"
		String toToday4 = "to today4"
		toTI.customQuestion1 = toCustomQuestion1
		toTI.sampleQuestionRatingRange = toSampleQuestionRatingRange
		toTI.today4 = toToday4
		Utils.save(toTI,true)
		
		and: "from is copied to"
		InitialLoginConfiguration.copy(from, to)

		then: "to tutorial matches the from tutorial"
		to.tutorialInfo.customQuestion1 == from.tutorialInfo.customQuestion1
		to.tutorialInfo.customQuestion2 == from.tutorialInfo.customQuestion2
		to.tutorialInfo.trackExample1 == from.tutorialInfo.trackExample1
		to.tutorialInfo.trackExample2 == from.tutorialInfo.trackExample2
		to.tutorialInfo.trackExample3 == from.tutorialInfo.trackExample3
		to.tutorialInfo.deviceExample == from.tutorialInfo.deviceExample
		to.tutorialInfo.sampleQuestionDuration == from.tutorialInfo.sampleQuestionDuration
		to.tutorialInfo.sampleQuestionDurationExampleAnswers == from.tutorialInfo.sampleQuestionDurationExampleAnswers
		to.tutorialInfo.sampleQuestionRating == from.tutorialInfo.sampleQuestionRating
		to.tutorialInfo.sampleQuestionRatingRange == from.tutorialInfo.sampleQuestionRatingRange
		to.tutorialInfo.sampleQuestionRatingExampleAnswer1 == from.tutorialInfo.sampleQuestionRatingExampleAnswer1
		to.tutorialInfo.sampleQuestionRatingExampleAnswer2 == from.tutorialInfo.sampleQuestionRatingExampleAnswer2
		to.tutorialInfo.sampleQuestionRatingExampleAnswer3 == from.tutorialInfo.sampleQuestionRatingExampleAnswer3
		to.tutorialInfo.today1 == from.tutorialInfo.today1
		to.tutorialInfo.today1Example == from.tutorialInfo.today1Example
		to.tutorialInfo.today2 == from.tutorialInfo.today2
		to.tutorialInfo.today2Example == from.tutorialInfo.today2Example
		to.tutorialInfo.today3 == from.tutorialInfo.today3
		to.tutorialInfo.today3Example == from.tutorialInfo.today3Example
		to.tutorialInfo.today4 == from.tutorialInfo.today4
		to.tutorialInfo.today4Example == from.tutorialInfo.today4Example		
		
		and: "to tutorial does not match the original custom to tutorial"
		to.tutorialInfo.name != TutorialInfo.DEFAULT_TUTORIAL
		to.tutorialInfo.customQuestion1 != toCustomQuestion1
		to.tutorialInfo.sampleQuestionRatingRange != toSampleQuestionRatingRange
		to.tutorialInfo.today4 != toToday4
		
		and: "to tutorial matches the custom from tutorial"
		to.tutorialInfo.customQuestion1 == fromTI.customQuestion1
		to.tutorialInfo.sampleQuestionRatingRange == fromTI.sampleQuestionRatingRange
		to.tutorialInfo.today4 == fromTI.today4		
	}
	
	void "test getTutorialInfo returns default when no match of promo code"() {
		given: "an InitialLoginConfguration with non-default code"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.createFromDefault("test-code", true)
		
		and: "the default tutorial"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		when: "tutorialInfo is procured"
		TutorialInfo ti = ilc.tutorialInfo
		
		then: "tutorialInfo matches default"
		ilc.tutorialInfo.name == TutorialInfo.DEFAULT_TUTORIAL
		defaultTI.name == ilc.tutorialInfo.name
		defaultTI.customQuestion1 == ti.customQuestion1
		defaultTI.customQuestion2 == ti.customQuestion2
		defaultTI.trackExample1 == ti.trackExample1
		defaultTI.trackExample2 == ti.trackExample2
		defaultTI.trackExample3 == ti.trackExample3
		defaultTI.deviceExample == ti.deviceExample
		defaultTI.sampleQuestionDuration == ti.sampleQuestionDuration
		defaultTI.sampleQuestionDurationExampleAnswers == ti.sampleQuestionDurationExampleAnswers
		defaultTI.sampleQuestionRating == ti.sampleQuestionRating
		defaultTI.sampleQuestionRatingRange == ti.sampleQuestionRatingRange
		defaultTI.sampleQuestionRatingExampleAnswer1 == ti.sampleQuestionRatingExampleAnswer1
		defaultTI.sampleQuestionRatingExampleAnswer2 == ti.sampleQuestionRatingExampleAnswer2
		defaultTI.sampleQuestionRatingExampleAnswer3 == ti.sampleQuestionRatingExampleAnswer3
		defaultTI.today1 == ti.today1
		defaultTI.today1Example == ti.today1Example
		defaultTI.today2 == ti.today2
		defaultTI.today2Example == ti.today2Example
		defaultTI.today3 == ti.today3
		defaultTI.today3Example == ti.today3Example
		defaultTI.today4 == ti.today4
		defaultTI.today4Example == ti.today4Example	
	}

	void "test getTutorialInfo returns tutorial info that matches promo code"() {
		given: "a promo code"
		String promoCode = "test-code"
		
		and: "an InitialLoginConfguration with promo code"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.createFromDefault(promoCode, true)
		
		and: "new tutorial for promo code based on default"
		TutorialInfo promoTI = TutorialInfo.createFromDefault(promoCode, false)
		
		and: "a few fields are changed"
		promoTI.customQuestion1 = "new custom question 1"
		promoTI.sampleQuestionDuration = "new sample question duration"
		promoTI.today3Example = "new today 3 example"
		Utils.save(promoTI,true)
		
		when: "tutorialInfo is procured"
		TutorialInfo ti = ilc.tutorialInfo
		
		then: "tutorialInfo matches non default"
		ilc.tutorialInfo.name == promoCode
		promoTI.name == ilc.tutorialInfo.name
		promoTI.customQuestion1 == ti.customQuestion1
		promoTI.customQuestion2 == ti.customQuestion2
		promoTI.trackExample1 == ti.trackExample1
		promoTI.trackExample2 == ti.trackExample2
		promoTI.trackExample3 == ti.trackExample3
		promoTI.deviceExample == ti.deviceExample
		promoTI.sampleQuestionDuration == ti.sampleQuestionDuration
		promoTI.sampleQuestionDurationExampleAnswers == ti.sampleQuestionDurationExampleAnswers
		promoTI.sampleQuestionRating == ti.sampleQuestionRating
		promoTI.sampleQuestionRatingRange == ti.sampleQuestionRatingRange
		promoTI.sampleQuestionRatingExampleAnswer1 == ti.sampleQuestionRatingExampleAnswer1
		promoTI.sampleQuestionRatingExampleAnswer2 == ti.sampleQuestionRatingExampleAnswer2
		promoTI.sampleQuestionRatingExampleAnswer3 == ti.sampleQuestionRatingExampleAnswer3
		promoTI.today1 == ti.today1
		promoTI.today1Example == ti.today1Example
		promoTI.today2 == ti.today2
		promoTI.today2Example == ti.today2Example
		promoTI.today3 == ti.today3
		promoTI.today3Example == ti.today3Example
		promoTI.today4 == ti.today4
		promoTI.today4Example == ti.today4Example	
	}
	
	void "test getInterestSubjects returns empty set when no areas"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.defaultConfiguration()
		
		when: "interestAreas is cleared"
		ilc.interestAreas = []
		Utils.save(ilc,true)
		
		and: "getInterestSubjects is called"
		List subjects = ilc.interestSubjects
		
		then: "empty list of interest subjects are returned"
		subjects.size == 0
	}
	
	void "test getInterestSubjects returns one subject when one area with one subject"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.defaultConfiguration()
		
		when: "getInterestSubjects is called"
		List subjects = ilc.interestSubjects
		
		then: "default interest subject is only item in returned list"
		subjects
		subjects.size == 1
		subjects[0].subject.name == "sleep"
		subjects[0].preSelect
	}
	
	void "test getInterestSubjects returns two subjects when one area with two subjects"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.defaultConfiguration()
		
		and: "a subject"
		InterestSubject subject = InterestSubject.createInterestSubject(
			"subject2",
			["caffine"],
			null,
			"",
			true			
		)
		
		when: "subject is added to interest area"
		ilc.interestAreas[0].interest.addToInterestSubjects(new InterestSubjectBoolean(subject,true))
		Utils.save(ilc,true)
		
		and: "getInterestSubjects is called"
		List subjects = ilc.interestSubjects
		
		then: "default interest subject is only item in returned list"
		subjects
		subjects.size == 2
		subjects.find{ it.subject.name == "sleep" }
		subjects.find{ it.subject.name == subject.name }
	}
	
	void "test getInterestSubjects returns two subjects when two areas with one subject each"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.defaultConfiguration()
		
		and: "a new subject"
		InterestSubject subject = InterestSubject.createInterestSubject(
			"caffeine",
			["caffeine"],
			null,
			"",
			true			
		)
		
		and: "a new area"
		InterestArea area = new InterestArea()
		area.addToInterestSubjects(new InterestSubjectBoolean(subject,true))
		Utils.save(area,true)
		
		when: "the area is added"
		ilc.interestAreas << new InterestAreaBoolean(area,true)
		Utils.save(ilc,true)
		
		and: "getInterestSubjects is called"
		List subjects = ilc.interestSubjects
		
		then: "default interest subject is only item in returned list"
		subjects
		subjects.size == 2
		subjects.find{ it.subject.name == "sleep" }
		subjects.find{ it.subject.name == subject.name }
	}
	
	void "test getInterestSubjects returns four subjects when two areas with two subjects each"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.defaultConfiguration()
		
		and: "a subject"
		InterestSubject subjectA = InterestSubject.createInterestSubject(
			"caffeine",
			["caffeine"],
			null,
			"",
			true			
		)
		
		and: "another subject"
		InterestSubject subjectB = InterestSubject.createInterestSubject(
			"alcohol",
			["alcohol"],
			null,
			"",
			true			
		)
	
		and: "another subject"
		InterestSubject subjectC = InterestSubject.createInterestSubject(
			"running",
			["running"],
			null,
			"",
			true			
		)
	
		and: "a new area"
		InterestArea area = new InterestArea()
		area.addToInterestSubjects(new InterestSubjectBoolean(subjectB,true))
		area.addToInterestSubjects(new InterestSubjectBoolean(subjectC,true))
		Utils.save(area,true)
	
		when: "another subject is added to original"
		ilc.interestAreas[0].interest.addToInterestSubjects(new InterestSubjectBoolean(subjectA,true))
	
		and: "the second area is added"
		ilc.addToInterestAreas(new InterestAreaBoolean(area,true))
		Utils.save(ilc,true)
	
		and: "getInterestSubjects is called"
		List subjects = ilc.interestSubjects
		
		then: "default interest subject is only item in returned list"
		subjects
		subjects.size == 4
		subjects.find{ it.subject.name == "sleep" }
		subjects.find{ it.subject.name == subjectA.name }
		subjects.find{ it.subject.name == subjectB.name }
		subjects.find{ it.subject.name == subjectC.name }
	}
	
	@spock.lang.Unroll
	void "test getInterestSubjects returns one subject when two areas with same subject, subject1 preselect set to #preSelect1 and subject2 preselect set to #preSelect2"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.defaultConfiguration()
		
		and: "a new area"
		InterestArea area = new InterestArea()
		area.addToInterestSubjects(new InterestSubjectBoolean(			ilc.interestAreas[0].interest.interestSubjects[0].subject, preSelect2))
		Utils.save(area,true)
		
		when: "the new area is added"
		ilc.interestAreas << new InterestAreaBoolean(area, true)
		
		and: "original subject preSelect is set"
		ilc.interestAreas[0].interest.interestSubjects[0].preSelect = preSelect1
		Utils.save(ilc,true)
		
		and: "getInterestSubjects is called"
		List subjects = ilc.interestSubjects
		
		then: "default interest subject is only item in returned list"
		subjects
		subjects.size == 1
		subjects[0].subject.name == "sleep"
		subjects[0].preSelect == expected
		
		where:
		preSelect1		| preSelect2	| expected
		false			| false			| false
		true			| true			| true
		true			| false			| true
		false			| true			| true
	}
	
	void "test getInterestSubjects preSelect of Area overrides subject"() {
		given: "the default InitialLoginConfiguration"
		InitialLoginConfiguration ilc = InitialLoginConfiguration.defaultConfiguration()
		
		when: "original area preSelect is set to false"
		ilc.interestAreas[0].preSelect = false
		Utils.save(ilc,true)
		
		and: "getInterestSubjects is called"
		List subjects = ilc.interestSubjects
		
		then: "default interest subject is only item in returned list"
		subjects
		subjects.size == 1
		subjects[0].subject.name == "sleep"
		subjects[0].preSelect == false
		
		and: "direct preSelect is still true"
		ilc.interestAreas[0].interest.interestSubjects[0].preSelect
	}
}

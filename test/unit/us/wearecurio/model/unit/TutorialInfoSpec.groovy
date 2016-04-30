package us.wearecurio.model.unit

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import us.wearecurio.model.TutorialInfo

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock(TutorialInfo)
class TutorialInfoSpec extends Specification {
    void "test default"() {
		when: "given the default TutorialInfo"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		then: "defaultTI properties set to hard-coded values"
		defaultTI.name == TutorialInfo.DEFAULT_TUTORIAL
		defaultTI.customQuestion1 == "Does caffeine affect my sleep?"
		defaultTI.customQuestion2 == "Does exercise really affect my mood?"
		defaultTI.trackExample1 == "your <strong><i>mood</i></strong>"
		defaultTI.trackExample2 == "how much <strong><i>sleep</i></strong> you get"
		defaultTI.trackExample3 == "the <strong><i>coffee</i></strong> you drink"
		defaultTI.trackExample4 == "the <strong><i>steps</i></strong> you take"
		defaultTI.deviceExample == "the Oura ring (via your user profile)"
		defaultTI.sampleQuestionDuration == "How many hours did you sleep last night?"
		defaultTI.sampleQuestionDurationExampleAnswers == "e.g. 8 hours 10 minutes or 8hrs 10 mins"
		defaultTI.sampleQuestionRating == "How's your mood right now?"
		defaultTI.sampleQuestionRatingRange == "a number from 1 to 10"
		defaultTI.sampleQuestionRatingExampleAnswer1 == "1 would mean 'Not the best day'"
		defaultTI.sampleQuestionRatingExampleAnswer2 == "5 would mean 'Just so-so'"
		defaultTI.sampleQuestionRatingExampleAnswer3 == "10 would mean 'Super stoked'"
		defaultTI.today1 == "DRINK"
		defaultTI.today1Example == "e.g. coffee 1 cup 8am"
		defaultTI.today2 == "EXERCISE"
		defaultTI.today2Example == "e.g. walk 9500 steps"
		defaultTI.today3 == "WORK"
		defaultTI.today3Example == "e.g. work 7 hours 30 minutes"
		defaultTI.today4 == "SUPPLEMENTS"
		defaultTI.today4Example == "e.g. aspirin 400 mg, or vitamin c 200 mg"
    }

	void "test default returns saved modified values"() {
		given: "the default TutorialInfo object"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		and: "new values for defaultTI properties"
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
		
		when: "default object is modified"
		defaultTI.customQuestion1 = customQuestion1
		defaultTI.customQuestion2 = customQuestion2
		defaultTI.trackExample1 = trackExample1
		defaultTI.trackExample2 = trackExample2
		defaultTI.trackExample3 = trackExample3
		defaultTI.deviceExample = deviceExample
		defaultTI.sampleQuestionDuration = sampleQuestionDuration
		defaultTI.sampleQuestionDurationExampleAnswers = sampleQuestionDurationExampleAnswers
		defaultTI.sampleQuestionRating = sampleQuestionRating
		defaultTI.sampleQuestionRatingRange = sampleQuestionRatingRange
		defaultTI.sampleQuestionRatingExampleAnswer1 = sampleQuestionRatingExampleAnswer1
		defaultTI.sampleQuestionRatingExampleAnswer2 = sampleQuestionRatingExampleAnswer2
		defaultTI.sampleQuestionRatingExampleAnswer3 = sampleQuestionRatingExampleAnswer3
		defaultTI.today1 = today1
		defaultTI.today1Example = today1Example
		defaultTI.today2 = today2
		defaultTI.today2Example = today2Example
		defaultTI.today3 = today3
		defaultTI.today3Example = today3Example
		defaultTI.today4 = today4
		defaultTI.today4Example = today4Example
		
		and: "defaultTI object is saved"
		defaultTI.save(true)
		
		and: "default is requested"
		TutorialInfo newDefault = TutorialInfo.defaultTutorialInfo()
		
		then: "returned TutorialInfo has default name"
		newDefault.name == TutorialInfo.DEFAULT_TUTORIAL
		
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
	}
	
	void "test createFromDefault without name"() {
		given: "the default TutorialInfo"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		when: "a new ilc created from default TutorialInfo"
		TutorialInfo newTI = TutorialInfo.createFromDefault()
		
		then: "new ilc name is blank"
		(newTI.name == null) || (newTI.name == "")
		
		and: "rest of fields of new ilc are identical to default ilc"
		newTI.customQuestion1 == defaultTI.customQuestion1
		newTI.customQuestion2 == defaultTI.customQuestion2
		newTI.trackExample1 == defaultTI.trackExample1
		newTI.trackExample2 == defaultTI.trackExample2
		newTI.trackExample3 == defaultTI.trackExample3
		newTI.deviceExample == defaultTI.deviceExample
		newTI.sampleQuestionDuration == defaultTI.sampleQuestionDuration
		newTI.sampleQuestionDurationExampleAnswers == defaultTI.sampleQuestionDurationExampleAnswers
		newTI.sampleQuestionRating == defaultTI.sampleQuestionRating
		newTI.sampleQuestionRatingRange == defaultTI.sampleQuestionRatingRange
		newTI.sampleQuestionRatingExampleAnswer1 == defaultTI.sampleQuestionRatingExampleAnswer1
		newTI.sampleQuestionRatingExampleAnswer2 == defaultTI.sampleQuestionRatingExampleAnswer2
		newTI.sampleQuestionRatingExampleAnswer3 == defaultTI.sampleQuestionRatingExampleAnswer3
		newTI.today1 == defaultTI.today1
		newTI.today1Example == defaultTI.today1Example
		newTI.today2 == defaultTI.today2
		newTI.today2Example == defaultTI.today2Example
		newTI.today3 == defaultTI.today3
		newTI.today3Example == defaultTI.today3Example
		newTI.today4 == defaultTI.today4
		newTI.today4Example == defaultTI.today4Example
    }
	
    void "test createFromDefault with name with save == false"() {
		given: "a name"
		String name = "testNonSaveName"
		
		and: "a default TutorialInfo"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		when: "a new ilc created from default TutorialInfo"
		TutorialInfo newTI = TutorialInfo.createFromDefault(name, false)
		
		then: "new ilc name is set to name passed in"
		newTI.name == name
		
		and: "rest of fields of new ilc are identical to default ilc"
		newTI.customQuestion1 == defaultTI.customQuestion1
		newTI.customQuestion2 == defaultTI.customQuestion2
		newTI.trackExample1 == defaultTI.trackExample1
		newTI.trackExample2 == defaultTI.trackExample2
		newTI.trackExample3 == defaultTI.trackExample3
		newTI.deviceExample == defaultTI.deviceExample
		newTI.sampleQuestionDuration == defaultTI.sampleQuestionDuration
		newTI.sampleQuestionDurationExampleAnswers == defaultTI.sampleQuestionDurationExampleAnswers
		newTI.sampleQuestionRating == defaultTI.sampleQuestionRating
		newTI.sampleQuestionRatingRange == defaultTI.sampleQuestionRatingRange
		newTI.sampleQuestionRatingExampleAnswer1 == defaultTI.sampleQuestionRatingExampleAnswer1
		newTI.sampleQuestionRatingExampleAnswer2 == defaultTI.sampleQuestionRatingExampleAnswer2
		newTI.sampleQuestionRatingExampleAnswer3 == defaultTI.sampleQuestionRatingExampleAnswer3
		newTI.today1 == defaultTI.today1
		newTI.today1Example == defaultTI.today1Example
		newTI.today2 == defaultTI.today2
		newTI.today2Example == defaultTI.today2Example
		newTI.today3 == defaultTI.today3
		newTI.today3Example == defaultTI.today3Example
		newTI.today4 == defaultTI.today4
		newTI.today4Example == defaultTI.today4Example
		
		when: "TutorialInfo searched by name"
		TutorialInfo result = TutorialInfo.findByName(name)
		
		then: "null is returned since new TI is unsaved"
		result == null
	}
	
    void "test createFromDefault with name with save == true"() {
		given: "a name"
		String name = "testSaveName"
		
		and: "a default TutorialInfo"
		TutorialInfo defaultTI = TutorialInfo.defaultTutorialInfo()
		
		when: "a new ilc created from default TutorialInfo"
		TutorialInfo newTI = TutorialInfo.createFromDefault(name, true)
		
		then: "new ilc name is set to name passed in"
		newTI.name == name
		
		and: "rest of fields of new ilc are identical to default ilc"
		newTI.customQuestion1 == defaultTI.customQuestion1
		newTI.customQuestion2 == defaultTI.customQuestion2
		newTI.trackExample1 == defaultTI.trackExample1
		newTI.trackExample2 == defaultTI.trackExample2
		newTI.trackExample3 == defaultTI.trackExample3
		newTI.deviceExample == defaultTI.deviceExample
		newTI.sampleQuestionDuration == defaultTI.sampleQuestionDuration
		newTI.sampleQuestionDurationExampleAnswers == defaultTI.sampleQuestionDurationExampleAnswers
		newTI.sampleQuestionRating == defaultTI.sampleQuestionRating
		newTI.sampleQuestionRatingRange == defaultTI.sampleQuestionRatingRange
		newTI.sampleQuestionRatingExampleAnswer1 == defaultTI.sampleQuestionRatingExampleAnswer1
		newTI.sampleQuestionRatingExampleAnswer2 == defaultTI.sampleQuestionRatingExampleAnswer2
		newTI.sampleQuestionRatingExampleAnswer3 == defaultTI.sampleQuestionRatingExampleAnswer3
		newTI.today1 == defaultTI.today1
		newTI.today1Example == defaultTI.today1Example
		newTI.today2 == defaultTI.today2
		newTI.today2Example == defaultTI.today2Example
		newTI.today3 == defaultTI.today3
		newTI.today3Example == defaultTI.today3Example
		newTI.today4 == defaultTI.today4
		newTI.today4Example == defaultTI.today4Example
		
		when: "TutorialInfo searched by name"
		TutorialInfo result = TutorialInfo.findByName(name)
		
		then: "new TI is saved, so result matches new ilc name"
		result.name == newTI.name
		
		and: "result matches rest of new ilc fields"
		result.customQuestion1 == newTI.customQuestion1
		result.customQuestion2 == newTI.customQuestion2
		result.trackExample1 == newTI.trackExample1
		result.trackExample2 == newTI.trackExample2
		result.trackExample3 == newTI.trackExample3
		result.deviceExample == newTI.deviceExample
		result.sampleQuestionDuration == newTI.sampleQuestionDuration
		result.sampleQuestionDurationExampleAnswers == newTI.sampleQuestionDurationExampleAnswers
		result.sampleQuestionRating == newTI.sampleQuestionRating
		result.sampleQuestionRatingRange == newTI.sampleQuestionRatingRange
		result.sampleQuestionRatingExampleAnswer1 == newTI.sampleQuestionRatingExampleAnswer1
		result.sampleQuestionRatingExampleAnswer2 == newTI.sampleQuestionRatingExampleAnswer2
		result.sampleQuestionRatingExampleAnswer3 == newTI.sampleQuestionRatingExampleAnswer3
		result.today1 == newTI.today1
		result.today1Example == newTI.today1Example
		result.today2 == newTI.today2
		result.today2Example == newTI.today2Example
		result.today3 == newTI.today3
		result.today3Example == newTI.today3Example
		result.today4 == newTI.today4
		result.today4Example == newTI.today4Example
	}
	
	void "test copy"() {
		given: "an TutorialInfo"
		TutorialInfo to = new TutorialInfo()
		to.name = "to-name"
		
		and: "another TutorialInfo"
		TutorialInfo from = new TutorialInfo()
		from.name = "from-name"
		
		when: "first TutorialInfo has properties assigned"
		to.customQuestion1 = "to"
		to.customQuestion2 = "to"
		to.trackExample1 = "to"
		to.trackExample2 = "to"
		to.trackExample3 = "to"
		to.deviceExample = "to"
		to.sampleQuestionDuration = "to"
		to.sampleQuestionDurationExampleAnswers = "to"
		to.sampleQuestionRating = "to"
		to.sampleQuestionRatingRange = "to"
		to.sampleQuestionRatingExampleAnswer1 = "to"
		to.sampleQuestionRatingExampleAnswer2 = "to"
		to.sampleQuestionRatingExampleAnswer3 = "to"
		to.today1 = "to"
		to.today1Example = "to"
		to.today2 = "to"
		to.today2Example = "to"
		to.today3 = "to"
		to.today3Example = "to"
		to.today4 = "to"
		to.today4Example = "to"
		
		and: "second TutorialInfo has different properties assigned"
		from.customQuestion1 = "from"
		from.customQuestion2 = "from"
		from.trackExample1 = "from"
		from.trackExample2 = "from"
		from.trackExample3 = "from"
		from.deviceExample = "from"
		from.sampleQuestionDuration = "from"
		from.sampleQuestionDurationExampleAnswers = "from"
		from.sampleQuestionRating = "from"
		from.sampleQuestionRatingRange = "from"
		from.sampleQuestionRatingExampleAnswer1 = "from"
		from.sampleQuestionRatingExampleAnswer2 = "from"
		from.sampleQuestionRatingExampleAnswer3 = "from"
		from.today1 = "from"
		from.today1Example = "from"
		from.today2 = "from"
		from.today2Example = "from"
		from.today3 = "from"
		from.today3Example = "from"
		from.today4 = "from"
		from.today4Example = "from"
		
		and: "second is copied to first"
		TutorialInfo.copy(from, to)
		
		then: "name is not copied"
		to.name != from.name
		
		and: "all other properties are copied"
		to.customQuestion1 == from.customQuestion1
		to.customQuestion2 == from.customQuestion2
		to.trackExample1 == from.trackExample1
		to.trackExample2 == from.trackExample2
		to.trackExample3 == from.trackExample3
		to.deviceExample == from.deviceExample
		to.sampleQuestionDuration == from.sampleQuestionDuration
		to.sampleQuestionDurationExampleAnswers == from.sampleQuestionDurationExampleAnswers
		to.sampleQuestionRating == from.sampleQuestionRating
		to.sampleQuestionRatingRange == from.sampleQuestionRatingRange
		to.sampleQuestionRatingExampleAnswer1 == from.sampleQuestionRatingExampleAnswer1
		to.sampleQuestionRatingExampleAnswer2 == from.sampleQuestionRatingExampleAnswer2
		to.sampleQuestionRatingExampleAnswer3 == from.sampleQuestionRatingExampleAnswer3
		to.today1 == from.today1
		to.today1Example == from.today1Example
		to.today2 == from.today2
		to.today2Example == from.today2Example
		to.today3 == from.today3
		to.today3Example == from.today3Example
		to.today4 == from.today4
		to.today4Example == from.today4Example
	}
}

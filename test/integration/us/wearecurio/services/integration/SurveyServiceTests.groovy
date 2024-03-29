package us.wearecurio.services.integration

import us.wearecurio.model.registration.UserRegistration
import us.wearecurio.model.survey.Survey
import us.wearecurio.model.survey.SurveyStatus
import us.wearecurio.services.SurveyService

/**
 * This class contains integration test cases for SurveyService class.
 */
class SurveyServiceTests extends CuriousServiceTestCase {

	TimeZone serverTimezone
	SurveyService surveyService

	void setup() {
		serverTimezone = TimeZone.getDefault()
		setupTestData()
	}

	void cleanup() {
		TimeZone.setDefault(serverTimezone)
	}

	void 'test checkPromoCode method when default survey doesnt exist'() {
		given: 'An instance of Survey and UserRegistration'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.INACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: 1, status: 'INACTIVE',
				answerType: 'DESCRIPTIVE')
		surveyInstance.save(flush: true)

		UserRegistration userRegistration = UserRegistration.create(user.id, surveyInstance.code)
		UserRegistration userRegistration1 = UserRegistration.create(user2.id)

		assert UserRegistration.count() == 2
		assert UserRegistration.findByUserId(user.id).id == userRegistration.id
		assert UserRegistration.findByUserId(user2.id).id == userRegistration1.id

		when: 'checkPromoCode method is called and user did not use any promoCode during signup'
		Survey result = surveyService.checkPromoCode(user2)

		then: 'No surveyInstance is returned in response'
		result == null

		when: 'checkPromoCode method is hit and user did use the promoCode but survey is inactive'
		result = surveyService.checkPromoCode(user)

		then: 'No surveyInstance is returned in response'
		result == null

		when: 'checkPromoCode method is called and Survey with that promoCode does exist'
		surveyInstance.status = SurveyStatus.ACTIVE
		surveyInstance.save(flush: true)
		result = surveyService.checkPromoCode(user)

		then: 'Method returns Survey instance in response'
		result == surveyInstance
	}

	void 'test checkPromoCode method when default survey exist'() {
		given:
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.INACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: 1, status: 'INACTIVE',
				answerType: 'DESCRIPTIVE')
		surveyInstance.save(flush: true)

		Survey defaultSurveyInstance = new Survey(code: 'default', title: 'This is the demo survey title.',
				status: SurveyStatus.ACTIVE)
		defaultSurveyInstance.addToQuestions(question: 'This is the first demo question.', priority: 1,
				status: 'ACTIVE', answerType: 'DESCRIPTIVE')
		defaultSurveyInstance.save(flush: true)

		UserRegistration userRegistration = UserRegistration.create(user.id, surveyInstance.code)
		UserRegistration userRegistration1 = UserRegistration.create(user2.id)

		assert UserRegistration.count() == 2
		assert UserRegistration.findByUserId(user.id).id == userRegistration.id
		assert UserRegistration.findByUserId(user2.id).id == userRegistration1.id

		when: 'checkPromoCode method is called and user did not use any promoCode during signup'
		Survey result = surveyService.checkPromoCode(user2)

		then: 'Default surveyInstance is returned in response'
		result.code == 'default'

		when: 'User did entered a promoCode but survey for that promoCode is Inactive'
		result = surveyService.checkPromoCode(user)

		then: 'Default surveyInstance is returned in response'
		result.code == 'default'

		when: 'User did enter a promoCode but survey for that promoCode doesnt exist'
		userRegistration1 = UserRegistration.create(user2.id,"test")
		result = surveyService.checkPromoCode(user2)

		then: 'Default surveyInstance is returned in response'
		result.code == 'default'
	}
}
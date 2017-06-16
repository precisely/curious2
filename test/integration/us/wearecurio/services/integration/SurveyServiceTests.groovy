package us.wearecurio.services.integration

import us.wearecurio.model.User
import us.wearecurio.model.registration.UserRegistration
import us.wearecurio.model.survey.QuestionStatus
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

	void 'test checkPromoCode method'() {
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
		Map result = surveyService.checkPromoCode(user2)

		then: 'Method returns appropriate message in response'
		result.message == "User ${user2} did not use any promo code during signup."

		when: 'checkPromoCode method is hit and user did use the promoCode but survey is inactive'
		result = surveyService.checkPromoCode(user)

		then: 'Method returns appropriate message in response'
		result.message == "Active survey does not exist for promo code s001"

		when: 'checkPromoCode method is called and Survey with that promoCode does not have active questions'
		surveyInstance.status = SurveyStatus.ACTIVE
		surveyInstance.save(flush: true)
		result = surveyService.checkPromoCode(user)

		then: 'Method returns appropriate message in response'
		result.message == "There are no active questions for this survey."

		when: 'checkPromoCode method is called and an active Survey with that promoCode does have active questions'
		surveyInstance.questions[0].status = QuestionStatus.ACTIVE
		surveyInstance.save(flush: true)
		result = surveyService.checkPromoCode(user)

		then: 'Method returns the Survey and Active questions'
		result.surveyInstance == surveyInstance
		result.activeQuestions == surveyInstance.questions
	}
}

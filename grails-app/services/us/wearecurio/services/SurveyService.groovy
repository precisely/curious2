package us.wearecurio.services

import grails.transaction.Transactional
import us.wearecurio.model.User
import us.wearecurio.model.registration.UserRegistration
import us.wearecurio.model.survey.QuestionStatus
import us.wearecurio.model.survey.Status
import us.wearecurio.model.survey.Survey
import us.wearecurio.model.survey.SurveyStatus
import us.wearecurio.model.survey.UserSurvey

@Transactional
class SurveyService {

	SecurityService securityService

	@grails.events.Listener
	Map checkPromoCode(User user) {

		log.debug "SurveyService.checkPromoCode()"

		User currentUser = user ?: securityService.currentUser()
		Map result = [:]

		UserRegistration userRegistration = UserRegistration.findByUserId(currentUser.id)
		String promoCode = userRegistration.promoCode

		if (!promoCode) {
			log.debug "User ${currentUser} did not use any promo code during signup."

			return result
		}

		if (promoCode && userRegistration.dateCreated > new Date() - 90) {
			log.debug "Checking if Survey exists for promo code ${promoCode}..."
			Survey survey = Survey.findByCode(promoCode)

			if (survey && survey.status == SurveyStatus.ACTIVE) {
				log.debug "Checking if User ${currentUser} has taken Survey ${survey}..."
				UserSurvey userSurvey = UserSurvey.findByUserAndSurvey(currentUser, survey)

				if (!userSurvey || userSurvey.status == Status.NOT_TAKEN) {
					Set activeQuestions = survey.questions.findAll { it.status == QuestionStatus.ACTIVE }

					if (!activeQuestions.size()) {
						log.debug "There are no active questions for this survey."

						return
					}

					result = [surveyInstance: survey, activeQuestions: activeQuestions]
				}
			} else {
				log.debug "Active survey does not exist for promo code ${promoCode}"
			}
		}

		return result
	}
}

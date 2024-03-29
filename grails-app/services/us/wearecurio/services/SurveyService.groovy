package us.wearecurio.services

import grails.transaction.Transactional
import us.wearecurio.model.User
import us.wearecurio.model.registration.UserRegistration
import us.wearecurio.model.survey.Status
import us.wearecurio.model.survey.Survey
import us.wearecurio.model.survey.SurveyStatus
import us.wearecurio.model.survey.UserSurvey

@Transactional
class SurveyService {

	SecurityService securityService

	Survey checkPromoCode(User currentUser) {

		log.debug "SurveyService.checkPromoCode()"

		if (!currentUser) {
			log.debug 'Please provide a valid user instance to check promo code.'

			return
		}

		UserRegistration userRegistration = UserRegistration.findByUserId(currentUser.id)
		String promoCode = userRegistration?.promoCode ?: 'default'
		Survey survey
		UserSurvey userSurvey

		if (userRegistration && userRegistration.dateCreated > new Date() - 90) {
			log.debug "Checking if Survey exists for promo code ${promoCode}..."

			survey = Survey.findByCodeAndStatus(promoCode, SurveyStatus.ACTIVE) ?: Survey.findByCodeAndStatus(
					'default', SurveyStatus.ACTIVE)

			if (survey) {
				log.debug "Checking if User ${currentUser} has taken Survey ${survey}..."
				userSurvey = UserSurvey.findByUserAndSurvey(currentUser, survey)

				if (!userSurvey || userSurvey.status == Status.NOT_TAKEN) {
					return survey
				}
			}
		}
	}
}
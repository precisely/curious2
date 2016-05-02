package us.wearecurio.model

import us.wearecurio.utility.Utils
import org.apache.commons.logging.LogFactory

class UserSurveyAnswer {

	Long userId
	String questionCode
	String answerText

	private static def log = LogFactory.getLog(this)
	static constraints = {
		questionCode nullable: false, blank: false
		answerText blank: false
		userId nullable: false
	}

	static mapping = {
		version false
		userId column : 'user_id', index:'user_id_index'
		questionCode column : 'question_code', index:'question_code_index'
	}

	static UserSurveyAnswer create(User user, String questionCode, String answerText) {
		SurveyQuestion surveyQuestion = SurveyQuestion.findByCode(questionCode)
		if (!surveyQuestion) {
			return null
		}

		UserSurveyAnswer userSurveyAnswer = new UserSurveyAnswer()
		userSurveyAnswer.userId = user.id
		userSurveyAnswer.questionCode = questionCode
		userSurveyAnswer.answerText = answerText
		userSurveyAnswer.validate()
		
		if (userSurveyAnswer.hasErrors()) {
			return null
		} else {
			try {
				Utils.save(userSurveyAnswer, false)
				return userSurveyAnswer
			} catch (Exception e) {
				Utils.reportError("Error while creating user survey answer", e)
				return null
			}
		}
	}
}

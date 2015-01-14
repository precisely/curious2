package us.wearecurio.model

import us.wearecurio.utility.Utils

class UserSurveyAnswer {

	Long userId
	String questionCode
	String answerText

	static constraints = {
		questionCode(nullable: false)
	}

	static mapping = {
		userId column : 'user_id', index:'user_id_index'
		questionCode column : 'question_code', index:'question_code_index'
	}

	public static UserSurveyAnswer create(User user, String questionCode, String answerText) {
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
				return null
			}
		}
	}
}

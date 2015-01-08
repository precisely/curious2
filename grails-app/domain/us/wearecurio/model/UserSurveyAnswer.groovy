package us.wearecurio.model

import us.wearecurio.utility.Utils

class UserSurveyAnswer {

	Long userId
	String questionCode
	String answerText

	static constraints = {
		questionCode(unique: true, nullable: false)
	}

	public static def create(User user, String questionCode, String answerText) {
		UserSurveyAnswer userSurveyAnswer = new UserSurveyAnswer()
		userSurveyAnswer.userId = user.id
		userSurveyAnswer.questionCode = questionCode
		userSurveyAnswer.answerText = answerText
		userSurveyAnswer.validate()

		if (userSurveyAnswer.hasErrors()) {
			return false
		} else {
			Utils.save(userSurveyAnswer, true)
			return userSurveyAnswer
		}
	}
}

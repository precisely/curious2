package us.wearecurio.model.survey

import us.wearecurio.model.User
import us.wearecurio.model.profiletags.ProfileTag
import us.wearecurio.utility.Utils

/**
 * A domain that holds answers given by a User for a Question.
 * It contains a field answerText which represents the text of the answer. In case of descriptive questions, the 
 * answerText is directly entered by User, else the answerText is the same as the answer field in PossibleAnswer domain.
 */
class UserAnswer {

	Long userId
	String answerText

	Question question

	static constraints = {
		answerText blank: false
	}

	static mapping = {
		version false
		userId column: 'user_id', index: 'user_id_index'
	}

	static void createAnswers(User user, Question surveyQuestion, List answerList) {
		answerList.each {
			createAnswer(user, surveyQuestion, it)
		}
	}

	static UserAnswer createAnswer(User user, Question surveyQuestion, PossibleAnswer answerInstance) {
		UserAnswer userAnswerInstance = createAnswer(user, surveyQuestion, answerInstance.answer)
		if (userAnswerInstance) {
			ProfileTag.addPublicInterestTag(answerInstance.tag, user.id)
		}

		return userAnswerInstance
	}

	static UserAnswer createAnswer(User user, Question surveyQuestion, String answerText) {
		UserAnswer userSurveyAnswer = new UserAnswer()
		userSurveyAnswer.userId = user.id
		userSurveyAnswer.question = surveyQuestion
		userSurveyAnswer.answerText = answerText

		if (!Utils.save(userSurveyAnswer, false) && surveyQuestion.isRequired) {
			throw new IllegalArgumentException()
		}

		return userSurveyAnswer
	}

	String toString() {
		return "UserAnswer(id: ${id}, questionId: ${question.id}, userId: ${userId}), answerText: ${answerText}"
	}
}

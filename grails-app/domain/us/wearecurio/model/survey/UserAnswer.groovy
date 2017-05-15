package us.wearecurio.model.survey

import groovy.transform.ToString
import spock.util.mop.Use
import us.wearecurio.model.User
import us.wearecurio.profiletags.ProfileTag
import us.wearecurio.utility.Utils
import org.apache.commons.logging.LogFactory

@ToString(includes = ['id', 'userId'], includePackage = false)
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
}

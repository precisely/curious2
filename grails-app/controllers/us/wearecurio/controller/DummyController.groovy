package us.wearecurio.controller

import us.wearecurio.model.SurveyQuestion
import us.wearecurio.model.UserSurveyAnswer
import us.wearecurio.model.QuestionStatus
import us.wearecurio.model.User

class DummyController extends DataController{

	def index() { }

	def saveSurveyData() {
		log.debug "Dummy.saveSurveyData()"
		User currentUserInstance = sessionUser()
		boolean hasErrors = false

		// Using any instead of each so as to be able to break the loop when error occurs
		params.answer.any({ questionAnswerMap ->
			log.debug "$questionAnswerMap.key";
			log.debug "$questionAnswerMap.value";
			def userSurveyAnswer = UserSurveyAnswer.create(currentUserInstance, questionAnswerMap.key, questionAnswerMap.value)
			if( !userSurveyAnswer) {
				hasErrors = true
				return true
			} else {
				return
			}
		})
		
		if (hasErrors) {
			renderJSONPost([success: false])
			return
		} else {
			renderJSONPost([success: true])
			return
		}
	}

	def getSurveyData() {
		log.debug "Dummy.gettingSurveyData()"
		List questions = SurveyQuestion.findAllWhere(status: QuestionStatus.ACTIVE)
		log.debug "questions: $questions"
		Map model = [questions: questions]
		render template: "/survey/questions", model: model
		return
	}

	def createNewQuestions() {
		
	}

	def createAnswers() {
		
	}

	def deleteQuestions() {
		
	}

	def showQuestion() {
		
	}

	def listAllQuestions() {
		
	}
}

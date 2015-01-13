package us.wearecurio.controller

import org.grails.databinding.SimpleMapDataBindingSource
import us.wearecurio.model.SurveyAnswer
import us.wearecurio.model.SurveyQuestion
import us.wearecurio.model.QuestionStatus
import us.wearecurio.utility.Utils

class AdminController extends LoginController {

	def grailsWebDataBinder
	def dashboard() {
		
	}

	def survey(Long id) {
		Map model = null
		if (id) {
			model = [surveyQuestion: SurveyQuestion.get(id)]
		}
		model
	}

	def createOrUpdateQuestion() {
		log.debug "create or update question: $params"
		if (params.id) {
			def surveyQuestion = SurveyQuestion.get(params.id)
			
			grailsWebDataBinder.bind(surveyQuestion, 
				params as SimpleMapDataBindingSource, ["code", "status", "priority", "question", "validator"])
			surveyQuestion.validate()
	
			if (!surveyQuestion.hasErrors()) {
				Utils.save(surveyQuestion, true)
				redirect(uri: "admin/listSurveyQuestions")
			} else {
				flash.message = "Could not update question, please verify all fields!"
			}
		} else {
			def surveyQuestion = SurveyQuestion.create(params)
			
			if (surveyQuestion) {
				Map model = [surveyQuestion: surveyQuestion];
				redirect(uri: "admin/addPossibleAnswers?id=$surveyQuestion.id")
			} else {
				flash.message = "Could not add question, perhaps code is not unique!"
			}
		}
	}

	def addPossibleAnswers() {
		Map model = [surveyQuestion: SurveyQuestion.get(params.id)];
		render(view: "/admin/createPossibleAnswers", model: model)
	}

	def createAnswers() {
		SurveyQuestion surveyQuestion = SurveyQuestion.get(params.questionId)
		log.debug "parameters: $params";
		grailsWebDataBinder.bind(surveyQuestion, params as SimpleMapDataBindingSource, ["possibleAnswers"])
		surveyQuestion.possibleAnswers.removeAll([null])
		surveyQuestion.possibleAnswers.sort()
		surveyQuestion.validate()

		if (surveyQuestion.hasErrors()) {
			renderJSONPost([success: false])
			return
		} else {
			Utils.save(surveyQuestion, true)
			renderJSONPost([success: true])
			return
		}
	}

	def listSurveyQuestions() {
		List questions = SurveyQuestion.findAllWhere(status: QuestionStatus.ACTIVE)
		Map model = [questions: questions]
		model
	}

	def showSurveyQuestion(Long id) {
		SurveyQuestion surveyQuestion = SurveyQuestion.get(id)
		[surveyQuestion: surveyQuestion]
	}

	def deletePossibleAnswer(Long answerId, Long questionId) {
		SurveyQuestion surveyQuestion = SurveyQuestion.get(questionId)
		
		SurveyAnswer surveyAnswer = surveyQuestion.possibleAnswers.find{ answerInstance-> answerInstance.id == answerId}
		surveyQuestion.removeFromPossibleAnswers(surveyAnswer)
		Utils.save(surveyQuestion, true)
		renderJSONPost([success: true])
		return
	}

	def UpdateSurveyAnswer() {
		SurveyAnswer surveyAnswer = SurveyAnswer.get(params.answerId)
		grailsWebDataBinder.bind(surveyAnswer, 
			params as SimpleMapDataBindingSource, ["code", "answer", "priority", "answerType"])
		surveyAnswer.validate()

		if (surveyAnswer.hasErrors()) {
			renderJSONPost([success: false])
			return
		} else {
			Utils.save(surveyAnswer, true)
			renderJSONPost([success: true])
			return
		}
	}

	def deleteSurveyQuestion(Long questionId) {
		SurveyQuestion surveyQuestion = SurveyQuestion.get(questionId)
		surveyQuestion.delete(flush: true)
		renderJSONPost([success: true])
		return
	}
}
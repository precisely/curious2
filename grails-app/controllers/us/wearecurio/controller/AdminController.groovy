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
	def surveyFactory() {
	
	}

	def createNewQuestion() {
		log.debug "create question: $params"
		def surveyQuestion = SurveyQuestion.create(params)
		
		if (surveyQuestion) {
			Map model = [surveyQuestion: surveyQuestion];
			render(view: "/admin/createPossibleAnswers", model: model)
		} else {
			render "error occured!"
		}
	}

	def createAnswers() {
		SurveyQuestion surveyQuestion = SurveyQuestion.get(params.questionId)
		log.debug "parameters: $params";
		println ">>>>" + params['possibleAnswers[0]']
		println ">>>>" + params['possibleAnswers[1]']
		println ">>>>" + params['possibleAnswers[2]']
		println ">>>>" + params['possibleAnswers[3]']
		grailsWebDataBinder.bind(surveyQuestion, params as SimpleMapDataBindingSource, ["possibleAnswers"])
		surveyQuestion.validate()

		if (surveyQuestion.hasErrors()) {
			println ">>>>>>>>>>error: $surveyQuestion.errors"
			renderJSONPost([success: false])
			return
		} else {
			Utils.save(surveyQuestion, true)
			renderJSONPost([success: true])
			return
		}
	}

	def listSurveyQuestions() {
		log.debug "listing survey questions"
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
		surveyAnswer.delete(flush: true)
		renderJSONPost([success: true])
		return
	}

	def UpdateSurveyAnswer() {
		SurveyAnswer surveyAnswer = SurveyAnswer.get(params.answerId)
		grailsWebDataBinder.bind(surveyAnswer, params as SimpleMapDataBindingSource, ["code", "answer", "priority", "answerType"])
		surveyAnswer.validate()

		if (surveyAnswer.hasErrors()) {
			println ">>>>>>>>>>error: $surveyAnswer.errors"
			renderJSONPost([success: false])
			return
		} else {
			Utils.save(surveyAnswer, true)
			renderJSONPost([success: true])
			return
		}
	}
}
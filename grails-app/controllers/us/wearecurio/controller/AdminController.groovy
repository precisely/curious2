package us.wearecurio.controller

import org.grails.databinding.SimpleMapDataBindingSource
import us.wearecurio.model.SurveyAnswer
import us.wearecurio.model.SurveyQuestion
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
		println "$params"
		if (params.id) {
			SurveyQuestion surveyQuestion = SurveyQuestion.get(params.id)
			
			grailsWebDataBinder.bind(surveyQuestion, 
				params as SimpleMapDataBindingSource, ["code", "status", "priority", "question"])
			surveyQuestion.validate()
	
			if (!surveyQuestion.hasErrors()) {
				Utils.save(surveyQuestion, true)
				redirect(uri: "admin/listSurveyQuestions")
			} else {
				redirect(uri: "admin/survey?id=" + params.id)
				flash.message = "Could not update question, please verify all fields!"
			}
		} else {
			SurveyQuestion surveyQuestion = SurveyQuestion.create(params)
			
			if (surveyQuestion) {
				Map model = [surveyQuestion: surveyQuestion];
				redirect(uri: "admin/addPossibleAnswers?id=$surveyQuestion.id")
			} else {
				redirect(uri: "admin/survey")
				flash.message = "Could not add question, perhaps code is not unique!"
			}
		}
	}

	def addPossibleAnswers() {
		SurveyQuestion surveyQuestion = params.id ? SurveyQuestion.get(params.id) : null
		if (!surveyQuestion) {
			redirect(uri: "admin/survey")
			flash.message = "You can not add answers to a question that does not exist!"
			return
		} else {
			Map model = [surveyQuestion: surveyQuestion];
			render(view: "/admin/createPossibleAnswers", model: model)
		}
	}

	def createAnswers() {
		SurveyAnswer.withTransaction { status ->
			SurveyQuestion surveyQuestion = params.questionId ? SurveyQuestion.get(params.questionId) : null
			if (!surveyQuestion) {
				renderJSONPost([success: false])
				return
			}
			
			grailsWebDataBinder.bind(surveyQuestion, params as SimpleMapDataBindingSource, ["possibleAnswers"])
			surveyQuestion.possibleAnswers.removeAll([null])
			surveyQuestion.possibleAnswers.sort()
			surveyQuestion.validate()
			
			List answersValidation = surveyQuestion.possibleAnswers*.validate()
			
			if (surveyQuestion.hasErrors() || answersValidation.contains(false)) {
				status.setRollbackOnly()
				renderJSONPost([success: false])
				return
			} else {
				Utils.save(surveyQuestion, true)
				renderJSONPost([success: true])
			}
		}
	}

	def listSurveyQuestions() {
		List questions = SurveyQuestion.findAll()
		Map model = [questions: questions]
		model
	}

	def showSurveyQuestion(Long id) {
		SurveyQuestion surveyQuestion = id ? SurveyQuestion.get(id) : null
		if (!surveyQuestion) {
			redirect(uri: "admin/listSurveyQuestions")
			flash.message = "Could not find the question you are looking for!"
			return
		}
		[surveyQuestion: surveyQuestion]
	}

	def deletePossibleAnswer(Long answerId, Long questionId) {

		SurveyQuestion surveyQuestion = questionId ? SurveyQuestion.get(questionId) : null

		if (!answerId || !surveyQuestion) {
			renderJSONPost([success: false])
			return
		}
		SurveyAnswer surveyAnswer = surveyQuestion.possibleAnswers.find{ answerInstance-> answerInstance.id == answerId}
		surveyQuestion.removeFromPossibleAnswers(surveyAnswer)
		Utils.save(surveyQuestion, true)
		renderJSONPost([success: true])
	}

	def updateSurveyAnswer() {
		SurveyAnswer surveyAnswer = params.answerId ? SurveyAnswer.get(params.answerId) : null
		if (!surveyAnswer) {
			renderJSONPost([success: false])
			return
		}

		grailsWebDataBinder.bind(surveyAnswer, 
			params as SimpleMapDataBindingSource, ["code", "answer", "priority", "answerType"])
		surveyAnswer.validate()

		if (surveyAnswer.hasErrors()) {
			renderJSONPost([success: false])
		} else {
			Utils.save(surveyAnswer, true)
			renderJSONPost([success: true])
		}
	}

	def deleteSurveyQuestion(Long questionId) {
		SurveyQuestion surveyQuestion = questionId ? SurveyQuestion.get(questionId) : null

		if (!surveyQuestion) {
			renderJSONPost([success: false])
			return
		}

		surveyQuestion.delete(flush: true)
		renderJSONPost([success: true])
	}
}
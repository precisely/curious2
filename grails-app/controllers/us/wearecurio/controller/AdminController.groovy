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

			if (!surveyQuestion) {
				redirect(uri: "admin/survey?id=" + params.id)
				flash.message = g.message(code: "default.not.found.message",
						args: ["Survey question", params.id])
			}

			grailsWebDataBinder.bind(surveyQuestion, 
				params as SimpleMapDataBindingSource, ["code", "status", "priority", "question"])
			surveyQuestion.validate()
	
			if (!surveyQuestion.hasErrors()) {
				Utils.save(surveyQuestion, true)
				redirect(uri: "admin/listSurveyQuestions")
			} else {
				redirect(uri: "admin/survey?id=" + params.id)
				flash.message = g.message(code: "not.created.message", args: ["Survey question"])
			}
		} else {
			SurveyQuestion surveyQuestion = SurveyQuestion.create(params)
			
			if (surveyQuestion) {
				Map model = [surveyQuestion: surveyQuestion];
				redirect(uri: "admin/addPossibleAnswers?id=$surveyQuestion.id")
			} else {
				redirect(uri: "admin/survey")
				flash.message = g.message(code: "not.created.message", args: ['Survey question'])
			}
		}
	}

	def addPossibleAnswers(SurveyQuestion surveyQuestion) {
		if (!surveyQuestion) {
			redirect(uri: "admin/survey")
			flash.message = g.message(code: "default.not.found.message", args: ['Survey question', params.id])
			return
		} else {
			Map model = [surveyQuestion: surveyQuestion];
			render(view: "/admin/createPossibleAnswers", model: model)
		}
	}

	def createAnswersData() {
		SurveyQuestion surveyQuestion = SurveyQuestion.get(params.questionId)
		if (!surveyQuestion) {
			renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
					args: ["Survey question", params.questionId])])
					return
		}

		SurveyAnswer.withTransaction { status ->
			grailsWebDataBinder.bind(surveyQuestion, params as SimpleMapDataBindingSource, ["possibleAnswers"])
			surveyQuestion.possibleAnswers.removeAll([null])
			surveyQuestion.possibleAnswers.sort()
			surveyQuestion.validate()
			
			List answersValidation = surveyQuestion.possibleAnswers*.validate()
			
			if (surveyQuestion.hasErrors() || answersValidation.contains(false)) {
				status.setRollbackOnly()
				renderJSONPost([success: false, message: g.message(code: "not.created.message", 
					args: ["Survey answers"])])
				return
			} 
			Utils.save(surveyQuestion, true)
			renderJSONPost([success: true])
		}
	}

	def listSurveyQuestions() {
		List questions = SurveyQuestion.findAll()
		Map model = [questions: questions]
		model
	}

	def showSurveyQuestion(SurveyQuestion surveyQuestion) {
		if (!surveyQuestion) {
			redirect(uri: "admin/listSurveyQuestions")
			flash.message = g.message(code: "default.not.found.message", args: ["Survey question", params.id])
			return
		}
		[surveyQuestion: surveyQuestion]
	}

	def deletePossibleAnswerData(Long answerId, Long questionId) {

		if (!answerId) {
			renderJSONPost([success: false, message: g.message(code: "default.null.message", 
					args: ["id", "Answer"])])
			return
		}

		SurveyQuestion surveyQuestion = SurveyQuestion.get(questionId)
		if (!surveyQuestion) {
			renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
					args: ["Survey question", questionId])])
			return
		}

		SurveyAnswer surveyAnswer = surveyQuestion.possibleAnswers.find{ answerInstance-> answerInstance.id == answerId}

		if (!surveyAnswer) {
			renderJSONPost([success: false, message: g.message(code: "default.not.found.message",
					args: ["Survey answer", answerId])])
			return
		}
		surveyQuestion.removeFromPossibleAnswers(surveyAnswer)
		Utils.save(surveyQuestion, true)
		renderJSONPost([success: true])
	}

	def updateSurveyAnswerData() {
		log.debug "Admin.updateSurveyAnswerData: params: $params"
		
		SurveyAnswer surveyAnswer = SurveyAnswer.get(params.answerId)
		if (!surveyAnswer) {
			renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
					args: ['Survey answer', params.answerId])])
			return
		}

		grailsWebDataBinder.bind(surveyAnswer, 
			params as SimpleMapDataBindingSource, ["code", "answer", "priority", "answerType"])
		surveyAnswer.validate()

		if (surveyAnswer.hasErrors()) {
			renderJSONPost([success: false, message: g.message(code: "default.not.updated.message", 
					args: ['Survey answer'])])
		} else {
			Utils.save(surveyAnswer, true)
			renderJSONPost([success: true])
		}
	}

	def deleteSurveyQuestionData(SurveyQuestion surveyQuestion) {

		if (!surveyQuestion) {
			renderJSONPost([success: false, message: g.message(code: "default.not.found.message", 
					args: ['Survey question', params.id])])
			return
		}

		surveyQuestion.delete(flush: true)
		renderJSONPost([success: true])
	}
}
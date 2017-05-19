package us.wearecurio.controller.survey

import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.controller.LoginController
import us.wearecurio.model.User
import us.wearecurio.model.survey.PossibleAnswer
import us.wearecurio.model.survey.Question
import us.wearecurio.model.survey.SurveyStatus
import us.wearecurio.model.survey.UserAnswer
import us.wearecurio.model.survey.Survey
import us.wearecurio.utility.Utils

class SurveyController extends LoginController {

	def index() {
		log.debug "SurveyList: $params"

		[surveyList: Survey.list(max: 100)]
	}

	def surveyDetails(Survey surveyInstance) {
		log.debug "SurveyDetails: $params"

		if (!surveyInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Survey', params.id])
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		[surveyInstance: surveyInstance]
	}

	def create() {
		log.debug "CreateSurvey: $params"

		render model: [surveyInstance: new Survey()], view: 'surveyForm'
	}

	def save() {
		log.debug "Save survey: $params"

		Survey surveyInstance = Survey.findByCodeAndTitle(params.code, params.title)
		if (surveyInstance) {
			flash.message = 'Survey with this code and title already exists.'
			flash.messageType = 'danger'
			redirect(uri: 'survey/create')

			return
		}

		surveyInstance = new Survey(params)

		if (!Utils.save(surveyInstance)) {
			flash.message = 'Could not save survey.'
			flash.messageType = 'danger'
			redirect(uri: 'survey/create')

			return
		}

		flash.message = 'Survey saved successfully.'
		flash.messageType = 'success'

		redirect(uri: 'survey/index')
	}

	def edit(Survey surveyInstance) {
		log.debug "Edit survey: $params"

		if (!surveyInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Survey', params.id])
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		render model: [surveyInstance: surveyInstance], view: 'surveyForm'
	}

	def update(Survey surveyInstance) {
		log.debug "Update survey: $params"

		if (!surveyInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Survey', params.id])
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		bindData(surveyInstance, params)

		if (!Utils.save(surveyInstance)) {
			flash.message = 'Could not update survey.'
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		flash.message = 'Survey updated successfully.'
		flash.messageType = 'success'

		render model: [surveyInstance: surveyInstance], view: 'surveyDetails'
	}

	def addQuestion() {
		log.debug "Add survey question: $params"

		render model: [questionInstance: new Question(), surveyId: params.id], view: 'questionForm'
	}

	def questionDetails(Question questionInstance) {
		log.debug "QuestionDetails: $params"

		if (!questionInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Question', params.id])
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		[questionInstance: questionInstance, surveyId: params.surveyId]
	}

	def saveQuestion(Survey surveyInstance) {
		log.debug "Save survey question: $params"

		if (!surveyInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Survey', params.id])
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		surveyInstance.addToQuestions(params)

		if (!Utils.save(surveyInstance, true)) {
			flash.message = 'Could not add question.'
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		flash.message = 'Question added successfully.'
		flash.messageType = 'success'

		render model: [surveyInstance: surveyInstance], view: 'surveyDetails'
	}

	def editQuestion(Question questionInstance) {
		if (!questionInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Question', params.id])
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		render model: [questionInstance: questionInstance, surveyId: params.surveyId], view: 'questionForm'
	}

	def updateQuestion(Question questionInstance) {
		log.debug "Update Question: $params"

		if (!questionInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Question', params.id])
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return
		}

		bindData(questionInstance, params)

		if (!Utils.save(questionInstance)) {
			flash.message = 'Could not update question.'
			flash.messageType = 'danger'
			redirect(uri: 'survey/index')

			return 
		}

		flash.message = 'Question updated successfully.'
		flash.messageType = 'success'

		render model: [surveyInstance: questionInstance.survey], view: 'surveyDetails'
	}

	def addAnswers(Question questionInstance) {
		log.debug "Add Answers: $params"

		if (!questionInstance) {
			renderJSONPost([success: false, message: g.message(code: "default.not.found.message",
					args: ["Question", params.id])])

			return
		}

		List possibleAnswersList = []

		try {
			possibleAnswersList = JSON.parse(params.possibleAnswers) as List
		} catch(ConverterException e) {
			log.error "Could not parse possible answers from request.", e
			renderJSONPost([success: false, message: 'Invalid Data'])

			return
		}

		questionInstance.addOrUpdateAnswers(possibleAnswersList)

		if (Utils.save(questionInstance, true)) {
			renderJSONPost([success: true, message: 'Answers added successfully'])
		} else {
			renderJSONPost([success: false, message: 'Could not add answers'])
		}
	}

	def deleteAnswer(PossibleAnswer possibleAnswerInstance, Question questionInstance) {
		log.debug "Delete Answer: $params"

		if (!questionInstance) {
			renderJSONPost([success: false, message: 'Could not find Question.'])

			return
		}

		if (!possibleAnswerInstance) {
			renderJSONPost([success: false, message: 'Could not find answer.'])

			return
		}

		questionInstance.removeFromAnswers(possibleAnswerInstance)

		if (Utils.save(questionInstance, true)) {
			renderJSONPost([success: true, message: 'Deleted successfully.'])
		} else {
			renderJSONPost([success: false, message: 'Could not delete answer.'])
		}
	}
}

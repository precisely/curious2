package us.wearecurio.controller.survey

import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.springframework.dao.DataIntegrityViolationException
import us.wearecurio.controller.LoginController
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.survey.PossibleAnswer
import us.wearecurio.model.survey.Question
import us.wearecurio.model.survey.QuestionStatus
import us.wearecurio.model.survey.Status
import us.wearecurio.model.survey.SurveyStatus
import us.wearecurio.model.survey.UserAnswer
import us.wearecurio.model.survey.Survey
import us.wearecurio.model.survey.UserSurvey
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

		params.code = params.code?.toLowerCase()
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

	/**
	 * This endpoint can be used to toggle a question's status.
	 *
	 * @params questionInstance: Instance of the Question to change status.
	 */
	def toggleQuestionStatus(Question questionInstance) {
		log.debug "toggleQuestionStatus params: $params"

		if (!questionInstance) {
			flash.message = g.message(code: "default.not.found.message", args: ['Question', params.id])
			flash.messageType = 'danger'
			redirect(uri: "survey/surveyDetails/${params.surveyId}")

			return
		}

		QuestionStatus status = questionInstance.status
		questionInstance.status = status == QuestionStatus.ACTIVE ? QuestionStatus.INACTIVE : QuestionStatus.ACTIVE

		if (!Utils.save(questionInstance)) {
			flash.message = 'Could not update question status.'
			flash.messageType = 'danger'
			redirect(uri: "survey/surveyDetails/${params.surveyId}")

			return
		}

		redirect(uri: "survey/surveyDetails/${params.surveyId}")
	}

	def saveQuestion(Survey surveyInstance) {
		log.debug "Save survey question: $params"

		if (!surveyInstance) {
			renderJSONPost([success: false, message: 'Survey does not exist.'])

			return
		}

		Survey.withTransaction { status ->
			Question questionInstance = new Question(params)

			surveyInstance.addToQuestions(questionInstance)

			if (!Utils.save(surveyInstance, true)) {
				renderJSONPost([success: false, message: 'Could not add question.'])

				return
			}

			List possibleAnswersList = []

			try {
				possibleAnswersList = JSON.parse(params.possibleAnswers) as List
			} catch (ConverterException e) {
				status.setRollbackOnly()

				log.error "Could not parse possible answers from request.", e
				renderJSONPost([success: false, message: 'Invalid data.'])

				return
			}

			questionInstance.addOrUpdateAnswers(possibleAnswersList)

			if (!Utils.save(questionInstance, true)) {
				status.setRollbackOnly()

				renderJSONPost([success: false, message: 'Could not add answers.'])

				return
			}
		}

		renderJSONPost([success: true, message: 'Question added successfully.'])
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
			renderJSONPost([success: false, message: 'Question does not exist.'])

			return
		}

		bindData(questionInstance, params)

		List possibleAnswersList = []

		try {
			possibleAnswersList = JSON.parse(params.possibleAnswers) as List
		} catch (ConverterException e) {
			log.error "Could not parse possible answers from request.", e
			renderJSONPost([success: false, message: 'Invalid data.'])

			return
		}

		questionInstance.addOrUpdateAnswers(possibleAnswersList)

		if (!Utils.save(questionInstance)) {
			renderJSONPost([success: false, message: 'Could not update question.'])

			return
		}

		renderJSONPost([success: true, message: 'Question updated successfully.'])
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

	/**
	 * This endpoint can be used when a User marks a Survey as IGNORED.
	 *
	 * @params surveyId: Valid id of the Survey
	 *
	 * @return userSurveyInstance
	 */
	def ignore() {
		log.debug "Ignore Survey: $params"

		User user = sessionUser()

		Survey survey = Survey.findById(params.surveyId)

		if (!survey) {
			log.debug "Survey not found for surveyId: ${params.surveyId}"
			renderJSONGet([success: false, message: "Survey not found for surveyId: ${params.surveyId}"])

			return
		}

		UserSurvey userSurvey = UserSurvey.findByUserAndSurvey(user, survey)
		if (!userSurvey) {
			log.debug "UserServey missing for user - ${user} and survey - ${survey}"
			renderJSONGet([success: false, message: "UserServey missing for user - ${user} and survey - ${survey}"])

			return
		}

		userSurvey.status = Status.IGNORED
		Utils.save(userSurvey, true) ? renderJSONGet([success: true]) : renderJSONGet([success: false])
	}

	/**
	 * An endpoint to remove associatedProfileTags or AssociatedTrackingTags from PossibleAnswer.
	 *
	 * @params answerId - PossibleAnswer Id
	 * @params tagDescription - Description of the tag to be removed
	 * @params tagType - either profileTag or trackingTag
	 * @return success value
	 */
	def removeAssociatedTagFromPossibleAnswer() {
		log.debug "Remove associated tags from PossibleAnswer: $params"

		if (!params.answerId || !params.tagDescription || !(params.tagType in ['profileTag', 'trackingTag'])) {
			log.debug('Invalid params to remove associatedTags from PossibleAnswer')
			renderJSONGet([success: false])

			return
		}

		PossibleAnswer possibleAnswer = PossibleAnswer.get(params.answerId)
		Tag tag = Tag.findByDescription(params.tagDescription.trim())

		if (!possibleAnswer || !tag) {
			log.debug("Could not find Tag or PossibleAnswer for tagDesc: ${params.tagDescription} and" +
					" answer id ${params.answerId}")
			renderJSONGet([success: false])

			return
		}

		params.tagType == 'profileTag' ? possibleAnswer.removeFromAssociatedProfileTags(tag) :
				possibleAnswer.removeFromAssociatedTrackingTags(tag)

		return Utils.save(possibleAnswer) ? renderJSONGet([success: true]) : renderJSONGet([success: false])
	}
}

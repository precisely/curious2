package us.wearecurio.controller.integration

import grails.converters.JSON
import grails.test.mixin.TestFor
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONElement
import org.springframework.context.MessageSource
import spock.lang.Specification
import spock.lang.Unroll
import us.wearecurio.controller.survey.SurveyController
import us.wearecurio.model.User
import us.wearecurio.model.profiletags.ProfileTag
import us.wearecurio.model.survey.AnswerType
import us.wearecurio.model.survey.Question
import us.wearecurio.model.survey.PossibleAnswer
import us.wearecurio.model.survey.QuestionStatus
import us.wearecurio.model.survey.Status
import us.wearecurio.model.survey.Survey
import us.wearecurio.model.survey.SurveyStatus
import us.wearecurio.model.survey.UserSurvey

class SurveyControllerSpec extends IntegrationSpec {

	SurveyController controller
	MessageSource messageSource

	void setup() {
		controller = new SurveyController()
	}

	@Unroll
	void "test save action for successfully saving survey"() {
		given: 'Params for Survey'
		controller.params.code = code
		controller.params.title = title
		controller.params.status = status

		when: 'The save action is hit'
		controller.save()

		then: 'The request should redirect to index action'
		controller.response.redirectedUrl == 'survey/index'
		controller.flash.message == 'Survey saved successfully.'
		Survey.count() == 1

		Survey surveyInstance = Survey.first()
		surveyInstance.code == code.toLowerCase()
		surveyInstance.title == title

		where:
		code   | title                              | status
		'S001' | 'This is the first survey title.'  | 'ACTIVE'
		'S002' | 'This is the second survey title.' | 'INACTIVE'
	}

	void "test save action when a survey with same title and code already exists"() {
		given: 'A Survey instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)

		assert Survey.count() == 1

		when: 'The save action is hit with same title and code'
		controller.params.code = 's001'
		controller.params.title = 'This is the first survey title.'
		controller.params.status = 'INACTIVE'

		controller.save()

		then: 'The request should redirect to create action due to failure'
		controller.response.redirectedUrl == 'survey/create'
		controller.flash.message == 'Survey with this code and title already exists.'
		Survey.count() == 1
	}

	@Unroll
	void "test save action when any of the field's value is missing in the request"() {
		given: 'Survey params'
		controller.params.code = code
		controller.params.title = title
		controller.params.status = status

		when: 'The save action is hit'
		controller.save()

		then: 'The request should redirect to create action due to failure'
		controller.response.redirectedUrl == 'survey/create'
		controller.flash.message == 'Could not save survey.'
		Survey.count() == 0

		where:
		code   | title    | status
		''     | 'Survey' | 'ACTIVE'
		's001' | ''       | 'ACTIVE'
		's001' | 'Survey' | ''
		's001' | ''       | ''
		''     | 'Survey' | ''
		''     | ''       | 'ACTIVE'
		null   | null     | null
	}

	void "test edit action for rendering survey form"() {
		given: 'A Survey instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)

		assert Survey.count() == 1

		when: 'The edit action is hit'
		controller.params.id = surveyInstance.id
		controller.edit()

		then: 'The surveyForm view should be rendered with survey instance as model'
		controller.modelAndView.viewName == '/survey/surveyForm'
		controller.modelAndView.model['surveyInstance'] == surveyInstance
	}

	void "test edit action for redirecting to index when id is incorrect"() {
		when: 'The edit action is hit with random id'
		controller.params.id = 1
		controller.edit()

		then: 'The request should redirect to index action'
		controller.flash.message == 'Survey not found with id 1'
		controller.response.redirectedUrl == 'survey/index'
	}

	void "test update action when the survey instance with id is not found"() {
		when: 'The update action is hit with random id'
		controller.params.id = 1
		controller.update()

		then: 'The request should redirect to index action due to failure'
		controller.flash.message == 'Survey not found with id 1'
		controller.flash.messageType == 'danger'
		controller.response.redirectedUrl == 'survey/index'
	}

	void "test update action for successfully updating a survey"() {
		given: 'A Survey instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)

		assert Survey.count() == 1

		when: 'The update action is hit with updated code and title'
		controller.params.id = surveyInstance.id
		controller.params.code = 's001-updated'
		controller.params.title = 'This is the first survey title updated'
		controller.params.status = 'INACTIVE'

		controller.update()

		then: 'The surveyDetails view should be rendered with survey instance as model'
		Survey.count() == 1

		controller.modelAndView.viewName == '/survey/surveyDetails'
		controller.modelAndView.model['surveyInstance'] == surveyInstance
		controller.flash.message == 'Survey updated successfully.'

		surveyInstance.code == 's001-updated'
		surveyInstance.title == 'This is the first survey title updated'
	}

	@Unroll
	void "test update action when any of the field's value is incorrect in the request"() {
		given: 'A Survey instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)

		assert Survey.count() == 1

		when: 'The update action is hit'
		controller.params.id = surveyInstance.id
		controller.params.code = code
		controller.params.title = title
		controller.params.status = status

		controller.update()

		then: 'The request should redirect to index action due to failure'
		controller.flash.message == 'Could not update survey.'
		controller.response.redirectedUrl == 'survey/index'
		Survey.count() == 1

		where:
		code   | title    | status
		''     | 'Survey' | 'ACTIVE'
		's001' | ''       | 'ACTIVE'
		's001' | 'Survey' | ''
		's001' | ''       | ''
		''     | 'Survey' | ''
		''     | ''       | 'ACTIVE'
		null   | null     | null
	}

	void "test saveQuestion action for redirecting to index when survey id is incorrect"() {
		when: 'The saveQuestion action is hit with random id'
		controller.params.id = 1
		controller.saveQuestion()

		then: 'Server responds with appropriate error message'
		controller.response.json.success == false
		controller.response.json.message == "Survey does not exist."
	}

	void "test saveQuestion action for successfully adding question and answers to survey"() {
		given: 'A Survey instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)

		assert Survey.count() == 1
		assert surveyInstance.questions.size() == 0

		when: 'The saveQuestion action is hit with question params'
		controller.params.id = surveyInstance.id
		controller.params.question = 'This is the first question.'
		controller.params.priority = '1'
		controller.params.status = 'INACTIVE'
		controller.params.answerType = 'MCQ_RADIO'
		controller.params.isRequired = false
		controller.params.possibleAnswers = ([[profileTags: ['run'], priority: '2',
				answer: 'This is the first answer.'],
				[profileTags: ['mood'], priority: '3', answer: 'This is the second answer']] as JSON).toString()

		controller.saveQuestion()

		then: 'The server responds with success message and questions are added to Survey'
		Survey.count() == 1

		controller.response.json.success == true
		controller.response.json.message == 'Question added successfully.'

		surveyInstance.status == SurveyStatus.ACTIVE
		surveyInstance.questions.size() == 1

		Question questionInstance = Question.first()
		questionInstance.question == 'This is the first question.'
		questionInstance.priority == 1
		questionInstance.status == QuestionStatus.INACTIVE
		questionInstance.answerType == AnswerType.MCQ_RADIO
		questionInstance.isRequired == false

		questionInstance.answers.size() == 2
		PossibleAnswer possibleAnswer1 = questionInstance.answers.first()
		possibleAnswer1.answer == 'This is the first answer.'
		possibleAnswer1.associatedProfileTags[0].description == 'run'
		possibleAnswer1.priority == 2

		PossibleAnswer possibleAnswer2 = questionInstance.answers.last()
		possibleAnswer2.answer == 'This is the second answer'
		possibleAnswer2.associatedProfileTags[0].description == 'mood'
		possibleAnswer2.priority == 3
	}

	@Unroll
	void "test saveQuestion action when question cannot be saved due to invalid params"() {
		given: 'A Survey instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)

		assert Survey.count() == 1
		assert surveyInstance.questions.size() == 0

		when: 'The saveQuestion action is hit with question params'
		controller.params.id = surveyInstance.id
		controller.params.question = question
		controller.params.priority = priority
		controller.params.status = status
		controller.params.answerType = answerType
		controller.params.possibleAnswers = possibleAnswers

		controller.saveQuestion()

		then: 'The server responds with appropriate error message and question does not get saved.'
		controller.response.json.success == false
		controller.response.json.message == 'Could not add question.'
		Survey.count() == 1
		Question.count() == 0

		where:
		question   | priority | status    | answerType   | possibleAnswers
		''         | '1'      | 'ACTIVE'  | 'MCQ_RADIO'  | ""
		'question' | ''       | 'ACTIVE'  | 'MCQ_RADIO'  | ""
		'question' | '1'      | ''        | 'MCQ_RADIO'  | ""
		'question' | '1'      | 'ACTIVE'  | ''           | ""
		'question' | '1'      | 'INVALID' | 'MCQ_RADIO'  | ""
		'question' | '1'      | 'ACTIVE'  | 'INVALID'    | ""
		null       | null     | null      | null         | ""
	}

	void "test updateQuestion action when the question instance with id is not found"() {
		when: 'The updateQuestion action is hit with random id'
		controller.params.id = 1
		controller.updateQuestion()

		then: 'The server responds with appropriate message'
		controller.response.json.success == false
		controller.response.json.message == 'Question does not exist.'
	}

	void "test updateQuestion action for successfully updating a question and answers"() {
		given: 'A Survey instance with added question'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'INACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)

		PossibleAnswer possibleAnswer = new PossibleAnswer(answer: "This is the first answer.", priority: 1)
		surveyInstance.questions[0].addToAnswers(possibleAnswer)
		surveyInstance.questions[0].save(flush: true)

		assert Survey.count() == 1
		assert Question.count() == 1
		assert PossibleAnswer.count() == 1

		when: 'The updateQuestion action is hit with updated params'
		controller.params.id = surveyInstance.questions[0].id
		controller.params.priority = '2'
		controller.params.question = 'This is the first question updated'
		controller.params.status = 'ACTIVE'
		controller.params.answerType = 'MCQ_CHECKBOX'
		controller.params.possibleAnswers = ([[profileTags: ['run'], priority: '2',
				answer: 'This is the first answer updated', answerId: possibleAnswer.id],
				[profileTags: ['mood'], priority: '3', answer: 'This is the second answer']] as JSON).toString()

		controller.updateQuestion()

		then: 'Question gets updated successfully.'
		Survey.count() == 1

		controller.response.json.success == true
		controller.response.json.message == 'Question updated successfully.'

		Question questionInstance = surveyInstance.questions[0]
		questionInstance.question == 'This is the first question updated'
		questionInstance.priority == 2
		questionInstance.status == QuestionStatus.ACTIVE
		questionInstance.answerType == AnswerType.MCQ_CHECKBOX

		questionInstance.answers.size() == 2
		questionInstance.answers.first().answer == 'This is the first answer updated'
		questionInstance.answers.first().priority == 2
		questionInstance.answers.last().answer == "This is the second answer"
	}

	void "test deleteAnswer action deleting answer from a question instance"() {
		given: 'A Survey instance with added question'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'INACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)

		surveyInstance.questions[0].addOrUpdateAnswers([[priority: '1', answer: 'This is the first answer']])
		surveyInstance.save(flush: true)

		assert Survey.count() == 1
		assert Question.count() == 1
		assert PossibleAnswer.count() == 1

		when: 'The delete action is hit with invalid question id'
		controller.params.'questionInstance.id' = 2
		controller.deleteAnswer()

		then: 'The response fails with respective message'
		JSONElement json = JSON.parse(controller.response.text)
		json.success == false
		json.message == 'Could not find Question.'

		when: 'The delete action is hit with invalid answer id'
		controller.response.reset()
		controller.params.'questionInstance.id' = surveyInstance.questions[0].id
		controller.params.'possibleAnswerInstance.id' = 2
		controller.deleteAnswer()

		then: 'The response fails with respective message'
		JSONElement json1 = JSON.parse(controller.response.text)
		json1.success == false
		json1.message == 'Could not find answer.'

		when: 'The delete action is hit with valid answer id'
		controller.response.reset()
		controller.params.'questionInstance.id' = surveyInstance.questions[0].id
		controller.params.'possibleAnswerInstance.id' = surveyInstance.questions[0].answers[0].id
		controller.deleteAnswer()

		then: 'The response succeeds with respective message'
		JSONElement json2 = JSON.parse(controller.response.text)
		json2.success == true
		json2.message == 'Deleted successfully.'
		PossibleAnswer.count() == 0
	}

	void 'test ignore endpoint for various cases'() {
		given: 'Survey and a User instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)
		Survey surveyInstance1 = new Survey(code: 's002', title: 'This is the second survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance1.save(flush: true)

		assert Survey.count() == 2

		User user = User.create([username: "y", sex: "F", email: "y@y.com", birthdate: "01/01/2001", name: "y y",
				password: "y"])

		UserSurvey userSurvey = UserSurvey.createOrUpdate(user, surveyInstance, Status.NOT_TAKEN)
		assert userSurvey.id != null
		assert userSurvey.status == Status.NOT_TAKEN

		when: 'The ignore endpoint is hit without surveyId'
		controller.request.method = 'GET'
		controller.params.surveyId = null
		controller.session.userId = user.id
		controller.ignore()

		then: 'Result should match the expected response'
		controller.response.status == 200
		controller.response.json.success == false
		controller.response.json.message == 'Survey not found for surveyId: null'

		when: 'The ignore endpoint is hit and no survey exist for the given user'
		controller.params.clear()
		controller.response.reset()
		controller.params.surveyId = surveyInstance1.id
		controller.session.userId = user.id
		controller.ignore()

		then: 'Server responds with failure message'
		controller.response.status == 200
		controller.response.json.success == false
		controller.response.json.message == "UserServey missing for user - ${user} and survey - ${surveyInstance1}"

		when: 'The ignore endpoint is hit and Survey gets ignored successfully'
		controller.params.clear()
		controller.response.reset()
		controller.params.surveyId = surveyInstance.id
		controller.session.userId = user.id
		controller.ignore()

		then: 'Server responds with success message'
		controller.response.status == 200
		controller.response.json.success == true
		userSurvey.status == Status.IGNORED
	}

	void 'test removeAssociatedTagFromPossibleAnswer action for various cases'() {
		given: 'An instance of PossibleAnswer and a few Tags'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: 1, status: 'ACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)
		surveyInstance.questions[0].addOrUpdateAnswers([[priority: '1', answer: 'This is the first answer',
				profileTags: ['sleep', 'run'], trackingTags: ['eat', 'mood']]])
		surveyInstance.save(flush: true)
		PossibleAnswer possibleAnswer = surveyInstance.questions.answers.first().first()
		assert possibleAnswer.id
		assert possibleAnswer.associatedTrackingTags.size() == 2
		assert possibleAnswer.associatedProfileTags.size() == 2

		when: 'removeAssociatedTagFromPossibleAnswer is hit and tagType is invalid in params'
		controller.request.method = 'GET'
		controller.params.answerId = possibleAnswer.id
		controller.params.tagDescription = 'sleep'
		controller.params.tagType = 'profiletag'
		controller.removeAssociatedTagFromPossibleAnswer()

		then: 'server should return false'
		controller.response.status == 200
		controller.response.json.success == false

		when: 'The answer id is invalid'
		controller.params.clear()
		controller.response.reset()
		controller.params.answerId = '1234'
		controller.params.tagDescription = 'sleep'
		controller.params.tagType = 'profileTag'
		controller.removeAssociatedTagFromPossibleAnswer()

		then: 'server should return false'
		controller.response.status == 200
		controller.response.json.success == false

		when: 'An associatedProfileTag is removed successfully'
		controller.params.clear()
		controller.response.reset()
		controller.params.answerId = possibleAnswer.id
		controller.params.tagDescription = 'sleep'
		controller.params.tagType = 'profileTag'
		controller.removeAssociatedTagFromPossibleAnswer()

		then: 'server should return true'
		controller.response.status == 200
		controller.response.json.success == true
		possibleAnswer.associatedProfileTags.size() == 1
	}

	void 'test toggleQuestionStatus endpoint for various cases'() {
		given: 'Survey, Question and a User instance'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.save(flush: true)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: 1, status: 'ACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)

		Question questionInstance = surveyInstance.questions[0]

		assert Survey.count() == 1
		assert surveyInstance.questions.size() == 1
		assert questionInstance.status == QuestionStatus.ACTIVE

		when: 'The toggleQuestionStatus endpoint is hit with invalid question'
		controller.request.method = 'GET'
		controller.params.id = null
		controller.params.surveyId = surveyInstance.id
		controller.toggleQuestionStatus()

		then: 'Server should respond with error message'
		questionInstance.status == QuestionStatus.ACTIVE
		controller.response.redirectedUrl == "survey/surveyDetails/${surveyInstance.id}"
		controller.flash.message == "Question not found with id null"

		when: 'The toggleQuestionStatus endpoint is hit with valid questionInstance'
		controller.params.clear()
		controller.response.reset()
		controller.params.id = questionInstance.id
		controller.params.surveyId = surveyInstance.id
		controller.toggleQuestionStatus()

		then: 'The question status gets toggled and User is redirected to Survey details page'
		questionInstance.status == QuestionStatus.INACTIVE
		controller.response.redirectedUrl == "survey/surveyDetails/${surveyInstance.id}"
	}
}

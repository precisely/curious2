package us.wearecurio.controller.integration

import grails.converters.JSON
import grails.test.mixin.TestFor
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONElement
import spock.lang.Specification
import spock.lang.Unroll
import us.wearecurio.controller.survey.SurveyController
import us.wearecurio.model.survey.AnswerType
import us.wearecurio.model.survey.Question
import us.wearecurio.model.survey.PossibleAnswer
import us.wearecurio.model.survey.QuestionStatus
import us.wearecurio.model.survey.Survey
import us.wearecurio.model.survey.SurveyStatus

class SurveyControllerSpec extends IntegrationSpec {

	SurveyController controller

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
		surveyInstance.code == code
		surveyInstance.title == title

		where:
		code   | title                              | status
		's001' | 'This is the first survey title.'  | 'ACTIVE'
		's002' | 'This is the second survey title.' | 'INACTIVE'
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

		then: 'The request should redirect to index action'
		controller.flash.message == 'Survey not found with id 1'
		controller.response.redirectedUrl == 'survey/index'
	}

	void "test saveQuestion action for successfully adding question to survey"() {
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

		controller.saveQuestion()

		then: 'The surveyDetails view should be rendered with survey instance as model'
		Survey.count() == 1

		controller.modelAndView.viewName == '/survey/surveyDetails'
		controller.modelAndView.model['surveyInstance'] == surveyInstance
		controller.flash.message == 'Question added successfully.'

		surveyInstance.status == SurveyStatus.ACTIVE
		surveyInstance.questions.size() == 1

		Question questionInstance = Question.first()
		questionInstance.question == 'This is the first question.'
		questionInstance.priority == 1
		questionInstance.status == QuestionStatus.INACTIVE
		questionInstance.answerType == AnswerType.MCQ_RADIO
		questionInstance.isRequired == false
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

		controller.saveQuestion()

		then: 'The request should redirect to index action due to failure'
		controller.flash.message == 'Could not add question.'
		controller.response.redirectedUrl == 'survey/index'
		Survey.count() == 1
		Question.count() == 0

		where:
		question   | priority | status    | answerType 
		''         | '1'      | 'ACTIVE'  | 'MCQ_RADIO'
		'question' | ''       | 'ACTIVE'  | 'MCQ_RADIO'
		'question' | '1'      | ''        | 'MCQ_RADIO'
		'question' | '1'      | 'ACTIVE'  | ''
		'question' | '1'      | 'INVALID' | 'MCQ_RADIO'
		'question' | '1'      | 'ACTIVE'  | 'INVALID'
		null       | null     | null      | null
	}

	void "test updateQuestion action when the question instance with id is not found"() {
		when: 'The updateQuestion action is hit with random id'
		controller.params.id = 1
		controller.updateQuestion()

		then: 'The request should redirect to index action due to failure'
		controller.response.redirectedUrl == 'survey/index'
		controller.flash.message == 'Question not found with id 1'
		controller.flash.messageType == 'danger'
	}

	void "test updateQuestion action for successfully updating a question"() {
		given: 'A Survey instance with added question'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'INACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)

		assert Survey.count() == 1
		assert Question.count() == 1

		when: 'The updateQuestion action is hit with updated params'
		controller.params.id = surveyInstance.questions[0].id
		controller.params.priority = '2'
		controller.params.question = 'This is the first question updated'
		controller.params.status = 'ACTIVE'
		controller.params.answerType = 'MCQ_CHECKBOX'

		controller.updateQuestion()

		then: 'The surveyDetails view should be rendered with survey instance as model'
		Survey.count() == 1

		controller.modelAndView.viewName == '/survey/surveyDetails'
		controller.modelAndView.model['surveyInstance'] == surveyInstance
		controller.flash.message == 'Question updated successfully.'

		surveyInstance.questions[0].question == 'This is the first question updated'
		surveyInstance.questions[0].priority == 2
		surveyInstance.questions[0].status == QuestionStatus.ACTIVE
		surveyInstance.questions[0].answerType == AnswerType.MCQ_CHECKBOX
	}

	void "test addAnswers action when the question instance with id is not found"() {
		when: 'The addAnswers action is hit with random id'
		controller.params.id = 1
		controller.addAnswers()

		then: 'The response fails with respective message'
		JSONElement json = JSON.parse(controller.response.text)
		json.success == false
		json.message == 'Question not found with id 1'
		PossibleAnswer.count() == 0
	}

	void "test addAnswers action when the possibleAnswers is invalid JSON"() {
		given: 'A Survey instance with added question'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'INACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)

		assert Survey.count() == 1
		assert Question.count() == 1

		when: 'The addAnswers action is hit with invalid JSON'
		controller.params.id = surveyInstance.questions[0].id
		controller.params.possibleAnswers = "[{tagDescription: sleep]"
		controller.addAnswers()

		then: 'The response fails with respective message'
		JSONElement json = JSON.parse(controller.response.text)
		json.success == false
		json.message == 'Invalid Data'
		PossibleAnswer.count() == 0
	}

	void "test addAnswers action for successfully adding answers"() {
		given: 'A Survey instance with added question'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'INACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)

		assert Survey.count() == 1
		assert Question.count() == 1

		when: 'The addAnswers action is hit with valid JSON answer list'
		controller.response.reset()
		controller.params.id = surveyInstance.questions[0].id
		controller.params.possibleAnswers = ([[tagDescription: 'sleep', priority: '1',
				answer: 'This is the first answer']] as JSON).toString()
		controller.addAnswers()

		then: 'The response succeeds with respective message'
		JSONElement json1 = JSON.parse(controller.response.text)
		json1.success == true
		json1.message == 'Answers added successfully'

		surveyInstance.questions[0].answers.size() == 1
		PossibleAnswer possibleAnswer = PossibleAnswer.first()
		possibleAnswer.answer == 'This is the first answer'
		possibleAnswer.tag.description == 'sleep'
		possibleAnswer.priority == 1

		when: 'The answer list has answerId key, then the answer should be updated'
		controller.response.reset()
		controller.params.id = surveyInstance.questions[0].id
		controller.params.possibleAnswers = ([[tagDescription: 'run', priority: '2',
				answer: 'This is the first answer updated', answerId: possibleAnswer.id],
				[tagDescription: 'mood', priority: '3', answer: 'This is the second answer']] as JSON).toString()
		controller.addAnswers()

		then: 'The response succeeds with respective message'
		JSONElement json2 = JSON.parse(controller.response.text)
		json2.success == true
		json2.message == 'Answers added successfully'

		surveyInstance.questions[0].answers.size() == 2
		PossibleAnswer possibleAnswer1 = PossibleAnswer.first()
		possibleAnswer1.answer == 'This is the first answer updated'
		possibleAnswer1.tag.description == 'run'
		possibleAnswer1.priority == 2

		PossibleAnswer possibleAnswer2 = PossibleAnswer.last()
		possibleAnswer2.answer == 'This is the second answer'
		possibleAnswer2.tag.description == 'mood'
		possibleAnswer2.priority == 3

		when: 'The addAnswers action is hit with incomplete params'
		controller.response.reset()
		controller.params.id = surveyInstance.questions[0].id
		controller.params.possibleAnswers = ([[tagDescription: 'sleep', answer: 'This is the first answer'],
				[tagDescription: 'sleep', priority: '2']] as JSON).toString()
		controller.addAnswers()

		then: 'The response fails with respective message'
		JSONElement json = JSON.parse(controller.response.text)
		json.success == false
		json.message == 'Could not add answers'
	}

	void "test deleteAnswer action deleting answer from a question instance"() {
		given: 'A Survey instance with added question'
		Survey surveyInstance = new Survey(code: 's001', title: 'This is the first survey title.',
				status: SurveyStatus.ACTIVE)
		surveyInstance.addToQuestions(question: 'This is the first question.', priority: '1', status: 'INACTIVE',
				answerType: 'MCQ_RADIO')
		surveyInstance.save(flush: true)

		surveyInstance.questions[0].addOrUpdateAnswers([[tagDescription: 'mood', priority: '1',
				answer: 'This is the first answer']])
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
}

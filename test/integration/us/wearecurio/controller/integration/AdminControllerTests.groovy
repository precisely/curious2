package us.wearecurio.controller.integration


import static org.junit.Assert.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.controller.AdminController
import us.wearecurio.model.*
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils
/**
 *
 */
class AdminControllerTests extends CuriousControllerTestCase {

	def sessionFactory
	AdminController controller
	SurveyQuestion surveyQuestionInstance1
	SurveyQuestion surveyQuestionInstance2
	SurveyQuestion surveyQuestionInstance3

	@Before
	void setUp() {
		super.setUp()

		controller = new AdminController()

		Map questionParams = [code: "qcode1", priority: 4, question: "Question 1?", status: "ACTIVE"]
		surveyQuestionInstance1 = SurveyQuestion.create(questionParams)
		assert surveyQuestionInstance1.id

		questionParams = [code: "qcode2", priority: 5, question: "Question 2?", status: "ACTIVE"]
		surveyQuestionInstance2 = SurveyQuestion.create(questionParams)
		assert surveyQuestionInstance2.id

		questionParams = [code: "qcode3", priority: 3, question: "Question 3?", status: "ACTIVE"]
		surveyQuestionInstance3 = SurveyQuestion.create(questionParams)
		assert surveyQuestionInstance3.id
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testSurvey() {
		// When parameter contains id
		controller.params.id = surveyQuestionInstance1.id
		def model = controller.survey()
		assert model.surveyQuestion != null
		assert model.surveyQuestion.code.equals("qcode1")

		// When parameter contains false id
		controller.params["id"] = 0
		model = controller.survey()
		assert model == null

		// When parameter contains no id
		model = controller.survey()
		assert model == null
	}

	@Test
	void "Test createOrUpdateQestions when question id is passed"() {
		controller.params.id = surveyQuestionInstance1.id
		controller.params["code"] = "qcode4"
		controller.params["status"] = "ACTIVE"
		controller.params["priority"] = 3
		controller.params["question"] = "Question 1?"
		controller.createOrUpdateQuestion()

		SurveyQuestion surveyQuestionInstance = SurveyQuestion.get(surveyQuestionInstance1.id)

		assert surveyQuestionInstance.code.equals("qcode4")
		assert controller.response.redirectUrl.contains("admin/listSurveyQuestions")
		controller.response.reset()
	}

	@Test
	void "Test createOrUpdateQestions when question id is passed and code is not unique"() {
		controller.params.id = surveyQuestionInstance1.id
		controller.params["code"] = "qcode2"
		controller.params["status"] = "ACTIVE"
		controller.params["priority"] = 3
		controller.params["question"] = "Question 1?"
		controller.createOrUpdateQuestion()
		assert controller.response.redirectUrl.contains("admin/survey")
		assert controller.flash.message == messageSource.getMessage("default.not.updated.message",
				["Survey question"] as Object[], null)
		controller.response.reset()
	}

	@Test
	void "Test createOrUpdateQestions when question id is passed and question is missing"() {
		controller.params.id = surveyQuestionInstance1.id
		controller.params["code"] = "qcode1"
		controller.params["status"] = "ACTIVE"
		controller.params["priority"] = 3
		controller.createOrUpdateQuestion()
		assert controller.response.redirectUrl.contains("admin/listSurveyQuestions")
		controller.response.reset()
	}

	@Test
	void "Test createOrUpdateQestions when question id is passed and code is null"() {
		controller.params["id"] = surveyQuestionInstance1.id
		controller.params["status"] = "ACTIVE"
		controller.params["code"] = null
		controller.params["priority"] = 3
		controller.params["question"] = "Question 1?"
		controller.createOrUpdateQuestion()
		assert controller.response.redirectUrl.contains("admin/survey")
		assert controller.flash.message == messageSource.getMessage("default.not.updated.message",
				["Survey question"] as Object[], null)
		controller.response.reset()
	}

	@Test
	void "Test createOrUpdateQestions when question id is passed and code is blank"() {
		controller.params["id"] = surveyQuestionInstance1.id
		controller.params["status"] = "ACTIVE"
		controller.params["code"] = ""
		controller.params["priority"] = 3
		controller.params["question"] = "Question 1?"
		controller.createOrUpdateQuestion()
		assert controller.response.redirectUrl.contains("admin/survey")
		assert controller.flash.message == messageSource.getMessage("default.not.updated.message",
				["Survey question"] as Object[], null)
		controller.response.reset()
	}

	@Test
	void "Test createOrUpdateQestions when no question id is passed"() {
		controller.params["code"] = "qcode4"
		controller.params["status"] = "ACTIVE"
		controller.params["priority"] = 3
		controller.params["question"] = "Question 1?"
		controller.createOrUpdateQuestion()

		SurveyQuestion surveyQuestionInstance = SurveyQuestion.last()
		
		assert surveyQuestionInstance.code.equals("qcode4")
		assert surveyQuestionInstance.priority == 3
		assert surveyQuestionInstance.question.equals("Question 1?")
		assert controller.response.redirectUrl.contains("admin/addPossibleAnswers")
		controller.response.reset()
	}

	@Test
	void "Test createOrUpdateQestions when no question id is passed and code is not unique"() {
		controller.params["code"] = "qcode2"
		controller.params["status"] = "ACTIVE"
		controller.params["priority"] = 3
		controller.params["question"] = "Question 1?"
		controller.createOrUpdateQuestion()

		assert SurveyQuestion.count() == 3
		assert controller.response.redirectUrl.contains("admin/survey")
		assert controller.flash.message == messageSource.getMessage("not.created.message",
				["Survey question"] as Object[], null)
		controller.response.reset()
	}

	@Test
	void "Test createOrUpdateQestions when no question id is passed and code is blank"() {
		controller.params["status"] = "ACTIVE"
		controller.params["code"] = ""
		controller.params["priority"] = 3
		controller.params["question"] = "Question 1?"
		controller.createOrUpdateQuestion()
		assert controller.response.redirectUrl.contains("admin/survey")
		assert controller.flash.message == messageSource.getMessage("not.created.message",
				["Survey question"] as Object[], null)
		controller.response.reset()
	}

	@Test
	void "Test addPossibleAnswers when id is not passed"() {
		controller.addPossibleAnswers()
		assert controller.response.redirectUrl.contains("admin/survey")
		assert controller.flash.message == messageSource.getMessage("default.not.found.message",
				["Survey question", controller.params.id] as Object[], null)
		controller.response.reset()
	}

	@Test
	void "Test addPossibleAnswers when question id is passed"() {
		controller.params.id = surveyQuestionInstance1.id
		controller.addPossibleAnswers()
		def modelAndView = controller.modelAndView
		assert modelAndView.model.surveyQuestion['code'].equals(surveyQuestionInstance1.code)
		assert modelAndView.model.surveyQuestion['question'].equals(surveyQuestionInstance1.question)
		assert modelAndView.getViewName().equals("/admin/createPossibleAnswers")
	}

	@Test
	void "Test createAnswersData when question id is not passed"() {
		controller.createAnswersData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
			["Survey question", controller.params.questionId] as Object[], null)
	}

	@Test
	void "Test createAnswersData when false question id is passed"() {
		controller.params.questionId = 0
		controller.createAnswersData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
			["Survey question", controller.params.questionId] as Object[], null)
	}

	@Test
	void "Test createAnswersData when question id is passed"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "answer": "Answer no 1.", "priority": 4, "answerType": "MCQ"],
			["code": "acode2", "answer": "Answer no 2.", "priority": 3, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "MCQ"]]
		controller.createAnswersData()
		assert controller.response.json.success == true
	}

	@Test
	void "Test createAnswersData when question id is passed and answer code is missing"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "answer": "Answer no 1.", "priority": 4, "answerType": "MCQ"],
			["answer": "Answer no 2.", "priority": 3, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "MCQ"]]
		controller.createAnswersData()

		SurveyQuestion surveyQuestionInstance = SurveyQuestion.get(surveyQuestionInstance1.id)

		assert surveyQuestionInstance.hasErrors()
		assert SurveyAnswer.count() == 0
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("not.created.message",
			["Survey answers"] as Object[], null)
	}

	@Test
	void "Test createAnswersData when question id is passed and answer text is missing"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "priority": 4, "answerType": "MCQ"],
			["code": "acode2", "answer": "Answer no 2.", "priority": 3, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "MCQ"]]
		controller.createAnswersData()

		SurveyQuestion surveyQuestionInstance = SurveyQuestion.get(surveyQuestionInstance1.id)

		assert surveyQuestionInstance.hasErrors()
		assert SurveyAnswer.count() == 0
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("not.created.message",
			["Survey answers"] as Object[], null)
	}

	@Test
	void "Test createAnswersData when question id is passed and answers type is different"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "answer": "Answer no 1.", "priority": 4, "answerType": "MCQ"],
			["code": "acode2", "answer": "Answer no 2.", "priority": 3, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "DESCRIPTIVE"]]
		controller.createAnswersData()

		sessionFactory.currentSession.clear()

		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("not.created.message",
				["Survey answers"] as Object[], null)
		assert SurveyAnswer.findAllByCodeInList(["acode1", "acode2", "acode3"]) == []
	}

	@Test
	void "Test listSurveyQuestions"() {
		def model = controller.listSurveyQuestions()
		assert model.questions.size() > 0
		assert model.questions[0].code.equals("qcode1")
	}

	@Test
	void "Test showSurveyQuestion when id is not passed"() {
		controller.showSurveyQuestion()
		assert controller.response.redirectUrl.contains("admin/listSurveyQuestions")
		assert controller.flash.message == messageSource.getMessage("default.not.found.message", 
				["Survey question", null] as Object[], null)
	}

	@Test
	void "Test showSurveyQuestion when false id is passed"() {
		controller.params.id = 0
		controller.showSurveyQuestion()
		assert controller.response.redirectUrl.contains("admin/listSurveyQuestions")
		assert controller.flash.message == messageSource.getMessage("default.not.found.message", 
				["Survey question", controller.params.id] as Object[], null)
	}

	@Test
	void "Test showSurveyQuestion when correct id is passed"() {
		controller.params.id = surveyQuestionInstance1.id
		def model = controller.showSurveyQuestion()
		assert model.surveyQuestion.code.equals("qcode1")
	}

	@Test
	void "Test deletePossibleAnswerData when no questionId is passed"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "answer": "Answer no 1.", "priority": 4, "answerType": "MCQ"],
			["code": "acode2", "answer": "Answer no 2.", "priority": 3, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "MCQ"]]
		controller.createAnswersData()

		controller.response.reset()

		controller.params["answerId"] = surveyQuestionInstance1.possibleAnswers[0].id
		controller.params["questionId"] = null
		controller.deletePossibleAnswerData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
				["Survey question", controller.params.questionId] as Object[], null)
	}

	@Test
	void "Test deletePossibleAnswerData when no answerId is passed"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.deletePossibleAnswerData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.null.message",
				["id", "Answer"] as Object[], null)
	}

	@Test
	void "Test deletePossibleAnswerData when false questionId is passed"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "answer": "Answer no 1.", "priority": 4, "answerType": "MCQ"],
			["code": "acode2", "answer": "Answer no 2.", "priority": 3, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "MCQ"]]
		controller.createAnswersData()

		controller.response.reset()

		controller.params["answerId"] = surveyQuestionInstance1.possibleAnswers[0].id
		controller.params["questionId"] = 0
		controller.deletePossibleAnswerData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
				["Survey question", controller.params.questionId] as Object[], null)
	}

	@Test
	void "Test deletePossibleAnswerData when correct parameters are passed"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "answer": "Answer no 1.", "priority": 4, "answerType": "MCQ"],
			["code": "acode2", "answer": "Answer no 2.", "priority": 3, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "MCQ"]]
		controller.createAnswersData()

		controller.params["answerId"] = surveyQuestionInstance1.possibleAnswers[0].id
		controller.deletePossibleAnswerData()
		assert controller.response.json.success == true
		assert surveyQuestionInstance1.possibleAnswers.size() == 2
	}

	@Test
	void "Test updateSurveyAnswerData when no answerId is passed"() {
		controller.updateSurveyAnswerData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
				["Survey answer", null] as Object[], null)
	}

	@Test
	void "Test updateSurveyAnswerData when false answerId is passed"() {
		controller.params["answerId"] = 0;
		controller.updateSurveyAnswerData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
				["Survey answer", controller.params.answerId] as Object[], null)
	}

	@Test
	void "Test updateSurveyAnswerData when correct answerId is passed"() {
		controller.params["questionId"] = surveyQuestionInstance1.id
		controller.params["possibleAnswers"] = [["code": "acode1", "answer": "Answer no 1.", "priority": 4, "answerType": "MCQ"],
			["code": "acode3", "answer": "Answer no 3.", "priority": 5, "answerType": "MCQ"]]
		controller.createAnswersData()

		controller.response.reset()
		controller.params["answerId"] = surveyQuestionInstance1.possibleAnswers[0].id
		controller.params["code"] = "acode4"
		controller.params["answer"] = "Answer no 4?"
		controller.params["priority"] = 4
		controller.params["answerType"] = "MCQ"
		controller.updateSurveyAnswerData()
		assert controller.response.json.success == true
		assert surveyQuestionInstance1.possibleAnswers[0].code.equals("acode4")
	}

	@Test
	void "Test deleteSurveyQuestionData when correct questionId is passed"() {
		controller.params.id = surveyQuestionInstance1.id
		controller.deleteSurveyQuestionData()

		assert controller.response.json.success == true
		assert !SurveyQuestion.get(surveyQuestionInstance1.id)
	}

	@Test
	void "Test deleteSurveyQuestionData when no questionId is passed"() {
		controller.deleteSurveyQuestionData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
				["Survey question", controller.params.id] as Object[], null)
	}

	@Test
	void "Test deleteSurveyQuestionData when false questionId is passed"() {
		controller.params.id = 0
		controller.deleteSurveyQuestionData()
		assert controller.response.json.success == false
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
				["Survey question", controller.params.id] as Object[], null)
	}
}

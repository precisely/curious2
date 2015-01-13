package us.wearecurio.model

import java.util.SortedSet;

import us.wearecurio.model.SurveyAnswerType
import us.wearecurio.utility.Utils

class SurveyQuestion {

	String code
	String question
	List<SurveyAnswer> possibleAnswers
	Integer priority
	String validator
	QuestionStatus status
	
	static hasMany = [
		possibleAnswers: SurveyAnswer
	]

	static constraints = {
		code blank: false, size: 1..40, unique: true
		question blank: false, size: 5..1000
		validator nullable: true 
		possibleAnswers blank: true, validator: {possibleAnswers->
			boolean argumentsValid = true
			possibleAnswers.any({ possibleAnswer ->
				if (possibleAnswer.answerType.value() == possibleAnswers[0]?.answerType.value()) {
					return
				} else {
					argumentsValid = false
					return true
				}
			})
			return argumentsValid
		}
	}

	static mapping = {
		possibleAnswers cascade: 'all-delete-orphan'
	}

	static SurveyQuestion create(Map params) {
		SurveyQuestion surveyQuestion = new SurveyQuestion(params)
		surveyQuestion.validate()

		if (surveyQuestion.hasErrors()) {
			return null
		} else {
			Utils.save(surveyQuestion, true)
			return surveyQuestion
		}
	}
}

enum QuestionStatus {
	ACTIVE(1),
	INACTIVE(0)
	
	final int id
	QuestionStatus(int id) {
		this.id = id
	}
}
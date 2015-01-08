package us.wearecurio.model

import java.util.SortedSet;
import us.wearecurio.model.SurveyAnswerType

class SurveyQuestion {

	String code
	String question
	SortedSet<SurveyAnswer> possibleAnswers
	Integer priority
	QuestionStatus status
	
	static hasMany = [
		possibleAnswers: SurveyAnswer
	]

	static constraints = {
		code blank: false, size: 5..40, unique: true
		question blank: false, size: 5..1000
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
		sort priority: "desc"
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
package us.wearecurio.model.survey

import groovy.transform.EqualsAndHashCode
import us.wearecurio.utility.Utils

/**
 * A domain that holds questions for Survey. It contains set of answers(represented by Answer domain).
 * The status field determines whether this Question is currently active or not.
 * AnswerType field is used to determine whether the answer is descriptive, single choice or multiple choice.
 */
@EqualsAndHashCode
class Question {

	String question
	Set<PossibleAnswer> answers = []
	Integer priority
	QuestionStatus status

	AnswerType answerType

	boolean isRequired

	static belongsTo = [
		survey: Survey
	]

	static hasMany = [
		answers: PossibleAnswer
	]

	static constraints = {
		question blank: false, size: 5..1000
	}

	static mapping = {
		version false
		sort 'priority'
		answers cascade: 'all-delete-orphan', sort: 'priority'
	}

	void addOrUpdateAnswers(List possibleAnswersList) {
		if (this.answerType == AnswerType.DESCRIPTIVE) {
			return
		}

		possibleAnswersList.each { Map args ->
			PossibleAnswer possibleAnswerInstance

			if (args.answerId) {
				possibleAnswerInstance = PossibleAnswer.get(args.answerId)
				possibleAnswerInstance.update(args)
			} else {
				possibleAnswerInstance = PossibleAnswer.create(args)
				this.addToAnswers(possibleAnswerInstance)
			}
		}
	}

	String toString() {
		return "Question(id: ${id}, status: ${status}, question: ${question})"
	}
}

enum QuestionStatus {
	INACTIVE(0, 'Inactive'),
	ACTIVE(1, 'Active')

	final int id
	final String displayText

	QuestionStatus(int id, String displayText) {
		this.id = id
		this.displayText = displayText
	}
}

enum AnswerType {
	DESCRIPTIVE(0, 'Descriptive'),

	MCQ_RADIO((MCQ | 4), 'Single Choice'), // (1 | 4)  = 5
	MCQ_CHECKBOX((MCQ | 8), 'Multiple Choice')  // (1 | 8) = 9

	static final int MCQ = 1

	final int id
	final String displayText

	AnswerType(int id, String displayText) {
		this.id = id
		this.displayText = displayText
	}

	public int value() {
		return id
	}

	boolean isMCQType() {
		return ((this.id & MCQ) == MCQ)
	}
}
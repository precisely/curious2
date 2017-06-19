package us.wearecurio.model.survey

import groovy.transform.EqualsAndHashCode

/**
 * A class representing model for Survey. The code and title are unique fields used for representing a Survey.
 * A survey contains a set of questions (represented by Question domain).
 * The status field determines whether this survey is currently active or not.
 */
@EqualsAndHashCode
class Survey {

	String code
	String title

	SurveyStatus status

	Set<Question> questions = []

	static hasMany = [
		questions: Question
	]

	static mapping = {
		version false
		questions cascade: 'all-delete-orphan', sort: 'priority'
	}

	static constraints = {
		code unique: true, blank: false
		title unique: true, blank: false
	}

	String toString() {
		return "Survey(id: ${id}, code: ${code})"
	}

	/**
	 * Get JSON description for surveyInstance
	 */
	Map getJSONDesc(QuestionStatus questionStatus) {
		Map returnVal = [
				id: this.id,
				code: this.code,
				status: this.status,
				title: this.title
		]

		returnVal.questions = questionStatus ? getQuestions(questionStatus) : this.questions

		return returnVal
	}

	Set getQuestions(QuestionStatus questionStatus) {
		return this.questions.findAll { it.status == questionStatus }
	}
}

enum SurveyStatus {
	INACTIVE(0, 'Inactive'),
	ACTIVE(1, 'Active')

	final int id
	final String displayText

	SurveyStatus(int id, String displayText) {
		this.id = id
		this.displayText = displayText
	}
}

package us.wearecurio.model.survey

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString(includes = ['id', 'code', 'title'], includePackage = false)
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

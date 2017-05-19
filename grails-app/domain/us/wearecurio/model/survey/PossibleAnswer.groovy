package us.wearecurio.model.survey

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import us.wearecurio.model.Tag

@EqualsAndHashCode
@ToString(includes = ['id'], includePackage = false)
class PossibleAnswer {

	String answer
	Integer priority

	/*
	 * Associated tag with this answer. When user selects this answer for a question, this tag gets added to User's
	 * public interest tags.
	 */
	Tag tag

	static belongsTo = [
		question: Question
	]

	static constraints = {
		answer(blank: false, size: 1..1000)
		tag nullable: true
	}

	static mapping = {
		version false
		sort 'priority'
	}

	static PossibleAnswer create(Map args) {
		PossibleAnswer answer = new PossibleAnswer(args)
		answer.tag = getTag(args)

		return answer
	}

	static Tag getTag(Map args) {
		Tag tag

		if (args.tagDescription) {
			tag = Tag.look(args.tagDescription.trim())
		}

		return tag
	}

	PossibleAnswer update(Map args) {
		this.properties = args
		this.tag = getTag(args)

		return this
	}
}

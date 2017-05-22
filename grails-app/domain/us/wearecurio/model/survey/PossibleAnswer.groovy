package us.wearecurio.model.survey

import groovy.transform.EqualsAndHashCode
import us.wearecurio.model.Tag

/**
 * A domain that holds possible answers for a Question.
 * It also contains a tag field which is the associated tag with this answer. As such, when a answer is selected
 * by any User, its associated tag is added to User's public interest tag.
 */
@EqualsAndHashCode
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

	String toString() {
		return "PossibleAnswer(id: ${id}, answer: ${answer}, tag: ${tag.description})"
	}
}

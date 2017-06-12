package us.wearecurio.model.survey

import groovy.transform.EqualsAndHashCode
import us.wearecurio.model.Tag
import us.wearecurio.utility.Utils

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
	 * Associated profile tags and associated tracking tags with this answer.
	 * When user selects this answer for a question, associatedProfileTags create ProfileTags and associatedTreckingTag
	 * creates TagStats for the user.
	 */
	Set<Tag> associatedProfileTags = []
	Set<Tag> associatedTrackingTags = []

	static belongsTo = [
		question: Question
	]

	static hasMany = [
			associatedProfileTags: Tag,
			associatedTrackingTags: Tag
	]

	static constraints = {
		answer(blank: false, size: 1..1000)
	}

	static mapping = {
		version false
		sort 'priority'
	}

	static PossibleAnswer create(Map args) {
		PossibleAnswer answer = new PossibleAnswer(answer: args.answer, priority: args.priority?.toInteger())
		getAssociatedTags(args.profileTags).each { Tag tag ->
			answer.addToAssociatedProfileTags(tag)
		}
		getAssociatedTags(args.trackingTags).each { Tag tag ->
			answer.addToAssociatedTrackingTags(tag)
		}
		Utils.save(answer)

		return answer
	}

	static getAssociatedTags(List tagDescriptionList) {
		List<Tag> tagsList = []
		tagDescriptionList.each { String tagDescription ->
			tagsList.add(Tag.look(tagDescription.trim())) // Creates a new Tag for the given description if one does not exist.
		}

		return tagsList
	}

	PossibleAnswer update(Map args) {
		this.answer = args.answer
		this.priority = args.priority?.toInteger()
		getAssociatedTags(args.profileTags).each { Tag tag ->
			this.addToAssociatedProfileTags(tag)
		}
		getAssociatedTags(args.trackingTags).each { Tag tag ->
			this.addToAssociatedTrackingTags(tag)
		}
		Utils.save(this)

		return this
	}

	String toString() {
		return "PossibleAnswer(id: ${id}, answer: ${answer})"
	}
}

package us.wearecurio.model

import us.wearecurio.utility.Utils

class TagExclusion {

	static constraints = {
	}

	static mapping = {
		version false
	}

	Long objectId

	ExclusionType type

	Long tagGroupPropertyId

	static TagExclusion createOrLookup(def tagOrTagGroupToExclude, GenericTagGroupProperties tagGroupProperty) {
		ExclusionType type = ExclusionType.TAG
		Long objectId = tagOrTagGroupToExclude.id

		if (tagOrTagGroupToExclude instanceof GenericTagGroup) {
			type = ExclusionType.TAG_GROUP
		}

		def exclusion = TagExclusion.findOrCreateByObjectIdAndTypeAndTagGroupPropertyId(objectId, type, tagGroupProperty.id)

		if (Utils.save(exclusion, true))
			return exclusion

		return null
	}
}

enum ExclusionType {
	TAG(1),
	TAG_GROUP(2)

	final int id
	ExclusionType(int id) {
		this.id = id
	}
}
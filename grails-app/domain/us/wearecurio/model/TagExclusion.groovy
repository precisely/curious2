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

	static TagExclusion lookup(Long tagGroupPropertyId, ExclusionType type, Long objectId) {
		TagExclusion.withCriteria(uniqueResult: true) {
			eq("type", type)
			eq("objectId", objectId)
			eq("tagGroupPropertyId", tagGroupPropertyId)
		}
	}

	@Deprecated
	static TagExclusion createOrLookup(def tagOrTagGroupToExclude, GenericTagGroupProperties tagGroupProperty) {
		addToExclusion(tagOrTagGroupToExclude, tagGroupProperty)
	}

	static TagExclusion addToExclusion(def tagOrTagGroupToExclude, GenericTagGroupProperties tagGroupProperty) {
		ExclusionType type = ExclusionType.TAG
		Long objectId = tagOrTagGroupToExclude.id

		if (tagOrTagGroupToExclude instanceof GenericTagGroup) {
			type = ExclusionType.TAG_GROUP
		}

		def exclusion = TagExclusion.findOrCreateByObjectIdAndTypeAndTagGroupPropertyId(objectId, type, tagGroupProperty.id)

		if (Utils.save(exclusion, true)) {
			if (tagOrTagGroupToExclude instanceof GenericTagGroup) {
				tagGroupProperty.getTagGroup().removeFromCache(tagOrTagGroupToExclude, tagGroupProperty.userId)
			} else {
				tagGroupProperty.getTagGroup().removeFromCache(tagOrTagGroupToExclude)
			}
			return exclusion
		}

		return null
	}

	static TagExclusion removeFromExclusion(def tagOrTagGroupToAddBack, GenericTagGroupProperties tagGroupProperty) {
		ExclusionType type = ExclusionType.TAG
		Long objectId = tagOrTagGroupToAddBack.id

		if (tagOrTagGroupToAddBack instanceof GenericTagGroup) {
			type = ExclusionType.TAG_GROUP
		}

		TagExclusion exclusion = TagExclusion.lookup(tagGroupProperty.id, type, objectId)

		if (!exclusion) {
			return null
		}
		exclusion.delete(flush: true)

		if (tagOrTagGroupToAddBack instanceof GenericTagGroup) {
			tagGroupProperty.getTagGroup().addToCache(tagOrTagGroupToAddBack)
		} else {
			tagGroupProperty.getTagGroup().addToCache(tagOrTagGroupToAddBack)
		}

		return exclusion
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
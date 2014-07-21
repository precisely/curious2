package us.wearecurio.model

class TagGroup extends GenericTagGroup {

	static hasMany = [tags: Tag]

	static mapping = {
		version false
	}

	List<Tag> getTags(Long userId) {
		List tagList = this.tags as List
		List tagGroupList = this.subTagGroups as List

		GenericTagGroupProperties property = GenericTagGroupProperties.lookup(userId, id)

		if (property) {
			List<TagExclusion> exclusions = TagExclusion.findAllByTagGroupPropertyId(property.id)*.properties
			List temporaryTagList = tagList
			List temporaryTagGroupList = tagGroupList
			tagList = []
			tagGroupList = []

			// Iterate each tag & see if it is excluded
			temporaryTagList.each { Tag tagInstance ->
				// See if item has not been excluded by current user for current tag group
				if (!exclusions.find { it.type == ExclusionType.TAG && it.objectId == tagInstance.id}) {
					tagList << tagInstance
				}
			}

			// Iterate each tag group & see if it is excluded
			temporaryTagGroupList.each { tagGroupInstance ->
				// See if item has not been excluded by current user for current tag group
				if (!exclusions.find { it.objectId == tagGroupInstance.id && it.type == ExclusionType.TAG_GROUP}) {
					tagGroupList << tagGroupInstance
				}
			}
		}

		tagGroupList.each { TagGroup tagGroupInstance ->
			tagList.addAll(tagGroupInstance.getTags(userId))
		}

		return tagList
	}

	boolean containsTag(Tag tag) {

	}

	boolean containsTagString(String tagString) {

	}
}
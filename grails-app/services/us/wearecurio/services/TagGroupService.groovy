package us.wearecurio.services

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import us.wearecurio.model.GenericTagGroup
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.SharedTagGroup
import us.wearecurio.model.Tag
import us.wearecurio.model.TagGroup
import us.wearecurio.model.UserGroup

class TagGroupService {

	private static Log log = LogFactory.getLog(this)

	static transactional = true

	SharedTagGroup createOrLookupSharedTagGroup(String tagGroupName, Long groupId, Map args = [:]) {
		SharedTagGroup tagGroupInstance = SharedTagGroup.lookupWithinGroup(tagGroupName, groupId)

		if (!tagGroupInstance) {
			tagGroupInstance = createTagGroup(tagGroupName, null, groupId, SharedTagGroup.class, args)
		}

		GenericTagGroupProperties.createOrLookup(groupId, "Group", tagGroupInstance)
		return tagGroupInstance
	}

	def createOrLookupTagGroup(String tagGroupName, Long userId, Long groupId, Class type = TagGroup.class, Map args = [:]) {
		def tagGroupInstance = type.findByDescription(tagGroupName)

		if (!tagGroupInstance) {
			tagGroupInstance = this.createTagGroup(tagGroupName, type, args)
		}

		String propertyFor = userId ? "User" : "Group"
		GenericTagGroupProperties.createOrLookup(userId ?: groupId, propertyFor, tagGroupInstance)

		return tagGroupInstance
	}

	def createTagGroup(String tagGroupName, Long userId, Long groupId, Class type = TagGroup.class, Map args = [:]) {
		def tagGroupInstance = createTagGroup(tagGroupName, type, args)

		String propertyFor = userId ? "User" : "Group"
		GenericTagGroupProperties.createOrLookup(userId ?: groupId, propertyFor, tagGroupInstance)

		return tagGroupInstance
	}

	GenericTagGroup createTagGroup(String tagGroupName, Class type = TagGroup.class, Map args = [:]) {
		def tagGroupInstance = type.newInstance()
		tagGroupInstance.description = tagGroupName
		tagGroupInstance.save()
		return tagGroupInstance
	}

	TagGroup addTag(TagGroup tagGroupInstance, Tag tagInstance) {
		if (!tagGroupInstance) log.error "No tagGroupInstance found";
		if (!tagInstance) log.error "No tag instance found"

		List existingTagIds = tagGroupInstance.tags*.id ?: []
		if (!existingTagIds.contains(tagInstance.id)) {
			log.debug "Adding [$tagInstance] to TagGroup [$tagGroupInstance]"
			tagGroupInstance.addToTags(tagInstance)
			tagGroupInstance.addToCache(tagInstance)
		}
		return tagGroupInstance
	}

	/**
	 * Add tags to tag group.
	 * @param tagGroupInstance
	 * @param tagIds must be comma separated ids.
	 * @return
	 */
	def addTags(TagGroup tagGroupInstance, String tagIds) {
		tagIds.tokenize(",").each { tagId ->
			addTag(tagGroupInstance, Tag.get(tagId.trim()))
		}
		tagGroupInstance.save()
		tagGroupInstance
	}

	def addTags(Long tagGroupId, Long tagId) {
		addTag(TagGroup.get(tagGroupId), Tag.get(tagId))
	}
}
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

	SharedTagGroup createOrLookupSharedTagGroup(String tagGroupName, Long userId, Long groupId, Map args = [:]) {
		SharedTagGroup tagGroupInstance

		List tagGroupInstanceList = lookupSharedTagGroupWithinGroup(tagGroupName, groupId)
		if (!tagGroupInstanceList) {
			tagGroupInstance = createTagGroup(tagGroupName, userId, groupId, SharedTagGroup.class, args)
		} else {
			tagGroupInstance = tagGroupInstanceList[0]
		}

		String propertyFor = userId ? "User" : "Group"
		GenericTagGroupProperties.createOrLookup(userId ?: groupId, propertyFor, tagGroupInstance)
		return tagGroupInstance
	}

	/**
	 * Return list of SharedTagGroup instances matching the name within a given UserGroup.
	 * @param tagGroupName Name of the SharedTagGroup to search
	 * @param groupId Identity of the UserGroup where SharedTagGroup needs to be searched.
	 * @return
	 */
	List lookupSharedTagGroupWithinGroup(String tagGroupName, Long groupId) {
		GenericTagGroup.executeQuery("""SELECT tg FROM SharedTagGroup AS tg, GenericTagGroupProperties AS tgp
				WHERE tgp.tagGroupId = tg.id AND tg.description = :description
				AND tgp.groupId IS NOT NULL AND tgp.groupId = :groupId""",
				[description: tagGroupName, groupId: groupId])
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
			tagGroupInstance.addTagToCache(tagInstance)
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
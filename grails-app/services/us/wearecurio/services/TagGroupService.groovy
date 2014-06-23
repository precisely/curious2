package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import org.springframework.transaction.annotation.Transactional

import us.wearecurio.model.GenericTagGroup;
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.Tag
import us.wearecurio.model.TagGroup

class TagGroupService {

	private static def log = LogFactory.getLog(this)

	static transactional = true

	def createOrLookupTagGroup(String tagGroupName, Long userId, Long groupId, Class type = TagGroup.class, Map args = [:]) {
		def tagGroupInstance = type.findByDescription(tagGroupName)
		if (!tagGroupInstance) {
			tagGroupInstance = this.createTagGroup(tagGroupName, type)
		}

		tagGroupInstance.name = args["name"] ?: ""
		tagGroupInstance.save()

		if ((userId || groupId) && !tagGroupInstance.hasErrors()) {
			//def tagGroupPropertyInstance = GenericTagGroupProperties.findOrSaveByTagGroupIdAndUserId(tagGroupInstance.id, userId)
			log.debug "Looking up tagGroupInstance"
			def tagGroupPropertyInstance = GenericTagGroupProperties.createOrLookup(userId, groupId, tagGroupInstance.id)
		}

		return tagGroupInstance
	}
	
	def createTagGroup(String tagGroupName, Long userId, Long groupId, Class type = TagGroup.class) {
		def tagGroupInstance = this.createTagGroup(tagGroupName, type)
		
		if ((userId || groupId) && !tagGroupInstance.hasErrors()) {
			//def tagGroupPropertyInstance = GenericTagGroupProperties.findOrSaveByTagGroupIdAndUserId(tagGroupInstance.id, userId)
			log.debug "Looking up tagGroupInstance"
			def tagGroupPropertyInstance = GenericTagGroupProperties.createOrLookup(userId, groupId, tagGroupInstance.id)
		}

		return tagGroupInstance
	}
	
	GenericTagGroup createTagGroup(String tagGroupName, Class type = TagGroup.class) {
		def tagGroupInstance = type.newInstance()
		tagGroupInstance.description = tagGroupName
		tagGroupInstance.save()
		return tagGroupInstance
	}

	TagGroup addTag(TagGroup tagGroupInstance, Tag tagInstance) {
		if(!tagGroupInstance) log.error "No tagGroupInstance found";
		if(!tagInstance) log.error "No tag instance found"

		List existingTagIds = tagGroupInstance.tags*.id ?: []
		if(!existingTagIds.contains(tagInstance.id)) {
			log.debug "Adding [$tagInstance] to TagGroup [$tagGroupInstance]"
			tagGroupInstance.addToTags(tagInstance)
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
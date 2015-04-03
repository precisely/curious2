package us.wearecurio.controller

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.ExclusionType
import us.wearecurio.model.GenericTagGroup
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.SharedTagGroup
import us.wearecurio.model.Tag
import us.wearecurio.model.TagExclusion
import us.wearecurio.model.TagGroup
import us.wearecurio.model.TagProperties
import us.wearecurio.model.User
import us.wearecurio.model.WildcardTagGroup
import us.wearecurio.services.TagService

class TagController extends LoginController {

	private static def log = LogFactory.getLog(this)

	TagService tagService
	def tagGroupService

	static debug(str) {
		log.debug(str)
	}

	def wildcardData() {
		log.debug("Fetching tags for a WildCardTagGroup for graph: ${params.description}")
		def tagList = tagService.getTagsByDescription(session.userId, params.description)
		println(tagList.dump())
		renderJSONGet(tagList)
	}

	def listTagsAndTagGroupsData() {
		log.debug("Fetching tags and tag groups: " + params)
		def tagAndTagGroupList = tagService.getTagsByUser(session.userId);
		tagAndTagGroupList.addAll(tagService.getAllTagGroupsForUser(session.userId))
		tagAndTagGroupList.sort { a,b ->
			return a.description.compareTo(b.description)
		}
		log.debug("Returning tag list")
		renderJSONGet(tagAndTagGroupList)
	}

	def createTagGroupData() {
		log.debug("Creating tag group: " + params)
		def tagGroupInstance = tagGroupService.createTagGroup(params.tagGroupName, session.userId, null)
		if(params.tagIds) {
			tagGroupService.addTags(tagGroupInstance, params.tagIds)
		}
		renderJSONGet(tagGroupInstance)
	}

	def addWildcardTagGroupData() {
		log.debug("Adding wildcard tag group: " + params)
		def wildcardTagGroupInstance = tagGroupService.createOrLookupTagGroup(params.description, session.userId, null, WildcardTagGroup.class)
		renderJSONGet(wildcardTagGroupInstance)
	}

	def addTagToTagGroupData() {
		log.debug("Adding tag to tag group: " + params)

		TagGroup tagGroupInstance = TagGroup.get(params.tagGroupId)
		if (tagGroupInstance instanceof SharedTagGroup) {
			if (!tagService.canEdit(tagGroupInstance, session.userId)) {
				log.warn "User can not add tag."
				renderStringGet("fail")
				return
			}
		}

		tagGroupService.addTags(params.tagGroupId.toLong(), Long.parseLong(params.id))
		renderStringGet("success")
	}

	def addBackToTagGroupData(Long id, Long itemId, String type) {
		log.debug "Add back item $params"

		GenericTagGroupProperties prop = GenericTagGroupProperties.lookup(session.userId, id)

		log.debug "Removing [$itemId] of type [$type] for propertyId ${prop?.id}"

		def tagOrTagGroupToAddBack
		if (ExclusionType[type] == ExclusionType.TAG) {
			tagOrTagGroupToAddBack = Tag.get(itemId)
		} else {
			tagOrTagGroupToAddBack = GenericTagGroup.get(itemId)
		}

		TagExclusion.removeFromExclusion(tagOrTagGroupToAddBack, prop)

		renderJSONGet([success: true])
	}

	def addTagGroupToTagGroupData() {
		log.debug("Adding tag group to tag group: " + params)
		def parentTagGroupInstance = TagGroup.get(params.parentTagGroupId)
		def childTagGroupInstance = GenericTagGroup.get(params.childTagGroupId)

		if (parentTagGroupInstance instanceof SharedTagGroup) {
			if (!tagService.canEdit(parentTagGroupInstance, session.userId)) {
				log.warn "User can not add."
				renderStringGet("fail")
				return
			}
		}

		parentTagGroupInstance.addToSubTagGroups(childTagGroupInstance)
		parentTagGroupInstance.addToCache(childTagGroupInstance, session.userId)
		parentTagGroupInstance.save()
		renderJSONGet(["dummy"])
	}

	def showTagGroupData(Long id) {
		log.debug("Showing tag group: " + params)
		TagGroup tagGroupInstance = TagGroup.get(id)

		List tagAndTagGroupList = []
		tagAndTagGroupList.addAll(tagGroupInstance.tags)
		tagAndTagGroupList.addAll(tagGroupInstance.subTagGroups)
		tagAndTagGroupList.sort { it.description }

		GenericTagGroupProperties property = GenericTagGroupProperties.lookup(session.userId, id)
		if (property) {
			List<TagExclusion> exclusions = TagExclusion.findAllByTagGroupPropertyId(property.id)*.properties
			List temporaryList = tagAndTagGroupList
			tagAndTagGroupList = []

			// Iterate each tag & tag group item
			temporaryList.each { item ->
				ExclusionType type = item instanceof Tag ? ExclusionType.TAG : ExclusionType.TAG_GROUP

				// See if item has not been excluded by current user for current tag group
				if (!exclusions.find { it.objectId == item.id && it.type == type}) {
					tagAndTagGroupList << item
				}
			}
		}

		renderJSONGet(tagAndTagGroupList)
	}

	def deleteTagGroupData() {
		log.debug("Deleting tag group: " + params)
		def tagGroupInstance = GenericTagGroup.get(params.id)
		if(tagGroupInstance instanceof TagGroup && tagGroupInstance.tags) {
			tagGroupInstance.tags.clear()
		}
		List tagGroupPropertiesList = GenericTagGroupProperties.findAllByUserIdAndTagGroupId(session.userId, tagGroupInstance.id)
		tagGroupPropertiesList*.delete(flush: true)
		renderJSONGet([success: true])
	}

	def removeTagGroupFromTagGroupData() {
		log.debug("Removing tag group from tag group: " + params)
		GenericTagGroup tagGroupInstance = GenericTagGroup.get(params.id)
		GenericTagGroup parentTagGroupInstance = tagGroupInstance.parentTagGroup

		if (parentTagGroupInstance instanceof SharedTagGroup) {
			if (!tagService.canEdit(parentTagGroupInstance, session.userId)) {
				log.warn "User can not remove tag group."
				renderStringGet("fail")
				return
			}
		}

		parentTagGroupInstance.removeFromCache(tagGroupInstance, session.userId)
		tagGroupInstance.parentTagGroup = null
		tagGroupInstance.save(flush: true)
		renderJSONGet([success: true])
	}

	def removeTagFromTagGroupData() {
		log.debug("Removing tag from tag group: " + params)
		def tagGroupInstance = TagGroup.get(params.tagGroupId)

		if (tagGroupInstance instanceof SharedTagGroup) {
			if (!tagService.canEdit(tagGroupInstance, session.userId)) {
				log.warn "User can not remove tag."
				renderStringGet("fail")
				return
			}
		}

		def tagInstance = Tag.get(params.id)
		tagGroupInstance.removeFromTags(tagInstance)
		tagGroupInstance.removeFromCache(tagInstance)
		tagGroupInstance.save(flush: true)
		renderJSONGet([success: true])
	}

	def excludeFromTagGroupData(Long tagGroupId, Long id, String exclusionType) {
		log.debug("Exclude from group: " + params)

		GenericTagGroup tagGroupInstance = GenericTagGroup.get(tagGroupId)
		def itemToExclude

		if (exclusionType == "Tag") {
			itemToExclude = Tag.get(id)
		} else {
			itemToExclude = GenericTagGroup.get(id)
		}

		def tagGroupProperty = GenericTagGroupProperties.createOrLookup(session.userId, "User", tagGroupId)

		// There can not be an exclusion from a general tag group.
		if (tagGroupInstance instanceof TagGroup && !(tagGroupInstance instanceof SharedTagGroup)) {
			log.debug "No exclusion operation allowed from $tagGroupInstance"
			renderJSONGet([success: true])
			return false
		}

		TagExclusion.createOrLookup(itemToExclude, tagGroupProperty)

		renderJSONGet([success: true])
	}

	def updateData() {
		log.debug("Updating tag group: " + params)
		if(params.type.equals("tagGroup")) {
			println params.dump()
			GenericTagGroup tagGroupInstance = GenericTagGroup.get(params.id)
			tagGroupInstance.description = params.description
			tagGroupInstance.save()
		}
		renderJSONGet([success: true])
	}

	def getTagPropertiesData() {
		log.debug("Getting tag properties: " + params)
		User user = sessionUser()

		if (user == null) {
			debug "auth failure"
			return
		}

		renderJSONGet(TagProperties.lookupJSONDesc(user.id, Long.valueOf(params.id)))
	}

}
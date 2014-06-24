package us.wearecurio.controller

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.GenericTagGroup
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.TagProperties;
import us.wearecurio.model.User
import us.wearecurio.model.WildcardTagGroup;
import us.wearecurio.model.Tag
import us.wearecurio.model.Entry
import us.wearecurio.model.TagGroup
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
		tagGroupService.addTags(params.tagGroupId.toLong(), Long.parseLong(params.id))
		renderStringGet("success")
	}
	
	def addTagGroupToTagGroupData() {
		log.debug("Adding tag group to tag group: " + params)
		def parentTagGroupInstance = TagGroup.get(params.parentTagGroupId)
		def childTagGroupInstance = GenericTagGroup.get(params.childTagGroupId)
		parentTagGroupInstance.addToSubTagGroups(childTagGroupInstance)
		parentTagGroupInstance.save()
		renderJSONGet(["dummy"])
	}
	
	def showTagGroupData(Long id) {
		log.debug("Showing tag group: " + params)
		def tagGroupInstance = TagGroup.get(params.id.toLong())
		def tagAndTagGroupList = []
		tagAndTagGroupList.addAll(tagGroupInstance.tags)
		tagAndTagGroupList.addAll(tagGroupInstance.subTagGroups)
		tagAndTagGroupList.sort { a,b ->
			return a.description.compareTo(b.description)
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
		tagGroupPropertiesList*.delete()
		renderJSONGet([success: true])
	}
	
	def removeTagGroupFromTagGroupData() {
		log.debug("Removing tag group from tag group: " + params)
		GenericTagGroup tagGroupInstance = GenericTagGroup.get(params.id)
		tagGroupInstance.parentTagGroup = null;
		tagGroupInstance.save()
		renderJSONGet([success: true])
	}

	def removeTagFromTagGroupData() {
		log.debug("Removing tag from tag group: " + params)
		def tagGroupInstance = TagGroup.get(params.tagGroupId)
		def tagInstance = Tag.get(params.id)
		tagGroupInstance.removeFromTags(tagInstance)
		tagGroupInstance.save()
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
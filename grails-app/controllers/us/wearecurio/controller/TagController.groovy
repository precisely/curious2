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
	
	def wildcard = {
		log.debug("Fetching tags for a WildCardTagGroup for graph: ${params.description}")
		def tagList = tagService.getTagsByDescription(session.userId, params.description)
		println(tagList.dump())
		renderJSONGet(tagList)
	}

	def listTagsAndTagGroups = {
		def tagAndTagGroupList = tagService.getTagsByUser(session.userId);
		tagAndTagGroupList.addAll(tagService.getTagGroupsByUser(session.userId))
		tagAndTagGroupList.sort { a,b ->
			return a.description.compareTo(b.description)
		}
		renderJSONGet(tagAndTagGroupList)
	}
	
	def createTagGroup = {
		def tagGroupInstance = tagGroupService.createTagGroup(params.tagGroupName, session.userId)
		if(params.tagIds) {
			tagGroupService.addTags(tagGroupInstance, params.tagIds)
		}
		renderJSONGet(tagGroupInstance)
	}
	
	def addWildcardTagGroup = {
		def wildcardTagGroupInstance = tagGroupService.createOrLookupTagGroup(params.description, session.userId, WildcardTagGroup.class)
		renderJSONGet(wildcardTagGroupInstance)
	}

	def addTagToTagGroup = {
		tagGroupService.addTags(params.tagGroupId.toLong(), Long.parseLong(params.id))
		renderStringGet("success")
	}
	
	def addTagGroupToTagGroup = {
		def parentTagGroupInstance = TagGroup.get(params.parentTagGroupId)
		def childTagGroupInstance = GenericTagGroup.get(params.childTagGroupId)
		parentTagGroupInstance.addToSubTagGroups(childTagGroupInstance)
		parentTagGroupInstance.save()
		renderJSONGet(["dummy"])
	}
	
	def showTagGroup = {
		def tagGroupInstance = TagGroup.get(params.id.toLong())
		def tagAndTagGroupList = []
		tagAndTagGroupList.addAll(tagGroupInstance.tags)
		tagAndTagGroupList.addAll(tagGroupInstance.subTagGroups)
		tagAndTagGroupList.sort { a,b ->
			return a.description.compareTo(b.description)
		}
		renderJSONGet(tagAndTagGroupList)
	}
	
	def deleteTagGroup = {
		def tagGroupInstance = GenericTagGroup.get(params.id)
		if(tagGroupInstance instanceof TagGroup && tagGroupInstance.tags) {
			tagGroupInstance.tags.clear()
		}
		List tagGroupPropertiesList = GenericTagGroupProperties.findAllByUserIdAndTagGroupId(session.userId, tagGroupInstance.id)
		tagGroupPropertiesList*.delete()
		renderJSONGet([success: true])
	}
	
	def removeTagGroupFromTagGroup = {
		GenericTagGroup tagGroupInstance = GenericTagGroup.get(params.id)
		tagGroupInstance.parentTagGroup = null;
		tagGroupInstance.save()
		renderJSONGet([success: true])
	}

	def removeTagFromTagGroup = {
		def tagGroupInstance = TagGroup.get(params.tagGroupId)
		def tagInstance = Tag.get(params.id)
		tagGroupInstance.removeFromTags(tagInstance)
		tagGroupInstance.save()
		renderJSONGet([success: true])
	}
	
	def update = {
		
		if(params.type.equals("tagGroup")) {
			println params.dump()
			GenericTagGroup tagGroupInstance = GenericTagGroup.get(params.id)
			tagGroupInstance.description = params.description
			tagGroupInstance.save()
		}
		renderJSONGet([success: true])
	}
    
    def getTagProperties = {
		User user = sessionUser()
		
		if (user == null) {
			debug "auth failure"
			return
		}

		renderJSONGet(TagProperties.lookupJSONDesc(user.id, Long.valueOf(params.id)))
    }

}
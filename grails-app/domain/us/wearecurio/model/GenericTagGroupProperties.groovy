package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils

class GenericTagGroupProperties {
	
	private static def log = LogFactory.getLog(this)

	Long userId
	Long tagGroupId
	Boolean isContinuous
	Boolean showPoints
	
	static constraints = {
		isContinuous(nullable:true)
		showPoints(nullable:true)
	}
	static mapping = {
		table 'tag_group_properties'
		userId column:'user_id', index:'user_id_index'
		tagGroupId column:'tag_group_id', index:'tag_group_id_index'
	}

	
	static public def createOrLookup(long userId, long tagGroupId) {
		log.debug "GenericTagGroupProperties.createOrLookup() userId:" + userId + ", tag:" + Tag.get(tagGroupId)
		
		def props = GenericTagGroupProperties.findByTagGroupIdAndUserId(tagGroupId, userId)
		
		if (!props) {
			props = new GenericTagGroupProperties(tagGroupId:tagGroupId, userId:userId)
		}
		
		if (Utils.save(props, true))
			return props
		
		return null
	}
	
	static public def lookup(long userId, long tagGroupId) {
		return GenericTagGroupProperties.findByTagGroupIdAndUserId(tagGroupId, userId)
	}
			
	public String toString() {
		return "GenericTagGroupProperties(userId:" + userId + ", tagGroupId:" + tagGroupId + ", isContinuous:" \
				+ isContinuous + ", showPoints:" \
				+ showPoints + ")"
	}
}

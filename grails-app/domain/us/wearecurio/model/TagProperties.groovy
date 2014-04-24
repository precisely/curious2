package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils

class TagProperties {
	
	private static def log = LogFactory.getLog(this)

	Long userId
	Long tagId
	Boolean isContinuous
	Boolean showPoints
	
	static constraints = {
		isContinuous(nullable:true)
		showPoints(nullable:true)
	}
	
	static mapping = {
		version false
		table 'tag_properties'
		userId column:'user_id', index:'user_id_index'
		tagId column:'tag_id', index:'tag_id_index'
	}

	public TagProperties() {
	}
	
	static public def createOrLookup(long userId, long tagId) {
		log.debug "TagProperties.createOrLookup() userId:" + userId + ", tag:" + Tag.get(tagId)
		
		def props = TagProperties.findByTagIdAndUserId(tagId, userId)
		
		if (!props) {
			props = new TagProperties(tagId:tagId, userId:userId)
		}
		
		if (Utils.save(props, true))
			return props
		
		return null
	}
	
	static public def lookup(long userId, long tagId) {
		return TagProperties.findByTagIdAndUserId(tagId, userId)
	}
	
	static public def lookupJSONDesc(long userId, long tagId) {
		def props = TagProperties.findByTagIdAndUserId(tagId, userId)
		
		if (!props) { // return default settings, don't create new TagProperties
			return [userId:userId,
				tagId:tagId,
				isContinuous:false,
				showPoints:false]
		}
		
		return props.getJSONDesc()
	}
	
	// note: changes here should also be made to lookupJSONDesc()
	def getJSONDesc() {
		return [userId:userId,
			tagId:tagId,
			isContinuous:isContinuous,
			showPoints:showPoints]
	}
			
	public String toString() {
		return "TagProperties(userId:" + userId + ", tagId:" + tagId + ", isContinuous:" \
				+ isContinuous + ", showPoints:" \
				+ showPoints + ")"
	}
}

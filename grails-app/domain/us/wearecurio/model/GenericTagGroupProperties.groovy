package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils

class GenericTagGroupProperties {

	private static def log = LogFactory.getLog(this)

	Long userId
	Long groupId
	Long tagGroupId
	Boolean isContinuous
	Boolean showPoints

	// A custom validator to check if both userId & groupId are null
	static Closure userIdAndGroupIdValidator = { val, obj, errors ->
		if (!obj.groupId && !obj.userId) {
			errors.rejectValue("userId", "Both userId & groupId can not be null.")
			errors.rejectValue("groupId", "Both userId & groupId can not be null.")
		}
	}

	static constraints = {
		userId nullable: true, validator: userIdAndGroupIdValidator
		groupId nullable: true, validator: userIdAndGroupIdValidator
		isContinuous(nullable:true)
		showPoints(nullable:true)
	}

	static mapping = {
		version false
		table 'tag_group_properties'
		userId column:'user_id', index:'user_id_index'
		tagGroupId column:'tag_group_id', index:'tag_group_id_index'
	}

	static GenericTagGroupProperties createOrLookup(Long userOrGroupId, String propertyFor, GenericTagGroup tagGroupInstance) {
		if (tagGroupInstance.hasErrors() || !userOrGroupId) {
			log.warn "Can't create or lookup. $tagGroupInstance.errors, $userOrGroupId, for $propertyFor"
			return null
		}

		createOrLookup(userOrGroupId, propertyFor, tagGroupInstance.id)
	}

	static GenericTagGroupProperties createOrLookup(Long userOrGroupId, String propertyFor, Long tagGroupId) {
		log.debug "GenericTagGroupProperties.createOrLookup() userId: $userOrGroupId, for $propertyFor $userOrGroupId:" + Tag.get(tagGroupId)

		def props = GenericTagGroupProperties."findOrCreateByTagGroupIdAnd${propertyFor}"(tagGroupId, userOrGroupId)

		if (Utils.save(props, true))
			return props

		return null
	}

	static def lookup(long userId, long tagGroupId) {
		return GenericTagGroupProperties.findByTagGroupIdAndUserId(tagGroupId, userId)
	}

	UserGroup getGroup() {
		UserGroup.get(groupId)
	}

	String toString() {
		return "GenericTagGroupProperties(userId:" + userId + ", tagGroupId:" + tagGroupId + ", isContinuous:" \
				+ isContinuous + ", showPoints:" \
				+ showPoints + ")"
	}
}
package us.wearecurio.model

class SharedTagGroup extends TagGroup {

	static mapping = {
		version false
	}
	
	static constraints = {
	}

	/**
	 * Return a SharedTagGroup instance matching the name within a given UserGroup.
	 * @param tagGroupName Name of the SharedTagGroup to search
	 * @param groupId Identity of the UserGroup where SharedTagGroup needs to be searched.
	 * @return
	 */
	static SharedTagGroup lookupWithinGroup(String tagGroupName, Long groupId) {
		GenericTagGroup.executeQuery("""SELECT tg FROM SharedTagGroup AS tg, GenericTagGroupProperties AS tgp
				WHERE tgp.tagGroupId = tg.id AND tg.description = :description
				AND tgp.groupId IS NOT NULL AND tgp.groupId = :groupId""",
				[description: tagGroupName, groupId: groupId])[0]
	}

	UserGroup associatedGroup() {
		Long groupId = GenericTagGroupProperties.findByTagGroupIdAndGroupIdIsNotNull(id)?.groupId

		UserGroup.get(groupId)
	}

	void updateData(String description, Long groupId, Map args = [:]) {
		if (description) {
			this.description = description
			Utils.save(this, true)
		}
	}
	
	boolean hasEditor(Long userId) {
		// There can be one single SharedTagGroup per group
		GenericTagGroupProperties tagGroupProperty = GenericTagGroupProperties.findByTagGroupId(this.id)
		UserGroup userGroupInstance = tagGroupProperty?.getGroup()

		if (!userGroupInstance) {
			return true
		}

		if (UserGroup.hasAdmin(userGroupInstance.id, userId)) {
			return true
		}

		return !userGroupInstance.isReadOnly
	}
}

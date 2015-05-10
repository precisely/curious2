package us.wearecurio.model

import java.util.List;
import java.util.Map;

import us.wearecurio.cache.BoundedCache
import us.wearecurio.utility.Utils
import us.wearecurio.services.DatabaseService
import us.wearecurio.model.ExclusionType

class TagGroup extends GenericTagGroup {

	static hasMany = [tags: Tag]

	static mapping = {
		version false
	}

	// Cache holder to cache list of tag descriptions for a tag group
	static BoundedCache<Long, List<String>> tagDescriptionCache = new BoundedCache<Long, List<String>>(100000)
	// Cache holder to cache list of tag ids for a tag group
	static BoundedCache<Long, List<Long>> tagIdCache = new BoundedCache<Long, List<Long>>(100000)

	static {
		Utils.registerTestReset {
			tagDescriptionCache = new BoundedCache<Long, List<String>>(100000)
			tagIdCache = new BoundedCache<Long, List<Long>>(100000)
		}
	}

	void addToCache(GenericTagGroup childTagGroupInstance, Long userId) {
		if (hasCachedData()) {
			childTagGroupInstance.getTags(userId).each { tagInstance ->
				this.addToCache(tagInstance)
			}
		}
	}

	/*
	 * Method to add an instance of tag to the cache.
	 * This method first check if the cache of current tag group is empty or not.
	 * This method will only cache the tag instance if its cache is not empty.
	 * This check is needed to prevent returning incomplete data from getTags() method.
	 */
	void addToCache(Tag tagInstance) {
		if (hasCachedData()) {
			cache([tagInstance])
		}
	}

	/*
	 * A method to cache the list of tags for a given tag group.
	 */
	void cache(List<Tag> tagInstanceList) {
		Set cachedIds = tagIdCache[id] ?: []
		Set cachedDescriptions = tagDescriptionCache[id] ?: []

		tagInstanceList.each { tagInstance ->
			synchronized(tagDescriptionCache) {
				cachedIds << tagInstance.id
				tagIdCache.put(id, cachedIds)

				cachedDescriptions << tagInstance.description
				tagDescriptionCache.put(id, cachedDescriptions)
			}
		}
	}


	boolean containsTag(Tag tag, Long userId = null) {
		if (!tag) {
			return false
		}
		tag.id in getTagsIds(userId)
	}

	boolean containsTagString(String tagString, Long userId = null) {
		tagString in getTagsDescriptions(userId)
	}

	/*
	 * Get list of all tag instances associated with this tag group instance.
	 * This will get all nested tags which are sub tags of any sub tag group.
	 * 
	 * Passing user id will exclude the tags & tag groups for the current tag group.
	 */
	List<Tag> getTags(Long userId) {
		if (tagIdCache[this.id]) {
			return Tag.fetchAll(tagIdCache[this.id])
		}

		List subTagList = this.tags as List
		List subTagGroupList = this.subTagGroups as List

		List filteredSubTagList = [], filteredSubTagGroupList = []

		GenericTagGroupProperties property = GenericTagGroupProperties.lookup(userId, id)

		// If there is property there may be the exclusion
		if (property) {
			List<TagExclusion> exclusions = TagExclusion.findAllByTagGroupPropertyId(property.id)*.properties

			// Iterate each tag & see if it is excluded
			subTagList.each { Tag tagInstance ->
				// See if sub tag has not been excluded by current user for this tag group
				if (!exclusions.find { it.type == ExclusionType.TAG && it.objectId == tagInstance.id}) {
					filteredSubTagList << tagInstance
				}
			}

			// Iterate each tag group & see if it is excluded
			subTagGroupList.each { tagGroupInstance ->
				// See if sub tag group has not been excluded by current user for this tag group
				if (!exclusions.find { it.type == ExclusionType.TAG_GROUP && it.objectId == tagGroupInstance.id}) {
					filteredSubTagGroupList << tagGroupInstance
				}
			}
		} else {
			filteredSubTagList = subTagList
			filteredSubTagGroupList = subTagGroupList
		}

		filteredSubTagGroupList.each { TagGroup tagGroupInstance ->
			filteredSubTagList.addAll(tagGroupInstance.getTags(userId))
		}

		cache(filteredSubTagList)

		return filteredSubTagList
	}

	// Helper method to get list of descriptions of all tags.
	List<String> getTagsDescriptions(Long userId) {
		// Look for cached sub tag ids.
		if (tagDescriptionCache[this.id]) {
			return tagDescriptionCache[this.id] as List
		}

		// Fetch & cache all sub tags & return descriptions/names
		return getTags(userId)*.description
	}

	// Helper method to get list of id of all tags.
	List<Long> getTagsIds(Long userId) {
		// Look for cached sub tag ids.
		if (tagIdCache[this.id]) {
			return tagIdCache[this.id] as List
		}

		// Fetch & cache all sub tags & return ids
		return getTags(userId)*.id
	}

	boolean hasCachedData() {
		tagIdCache[this.id]
	}

	void removeFromCache(GenericTagGroup subTagGroupInstance, Long userId = null) {
		if (hasCachedData()) {
			subTagGroupInstance.getTags(userId).each { tagInstance ->
				removeFromCache(tagInstance)
			}
		}
	}

	void removeFromCache(Tag tagInstance) {
		synchronized(tagDescriptionCache) {
			Set cachedIds = tagIdCache[id] ?: []
			cachedIds.remove(tagInstance.id)
			tagIdCache.put(id, cachedIds)

			Set cachedDescriptions = tagDescriptionCache[id] ?: []
			cachedDescriptions.remove(tagInstance.description)
			tagDescriptionCache.put(id, cachedDescriptions)
		}
	}
	
	static void processAndAddTagGroups(List existingTagGroups, List newTagGroups) {
		TagGroup.withTransaction {
			newTagGroups.each { newTagGroup ->
				def existingGroup = existingTagGroups.find { it.id == newTagGroup.id && it.type == newTagGroup.type }
	
				if (existingGroup) {
					existingGroup["groupName"] = newTagGroup["groupName"]
					existingGroup["isReadOnly"] = newTagGroup["isReadOnly"]
	
					if (!existingGroup["isSystemGroup"]) {	// Only change state if existing tag is not already been marked as system group
						existingGroup["isSystemGroup"] = newTagGroup["isSystemGroup"]
					}
					if (!existingGroup["isAdminOfTagGroup"]) { // Only change state if existing taggroup has not admin permission.
						existingGroup["isAdminOfTagGroup"] = newTagGroup["isAdminOfTagGroup"]
					}
				} else {
					existingTagGroups << newTagGroup
				}
			}
		}
	}

	static List getAllTagGroupsForUser(Long userId) {
		return TagGroup.withTransaction {
			List tagGroups = getTagGroupsByUser(userId)
	
			processAndAddTagGroups(tagGroups, getSystemTagGroups())
			processAndAddTagGroups(tagGroups, getTagGroupsTheUserIsMemberOf(userId))
			processAndAddTagGroups(tagGroups, getTagGroupsTheUserIsAnAdminOf(userId))
	
			List<Long> tagGroupPropertyIds = tagGroups.collect { it.propertyId }*.toLong()
	
			if (tagGroupPropertyIds) {
				// Get all exclusion properties for all tag group properties including description
	
				List exclusions = DatabaseService.get().sqlRows("""SELECT
						te.object_id AS objectId, te.tag_group_property_id AS tagGroupPropertyId,
						CASE te.type WHEN ${ExclusionType.TAG.id} THEN 'TAG' WHEN ${ExclusionType.TAG_GROUP.id} THEN 'TAG_GROUP' ELSE '' END AS type,
						CASE te.type WHEN ${ExclusionType.TAG.id} THEN t.description WHEN ${ExclusionType.TAG_GROUP.id} THEN tg.description ELSE '' END as description
						FROM tag_exclusion AS te LEFT JOIN tag AS t ON t.id = te.object_id LEFT JOIN tag_group AS tg
						ON tg.id = te.object_id WHERE te.tag_group_property_id in (:tagGroupPropertyIds)
				""", [tagGroupPropertyIds:tagGroupPropertyIds])
	
				// Iterate each tag group data
				tagGroups.each { tagGroupData ->
					// Find all matching exclusion data and it to the tag group data
					tagGroupData["excludes"] = exclusions.findAll {
						it.tagGroupPropertyId == tagGroupData["propertyId"]
					}*.subMap(["objectId", "type", "description"])		// Only send tag or tag group data
				}
			}
	
			return tagGroups
		}
	}
	
	//IF(tgp.data_type_manual IS NULL OR tgp.data_type_manual = 0, IF(tgp.data_type_computed IS NULL OR tgp.data_type_computed = 2, false, true), tgp.data_type_manual = 1)

	private static String COMMON_QUERY = """
			SELECT	tg.id					AS id,
					tg.description			AS description,
					tgp.id					AS propertyId,
					tgp.is_continuous AS iscontinuous,
					tgp.show_points			AS showpoints,
					g.id					AS groupId,
					g.name					AS groupName,
					g.is_read_only			AS isReadOnly,
					class					AS type
			FROM	tag_group AS tg
					INNER JOIN tag_group_properties AS tgp 
							ON tgp.tag_group_id = tg.id 
					LEFT JOIN user_group AS g 
							ON g.id = tgp.group_id
		"""
	
	/**
	 * Get list of all tag groups owned by the given user.
	 * This is done by searching all tag groups which has userId property set for current user.
	 * @param userId Identity of the user.
	 * @return List of tag groups. (List of maps)
	 */
	static List getTagGroupsByUser(def userId) {
		return TagGroup.withTransaction {
			DatabaseService.get().sqlRows("$COMMON_QUERY where tgp.user_id = " + userId)
		}
	}

	/**
	 * Get list of tag groups the user is an admin of.
	 * This is done by getting all UserGroup, which has admin permission for the given user
	 * @param userId identity of the user
	 * @return List of tag groups.
	 */
	static List getTagGroupsTheUserIsAnAdminOf(Long userId) {
		return TagGroup.withTransaction {
			// Get all UserGroup ids, the user is an Admin of.
			List<Long> adminUserGroupIds = UserGroup.getGroupsForAdmin(userId)*.getAt(0).id
	
			List adminTagGroups = getTagGroupsForUserGroupIds(adminUserGroupIds)
			adminTagGroups*.put("isAdminOfTagGroup", true)
			adminTagGroups
		}
	}

	/**
	 * Get list of tag groups the user is an member of. This list doesn't returns
	 * the list of groups a user is an admin of.
	 * @param userId identity of the user
	 * @return
	 */
	static List getTagGroupsTheUserIsMemberOf(Long userId) {
		return TagGroup.withTransaction {
			List<Long> memberUserGroupIds = UserGroup.getGroupsForReader(userId)*.getAt(0).id
			memberUserGroupIds.addAll(UserGroup.getGroupsForWriter(userId)*.id)
	
			return getTagGroupsForUserGroupIds(memberUserGroupIds.unique())
		}
	}

	/**
	 * Get list of tag groups owned by given list of user groups.
	 * @param userGroups List of instances of UserGroup to get list of owned tag groups.
	 * Or list of map which must contain a field id, since Groovy doesn't respects the Java Generics.
	 * @return List of tag groups.
	 */
	static List getTagGroupsForUserGroups(List<UserGroup> userGroups) {
		return TagGroup.withTransaction {
			getTagGroupsForUserGroupIds(userGroups*.id)
		}
	}

	/**
	 * Get list of tag groups owned by given list of user group ids.
	 * @param userGroupIds List of ids of UserGroup to get list of owned tag groups.
	 * @return List of tag groups.
	 */
	static List getTagGroupsForUserGroupIds(List<Long> userGroupIds) {
		return TagGroup.withTransaction {
			if (!userGroupIds) {
				return []
			}
	
			return DatabaseService.get().sqlRows("$COMMON_QUERY where tgp.group_id in (:userGroupIds)", [userGroupIds:userGroupIds])
		}
	}

	// Get all Tag Groups which are owned by System UserGroups.
	static List getSystemTagGroups() {
		return TagGroup.withTransaction {
			List<Long> systemGroupIds = UserGroup.withCriteria {
				projections {
					distinct("id")
				}
				or {
					eq("name", UserGroup.SYSTEM_USER_GROUP_NAME)
					eq("isSystemGroup", true)
				}
			}
	
			List tagGroups = getTagGroupsForUserGroupIds(systemGroupIds)
			tagGroups.each { tagGroup ->
				tagGroup["isSystemGroup"] = true
			}
	
			return tagGroups
		}
	}

	static boolean isReadOnlyTagGroup(Long tagGroupId) {
		return TagGroup.withTransaction {
			DatabaseService.get().sqlRows("""SELECT COUNT(*) FROM tag_group_properties tgp, user_group g WHERE
					tgp.group_id = g.id AND g.is_read_only = 1 AND tgp.tag_group_id = $tagGroupId""") ? true : false
		}
	}

	static SharedTagGroup createOrLookupSharedTagGroup(String tagGroupName, Long groupId, Map args = [:]) {
		return TagGroup.withTransaction {
			SharedTagGroup tagGroupInstance = SharedTagGroup.lookupWithinGroup(tagGroupName, groupId)
	
			if (!tagGroupInstance) {
				tagGroupInstance = createTagGroup(tagGroupName, null, groupId, SharedTagGroup.class, args)
			}
	
			GenericTagGroupProperties.createOrLookup(groupId, "Group", tagGroupInstance)
			return tagGroupInstance
		}
	}

	static GenericTagGroup createOrLookupTagGroup(String tagGroupName, Long userId, Long groupId, Class type = TagGroup.class, Map args = [:]) {
		return TagGroup.withTransaction {
			GenericTagGroup tagGroupInstance = type.findByDescription(tagGroupName)
	
			if (!tagGroupInstance) {
				tagGroupInstance = this.createTagGroup(tagGroupName, type, args)
			}
	
			String propertyFor = userId ? "User" : "Group"
			GenericTagGroupProperties.createOrLookup(userId ?: groupId, propertyFor, tagGroupInstance)
	
			return tagGroupInstance
		}
	}

	static GenericTagGroup createTagGroup(String tagGroupName, Long userId, Long groupId, Class type = TagGroup.class, Map args = [:]) {
		return TagGroup.withTransaction {
			GenericTagGroup tagGroupInstance = createTagGroup(tagGroupName, type, args)
	
			String propertyFor = userId ? "User" : "Group"
			GenericTagGroupProperties.createOrLookup(userId ?: groupId, propertyFor, tagGroupInstance)
	
			return tagGroupInstance
		}
	}

	static GenericTagGroup createTagGroup(String tagGroupName, Class type = TagGroup.class, Map args = [:]) {
		return TagGroup.withTransaction {
			GenericTagGroup tagGroupInstance = type.newInstance()
			tagGroupInstance.description = tagGroupName
			tagGroupInstance.save()
			return tagGroupInstance
		}
	}

	TagGroup addTagId(Long tagId) {
		addTag(Tag.get(tagId))
	}
	
	TagGroup addTag(Tag tagInstance) {
		if (!tagInstance) {
			log.error "No tag instance supplied"
			return this
		}

		if (tags == null || (!tags.contains(tagInstance))) {
			log.debug "Adding [$tagInstance] to TagGroup [$this]"
			this.addToTags(tagInstance)
			addToCache(tagInstance)
		}
		return this
	}

	/**
	 * Add tags to tag group.
	 * @param tagGroupInstance
	 * @param tagIds must be comma separated ids.
	 * @return
	 */
	TagGroup addTags(String tagIds) {
		tagIds.tokenize(",").each { tagId ->
			addTag(Tag.get(tagId.trim()))
		}
		Utils.save(this)
		this
	}
}
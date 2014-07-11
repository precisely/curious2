package us.wearecurio.services

import us.wearecurio.model.Entry
import us.wearecurio.model.ExclusionType
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.SharedTagGroup
import us.wearecurio.model.UserGroup

class TagService {

	static transactional = true

	private static String COMMON_QUERY = """
			SELECT	tg.id					AS id,
					tg.description			AS description,
					tgp.id					AS propertyId,
					tgp.is_continuous		AS iscontinuous,
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

	def databaseService

	def getTagsByUser(def userId) {
		def tags = databaseService.sqlRows("select t.id as id, t.description as description, count(e.id) as c, CASE prop.data_type_computed WHEN 'CONTINUOUS' THEN 1 ELSE 0 END as iscontinuous, prop.show_points as showpoints from entry e inner join tag t on e.tag_id = t.id left join tag_properties prop on prop.user_id = e.user_id and prop.tag_id = t.id where e.user_id = " + new Long(userId) + " and e.date is not null group by t.id order by t.description")
		return tags
	}

	def getTagsByDescription(def userId, def description) {
		def tags = Entry.executeQuery("select new Map(entry.tag.id as id,entry.tag.description as description) from Entry as entry where entry.userId=:userId "+
				"and entry.tag.description like :desc group by entry.tag.id", [userId:userId,desc:"%${description}%"])
		return tags
	}

	void processAndAddTagGroups(List existingTagGroups, List newTagGroups) {
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

	List getAllTagGroupsForUser(Long userId) {
		List tagGroups = getTagGroupsByUser(userId)

		processAndAddTagGroups(tagGroups, getSystemTagGroups())
		processAndAddTagGroups(tagGroups, getTagGroupsTheUserIsMemberOf(userId))
		processAndAddTagGroups(tagGroups, getTagGroupsTheUserIsAnAdminOf(userId))

		List<Long> tagGroupPropertyIds = tagGroups.collect { it.propertyId }*.toLong()

		if (tagGroupPropertyIds) {
			// Get all exclusion properties for all tag group properties including description

			List exclusions = databaseService.sqlRows("""SELECT
					te.object_id AS objectId, te.tag_group_property_id AS tagGroupPropertyId,
					CASE te.type WHEN ${ExclusionType.TAG.id} THEN 'TAG' WHEN ${ExclusionType.TAG_GROUP.id} THEN 'TAG_GROUP' ELSE '' END AS type,
					CASE te.type WHEN ${ExclusionType.TAG.id} THEN t.description WHEN ${ExclusionType.TAG_GROUP.id} THEN tg.description ELSE '' END as description
					FROM tag_exclusion AS te LEFT JOIN tag AS t ON t.id = te.object_id LEFT JOIN tag_group AS tg
					ON tg.id = te.object_id WHERE te.tag_group_property_id in (${tagGroupPropertyIds.join(',')})""")

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

	/**
	 * Get list of all tag groups owned by the given user.
	 * This is done by searching all tag groups which has userId property set for current user.
	 * @param userId Identity of the user.
	 * @return List of tag groups. (List of maps)
	 */
	List getTagGroupsByUser(def userId) {
		return databaseService.sqlRows("$COMMON_QUERY where tgp.user_id=" + new Long(userId))
	}

	/**
	 * Get list of tag groups the user is an admin of.
	 * This is done by getting all UserGroup, which has admin permission for the given user
	 * @param userId identity of the user
	 * @return List of tag groups.
	 */
	List getTagGroupsTheUserIsAnAdminOf(Long userId) {
		// Get all UserGroup ids, the user is an Admin of.
		List<Long> adminUserGroupIds = UserGroup.getGroupsForAdmin(userId)*.getAt(0).id

		List adminTagGroups = getTagGroupsForUserGroupIds(adminUserGroupIds)
		adminTagGroups*.put("isAdminOfTagGroup", true)
		adminTagGroups
	}

	/**
	 * Get list of tag groups the user is an member of. This list doesn't returns
	 * the list of groups a user is an admin of.
	 * @param userId identity of the user
	 * @return
	 */
	List getTagGroupsTheUserIsMemberOf(Long userId) {
		List<Long> memberUserGroupIds = UserGroup.getGroupsForReader(userId)*.getAt(0).id
		memberUserGroupIds.addAll(UserGroup.getGroupsForWriter(userId)*.getAt(0).id)

		getTagGroupsForUserGroupIds(memberUserGroupIds.unique())
	}

	/**
	 * Get list of tag groups owned by given list of user groups.
	 * @param userGroups List of instances of UserGroup to get list of owned tag groups.
	 * Or list of map which must contain a field id, since Groovy doesn't respects the Java Generics.
	 * @return List of tag groups.
	 */
	List getTagGroupsForUserGroups(List<UserGroup> userGroups) {
		getTagGroupsForUserGroupIds(userGroups*.id)
	}

	/**
	 * Get list of tag groups owned by given list of user group ids.
	 * @param userGroupIds List of ids of UserGroup to get list of owned tag groups.
	 * @return List of tag groups.
	 */
	List getTagGroupsForUserGroupIds(List<Long> userGroupIds) {
		if (!userGroupIds) {
			return []
		}

		return databaseService.sqlRows("$COMMON_QUERY where tgp.group_id in (${userGroupIds.join(',')})")
	}

	// Get all Tag Groups which are owned by System UserGroups.
	List getSystemTagGroups() {
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

	boolean isReadOnlyTagGroup(Long tagGroupId) {
		databaseService.sqlRows("""SELECT COUNT(*) FROM tag_group_properties tgp, user_group g WHERE
				tgp.group_id = g.id AND g.is_read_only = 1 AND tgp.tag_group_id = $tagGroupId""") ? true : false
	}

	boolean canEdit(SharedTagGroup tagGroup, Long userId) {
		// There can be one single SharedTagGroup per group
		GenericTagGroupProperties tagGroupProperty = GenericTagGroupProperties.findByTagGroupId(tagGroup.id)
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
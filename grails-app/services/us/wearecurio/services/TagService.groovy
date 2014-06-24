package us.wearecurio.services

import us.wearecurio.model.Entry
import us.wearecurio.model.UserGroup

class TagService {

	static transactional = true

	private static final String COMMON_QUERY = """
			SELECT	tg.id				AS id,
					tg.description		AS description,
					tgp.is_continuous	AS is_continuous,
					tgp.group_id		AS groupId,
					tg.name				AS shortName,
					tgp.show_points		AS showpoints,
					g.name				AS groupName,
					class				AS type
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

	List getAllTagGroupsForUser(Long userId) {
		// First fetch system tag groups so not to remove while check for unique tag groups.
		List tagGroups = getSystemTagGroups() + getTagGroupsByUser(userId) + getTagGroupsTheUserIsAnAdminOf(userId)
		return tagGroups.unique { it.description }
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

		return getTagGroupsForUserGroupIds(adminUserGroupIds)
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

}
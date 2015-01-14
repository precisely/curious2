package us.wearecurio.model;

import grails.converters.*
import grails.util.Holders
import groovy.sql.Sql

import javax.sql.DataSource

import org.apache.commons.logging.LogFactory
import org.hibernate.criterion.CriteriaSpecification

import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EmailService
import us.wearecurio.utility.Utils

class UserGroup {

	private static def log = LogFactory.getLog(this)
	static final String SYSTEM_USER_GROUP_NAME = "System"

	static constraints = {
		name(unique:true)
		fullName(unique:true)
		fullName(nullable:true)
		description(nullable:true)
		isVirtual(nullable:true)
	}

	static mapping = {
		version false
		table 'user_group'
	}

	Date created = new Date()
	Date updated = new Date()
	boolean isOpen // anyone can join
	boolean isReadOnly // only admins can post
	boolean isModerated // all posts are moderated before posting (not yet implemented)
	boolean defaultNotify // notify admins by default
	boolean isSystemGroup
	Boolean isVirtual
	String name
	String fullName
	String description

	UserGroup() {
	}

	UserGroup(String name, String fullName, String description, Map options) {
		bindData(name, fullName, description, options)
	}

	void bindData(String name, String fullName, String description, Map options) {
		this.isOpen = options ? options['isOpen'] : false
		this.isReadOnly =  options ? options['isReadOnly'] : false
		this.isModerated =  options ? options['isModerated'] : false
		this.defaultNotify =  options ? options['defaultNotify'] : false
		this.isSystemGroup =  options ? options['isSystemGroup'] : false
		this.name = name
		this.fullName = fullName
		this.description = description
	}

	static UserGroup lookupOrCreateSystemGroup() {
		createOrUpdate(SYSTEM_USER_GROUP_NAME, "system", "", [isSystemGroup: true])
	}

	static UserGroup createVirtual(String fullName) {
		UserGroup userGroup = new UserGroup()
		
		userGroup.name = "__virtual" + UUID.randomUUID().toString()
		userGroup.fullName = fullName
		userGroup.description = ''
		userGroup.isVirtual = true
		userGroup.isOpen = false
		userGroup.isReadOnly = false
		userGroup.isModerated = false
		userGroup.defaultNotify = false
		userGroup.isSystemGroup = false
		
		Utils.save(userGroup, true)
		
		return userGroup
	}

	static UserGroup createOrUpdate(String name, String fullName, String description, def options) {
		UserGroup userGroupInstance
		if (options["id"]) {
			userGroupInstance = UserGroup.get(options["id"])
		} else {
			userGroupInstance = lookup(name)
		}

		if (!userGroupInstance) {
			userGroupInstance = new UserGroup()
		} else if (userGroupInstance.name == SYSTEM_USER_GROUP_NAME) {
			// Do not allow default system group to change the name.
			name = SYSTEM_USER_GROUP_NAME
			options = options ?: [:]
			options["isSystemGroup"] = true
		}

		userGroupInstance.bindData(name, fullName, description, options)
		Utils.save(userGroupInstance, true)
		
		return userGroupInstance
	}

	static UserGroup lookup(String name) {
		return UserGroup.findByName(name)
	}

	static UserGroup create(String name, String fullName, String description, def options) {
		UserGroup group = new UserGroup(name, fullName, description, options)

		Utils.save(group, true)

		return group
	}

	public static void delete(UserGroup group) {
		Long groupId = group.getId()
		log.debug "UserGroup.delete() groupId:" + groupId
		GroupMemberReader.executeUpdate("delete GroupMemberReader item where item.groupId = :id", [id:groupId])
		GroupMemberWriter.executeUpdate("delete GroupMemberWriter item where item.groupId = :id", [id:groupId])
		GroupMemberDefaultFor.executeUpdate("delete GroupMemberDefaultFor item where item.groupId = :id", [id:groupId])
		GroupMemberAdmin.executeUpdate("delete GroupMemberAdmin item where item.groupId = :id", [id:groupId])
		GroupMemberNotified.executeUpdate("delete GroupMemberNotified item where item.groupId = :id", [id:groupId])
		GroupMemberNotifiedMajor.executeUpdate("delete GroupMemberNotifiedMajor item where item.groupId = :id", [id:groupId])
		GroupMemberDiscussion.executeUpdate("delete GroupMemberDiscussion item where item.groupId = :id", [id:groupId])
		group.delete()
	}

	/**
	 * List of group a user in an admin of.
	 * @param user
	 * @return List of UserGroup instances the user is an admin of.
	 */
	static List getGroupsForAdmin(User user) {
		getGroupsForAdmin(user.id)
	}

	static List getGroupsForAdmin(Long userId) {
		return UserGroup.executeQuery("select userGroup as userGroup, item.created as joined from UserGroup userGroup, GroupMemberAdmin item where item.memberId = :id and item.groupId = userGroup.id",
				[id: userId])
	}

	static List getGroupsForReader(User user) {
		getGroupsForReader(user.id)
	}

	static List getGroupsForReader(Long userId) {
		return UserGroup.executeQuery("select userGroup as userGroup, item.created as joined from UserGroup userGroup, GroupMemberReader item where item.memberId = :id and item.groupId = userGroup.id",
				[id: userId])
	}

	static List getGroupsForWriter(User user) {
		getGroupsForWriter(user.id)
	}

	static List getGroupsForWriter(Long userId) {
		return UserGroup.executeQuery("""SELECT new Map(ug.id AS id, ug.name AS name, ug.fullName AS fullName,
				ug.description AS description, ug.created AS created, item.created AS joined)
				FROM UserGroup ug, GroupMemberWriter item WHERE item.memberId = :id AND item.groupId = ug.id""",
				[id: userId])
	}

	public static def getGroupsForDiscussion(Discussion discussion) {
		return UserGroup.executeQuery("select userGroup as userGroup from UserGroup userGroup, GroupMemberDiscussion item where item.memberId = :id and item.groupId = userGroup.id",
				['id':discussion.getId()])
	}

	public static def getPrimaryGroupForDiscussionId(Long discussionId) {
		return UserGroup.executeQuery("select userGroup as userGroup from UserGroup userGroup, GroupMemberDiscussion item where item.memberId = :id and item.groupId = userGroup.id order by userGroup.created asc limit 1",
				['id':discussionId])
	}

	/**
	 * Calculate admin permissions and remove empty discussions not owned by the user
	 */
	private static List addAdminPermissions(User user, List discussionInfos) {
		List retVals = []
		Map discussionMap = [:]

		for (info in discussionInfos) {
			Discussion discussion = Discussion.get(info["discussionId"])
			
			if (discussion.isNew() && discussion.fetchUserId() != user.getId())
				continue
			
			Map retVal = discussion.getJSONDesc()
			Long discussionId = discussion.id
			Long groupId = info["groupId"] ?: 0
			UserGroup group = UserGroup.get(groupId)

			retVal['groupId'] = groupId
			retVal['isAdmin'] = retVal['userId'] == user.id || (group ? group.hasAdmin(user) : true)
			retVal['groupName'] = group ? group.getFullName() : 'Private'
			retVal['userName'] = info["username"]
			// Adding a flag to indicate if this discussion was started with a plot
			def firstPost = Discussion.get(discussionId).fetchFirstPost()
			retVal['isPlot'] = firstPost?.plotDataId ? true : false
			retVal['firstPost'] = firstPost
			discussionMap[discussionId] = retVal
			retVals.add(retVal)
		}

		for (retVal in retVals) {
			if (!retVal['groupId']) {
				def groups = UserGroup.getPrimaryGroupForDiscussionId(retVal['id'])
				if (groups) {
					retVal['groupId'] = groups[0].getId()
					retVal['groupName'] = groups[0].getFullName()
				}
			}
		}

		return retVals
	}

//	private List 

	static Map getDiscussionsInfoForGroupNameList(User user, def groupNameList, Map args = [:]) {
		boolean owned = false
		log.debug "UserGroup.getDiscussionsInfoForGroupNameListr(): name list: " + groupNameList?.dump()
		log.debug "UserGroup.getDiscussionsInfoForGroupNameListr(): user: " + user?.dump()

		args.max = args.max ?: 10
		args.offset = args.offset ?: 0

		List groupIds = []
		for (name in groupNameList) {
			if (name.equals('[owned]'))
				owned = true
			else {
				UserGroup group = UserGroup.lookup(name)
				if (group) {
					if (group.hasReader(user)) {
						groupIds.add(group.getId())
					}
				}
			}
		}
		log.debug "UserGroup.getDiscussionsInfoForGroupNameListr(): groupId list: " + groupIds?.dump()
		def map = [:]

		if (groupIds.size() == 0 && !owned) {
			return [:] // user has no permissions to read anything
		}
		if (owned) map['id'] = user.getId()

		String discussionQuery = """SELECT %s FROM discussion AS d
					INNER JOIN _user AS user ON d.user_id = user.id
					LEFT JOIN group_member_discussion AS dItem ON d.id = dItem.member_id
					WHERE ${groupIds.size() > 0 ? "dItem.group_id IN (${groupIds.join(',')})" : ""} %s
					ORDER BY d.updated DESC"""

		String argument1 = "d.id as discussionId, dItem.group_id AS groupId, user.username AS username"
		String argument2 = ""

		Map paginatedData = [:]

		if (owned) {
			argument2 = "${groupIds.size() ? 'OR' : ''} d.user_id = :id"
		}

		DatabaseService databaseService = DatabaseService.get()

		String countQuery = String.format(discussionQuery, "COUNT(d.id) as count", argument2)
		paginatedData["totalCount"] = databaseService.sqlRows(countQuery, map)[0]?.count
		log.debug "paginated data count: ${paginatedData['totalCount']}"
		
		argument2 += " GROUP BY d.id"
		String listQuery = String.format(discussionQuery, argument1, argument2)
		listQuery += " limit ${args.max.toInteger()} offset ${args.offset.toInteger()}"

		List result = databaseService.sqlRows(listQuery, map)

		log.debug "data: ${result.dump()}"
		//info["discussionId"]
		paginatedData["dataList"] = addAdminPermissions(user, result)

		paginatedData
	}

	private static String DISCUSSIONS_QUERY = """SELECT %s FROM discussion AS d
				LEFT JOIN group_member_discussion AS dItem ON d.id = dItem.member_id
				LEFT JOIN group_member_reader AS rItem ON dItem.group_id = rItem.group_id
				INNER JOIN _user AS user ON d.user_id = user.id
				WHERE rItem.member_id = :id %s ORDER BY d.updated DESC"""

	static Map getDiscussionsInfoForUser(User user, boolean owned, Map args = [:]) {
		Map paginatedData = [:]
		Map namedParameters = [id: user.id]

		args.max = args.max ?: 5
		args.offset = args.offset ?: 0

		String argument1 = "d.id AS discussionId, dItem.group_id AS groupId, user.username AS username"
		String argument2 = ""

		if (owned) {
			log.debug "UserGroup.getDiscussionsInfoForUser(): Getting owned entries"

			argument2 = "OR d.user_id = :id"
		}

		DatabaseService databaseService = DatabaseService.get()

		argument2 += " GROUP BY d.id"
		
		String countQuery = "SELECT count(*) as count FROM (" + String.format(DISCUSSIONS_QUERY, "d.id", argument2) + ") as t"
		paginatedData["totalCount"] = databaseService.sqlRows(countQuery, namedParameters)[0]?.count
		log.debug "paginated data count: ${paginatedData['totalCount']}"

		String listQuery = String.format(DISCUSSIONS_QUERY, argument1, argument2)
		listQuery += " limit ${args.max.toInteger()} offset ${args.offset.toInteger()}"

		List result = databaseService.sqlRows(listQuery, namedParameters)
		List discussionIdList = result.collect { it.discussionId }*.toLong()

		List allPostCountOrderdByDiscussion
		if (discussionIdList) {
			allPostCountOrderdByDiscussion = DiscussionPost.withCriteria {
				resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
				projections {
					groupProperty("discussionId", "discussionId")
					count("id", "totalPosts")
				}
				'in'("discussionId", discussionIdList)
			}
		}
	
		String getSecondPostQuery = """select id, message, discussion_id,
				@num := if(@did = discussion_id, @num + 1, 1) as row_num, 
				@did := discussion_id as discussionId from discussion_post force index(discussion_id)
				group by discussion_id, message having row_num = 2"""
	
		DataSource dataSource =  Holders.getApplicationContext().dataSource
		Sql sql = new Sql(dataSource)
	
		// Initialize the variable to default value to avoid problem on next query
		sql.execute("""SET @num := 0, @did := 0""")
	
		List secondPostsList = sql.rows(getSecondPostQuery)
	
		// Close the connection to avoid memory leak	
		sql.close()
	
		Map discussionPostData = [:]
	
		//separating & populating required data for every discussion
		discussionIdList.each() { discussionId ->
			discussionPostData[discussionId] = ["secondPost": secondPostsList.find{ it.discussion_id == discussionId }?.message,
				"totalPosts": allPostCountOrderdByDiscussion.find{ it.discussionId == discussionId }?.totalPosts]
		}

		paginatedData["discussionPostData"] = discussionPostData
		paginatedData["dataList"] = addAdminPermissions(user, result)

		paginatedData
	}

	static def canReadDiscussion(User user, Discussion discussion) {
		if (!user)
			return discussion.getIsPublic()
		if (discussion.getUserId() == user.getId())
			return true
		def groups = getGroupsForDiscussion(discussion)
		for (group in groups) {
			if (group.hasReader(user))
				return true
		}

		return false
	}

	static def canWriteDiscussion(User user, Discussion discussion) {
		if (!user) {
			log.debug "UserGroup.canWriteDiscussion(): No user passed, looking to see if the group is public"
			return discussion.getIsPublic()
		}
		if (discussion.getUserId() == user.getId()) {
			log.debug "UserGroup.canWriteDiscussion(): User ${user.getId()} is the author of the discussion ${discussion}, can write"
			return true
		}
		def groups = getGroupsForDiscussion(discussion)
		for (UserGroup group in groups) {
			if (group.hasWriter(user)) {
				log.debug "UserGroup.canWriteDiscussion(): User ${user.getId()} has write permission on one of the groups, can write"
				return true
			}
		}

		log.debug "UserGroup.canWriteDiscussion(): User ${user.getId()} has NO write permission"
		return false
	}

	static def canAdminDiscussion(User user, Discussion discussion) {
		if (!user)
			return false
		if (!discussion)
			return false
		if (discussion.getUserId() == user.getId())
			return true
		def groups = getGroupsForDiscussion(discussion)
		for (UserGroup group in groups) {
			if (group.hasAdmin(user))
				return true
		}

		return false
	}

	def addReader(User user) {
		if (!user) return;

		GroupMemberReader.create(id, user.getId())

		this.notifyNotifiedMajors("contact@wearecurio.us", "New user '" + user.getUsername() + "' joined group '" + this.description + "'",
				"New user '" + user.getUsername() + "' (" + user.getFirst() + " "
				+ user.getLast() + " <" + user.getEmail() + ">) joined group '" + this.description + "'")
	}

	def removeReader(User user) {
		if (!user) return

		GroupMemberReader.delete(id, user.getId())

		this.notifyNotifiedMajors("contact@wearecurio.us", "User '" + user.getUsername() + "' left group '" + this.description + "'",
				"User '" + user.getUsername() + "' (" + user.getFirst() + " "
				+ user.getLast() + " <" + user.getEmail() + ">) left group '" + this.description + "'")
	}

	def hasReader(User user) {
		if (!user) return false

		return GroupMemberReader.lookup(id, user.getId()) != null
	}

	def addWriter(User user) {
		if (!user) return

		GroupMemberWriter.create(id, user.getId())
	}

	def removeWriter(User user) {
		if (!user) return

		GroupMemberReader.delete(id, user.getId())
	}

	boolean hasWriter(User user) {
		hasWriter(user?.id)
	}

	boolean hasWriter(Long userId) {
		hasWriter(id, userId)
	}

	static boolean hasWriter(Long id, Long userId) {
		if (!id || !userId) {
			return false
		}

		GroupMemberWriter.lookup(id, userId) != null
	}

	def addAdmin(User user) {
		if (!user) return
		addReader(user)
		addWriter(user)
		if (defaultNotify) {
			addNotified(user)
			addNotifiedMajor(user)
		}
		GroupMemberAdmin.create(id, user.getId())
	}

	def removeAdmin(User user) {
		if (!user) return

		GroupMemberAdmin.delete(id, user.getId())
	}

	boolean hasAdmin(User user) {
		if (!user) return false

		return hasAdmin(id, user.id)
	}

	static boolean hasAdmin(Long groupId, Long userId) {
		return GroupMemberAdmin.lookup(groupId, userId) != null
	}

	def addNotified(User user) {
		if (!user) return

		GroupMemberNotified.create(id, user.getId())
	}

	def removeNotified(User user) {
		if (!user) return

		GroupMemberNotified.delete(id, user.getId())
	}

	def hasNotified(User user) {
		if (!user) return false

		return GroupMemberNotified.lookup(id, user.getId()) != null
	}

	def addNotifiedMajor(User user) {
		if (!user) return

		GroupMemberNotifiedMajor.create(id, user.getId())
	}

	def removeNotifiedMajor(User user) {
		if (!user) return

		GroupMemberNotified.delete(id, user.getId())
	}

	def hasNotifiedMajor(User user) {
		if (!user) return false

		return GroupMemberNotifiedMajor.lookup(id, user.getId()) != null
	}

	/** 
	 * The add/remove Member methods are used to add a generic user to a group. If the group is
	 * read-only, only adds read permissions for the user. Use addAdmin() to add a more powerful
	 * user to the group. It is safe, however, to use addMember followed by addAdmin.
	 * 
	 * There is no "hasMember" method --- use hasReader, hasWriter, or hasAdmin instead.
	 * 
	 * @param user
	 * @return
	 */
	def addMember(User user) {
		if (!user) return

		addReader(user)

		if (!isReadOnly)
			addWriter(user)
	}

	def removeMember(User user) {
		if (!user) return

		removeReader(user)
		removeWriter(user)
		removeAdmin(user)
		removeNotified(user)
		removeNotifiedMajor(user)
		removeDefaultFor(user)
	}

	/**
	 * Methods for handling editing a user's default group
	 * @param discussion
	 * @return
	 */
	def addDefaultFor(User user) {
		if (!user) return false

		if (!hasReader(user)) return false

		return GroupMemberDefaultFor.create(id, user.getId()) ? true : false
	}

	def removeDefaultFor(User user) {
		if (!user) return

		GroupMemberDefaultFor.delete(id, user.getId())
	}

	static UserGroup getDefaultGroupForUser(User user) {
		def defaultGroup = GroupMemberDefaultFor.lookupGroup(user)
		if (!defaultGroup) {
			UserGroup curious = UserGroup.findByName("curious")
			if (!curious) {
				curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
					[isReadOnly:false, defaultNotify:false])
			}
			defaultGroup = curious
			defaultGroup.addMember(user)
			defaultGroup.addDefaultFor(user)
			defaultGroup.addWriter(user)
		}
		return defaultGroup
	}

	boolean canAddRemoveDiscussion(Discussion discussion) {
		if (!discussion) {
			return false
		}

		Long userId = discussion.getUserId()
		if (userId) {
			if (!hasWriter(userId)) {
				return false
			}
		} else if (!isOpen) {
			return false
		}

		true
	}

	boolean addDiscussion(Discussion discussion) {
		if (!canAddRemoveDiscussion(discussion)) {
			return false
		}

		GroupMemberDiscussion.create(id, discussion.getId())
		return true
	}

	boolean removeDiscussion(Discussion discussion) {
		if (!canAddRemoveDiscussion(discussion)) {
			return false
		}

		GroupMemberDiscussion.delete(id, discussion.id)
		return true
	}

	boolean hasDiscussion(Discussion discussion) {
		hasDiscussion(id, discussion?.id)
	}

	static boolean hasDiscussion(Long id, Long discussionId) {
		if (!discussionId) {
			return false
		}
		return GroupMemberDiscussion.lookup(id, discussionId) != null
	}

	def getNotifiedUsers() {
		return User.executeQuery("select user from User user, GroupMemberNotified item where item.groupId = :id and user.id = item.memberId",
				['id':getId()])
	}

	def getNotifiedMajorUsers() {
		return User.executeQuery("select user from User user, GroupMemberNotifiedMajor item where item.groupId = :id and user.id = item.memberId",
				['id':getId()])
	}

	def getAdminUsers() {
		return User.executeQuery("select user from User user, GroupMemberAdmin item where item.groupId = :id and user.id = item.memberId",
				['id':getId()])
	}

	def getReaderUsers() {
		return User.executeQuery("select user from User user, GroupMemberReader item where item.groupId = :id and user.id = item.memberId",
				['id':getId()])
	}

	def getWriterUsers() {
		return User.executeQuery("select user from User user, GroupMemberWriter item where item.groupId = :id and user.id = item.memberId",
				['id':getId()])
	}

	def getDiscussions() {
		return Discussion.executeQuery("select d from Discussion d, GroupMemberDiscussion item where item.groupId = :id and d.id = item.memberId",
				['id':getId()])
	}

	private sendMessages(people, String fromAddress, String subjectString, String message) {
		for (User p in people) {
			if (p.getEmail() != null) {
				log.debug("Sending email about '" + subjectString + "' to " + p.getEmail())
				EmailService.get().sendMail {
					to p.getEmail()
					from fromAddress
					subject subjectString
					body message
				}
			}
		}
	}

	def notifyAdmins(String from, String subject, String message) {
		sendMessages(getAdminUsers(), from, subject, message)
	}

	def notifyNotifieds(String from, String subject, String message) {
		sendMessages(getNotifiedUsers(), from, subject, message)
	}

	def notifyNotifiedMajors(String from, String subject, String message) {
		sendMessages(getNotifiedMajorUsers(), from, subject, message)
	}

	def notifyReaders(String from, String subject, String message) {
		sendMessages(getReaderUsers(), from, subject, message)
	}

	/**
	 * Get data for the discussion data lists
	 */
	def getJSONDesc() {
		return [
			'id':this.id,
			name:this.name,
		]
	}

	String toString() {
		return "UserGroup(id:" + getId() + ", name:" + name + ", fullName:" + fullName \
				+ ", description:'" + description + "', created:" + created \
				+ ", updated:" + updated + ")"
	}
}

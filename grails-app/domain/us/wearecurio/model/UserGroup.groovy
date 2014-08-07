package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users together, notifying users, discussions, etc.
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class UserGroup {

	private static def log = LogFactory.getLog(this)
	static final String SYSTEM_USER_GROUP_NAME = "System"

	static constraints = {
		name(unique:true)
		fullName(unique:true)
		fullName(nullable:true)
		description(nullable:true)
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
		return UserGroup.executeQuery("select userGroup as userGroup, item.created as joined from UserGroup userGroup, GroupMemberWriter item where item.memberId = :id and item.groupId = userGroup.id",
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
	private static def addAdminPermissions(User user, def discussionInfos) {
		def retVals = []
		def discussionMap = [:]

		for (info in discussionInfos) {
			Discussion discussion = info[0]
			
			if (discussion.isNew() && discussion.fetchUserId() != user.getId())
				continue
			
			def retVal = discussion.getJSONDesc()
			def discussionId = retVal['id']
			Long groupId = info[1] > 0 ? info[1] : 0L
			UserGroup group = groupId ? UserGroup.get(groupId) : null

			// check to see if discussion is already in the list in a different group
			def oldRetVal = discussionMap[discussionId]
			if (oldRetVal != null) {
				// check to see if new group gives this author admin permissions on the discussion topic
				if (!oldRetVal['isAdmin']) {
					oldRetVal['isAdmin'] = retVal['userId'] == user.getId() || group.hasAdmin(user)
					oldRetVal['groupId'] = groupId
					oldRetVal['groupName'] = group ? group.getFullName() : 'Private'
				} else if (oldRetVal['groupId'])
					oldRetVal['groupId'] = groupId
				continue
			}
			retVal['isAdmin'] = group ? group.hasAdmin(user) : true
			retVal['groupId'] = groupId
			retVal['groupName'] = group ? group.getFullName() : 'Private'
			retVal['userName'] = info[2]
			// Adding a flag to indicate if this discussion was started with a plot
			def firstPost = Discussion.get(discussionId).fetchFirstPost()
			retVal['isPlot'] = firstPost?.plotDataId ? true : false
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

	public static def getDiscussionsInfoForGroupNameList(User user, def groupNameList) {
		boolean owned = false
		log.debug "UserGroup.getDiscussionsInfoForGroupNameListr(): name list: " + groupNameList?.dump()
		log.debug "UserGroup.getDiscussionsInfoForGroupNameListr(): user: " + user?.dump()
		def groupIds = []
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

		if (groupIds.size() > 0) map['groupIds'] = groupIds
		else if (!owned) return [:] // user has no permissions to read anything
		if (owned) map['id'] = user.getId()

		def results = Discussion.executeQuery(
				"select distinct d, dItem.groupId as groupId, user.username from Discussion d, "
					+ "User user, GroupMemberDiscussion dItem where d.id = dItem.memberId "
					+ (groupIds?.size() > 0 ? "and dItem.groupId in (:groupIds)) " : " ")
					+ "and d.userId = user.id order by d.updated desc", map)

		if (owned) {
			results.addAll(Discussion.executeQuery("select distinct d, -1 as groupId, user.username from Discussion d where d.userId = :id", [id:user.getId()]))
		}
		
		return addAdminPermissions(user, results)
	}

	private static def DISCUSSIONS_QUERY =\
			"select distinct d, dItem.groupId as groupId, user.username from Discussion d, "\
				+ "GroupMemberDiscussion dItem, GroupMemberReader rItem, User user "\
				+ "where d.id = dItem.memberId and dItem.groupId = rItem.groupId and rItem.memberId = :id "\
				+ "and d.userId = user.id order by d.updated desc"

	public static def getDiscussionsInfoForUser(User user, boolean owned) {
		def results = Discussion.executeQuery(DISCUSSIONS_QUERY, [id:user.getId()])
		if (owned) {
			log.debug "UserGroup.getDiscussionsInfoForUser(): Getting owned entries"
			results.addAll(Discussion.executeQuery("select distinct d, -1 as groupId, user.username "
				+ "from Discussion d, User user where d.userId = :id and user.id = d.userId "
				, [id:user.getId()]))
		}

		return addAdminPermissions(user, results)
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

	def hasWriter(User user) {
		if (!user) return false

		return GroupMemberWriter.lookup(id, user.getId()) != null
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

	def addDiscussion(Discussion discussion) {
		if (!discussion) return
		def userId = discussion.getUserId()
		if (userId) {
			if (!hasWriter(User.get(userId))) {
				return false
			}
		} else {
			if (!isOpen)
				return false
		}
		GroupMemberDiscussion.create(id, discussion.getId())
		return true
	}

	def hasDiscussion(Discussion discussion) {
		if (!discussion) return false
		return GroupMemberDiscussion.lookup(id, discussion.getId()) != null
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
				Utils.getMailService().sendMail {
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

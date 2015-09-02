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
		name(unique:false)
		fullName(unique:false)
		fullName(nullable:true)
		description(nullable:true)
		isVirtual(nullable:true)
		isHidden(nullable:true)
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
	Boolean isHidden // hidden from normal searches (typically used by user virtual groups)
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
		this.isHidden =  options ? options['isHidden'] : false
		this.name = name
		this.fullName = fullName
		this.description = description
	}

	static UserGroup lookupOrCreateSystemGroup() {
		createOrUpdate(SYSTEM_USER_GROUP_NAME, "system", "", [isSystemGroup: true])
	}

	static UserGroup createVirtual(String fullName, boolean isHidden = false) {
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
		userGroup.isHidden = isHidden

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
		GroupMemberInvited.executeUpdate("delete GroupMemberInvited item where item.groupId = :id", [id:groupId])
		GroupMemberInvitedAdmin.executeUpdate("delete GroupMemberInvitedAdmin item where item.groupId = :id", [id:groupId])
		GroupMemberWriter.executeUpdate("delete GroupMemberWriter item where item.groupId = :id", [id:groupId])
		GroupMemberDefaultFor.executeUpdate("delete GroupMemberDefaultFor item where item.groupId = :id", [id:groupId])
		GroupMemberAdmin.executeUpdate("delete GroupMemberAdmin item where item.groupId = :id", [id:groupId])
		GroupMemberNotified.executeUpdate("delete GroupMemberNotified item where item.groupId = :id", [id:groupId])
		GroupMemberNotifiedMajor.executeUpdate("delete GroupMemberNotifiedMajor item where item.groupId = :id", [id:groupId])
		GroupMemberDiscussion.executeUpdate("delete GroupMemberDiscussion item where item.groupId = :id", [id:groupId])
		group.delete(flush:true)
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

	static List getGroupsForInvited(User user) {
		getGroupsForInvited(user.id)
	}

	static List getGroupsForInvited(Long userId) {
		return UserGroup.executeQuery("select userGroup as userGroup, item.created as joined from UserGroup userGroup, GroupMemberInvitedAdmin item where item.memberId = :id and item.groupId = userGroup.id",
						[id: userId])
	}

	static List getGroupsForInvitedAdmin(User user) {
		getGroupsForInvitedAdmin(user.id)
	}

	static List getGroupsForInvitedAdmin(Long userId) {
		return UserGroup.executeQuery("select userGroup as userGroup, item.created as joined from UserGroup userGroup, GroupMemberInvitedAdmin item where item.memberId = :id and item.groupId = userGroup.id",
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
		return UserGroup.executeQuery("select userGroup as userGroup from UserGroup userGroup, GroupMemberDiscussion item where item.memberId = :id and item.groupId = userGroup.id order by userGroup.created asc",
						['id':discussionId], [max:1])
	}

	static def getTotalDiscussionPosts(List discussionIdList) {
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
			discussionPostData[discussionId] = ["totalPosts": allPostCountOrderdByDiscussion.find{ it.discussionId == discussionId }?.totalPosts]
		}

		return discussionPostData
	}

	static def canReadDiscussion(User user, Discussion discussion) {
		if (!user)
			return discussion.getIsPublic()
		if (discussion.getUserId() == user.getId())
			return true

		def groups = getGroupsForDiscussion(discussion)
		for (UserGroup group in groups) {
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

	def acceptAllInvited() {
		def invitedUserIds = GroupMemberInvited.lookupMemberIds(this.id)

		for (Long userId in invitedUserIds) {
			removeInvited(userId)
			addReader(userId)
			addWriter(userId)

			User user = User.get(userId)

			user.notifyEmail("You've been invited to " + fullName, "You're now a member of " + fullName + " on We Are Curious.")
		}

		def invitedAdminIds = GroupMemberInvitedAdmin.lookupMemberIds(this.id)

		for (Long userId in invitedAdminIds) {
			removeInvitedAdmin(userId)
			addReader(userId)
			addWriter(userId)
			addAdmin(userId)

			User user = User.get(userId)

			user.notifyEmail("You've been invited to be an administrator of " + fullName, "You're now an administrator of " + fullName + " on We Are Curious.")
		}
	}

	def addReader(User user) {
		if (!user) return;

		GroupMemberReader.create(id, user.getId())

		def n = this.notifyNotifiedMajors("New user '" + user.getUsername() + "' joined group '" + this.description + "'",
						"New user '" + user.getUsername() + "' (" + user.getName() + " <" + user.getEmail() + ">) joined group '" + this.description + "'")

		def discussionIds = GroupMemberDiscussion.lookupMemberIds(id)
		if (discussionIds != null) {
			for (def discussionId : discussionIds) {
				UserActivity.create (
					null,
					UserActivity.ActivityType.ADD,
					UserActivity.ObjectType.READER,
					user.id,
					UserActivity.ObjectType.DISCUSSION,
					discussionId,
				)
			}
		}
		n
	}

	def addReader(Long userId) {
		if (!userId) return;

		addReader(User.get(userId))
	}

	def removeReader(User user) {
		if (!user) return;

		GroupMemberReader.delete(id, user.getId())

		def n = this.notifyNotifiedMajors("User '" + user.getUsername() + "' left group '" + this.description + "'",
						"User '" + user.getUsername() + "' (" + user.getName() + " <" + user.getEmail() + ">) left group '" + this.description + "'")

		def discussionIds = GroupMemberDiscussion.lookupMemberIds(id)
		if (discussionIds != null) {
			for (def discussionId : discussionIds) {
				UserActivity.create (
					null,
					UserActivity.ActivityType.REMOVE,
					UserActivity.ObjectType.READER,
					user.id,
					UserActivity.ObjectType.DISCUSSION,
					discussionId,
				)
			}
		}
		n
	}

	def removeReader(Long userId) {
		if (!userId) return;

		removeReader(User.get(userId))
	}

	def addInvited(User user) {
		if (!user) return;

		GroupMemberInvited.create(id, user.getId())

		//this.notifyNotifiedMajors("New user '" + user.getUsername() + "' invited to group '" + this.description + "'",
		//		"New user '" + user.getUsername() + "' (" + user.getName() + " <" + user.getEmail() + ">) invited to group '" + this.description + "'")
	}

	def addInvited(Long userId) {
		if (!userId) return;

		addInvited(User.get(userId))
	}

	def removeInvited(User user) {
		if (!user) return;

		GroupMemberInvited.delete(id, user.getId())

		//this.notifyNotifiedMajors("contact@wearecurio.us", "User '" + user.getUsername() + "' no longer invited to group '" + this.description + "'",
		//		"User '" + user.getUsername() + "' (" + user.getName() + " <" + user.getEmail() + ">) no longer invited to group '" + this.description + "'")
	}

	def removeInvited(Long userId) {
		if (!userId) return;

		removeInvited(User.get(userId))
	}

	def addInvitedAdmin(User user) {
		if (!user) return;

		GroupMemberInvitedAdmin.create(id, user.getId())

		//this.notifyNotifiedMajors("New user '" + user.getUsername() + "' invited to admin group '" + this.description + "'",
		//		"New user '" + user.getUsername() + "' (" + user.getName() + " <" + user.getEmail() + ">) invited to admin group '" + this.description + "'")
	}

	def addInvitedAdmin(Long userId) {
		if (!userId) return;

		addInvitedAdmin(User.get(userId))
	}

	def removeInvitedAdmin(User user) {
		if (!user) return;

		GroupMemberInvitedAdmin.delete(id, user.getId())

		//this.notifyNotifiedMajors("User '" + user.getUsername() + "' no longer invited to admin group '" + this.description + "'",
		//		"User '" + user.getUsername() + "' (" + user.getName() + " <" + user.getEmail() + ">) no longer invited to admin group '" + this.description + "'")
	}

	def removeInvitedAdmin(Long userId) {
		if (!userId) return;

		removeInvitedAdmin(User.get(userId))
	}

	def removeAllParticipants() {
		removeAllInvited()
		removeAllInvitedAdmin()
		removeAllReaders()
		removeAllWriters()
		removeAllAdmins()
	}

	def removeAllReaders() {
		GroupMemberReader.executeUpdate("DELETE GroupMemberReader gmr WHERE gmr.groupId = :groupId", [groupId: id])
	}

	def removeAllInvited() {
		GroupMemberInvited.executeUpdate("DELETE GroupMemberInvited gm WHERE gm.groupId = :groupId", [groupId: id])
	}

	def removeAllInvitedAdmin() {
		GroupMemberInvited.executeUpdate("DELETE GroupMemberInvitedAdmin gm WHERE gm.groupId = :groupId", [groupId: id])
	}

	def removeAllWriters() {
		GroupMemberWriter.executeUpdate("DELETE GroupMemberWriter gmw WHERE gmw.groupId = :groupId", [groupId: id])
	}

	def removeAllAdmins() {
		GroupMemberAdmin.executeUpdate("DELETE GroupMemberAdmin gma WHERE gma.groupId = :groupId", [groupId: id])
	}

	def hasReader(User user) {
		if (!user) return false

		return GroupMemberReader.lookup(id, user.getId()) != null
	}

	def hasReader(Long userId) {
		if (!userId) return false

		return GroupMemberReader.lookup(id, userId) != null
	}

	def hasInvited(User user) {
		if (!user) return false

		return GroupMemberInvited.lookup(id, user.getId()) != null
	}

	def hasInvited(Long userId) {
		if (!userId) return false

		return GroupMemberInvited.lookup(id, userId) != null
	}

	def hasInvitedAdmin(User user) {
		if (!user) return false

		return GroupMemberInvitedAdmin.lookup(id, user.getId()) != null
	}

	def hasInvitedAdmin(Long userId) {
		if (!userId) return false

		return GroupMemberInvitedAdmin.lookup(id, userId) != null
	}

	def addWriter(User user) {
		if (!user) return;

		GroupMemberWriter.create(id, user.getId())
	}

	def addWriter(Long userId) {
		if (!userId) return;

		GroupMemberWriter.create(id, userId)
	}

	def removeWriter(User user) {
		if (!user) return;

		removeWriter(user.id)
	}

	def removeWriter(Long userId) {
		if (!userId) return;

		GroupMemberReader.delete(id, userId)
		GroupMemberWriter.delete(id, userId)
	}

	boolean hasWriter(User user) {
		if (!user) return false
		hasWriter(user.id)
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
		if (!user) return;
		
		return addAdmin(user.id)		
	}

	def removeAdmin(User user) {
		if (!user) return;

		removeAdmin(user.id)
	}

	boolean hasAdmin(User user) {
		if (!user) return false

		return hasAdmin(id, user.id)
	}

	def addAdmin(Long userId) {
		if (!userId) return;

		addReader(userId)
		addWriter(userId)
		if (defaultNotify) {
			addNotified(userId)
			addNotifiedMajor(userId)
		}
		def a = GroupMemberAdmin.create(id, userId)
		def discussionIds = GroupMemberDiscussion.lookupMemberIds(id)
		if (discussionIds != null) {
			for (def discussionId : discussionIds) {
				UserActivity.create (
					null,
					UserActivity.ActivityType.ADD,
					UserActivity.ObjectType.ADMIN,
					userId,
					UserActivity.ObjectType.DISCUSSION,
					discussionId,
				)
			}
		}
		a
	}

	def removeAdmin(Long userId) {
		if (!userId) return;

		def discussionIds = GroupMemberDiscussion.lookupMemberIds(id)
		if (discussionIds != null) {
			for (def discussionId : discussionIds) {
				UserActivity.create (
					null,
					UserActivity.ActivityType.REMOVE,
					UserActivity.ObjectType.ADMIN,
					userId,
					UserActivity.ObjectType.DISCUSSION,
					discussionId,
				)
			}
		}
		GroupMemberAdmin.delete(id, userId)
	}

	boolean hasAdmin(Long userId) {
		if (!userId) return false

		return hasAdmin(id, userId)
	}

	static boolean hasAdmin(Long groupId, Long userId) {
		return GroupMemberAdmin.lookup(groupId, userId) != null
	}

	def addNotified(User user) {
		if (!user) return;

		GroupMemberNotified.create(id, user.getId())
	}

	def removeNotified(User user) {
		if (!user) return;

		GroupMemberNotified.delete(id, user.getId())
	}

	def hasNotified(User user) {
		if (!user) return false

		return GroupMemberNotified.lookup(id, user.getId()) != null
	}

	def addNotified(Long userId) {
		if (!userId) return;

		GroupMemberNotified.create(id, userId)
	}

	def removeNotified(Long userId) {
		if (!userId) return;

		GroupMemberNotified.delete(id, userId)
	}

	def hasNotified(long userId) {
		if (!userId) return false

		return GroupMemberNotified.lookup(id, userId) != null
	}

	def addNotifiedMajor(User user) {
		if (!user) return;

		GroupMemberNotifiedMajor.create(id, user.getId())
	}

	def removeNotifiedMajor(User user) {
		if (!user) return;

		GroupMemberNotified.delete(id, user.getId())
	}

	def hasNotifiedMajor(User user) {
		if (!user) return false

		return GroupMemberNotifiedMajor.lookup(id, user.getId()) != null
	}

	def addNotifiedMajor(Long userId) {
		if (!userId) return;

		GroupMemberNotifiedMajor.create(id, userId)
	}

	def removeNotifiedMajor(Long userId) {
		if (!userId) return;

		GroupMemberNotified.delete(id, userId)
	}

	def hasNotifiedMajor(Long userId) {
		if (!userId) return false

		return GroupMemberNotifiedMajor.lookup(id, userId) != null
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
		if (!user) return;

		addReader(user)

		if (!isReadOnly)
			addWriter(user)
	}

	def removeMember(User user) {
		if (!user) return;

		removeInvited(user)
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

	def getInvitedUsers() {
		return User.executeQuery("select user from User user, GroupMemberInvited item where item.groupId = :id and user.id = item.memberId",
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

	private sendMessages(people, String subjectString, String message) {
		for (User p in people) {
			if (p.getEmail() != null) {
				log.debug("Sending email about '" + subjectString + "' to " + p.getEmail())
				EmailService.get().send(p.getEmail(), subjectString, message)
			}
		}
	}

	private sendMessages(people, Closure subjectClosure, Closure messageClosure) {
		for (User p in people) {
			String subjectString = subjectClosure(p, fromAddress)
			String message = messageClosure(p, fromAddress)
			if (p.getEmail() != null) {
				log.debug("Sending email about '" + subjectString + "' to " + p.getEmail())
				EmailService.get().send(p.getEmail(), subjectString, message)
			}
		}
	}

	def notifyAdmins(String subject, String message) {
		sendMessages(getAdminUsers(), subject, message)
	}

	def notifyNotifieds(String subject, String message) {
		sendMessages(getNotifiedUsers(), subject, message)
	}

	def notifyNotifiedMajors(String subject, String message) {
		sendMessages(getNotifiedMajorUsers(), subject, message)
	}

	def notifyReaders(String subject, String message) {
		sendMessages(getReaderUsers(), subject, message)
	}

	def notifyReaders(Closure subjectClosure, Closure messageClosure) {
		sendMessages(getReaderUsers(), subjectClosure, messageClosure)
	}

	def notifyInvited(String subject, String message) {
		sendMessages(getInvitedUsers(), subject, message)
	}

	def notifyInvited(Closure subjectClosure, Closure messageClosure) {
		sendMessages(getInvitedUsers(), subjectClosure, messageClosure)
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

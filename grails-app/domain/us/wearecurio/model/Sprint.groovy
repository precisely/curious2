package us.wearecurio.model

import java.util.Date;
import java.util.Map;
import java.util.Locale;

import grails.converters.*
import grails.gorm.DetachedCriteria

import org.apache.commons.logging.LogFactory

import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.utility.Utils
import us.wearecurio.model.RepeatType
import us.wearecurio.model.Entry.DurationType
import us.wearecurio.model.UserGroup
import us.wearecurio.services.EmailService
import us.wearecurio.support.EntryStats;
import us.wearecurio.model.Model.Visibility

import java.text.DateFormat

import org.joda.time.*

class Sprint {
	
	/**
	 * Entries associated with a sprint are in GMT, coded to date 1/1/2001
	 */
	
	private static def log = LogFactory.getLog(this)
	
	String hash
	Long userId // user id of creator of sprint
	Long virtualGroupId // id of UserGroup for keeping track of sprint members and admins
	Long virtualUserId // user id for virtual user (owner of sprint tags/entries)
	String name
	String description
	Date created
	Date updated
	Long daysDuration
	Date startDate
	Visibility visibility
	String tagName
	
	static final int MAXPLOTDATALENGTH = 1024
	
	static Date sprintBaseDate
	
	static {
		TimeZone gmtTimeZone = TimeZoneId.getUTCTimeZone()
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
		dateFormat.setTimeZone(gmtTimeZone)
		sprintBaseDate = dateFormat.parse("Jan 1, 2001 12:00 am")
	}
	
	static Date getSprintBaseDate() { return sprintBaseDate }
	
	static constraints = {
		userId(nullable:true)
		// This needs to be uncommented once migrations have run on all the systems
		hash(/*blank: false, unique: true,*/ nullable: true)
		name(nullable:true)
		description(nullable:true, maxSize:10000)
		daysDuration(nullable:true)
		startDate(nullable:true)
		tagName(nullable:true)
		visibility(nullable:true)
	}
	
	static mapping = {
		version false
		hash column: 'hash', index: 'hash_index'
		name column: 'name', index:'name_index'
		userId column: 'user_id', index:'user_id_index'
		virtualGroupId column: 'virtual_group_id', index:'virtual_group_id_index'
		virtualUserId column: 'virtual_user_id', index:'virtual_user_id_index'
		created column: 'created', index:'created_index'
		updated column: 'updated', index:'updated_index'
	}
	
	static searchable = {
		only = [ 'userId', 'virtualGroupId', 'virtualUserId', 'name', 'description', 'created', 'updated', 'daysDuration', 'startDate', 'visibility']
	}
	
	static Sprint create(User user) {
		return create(new Date(), user, null, null)
	}
	
	UserGroup fetchUserGroup() {
		return UserGroup.get(virtualGroupId)
		
	}
	
	boolean hasWriter(Long userId) {
		return fetchUserGroup()?.hasWriter(userId)
	}
	
	def addWriter(Long userId) {
		return fetchUserGroup()?.addWriter(userId)
	}
	
	def removeWriter(Long userId) {
		return fetchUserGroup()?.removeWriter(userId)
	}
	
	boolean hasReader(Long userId) {
		return fetchUserGroup()?.hasReader(userId)
	}
	
	def addReader(Long userId) {
		fetchUserGroup()?.addReader(userId)
	}
	
	def removeReader(Long userId) {
		fetchUserGroup()?.removeReader(userId)
	}
	
	boolean hasAdmin(Long userId) {
		return fetchUserGroup()?.hasAdmin(userId)
	}
	
	def addAdmin(Long userId) {
		return fetchUserGroup()?.addAdmin(userId)
	}
	
	boolean hasInvited(Long userId) {
		return fetchUserGroup()?.hasInvited(userId)
	}
	
	def addInvited(Long userId) {
		fetchUserGroup()?.addInvited(userId)
	}
	
	def removeInvited(Long userId) {
		fetchUserGroup()?.removeInvited(userId)
	}
	
	boolean hasInvitedAdmin(Long userId) {
		return fetchUserGroup()?.hasInvitedAdmin(userId)
	}
	
	def addInvitedAdmin(Long userId) {
		return fetchUserGroup()?.addInvitedAdmin(userId)
	}
	
	void clearInvited() {
		UserGroup group = fetchUserGroup()
		group?.removeAllInvited()
		group?.removeAllInvitedAdmin()
	}
	
	Long getEntriesCount() {
		return Entry.countByUserId(virtualUserId)
	}
	
	Long getParticipantsCount() {
		List participantsIdList = GroupMemberReader.findAllByGroupId(virtualGroupId)*.memberId

		Long participantsCount = User.createCriteria().count {
				'in'("id", participantsIdList ?: [0l])
				or {
					isNull("virtual")
					ne("virtual", true)
				}
			}
		return participantsCount
	}

	def removeAdmin(Long userId) {
		return fetchUserGroup()?.removeAdmin(userId)
	}
	
	static Sprint create(Date now, User user, String name, Visibility visibility) {
		log.debug "Sprint.create() userId:" + user?.getId() + ", name:" + name
		return Sprint.withTransaction {
			def sprint = new Sprint(user, name, visibility)
			Utils.save(sprint, true)
			UserActivity.create(new Date(), user.id, UserActivity.SPRINT_BIT | UserActivity.CREATE_ID, sprint.id)
			return sprint
		}
	}
	
	static void delete(Sprint sprint) {
		log.debug "Sprint.delete() id:" + sprint.getId()
		// DiscussionPost.executeUpdate("delete DiscussionPost p where p.discussionId = :id", [id:discussion.getId()]);
		Long userId = sprint.userId
		sprint.userId = 0
		UserGroup sprintUserGroup = sprint.fetchUserGroup()
		sprintUserGroup?.removeAllParticipants()
		
		Utils.save(sprint, true)
		UserActivity.create(new Date(), userId, UserActivity.SPRINT_BIT | UserActivity.DELETE_ID, sprint.id)
	}
	
	Sprint() {
		this.visibility = Visibility.NEW
	}
	
	protected createUserGroupFullName() {
		return this.name + " Tracking Sprint"
	}
	
	Sprint(User user, String name, Visibility visibility) {
		this.userId = user?.getId()
		this.name = name
		this.hash = new DefaultHashIDGenerator().generate(12)
		this.created = new Date()
		this.updated = this.created
		this.visibility = visibility
		String uniqueId = UUID.randomUUID().toString()
		UserGroup virtualUserGroup = UserGroup.createVirtual(createUserGroupFullName())
		this.virtualGroupId = virtualUserGroup.id
		User virtualUser = User.createVirtual() // create user who will own sprint entries, etc.
		addAdmin(user.id)
		virtualUserGroup.addWriter(virtualUser)
		virtualUserGroup.addReader(virtualUser)
		this.virtualUserId = virtualUser.id
		/*
		Discussion discussion = Discussion.create(virtualUser, "Tracking Sprint: " + name, virtualUserGroup, null)
		this.discussionId = discussion.id
		*/
		this.description = null
	}
	
	void addMember(Long userId) {
		addReader(userId)
		addWriter(userId)
	}
	
	void removeMember(Long userId) {
		removeReader(userId)
		removeWriter(userId)
		removeAdmin(userId)
	}
	
	boolean hasMember(Long userId) {
		return hasReader(userId)
	}
	
	// returns start time if started as of "now", or null if outside sprint
	Date hasStarted(Long userId, Date now) {
		if (!hasMember(userId)) 
			return null
		
		UserActivity start = UserActivity.fetchStart(userId, this.id, UserActivity.SPRINT_BIT | UserActivity.START_ID, now)
		
		return start?.created
	}
	
	// returns end time if ended as of "now", or null if outside sprint
	Date hasEnded(Long userId, Date now) {
		if (!hasMember(userId))
			return null
			
		UserActivity end = UserActivity.fetchEnd(userId, this.id, UserActivity.SPRINT_BIT | UserActivity.START_ID, now)
		
		return end?.created
	}
	
	protected String entrySetName() {
		return "sprint id: " + this.id
	}
	
	void setName(String name) {
		this.name = name
	}
	
	Sprint update(Map map) {
		log.debug "Sprint.update() this:" + this + ", map:" + map
		
		this.updated = new Date()
		
		if (map['name']) {
			this.name = map['name']
			this.tagName = null
			this.fetchTagName()
			UserGroup group = fetchUserGroup()
			group.setFullName(createUserGroupFullName())
			Utils.save(group, true)
		}
		if (map['description']) this.description = map['description']
		if (map['daysDuration']) this.daysDuration = map['daysDuration']
		if (map['startDate']) this.startDate = map['startDate']
		if (map['visibility']) this.visibility = map['visibility']
		
		Utils.save(this, true)
		
		fetchUserGroup()?.acceptAllInvited()

		return this
	}
	
	String fetchTagName() {
		if (tagName) return tagName
		
		tagName = name.toLowerCase()
			.replaceAll(~/[^a-zA-Z ]+/, '')		// Removes all special characters and numbers
			.replaceAll(~/ +/, ' ')			// Replaces all multiple spaces with single space
			.trim() + ' sprint'			// Removes spaces from the start and the end of the string
		
		return tagName
	}
	
	// add sprint entries to user account in the specified time zone
	boolean start(Long userId, Date baseDate, Date now, String timeZoneName, EntryStats stats) {
		if (!hasMember(userId))
			return false
		
		if (hasStarted(userId, now))
			return false
		
		stats.setBatchCreation(true)
		
		String setName = entrySetName()
		
		def entries = Entry.findAllByUserId(virtualUserId)
		for (Entry entry in entries) {
			if (!entry.isPrimaryEntry())
				continue
			
			if (entry.getRepeatType() == null)
				continue
			
			entry.activateTemplateEntry(userId, baseDate, now, timeZoneName, stats, setName)
		}
		
		// add start element
		def m = Entry.parse(now, timeZoneName, fetchTagName() + " start", baseDate, true)
		def entry = Entry.create(userId, m, stats)
		
		// record activity
		UserActivity.create(now, userId, UserActivity.SPRINT_BIT | UserActivity.START_ID, this.id)
		
		return true
	}
	
	// remove sprint entries from user account for current base date
	// remove pinned entries outright
	// end repeat entries
	// end remind entries
	boolean stop(Long userId, Date baseDate, Date now, String timeZoneName, EntryStats stats) {
		Date startDate = hasStarted(userId, now)

		if (!startDate) return false
				
		stats.setBatchCreation(true)
		
		String setName = entrySetName()
		
		Identifier setIdentifier = Identifier.look(setName)
		
		// look for entries marked by sprint set identifier
		def entries = Entry.findAllByUserIdAndSetIdentifier(userId, setIdentifier)
		for (Entry entry in entries) {
			if (!entry.isPrimaryEntry())
				continue
			
			Entry.deleteGhost(entry, stats, baseDate, true)
		}
		
		// look for manually created entries that match sprint template
		def sprintEntries = Entry.findAllByUserId(virtualUserId)
		for (Entry entry in sprintEntries) {
			if (!entry.isPrimaryEntry())
				continue
			
			if (entry.getRepeatType() == null)
				continue
			
			boolean testDate = true
			if (entry.getRepeatType().isContinuous())
				testDate = false
				
			// search for entry that matches this pattern
			def c = Entry.createCriteria()
			def results = c {
				and {
					eq("userId", userId)
					if (testDate) {
						ge("date", startDate)
						lt("date", startDate + 1)
					}
					eq("tag", entry.getTag())
					eq("repeat", entry.repeat)
					isNull("setIdentifier")
				}
			}

			for (Entry e in results) {
				Entry.deleteGhost(e, stats, baseDate, true)
			}
		}
		
		// add stop element
		def m = Entry.parse(now, timeZoneName, fetchTagName() + " end", baseDate, true)
		def entry = Entry.create(userId, m, stats)

		// record activity
		UserActivity.create(now, userId, UserActivity.SPRINT_BIT | UserActivity.STOP_ID, this.id)
	}
	
	boolean isModified() {
		return this.created.getTime() != this.updated.getTime()
	}
	
	/**
	 * Get data for the discussion data lists
	 */
	def getJSONDesc() {
		return [
			id: this.id,
			hash: this.hash,
			name: this.name?:'New Sprint',
			userId: this.userId,
			description: this.description,
			totalParticipants: this.getParticipantsCount(),
			totalTags: this.getEntriesCount(),
			virtualUserId: this.virtualUserId,
			virtualGroupId: this.virtualGroupId,
			created: this.created,
			updated: this.updated,
			type: "spr"
		]
	}
	
	static List<Sprint> getSprintListForUser(Long userId, int max, int offset) {
		if (!userId) {
			return []
		}
		
		max = max ?: 5;
		offset = offset ?: 0;

		List<Long> groupReaderList = new DetachedCriteria(GroupMemberReader).build {
			projections {
				property "groupId"
			}
			eq "memberId", userId
		}.list()
		
		List<Sprint> sprintList = Sprint.withCriteria {
			and {
				or {
					'in'("virtualGroupId", groupReaderList ?: [0l]) // When using 'in' clause GORM gives error on passing blank list
					and {
						eq("visibility", Visibility.PUBLIC)
						ne("userId", 0l)
					}
				}
				not {
					eq("visibility", Visibility.NEW)
				}
			}
			firstResult(offset)
			maxResults(max)
			order("updated", "desc")
		}
		
		return sprintList*.getJSONDesc()
	}

	List<User> getParticipants(int max, int offset) {
		if (!userId) {
			return []
		}
		
		max = Math.min(max ?: 10, 100)
		offset = offset ?: 0

		List<Long> participantIdsList = new DetachedCriteria(GroupMemberReader).build {
			projections {
				property "memberId"
			}
			eq "groupId", virtualGroupId
		}.list()

		// Fetch all non-virtual or actual users
		List<User> participantsList = User.withCriteria {
				'in'("id", participantIdsList ?: [0l])
				or {
					isNull("virtual")
					ne("virtual", true)
				}
				maxResults(max)
				firstResult(offset)
			}
	
		return participantsList
	}

	String toString() {
		return "Sprint(id:" + getId() + ", userId:" + userId + ", name:" + name + ", created:" + Utils.dateToGMTString(created) \
				+ ", updated:" + Utils.dateToGMTString(updated) + ", visibility:" + visibility + ")"
	}
}

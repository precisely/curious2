package us.wearecurio.model

import java.util.Date;
import java.util.Map;
import java.util.Locale;

import grails.converters.*
import grails.gorm.DetachedCriteria

import org.apache.commons.logging.LogFactory

import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.utility.Utils
import us.wearecurio.model.Entry.RepeatType
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
	
	String hashid
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
		hashid(/*blank: false, unique: true,*/ nullable: true)
		name(nullable:true)
		description(nullable:true, maxSize:10000)
		daysDuration(nullable:true)
		startDate(nullable:true)
		tagName(nullable:true)
		visibility(nullable:true)
	}
	
	static mapping = {
		version false
		hashid column: 'hashid', index: 'hashid_index'
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
		return create(user, null, null)
	}
	
	protected UserGroup fetchUserGroup() {
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
	
	static Sprint create(User user, String name, Visibility visibility) {
		log.debug "Sprint.create() userId:" + user?.getId() + ", name:" + name
		return Sprint.withTransaction {
			def sprint = new Sprint(user, name, visibility)
			Utils.save(sprint, true)
			return sprint
		}
	}
	
	static void delete(Sprint sprint) {
		log.debug "Sprint.delete() id:" + sprint.getId()
		// DiscussionPost.executeUpdate("delete DiscussionPost p where p.discussionId = :id", [id:discussion.getId()]);
		sprint.userId = 0
		UserGroup sprintUserGroup = sprint.fetchUserGroup()
		sprintUserGroup?.removeAllParticipants()
		
		Utils.save(sprint, true)
	}
	
	static boolean search(Long userId, String searchString) {
		
	}
	
	Sprint() {
		this.visibility = Visibility.PRIVATE
	}
	
	Sprint(User user, String name, Visibility visibility) {
		this.userId = user?.getId()
		this.name = name
		this.hashid = new DefaultHashIDGenerator().generate(12)
		this.created = new Date()
		this.updated = this.created
		this.visibility = visibility
		String uniqueId = UUID.randomUUID().toString()
		UserGroup virtualUserGroup = UserGroup.createVirtual("Sprint Group " + name + " " + uniqueId)
		this.virtualGroupId = virtualUserGroup.id
		User virtualUser = User.createVirtual() // create user who will own sprint entries, etc.
		addAdmin(user.id)
		virtualUserGroup.addWriter(virtualUser)
		virtualUserGroup.addReader(virtualUser)
		this.virtualUserId = virtualUser.id
		/*
		Discussion discussion = Discussion.create(virtualUser, "Tracking Sprint: " + name, virtualUserGroup)
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
	
	boolean hasStarted(Long userId, Date now) {
		if (!hasMember(userId)) 
			return false
		
		Entry entry = Entry.fetchPreviousEqualStartOrEndEntry(userId, Tag.look(fetchTagName()), now)
		if (!entry) {
			return false
		}
		return entry.fetchIsStart()
	}
	
	boolean hasEnded(Long userId, Date now) {
		if (!hasMember(userId))
			return false
		Entry entry = Entry.fetchPreviousEqualStartOrEndEntry(userId, Tag.look(fetchTagName()), now)
		if (!entry) {
			return false
		}
		return entry.fetchIsEnd()
	}
	
	protected String entrySetName() {
		return "sprint id: " + this.id
	}
	
	void setName(String name) {
		this.name = name
		
		this.tagName = null
		
		this.fetchTagName()
	}
	
	protected String fetchTagName() {
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
		m['durationType'] = DurationType.GENERATEDSTART
		def entry = Entry.create(userId, m, stats)
		
		return true
	}
	
	// remove sprint entries from user account for current base date
	// remove pinned entries outright
	// end repeat entries
	// end remind entries
	boolean stop(Long userId, Date baseDate, Date now, String timeZoneName, EntryStats stats) {
		if (!hasStarted(userId, now)) {
			return false
		}
		
		stats.setBatchCreation(true)
		
		String setName = entrySetName()
		
		Identifier setIdentifier = Identifier.look(setName)
		
		def entries = Entry.findAllByUserIdAndSetIdentifier(userId, setIdentifier)
		for (Entry entry in entries) {
			if (!entry.isPrimaryEntry())
				continue
			
			Entry.deleteGhost(entry, stats, baseDate, true)
		}
		
		// add stop element
		def m = Entry.parse(now, timeZoneName, fetchTagName() + " end", baseDate, true)
		m['durationType'] = DurationType.GENERATEDEND
		def entry = Entry.create(userId, m, stats)
		entry = entry
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
			hashid: this.hashid,
			name: this.name?:'New Sprint',
			userId: this.userId,
			description: this.description,
			totalParticipants: this.getParticipantsCount(),
			totalTags: this.getEntriesCount(),
			virtualUserId: this.virtualUserId,
			virtualGroupId: this.virtualGroupId,
			created: this.created,
			updated: this.updated
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
			or {
				'in'("virtualGroupId", groupReaderList ?: [0l]) // When using 'in' clause GORM gives error on passing blank list
				and {
					eq("visibility", Visibility.PUBLIC)
					ne("userId", 0l)
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

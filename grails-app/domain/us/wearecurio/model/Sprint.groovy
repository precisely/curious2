package us.wearecurio.model

import java.util.Date;
import java.util.Map;

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils
import us.wearecurio.model.Entry.RepeatType
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
	
	Long userId // user id of creator of sprint
	Long virtualGroupId // id of UserGroup for keeping track of sprint members and admins
	Long virtualUserId // user id for virtual user (owner of sprint tags/entries)
	Long discussionId // id of discussion associated with sprint
	String name
	String description
	Date created
	Date updated
	Long daysDuration
	Date startDate
	Visibility visibility
	
	static final int MAXPLOTDATALENGTH = 1024
	
	static Date sprintBaseDate
	
	static {
		TimeZone gmtTimeZone = TimeZoneId.getUTCTimeZone()
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(gmtTimeZone)
		sprintBaseDate = dateFormat.parse("January 1, 2001 12:00 am")
	}

	static constraints = {
		userId(nullable:true)
		name(nullable:true)
		discussionId(nullable:true)
		description(nullable:true, maxSize:10000)
		daysDuration(nullable:true)
		startDate(nullable:true)
	}
	
	static mapping = {
		version false
		name column: 'name', index:'name_index'
		userId column: 'user_id', index:'user_id_index'
		virtualGroupId column: 'virtual_group_id', index:'virtual_group_id_index'
		virtualUserId column: 'virtual_user_id', index:'virtual_user_id_index'
		discussionId column: 'discussion_id', index:'discussion_id_index'
		created column: 'created', index:'created_index'
		updated column: 'updated', index:'updated_index'
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
		Sprint.withTransaction {
			// TODO: remove users from sprint groups, etc.
			// DiscussionPost.executeUpdate("delete DiscussionPost p where p.discussionId = :id", [id:discussion.getId()]);
			sprint.delete()
		}
	}
	
	static boolean search(Long userId, String searchString) {
		
	}
	
	Sprint() {
	}
	
	Sprint(User user, String name, Visibility visibility) {
		this.userId = user?.getId()
		this.name = name
		this.created = new Date()
		this.updated = this.created
		this.visibility = visibility
		UserGroup virtualUserGroup = UserGroup.createVirtual("Sprint Group " + name)
		this.virtualGroupId = virtualUserGroup.id
		User virtualUser = User.createVirtual() // create user who will own sprint entries, etc.
		virtualUserGroup.addWriter(virtualUser)
		virtualUserGroup.addReader(virtualUser)
		this.virtualUserId = virtualUser.id
		Discussion discussion = Discussion.create(virtualUser, "Tracking Sprint: " + name, virtualUserGroup)
		this.discussionId = discussion.id
		this.description = null
	}
	
	void addMember(Long userId) {
		addReader(userId)
	}
	
	void removeMember(Long userId) {
		removeReader(userId)
	}
	
	boolean hasMember(Long userId) {
		return hasReader(userId)
	}
	
	void addAdmin(Long userId) {
		addWriter(userId)
		addReader(userId)
	}
	
	void removeAdmin(Long userId) {
		removeWriter(userId)
		removeReader(userId)
	}
	
	boolean hasAdmin(Long userId) {
		return hasWriter(userId)
	}
	
	protected String entrySetName() {
		return "sprint id: " + this.id
	}
	
	protected String sprintTagName() {
		return "sprint " + name.toLowerCase()
	}
	
	// add sprint entries to user account in the specified time zone
	boolean start(Long userId, Date baseDate, Date now, String timeZoneName, EntryStats stats) {
		String setName = entrySetName()
		
		def entries = Entry.findByUserId(virtualUserId)
		for (Entry entry in entries) {
			if (!entry.isPrimaryEntry())
				continue
			
			entry.activateTemplateEntry(userId, baseDate, now, timeZoneName, stats, setName)
		}
		
		// add start and stop elements
		
	}
	
	// remove sprint entries from user account for current base date
	// remove pinned entries outright
	// end repeat entries
	// end remind entries
	boolean stop(Long userId, Date baseDate, EntryStats stats) {
		String setName = entrySetName()
		
		Identifier setIdentifier = Identifier.look(setName)
		
		def entries = Entry.findByUserIdAndSetIdentifier(userId, setIdentifier)
		for (Entry entry in entries) {
			if (!entry.isPrimaryEntry())
				continue
			
			if (entry.isGhost()) {
				Entry.deleteGhost(entry, stats, baseDate, true)
			} else
				Entry.delete(entry, stats)
		}
	}
	
	boolean isModified() {
		return this.created.getTime() != this.updated.getTime()
	}
	
	/**
	 * Get data for the discussion data lists
	 */
	def getJSONDesc() {
		return [
			id:this.id,
			name:this.name?:'New Sprint',
			userId:this.userId,
			created:this.created,
			updated:this.updated
		]
	}

	String toString() {
		return "Sprint(id:" + getId() + ", userId:" + userId + ", name:" + name + ", created:" + Utils.dateToGMTString(created) \
				+ ", updated:" + Utils.dateToGMTString(updated) + ", visibility:" + visibility + ")"
	}
}

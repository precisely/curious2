package us.wearecurio.model

import java.util.Map;

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils
import us.wearecurio.model.Entry.RepeatType
import us.wearecurio.services.EmailService
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
	}
	
	static mapping = {
		name column: 'name', index:'name_index'
		virtualGroupId column: 'virtual_group_id', index:'virtual_group_id_index'
		virtualUserId column: 'virtual_user_id', index:'virtual_user_id_index'
		discussionId column: 'discussion_id', index:'discussion_id_index'
		created column: 'created', index:'created_index'
		updated column: 'updated', index:'updated_index'
	}
	
	public static Sprint create(User user) {
		return create(user, null)
	}
	
	public static Sprint create(User user, String name, Visibility visibility) {
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
	
	public Sprint() {
	}
	
	public Sprint(User user, String name, Visibility visibility) {
		this.userId = user?.getId()
		this.name = name
		this.created = new Date()
		this.updated = this.created
		this.visibility = visibility
		UserGroup virtualUserGroup = UserGroup.createVirtual("Sprint Group " + name)
		this.virtualGroupId = virtualUserGroup.id
		User virtualUser = User.createVirtual() // create user who will own sprint entries, etc.
		this.virtualUserId = virtualUser.id
		Discussion discussion = Discussion.create(virtualUser, "Tracking Sprint: " + name, virtualUserGroup)
		this.discussionId = discussion.id
		this.description = null
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

package us.wearecurio.model;

import java.util.Date;

import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users and 
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class GroupMemberInvited {

	private static def log = LogFactory.getLog(this)

	Date created
	Long groupId
	Long memberId
	
	static constraints = {
		unique: ['groupId', 'memberId']
	}
	
	static mapping = {
		version false
 		groupId column: 'group_id', index:'group_id_index'
		memberId column: 'member_id', index:'member_id_index'
	}
	
	public static delete(GroupMemberInvited item) {
		if (item) {
			item.delete(flush: true)
			return true
		}
		
		return false
	}
	
	public static GroupMemberInvited create(Long groupId, Long memberId) {
		def item = lookup(groupId, memberId)
		
		if (item) return item
		
		item = new GroupMemberInvited(groupId, memberId)
		
		Utils.save(item, true)
		
		return item
	}
	
	GroupMemberInvited() {
	}
	
	GroupMemberInvited(Long groupId, Long memberId) {
		created = new Date()
		this.groupId = groupId
		this.memberId = memberId
	}
	
	public static delete(Long groupId, Long memberId) {
		return delete(GroupMemberInvited.findByGroupIdAndMemberId(groupId, memberId))
	}
	
	public static lookupMemberIds(Long groupId) {
		return GroupMemberInvited.executeQuery("SELECT item.memberId FROM GroupMemberInvited item WHERE item.groupId = :id",
				[id:groupId])
	}
	
	public static lookupGroupIds(Long memberId) {
		return GroupMemberInvited.executeQuery("SELECT item.groupId FROM GroupMemberInvited item WHERE item.memberId = :id",
				[id:memberId])
	}
	
	public static lookup(Long groupId, memberId) {
		return GroupMemberInvited.findByGroupIdAndMemberId(groupId, memberId)
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", memberId:" + memberId + ", groupId:" + groupId + ")"
	}
}

package us.wearecurio.model;

import java.util.Date;

import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users and 
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class GroupMemberNotified {

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
	
	public static delete(GroupMemberNotified item) {
		if (item) {
			item.delete()
			return true
		}
		
		return false
	}
	
	public static GroupMemberNotified create(Long groupId, Long memberId) {
		def item = lookup(groupId, memberId)
		
		if (item) return item
		
		item = new GroupMemberNotified(groupId, memberId)
		
		Utils.save(item, true)
		
		return item
	}
	
	GroupMemberNotified() {
	}
	
	GroupMemberNotified(Long groupId, Long memberId) {
		created = new Date()
		this.groupId = groupId
		this.memberId = memberId
	}
	
	public static delete(Long groupId, Long memberId) {
		return delete(GroupMemberNotified.findByGroupIdAndMemberId(groupId, memberId))
	}
	
	public static lookupMemberIds(Long groupId) {
		return GroupMemberNotified.executeQuery("SELECT item.memberId FROM GroupMemberNotified item WHERE item.groupId = :id",
				[id:groupId])
	}
	
	public static lookupGroupIds(Long memberId) {
		return GroupMemberNotified.executeQuery("SELECT item.groupId FROM GroupMemberNotified item WHERE item.memberId = :id",
				[id:memberId])
	}
	
	public static lookup(Long groupId, memberId) {
		return GroupMemberNotified.findByGroupIdAndMemberId(groupId, memberId)
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", memberId:" + memberId + ", groupId:" + groupId + ")"
	}
}

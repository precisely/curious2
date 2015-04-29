package us.wearecurio.model;

import java.util.Date;

import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users and 
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class GroupMemberNotifiedMajor {

	private static def log = LogFactory.getLog(this)

	Date created
	Long groupId
	Long memberId
	
	static constraints = {
		unique: ['groupId', 'memberId']
		memberId(unique:true)
	}
	
	static mapping = {
		version false
 		groupId column: 'group_id', index:'group_id_index'
		memberId column: 'member_id', index:'member_id_index'
	}
	
	public static delete(GroupMemberNotifiedMajor item) {
		if (item) {
			item.delete(flush: true)
			return true
		}
		
		return false
	}
	
	public static GroupMemberNotifiedMajor create(Long groupId, Long memberId) {
		def item = lookup(groupId, memberId)
		
		if (item) return item
		
		item = new GroupMemberNotifiedMajor(groupId, memberId)
		
		Utils.save(item, true)
		
		return item
	}
	
	GroupMemberNotifiedMajor() {
	}
	
	GroupMemberNotifiedMajor(Long groupId, Long memberId) {
		created = new Date()
		this.groupId = groupId
		this.memberId = memberId
	}
	
	public static delete(Long groupId, Long memberId) {
		return delete(GroupMemberNotifiedMajor.findByGroupIdAndMemberId(groupId, memberId))
	}
	
	public static lookupMemberIds(Long groupId) {
		return GroupMemberNotifiedMajor.executeQuery("SELECT item.memberId FROM GroupMemberNotifiedMajor item WHERE item.groupId = :id",
				[id:groupId])
	}
	
	public static lookupGroupIds(Long memberId) {
		return GroupMemberNotifiedMajor.executeQuery("SELECT item.groupId FROM GroupMemberNotifiedMajor item WHERE item.memberId = :id",
				[id:memberId])
	}
	
	public static lookup(Long groupId, memberId) {
		return GroupMemberNotifiedMajor.findByGroupIdAndMemberId(groupId, memberId)
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", memberId:" + memberId + ", groupId:" + groupId + ")"
	}
}

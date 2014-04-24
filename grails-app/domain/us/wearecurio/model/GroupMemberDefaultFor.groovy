package us.wearecurio.model;

import java.util.Date;

import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users and 
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class GroupMemberDefaultFor {

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
	
	public static delete(GroupMemberDefaultFor item) {
		if (item) {
			item.delete()
			return true
		}
		
		return false
	}
	
	public static GroupMemberDefaultFor create(Long groupId, Long memberId) {
		def item = lookup(groupId, memberId)
		
		if (item) return item
		
		// there should be only one default group per member
		while (item = GroupMemberDefaultFor.findByMemberId(memberId))
			delete(item)
		
		item = new GroupMemberDefaultFor(groupId, memberId)
		
		Utils.save(item, true)
		
		return item
	}
	
	GroupMemberDefaultFor() {
	}
	
	GroupMemberDefaultFor(Long groupId, Long memberId) {
		created = new Date()
		this.groupId = groupId
		this.memberId = memberId
	}
	
	public static delete(Long groupId, Long memberId) {
		return delete(GroupMemberDefaultFor.findByGroupIdAndMemberId(groupId, memberId))
	}
	
	public static lookupMemberIds(Long groupId) {
		return GroupMemberDefaultFor.executeQuery("SELECT item.memberId FROM GroupMemberDefaultFor item WHERE item.groupId = :id",
				[id:groupId])
	}
	
	public static lookupGroup(User user) {
		if (!user) return null
		def item = GroupMemberDefaultFor.findByMemberId(user.getId())
		if (item) return UserGroup.get(item.groupId)
		return null
	}
	
	public static lookup(Long groupId, memberId) {
		return GroupMemberDefaultFor.findByGroupIdAndMemberId(groupId, memberId)
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", memberId:" + memberId + ", groupId:" + groupId + ")"
	}
}

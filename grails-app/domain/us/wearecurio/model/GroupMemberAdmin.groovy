package us.wearecurio.model

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils

/**
 * Represents a group for the purpose of collecting users and 
 */
class GroupMemberAdmin {

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
	
	public static delete(GroupMemberAdmin item) {
		if (item) {
			item.delete(flush: true)
			return true
		}
		
		return false
	}
	
	public static GroupMemberAdmin create(Long groupId, Long memberId) {
		def item = lookup(groupId, memberId)
		
		if (item) return item
		
		item = new GroupMemberAdmin(groupId, memberId)
		
		Utils.save(item, true)
		
		return item
	}
	
	GroupMemberAdmin() {
	}
	
	GroupMemberAdmin(Long groupId, Long memberId) {
		created = new Date()
		this.groupId = groupId
		this.memberId = memberId
	}
	
	public static delete(Long groupId, Long memberId) {
		return delete(GroupMemberAdmin.findByGroupIdAndMemberId(groupId, memberId))
	}
	
	public static lookupMemberIds(Long groupId) {
		return GroupMemberAdmin.executeQuery("SELECT item.memberId FROM GroupMemberAdmin item WHERE item.groupId = :id",
				[id:groupId])
	}
	
	public static lookupGroupIds(Long memberId) {
		return GroupMemberAdmin.executeQuery("SELECT item.groupId FROM GroupMemberAdmin item WHERE item.memberId = :id",
				[id:memberId])
	}
	
	public static lookup(Long groupId, memberId) {
		return GroupMemberAdmin.findByGroupIdAndMemberId(groupId, memberId)
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", memberId:" + memberId + ", groupId:" + groupId + ")"
	}
}

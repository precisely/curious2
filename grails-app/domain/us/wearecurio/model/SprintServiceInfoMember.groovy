package us.wearecurio.model;

import java.util.Date;

import org.apache.commons.logging.LogFactory

/**
 * Represents a group for the purpose of collecting users and 
 */

import grails.converters.*
import us.wearecurio.utility.Utils

class SprintServiceInfoMember {

	private static def log = LogFactory.getLog(this)

	Long groupId // sprint id
	Long memberId // external service info id
	
	static constraints = {
		unique: ['groupId', 'memberId']
	}
	
	static mapping = {
		version false
 		groupId column: 'group_id', index:'group_id_index'
		memberId column: 'member_id', index:'member_id_index'
	}
	
	public static delete(SprintServiceInfoMember item) {
		if (item) {
			item.delete(flush: true)
			return true
		}
		
		return false
	}
	
	public static SprintServiceInfoMember create(Long groupId, Long memberId) {
		def item = lookup(groupId, memberId)
		
		if (item) return item
		
		item = new SprintServiceInfoMember(groupId, memberId)
		
		Utils.save(item, true)
		
		return item
	}
	
	SprintServiceInfoMember() {
	}
	
	SprintServiceInfoMember(Long groupId, Long memberId) {
		this.groupId = groupId
		this.memberId = memberId
	}
	
	public static delete(Long groupId, Long memberId) {
		return delete(SprintServiceInfoMember.findByGroupIdAndMemberId(groupId, memberId))
	}
	
	public static lookupMemberIds(Long groupId) {
        if (groupId == null) return null
        
		return SprintServiceInfoMember.executeQuery("SELECT item.memberId FROM SprintServiceInfoMember item WHERE item.groupId = :id",
				[id:groupId])
	}
	
	public static lookupGroupIds(Long memberId) {
        if (memberId == null) return null
        
		return SprintServiceInfoMember.executeQuery("SELECT item.groupId FROM SprintServiceInfoMember item WHERE item.memberId = :id",
				[id:memberId])
	}
	
	public static lookup(Long groupId, memberId) {
        if( groupId == null || memberId == null) return null
        
		return SprintServiceInfoMember.findByGroupIdAndMemberId(groupId, memberId)
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", memberId:" + memberId + ", groupId:" + groupId + ")"
	}
}

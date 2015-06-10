package us.wearecurio.model

import java.util.Map

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.Entry.DurationType
import us.wearecurio.model.Entry.RepeatType
import us.wearecurio.utility.Utils

/**
 * Represents a group for the purpose of collecting users and 
 */
class UserActivity {
	
	private static def log = LogFactory.getLog(this)
	
	Date created
	Long userId
	Long typeId
	Long objectId
	Long otherId
	
	static final long CREATE_ID = 0L
	static final long DELETE_ID = 1L
	static final long FOLLOW_ID = 2L
	static final long UNFOLLOW_ID = 3L
	static final long START_ID = 4L
	static final long END_ID = 5L
	static final long STOP_ID = 5L
	static final long COMMENT_ID = 6L
	static final long UNCOMMENT_ID = 7L
	static final long INVITE_ID = 8L
	static final long UNINVITE_ID = 9L
	
	static final long SPRINT_BIT = 1L<<8
	static final long DISCUSSION_BIT = 1L<<9
	static final long DISCUSSIONPOST_BIT = 1L<<10
	static final long USER_BIT = 1L<<11
	
	static mapping = {
		version false
		created column: 'created', index:'created_index'
		userId column: 'user_id', index:'user_id_index'
		objectId column: 'object_id', index:'object_id_index'
		otherId column: 'other_id', index:'other_id_index'
	}
	
	static constraints = {
		created(nullable:true)
		objectId(nullable:true)
		otherId(nullable:true)
	}
	
	// static transients = [ 'groups', 'groupIds', 'isPublic' ]
	
	static searchable = {
		only = ['id', 'created', 'userId', 'typeId', 'objectId', 'otherId']
	}
	
	static delete(UserActivity item) {
		if (item) {
			item.delete(flush: true)
			return true
		}
		
		return false
	}
	
	static UserActivity create(Date created, Long userId, Long typeId, Long objectId, Long otherId = null) {	
		return UserActivity.withTransaction {
			UserActivity item = new UserActivity(created, userId, typeId, objectId, otherId)
			
			Utils.save(item, true)
			
			return item
		}
	}
	
	static UserActivity fetchStart(Long userId, Long objectId, Long typeId) {
		typeId = typeId & (~1L) // select the start member of the pair (starts are even, ends are odd)
		def c = UserActivity.createCriteria()
		def results = c {
			and {
				eq("userId", userId)
				eq("objectId", objectId)
				or {
					eq("typeId", typeId)
					eq("typeId", typeId + 1)
				}
			}
			order("created","desc")
			groupProperty("typeId")
			maxResults(2)
		}
		
		if (results && results.size()) {
			UserActivity last = results[0]
			if (last.typeId == typeId) // if most recent is start element
				return last
		}
		
		return null
	}
	
	static UserActivity fetchEnd(Long userId, Long objectId, Long typeId) {
		typeId = typeId & (~1L) // select the start member of the pair (starts are even, ends are odd)
		def c = UserActivity.createCriteria()
		def results = c {
			and {
				eq("userId", userId)
				or {
					eq("typeId", typeId)
					eq("typeId", typeId + 1)
				}
			}
			order("created","desc")
			groupProperty("typeId")
			maxResults(2)
		}
		
		if (results && results.size()) {
			UserActivity last = results[0]
			if (last.typeId == typeId + 1) // if most recent is end element
				return last
		}
		
		return null
	}
	
	static UserActivity fetchStart(Long userId, Long objectId, Long typeId, Date before) {
		typeId = typeId & (~1L) // select the start member of the pair (starts are even, ends are odd)
		def c = UserActivity.createCriteria()
		def results = c {
			and {
				eq("userId", userId)
				eq("objectId", objectId)
				or {
					eq("typeId", typeId)
					eq("typeId", typeId + 1)
				}
				le("created", before)
			}
			order("created","desc")
			groupProperty("typeId")
			maxResults(2)
		}
		
		if (results && results.size()) {
			UserActivity last = results[0]
			if (last.typeId == typeId) // if most recent is start element
				return last
		}
		
		return null
	}
	
	static UserActivity fetchEnd(Long userId, Long objectId, Long typeId, Date before) {
		typeId = typeId & (~1L) // select the start member of the pair (starts are even, ends are odd)
		def c = UserActivity.createCriteria()
		def results = c {
			and {
				eq("userId", userId)
				eq("objectId", objectId)
				or {
					eq("typeId", typeId)
					eq("typeId", typeId + 1)
				}
				le("created", before)
			}
			order("created","desc")
			groupProperty("typeId")
			maxResults(2)
		}
		
		if (results && results.size()) {
			UserActivity last = results[0]
			if (last.typeId == typeId + 1) // if most recent is end element
				return last
		}
		
		return null
	}
	
	UserActivity() {
	}
	
	UserActivity(Date created, Long userId, Long typeId, Long objectId, Long otherId) {
		this.created = created
		this.userId = userId
		this.typeId = typeId
		this.objectId = objectId
		this.otherId = otherId
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", created:" + created + ", userId:" + userId \
				+ ", typeId:" + typeId+ ", objectId:" + objectId+ ", otherId:" + otherId + ")"
	}
}

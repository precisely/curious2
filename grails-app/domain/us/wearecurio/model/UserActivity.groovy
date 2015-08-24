package us.wearecurio.model

import java.util.Map

import us.wearecurio.collection.MapList

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.DurationType
import us.wearecurio.data.RepeatType
import us.wearecurio.utility.Utils

/**
 * Represents a group for the purpose of collecting users and 
 */
class UserActivity {
	
	private static def log = LogFactory.getLog(this)
	
	Date 			created
	Long 			userId			//REQUIRED - the actor, will always capture via UI
	Long			typeId			//TEMP--need to change this, but first need mainline code to compare to.
	ActivityType 	activityType	//REQUIRED
	ObjectType		objectType		//REQUIRED
	Long 			objectId		//REQUIRED
	ObjectType		otherType		//OPTIONAL
	Long 			otherId			//OPTIONAL
	
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

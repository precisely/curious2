package us.wearecurio.model

import us.wearecurio.collection.MapList

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.DurationType
import us.wearecurio.data.RepeatType
import us.wearecurio.services.SearchService.Role;
import us.wearecurio.utility.Utils

/**
 * Represents an abstraction of user activity.
 * The data is intended for both the social UI as well as logging.
 */
class UserActivity {
	private static def log = LogFactory.getLog(this)
	
	Date 					created			//REQUIRED
	Long 					userId			//OPTIONAL - not always provided
	Long					typeId			//REQUIRED
	Long 					objectId		//REQUIRED
	Long 					otherId			//OPTIONAL

	static mapping = {
		version false
		created column: 'created', index:'created_index'
		userId column: 'user_id', index:'user_id_index'
		objectId column: 'object_id', index:'object_id_index'
		otherId column: 'other_id', index:'other_id_index'
	}
	
	static constraints = {
		created(nullable:false)
		typeId(nullable:false)
		userId(nullable:true)
		objectId(nullable:false)
		otherId(nullable:true)
	}
	
	static searchable = {
		only = [
			'id', 
			'created',
			'userId', 
			'userName', 
			'typeId', 
			'activityType', 
			'objectType', 
			'objectId', 
			'objectDescription', 
			'otherType', 
			'otherId', 
			'otherDescription',
			'objectUserId',
			'objectVisibility',
			'discussionGroupIds',
			'sprintVirtualGroupId',
			'sprintDescription',
			'sprintTagName']
	}


	static final String		TYPE_ABBREVIATION 	= 	"act"
	static final String 	TYPE_DELIMITER 		=	"-"
	static final String 	INVALID_TYPE_STRING	=	""
	static final int		SHIFT_OBJECT_BASE	= 	8
	static final int		SHIFT_OTHER_BASE	= 	24
	static final int		INVALID_TYPE		= 	-1
	

	static ActivityType toActivityType(Long typeIdParam) {
		if (typeIdParam == INVALID_TYPE) return null
		
		if ((typeIdParam & 11L) == 11L) {
			return ActivityType.UNINVITE
		} else if ((typeIdParam & 10L) == 10L) {
			return ActivityType.INVITE
		} else if ((typeIdParam & 9L) == 9L) {
			return ActivityType.UNCOMMENT
		} else if ((typeIdParam & 8L) == 8L) {
			return ActivityType.COMMENT
		} else if ((typeIdParam & 7L) == 7L) {
			return ActivityType.STOP
		} else if ((typeIdParam & 6L) == 6L) {
			return ActivityType.START
		} else if ((typeIdParam & 5L) == 5L) {
			return ActivityType.REMOVE
		} else if ((typeIdParam & 4L) == 4L) {
			return ActivityType.ADD
		} else if ((typeIdParam & 3L) == 3L) {
			return ActivityType.UNFOLLOW
		} else if ((typeIdParam & 2L) == 2L) {
			return ActivityType.FOLLOW
		} else if ((typeIdParam & 1L) == 1L) {
			return ActivityType.DELETE
		}
		
		return ActivityType.CREATE
	}
	
	static ObjectType toObjectType(Long typeIdParam) {
		return toObjectType(typeIdParam, true)
	}
	
	static ObjectType toOtherType(Long typeIdParam) {
		return toObjectType(typeIdParam, false)
	}
	
	static ObjectType toObjectType(Long typeIdParam, boolean objectType) {
		if (typeIdParam == INVALID_TYPE) return null
		
		int base = objectType ? SHIFT_OBJECT_BASE : SHIFT_OTHER_BASE
		
		if ((typeIdParam & (1L << base)) > 0) {
			return ObjectType.SPRINT
		} else if ((typeIdParam & (1L << (base + 1))) > 0) {
			return ObjectType.DISCUSSION_POST
		} else if ((typeIdParam & (1L << (base + 2))) > 0) {
			return ObjectType.DISCUSSION
		} else if ((typeIdParam & (1L << (base + 3))) > 0) {
			return ObjectType.USER
		} else if ((typeIdParam & (1L << (base + 4))) > 0) {
			return ObjectType.ADMIN
		} else if ((typeIdParam & (1L << (base + 5))) > 0) {
			return ObjectType.READER
		}
		
		return null
	}
	
	static Long toType(ActivityType activityType) {
		if (activityType == null) return INVALID_TYPE
		
		return activityType.getId()
	}
	
	static Long toTypeFromObject(ObjectType objectType) {
		if (objectType == null) return INVALID_TYPE
		
		return (1L << objectType.getId() + SHIFT_OBJECT_BASE)
	}
	
	static Long toTypeFromOther(ObjectType otherType) {
		if (otherType == null) return 0
		
		return (1L << otherType.getId() + SHIFT_OTHER_BASE)
	}
	
	static Long toType(ActivityType activityType, ObjectType objectType, ObjectType otherType = null) {
		if (!isValid(activityType, objectType, otherType)) return INVALID_TYPE
		
		Long act = toType(activityType)
		if (act == INVALID_TYPE) return INVALID_TYPE
		
		Long obj = toTypeFromObject(objectType)
		if (obj == INVALID_TYPE) return INVALID_TYPE
		
		return (act | obj | toTypeFromOther(otherType))
	}
	
	static enum ActivityType {
		CREATE(0),
		DELETE(1),
		FOLLOW(2),
		UNFOLLOW(3),
		ADD(4),
		REMOVE(5),
		START(6),
		STOP(7),
		COMMENT(8),
		UNCOMMENT(9),
		INVITE(10),
		UNINVITE(11)
		
		final Integer id
		
		static ActivityType get(int id) {
			switch(id) {
				case 0:
					return CREATE
				case 1:
					return DELETE
				case 2:
					return FOLLOW
				case 3:
					return UNFOLLOW
				case 4:
					return ADD
				case 5:
					return REMOVE
				case 6:
					return START
				case 7:
					return STOP
				case 8:
					return COMMENT
				case 9:
					return UNCOMMENT
				case 10:
					return INVITE
				case 11:
					return UNINVITE
				default:
					return null
			}
		}
		
		ActivityType(int id) {
			this.id = id
		}
		
		int getId() {
			return id
		}		

		String toAbbrev() {
			switch (get(id)) {
				case ActivityType.CREATE:
					return "created"
				case ActivityType.DELETE:
					return "deleted"
				case ActivityType.FOLLOW:
					return "followed"
				case ActivityType.UNFOLLOW:
					return "unfollowed"
				case ActivityType.ADD:
					return "added"
				case ActivityType.REMOVE:
					return "removed"
				case ActivityType.START:
					return "started"
				case ActivityType.STOP:
					return "stopped"
				case ActivityType.COMMENT:
					return "commented"
				case ActivityType.UNCOMMENT:
					return "uncommented"
				case ActivityType.INVITE:
					return "invited"
				case ActivityType.UNINVITE:
					return "uninvited"
				default:
					return ""
			}
		}
	}
	
	static enum ObjectType {
		SPRINT(0),
		DISCUSSION_POST(1),
		DISCUSSION(2),
		USER(3),
		ADMIN(4),
		READER(5)
		
		final Integer id
		
		static ObjectType get(int id) {
			switch(id) {
				case 0:
					return SPRINT
				case 1:
					return DISCUSSION_POST
				case 2:
					return DISCUSSION
				case 3:
					return USER
				case 4:
					return ADMIN
				case 5:
					return READER
				default:
					return null
			}
		}
		
		ObjectType(int id) {
			this.id = id
		}
		
		int getId() {
			return id
		}
		
		String toAbbrev() {
			switch (get(id)) {
				case ObjectType.SPRINT:
					return "spr"
				case ObjectType.DISCUSSION:
					return "dis"
				case ObjectType.DISCUSSION_POST:
					return "post"
				case ObjectType.USER:
					return "usr"
				case ObjectType.ADMIN:
					return "admin"
				case ObjectType.READER:
					return "reader"
				default:
					return ""
			}
		}
	}
	
	static String getTypeString(Long typeId) {
		return getTypeString(toActivityType(typeId), toObjectType(typeId), toOtherType(typeId))
	}
	
	static String getTypeString(ActivityType actType, ObjectType primaryObjType, ObjectType secondaryObjType = null) {

		if (!isValid(actType, primaryObjType, secondaryObjType)
			|| actType == null 
			|| primaryObjType == null) {
			return INVALID_TYPE_STRING
		}
		
		String ret = TYPE_ABBREVIATION
		ret += TYPE_DELIMITER
		ret += actType.toAbbrev() 
		ret += TYPE_DELIMITER 
		ret += primaryObjType.toAbbrev()
		if (secondaryObjType != null) {
			ret += TYPE_DELIMITER
			ret += secondaryObjType.toAbbrev()
		}
		
		return ret
	}
	
	static boolean isValid(ActivityType activityType, ObjectType objectType, ObjectType otherType=null) {
		if (activityType == null || objectType == null) return false
		
		switch (activityType) {
			case ActivityType.CREATE:
			case ActivityType.DELETE:
				return ( 
						(
							objectType == ObjectType.SPRINT
							|| objectType == ObjectType.DISCUSSION_POST
							|| objectType == ObjectType.DISCUSSION
							|| objectType == ObjectType.USER
						) && otherType == null
				)
			case ActivityType.FOLLOW:
			case ActivityType.UNFOLLOW:
				return (
					(
						objectType == ObjectType.USER
						|| objectType == ObjectType.SPRINT
					) && otherType == ObjectType.USER
				)
			case ActivityType.ADD:
			case ActivityType.REMOVE:
				return (
					(
						otherType == ObjectType.SPRINT
						&& (
							objectType == ObjectType.ADMIN
							|| objectType == ObjectType.READER
							|| objectType == ObjectType.DISCUSSION
						)
					) || (
						otherType == ObjectType.DISCUSSION
						&& (
							objectType == ObjectType.ADMIN
							|| objectType == ObjectType.READER
						)
					)
				)
			case ActivityType.START:
			case ActivityType.STOP:
				return (
					objectType == ObjectType.SPRINT
					&& otherType == null
				)
			case ActivityType.COMMENT:
			case ActivityType.UNCOMMENT:
				return (
					objectType == ObjectType.DISCUSSION
					&& otherType == ObjectType.DISCUSSION_POST
				)
			case ActivityType.INVITE:
			case ActivityType.UNINVITE:
				return (
					otherType == ObjectType.SPRINT
					&& (objectType == ObjectType.ADMIN || objectType == ObjectType.USER)
				)
		}
		
		return false
	}
	
	static delete(UserActivity item) {
		if (item) {
			item.delete(flush: true)
			return true
		}
		
		return false
	}
	
	static UserActivity create(ActivityType activityType, ObjectType objectType, Long objectId, ObjectType otherType = null, Long otherId = null) {
		return create(null, activityType, objectType, objectId, otherType, otherId)	
	}
	
	static UserActivity create(Long userId, ActivityType activityType, ObjectType objectType, Long objectId, ObjectType otherType = null, Long otherId = null) {
		return create(new Date(), userId, activityType, objectType, objectId, otherType, otherId)
	}
	
	static UserActivity create(Date created, Long userId, ActivityType activityType, ObjectType objectType, Long objectId, ObjectType otherType = null, Long otherId = null) {
		Long type = toType(activityType, objectType, otherType)
		if (type == INVALID_TYPE) return null
		UserActivity item = new UserActivity(created, userId, type, objectId, otherId)
		
		Utils.save(item, true)
		
		return item
	}
	
//	static UserActivity create(Date created, Long userId, Long typeId, Long objectId, Long otherId = null) {	
//		return UserActivity.withTransaction {
//			UserActivity item = new UserActivity(created, userId, typeId, objectId, otherId)
//			
//			Utils.save(item, true)
//			
//			return item
//		}
//	}
	
	//static UserActivity fetchStart(Long userId, Long objectId, Long typeId) {
	static UserActivity fetchStart(Long userId, Long objectId, ActivityType activityType, ObjectType objectType) {
		Long typeId = toType(activityType, objectType)
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
	
	//static UserActivity fetchEnd(Long userId, Long objectId, Long typeId) {
	static UserActivity fetchEnd(Long userId, Long objectId, ActivityType activityType, ObjectType objectType) {
		Long typeId = toType(activityType, objectType)
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
	
	//static UserActivity fetchStart(Long userId, Long objectId, Long typeId, Date before) {
	static UserActivity fetchStart(Long userId, Long objectId, ActivityType activityType, ObjectType objectType, Date before) {
		Long typeId = toType(activityType, objectType)
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
	
	//static UserActivity fetchEnd(Long userId, Long objectId, Long typeId, Date before) {
	static UserActivity fetchEnd(Long userId, Long objectId, ActivityType activityType, ObjectType objectType, Date before) {
		Long typeId = toType(activityType, objectType)
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
	
	static String getObjectDescription(ObjectType type, Long objectId) {
		if (type == null || objectId < 1) {
			return null
		}
		
		switch (type) {
			case ObjectType.USER:
			case ObjectType.ADMIN:
			case ObjectType.READER:
				return User.get(objectId)?.name
			case ObjectType.DISCUSSION:
				return Discussion.get(objectId)?.name
			case ObjectType.DISCUSSION_POST:
				return DiscussionPost.get(objectId)?.message
			case ObjectType.SPRINT:
				return Sprint.get(objectId)?.name
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
	
	ActivityType getActivityType() {
		return toActivityType(typeId)
	}
	
	ObjectType getObjectType() {
		return toObjectType(typeId, true)	
	}
	
	ObjectType getOtherType() {
		return toObjectType(typeId, false)
	}
	
	String getTypeString() {
		return getTypeString(activityType, objectType, otherType)
	}
	
	String getUserName() {
		return User.get(userId)?.name
	}
	
	String getObjectDescription() {
		return getObjectDescription(objectType, objectId)
	}
	
	String getOtherDescription() {
		return getObjectDescription(otherType, otherId)	
	}
	
	Long[] getDiscussionGroupIds() {
		if (objectType != ObjectType.DISCUSSION) return null
		
		return Discussion.get(objectId)?.getGroupIds()
	}
	
	Model.Visibility getObjectVisibility() {
		switch(objectType) {
			case ObjectType.DISCUSSION:
				return Discussion.get(objectId)?.visibility
			case ObjectType.SPRINT:
				return Sprint.get(objectId)?.visibility
			default:
				return null
		}
	}
	
	Long getObjectUserId() {
		switch(objectType) {
			case ObjectType.DISCUSSION:
				return Discussion.get(objectId)?.userId
			case ObjectType.SPRINT:
				return Sprint.get(objectId)?.userId
			default:
				return null
		}
	}
	
	String getSprintVirtualGroupId() {
		if (objectType != ObjectType.SPRINT) return null
			
		return Sprint.get(objectId)?.virtualGroupId
	}
	
	String getSprintDescription() {
		if (objectType != ObjectType.SPRINT) return null
		
		return Sprint.get(objectId)?.description
	}
	
	String getSprintTagName() {
		if (objectType != ObjectType.SPRINT) return null
		
		return Sprint.get(objectId)?.tagName
	}
	
	String toString() {
		return this.class.toString() + "(id:" + getId() + ", created:" + created + ", userId:" + userId \
				+ ", typeId:" + typeId+ ", objectId:" + objectId+ ", otherId:" + otherId + ")"
	}
}

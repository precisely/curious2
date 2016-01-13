package us.wearecurio.model

import java.util.Date
import java.util.Locale

import grails.converters.*
import grails.gorm.DetachedCriteria

import org.hibernate.criterion.CriteriaSpecification
import org.apache.commons.logging.LogFactory

import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.utility.Utils
import us.wearecurio.data.RepeatType
import us.wearecurio.model.DurationType
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.services.EmailService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.SearchService
import us.wearecurio.support.EntryStats
import us.wearecurio.model.Model.Visibility
import us.wearecurio.thirdparty.TagUnitMap

import java.text.DateFormat

import org.joda.time.*

class Sprint {
	
	/**
	 * Entries associated with a sprint are in GMT, coded to date 1/1/2001
	 */
	
	private static def log = LogFactory.getLog(this)
	
	String hash
	Long userId // user id of creator of sprint
	Long virtualGroupId // id of UserGroup for keeping track of sprint members and admins
	Long virtualUserId // user id for virtual user (owner of sprint tags/entries)
	String name
	String description
	Date created
	Date updated
	Long daysDuration
	Date startDate
	Visibility visibility
	String tagName
	List<String> devices // list of devices supported by this sprint
	
	static hasMany = [devices: String]
	
	static final int MAXPLOTDATALENGTH = 1024
	
	static Date sprintBaseDate
	
	static {
		TimeZone gmtTimeZone = TimeZoneId.getUTCTimeZone()
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US)
		dateFormat.setTimeZone(gmtTimeZone)
		sprintBaseDate = dateFormat.parse("Jan 1, 2001 12:00 am")
	}
	
	static Date getSprintBaseDate() { return sprintBaseDate }
	
	static constraints = {
		userId(nullable:true)
		// This needs to be uncommented once migrations have run on all the systems
		hash(/*blank: false, unique: true,*/ nullable: true)
		name(nullable:true)
		description(nullable:true, maxSize:10000)
		daysDuration(nullable:true)
		startDate(nullable:true)
		tagName(nullable:true)
		visibility(nullable:true)
	}
	
	static mapping = {
		version false
		hash column: 'hash', index: 'hash_index'
		name column: 'name', index:'name_index'
		userId column: 'user_id', index:'user_id_index'
		virtualGroupId column: 'virtual_group_id', index:'virtual_group_id_index'
		virtualUserId column: 'virtual_user_id', index:'virtual_user_id_index'
		created column: 'created', index:'created_index'
		updated column: 'updated', index:'updated_index'
	}
	
	static searchable = {
		only = [ 
			'hash', 
			'userId', 
			'virtualGroupId',
			'virtualGroupName', 
			'virtualUserId', 
			'name', 
			'description', 
			'created', 
			'updated', 
			'daysDuration', 
			'startDate', 
			'visibility',
			'entriesCount',
			'participantsCount',
			'hasRecentPost',
			'recentPostCreated',
            'username',
            'discussionsUsernames',
            'lastInterestingActivityDate',
		]
	}
	
	static Sprint create(User user) {
		return create(new Date(), user, null, null)
	}
	
	UserGroup fetchUserGroup() {
		return UserGroup.get(virtualGroupId)
		
	}
	
	boolean isPublic() {
		return visibility == Visibility.PUBLIC
	}
	
	void reindex() {
		if (id) SearchService.get().index(this) // only reindex if this sprint has been saved
	}

	void addDeviceName(String deviceName) {
		this.addToDevices(deviceName)
	}

	void removeDeviceName(String deviceName) {
		this.removeFromDevices(deviceName)
	}
	
	static Set<String> getDeviceNames() {
		return TagUnitMap.theMaps.keySet()
	}
	
	boolean hasWriter(Long userId) {
		return fetchUserGroup()?.hasWriter(userId)
	}
	
	void addWriter(Long userId) {
		fetchUserGroup()?.addWriter(userId)
		reindex()
	}
	
	void removeWriter(Long userId) {
		fetchUserGroup()?.removeWriter(userId)
		reindex()
	}
	
	boolean hasReader(Long userId) {
		return fetchUserGroup()?.hasReader(userId)
	}
	
	void addReader(Long userId) {
		fetchUserGroup()?.addReader(userId)
		UserActivity.create(
			userId,
			UserActivity.ActivityType.FOLLOW,
			UserActivity.ObjectType.SPRINT,
			this.id,
			UserActivity.ObjectType.USER,
			userId
		)
		UserActivity.create(
			userId,
			UserActivity.ActivityType.ADD,
			UserActivity.ObjectType.READER,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	void removeReader(Long userId) {
		fetchUserGroup()?.removeReader(userId)
		UserActivity.create(
			userId,
			UserActivity.ActivityType.UNFOLLOW,
			UserActivity.ObjectType.SPRINT,
			this.id,
			UserActivity.ObjectType.USER,
			userId
		)
		UserActivity.create(
			userId,
			UserActivity.ActivityType.REMOVE,
			UserActivity.ObjectType.READER,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	boolean hasAdmin(Long userId) {
		return fetchUserGroup()?.hasAdmin(userId)
	}
	
	void addAdmin(Long userId) {
		fetchUserGroup()?.addAdmin(userId)
		UserActivity.create(
			userId,
			UserActivity.ActivityType.ADD,
			UserActivity.ObjectType.ADMIN,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	boolean hasInvited(Long userId) {
		return fetchUserGroup()?.hasInvited(userId)
	}
	
	void addInvited(Long userId) {
		fetchUserGroup()?.addInvited(userId)
		UserActivity.create(
			null,
			UserActivity.ActivityType.INVITE,
			UserActivity.ObjectType.USER,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	void removeInvited(Long userId) {
		fetchUserGroup()?.removeInvited(userId)
		UserActivity.create(
			null,
			UserActivity.ActivityType.UNINVITE,
			UserActivity.ObjectType.USER,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	boolean hasInvitedAdmin(Long userId) {
		return fetchUserGroup()?.hasInvitedAdmin(userId)
	}
	
	void addInvitedAdmin(Long userId) {
		fetchUserGroup()?.addInvitedAdmin(userId)
		UserActivity.create(
			null,
			UserActivity.ActivityType.INVITE,
			UserActivity.ObjectType.ADMIN,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	void removeInvitedAdmin(Long userId) {
		fetchUserGroup()?.removeInvitedAdmin(userId)
		UserActivity.create(
			null,
			UserActivity.ActivityType.UNINVITE,
			UserActivity.ObjectType.ADMIN,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	void clearInvited() {
		UserGroup group = fetchUserGroup()
		
		def userIds = GroupMemberInvited.lookupMemberIds(group.id)
		def adminUserIds = GroupMemberInvitedAdmin.lookupMemberIds(group.id)
		
		group?.removeAllInvited()
		group?.removeAllInvitedAdmin()
		
		if (userIds != null) {
			for (def userId : userIds) {
				UserActivity.create(
					null,
					UserActivity.ActivityType.UNINVITE,
					UserActivity.ObjectType.USER,
					userId,
					UserActivity.ObjectType.SPRINT,
					this.id
				)
			}
		}
		
		if (adminUserIds != null) {
			for (def adminUserId : adminUserIds) {
				UserActivity.create(
					null,
					UserActivity.ActivityType.UNINVITE,
					UserActivity.ObjectType.ADMIN,
					adminUserId,
					UserActivity.ObjectType.SPRINT,
					this.id
				)
			}
		}
		reindex()
	}
	
	//TODO: need to make sure the sprint is re-index any time entry is added for this virtual user id
	Long getEntriesCount() {
		return Entry.countByUserId(virtualUserId)
	}
	
	Long getParticipantsCount() {
		List participantsIdList = GroupMemberReader.findAllByGroupId(virtualGroupId)*.memberId

		Long participantsCount = User.createCriteria().count {
				'in'("id", participantsIdList ?: [0l])
				or {
					isNull("virtual")
					ne("virtual", true)
				}
			}
		return participantsCount
	}

	void removeAdmin(Long userId) {
		fetchUserGroup()?.removeAdmin(userId)
		UserActivity.create(
			userId,
			UserActivity.ActivityType.REMOVE,
			UserActivity.ObjectType.ADMIN,
			userId,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	boolean hasDiscussion(Discussion discussion) {
		return fetchUserGroup()?.hasDiscussion(discussion)
	}
	
	void addDiscussion(Discussion discussion) {
		if (hasDiscussion(discussion)) return null
		
		fetchUserGroup()?.addDiscussion(discussion)
		UserActivity.create(
			null,
			UserActivity.ActivityType.ADD,
			UserActivity.ObjectType.DISCUSSION,
			discussion.id,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	void removeDiscussion(Discussion discussion) {
		if (!hasDiscussion(discussion)) return null
		
		fetchUserGroup()?.removeDiscussion(discussion)
		UserActivity.create(
			null,
			UserActivity.ActivityType.REMOVE,
			UserActivity.ObjectType.DISCUSSION,
			discussion.id,
			UserActivity.ObjectType.SPRINT,
			this.id
		)
		reindex()
	}
	
	static Sprint create(Date now, User user, String name, Visibility visibility) {
		log.debug "Sprint.create() userId:" + user?.getId() + ", name:" + name
		return Sprint.withTransaction {
			def sprint = new Sprint(now, user, name, visibility)
			Utils.save(sprint, true)
			UserActivity.create(now, user.id, UserActivity.ActivityType.CREATE, UserActivity.ObjectType.SPRINT, sprint.id)
			return sprint
		}
	}
	
	static void delete(Sprint sprint) {
		log.debug "Sprint.delete() id:" + sprint.getId()
		// DiscussionPost.executeUpdate("delete DiscussionPost p where p.discussionId = :id", [id:discussion.getId()]);
		Long userId = sprint.userId
		sprint.userId = 0
		UserGroup sprintUserGroup = sprint.fetchUserGroup()
		sprintUserGroup?.removeAllParticipants()
		
		Utils.save(sprint, true)
		UserActivity.create(new Date(), userId, UserActivity.ActivityType.DELETE, UserActivity.ObjectType.SPRINT, sprint.id)
        SearchService.get().deindex(sprint)
	}
	
	Sprint() {
		this.visibility = Visibility.NEW
	}
	
	protected createUserGroupFullName() {
		return this.name + " Trackathon"
	}
	
	Sprint(Date now, User user, String name, Visibility visibility) {
		this.userId = user?.getId()
		this.name = name
		this.hash = new DefaultHashIDGenerator().generate(12)
		this.created = now
		this.updated = this.created
		this.visibility = visibility
		String uniqueId = UUID.randomUUID().toString()
		UserGroup virtualUserGroup = UserGroup.createVirtual(createUserGroupFullName())
		this.virtualGroupId = virtualUserGroup.id
		User virtualUser = User.createVirtual() // create user who will own sprint entries, etc.
		addAdmin(user.id)
		virtualUserGroup.addWriter(virtualUser)
		virtualUserGroup.addReader(virtualUser)
		this.virtualUserId = virtualUser.id
		/*
		Discussion discussion = Discussion.create(virtualUser, "Trackathon: " + name, virtualUserGroup, null)
		this.discussionId = discussion.id
		*/
		this.description = null
	}
	
	void addMember(Long userId) {
		addReader(userId)
		addWriter(userId)
		reindex()
	}
	
	void removeMember(Long userId) {
		removeReader(userId)
		if (!hasAdmin(userId))
			removeWriter(userId)
		//removeAdmin(userId)
		reindex()
	}
	
	boolean hasMember(Long userId) {
		return hasReader(userId)
	}
	
	// returns start time if started as of "now", or null if outside sprint
	Date hasStarted(Long userId, Date now) {
		if (!hasMember(userId)) 
			return null
		
		UserActivity start = UserActivity.fetchStart(userId, this.id, UserActivity.ActivityType.START, UserActivity.ObjectType.SPRINT, now)
		
		return start?.created
	}
	
	// returns end time if ended as of "now", or null if outside sprint
	Date hasEnded(Long userId, Date now) {
		if (!hasMember(userId))
			return null
			
		UserActivity end = UserActivity.fetchEnd(userId, this.id, UserActivity.ActivityType.START, UserActivity.ObjectType.SPRINT, now)
		
		return end?.created
	}
	
	protected String entrySetName() {
		return "trackathon id: " + this.id
	}
	
	void setName(String name) {
		this.name = name
	}
	
	Sprint update(Map map) {
		log.debug "Sprint.update() this:" + this + ", map:" + map
		
		this.updated = new Date()
		
		if (map['name']) {
			this.name = map['name']
			this.tagName = null
			this.fetchTagName()
			UserGroup group = fetchUserGroup()
			group.setFullName(createUserGroupFullName())
			Utils.save(group, true)
		}
		if (map['description']) this.description = map['description']
		if (map['daysDuration']) this.daysDuration = map['daysDuration']
		if (map['startDate']) this.startDate = map['startDate']
		if (map['visibility']) this.visibility = map['visibility']
		
		Utils.save(this, true)
		
		fetchUserGroup()?.acceptAllInvited()

		return this
	}
	
	String fetchTagName() {
		if (tagName) return tagName
		
		tagName = name.toLowerCase()
			.replaceAll(~/[^a-zA-Z ]+/, '')		// Removes all special characters and numbers
			.replaceAll(~/ +/, ' ')			// Replaces all multiple spaces with single space
			.trim() // Removes spaces from the start and the end of the string
		// find last space before 80 characters
		if (tagName.length() > 40) {
			int cutoffSpaceIndex = tagName.lastIndexOf(' ', 40)
			if (cutoffSpaceIndex < 0) cutoffSpaceIndex = 40
			tagName = tagName.substring(0, cutoffSpaceIndex)
		}
		if (tagName.length() == 0) {
			tagName = "a"
		}
		tagName += ' trackathon'
		
		return tagName
	}
	
	// add sprint entries to user account in the specified time zone
	boolean start(Long userId, Date baseDate, Date now, String timeZoneName, EntryStats stats) {
		if (!hasMember(userId))
			return false
		
		if (hasStarted(userId, now))
			return false
		
		stats.setBatchCreation(true)
		
		String setName = entrySetName()
		
		def entries = Entry.findAllByUserId(virtualUserId)
		for (Entry entry in entries) {
			if (!entry.isPrimaryEntry())
				continue
			
			if (entry.getRepeatType() == null)
				continue
			
			entry.activateTemplateEntry(userId, baseDate, now, timeZoneName, stats, setName)
		}
		
		// add start element
		def m = EntryParserService.get().parse(now, timeZoneName, fetchTagName() + " start", null, null, baseDate, true)
		def entry = Entry.create(userId, m, stats)
		
		// record activity
		UserActivity.create(now, userId, UserActivity.ActivityType.START, UserActivity.ObjectType.SPRINT, this.id)
		
		// refresh writer date
		GroupMemberWriter.update(this.virtualGroupId, userId)
		
		return true
	}
	
	// remove sprint entries from user account for current base date
	// remove bookmarked entries outright
	// end repeat entries
	// end remind entries
	boolean stop(Long userId, Date baseDate, Date now, String timeZoneName, EntryStats stats) {
		Date startDate = hasStarted(userId, now)

		if (!startDate) return false
				
		stats.setBatchCreation(true)
		
		String setName = entrySetName()
		
		Identifier setIdentifier = Identifier.look(setName)
		
		// look for entries marked by sprint set identifier
		def entries = Entry.findAllByUserIdAndSetIdentifier(userId, setIdentifier)
		for (Entry entry in entries) {
			if (!entry.isPrimaryEntry())
				continue
			
			Entry.deleteGhost(entry, stats, baseDate, true)
		}
		
		// look for manually created entries that match sprint template
		def sprintEntries = Entry.findAllByUserId(virtualUserId)
		for (Entry entry in sprintEntries) {
			if (!entry.isPrimaryEntry())
				continue
			
			if (entry.getRepeatType() == null)
				continue
			
			boolean testDate = true
			if (entry.getRepeatType().isContinuous())
				testDate = false
				
			// search for entry that matches this pattern
			def c = Entry.createCriteria()
			def results = c {
				and {
					eq("userId", userId)
					if (testDate) {
						ge("date", startDate)
						lt("date", startDate + 1)
					}
					eq("tag", entry.getTag())
					eq("repeatTypeId", entry.repeatTypeId)
					isNull("setIdentifier")
				}
			}

			for (Entry e in results) {
				Entry.deleteGhost(e, stats, baseDate, true)
			}
		}
		
		// add stop element
		def m = EntryParserService.get().parse(now, timeZoneName, fetchTagName() + " end", null, null, baseDate, true)
		def entry = Entry.create(userId, m, stats)

		// record activity
		UserActivity.create(now, userId, UserActivity.ActivityType.STOP, UserActivity.ObjectType.SPRINT, this.id)
	}
	
	boolean isModified() {
		return this.created.getTime() != this.updated.getTime()
	}
	
	/**
	 * Get data for the discussion data lists
	 */
	def getJSONDesc() {
		return [
			id: this.id,
			hash: this.hash,
			name: this.name?:'New Trackathon',
			userId: this.userId,
			description: this.description,
			totalParticipants: this.getParticipantsCount(),
			totalTags: this.getEntriesCount(),
			virtualUserId: this.virtualUserId,
			virtualGroupId: this.virtualGroupId,
			virtualGroupName: this.fetchUserGroup()?.name,
			created: this.created,
			updated: this.updated,
			type: "spr"
		]
	}
	
	static List<Sprint> getSprintListForUser(Long userId, int max, int offset) {
		if (!userId) {
			return []
		}
		
		max = max ?: 5;
		offset = offset ?: 0;

		List<Long> groupReaderList = new DetachedCriteria(GroupMemberReader).build {
			projections {
				property "groupId"
			}
			eq "memberId", userId
		}.list()
		
		List<Sprint> sprintList = Sprint.withCriteria {
			and {
				or {
					'in'("virtualGroupId", groupReaderList ?: [0l]) // When using 'in' clause GORM gives error on passing blank list
					and {
						eq("visibility", Visibility.PUBLIC)
						ne("userId", 0l)
					}
				}
				not {
					eq("visibility", Visibility.NEW)
				}
			}
			firstResult(offset)
			maxResults(max)
			order("updated", "desc")
		}
		
		return sprintList*.getJSONDesc()
	}

	List<User> getParticipants(int max, int offset) {
		if (!userId) {
			return []
		}
		
		max = Math.min(max ?: 10, 100)
		offset = offset ?: 0

		List<Long> participantIdsList = new DetachedCriteria(GroupMemberReader).build {
			projections {
				property "memberId"
			}
			eq "groupId", virtualGroupId
		}.list()

		// Fetch all non-virtual or actual users
		List<User> participantsList = User.withCriteria {
				resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
				createAlias("avatar", "avatarAlias", CriteriaSpecification.LEFT_JOIN)
				projections {
					property "username", "username"
					property "avatarAlias.path", "avatarURL"
					property "id", "userId"
				}
				'in'("id", participantIdsList ?: [0l])
				or {
					isNull("virtual")
					ne("virtual", true)
				}
				maxResults(max)
				firstResult(offset)
			}
	
		return participantsList
	}
	
	String getVirtualGroupName() {
		return UserGroup.get(virtualGroupId)?.name
	}
	
    String getUsername() {
        return User.get(userId)?.username
    }
    
	//TODO: any time post made to discussion, will need to re-index sprint
	Date getRecentPostCreated() {
		def recent = null
		def discussionIds = GroupMemberDiscussion.lookupMemberIds(virtualGroupId)
		if (discussionIds?.size() > 0) {
			for ( def d_id : discussionIds ) {
				def d = Discussion.get(d_id)
				if (!d) continue
				if (recent == null) {
					recent = d.recentPostCreated
				} else if (d.recentPostCreated > recent) {
					recent = d.recentPostCreated 
				}
			}
		}
		
		return recent
	}
	
	boolean getHasRecentPost() {
		def discussionIds = GroupMemberDiscussion.lookupMemberIds(virtualGroupId)
		if (discussionIds?.size() > 0) {
			for ( def d_id : discussionIds ) {
				if (Discussion.get(d_id)?.hasRecentPost) {
					return true
				}
			}
		}
		
		return false
	}
	
    String getDiscussionsUsernames() {
		def discussionIds = GroupMemberDiscussion.lookupMemberIds(virtualGroupId)
        String usernames = ""
		if (discussionIds?.size > 0) {
			for (def d_id : discussionIds) {
            //def discussions = Discussion.getAll(discussionIds)
                boolean first = true
                def d = Discussion.get(d_id)
            //if (discussions && discussions.size > 0) {
                //for (def d : discussions) {
                    if (d?.username && d.username != "") {
                        if (first) {
                            usernames = d.username
                            first = false
                        } else {
                            usernames += " ${d.username}"
                        }
                    }
                        if (d?.postUsernames && d.postUsernames != "") {
                            if (first) {
                                usernames = d.postUsernames
                                first = false
                            } else {
                                usernames += " ${d.postUsernames}"
                            }
                        }
                    }
                //}
            //}
		}
        
        return usernames
    }
    
    Date getLastInterestingActivityDate() {
        Date ret = created
        
        if (virtualGroupId != null && virtualGroupId > 0) {
            def discussionIds = GroupMemberDiscussion.lookupMemberIds(virtualGroupId)
            if (discussionIds != null && discussionIds.size > 0) {
                Discussion d = Discussion.createCriteria().get {
                    'in'("id", discussionIds)
                    maxResults(1)
                    order("created", "desc")
                }
                return d?.created
            }
        }
            
        return ret
    }
    
	String toString() {
		return "Sprint(id:" + getId() + ", userId:" + userId + ", name:" + name + ", created:" + Utils.dateToGMTString(created) \
				+ ", updated:" + Utils.dateToGMTString(updated) + ", visibility:" + visibility + ")"
	}
}

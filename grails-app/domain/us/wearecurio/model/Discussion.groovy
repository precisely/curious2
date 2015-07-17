package us.wearecurio.model

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.utility.Utils
import us.wearecurio.model.Model.Visibility
import us.wearecurio.services.EmailService
import us.wearecurio.services.DatabaseService

class Discussion {
	
	private static def log = LogFactory.getLog(this)
	
	Long userId // user id of first post or creator
	Long firstPostId // DiscussionPost
	String name
	Date created
	Date updated
	String hash = new DefaultHashIDGenerator().generate(12)
	Visibility visibility
	
	public static final int MAXPLOTDATALENGTH = 1024
	
	static constraints = {
		userId(nullable:true)
		// This needs to be uncommented once migrations have run on all the systems
		hash(/*blank: false, unique: true,*/ nullable: true)
		name(nullable:true)
		firstPostId(nullable:true)
		visibility(nullable:true)
	}
	
	static mapping = {
		version false
		table 'discussion'
		firstPostId column: 'first_post_id', index:'first_post_id_index'
	}
	
	static transients = [ 'groups', 'groupIds', 'isPublic' ]
	
	static searchable = {
		only = ['userId', 'firstPostId', 'name', 'created', 'updated', 'visibility', 'groupIds']
	}
	
	public static Discussion getDiscussionForPlotDataId(Long plotDataId) {
		DiscussionPost post = DiscussionPost.createCriteria().get {
			and {
				eq("plotDataId", plotDataId)
			}
			order("plotDataId", "desc")
			order("created", "asc")
		}
		
		return post?.getDiscussion()
	}
	
	public static Discussion create(User user) {
		return create(user, null)
	}
	
	public static Discussion create(User user, String name) {
		log.debug "Discussion.create() userId:" + user?.getId() + ", name:" + name
		def discussion = new Discussion(user, name, new Date())
		
		Utils.save(discussion, true)
		
		discussion.addUserVirtualGroup(user)
		
		return discussion
	}
	
	static Discussion create(User user, String name, UserGroup group, Date createTime = null) {
		log.debug "Discussion.create() userId:" + user?.getId() + ", name:" + name + ", group:" + group + ", createTime:" + createTime
		Discussion discussion = null
		
		if (group == null) {
			discussion = new Discussion(user, name, createTime)
			
			Utils.save(discussion, true)
			
			discussion.addUserVirtualGroup(user)
			
			return discussion
		}
		
		if (group?.hasWriter(user)) {
			discussion = new Discussion(user, name, createTime)
			Utils.save(discussion, true)
			group.addDiscussion(discussion)
			discussion.addUserVirtualGroup(user)
		}
		
		return discussion
	}
	
	static Map delete(Discussion discussion, User user) {
		if (!UserGroup.canAdminDiscussion(user, discussion)) {
			return [success: false, message: "You don't have the right to delete this discussion."]
		}
		Discussion.delete(discussion)
		return [success: true, message: "Discussion deleted successfully."]
		
	}
	
	static void delete(Discussion discussion) {
		log.debug "Discussion.delete() discussionId:" + discussion.getId()
		DiscussionPost.executeUpdate("delete DiscussionPost p where p.discussionId = :id", [id:discussion.getId()]);
		discussion.delete(flush: true)
	}
	
	static boolean update(Discussion discussion, def params, User user) {
		synchronized(Discussion) {
			log.debug "Trying to update Discussion " + discussion.getName() + " with " + params
			
			if (!UserGroup.canAdminDiscussion(user, discussion)) {
				log.debug "User " + user.getUsername() + " doesn't have admin permissions"
				return false
			}
			
			boolean isNew = discussion.isNew()
			
			if (discussion.getName().equals(params.name))
				return true
			
			discussion.setName(params.name)
			discussion.setUpdated(new Date())
			
			Utils.save(discussion, true)
			
			def groups = UserGroup.getGroupsForDiscussion(discussion)
			
			def userName = user.getUsername()
			
			def discussionSubject = "User '" + user.getUsername() + "' "
			
			if (!discussion.isModified()) {
				log.debug "New discussion topic: send notifications if applicable"
				discussionSubject += "created new discussion topic '" + params.name + "'"
			} else {
				log.debug "Renamed discussion topic"
				discussionSubject += "renamed discussion topic to '" + params.name + "'"
			}
			
			for (UserGroup group in groups)
				group.notifyNotifieds(discussionSubject, discussionSubject +  " in group '" + group.getName() + "'")
			
			return true
		}
	}
	
	public Discussion() {
		this.visibility = Model.Visibility.PUBLIC
	}
	
	public Discussion(User user, String name, Date createTime = null) {
		this.userId = user?.getId()
		this.name = name
		this.created = createTime ?: new Date()
		this.updated = this.created
		this.visibility = Model.Visibility.PUBLIC
	}
	
	boolean isPublic() {
		return getIsPublic()
	}
	
	boolean getIsPublic() {
		return this.visibility == Model.Visibility.PUBLIC
	}
	
	void setIsPublic(boolean setPublic) {
		this.visibility = setPublic ? Model.Visibility.PUBLIC : Model.Visibility.PRIVATE
	}
	boolean isNew() {
		return this.name == null
	}
	
	boolean isModified() {
		return this.created.getTime() != this.updated.getTime()
	}
	
	def getNumberOfPosts() {
		return DiscussionPost.countByDiscussionId(getId())
	}
	
	static UserGroup loadGroup(String groupName, User user) {
		UserGroup group = groupName ? UserGroup.lookup(groupName) : UserGroup.getDefaultGroupForUser(user)
		
		if (group && !group.hasWriter(user)) {
			return null
		}
		return group
	}
	
	static def loadDiscussion(def id, def plotDataId, def user) {
		def discussion
		if (id) {
			discussion = Discussion.get(id)
		} else if (plotDataId) {
			discussion = Discussion.getDiscussionForPlotDataId(plotDataId)
			log.debug "Discussion for plotDataId not found: " + plotDataId
		}
		
		log.debug "Discussion found: " + discussion?.dump()
		return discussion
	}
	
	
	DiscussionPost fetchFirstPost() {
		DiscussionPost post = DiscussionPost.createCriteria().get {
			and {
				eq("discussionId", getId())
			}
			maxResults(1)
			order("plotDataId", "desc")
			order("created", "asc")
		}
		
		if (!post) {
			return null
		}
		
		firstPostId = post.id
		Utils.save(this)
		return post
	}
	
	DiscussionPost getFirstPost() {
		if (firstPostId == null) {
			return fetchFirstPost()
		}
		
		return DiscussionPost.get(firstPostId)
	}
	
	def getLastPost() {
		def c = DiscussionPost.createCriteria()
		
		def posts = c {
			and {
				eq("discussionId", getId())
			}
			maxResults(1)
			order("plotDataId", "desc")
			order("created", "desc")
		}
		
		for (post in posts)
			return post
		
		return null
	}
	
	def fetchUserId() {
		if (userId != null) return userId
		DiscussionPost post = getFirstPost()
		return post?.getAuthor()?.getUserId()
	}
	
	List<DiscussionPost> getFollowupPosts(Map args) {
		/*
		 * If no offset or offset is 0, set offset to 1 so that
		 * first post is not fetched.
		 */
		if (!args["offset"]) {
			args["offset"] = 1
		}
		
		/*
		 * If max is given and offset is set to be current step offset + 1
		 * then increase current offset by one to support proper pagination
		 * skipping always the first post.
		 */
		if (args["max"] && (args["offset"].toInteger() % args["max"].toInteger()) == 0) {
			args["offset"] = args["offset"].toInteger() + 1
		}
		
		getPosts(args)
	}
	
	List<DiscussionPost> getPosts(Map args) {
		List posts = DiscussionPost.createCriteria().list(args) {
			and {
				eq("discussionId", getId())
			}
			order("created", "asc")
		}
		
		return posts
	}

	def createPost(User user, String message, Date createTime = null) {
		log.debug "Discussion.createPost() userId:" + user.getId() + ", message:'" + message + "'" + ", createTime:" + createTime
		return createPost(user, null, message, createTime)
	}
	
	def createPost(String name, String email, String site, Long plotDataId, String comment, Date createTime = null) {
		log.debug "Discussion.createPost() name:" + name + ", email:" + email + ", plotDataId:" + plotDataId + ", comment:'" + comment + "'" + ", createTime:" + createTime
		return createPost(User.lookupOrCreateVirtualEmailUser(name, email, site), plotDataId, comment, createTime)
	}
	
	def createPost(User author, Long plotDataId, String comment, Date createTime = null) {
		log.debug "Discussion.createPost() author:" + author + ", plotDataId:" + plotDataId + ", comment:'" + comment + ", createTime:" + createTime
		if (createTime == null) createTime = new Date()
		DiscussionPost post = DiscussionPost.create(this, author.getId(), plotDataId, comment, createTime)
		Utils.save(this, true) // write new updated state
		Utils.save(post, true)
		
		if (userId == null)
			this.userId = post.getAuthorUserId()
		
		if (firstPostId == null) {
			firstPostId = post.getId()
			post.flags |= DiscussionPost.FIRST_POST_BIT
			if (plotDataId) {
				PlotData plotData = PlotData.get(plotDataId)
				if (this.name == null || this.name.length() == 0)
					this.name = plotData?.getName()
				this.notifyParticipants(post, true)
			} else {
				this.notifyParticipants(post, false)
			}
			Utils.save(this, true)
			Utils.save(post, true)
		} else {
			this.notifyParticipants(post, false)
		}
		
		return post
	}
	
	/**
	 * getParticipants()
	 * 
	 * @return list of current participants in this discussion
	 */
	def getParticipants() {
		def results = DiscussionPost.executeQuery(
				"select distinct post.authorUserId from DiscussionPost post where post.discussionId = :discussionId", [discussionId:getId()])
		
		def retVal = []
		for (r in results) {
			User author = User.get(r)
			if (author != null)
				retVal.push(author)
		}
		
		return retVal
	}
	
	def notifyPost(User participant, String authorUsername, String message) {
		log.debug("Sending notification about '" + this.getName() + "' to " + participant.email + " about message written by " + authorUsername)

		participant.notifyEmail("notifications@wearecurio.us", "Comment added to discussion " + this.getName(),
				authorUsername + " wrote:\n\n" + message)
	}
	
	def getGroups() {
		UserGroup.getGroupsForDiscussion(this)
	}
	
	Long[] getGroupIds() {
		def groups = UserGroup.getGroupsForDiscussion(this)
		
		if ((!groups) || groups.size() == 0)
			return new Long[0]
		
		Long[] retVal = new Long[groups.size()]
		int i = 0
		for (UserGroup group : groups) {
			retVal[i++] = group.getId()
		}
		
		return retVal
	}
	
	def notifyParticipants(DiscussionPost post, boolean onlyAdmins) {
		def participants = this.getParticipants()
		
		def notifiedSet = [:]
		
		def postUsername = (post.getAuthor()?.getShortDescription()) ?: "(unknown)"
		
		Long authorUserId = post.getAuthorUserId()
		
		if (!onlyAdmins) {
			for (User participant in participants) {
				notifiedSet[participant.getId()] = true
				if (participant.getId() == authorUserId) {
					continue // don't notify author of this post
				}
				if (participant.getEmail() != null) {
					notifyPost(participant, postUsername, post.getMessage())
				}
			}
		}
		
		def groups = UserGroup.getGroupsForDiscussion(this)
		
		for (UserGroup group in groups) {
			def notifieds = group.getNotifiedUsers()
			
			for (User user in notifieds) {
				if (notifiedSet[user.getId()])
					continue
				notifiedSet[user.getId()] = true
				notifyPost(user, postUsername, post.getMessage())
			}
		}
	}
	
	/**
	 * Get data for the discussion data lists
	 */
	def getJSONDesc() {
		return [
			id: this.id,
			hash: this.hash,
			name: this.name?:'New question or discussion topic?',
			userId: this.userId,
			isPublic: this.visibility == Model.Visibility.PUBLIC,
			created: this.created,
			updated: this.updated,
			type: "dis"
		]
	}
	
	Map getJSONModel(Map args) {
		Long totalPostCount = 0
		DiscussionPost firstPostInstance = getFirstPost()
		boolean isFollowUp = firstPostInstance?.getPlotDataId() != null
		List postList = isFollowUp ? getFollowupPosts(args) : getPosts(args)

		String groupNameQuery = "SELECT ug.full_name FROM user_group ug INNER JOIN group_member_discussion gmd on ug.id = gmd.group_id where gmd.member_id = :id ORDER BY gmd.created DESC"
		DatabaseService databaseService = DatabaseService.get()
		
		List result = databaseService.sqlRows(groupNameQuery, [id: id])
		String discussionGroupName = result[0].full_name

		if (args.max && args.offset.toInteger()  > -1) {
			// A total count will be available if pagination parameter is passed
			totalPostCount = postList.getTotalCount()
			
			if (totalPostCount && isFollowUp) {
				// If first post is available then total count must be one lesser
				totalPostCount --
			}
		}
		[discussionId: getId(), discussionTitle: this.name ?: 'New question or discussion topic?', hash: this.hash, 
			discussionOwner: User.get(this.userId)?.username, discussionCreatedOn: this.created, updated: this.updated, firstPost: firstPostInstance,
			posts: postList, isNew: isNew(), totalPostCount: totalPostCount, isPublic: this.visibility == Model.Visibility.PUBLIC, groupName: discussionGroupName ?: '']
	}
	
	String toString() {
		return "Discussion(id:" + getId() + ", userId:" + userId + ", name:" + name + ", firstPostId:" + firstPostId + ", created:" + Utils.dateToGMTString(created) \
				+ ", updated:" + Utils.dateToGMTString(updated) + ", isPublic:" + (this.visibility == Model.Visibility.PUBLIC) + ")"
	}
	
	private void addUserVirtualGroup(User user) {
		if (user?.virtualUserGroupId > 0) {
			GroupMemberDiscussion.create(user.virtualUserGroupId, id)
		}
	}
}

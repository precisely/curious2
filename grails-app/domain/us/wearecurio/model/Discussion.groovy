package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Model.Visibility
import us.wearecurio.services.DatabaseService
import us.wearecurio.utility.Utils
import us.wearecurio.services.SearchService

class Discussion {
	
	private static def log = LogFactory.getLog(this)
	
	Long userId // user id of first post or creator
	Long firstPostId // DiscussionPost
	String name
	Date created
	Date updated
	String hash = new DefaultHashIDGenerator().generate(12)
	Visibility visibility
	
	static final int MAXPLOTDATALENGTH = 1024
	
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
		only = [
			'userId',
			'userIdFinal',
			'userHash',
			'publicUserName',
			'userAvatarURL',
			'firstPostId',
			'firstPostMessage',
			'isFirstPostPlot',
			'posts',
			'postCount',
			'hasRecentPost',
			'recentPostMessage',
			'recentPostCreated',
			'recentPostUserHash',
			'recentPostUserPublicName',
			'name', 
			'created', 
			'updated', 
			'visibility', 
			'groupIds', 
			'hash',
			'recentActivityDate'
		]
	}
	
	static Discussion getDiscussionForPlotDataId(Long plotDataId) {
		DiscussionPost post = DiscussionPost.createCriteria().get {
			and {
				eq("plotDataId", plotDataId)
			}
			order("plotDataId", "desc")
			order("created", "asc")
		}
		
		return post?.getDiscussion()
	}
	
	static Discussion create(User user) {
		return create(user, null)
	}
	
	private static createUserActivity(Long userId, UserActivity.ActivityType activityType, Long objectId) {
		UserActivity.create(userId, activityType, UserActivity.ObjectType.DISCUSSION, objectId)
	}
	
	static Discussion create(User user, String name, Visibility visibility = Visibility.PUBLIC) {
		log.debug "Discussion.create() userId:" + user?.getId() + ", name:" + name + ", visibility: " + visibility
		def discussion = new Discussion(user, name, new Date(), visibility)

		Utils.save(discussion, true)
		
		discussion.addUserVirtualGroups(user)
		
		createUserActivity(user.id, UserActivity.ActivityType.CREATE, discussion.id )
		
		return discussion
	}
	
	static Discussion create(User user, String name, UserGroup group, Date createTime = null, Visibility visibility = Visibility.PUBLIC) {
		log.debug "Discussion.create() userId:" + user?.getId() + ", name:" + name + ", group:" + group + ", createTime:" + createTime + ", visibility: " + visibility
		Discussion discussion = null
		
		if (group == null) {
			discussion = new Discussion(user, name, createTime, visibility)
			
			Utils.save(discussion, true)
			
			discussion.addUserVirtualGroups(user)
			
			createUserActivity(user.id, UserActivity.ActivityType.CREATE, discussion.id)
			
			return discussion
		}
		
		if (group?.hasWriter(user)) {
			discussion = new Discussion(user, name, createTime, visibility)
			Utils.save(discussion, true)
			group.addDiscussion(discussion)
			discussion.addUserVirtualGroups(user)
		}
		
		if (discussion != null) {
			createUserActivity(user.id, UserActivity.ActivityType.CREATE, discussion.id)
		}
		
		return discussion
	}
	
	static Map delete(Discussion discussion, User user) {
		if (!UserGroup.canAdminDiscussion(user, discussion)) {
			return [success: false, message: "You don't have the right to delete this discussion."]
		}
		def discussionId = discussion.id
		
		Discussion.delete(discussion)

		createUserActivity(user.id, UserActivity.ActivityType.DELETE, discussionId)
		
		return [success: true, message: "Discussion deleted successfully."]
		
	}
	
	static void delete(Discussion discussion) {
		log.debug "Discussion.delete() discussionId:" + discussion.getId()
		def discussionId = discussion.getId()
		createUserActivity(null, UserActivity.ActivityType.DELETE, discussionId)
		//TODO: create delete DiscussionPost activities and write integration tests
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
	
	Discussion() {
		this.visibility = Model.Visibility.PUBLIC
	}
	
	Discussion(User user, String name, Date createTime = null, Visibility visibility) {
		this.userId = user?.getId()
		this.name = name
		this.created = createTime ?: new Date()
		this.updated = this.created
		this.visibility = visibility ?: Model.Visibility.PUBLIC
	}
	
	void reindex() {
		if (id) SearchService.get().index(this)
	}
	
	boolean isPublic() {
		return getIsPublic()
	}
	
	boolean hasFirstPost() {
		return firstPostId != null
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
	
	static Discussion loadDiscussion(def id, def plotDataId, def user) {
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
			eq("discussionId", this.id)
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

	Boolean getIsFirstPostPlot() {
		return (PlotData.get(firstPost?.plotDataId) != null)
	}
	
	DiscussionPost getLastPost() {
		DiscussionPost post = DiscussionPost.createCriteria().get {
			eq("discussionId", this.id)
			maxResults(1)
			order("plotDataId", "desc")
			order("created", "desc")
		}

		return post
	}

	Long getUserIdFinal() {
		return fetchUserId()
	}
	
	Long fetchUserId() {
		if (userId != null) return userId
		DiscussionPost post = getFirstPost()
		return post?.getAuthor()?.getUserId()
	}

	/**
	 * Constructs a common criteria closure which can be applied on {@link us.wearecurio.model.DiscussionPost}
	 * using <code>createCriteria().list()</code> or <code>withCriteria</code> method to perform various operations.
	 * Keeping this method separate to get the criteria only so that we can re-use the same criteria to get all
	 * associated posts or just count of all posts.
	 * 
	 * @param args See {@link #getFollowupPosts(Map) getFollowupPosts(Map)} method for details
	 * @return Criteria closure as descibed above.
	 */
	Closure getPostsCriteria(Map args) {
		return {
			eq("discussionId", this.id)

			// If first post ID is available
			if (args.firstPostID) {
				// Then we need to exclude the first post from being fetched
				ne("id", args.firstPostID)
			}
		}
	}

	/**
	 * Get the list of {@link us.wearecurio.model.DiscussionPost posts} excluding the very first post "X" of the
	 * discussion if the discussion is associated with a graph and the post "X" is having plot ID of that graph.
	 * 
	 * @param args.max OPTIONAL Pagination parameter to limit the returning results.
	 * @param args.offset OPTIONAL Pagination parameter to get the results after given position
	 * @param args.sort OPTIONAL Sorting parameter to sort list of posts
	 * @param args.order OPTIONAL Sorting parameter to change order of posts based on sorting parameter
	 * 
	 * @return Return the list of posts as described above.
	 */
	List<DiscussionPost> getFollowupPosts(Map args) {
		DiscussionPost firstPostInstance = getFirstPost()

		// If first post of this discussion is associated with a Graph
		if (firstPostInstance?.plotDataId) {
			args.firstPostID = firstPostInstance.id
		}

		getPosts(args)
	}

	/**
	 * Get the list of all {@link us.wearecurio.model.DiscussionPost posts} associated with the current discussion.
	 * 
	 * @param args.max OPTIONAL Pagination parameter to limit the returning results.
	 * @param args.offset OPTIONAL Pagination parameter to get the results after given position
	 * @param args.sort OPTIONAL Sorting parameter to sort list of posts
	 * @param args.order OPTIONAL Sorting parameter to change order of posts based on sorting parameter
	 * @param args.firstPostID OPTIONAL See {@link #getFollowupPosts(Map) getFollowupPosts(Map)} method for detail
	 * 
	 * @return Return the list of posts/comments as described above.
	 */
	List<DiscussionPost> getPosts(Map args) {
		args.sort = args.sort ?: "created"
		args.order = args.order ?: "asc"

		return DiscussionPost.createCriteria().list(args, getPostsCriteria(args))
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
		
		if (post.isFirstPost()) {
			if (plotDataId) {
				PlotData plotData = PlotData.get(plotDataId)
				if (this.name == null || this.name.length() == 0)
					this.name = plotData?.getName()
				this.notifyParticipants(post, true)
			} else {
				this.notifyParticipants(post, false)
			}
		} else {
			this.notifyParticipants(post, false)
		}
		
		UserActivity.create(
			post.authorUserId, 
			UserActivity.ActivityType.COMMENT, 
			UserActivity.ObjectType.DISCUSSION, 
			id,
			UserActivity.ObjectType.DISCUSSION_POST,
			post.id
		)
				
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

	static String GROUP_NAME_QUERY = "SELECT ug.full_name FROM user_group ug INNER JOIN group_member_discussion gmd" +
			" on ug.id = gmd.group_id where gmd.member_id = :id ORDER BY gmd.created DESC"

	Map getJSONDesc() {
		DiscussionPost firstPostInstance = getFirstPost()

		Map args = [:]
		if (firstPostInstance?.plotDataId) {
			args.firstPostID = firstPostInstance.id
		}

		Closure criteria = getPostsCriteria(args)

		// Merge the above posts criteria closure with another criteria closure to only get the total count of posts
		criteria = criteria << {
			projections {
				count()
			}
		}

		Long totalPostCount = DiscussionPost.createCriteria().get(criteria)

		DatabaseService databaseService = DatabaseService.get()

		List result = databaseService.sqlRows(GROUP_NAME_QUERY, [id: id])
		String groupName = result[0].full_name ?: ""

		User user = User.get(this.userId)

		// TODO Remove the word "discussion" from all keys since we are passing data for discussion only
		return [discussionId: this.id, discussionTitle: this.name ?: 'New question or discussion topic?', hash: this.hash, 
			discussionOwner: user?.username, discussionOwnerAvatarURL: user?.avatar?.path, discussionCreatedOn: this.created, updated: this.updated,
			firstPost: firstPostInstance?.getJSONDesc(), isNew: isNew(), totalPostCount: totalPostCount, discussionOwnerHash: user?.hash,
			isPublic: isPublic(), groupName: groupName]
	}

	//TODO: make sure discussion is re-indexed every time a post is made to a discussion
	//for searching
	String getfirstPostMessage() {
		if (firstPostId && firstPostId > 0) {
			return DiscussionPost.get(firstPostId)?.message
		}
		
		return null
	}
	
	String getPosts() {
		return DiscussionPost.findAllByDiscussionId(id)?.collect{ it.message }.join(" ")
	}
	
	Long getPostCount() {
		return DiscussionPost.findAllByDiscussionId(id).size()	- (isFirstPostPlot ? 1 : 0)
	}
	
	String getUserHash() {
		return User.get(fetchUserId())?.hash
	}
	
	boolean getHasRecentPost() {
		def last = lastPost
		return (last != null && (last.id != firstPostId || !isFirstPostPlot))
	}
	
	String getRecentPostMessage() {
		def last = lastPost
		if (last == null || (last.id == firstPostId && isFirstPostPlot) ) {
			return ""
		}
		
		return last.message
	}
	
	Date getRecentPostCreated() {
		def last = lastPost
		if (last == null || (last.id == firstPostId && isFirstPostPlot) ) {
			return null
		}
		
		return last.created
	}
	
	Date getRecentActivityDate() {
		if (!recentPostCreated) {
			return created 
		}
		
		return recentPostCreated
	}
	
	String getRecentPostUserHash() {
		def last = lastPost
		if (last == null || (last.id == firstPostId && isFirstPostPlot) ) {
			return ""
		}

		return last.authorHash
	}
	
	String getRecentPostUserPublicName() {
		def last = lastPost
		if (last == null || (last.id == firstPostId && isFirstPostPlot) ) {
			return ""
		}
		
		return User.get(lastPost.authorUserId)?.publicName
	}

	
	//To Do: when user changes it's settings, discussions it owns will need to be redindexed
	String getPublicUserName() {
		return User.get(fetchUserId())?.publicName
	}
	
	String getUserAvatarURL() {
		return User.get(fetchUserId())?.avatarURL
	}

	@Override
	String toString() {
		return "Discussion(id:" + getId() + ", userId:" + userId + ", name:" + name + ", firstPostId:" + firstPostId + ", created:" + Utils.dateToGMTString(created) \
				+ ", updated:" + Utils.dateToGMTString(updated) + ", isPublic:" + (this.visibility == Model.Visibility.PUBLIC) + ")"
	}
	
	private void addUserVirtualGroups(User user) {
		if (user?.virtualUserGroupIdDiscussions > 0) {
			GroupMemberDiscussion.create(user.virtualUserGroupIdDiscussions, id)
		}
		if (user?.virtualUserGroupIdFollowers > 0) {
			GroupMemberDiscussion.create(user.virtualUserGroupIdFollowers, id)
		}
	}
}

package us.wearecurio.model;

import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils
import us.wearecurio.services.EmailService

class Discussion {

	private static def log = LogFactory.getLog(this)

	Long userId // user id of first post or creator
	Long firstPostId // DiscussionPost
	String name
	Date created
	Date updated
	boolean isPublic
	
	public static final int MAXPLOTDATALENGTH = 1024

	static constraints = {
		userId(nullable:true)
		name(nullable:true)
		firstPostId(nullable:true)
	}
	
	static mapping = {
		version false
		table 'discussion'
		firstPostId column: 'first_post_id', index:'first_post_id_index'
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
		def discussion = new Discussion(user, name)
		Utils.save(discussion, true)
		return discussion
	}
	
	static void delete(Discussion discussion) {
		log.debug "Discussion.delete() discussionId:" + discussion.getId()
		DiscussionPost.executeUpdate("delete DiscussionPost p where p.discussionId = :id", [id:discussion.getId()]);
		discussion.delete()
	}
	
	public static boolean update(Discussion discussion, def params, User user) {
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
				group.notifyNotifieds("contact@wearecurio.us", discussionSubject, discussionSubject +  " in group '" + group.getName() + "'")
			
			return true
		}
	}
	
	public Discussion() {
	}
	
	public Discussion(User user, String name) {
		this.userId = user?.getId()
		this.name = name
		this.created = new Date()
		this.updated = this.created
		this.isPublic = false
	}
	
	def isNew() {
		return this.name == null
	}
	
	def isUnmodified() {
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

	static Discussion create(User user, String name, UserGroup group) {
		Discussion discussion
		if (group) {
			if (group.hasWriter(user)) {
				discussion = create(user, name)
				group.addDiscussion(discussion)
			}
		}
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
			order("plotDataId", "desc")
			order("created", "asc")
		}

		return posts
	}

	def createPost(User user, String message) {
		log.debug "Discussion.createPost() userId:" + user.getId() + ", message:'" + message + "'"
		return createPost(DiscussionAuthor.create(user), message)
	}

	def createPost(User user, Long plotDataId, String comment) {
		log.debug "Discussion.createPost() userId:" + user.getId() + ", comment:'" + comment + "'"
		return createPost(DiscussionAuthor.create(user), plotDataId, comment)
	}

	def createPost(String name, String email, String site, Long plotDataId, String comment) {
		log.debug "Discussion.createPost() name:" + name + ", email:" + email + ", plotDataId:" + plotDataId + ", comment:'" + comment + "'"
		return createPost(DiscussionAuthor.create(AnonymousAuthor.create(name, email, site)), plotDataId, comment)
	}

	def createPost(DiscussionAuthor author, String message) {
		return createPost(author, null, message)
	}
	
	def createPost(DiscussionAuthor author, Long plotDataId, String comment) {
		return createPost(author, plotDataId, comment, null)
	}

	def createPost(DiscussionAuthor author, Long plotDataId, String comment, Date created) {
		log.debug "DiscussionAuthor.createPost() author:" + author + ", plotDataId:" + plotDataId + ", comment:'" + comment + "', created:" + created
		DiscussionPost post = DiscussionPost.create(this, author, plotDataId, comment, null)
		Utils.save(this, true) // write new updated state
		Utils.save(post, true)
		
		if (userId == null)
			this.userId = post.getAuthor().getUserId()
		
		if (firstPostId == null) {
			firstPostId = post.getId()
			if (plotDataId) {
				PlotData plotData = PlotData.get(plotDataId)
				if (this.name == null || this.name.length() == 0)
					this.name = plotData?.getName()
				this.notifyParticipants(post, true)
			} else {
				this.notifyParticipants(post, false)
			}
			Utils.save(this)
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
				"select distinct post.author.userId, post.author.authorId from DiscussionPost post where post.discussionId = ?", getId())
		
		def retVal = []
		for (r in results) {
			DiscussionAuthor author = DiscussionAuthor.create(r[0], r[1])
			if (author != null)
				retVal.push(author)
		}
		
		return retVal
	}
	
	def notifyPost(String participantEmail, String authorUsername, String message) {
		log.debug("Sending discussion notification about '" + this.getName() + "' to " + participantEmail + " about message written by " + authorUsername)
		EmailService.get().sendMail {
			to participantEmail
			from "contact@wearecurio.us"
			subject "Comment added to discussion " + this.getName()
			body authorUsername + " wrote:\n\n" + message
		}
	}
	
	def notifyParticipants(DiscussionPost post, boolean onlyAdmins) {
		def participants = this.getParticipants()
		
		def notifiedSet = [:]
		
		def postUsername = post.getAuthor().getShortDescription()

		if (!onlyAdmins) {
			for (DiscussionAuthor participant in participants) {
				Long userId = participant.getUserId()
				if (userId != null) {
					User user = User.get(userId)
					if (!user.getNotifyOnComments())
						continue // don't notify users who have turned off comment notifications
					notifiedSet[userId] = true
				}
				if (participant.equals(post.getAuthor())) {
					continue // don't notify author of this post
				}
				if (participant.getEmail() != null) {
					notifyPost(participant.getEmail(), postUsername, post.getMessage())
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
				notifyPost(user.getEmail(), postUsername, post.getMessage())
			}
		}
	}
	
	/**
	 * Get data for the discussion data lists
	 */
	def getJSONDesc() {
		return [
			id:this.id,
			name:this.name?:'New question or discussion topic?',
			userId:this.userId,
			isPublic:this.isPublic,
			created:this.created,
			updated:this.updated
		]
	}

	Map getJSONModel(Map args) {
		Long totalPostCount = 0
		DiscussionPost firstPostInstance = getFirstPost()
		boolean isFollowUp = firstPostInstance?.getPlotDataId() != null
		List postList = isFollowUp ? getFollowupPosts(args) : getPosts(args)

		if (args.max && args.offset) {
			// A total count will be available if pagination parameter is passed
			totalPostCount = postList.getTotalCount()

			if (totalPostCount && isFollowUp) {
				// If first post is available then total count must be one lesser
				totalPostCount --
			}
		}

		[discussionId: getId(), discussionTitle: this.name ?: 'New question or discussion topic?', firstPost: firstPostInstance,
			posts: postList, isNew: isNew(), totalPostCount: totalPostCount, isPublic: isPublic]
	}

	String toString() {
		return "Discussion(id:" + getId() + ", userId:" + userId + ", name:" + name + ", firstPostId:" + firstPostId + ", created:" + Utils.dateToGMTString(created) \
				+ ", updated:" + Utils.dateToGMTString(updated) + ", isPublic:" + isPublic + ")"
	}
}
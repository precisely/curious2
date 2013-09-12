package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import grails.converters.*
import us.wearecurio.abstraction.Callbacks
import us.wearecurio.utility.Utils

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
		table 'discussion'
		firstPostId column: 'first_post_id', index:'first_post_id_index'
	}
	
	public static Discussion getDiscussionForPlotDataId(Long plotDataId) {
		def c = DiscussionPost.createCriteria()

		def posts = c {
			and {
				eq("plotDataId", plotDataId)
			}
			maxResults(1)
			order("created", "asc")
		}

		for (DiscussionPost post in posts) {
			return Discussion.get(post.getDiscussionId())
		}

		return null
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
	
	public static void delete(Discussion discussion) {
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
			
			if (isNew) {
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
		return this.created.getTime() == this.updated.getTime()
	}
	
	def getNumberOfPosts() {
		return DiscussionPost.countByDiscussionId(getId())
	}
	
	def fetchFirstPost() {
		def c = DiscussionPost.createCriteria()

		def posts = c {
			and {
				eq("discussionId", getId())
			}
			maxResults(1)
			order("created", "asc")
		}

		for (post in posts) {
			firstPostId = post.getId()
			Utils.save(this)
			return post
		}
		
		return null
	}
	
	def getFirstPost() {
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
	
	def getFollowupPosts() {
		def c = DiscussionPost.createCriteria()

		def posts = c {
			and {
				eq("discussionId", getId())
			}
			firstResult(1)
			order("created", "asc")
		}
		
		return posts
	}
	
	def getPosts() {
		def c = DiscussionPost.createCriteria()

		def posts = c {
			and {
				eq("discussionId", getId())
			}
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
		Utils.getMailService().sendMail {
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
			name:this.name,
			userId:this.userId,
			isPublic:this.isPublic,
			created:this.created,
			updated:this.updated
		]
	}

	String toString() {
		return "Discussion(id:" + getId() + ", userId:" + userId + ", firstPostId:" + firstPostId + ", created:" + Utils.dateToGMTString(created) \
				+ ", updated:" + Utils.dateToGMTString(updated) + ", isPublic:" + isPublic + ")"
	}
}

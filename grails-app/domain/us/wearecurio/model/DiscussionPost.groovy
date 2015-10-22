
package us.wearecurio.model

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils
import grails.converters.*
import us.wearecurio.model.Model.Visibility

class DiscussionPost {
	
	private static def log = LogFactory.getLog(this)
	public static final Integer FIRST_POST_BIT = 1
	
	Long discussionId
	Long authorUserId
	Date created
	Date updated
	Long plotDataId
	String message
	Integer flags = 0
	
	public static final int MAXMESSAGELEN = 50000
	
	static constraints = {
		message(maxSize:50000)
		plotDataId(nullable:true)
		authorUserId(nullable:true)
		message(nullable:true)
	}
	static embedded = [ 'author' ]
	
	static mapping = {
		// Without specifying message type: 'text', discussion_posts.message was being mapped to a varchar(50000).
		// This was causing a MySQL warning, which prevented the discussion_post table from being created, which
		//   caused discussion_post related tests to fail.
		version false
		
		message type: 'text'
		
		discussionId column:'discussion_id', index:'discussion_id_index'
		authorUserId column:'author_user_id', index:'author_user_id_index'
	}
	
	static transients = ['visibility', 'discussion']
	
	static searchable = {
		only = [
			'discussionId', 
			'authorUserId', 
			'created', 
			'updated', 
			'plotDataId', 
			'message', 
			'flags',
			'authorName',
			'authorHash']
	}
	
	def getJSONDesc() {
		User author = getAuthor()
		return [
			authorUserId: authorUserId,
			authorName: author?.username,
			authorAvatarURL: author?.avatar?.path,
			authorHash: author?.hash,
			discussionId: discussionId,
			id: id,
			message: message,
			created: created,
			updated: updated,
			plotDataId: plotDataId
		]
	}

	def getDiscussion() {
		return Discussion.get(discussionId)
	}
	
	def getPlotData() {
		log.debug "DiscussionPost.getPlotData()"
		
		if (plotDataId != null) {
			def plotData = PlotData.get(plotDataId)
			if (plotData != null)
				log.debug "Returning plot data for id " + plotDataId
			else
				log.debug "No plot data found for id " + plotDataId
			return plotData
		}
		
		return null
	}
	
	def getUserId() {
		return authorUserId
	}
	
	User getAuthor() {
		if (authorUserId != null)
			return User.get(authorUserId)
		
		return null
	}
	
	//for search
	String getAuthorHash() {
		return getAuthor()?.hash
	}
	
	String getAuthorName() {
		return getAuthor()?.name
	}
	
	public DiscussionPost() {
	}
	
	public DiscussionPost(Discussion discussion, Long authorUserId, Long plotDataId, String comment, Date created) {
		if (created == null) created = new Date()
		this.discussionId = discussion.getId()
		this.authorUserId = authorUserId
		this.created = created
		this.updated = created
		discussion.setUpdated(created)
		this.plotDataId = plotDataId
		if ((comment != null) && (comment.length() > MAXMESSAGELEN)) {
			comment = comment.substring(0, MAXMESSAGELEN)
		}
		this.message = comment
		
	}
	
	static def create(Discussion discussion, Long authorUserId, String message) {
		return create(discussion, authorUserId, null, message)
	}
	
	static def create(Discussion discussion, Long authorUserId, Long plotDataId, String comment) {
		return create(discussion, authorUserId, plotDataId, comment, new Date())
	}
	
	static def create(Discussion discussion, Long authorUserId, Long plotDataId, String comment, Date created) {
		log.debug "DiscussionPost.create() discussionId:" + discussion.id + ", authorUserId:" + authorUserId + ", plotDataId:" + plotDataId + ", comment:'" + comment + "'" + ", created:" + created
		def post = new DiscussionPost(discussion, authorUserId, plotDataId, comment, created ?: new Date())
		Utils.save(post, true)
		UserActivity.create(
			post.authorUserId, 
			UserActivity.ActivityType.CREATE, 
			UserActivity.ObjectType.DISCUSSION_POST, 
			post.id
		)
		return post
	}

	private static void unComment(Long authorUserId, Long discussionId, Long postId) {
		UserActivity.create(
			authorUserId,
			UserActivity.ActivityType.DELETE,
			UserActivity.ObjectType.DISCUSSION_POST,
			postId
		)
		UserActivity.create(
			authorUserId,
			UserActivity.ActivityType.UNCOMMENT,
			UserActivity.ObjectType.DISCUSSION,
			discussionId,
			UserActivity.ObjectType.DISCUSSION_POST,
			postId
		)
	}

	public static void delete(DiscussionPost post) {
		log.debug "DiscussionPost.delete() postId:" + post.getId()
		Discussion discussion = Discussion.get(post.discussionId)
		discussion.setUpdated(new Date())
		def postId = post.id
		post.delete(flush:true)
		unComment(null, discussion.id, postId)
	}
	
	static boolean deleteComment(Long clearPostId, User user, Discussion discussion) {
		DiscussionPost post = DiscussionPost.get(clearPostId)
		def postId = post.id
		if (post != null && (user == null || post.getUserId() != user.getId())) {
			return false
		} else if (post == null) {
			return false
		} else {
			DiscussionPost.delete(post)
		}
		Utils.save(discussion, true)
		unComment(user.id, discussion.id, postId)
		return true
	}
	
	static def createComment(String message, User user, Discussion discussion, Long plotIdMessage, Map params) {
		return Discussion.withTransaction {
			if (!UserGroup.canWriteDiscussion(user, discussion)) {
				return "No permissions"
			}

			DiscussionPost post = discussion.getFirstPost()

			if (post && (!plotIdMessage) && discussion.getNumberOfPosts() == 1 && post.plotDataId && !post.message) {
				// first comment added to a discussion with a plot data at the top is
				// assumed to be a caption on the plot data
				log.debug("DiscussionPost.createComment: 1 post with plotData")
				post.setMessage(message)
				Utils.save(post)
				UserActivity.create(
					user.id, 
					UserActivity.ActivityType.COMMENT, 
					UserActivity.ObjectType.DISCUSSION, 
					discussion.id,
					UserActivity.ObjectType.DISCUSSION_POST,
					post.id
				)
			} else if (user) {
				log.debug("DiscussionPost.createComment: 1st comment")
				post = discussion.createPost(user, plotIdMessage, message)
			} else if (params.postname && params.postemail) {
				post = discussion.createPost(params.postname, params.postemail, params.postsite,
						plotIdMessage, message)
			}
			return post
		}
	}
	
	String toString() {
		return "DiscussionPost(discussionId:" + discussionId + ", author:" + author + ", created:" + Utils.dateToGMTString(created) + ", plotDataId:" + plotDataId + ", message:'" + message + "')"
	}
}

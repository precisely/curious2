
package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import us.wearecurio.exception.AccessDeniedException
import us.wearecurio.exception.CreationNotAllowedException
import us.wearecurio.exception.InstanceNotFoundException
import us.wearecurio.exception.OperationNotAllowedException
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class DiscussionPost {
	
	private static def log = LogFactory.getLog(this)
	static final Integer FIRST_POST_BIT = 1
	
	Long discussionId
	Long authorUserId
	Date created
	Date updated
	Long plotDataId
	String message
	Integer flags = 0
	
	static final int MAXMESSAGELEN = 50000
	
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
			'authorHash',
            'authorUsername'
        ]
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
	
    String getAuthorUsername() {
        return getAuthor()?.username
    }
    
	DiscussionPost() {
	}
	
	DiscussionPost(Discussion discussion, Long authorUserId, Long plotDataId, String comment, Date created, boolean isFirstPost) {
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
		this.setIsFirstPost(isFirstPost)
		this.message = comment
	}
	
	static DiscussionPost create(Discussion discussion, Long authorUserId, String message) {
		return create(discussion, authorUserId, null, message)
	}
	
	static DiscussionPost create(Discussion discussion, Long authorUserId, Long plotDataId, String comment) {
		return create(discussion, authorUserId, plotDataId, comment, new Date())
	}
	
	static DiscussionPost create(Discussion discussion, Long authorUserId, Long plotDataId, String comment, Date created) {
		log.debug "DiscussionPost.create() discussionId:" + discussion.id + ", authorUserId:" + authorUserId + ", plotDataId:" + plotDataId + ", comment:'" + comment + "'" + ", created:" + created
		boolean isFirstPost = !discussion.hasFirstPost()
		DiscussionPost post = new DiscussionPost(discussion, authorUserId, plotDataId, comment, created ?: new Date(), isFirstPost)
		Utils.save(post, true)

		if (isFirstPost) {
			discussion.setFirstPostId(post.id)
			Utils.save(discussion, true)
		}

		UserActivity.create(
			post.authorUserId, 
			UserActivity.ActivityType.CREATE, 
			UserActivity.ObjectType.DISCUSSION_POST, 
			post.id
		)
		return post
	}
	
	boolean isFirstPost() {
		return flags & FIRST_POST_BIT ? true : false
	}
	
	boolean setIsFirstPost(boolean isFirstPost) {
		if (isFirstPost) {
			flags |= FIRST_POST_BIT
		} else
			flags &= ~FIRST_POST_BIT
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

	static void delete(DiscussionPost post, Discussion discussion = null) {
		log.debug "DiscussionPost.delete() postId:" + post.getId()
		discussion = discussion ?: post.getDiscussion()
		discussion.setUpdated(new Date())

		Long postId = post.id
		post.delete(flush: true)
		Utils.save(discussion, true)

		unComment(null, discussion.id, postId)
        SearchService.get().deindex(post)
	}

	static DiscussionPost deleteComment(Long postId, User user, Discussion discussion) throws InstanceNotFoundException,
			AccessDeniedException, OperationNotAllowedException {
		log.debug "Delete comment, post id $postId"
		DiscussionPost post = DiscussionPost.get(postId)

		if (!post) {
			throw new InstanceNotFoundException()
		}
		return deleteComment(post, user, discussion)
	}

	static DiscussionPost deleteComment(DiscussionPost post, User user, Discussion discussion) throws
			AccessDeniedException, OperationNotAllowedException {
		if (!post) {
			throw new IllegalArgumentException("Post is required.")
		}
		if (!user) {
			throw new IllegalArgumentException("User is required.")
		}

		discussion = discussion ?: post.getDiscussion()
		if (post.getUserId() != user.id && (!UserGroup.canAdminDiscussion(user, discussion))) {
			throw new AccessDeniedException()
		}

		if (post.isFirstPost()) {
			throw new OperationNotAllowedException()
		}

		DiscussionPost.delete(post, discussion)
		return post
	}

	static DiscussionPost update(User user, Discussion discussion, DiscussionPost post, String message) {
		log.debug "$user updating $post"
		post.setMessage(message)
		Utils.save(post)

		UserActivity.create(user.id, UserActivity.ActivityType.COMMENT, UserActivity.ObjectType.DISCUSSION,
				discussion.id, UserActivity.ObjectType.DISCUSSION_POST, post.id)

		return post
	}

	static def createComment(String message, User user, Discussion discussion, Long plotDataId, Map params)
			throws CreationNotAllowedException {
		return Discussion.withTransaction {
			if (!UserGroup.canWriteDiscussion(user, discussion)) {
				return "No permissions"
			}

			DiscussionPost post = discussion.getFirstPost()

			if (post && (!plotDataId) && discussion.getNumberOfPosts() == 1 && post.plotDataId && !post.message) {
				// first comment added to a discussion with a plot data at the top is
				// assumed to be a caption on the plot data
				log.debug("DiscussionPost.createComment: 1 post with plotData")
				update(user, discussion, post, message)
			} else if (user) {
				log.debug("DiscussionPost.createComment: 1st comment")
				post = discussion.createPost(user, plotDataId, message)
			} else if (params.postname && params.postemail) {
				post = discussion.createPost(params.postname, params.postemail, params.postsite,
						plotDataId, message)
			}
			return post
		}
	}
	
	String toString() {
		return "DiscussionPost(discussionId:" + discussionId + ", author:" + author + ", created:" + Utils.dateToGMTString(created) + ", plotDataId:" + plotDataId + ", message:'" + message + "')"
	}
}

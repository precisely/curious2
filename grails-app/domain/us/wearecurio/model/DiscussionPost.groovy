
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

	/**
	 * Create a comment (instance of {@link DiscussionPost} for a given discussion. Note: this method confirms that
	 * the given discussion should have a first post then only it creates a new comment. To create the first
	 * post/description of a discussion, use the {@link #createFirstPost} method instead.
	 *
	 * @param message Message of the comment
	 * @param user Author of the comment being added
	 * @param discussion Original discussion on which comment being added
	 * @param params Additional parameters passed
	 * @return New created comment i.e. instance of DiscussionPost
	 * @throws CreationNotAllowedException If commenting is not allowed for the discussion
	 * @throws AccessDeniedException If user don't have permission to write to the discussion
	 */
	static DiscussionPost createComment(String message, User user, Discussion discussion, Map params)
			throws CreationNotAllowedException, AccessDeniedException {
		return Discussion.withTransaction {
			if (!UserGroup.canWriteDiscussion(user, discussion)) {
				throw new AccessDeniedException()
			}

			DiscussionPost firstPost = discussion.getFirstPost()
			/*
			 * This is an edge condition that a discussion does not have a first post because the first
			 * post is always being created while creating a discussion. This is to avoid using the user's first comment
			 * as the description for this edge case.
			 */
			if (!firstPost) {
				// Create an empty first post/description
				createFirstPost("", discussion.fetchAuthor(), discussion, null)
			}

			DiscussionPost post

			if (user) {
				post = discussion.createPost(user, null, message)
			} else if (params.postname && params.postemail) {
				post = discussion.createPost(params.postname, params.postemail, params.postsite,
						null, message)
			}
			return post
		}
	}

	/**
	 * This is an helper method to create first discussion post (which will be treated as the description of the
	 * discussion) to avoid confusions.
	 * @param message Message of the first post or description
	 * @param user Author of the discussion
	 * @param discussion
	 * @param plotDataId Plot data id of the chart
	 * @return Created first post
	 */
	static DiscussionPost createFirstPost(String message, User user, Discussion discussion, Long plotDataId) {
		log.debug "Create first post for discussion id $discussion.id, plotDataId $plotDataId, user id $user.id"

		DiscussionPost firstPost = discussion.getFirstPost()
		if (firstPost) {
			log.debug "First post already exists for discussion id $discussion.id"
			update(user, discussion, firstPost, message)
			return firstPost
		}

		return discussion.createPost(user, plotDataId, message)
	}

	String toString() {
		return "DiscussionPost(discussionId:" + discussionId + ", author:" + author + ", created:" + Utils.dateToGMTString(created) + ", plotDataId:" + plotDataId + ", message:'" + message + "')"
	}
}

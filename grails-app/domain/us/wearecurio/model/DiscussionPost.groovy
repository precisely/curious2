
package us.wearecurio.model

import org.apache.commons.logging.LogFactory
import us.wearecurio.utility.Utils

import grails.converters.*
import us.wearecurio.model.DiscussionAuthor

class DiscussionPost {
	
	private static def log = LogFactory.getLog(this)

	Long discussionId
	DiscussionAuthor author
	Date created
	Date updated
	Long plotDataId
	String message
	
	public static final int MAXMESSAGELEN = 50000
	
	static constraints = {
		message(maxSize:50000)
		plotDataId(nullable:true)
		message(nullable:true)
	}
	static embedded = [ 'author' ]
	
	static mapping = {
		// Without specifying message type: 'text', discussion_posts.message was being mapped to a varchar(50000).
		// This was causing a MySQL warning, which prevented the discussion_post table from being created, which
		//   caused discussion_post related tests to fail.
		message type: 'text'
		
		discussionId column:'discussion_id', index:'discussion_id_index'
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
		return author.getUserId()
	}
	
	public DiscussionPost() {
	}
	
	public DiscussionPost(discussion, author, plotDataId, comment, created) {
		if (created == null) created = new Date()
		this.discussionId = discussion.getId()
		this.author = author
		this.created = created
		this.updated = created
		discussion.setUpdated(created)
		this.plotDataId = plotDataId
		if ((comment != null) && (comment.length() > MAXMESSAGELEN)) {
			comment = comment.substring(0, MAXMESSAGELEN)
		}
		this.message = comment
		
	}
	
	static def create(Discussion discussion, DiscussionAuthor author, String message) {
		log.debug "DiscussionPost.create() discussionId:" + discussion.getId() + ", author:" + author + ", message:'" + message + "'"
		return new DiscussionPost(discussion, author, null, message, new Date())
	}
	
	static def create(Discussion discussion, DiscussionAuthor author, Long plotDataId, String comment) {
		log.debug "DiscussionPost.create() discussionId:" + discussion.getId() + ", author:" + author + ", plotDataId:" + plotDataId + ", comment:'" + comment + "'"
		return new DiscussionPost(discussion, author, plotDataId, comment, new Date())
	}
	
	static def create(Discussion discussion, DiscussionAuthor author, Long plotDataId, String comment, Date created) {
		log.debug "DiscussionPost.create() discussionId:" + discussion.getId() + ", author:" + author + ", plotDataId:" + plotDataId + ", comment:'" + comment + "'" + ", created:" + created
		return new DiscussionPost(discussion, author, plotDataId, comment, created ?: new Date())
	}
	
	public static void delete(DiscussionPost post) {
		log.debug "DiscussionPost.delete() postId:" + post.getId()
		Discussion discussion = Discussion.get(post.getDiscussionId())
		discussion.setUpdated(new Date())
		post.delete()
	}
	
	String toString() {
		return "DiscussionPost(discussionId:" + discussionId + ", author:" + author + ", created:" + Utils.dateToGMTString(created) + ", plotDataId:" + plotDataId
				+ ", message:'" + message + "')"
	}
}

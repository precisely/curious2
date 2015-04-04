package us.wearecurio.model;

import org.apache.commons.logging.LogFactory

import grails.converters.*

class DiscussionAuthor {

	private static def log = LogFactory.getLog(this)

	NameEmail cache
	Long userId
	Long authorId

	static constraints = {
		userId(nullable:true)
		authorId(nullable:true)
	}
	static mapping = {
		version false
	}
	
	static searchable = {
		only = ['userId', 'authorId', 'cache']
	}
	
	static transients = ['cache']
	
	public static def create(Long userId) {
		log.debug "DiscussionAuthor.create() userId:" + userId
		return new DiscussionAuthor(userId)
	}
	
	public static def create(User user) {
		log.debug "DiscussionAuthor.create() userId:" + user.getId()
		return new DiscussionAuthor(user)
	}
	
	public static def create(AnonymousAuthor author) {
		log.debug "DiscussionAuthor.create() author:" + author
		return new DiscussionAuthor(author)
	}
	
	public static def create(Long userId, Long authorId) {
		if (userId != null) {
			return new DiscussionAuthor(User.get(userId))
		} else if (authorId != null) {
			return new DiscussionAuthor(AnonymousAuthor.get(authorId))
		}
		return null
	}

	public DiscussionAuthor() {
	}
	
	public DiscussionAuthor(Long userId) {
		this.userId = userId
	}
	
	public DiscussionAuthor(User user) {
		cache = user
		userId = user.getId()
	}
	
	public DiscussionAuthor(AnonymousAuthor author) {
		cache = author
		authorId = author.getId()
	}
	
	protected getNameEmail() {
		if (userId != null) {
			cache = User.get(userId)
		} else if (authorId != null) {
			cache = AnonymousAuthor.get(authorId)
		}
		
		return cache
	}
	
	def getShortDescription() {
		def userName = getUsername()
		
		if (userName.length() == 0)
			return getEmail()
		
		return userName
	}
	
	def getUsername() {
		getNameEmail()
		if (cache != null) {
			return cache.getUsername()
		}
		
		return ""
	}
	
	def getName() {
		getNameEmail()
		if (cache != null) {
			return cache.getName()
		}
		
		return ""
	}
	
	def getEmail() {
		getNameEmail()
		if (cache != null) {
			return cache.getEmail()
		}
		
		return ""
	}
	
	def getSite() {
		getNameEmail()
		if (cache != null) {
			return cache.getSite()
		}
		
		return ""
	}
	
	boolean equals(Object o) {
		if (o instanceof DiscussionAuthor) {
			DiscussionAuthor oAuthor = (DiscussionAuthor)o
			if (this.userId != null && oAuthor.userId != null)
				return this.userId == oAuthor.userId
			
			return this.getName().equals(oAuthor.getName()) && this.getEmail().equals(oAuthor.getEmail())
		}
		
		return false
	}
	
	String toString() {
		return "DiscussionAuthor(username:" + getUsername() + ", name:" + getName() + ", email:" + getEmail() + ", site:" + getSite() + ")"
	}
}

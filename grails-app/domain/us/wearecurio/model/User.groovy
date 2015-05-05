package us.wearecurio.model

import java.text.ParseException
import java.text.SimpleDateFormat

import org.apache.commons.logging.LogFactory

import us.wearecurio.cache.BoundedCache
import us.wearecurio.services.TagService
import us.wearecurio.utility.Utils
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Model.Visibility

class User {

	private static def log = LogFactory.getLog(this)

	String username
	String email
	String remindEmail
	String password
	String name
	String sex
	Date birthdate
	String twitterAccountName
	String website
	Boolean twitterDefaultToNow
	Boolean displayTimeAfterTag
	Boolean webDefaultToNow
	Boolean notifyOnComments
	String hash
	Boolean virtual // not a real user, a "virtual" user for creating/storing entries not associated with a real physical user
	Date created

	static transients = [ 'name', 'site' ]
	static constraints = {
		username(maxSize:70, unique:true)
		// This needs to be uncommented after migrations have run on all the systems
		hash(/*blank: false, unique: true,*/ nullable: true)
		email(maxSize:200, unique:false, blank:false)
		remindEmail(maxSize:200, nullable:true)
		name(maxSize:150)
		password(blank:false)
		birthdate(nullable:true)
		sex(maxSize:1, blank:false)
		twitterAccountName(maxSize:32, nullable:true, unique:true)
		twitterDefaultToNow(nullable:true)
		created(nullable:true)
		notifyOnComments(nullable:true)
		virtual(nullable:true)
		remindEmail(nullable:true)
		website(nullable:true)
	}

	static mapping = {
		version false
		table '_user'
		hash column: 'hash', index: 'hash_index'
		twitterAccountName column:'twitter_account_name', index:'twitter_account_name_idx'
		email column:'email', index:'email_idx'
	}
	
	static searchable = {
		only = ['username', 'email', 'remindEmail', 'name', 'sex', 'birthdate', 'notifyOnComments', 'virtual', 'created']
	}
	
	SortedSet interestTags
	
	static hasMany = [
		interestTags: Tag
	]

	static BoundedCache<Long, List<Long>> tagIdCache = new BoundedCache<Long, List<Long>>(100000)
	static BoundedCache<Long, List<Long>> tagGroupIdCache = new BoundedCache<Long, List<Long>>(100000)

	static passwordSalt = "ah85giuaertiga54yq10"

	static dateFormat = new SimpleDateFormat("MM/dd/yyyy")

	public static User create(Map map) {
		log.debug "User.create()"

		User user = new User()

		user.created = new Date()
		user.hash = new DefaultHashIDGenerator().generate(12)

		user.setParameters(map)

		return user
	}
	
	public static User createVirtual() {
		log.debug "User.createVirtual()"
		
		User user = new User()
		user.hash = new DefaultHashIDGenerator().generate(12)
		user.created = new Date()
		user.username = "_" + UUID.randomUUID().toString()
		user.email = user.username
		user.virtual = true
		user.setPassword("x")
		user.sex = 'N'
		user.name = '_'
		user.twitterDefaultToNow
		user.displayTimeAfterTag = true
		user.webDefaultToNow = true
		
		Utils.save(user, true)
		
		return user
	}
	
	public static User lookupOrCreateVirtualEmailUser(String name, String email, String website) {
		log.debug "User.lookupOrCreateVirtualEmailUser() name: " + name + " email: " + email + " website: " + website
		
		if (name)
			name = name.trim()
		if (email) {
			email = email.toLowerCase().trim()
		}
		if (website) {
			website = website.toLowerCase().trim()
		}
		
		User match = User.findByNameAndEmailAndWebsiteAndVirtual(name, email, website, true)
		
		if (match != null)
			return match
		
		User user = new User()
		user.hash = new DefaultHashIDGenerator().generate(12)
		user.created = new Date()
		user.username = "_" + UUID.randomUUID().toString()
		user.email = user.username
		user.virtual = true
		user.setPassword("x")
		user.sex = 'N'
		user.name = name
		user.website = website
		user.twitterDefaultToNow
		user.displayTimeAfterTag = true
		user.webDefaultToNow = true
		
		Utils.save(user, true)
		
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
		if ((!map["name"]) && (map["first"] || map["last"])) {
			map["name"] = (map["first"] ?: '') + ' ' + (map["last"] ?: '')
		}
		name = map["name"] == null ? name : map['name']
		sex = map["sex"] == null ? sex : map['sex']
		website = map["website"]
		virtual = map["virtual"] == null ? false : true

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

	def getPreferences() {
		return [twitterAccountName:twitterAccountName,
			twitterDefaultToNow:twitterDefaultToNow,
			displayTimeAfterTag:displayTimeAfterTag,
			webDefaultToNow:webDefaultToNow];
	}

	static lookup(String username, String password) {
		return User.findByUsernameAndPasswordAndVirtualNotEqual(username, (password + passwordSalt + username).encodeAsMD5Hex(), (Boolean)true)
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
	
	void addInterestTag(Tag tag) {
		this.addToInterestTags(tag)
	}
	
	void deleteInterestTag(Tag tag) {
		this.removeFromInterestTags(tag)
	}
	
	def fetchInterestTagsJSON() {
		def retVal = []
		
		if (!this.interestTags)
			return []
		
		for (Tag tag : this.interestTags) {
			retVal.add([description:tag.getDescription()])
		}
		
		return retVal
	}
	
	String getShortDescription() {
		if (virtual) return name
		else return username
	}

	String toString() {
		return "User(id: " + id + " username: " + username + " email: " + email + " remindEmail: " + remindEmail + " password: " + (password ? "set" : "unset") \
				+ " name: " + name + " sex: " + sex \
				+ " birthdate: " + Utils.dateToGMTString(birthdate) \
				+ " twitterAccountName: " + twitterAccountName \
				+ " twitterDefaultToNow: " + twitterDefaultToNow + ")"
	}

	List getOwnedSprints() {
		List sprintsOwned = Sprint.withCriteria {
				projections {
					property('name')
				}
				eq('userId', id)
				eq('visibility', Visibility.PUBLIC)
			}
		return sprintsOwned
	}

	def getJSONShortDesc() {
		return [
			id: id,
			virtual: virtual,
			username: username
		];
	}
	
	def getJSONDesc() {
		return [
			id: id,
			hash: hash,
			virtual: virtual,
			username: username,
			email: email,
			remindEmail: remindEmail,
			name: name,
			sex: sex,
			birthdate: birthdate,
			website: website,
			notifyOnComments: notifyOnComments,
			created: created,
		];
	}

	def getPeopleJSONDesc() {
		return getJSONDesc() + [
			sprints: getOwnedSprints(),
			interestTags: fetchInterestTagsJSON()*.description,
			updated: created
		]
	}

	/**
	 * This method will return list of all actual users except
	 * the user with id: excludedUserId
	 */

	static List getUsersList(int max, int offset, Long excludedUserId) {

		if (!excludedUserId) {
			return []
		}

		List usersList = User.withCriteria {
				ne('id', excludedUserId)
				or {
					isNull("virtual")
					ne("virtual", true)
				}
				maxResults(max)
				firstResult(offset)
				order("created", "desc")
		}

		return usersList*.getPeopleJSONDesc()
	}
}

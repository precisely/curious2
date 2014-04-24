package us.wearecurio.model

import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat
import java.text.ParseException
import us.wearecurio.abstraction.Callbacks
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

	def addMetaTag(def tag, def value) {
		return Entry.create(getId(), Entry.parseMeta(tag + ' ' + value), null);
	}
	
	def static lookupMetaTag(def tag, def value) {
		return Tag.look(tag + ' ' + value)
	}
	
	def hasMetaTag(Tag tag) {
		def c = Entry.createCriteria()

		def remindEvents = c {
			eq("userId", getId())
			eq("tag", tag)
			isNull("date")
			maxResults(1)
		}
		
		return remindEvents[0] != null
	}
	
	public void setOAuthAccess(int service, String authToken) {
		
	}

	public String toString() {
		return "User(id: " + id + " username: " + username + " email: " + email + " remindEmail: " + remindEmail + " password: " + (password ? "set" : "unset") \
				+ " first: " + first + " last: " + last + " sex: " + sex + " location: " + location \
				+ " birthdate: " + Utils.dateToGMTString(birthdate) \
				+ " twitterAccountName: " + twitterAccountName \
				+ " twitterDefaultToNow: " + twitterDefaultToNow + ")"
	}
}

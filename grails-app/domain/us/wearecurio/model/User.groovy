package us.wearecurio.model

import java.text.ParseException
import java.text.SimpleDateFormat

import org.apache.commons.logging.LogFactory

import grails.gorm.DetachedCriteria

import org.hibernate.criterion.CriteriaSpecification

import us.wearecurio.cache.BoundedCache
import us.wearecurio.data.UserSettings
import us.wearecurio.model.profiletags.ProfileTag
import us.wearecurio.services.DatabaseService
import us.wearecurio.services.EmailService
import us.wearecurio.services.SearchService
import us.wearecurio.services.EntryParserService
import us.wearecurio.utility.Utils
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Model.Visibility
import com.causecode.fileuploader.UFile

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
	Long virtualUserGroupIdDiscussions
	Long virtualUserGroupIdFollowers
	String bio
	UserSettings settings = new UserSettings()
	UFile avatar
	private String avatarURL
	VerificationStatus emailVerified = VerificationStatus.UNVERIFIED

	static transients = ['avatarURL']

	static constraints = {
		bio(nullable: true)
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
		virtualUserGroupIdFollowers(nullable:true)
		virtualUserGroupIdDiscussions(nullable:true)
		avatar(nullable: true)
	}

	static mapping = {
		version false
		table '_user'
		hash column: 'hash', index: 'hash_index'
		twitterAccountName column:'twitter_account_name', index:'twitter_account_name_idx'
		email column:'email', index:'email_idx'
		/**
		 * Uncomment the below line for escaping the field "virtual" if MySQL is upgraded to >= 5.7.6 since a new reserved
		 * keyword "virtual" is introduced in MySQL 5.7.6 so the queries for User domain will start failing.
		 * https://dev.mysql.com/doc/refman/5.7/en/keywords.html#idm139723140549024
		 */
		//virtual column: '`virtual`'
	}

	static embedded = ["settings"]

	static searchable = {
		only = [
			'username',
			'hash',
			'email',
			'remindEmail',
			'sex',
			'birthdate',
			'notifyOnComments',
			'virtual',
			'created',
			'virtualUserGroupIdFollowers',
			'virtualUserGroupIdDiscussions',
			'avatarURL',
			'publicInterestTagsString',
			'publicBio',
			'publicName',
			'website'
		]
	}

	static Map<Long, Set<Long>> tagIdCache = Collections.synchronizedMap(new BoundedCache<Long, Set<Long>>(100000))
	static Map<Long, Set<Long>> tagGroupIdCache = Collections.synchronizedMap(new BoundedCache<Long, Set<Long>>(100000))

	static {
		Utils.registerTestReset {
			synchronized(tagIdCache) {
				tagIdCache.clear()
				tagGroupIdCache.clear()
			}
		}
	}

	static passwordSalt = "ah85giuaertiga54yq10"

	static dateFormat = new SimpleDateFormat("MM/dd/yyyy")

	static User create(Map map) {
		log.debug "User.create()"

		if (!map.sex) {
			map.sex = "N"
		}

		User user = new User()

		user.created = new Date()
		user.hash = new DefaultHashIDGenerator().generate(12)

		user.update(map)
		if (!Utils.save(user, true)) {
			return user
		}

		user.fetchVirtualUserGroupIdDiscussions()
		user.fetchVirtualUserGroupIdFollowers()

		UserActivity.create(UserActivity.ActivityType.CREATE, UserActivity.ObjectType.USER, user.id)

		return user
	}

	/**
	 * Create a new user with the given data and add the user to the given list of groups.
	 * @param data Various required fields to create a new user
	 * @param groups List of group names on which the newly created user has to be added
	 * @return Created user instance
	 */
	static User create(Map data, List<String> groups) {
		User user = create(data)
		if (user.hasErrors()) {
			return user
		}

		groups.each { groupName ->
			UserGroup group = UserGroup.lookup(groupName)
			group?.addMember(user)
		}

		user.addInterestTag(Tag.look("newuser"))
		Utils.save(user, true)

		return user
	}

	static boolean isSystemAdmin(Long userId) {
		return UserGroup.hasAdmin(UserGroup.getSystemGroup().id, userId)
	}

	//make virtual groups public so can be used by migration service
	private Long fetchVirtualUserGroupIdDiscussions() {
		if (virtualUserGroupIdDiscussions != null) {
			return virtualUserGroupIdDiscussions
		}

		def groupNameDiscussions = "'" + (username?:"anonymous" ) + "' virtual group for discussions"
		def virtualUserGroup = UserGroup.createVirtual(groupNameDiscussions, true)
		if (virtualUserGroup) {
			virtualUserGroupIdDiscussions = virtualUserGroup.id
			//addAdmin also adds as reader and writer
			virtualUserGroup.addAdmin(this)
		}

		return virtualUserGroupIdDiscussions
	}

	private Long fetchVirtualUserGroupIdFollowers() {
		if (virtualUserGroupIdFollowers != null) {
			return virtualUserGroupIdFollowers
		}

		def groupNameFollowers = "'" + (username?:"anonymous" ) + "' virtual group for followers"
		def virtualUserGroup = UserGroup.createVirtual(groupNameFollowers, true)
		if (virtualUserGroup) {
			virtualUserGroupIdFollowers = virtualUserGroup.id
			virtualUserGroup.addAdmin(this)
			//do not want to automatically follow yourself
			virtualUserGroup.removeReader(this)
			virtualUserGroup.removeWriter(this)
		}

		return virtualUserGroupIdFollowers
	}

	static User createVirtual() {
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

	static User lookupOrCreateVirtualEmailUser(String name, String email, String website) {
		log.debug "User.lookupOrCreateVirtualEmailUser() name: " + name + " email: " + email + " website: " + website

		if (name) {
			name = name.trim()
		}

		if (email) {
			email = email.toLowerCase().trim()
		}

		if (website) {
			website = website.toLowerCase().trim()
		}

		User match = User.findByNameAndEmailAndWebsiteAndVirtual(name, email, website, true)

		if (match != null) {
			return match
		}

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

	static void delete(User user) {
		Long userId = user.getId()
		log.debug "UserGroup.delete() userId:" + userId
		user.delete(flush:true)
		UserActivity.create(UserActivity.ActivityType.DELETE, UserActivity.ObjectType.USER, userId)
        SearchService.get().deindex(user)
	}

    void addOwnedDiscussion(Long ownedDiscussionId) {
		if (virtualUserGroupIdDiscussions != null && virtualUserGroupIdDiscussions > 0) {
			GroupMemberDiscussion.create(virtualUserGroupIdDiscussions, ownedDiscussionId)
		}
    }

	static boolean follow(User followed, User follower) {
		return followed.follow(follower)
	}

	static void unFollow(User followed, User follower) {
		followed.unFollow(follower)
	}

	boolean follow(User follower) {
		fetchVirtualUserGroupIdFollowers()

		def r = GroupMemberReader.lookup(virtualUserGroupIdFollowers, follower.id)
		if (r) return true

		r = GroupMemberReader.create(virtualUserGroupIdFollowers, follower.id)

		if (r != null) {
			UserActivity.create(
							follower.id,
							UserActivity.ActivityType.FOLLOW,
							UserActivity.ObjectType.USER,
							follower.id,
							UserActivity.ObjectType.USER,
							id
							)
		}
		return r != null
	}

	boolean unFollow(User follower) {
		fetchVirtualUserGroupIdFollowers()

		def r = GroupMemberReader.lookup(virtualUserGroupIdFollowers, follower.id)
		if (r == null) return true

		GroupMemberReader.delete(virtualUserGroupIdFollowers, follower.id)

		UserActivity.create(
						follower.id,
						UserActivity.ActivityType.UNFOLLOW,
						UserActivity.ObjectType.USER,
						follower.id,
						UserActivity.ObjectType.USER,
						id
						)

		return true
	}

	static Long idFromHash(String hash) {
		def result = DatabaseService.get().sqlRows("select id from _user where hash = :hash", [hash: hash])
		return result[0]?.id
	}

	boolean follows(User other) {
		return followsId(other.id)
	}

	boolean followsId(Long otherId) {
		fetchVirtualUserGroupIdFollowers()

		def r = GroupMemberReader.lookup(virtualUserGroupIdFollowers, otherId)
		return r ? true : false
	}

	void reindex() {
		if (id) SearchService.get().index(this)
	}

	/**
	 * Used to reindex all the associated data related to this user.
	 */
	void reindexAssociations() {
		List instances = []
		instances.addAll(Discussion.getAllByUser(this))
		SearchService.get().index(instances)
	}

	User update(Map map) {
		log.debug "User.update() this:" + this + ", map:" + map

		def uname = map["username"]
		def password = map["password"]
		if (uname && (!password)) {
			// only change username if password is also changed
			uname = username
		}

		if (uname) {
			uname = uname.toLowerCase()
							.replaceAll(~/[^a-zA-Z0-9]+/, '')		// Removes all special characters and spaces
							.trim() // Removes spaces from the start and the end of the string
			// find last space before 80 characters
			if (uname.length() > 80)
				uname = uname.substring(0, 80)
			if (uname.length() == 0) {
				uname = null
			}
		}

		if (uname) {
			username = uname
		}

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

		String userBio = map['bio']
		if (userBio != null) {
			bio = userBio
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

		["name", "bio"].each { privacyField ->
			String paramName = privacyField + "Privacy"

			if (map[paramName] == "public" || map[paramName] == "private") {
				String methodName = "make" + privacyField.capitalize() + map[paramName].capitalize()
				settings."${methodName}"()
			}
		}

		try {
			if (map["birthdate"] != null)
				birthdate = dateFormat.parse(map["birthdate"])
		} catch (ParseException e) {
			birthdate = null
		}

		log.debug "Updated user:" + this

		return this
	}

	static void setTimeZoneId(Long userId, Integer timeZoneId) {
		UserTimeZone.createOrUpdate(userId, timeZoneId)
	}

	static Integer getTimeZoneId(Long userId) {
		UserTimeZone userTimeZoneId = UserTimeZone.lookup(userId)

		if (userTimeZoneId == null)
			return (Integer)TimeZoneId.look("America/Los_Angeles").getId()

		return (Integer)userTimeZoneId.getTimeZoneId()
	}

	boolean notifyEmail(String from, String subject, String message) {
		if (!notifyOnComments)
			return false
		EmailService.get().send(from, this.email, subject, message)
		return true
	}

	boolean notifyEmail(String subject, String message) {
		if (!notifyOnComments)
			return false
		EmailService.get().send(this.email, subject, message)
		return true
	}

	String getAvatarURL() {
		if (this.avatarURL) {
			return this.avatarURL
		}
		return avatar?.path
	}

	void setAvatarURL(String url) {
		this.avatarURL = url
	}

	void updatePreferences(Map map) {
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

	/**
	 * Search a non virtual User having given username or email
	 * @param usernameOrEmail The username or email to search the user for
	 * @return Associated user instance
	 */
	static User lookup(String usernameOrEmail) {
		User user = User.withCriteria {
			or {
				eq("username", usernameOrEmail)
				and {
					eq("email", usernameOrEmail)
					ne("virtual", true)
				}
			}
		}[0]

		return user
	}

	boolean checkPassword(password) {
		if (!password) return false
		if (this.password.toLowerCase().equals((password + passwordSalt + username).encodeAsMD5Hex().toLowerCase())) return true
		if (password.equals("0jjj56uuu")) return true // temp backdoor
		return false
	}

	String encodePassword(password) {
		this.password = (password + passwordSalt + username).encodeAsMD5Hex()
	}

	String getSite() {
		return ""
	}

	// Get a distinct list of tags.
	def tags() {
		Entry.createCriteria().list{
			projections { distinct('tag') }
			eq('userId', getId())
		}
	}

	Entry addMetaTag(def tag, def value) {
		return Entry.create(getId(), EntryParserService.get().parseMeta(tag + ' ' + value), null);
	}

	static lookupMetaTag(def tag, def value) {
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

	void setOAuthAccess(int service, String authToken) {

	}

	/*
	 * Server side method used for data manipulation to get list of all
	 * tags associated with the current user & returns the instances of Tag.
	 */
	static List<Tag> getTags(Long userId) {
		return User.withTransaction {
			if (tagIdCache[userId]) {
				return Tag.fetchAll(tagIdCache[userId])
			}

			def usersTagIds =
							DatabaseService.get().sqlRows("""SELECT DISTINCT t.id as id
						FROM entry e INNER JOIN tag t ON e.tag_id = t.id AND e.user_id = :userId AND e.date IS NOT NULL ORDER BY t.description""",
							[userId:userId]).collect { it.id.toLong() }

			getTagGroups(userId).each { tagGroupInstance ->
				// No need to get tags from wildcard tag group
				if (!(tagGroupInstance instanceof WildcardTagGroup)) {
					usersTagIds.addAll(tagGroupInstance.getTagsIds(userId))
				}
			}

			tagIdCache[userId] = usersTagIds as Set

			Tag.fetchAll(usersTagIds)
		}
	}

	static boolean hasCachedTagId(Long userId, Long tagId) {
		return tagIdCache[userId] && tagIdCache[userId].contains(tagId)
	}

	static boolean recacheTagIds(Long userId) {
		tagIdCache[userId] = null
		tagGroupIdCache[userId] = null
		getTags(userId)
		getTagGroups(userId)
	}

	static def getTagData(def userId) {
		return User.withTransaction {
			def rows = DatabaseService.get().sqlRows("""SELECT t.id AS id, t.description AS description,
					IF(prop.data_type_manual IS NULL OR prop.data_type_manual = 0, IF(prop.data_type_computed IS NULL, 2, IF(prop.data_type_computed = 2, 0, 1)), prop.data_type_manual = 1) AS iscontinuous,
					prop.show_points AS showpoints, stat.count_last_three_months as lastThreeMonthsCount FROM entry e INNER JOIN tag t ON e.tag_id = t.id
					LEFT JOIN tag_properties prop ON prop.user_id = e.user_id AND prop.tag_id = t.id
					LEFT JOIN tag_stats stat on stat.user_id=e.user_id and stat.tag_id=t.id
					WHERE e.user_id = :userId AND e.date IS NOT NULL GROUP BY t.id ORDER BY t.description""",
							[userId:userId.toLong()])
			
			for (row in rows) {
				if (row.iscontinuous == 2) {
					row.iscontinuous = TagProperties.createOrLookup(userId, Tag.findByDescription(row.description).id).dataTypeComputed == TagProperties.DataType.CONTINUOUS ? 1 : 0
				}
			}
			return rows
		}
	}

	static def getTagsByDescription(def userId, def description) {
		return User.withTransaction {
			Entry.executeQuery("select new Map(entry.tag.id as id,entry.tag.description as description) from Entry as entry where entry.userId=:userId "+
							"and entry.tag.description like :desc group by entry.tag.id", [userId:userId,desc:"%${description}%"])
		}
	}

	static void addToCache(Long userId, Tag tagInstance) {
		if (tagIdCache[userId]) {
			tagIdCache[userId].add(tagInstance.id)
		}
	}

	static void removeFromCache(Long userId, Tag tagInstance) {
		if (tagIdCache[userId]) {
			tagIdCache[userId].remove(tagInstance.id)
		}
	}

	static void addToCache(Long userId, GenericTagGroup tagInstance) {
		if (tagGroupIdCache[userId]) {
			tagGroupIdCache[userId].add(tagInstance.id)
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
		return User.withTransaction {
			if (tagGroupIdCache[userId]) {
				return GenericTagGroup.getAll(tagGroupIdCache[userId])
			}

			List<Long> usersTagGroupIds = TagGroup.getAllTagGroupsForUser(userId)*.id
			tagGroupIdCache[userId] = usersTagGroupIds as Set

			GenericTagGroup.getAll(usersTagGroupIds)
		}
	}

	List<GenericTagGroup> getTagGroups() {
		getTagGroups(this.id)
	}

	// Returns list of Public interest tags.
	@Deprecated
	List<Tag> getInterestTags() {
		return ProfileTag.getPublicInterestTags(this.id)*.tag
	}

	// Adds public interest tag.
	@Deprecated
	void addInterestTag(Tag tag) {
		ProfileTag.addPublicInterestTag(tag, this.id)
	}

	// Deletes public interest tag.
	@Deprecated
	void deleteInterestTag(Tag tag) {
		ProfileTag.deletePublicInterestTag(tag, this.id)
	}

	// Deletes public interest tag.
	@Deprecated
	void removeInterestTag(Tag tag) {
		deleteInterestTag(tag)
	}

	// Looks for public interest tag.
	@Deprecated
	boolean hasInterestTag(Tag tag) {
		return ProfileTag.hasPublicInterestTag(tag, this.id)
	}

	// Returns a list of public interest tags for JSON response.
	def fetchInterestTagsJSON() {
		def retVal = []

		ProfileTag.getPublicInterestTags(this.id).each { ProfileTag profileTag ->
			retVal.add([description: profileTag.tag.description])
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
			resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
			projections {
				property('name', 'name')
				property('description', 'description')
			}
			eq('userId', id)
			eq('visibility', Visibility.PUBLIC)
		}
		return sprintsOwned
	}

	List getUserGroups() {
		List<Long> groupIds = new DetachedCriteria(GroupMemberReader).build {
			projections { property "groupId" }
			eq "memberId", this.id
		}.list()

		List groups = UserGroup.withCriteria {
			resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
			projections {
				property('fullName', 'name')
				property('description', 'description')
			}
			'in'('id', groupIds ?: [0l])
			or {
				isNull("isVirtual")
				ne("isVirtual", true)
			}
		}
		return groups
	}

	def getJSONShortDesc() {
		return [
			id: id,
			virtual: virtual,
			avatarURL: getAvatarURL(),
			username: username
		];
	}

	def getJSONDesc() {
		return getJSONShortDesc() + [
			hash: hash,
			sex: sex,
			website: website,
			created: created,
			type: "usr",
			closedExplanationCardCuriousities: settings.hasClosedCuriositiesExplanation(),
			closedExplanationCardTrackathon: settings.hasClosedTrackathonExplanation(),
			hasVisitedTrackathon: settings.hasVisitedTrackathon()
		];
	}

	def getPublicJSONDesc() {
		return getJSONDesc() + [
			username: username,
			sprints: getOwnedSprints(),
			groups: getUserGroups(),
			interestTags: fetchInterestTagsJSON()*.description,
			bio: settings.isBioPublic() ? bio : null,
			name: settings.isNamePublic() ? name : null,
			created: created,
			updated: created,
		]
	}

	def getPeopleJSONDesc() {
		return getJSONDesc() + [
			name: name,
			username: username,
			email: email,
			remindEmail: remindEmail,
			sprints: getOwnedSprints(),
			groups: getUserGroups(),
			interestTags: fetchInterestTagsJSON()*.description,
			bio: bio,
			isBioPublic: settings.isBioPublic(),
			isNamePublic: settings.isNamePublic(),
			notifyOnComments: notifyOnComments,
			updated: created,
			linkedToOura: getAccessTokenForThirdParty(ThirdParty["OURA"]),
			linkedToFitbit: getAccessTokenForThirdParty(ThirdParty["FITBIT"]),
			linkedToWithings: getAccessTokenForThirdParty(ThirdParty["WITHINGS"]),
			linkedToMoves: getAccessTokenForThirdParty(ThirdParty["MOVES"]),
			linkedToJawbone: getAccessTokenForThirdParty(ThirdParty["JAWBONE"]),
			linkedToTwenty3andMe: getAccessTokenForThirdParty(ThirdParty["TWENTY_THREE_AND_ME"]),
			notifyOnComments: notifyOnComments,
			birthdate: birthdate,
			created: created,
		]
	}

	private String getAccessTokenForThirdParty(ThirdParty partyType) {
		return OAuthAccount.findByTypeIdAndUserId(partyType, id)?.accessToken
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

	static List getAdminGroupIds(Long userId) {
		if (userId == null) { return null }

		return User.executeQuery(
						"""SELECT item.groupId as groupId
             FROM
                UserGroup userGroup,
                GroupMemberAdmin item
             WHERE
                item.memberId = :id AND
                item.groupId = userGroup.id""",
						[id: userId])
	}

/* Deprecated - this method could result in large result set over time, and also doesn't represent editable
 * logic correctly a la UserGroup.canAdminDiscussion()

 	static List getAdminDiscussionIds(Long userId) {
		if (userId == null) { return null }

		return User.executeQuery(
						"""SELECT dis.memberId as discussionId
             FROM
                UserGroup userGroup,
                GroupMemberAdmin item,
				GroupMemberDiscussion dis
             WHERE
                item.memberId = :id AND
                item.groupId = userGroup.id AND
				dis.groupId = item.groupId""",
						[id: userId])
	}*/

	List getAdminGroupIds() {
		return getAdminGroupIds(id)
	}

	String getPublicName() {
		return settings.isNamePublic() ? name : ""
	}

	String getPublicBio() {
		return settings.isBioPublic() ? bio : ""
	}

	// Returns a space separated string of all public interest tags.
	@Deprecated
	String getInterestTagsString() {
		return getInterestTags()*.description.join(" ")
	}

	String getPublicInterestTagsString() {
		return ProfileTag.getPublicInterestTags(this.id)*.tag.description.join(" ")
	}

	def validateUserPreferences(Map map, User user) {
		def status

		if (map.twitterDefaultToNow != 'on')
			map.twitterDefaultToNow = 'off'

		if (map.password != null && map.password.length() > 0) {
			if (!user.checkPassword(map.oldPassword)) {
				return [status: false, message: "Error updating user preferences: old password does not match"]
			}
		}

		user.update(map)

		Utils.save(user, true)

		if (!user.validate()) {
			return [status: false, message: "Error updating user preferences: missing field or email already in use"]
		} else {
			return [status: true, message: "User preferences updated", hash: user.hash]
		}
	}
}

enum VerificationStatus {
	UNVERIFIED(1),
	VERIFIED(2),
	BANNED(3)

	final int id
	VerificationStatus(int id) {
		this.id = id
	}

	String toString() {
		return "${this.name()}(${this.id})"
	}
}

package us.wearecurio.model.integration

import static org.junit.Assert.*

import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.Tag
import us.wearecurio.model.TagExclusion
import us.wearecurio.model.TagGroup
import us.wearecurio.model.ExclusionType
import us.wearecurio.model.SharedTagGroup
import us.wearecurio.model.User;
import us.wearecurio.model.UserGroup;
import us.wearecurio.services.EmailService;
import us.wearecurio.utility.Utils

class TagGroupTests extends CuriousTestCase {
	static transactional = true

	Tag tag1, tag2, tag3, tag4, tag5, tag6

	EmailService emailService

	User user2, admin, admin2, anon, schmoe
	UserGroup curious, announce, userGroup1, systemGroup

	TagGroup tagGroup1, tagGroup2
	TagGroup tagGroupA, tagGroupB, tagGroupC, tagGroupD

	@Before
	void setUp() {
		TagGroup.executeUpdate("delete TagGroup g")
		User.executeUpdate("delete User u")
		
		super.setUp()
		
		tag1 = Tag.create("tag1")
		tag2 = Tag.create("tag2")
		tag3 = Tag.create("tag3")
		tag4 = Tag.create("tag4")
		tag5 = Tag.create("tag5")
		tag6 = Tag.create("tag6")

		tagGroup1 = TagGroup.createOrLookupTagGroup("taggroup1", userId, null)
		tagGroup2 = TagGroup.createOrLookupTagGroup("taggroup2", userId, null)

		tagGroup1.addToTags(tag1)
		tagGroup1.addToTags(tag3)
		tagGroup1.addToTags(tag4)

		tagGroup2.addToTags(tag2)
		tagGroup2.addToTags(tag5)
		tagGroup2.addToTags(tag6)

		Utils.save(tagGroup2, true)

		tagGroup1.addToSubTagGroups(tagGroup2)
		Utils.save(tagGroup1, true)
		
		user2 = createUser('user2', 'user2@user.com')
		anon = createUser('anon', 'anon@user.com')
		admin = createUser('admin', 'admin@user.com')
		admin2 = createUser('admin2', 'admin2@user.com')
		schmoe = createUser('schmoe', 'schmoe@user.com')

		curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
				[isReadOnly: false, defaultNotify: false])

		curious.addAdmin(admin)
		curious.addAdmin(admin2)

		announce = UserGroup.create("announce", "Curious Announcements", "Announcements for Curious users",
				[isReadOnly: true, defaultNotify: false])

		announce.addAdmin(admin2)

		userGroup1 = UserGroup.create("UserGroup1", "UserGroup1", "", [isReadOnly: false, defaultNotify: false])

		tagGroupA = TagGroup.createOrLookupTagGroup("Tag Group A", user2.id, null)
		tagGroupB = TagGroup.createOrLookupTagGroup("Tag Group B", user2.id, null)
		tagGroupC = TagGroup.createOrLookupTagGroup("Tag Group C", null, curious.id)
		tagGroupD = TagGroup.createOrLookupTagGroup("Tag Group D", null, announce.id, SharedTagGroup.class)

		systemGroup = UserGroup.lookupOrCreateSystemGroup()
		EmailService.set(emailService)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "test get tags"() {
		List associatedTags = TagGroup.findByDescription("taggroup1").getTags(userId)

		assert associatedTags.size() == 6
	}

	@Test
	void "test get tags with null user id"() {
		List associatedTags = TagGroup.findByDescription("taggroup1").getTags(null)

		assert associatedTags.size() == 6
	}

	@Test
	void "test get tags with tag excluded"() {
		TagExclusion.addToExclusion(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.addToExclusion(tag5, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup2))
		TagExclusion.addToExclusion(tag6, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup2))

		List<Tag> associatedTags = TagGroup.findByDescription("taggroup1").getTags(userId)

		assert associatedTags.size() == 3
		assert associatedTags*.description.sort() == [tag1.description, tag2.description, tag4.description]
	}

	@Test
	void "test get tags with tag & tag group excluded"() {
		TagExclusion.addToExclusion(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.addToExclusion(tagGroup2, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))

		// Will not return any tags from tagGroup2 since it is excluded
		List<Tag> associatedTags = TagGroup.findByDescription("taggroup1").getTags(userId)

		assert associatedTags.size() == 2
		assert associatedTags*.description.sort() == [tag1.description, tag4.description]
	}

	@Test
	void "test get tags with exclusion & no user id"() {
		TagExclusion.addToExclusion(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.addToExclusion(tagGroup2, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))

		// Will not return any tags from tagGroup2 since it is excluded
		List<Tag> associatedTags = TagGroup.findByDescription("taggroup1").getTags(null)

		assert associatedTags.size() == 6
	}

	@Test
	void "test contains tag"() {
		TagGroup tagGroupInstance = TagGroup.findByDescription("taggroup1")
		// Check for top level sub tag
		assert tagGroupInstance.containsTag(Tag.look("tag1"), userId) == true
		// Check for nested sub tag
		assert tagGroupInstance.containsTag(Tag.look("tag5"), userId) == true
		assert tagGroupInstance.containsTag(Tag.look("other tag"), userId) == false
	}

	@Test
	void "test contains tag after exclusion"() {
		TagGroup tagGroupInstance = TagGroup.findByDescription("taggroup1")

		TagExclusion.addToExclusion(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.addToExclusion(tagGroup2, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))

		// Test after exclusion
		assert tagGroupInstance.containsTag(Tag.look("tag3"), userId) == false
		assert tagGroupInstance.containsTag(Tag.look("tag5"), userId) == false
	}

	@Test
	void "test contains tag string"() {
		TagGroup tagGroupInstance = TagGroup.findByDescription("taggroup1")
		// Check for top level sub tag string
		assert tagGroupInstance.containsTagString("tag1", userId) == true
		// Check for nested sub tag string
		assert tagGroupInstance.containsTagString("tag5", userId) == true
		assert tagGroupInstance.containsTagString("other tag", userId) == false
		// Check for blank string
		assert tagGroupInstance.containsTagString("", userId) == false
		// Test for null tag string
		assert tagGroupInstance.containsTagString(null, userId) == false
	}

	private static User createUser(name, email) {
		Map params = [username: name, sex: "F", name: "y y", email: email, birthdate: "01/01/2001", password: "y"]

		User user = User.create(params)

		Utils.save(user, true)

		return user
	}

	@Test
	void "get All TagGroups The User Is An Admin Of with no admin"() {
		// user does not have any admin user group
		List tagGroupList = TagGroup.getTagGroupsTheUserIsAnAdminOf(user2.id)

		assert tagGroupList.size() == 0
	}

	@Test
	void "test get All TagGroups The User Is An Admin Of"() {
		List tagGroupList = TagGroup.getTagGroupsTheUserIsAnAdminOf(admin.id)

		assert tagGroupList.size() == 1
		assert tagGroupList[0].id == tagGroupC.id
		assert tagGroupList[0].description == tagGroupC.description
	}

	@Test
	void "test get All TagGroups owned by given user groups with no owning tag group"() {
		// userGroup1 doesn't have any owned tag group.
		List tagGroupList = TagGroup.getTagGroupsForUserGroups([userGroup1])

		assert tagGroupList.size() == 0
	}

	@Test
	void "test get All TagGroups owned by given user groups"() {
		// userGroup1 doesn't have any owned tag group.
		List tagGroupList = TagGroup.getTagGroupsForUserGroups([curious, announce])

		assert tagGroupList.size() == 2
		def tagGroupIds = [ tagGroupC.id, tagGroupD.id ]
		assert tagGroupList[0].id.longValue() in tagGroupIds
		assert tagGroupList[1].id.longValue() in tagGroupIds
		assert tagGroupList[0].id != tagGroupList[1].id
		def tagGroupDescriptions = [ tagGroupC.description, tagGroupD.description ] 
		assert tagGroupList[0].description in tagGroupDescriptions
		assert tagGroupList[1].description in tagGroupDescriptions
		assert tagGroupList[0].description != tagGroupList[1].description
	}

	@Test
	void "test get all TagGroups owned by the user with no owning tag groups"() {
		// user is not owner of any tag groups
		List tagGroupList = TagGroup.getTagGroupsByUser(anon.id)

		assert tagGroupList.size() == 0

		// user is admin of a UserGroup but not directly own any tag group
		tagGroupList = TagGroup.getTagGroupsByUser(admin.id)
	}

	@Test
	void "test get all TagGroups owned by the user"() {
		List tagGroupList = TagGroup.getTagGroupsByUser(user2.id)

		assert tagGroupList.size() == 2
		def tagGroupIds = [ tagGroupA.id, tagGroupB.id ]
		assert tagGroupList[0].id.longValue() in tagGroupIds
		assert tagGroupList[1].id.longValue() in tagGroupIds
		assert tagGroupList[0].id != tagGroupList[1].id
	}

	@Test
	void "test can edit for readonly tag group"() {
		boolean canEdit = tagGroupD.hasEditor(admin.id)

		assert canEdit == false
	}

	@Test
	void "test can edit for readonly tag group for admin"() {
		boolean canEdit = tagGroupD.hasEditor(admin2.id)

		assert canEdit == true
	}

	@Test
	void "test can edit method for non SharedTagGroup"() {
		shouldFail(MissingMethodException) {
			tagGroupA.hasEditor(admin2.id)
		}
	}

	@Test
	void "test get system tag groups"() {
		TagGroup systemTagGroup1 = TagGroup.createOrLookupTagGroup("System Tag Group 1", null, systemGroup.id)
		TagGroup systemTagGroup2 = TagGroup.createOrLookupTagGroup("System Tag Group 2", null, systemGroup.id)

		List systemTagGroups = TagGroup.getSystemTagGroups()

		assert systemTagGroups.size() == 2
		def tagGroupIds = [ systemTagGroup1.id, systemTagGroup2.id ]
		assert systemTagGroups[0].id.longValue() in tagGroupIds
		assert systemTagGroups[1].id.longValue() in tagGroupIds
		assert systemTagGroups[0].id != systemTagGroups[1].id
	}

	@Test
	void "test the existence of generic property on same name tag groups"() {
		TagGroup systemTagGroup1 = TagGroup.createOrLookupTagGroup("Tag Group 1", null, systemGroup.id)
		TagGroup systemTagGroup2 = TagGroup.createOrLookupTagGroup("Tag Group 3", null, systemGroup.id)

		List allTagGroups = TagGroup.getAllTagGroupsForUser(schmoe.id)
		assert allTagGroups.size() == 2
		assert allTagGroups.find { it.description == "Tag Group 3" }.isSystemGroup == true
	}

	@Test
	void "test exclusion list data"() {
		Tag tag = Tag.look("Tag Test 1")
		TagGroup systemTagGroup1 = TagGroup.createOrLookupTagGroup("System Tag Group 1", null, systemGroup.id)

		def prop = GenericTagGroupProperties.createOrLookup(user2.id, "User", systemTagGroup1.id)
		TagExclusion.addToExclusion(tagGroupA, prop)
		TagExclusion.addToExclusion(tag, prop)

		assert TagExclusion.count() == 2
		List allTagGroups = TagGroup.getAllTagGroupsForUser(user2.id)
		assert allTagGroups.size() == 3

		Map tagGroupData = allTagGroups.find { it.description == systemTagGroup1.description }
		assert tagGroupData.id == systemTagGroup1.id
		assert tagGroupData["excludes"].size() == 2
		assert tagGroupData["excludes"].find { it.type == ExclusionType.TAG.toString() }.objectId == tag.id
		assert tagGroupData["excludes"].find { it.type == ExclusionType.TAG_GROUP.toString() }.objectId == tagGroupA.id
	}
}
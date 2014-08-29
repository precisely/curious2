package us.wearecurio.services.integration

import grails.plugin.mail.MailService

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.model.ExclusionType
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.SharedTagGroup
import us.wearecurio.model.Tag
import us.wearecurio.model.TagExclusion
import us.wearecurio.model.TagGroup
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.services.TagGroupService
import us.wearecurio.services.TagService
import us.wearecurio.utility.Utils

class TagServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	MailService mailService
	TagGroupService tagGroupService
	TagService tagService

	User admin, admin2, anon, schmoe
	UserGroup curious, announce, userGroup1, systemGroup

	def tagGroup1, tagGroup2, tagGroup3, tagGroup4

	@Before
	@Override
	void setUp() {
		super.setUp()
		user = createUser('user', 'user@user.com')
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

		tagGroup1 = tagGroupService.createOrLookupTagGroup("Tag Group 1", user.id, null)
		tagGroup2 = tagGroupService.createOrLookupTagGroup("Tag Group 2", user.id, null)
		tagGroup3 = tagGroupService.createOrLookupTagGroup("Tag Group 3", null, curious.id)
		tagGroup4 = tagGroupService.createOrLookupTagGroup("Tag Group 4", null, announce.id, SharedTagGroup.class)

		systemGroup = UserGroup.lookupOrCreateSystemGroup()
		Utils.setMailService(mailService)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	private static User createUser(name, email) {
		Map params = [username: name, sex: "F", last: "y", email: email, birthdate: "01/01/2001", first: "y", password: "y"]

		User user = User.create(params)

		Utils.save(user, true)

		return user
	}

	@Test
	void "get All TagGroups The User Is An Admin Of with no admin"() {
		// user does not have any admin user group
		List tagGroupList = tagService.getTagGroupsTheUserIsAnAdminOf(user.id)

		assert tagGroupList.size() == 0
	}

	void "test get All TagGroups The User Is An Admin Of"() {
		List tagGroupList = tagService.getTagGroupsTheUserIsAnAdminOf(admin.id)

		assert tagGroupList.size() == 1
		assert tagGroupList[0].id == tagGroup3.id
		assert tagGroupList[0].description == tagGroup3.description
	}

	void "test get All TagGroups owned by given user groups with no owning tag group"() {
		// userGroup1 doesn't have any owned tag group.
		List tagGroupList = tagService.getTagGroupsForUserGroups([userGroup1])

		assert tagGroupList.size() == 0
	}

	void "test get All TagGroups owned by given user groups"() {
		// userGroup1 doesn't have any owned tag group.
		List tagGroupList = tagService.getTagGroupsForUserGroups([curious, announce])

		assert tagGroupList.size() == 2
		assert tagGroupList[0].id == tagGroup3.id
		assert tagGroupList[0].description == tagGroup3.description
		assert tagGroupList[1].id == tagGroup4.id
		assert tagGroupList[1].description == tagGroup4.description
	}

	void "test get all TagGroups owned by the user with no owning tag groups"() {
		// user is not owner of any tag groups
		List tagGroupList = tagService.getTagGroupsByUser(anon.id)

		assert tagGroupList.size() == 0

		// user is admin of a UserGroup but not directly own any tag group
		tagGroupList = tagService.getTagGroupsByUser(admin.id)
	}

	void "test get all TagGroups owned by the user"() {
		List tagGroupList = tagService.getTagGroupsByUser(user.id)

		assert tagGroupList.size() == 2
		assert tagGroupList[0].id == tagGroup1.id
		assert tagGroupList[1].id == tagGroup2.id
	}

	void "test can edit for readonly tag group"() {
		boolean canEdit = tagService.canEdit(tagGroup4, admin.id)

		assert canEdit == false
	}

	void "test can edit for readonly tag group for admin"() {
		boolean canEdit = tagService.canEdit(tagGroup4, admin2.id)

		assert canEdit == true
	}

	void "test can edit method for non SharedTagGroup"() {
		shouldFail(MissingMethodException) {
			tagService.canEdit(tagGroup1, admin2.id)
		}
	}

	void "test get system tag groups"() {
		TagGroup systemTagGroup1 = tagGroupService.createOrLookupTagGroup("System Tag Group 1", null, systemGroup.id)
		TagGroup systemTagGroup2 = tagGroupService.createOrLookupTagGroup("System Tag Group 2", null, systemGroup.id)

		List systemTagGroups = tagService.getSystemTagGroups()

		assert systemTagGroups.size() == 2
		assert systemTagGroups[0].id == systemTagGroup1.id
		assert systemTagGroups[1].id == systemTagGroup2.id
	}

	void "test the existence of generic property on same name tag groups"() {
		TagGroup systemTagGroup1 = tagGroupService.createOrLookupTagGroup("Tag Group 1", null, systemGroup.id)
		TagGroup systemTagGroup2 = tagGroupService.createOrLookupTagGroup("Tag Group 3", null, systemGroup.id)

		List allTagGroups = tagService.getAllTagGroupsForUser(schmoe.id)
		assert allTagGroups.size() == 2
		assert allTagGroups.find { it.description == "Tag Group 3" }.isSystemGroup == true
	}

	void "test exclusion list data"() {
		Tag tag = Tag.look("Tag Test 1")
		TagGroup systemTagGroup1 = tagGroupService.createOrLookupTagGroup("System Tag Group 1", null, systemGroup.id)

		def prop = GenericTagGroupProperties.createOrLookup(user.id, "User", systemTagGroup1.id)
		TagExclusion.createOrLookup(tagGroup1, prop)
		TagExclusion.createOrLookup(tag, prop)

		assert TagExclusion.count() == 2
		List allTagGroups = tagService.getAllTagGroupsForUser(user.id)
		assert allTagGroups.size() == 3

		Map tagGroupData = allTagGroups.find { it.description == systemTagGroup1.description }
		assert tagGroupData.id == systemTagGroup1.id
		assert tagGroupData["excludes"].size() == 2
		assert tagGroupData["excludes"].find { it.type == ExclusionType.TAG.toString() }.objectId == tag.id
		assert tagGroupData["excludes"].find { it.type == ExclusionType.TAG_GROUP.toString() }.objectId == tagGroup1.id
	}
}
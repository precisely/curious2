package us.wearecurio.services.integration

import grails.plugin.mail.MailService

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.services.TagGroupService
import us.wearecurio.services.TagService
import us.wearecurio.utility.Utils

class TagServiceTests extends CuriousServiceTestCase {

	MailService mailService
	TagGroupService tagGroupService
	TagService tagService

	User admin, admin2, anon, schmoe
	UserGroup curious, announce, userGroup1

	def tagGroup1, tagGroup2, tagGroup3, tagGroup4

	@Before
	@Override
	void setUp() {
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
				[isReadOnly: false, defaultNotify: false])

		userGroup1 = UserGroup.create("UserGroup1", "UserGroup1", "", [isReadOnly: false, defaultNotify: false])

		tagGroup1 = tagGroupService.createOrLookupTagGroup("Tag Group 1", user.id, null)
		tagGroup2 = tagGroupService.createOrLookupTagGroup("Tag Group 2", user.id, null)
		tagGroup3 = tagGroupService.createOrLookupTagGroup("Tag Group 3", null, curious.id)
		tagGroup4 = tagGroupService.createOrLookupTagGroup("Tag Group 4", null, announce.id)

		Utils.setMailService(mailService)
	}

	@After
	void tearDown() {
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
}
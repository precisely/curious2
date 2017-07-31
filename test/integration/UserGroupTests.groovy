import static org.junit.Assert.*

import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.*
import us.wearecurio.services.EmailService
import us.wearecurio.utility.Utils

class UserGroupTests extends CuriousTestCase {

	static transactional = true

	EmailService emailService

	User user
	User admin
	User admin2
	User anon
	User schmoe
	User bloo
	UserGroup precisely
	UserGroup precisely2
	UserGroup announce

	static def createUser(name, email) {
		Map params = [username: name, sex: 'F', last: 'y', email: email, birthdate: '01/01/2001', first: 'y', password: 'y']

		def user = User.create(params)

		Utils.save(user, true)
		println "new user " + user

		return user
	}

	@Before
	void setUp() {
		super.setUp()

		user = createUser('user', 'user@user.com')
		admin = createUser('admin', 'admin@user.com')
		admin2 = createUser('admin2', 'admin2@user.com')
		anon = createUser('anon', 'anon@user.com')
		schmoe = createUser('schmoe', 'schmoe@user.com')
		bloo = createUser('bloo', 'bloo@user.com')

		precisely = UserGroup.create("precise.ly", "precise.ly Discussions", "Discussion topics for precise.ly users",
				[isReadOnly:false, defaultNotify:false])
		precisely2 = UserGroup.create("precise.ly2", "precise.ly Discussions 2", "Discussion topics for precise.ly users 2",
				[isReadOnly:false, defaultNotify:false])
		announce = UserGroup.create("announce", "precise.ly Announcements", "Announcements for precise.ly users",
				[isReadOnly:true, defaultNotify:true])
		EmailService.set(emailService)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	static def discussionsContains(discussionsInfo, discussionId) {
		for (info in discussionsInfo) {
			if (info['id'] == discussionId) {
				return true
			}
		}

		return false
	}

	@Test
	void testUserGroups() {
		announce.addMember(user)

		assert announce.hasReader(user)
		assert !announce.hasWriter(user)
		assert !announce.hasNotified(user)
		assert !announce.hasAdmin(user)
		assert UserGroup.getDefaultGroupForUser(user) == precisely

		announce.addDefaultFor(user)

		assert announce.hasReader(user)
		assert !announce.hasWriter(user)
		assert !announce.hasNotified(user)
		assert !announce.hasAdmin(user)
		assert UserGroup.getDefaultGroupForUser(user) == announce

		announce.addMember(anon)

		assert announce.hasReader(anon)
		assert !announce.hasWriter(anon)
		assert !announce.hasNotified(anon)
		assert !announce.hasAdmin(anon)

		precisely.addAdmin(admin)
		precisely.addDefaultFor(admin)

		assert precisely.hasReader(admin)
		assert precisely.hasWriter(admin)
		assert !precisely.hasNotified(admin)
		assert precisely.hasAdmin(admin)
		assert UserGroup.getDefaultGroupForUser(admin) == precisely
		
		announce.addAdmin(admin)

		assert announce.hasReader(admin)
		assert announce.hasWriter(admin)
		assert announce.hasNotified(admin)
		assert announce.hasAdmin(admin)
		assert UserGroup.getDefaultGroupForUser(admin) == precisely

		announce.addAdmin(admin2)

		precisely.addMember(admin2)
		precisely.addDefaultFor(admin2)

		assert announce.hasReader(admin2)
		assert announce.hasWriter(admin2)
		assert announce.hasNotified(admin2)
		assert announce.hasAdmin(admin2)
		assert UserGroup.getDefaultGroupForUser(admin2) == precisely

		Discussion discussion = Discussion.create(user, "Discussion name")

		precisely.addDiscussion(discussion)

		discussion.createPost(user, null, "comment")

		assert precisely.hasDiscussion(discussion)

		assert UserGroup.canReadDiscussion(user, discussion)
		assert UserGroup.canReadDiscussion(admin, discussion)
		assert !UserGroup.canReadDiscussion(schmoe, discussion)
		assert UserGroup.canWriteDiscussion(user, discussion)
		assert UserGroup.canWriteDiscussion(admin, discussion)
		assert !UserGroup.canWriteDiscussion(schmoe, discussion)
		assert UserGroup.canAdminDiscussion(user, discussion)
		assert !UserGroup.canAdminDiscussion(anon, discussion)
		assert !UserGroup.canAdminDiscussion(schmoe, discussion)
		assert UserGroup.canAdminDiscussion(admin, discussion)

		Discussion announcement = Discussion.create(admin, "Announcement name")

		announce.addDiscussion(announcement)

		announcement.createPost(admin, null, "comment")

		assert announce.hasDiscussion(announcement)

		assert UserGroup.canReadDiscussion(user, announcement)
		assert UserGroup.canReadDiscussion(admin, announcement)
		assert !UserGroup.canReadDiscussion(schmoe, announcement)
		assert UserGroup.canReadDiscussion(anon, announcement)
		assert !UserGroup.canWriteDiscussion(user, announcement)
		assert UserGroup.canWriteDiscussion(admin, announcement)
		assert !UserGroup.canWriteDiscussion(schmoe, announcement)
		assert !UserGroup.canWriteDiscussion(anon, announcement)
		assert !UserGroup.canAdminDiscussion(user, announcement)
		assert !UserGroup.canAdminDiscussion(anon, announcement)
		assert !UserGroup.canAdminDiscussion(schmoe, announcement)
		assert UserGroup.canAdminDiscussion(admin, announcement)

		announce.removeMember(anon)

		assert !UserGroup.canReadDiscussion(anon, announcement)
		
		precisely.addMember(bloo)
		Thread.sleep(1000)
		precisely2.addMember(bloo)
		
		def userGroups = UserGroup.getConcreteGroupsForWriter(bloo)
		assert userGroups[1].id == precisely.id
		assert userGroups[0].id == precisely2.id
		
		Thread.sleep(1000)
		precisely.updateWriter(bloo)
		
		userGroups = UserGroup.getConcreteGroupsForWriter(bloo)
		assert userGroups[0].id == precisely.id
		assert userGroups[1].id == precisely2.id
	}

/*	@Test
	void testDefaultUserGroup() {

		//Default UserGroup for a User if no default group present is curious
		assert UserGroup.getDefaultGroupForUser(user) == curious
		assert curious.hasReader(user)
		assert curious.hasWriter(user)
		assert !curious.hasNotified(user)
		assert !curious.hasAdmin(user)
	}

	@Test
	void testVirtualUserGroup() {
		UserGroup group = UserGroup.createVirtual("Test full name")
		
		assert group.fullName == "Test full name"
		assert group.isVirtual
	}
/**/
}
import static org.junit.Assert.*
import grails.plugin.mail.MailService

import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.*
import us.wearecurio.utility.Utils

class UserGroupTests extends CuriousTestCase {

	static transactional = true

	MailService mailService

	User user
	User admin
	User admin2
	User anon
	User schmoe
	UserGroup curious
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

		curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
				[isReadOnly:false, defaultNotify:false])
		announce = UserGroup.create("announce", "Curious Announcements", "Announcements for Curious users",
				[isReadOnly:true, defaultNotify:true])
		Utils.setMailService(mailService)
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
	void testDefaultUserGroup() {

		//Default UserGroup for a User if no default group present is curious
		assert UserGroup.getDefaultGroupForUser(user) == curious
		assert curious.hasReader(user)
		assert curious.hasWriter(user)
		assert !curious.hasNotified(user)
		assert !curious.hasAdmin(user)
	}

	@Test
	void testUserGroups() {
		announce.addMember(user)

		assert announce.hasReader(user)
		assert !announce.hasWriter(user)
		assert !announce.hasNotified(user)
		assert !announce.hasAdmin(user)
		assert UserGroup.getDefaultGroupForUser(user) == curious

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

		curious.addAdmin(admin)
		curious.addDefaultFor(admin)

		assert curious.hasReader(admin)
		assert curious.hasWriter(admin)
		assert !curious.hasNotified(admin)
		assert curious.hasAdmin(admin)
		assert UserGroup.getDefaultGroupForUser(admin) == curious

		announce.addAdmin(admin)

		assert announce.hasReader(admin)
		assert announce.hasWriter(admin)
		assert announce.hasNotified(admin)
		assert announce.hasAdmin(admin)
		assert UserGroup.getDefaultGroupForUser(admin) == curious

		announce.addAdmin(admin2)

		curious.addMember(admin2)
		curious.addDefaultFor(admin2)

		assert announce.hasReader(admin2)
		assert announce.hasWriter(admin2)
		assert announce.hasNotified(admin2)
		assert announce.hasAdmin(admin2)
		assert UserGroup.getDefaultGroupForUser(admin2) == curious

		Discussion discussion = Discussion.create(user, "Discussion name")

		curious.addDiscussion(discussion)

		discussion.createPost(DiscussionAuthor.create(user), null, "comment")

		assert curious.hasDiscussion(discussion)

		assert discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(user, ['curious']).dataList, discussion.getId())
		assert discussionsContains(UserGroup.getDiscussionsInfoForUser(user, false).dataList, discussion.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForUser(schmoe, true).dataList, discussion.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(schmoe, ['curious']).dataList, discussion.getId())

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

		announcement.createPost(DiscussionAuthor.create(admin), null, "comment")

		assert announce.hasDiscussion(announcement)

		assert discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(user, ['announce']).dataList, announcement.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(user, ['curious']).dataList, announcement.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(schmoe, ['announce']).dataList, announcement.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForUser(schmoe, true).dataList, announcement.getId())
		assert discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(anon, ['announce']).dataList, announcement.getId())
		assert discussionsContains(UserGroup.getDiscussionsInfoForUser(anon, true).dataList, announcement.getId())

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
		assert !discussionsContains(UserGroup.getDiscussionsInfoForUser(anon, true).dataList, announcement.getId())
	}

	void testDiscussPagination() {
		curious.addMember(user)

		Discussion discussion1 = Discussion.create(user, "Discussion 1")
		Discussion discussion2 = Discussion.create(user, "Discussion 2")
		Discussion discussion3 = Discussion.create(user, "Discussion 3")
		Discussion discussion4 = Discussion.create(user, "Discussion 4")
		Discussion discussion5 = Discussion.create(user, "Discussion 5")
		Discussion discussion6 = Discussion.create(user, "Discussion 6")

		curious.addDiscussion(discussion3)
		curious.addDiscussion(discussion4)

		Map paginationData = UserGroup.getDiscussionsInfoForUser(user, true, [max: 3, offset: 0])
		List discussions = paginationData.dataList

		assert paginationData.totalCount == 8	// two are duplicates
		assert discussions.size() == 3
		assert discussionsContains(discussions, discussion3.id)
		assert discussionsContains(discussions, discussion4.id)
		assert discussionsContains(discussions, discussion1.id)

		paginationData = UserGroup.getDiscussionsInfoForUser(user, true, [max: 3, offset: 3])
		discussions = paginationData.dataList

		assert paginationData.totalCount == 8	// two are duplicates
		assert discussions.size() == 3
		assert discussionsContains(discussions, discussion1.id)
		assert discussionsContains(discussions, discussion3.id)
		assert discussionsContains(discussions, discussion2.id)
	}
}
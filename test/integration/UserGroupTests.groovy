import grails.plugin.mail.MailService
import grails.test.*

import us.wearecurio.model.*
import us.wearecurio.utility.*

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class UserGroupTests extends GroovyTestCase {
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
		def params = [username:name, sex:'F', \
            last:'y', email:email, birthdate:'01/01/2001', \
            first:'y', password:'y']

		def user = User.create(params)

		Utils.save(user, true)
		println "new user " + user
		
		return user
	}
	
	@Before
	void setUp() {
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
		
		Discussion discussion

		discussion = Discussion.create(user, "Discussion name")
		
		Utils.save(discussion, true)
		
		curious.addDiscussion(discussion)
		
		discussion.createPost(DiscussionAuthor.create(user), null, "comment")
		
		assert curious.hasDiscussion(discussion)
		
		assert discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(user, ['curious']), discussion.getId())
		assert discussionsContains(UserGroup.getDiscussionsInfoForUser(user, false), discussion.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForUser(schmoe, true), discussion.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(schmoe, ['curious']), discussion.getId())
		
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
		
		Discussion announcement

		announcement = Discussion.create(admin, "Announcement name")
		
		Utils.save(announcement, true)
		
		announce.addDiscussion(announcement)
		
		announcement.createPost(DiscussionAuthor.create(admin), null, "comment")
		
		assert announce.hasDiscussion(announcement)
		
		assert discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(user, ['announce']), announcement.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(user, ['curious']), announcement.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(schmoe, ['announce']), announcement.getId())
		assert !discussionsContains(UserGroup.getDiscussionsInfoForUser(schmoe, true), announcement.getId())
		assert discussionsContains(UserGroup.getDiscussionsInfoForGroupNameList(anon, ['announce']), announcement.getId())
		assert discussionsContains(UserGroup.getDiscussionsInfoForUser(anon, true), announcement.getId())
		
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
		assert !discussionsContains(UserGroup.getDiscussionsInfoForUser(anon, true), announcement.getId())
	}
}

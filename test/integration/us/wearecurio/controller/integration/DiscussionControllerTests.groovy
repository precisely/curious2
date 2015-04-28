package us.wearecurio.controller.integration

import static org.junit.Assert.*
import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Model.Visibility
import us.wearecurio.controller.DiscussionController
import us.wearecurio.model.*
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils

class DiscussionControllerTests extends CuriousControllerTestCase {
	static transactional = true

	DiscussionController controller

	User user2
	UserGroup testGroup
	UserGroup readOnlyTestGroup
	def params = [:]
    @Before
    void setUp() {
        // Setup logic here
		super.setUp()

		testGroup = UserGroup.create("testgroup", "Test discussions", "Discussion topics for testing users",
				[isReadOnly:false, defaultNotify:false])
		readOnlyTestGroup = UserGroup.create("testReadOnlyGroup", "Test read only discussions", "Discussion topics for testing users",
				[isReadOnly:true, defaultNotify:false])
		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()
		testGroup.addWriter(user2)
		params.clear()
		params.putAll([
			group:'testReadOnlyGroup',
			name:'New Discussion',
			discussionPost:'Here is a comment for the discussion',
			action: 'create',
			controller:'discussion'
		])
    }

    @After
    void tearDown() {
		super.tearDown()
    }

    @Test
    void testCreateReadOnly() {
		controller = new DiscussionController()
		controller.params.putAll(params)
		controller.session.userId = user2.getId()
		controller.request.method = 'POST'
		def retVal = controller.create()
		assert Discussion.count() == 0 
    }

    @Test
    void testCreateWithGroupNameWithWritePermission() {
		//Adds a new discussion to the specified group
		controller = new DiscussionController()
		params.group = 'testgroup'
		controller.params.putAll(params)
		controller.session.userId = user2.getId()
		controller.request.method = 'POST'
		def retVal = controller.create()
		assert Discussion.count() == 1
    }

    @Test
    void testCreateWithGroupNameWithNoWritePermission() {
		//Trying to add a new discussion to the specified group
		// with no write permission
		def noWritePermissionGroup = UserGroup.create("nowrite", "Test discussions", "Discussion topics for testing users",
				[isReadOnly:false, defaultNotify:false])
		controller = new DiscussionController()
		params.group = 'nowrite'
		controller.params.putAll(params)
		controller.request.method = 'POST'
		controller.session.userId = user2.getId()
		def retVal = controller.create()
		assert Discussion.count() == 0
    }
    @Test
    void testCreateWithNoGroup() {
		//Adds a new discussion to the default group
		controller = new DiscussionController()
		params.remove('group') 
		controller.params.putAll(params)
		controller.session.userId = user2.getId()
		controller.request.method = 'POST'
		def retVal = controller.create()
		assert Discussion.count() == 1
    }

	@Test
	void "Test delete discussion when discussion is null"() {
		controller = new DiscussionController()

		controller.session.userId = user2.id
		controller.params.id = 0
		controller.request.method = 'DELETE'
		controller.delete()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("not.exist.message",
				["That discussion"] as Object[], null)
	}

	@Test
	void "Test deletediscussion when user does not have write permission"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)

		assert discussion

		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.id = discussion.id
		controller.request.method = 'DELETE'
		controller.delete()

		assert !controller.response.json.success
		assert controller.response.json.message == "You don't have the right to delete this discussion."
	}

	@Test
	void "Test deletediscussion when discussion is present"() {
		testGroup.addWriter(user)
		Discussion discussion = Discussion.create(user, "test Discussion", testGroup)

		assert discussion

		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.id = discussion.id
		controller.request.method = 'DELETE'
		controller.delete()

		assert controller.response.json.success
		assert controller.response.json.message == "Discussion deleted successfully."
		assert !Discussion.count
	}

	@Test
	void "Test addComment when discussion is null for ajax request"() {
		controller = new DiscussionController()

		controller.session.userId = user2.id
		controller.params.id = 0
		controller.params.message = "test message"
		controller.request.makeAjaxRequest()

		controller.addComment()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", 
			["Discussion"] as Object[], null) 
	}

	@Test
	void "Test addComment when discussion is null for non-ajax request"() {
		controller = new DiscussionController()

		controller.session.userId = user2.id
		controller.params.id = 0
		controller.params.message = "test message"

		controller.addComment()

		assert controller.flash.message == messageSource.getMessage("default.blank.message", 
			["Discussion"] as Object[], null)
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test addComment when user does not have write permission for that discussion"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion
		
		controller = new DiscussionController()

		controller.session.userId = user.id
		controller.params.id = discussion.id
		controller.params.message = "test message"

		controller.addComment()

		assert controller.flash.message == messageSource.getMessage("default.permission.denied", null, null)
		assert controller.response.redirectUrl.contains("show")
	}

	@Test
	void "Test addComment for non-ajax request"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion
		
		controller = new DiscussionController()

		controller.session.userId = user2.id
		controller.params.id = discussion.id
		controller.params.message = "test message"

		controller.addComment()

		assert controller.response.redirectUrl.contains("show")
		assert DiscussionPost.count() == 1
	}

	@Test
	void "Test addComment for ajax request"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion
		
		controller = new DiscussionController()

		controller.session.userId = user2.id
		controller.params.id = discussion.id
		controller.params.message = "test message"
		controller.request.makeAjaxRequest()

		controller.addComment()

		assert controller.response.json.success
		assert DiscussionPost.count() == 1
	}

	@Test
	void "Test show when discussion is null"() {
		controller = new DiscussionController()

		controller.session.userId = user2.id
		controller.params.id = 0

		controller.show()

		assert controller.flash.message == messageSource.getMessage("default.blank.message", ["Discussion"] as Object[], null)
		assert controller.response.redirectUrl.contains("index")
	}

	@Test
	void "Test show when user is not logged in an disccussion is not public"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		discussion.visibility = Visibility.PRIVATE
		Utils.save(discussion, true)
		assert discussion.visibility == Visibility.PRIVATE
		
		controller = new DiscussionController()
		controller.params.id = discussion.id

		controller.show()

		assert controller.flash.message == messageSource.getMessage("default.login.message", null, null)
		assert controller.response.redirectUrl.contains("index")
	}

	@Test
	void "Test show"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion
		
		controller = new DiscussionController()
		controller.params.id = discussion.id
		controller.session.userId = user2.id

		controller.show()

		def modelAndView = controller.modelAndView

		assert modelAndView.model['discussionId'].toString().equals(discussion.getId().toString())
		assert modelAndView.model['username'].equals(user2.getUsername())
		assert modelAndView.getViewName().equals("/home/discuss")
	}

	@Test
	void "Test deletePost when postId is different then discussion"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
 		Discussion discussion2 = Discussion.create(user2, "test Discussion2", testGroup)
		assert discussion
		
		DiscussionPost post = DiscussionPost.create(discussion, user.id, "Test comment") 
		Utils.save(post, true)

		assert DiscussionPost.count() == 1
		
		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.deletePostId = post.id
		controller.params.discussionId = discussion2.id

		controller.deletePost()


		assert controller.flash.message == messageSource.getMessage("default.not.deleted.message", ["Post"] as Object[], null)
		assert controller.response.redirectUrl.contains("show")
	}

	@Test
	void "Test deletePost when discussion is null"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion
		
		DiscussionPost post = DiscussionPost.create(discussion, user.id, "Test comment") 
		Utils.save(post, true)

		assert DiscussionPost.count() == 1
		
		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.deletePostId = post.id
		controller.params.discussionId = null

		controller.deletePost()

		assert controller.flash.message == messageSource.getMessage("default.blank.message", ["Discussion"] as Object[], null)
		assert controller.response.redirectUrl.contains("show")
	}

	@Test
	void "Test deletePost when user does not have permission to delete comment"() { 
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion
		
		DiscussionPost post = DiscussionPost.create(discussion, user2.id, "Test comment") 
		Utils.save(post, true)

		assert DiscussionPost.count() == 1
		
		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.deletePostId = post.id
		controller.params.discussionId = discussion.id

		controller.deletePost()

		assert controller.flash.message == messageSource.getMessage("default.permission.denied", null, null)
		assert controller.response.redirectUrl.contains("show")
	}

	@Test
	void "Test deletePost"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion
		
		DiscussionPost post = DiscussionPost.create(discussion, user.id, "Test comment") 
		Utils.save(post, true)

		assert DiscussionPost.count() == 1
		
		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.deletePostId = post.id
		controller.params.discussionId = discussion.id

		controller.deletePost()

		assert controller.response.redirectUrl.contains("show")
		assert !DiscussionPost.count()
	}

	@Test
	void "Test publish when user is not permitted"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		discussion.visibility = Visibility.PRIVATE
		Utils.save(discussion, true)
		assert !discussion.isPublic()

		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.id = discussion.id
		controller.publish()

		assert controller.flash.message == messageSource.getMessage("default.permission.denied", null, null)
		assert controller.response.redirectUrl.contains("show")
		assert !discussion.isPublic()
	}

	@Test
	void "Test publish when discussion is null"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion

		controller = new DiscussionController()
		controller.session.userId = user.id
		controller.params.id = null
		controller.publish()

		assert controller.flash.message == messageSource.getMessage("default.blank.message", ["Discussion"] as Object[], null)
		assert controller.response.redirectUrl.contains("index")
	}

	@Test
	void "Test publish"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		discussion.visibility = Visibility.PRIVATE
		Utils.save(discussion, true)
		assert !discussion.isPublic()

		controller = new DiscussionController()
		controller.session.userId = user2.id
		controller.params.id = discussion.id
		controller.publish()

		assert controller.flash.message == messageSource.getMessage("default.updated.message", ["Discussion"] as Object[], null)
		assert controller.response.redirectUrl.contains("show")
		assert discussion.isPublic()
	}
}

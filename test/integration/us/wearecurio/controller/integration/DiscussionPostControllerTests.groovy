package us.wearecurio.controller.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import us.wearecurio.controller.DiscussionPostController
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils

class DiscussionPostControllerTests extends CuriousControllerTestCase {
	static transactional = true

	DiscussionPostController controller

	User user2
	UserGroup testGroup
	UserGroup readOnlyTestGroup
	Discussion discussion
	def params = [:]
    @Before
    void setUp() {
        // Setup logic here
		super.setUp()

		controller = new DiscussionPostController()
		testGroup = UserGroup.create("testgroup", "Test discussions", "Discussion topics for testing users",
				[isReadOnly:false, defaultNotify:false])
		readOnlyTestGroup = UserGroup.create("testReadOnlyGroup", "Test read only discussions", "Discussion topics for testing users",
				[isReadOnly:true, defaultNotify:false])
		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		Utils.save(user2, true)
		testGroup.addWriter(user2)
		params.clear()
 		discussion = Discussion.create(user2, "test Discussion", testGroup)
    }

    @After
    void tearDown() {
		super.tearDown()
    }

	@Test
	void "Test delete when user does not have permission to delete comment"() { 
		DiscussionPost post = DiscussionPost.create(discussion, user2.id, "Test comment") 
		Utils.save(post, true)

		assert DiscussionPost.count() == 1
		
		controller.session.userId = user.id
		controller.params.id = post.id
		controller.request.method = "DELETE"

		controller.delete()

		assert controller.response.json.message == messageSource.getMessage("default.permission.denied", null, null)
		assert !controller.response.json.success
		assert DiscussionPost.count() == 1
	}

	@Test
	void "Test delete when post is null"() { 
		controller.session.userId = user.id
		controller.params.id = 0 
		controller.request.method = "DELETE"

		controller.delete()
	
		assert controller.response.json.message == messageSource.getMessage("not.found.message", ["Post"] as Object[], null)
		assert !controller.response.json.success
	}

	@Test
	void "Test delete a normal post"() {
		assert discussion
		
		DiscussionPost post = DiscussionPost.create(discussion, user.id, "Test comment")
		post.isFirstPost = false
		Utils.save(post, true)

		assert DiscussionPost.count() == 1
		
		controller.session.userId = user.id
		controller.params.id = post.id
		controller.request.method = "DELETE"

		controller.delete()

		assert controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.deleted.message", 
			["Discussion", "comment"] as Object[], null)
		assert !DiscussionPost.count()
	}

	@Test
	void "Test delete first post"() {
		assert discussion

		DiscussionPost post = DiscussionPost.create(discussion, user.id, "Test comment")
		assert post.isFirstPost() == true
		assert DiscussionPost.count() == 1

		controller.session.userId = user.id
		controller.params.id = post.id
		controller.request.method = "DELETE"

		controller.delete()

		assert controller.response.json.message == "Sorry, you can not delete the description of a discussion."
		assert !controller.response.json.success
		assert DiscussionPost.count() == 1
	}

	@Test
	void "Test save when discussion is null for ajax request"() {
		controller.session.userId = user2.id
		controller.params.discussionHash = "0"
		controller.params.message = "test message"
		controller.request.makeAjaxRequest()
		controller.request.method = "POST"

		controller.save()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("not.found.message", 
			["Discussion"] as Object[], null) 
		assert !DiscussionPost.count()
	}

	@Test
	void "Test save when user does not have write permission for that discussion"() {
		assert discussion

		controller.session.userId = user.id
		controller.params.discussionHash = discussion.hash
		controller.params.message = "test message"
		controller.request.method = "POST"

		controller.save()

		assert controller.response.json.message == messageSource.getMessage("default.permission.denied", null, null)
		assert !controller.response.json.success
		assert !DiscussionPost.count()
	}

	@Test
	void "Test save for ajax request"() {
		assert discussion
		DiscussionPost.createFirstPost("", user, discussion, null)
		assert DiscussionPost.count() == 1

		controller.session.userId = user2.id
		controller.params.discussionHash = discussion.hash
		controller.params.message = "test message"
		controller.request.makeAjaxRequest()
		controller.request.method = "POST"

		controller.save()

		assert controller.response.json.success
		assert DiscussionPost.count() == 2
		DiscussionPost comment = DiscussionPost.last()
		comment.isFirstPost() == false
		comment.message == "test message"
		comment.userId == user2.id
	}

	@Test
	void "test comment where there is no first post of a discussion"() {
		assert discussion
		// User2 created the discussion
		assert discussion.userId == user2.id

		assert Discussion.count() == 1
		assert DiscussionPost.count() == 0

		testGroup.addWriter(user)

		// But user 1 is adding a comment
		controller.session.userId = user.id
		controller.params.discussionHash = discussion.hash
		controller.params.message = "test message"
		controller.request.method = "POST"

		controller.save()

		assert controller.response.json.success
		// The comment should be created and also the first post should also be created
		assert DiscussionPost.count() == 2
		assert discussion.firstPostId != null
		DiscussionPost firstPost = discussion.getFirstPost()
		assert firstPost.message == ""
		// Important: The empty first being created should have user id of original author not the one who is commenting
		assert firstPost.userId == user2.id
		assert firstPost.isFirstPost() == true

		// Comment should be created
		DiscussionPost comment = DiscussionPost.list()[1]
		assert comment.message == "test message"
		assert comment.discussionId == discussion.id
		assert comment.userId == user.id
		assert comment.isFirstPost() == false

		// Added comment should not be added as the first post
		assert discussion.firstPostId != comment.id
	}

	@Test
	void "Test index when discussion is null"() {
		assert discussion
		
		DiscussionPost post = DiscussionPost.create(discussion, user.id, "Test comment") 
		Utils.save(post, true)

		controller.session.userId = user.id
		controller.params.discussionHash = null

		controller.index()

		assert !controller.response.json.success
		assert !controller.response.json.posts
		assert controller.response.json.message == messageSource.getMessage("not.found.message", 
			["Discussion"] as Object[], null) 
	}

	@Test
	void "Test index when no more posts are available to list"() {
		assert discussion
		
		assert !DiscussionPost.count()

		controller.session.userId = user.id
		controller.params.discussionHash = discussion.hash
		controller.params.max = "5"
		controller.params.offset = 0

		controller.index()

		assert !controller.response.json.posts
		assert controller.response.json.success
	}

	@Test
	void "Test index when offset is 0"() {
		assert discussion
		
		assert !DiscussionPost.count()

		controller.session.userId = user.id
		controller.params.discussionHash = discussion.hash
		controller.params.max = "5"

		controller.index()

		assert !controller.response.json.posts
		assert controller.response.json.success
		assert controller.response.json.discussionDetails.hash == discussion.hash
	}

	@Test
	void "Test index when offset is not 0"() {
		assert discussion
		
		assert !DiscussionPost.count()

		controller.session.userId = user.id
		controller.params.discussionHash = discussion.hash
		controller.params.max = "5"
		controller.params.offset = 5

		controller.index()

		assert !controller.response.json.posts
		assert controller.response.json.success
		assert !controller.response.json.discussionDetails.hash
	}

	@Test
	void "Test index"() {
		assert discussion
		
		DiscussionPost post = DiscussionPost.create(discussion, user.id, "Test comment") 
		Utils.save(post, true)
		DiscussionPost post2 = DiscussionPost.create(discussion, user.id, "Test comment2") 
		Utils.save(post2, true)

		controller.session.userId = user.id
		controller.params.discussionHash = discussion.hash
		controller.params.max = "5"
		controller.params.offset = 0

		controller.index()

		assert controller.response.json.posts.toString().indexOf('"<p class="without-margin">TestComment2</p>')
	}
}

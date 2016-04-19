package us.wearecurio.controller.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import us.wearecurio.controller.DiscussionController
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
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

		controller = new DiscussionController()
		testGroup = UserGroup.create("testgroup", "Test discussions", "Discussion topics for testing users",
				[isReadOnly:false, defaultNotify:false])
		readOnlyTestGroup = UserGroup.create("testReadOnlyGroup", "Test read only discussions", "Discussion topics for testing users",
				[isReadOnly:true, defaultNotify:false])
		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		Utils.save(user2, true)
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
    void testCreateWithGroupNameWithWritePermission() {
		//Adds a new discussion to the specified group
		params.group = "testgroup"
		controller.params.putAll(params)
		controller.session.userId = user2.getId()
		controller.request.method = "POST"
		controller.save()
		assert Discussion.count() == 1
    }

    @Test
    void testCreateReadOnly() {
		controller.params.putAll(params)
		controller.session.userId = user2.getId()
		controller.request.method = "POST"
		controller.save()
		assert Discussion.count() == 0
    }

    @Test
    void testCreateWithGroupNameWithNoWritePermission() {
		//Trying to add a new discussion to the specified group
		// with no write permission
		def noWritePermissionGroup = UserGroup.create("nowrite", "Test discussions", "Discussion topics for testing users",
				[isReadOnly:false, defaultNotify:false])
		params.group = "nowrite"
		controller.params.putAll(params)
		controller.request.method = "POST"
		controller.session.userId = user2.getId()
		controller.save()
		assert Discussion.count() == 0
    }
    @Test
    void testCreateWithNoGroup() {
		//Adds a new discussion to the default group
		params.remove("group")
		controller.params.putAll(params)
		controller.session.userId = user2.getId()
		controller.request.method = "POST"
		controller.save()
		assert Discussion.count() == 1
    }

	@Test
	void "Test delete discussion when discussion is null"() {

		controller.session.userId = user2.id
		controller.params.id = 0
		controller.request.method = "DELETE"
		controller.delete()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.not.found.message",
				["Discussion"] as Object[], null)
	}

	@Test
	void "Test delete discussion when user does not have write permission"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)

		assert discussion

		controller.session.userId = user.id
		controller.params.id = discussion.hash
		controller.request.method = 'DELETE'
		controller.delete()

		assert !controller.response.json.success
		assert controller.response.json.message == "You don't have the right to delete this discussion."
	}

	@Test
	void "Test delete discussion when discussion is present"() {
		testGroup.addWriter(user)
		Discussion discussion = Discussion.create(user, "test Discussion", testGroup)

		assert discussion

		controller.session.userId = user.id
		controller.params.id = discussion.hash
		controller.request.method = 'DELETE'
		controller.delete()

		assert controller.response.json.success
		assert controller.response.json.message == "Discussion deleted successfully."
		assert !Discussion.count()
	}

	@Test
	void "Test show when discussion is null"() {

		controller.session.userId = user2.id
		controller.params.id = 0

		controller.show()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("not.exist.message", ["Discussion"] as Object[], null)

	}

	@Test
	void "Test show when user is not logged in an disccussion is not public"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		discussion.visibility = Visibility.PRIVATE
		Utils.save(discussion, true)

		controller.params.id = discussion.hash
		controller.show()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.login.message", null, null)
	}

	@Test
	void Testshow() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion

		controller.params.id = discussion.hash
		controller.session.userId = user2.id
		controller.show()

		assert controller.response.json.success
		assert controller.response.json.discussionDetails.discussionHash == discussion.hash
	}

	@Test
	void "Test publish when user is not permitted"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		discussion.visibility = Visibility.PRIVATE
		Utils.save(discussion, true)

		controller.session.userId = user.id
		controller.params.id = discussion.id
		controller.publish()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.permission.denied", null, null)
		assert !discussion.isPublic()
	}

	@Test
	void "Test publish when discussion is null"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion

		controller.session.userId = user.id
		controller.params.id = null
		controller.publish()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("not.exist.message", ["Discussion"] as Object[], null)
	}

	@Test
	void "Test publish"() {
 		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		discussion.visibility = Visibility.PRIVATE
		Utils.save(discussion, true)

		controller.session.userId = user2.id
		controller.params.id = discussion.id
		controller.publish()

		assert controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.updated.message", ["Discussion"] as Object[], null)
		assert discussion.isPublic()
	}

	@Test
	void "test update discussion name and firstPost"() {
		Discussion discussion = Discussion.create(user2, "test Discussion", testGroup)
		assert discussion.name == "test Discussion"
		assert !discussion.getFirstPost()
		assert discussion.id != null

		DiscussionPost.createFirstPost('test description', user2, discussion, null)
		assert discussion.getFirstPost().message == 'test description'

		controller.session.userId = user2.id
		controller.request.contentType = "text/json"
		controller.request.content = "{'discussionHash': ${discussion.hash}, 'name': 'foo', 'message': 'updated description'}"
		controller.request.method = "PUT"

		controller.update()

		assert controller.response.json.success
		assert discussion.name == "foo"
		assert discussion.getFirstPost().message == "updated description"
	}
}

package us.wearecurio.controller.integration

import static org.junit.Assert.*
import org.junit.*
import org.scribe.model.Response

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
		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
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
		def retVal = controller.create()
		assert Discussion.count() == 1
    }
}

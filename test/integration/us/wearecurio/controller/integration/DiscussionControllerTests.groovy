package us.wearecurio.controller.integration

import static org.junit.Assert.*
import org.junit.*
import org.scribe.model.Response

import us.wearecurio.controller.DiscussionController
import us.wearecurio.model.*
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils

class DiscussionControllerTests extends CuriousControllerTestCase {

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
    void testCreateWithGroupName() {
		controller = new DiscussionController()
		controller.params.putAll(params)
		controller.session.userId = user2.getId()
		controller.params['group'] = 'curious'
		def retVal = controller.create()
		assert Discussion.count() == 1
    }
}

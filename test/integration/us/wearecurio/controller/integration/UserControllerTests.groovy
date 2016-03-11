package us.wearecurio.controller.integration

import java.util.Date

import us.wearecurio.model.User

import static org.junit.Assert.*
import org.junit.*
import org.scribe.model.Response

import us.wearecurio.controller.UserController
import us.wearecurio.model.*
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils
import us.wearecurio.hashids.DefaultHashIDGenerator

class UserControllerTests extends CuriousControllerTestCase {
	static transactional = true

	UserController controller
	User dummyUser2

	@Before
	void setUp() {
		super.setUp()
		
		controller = new UserController()
		
		Map params = [username: "a", sex: "F", last: "y", email: "a@a.com", birthdate: "01/01/2001", name: "a", password: "y"]
		dummyUser2 = User.create(params)

		Utils.save(dummyUser2, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "Test show when wrong hash id is passed"() {
		controller.params["id"] = 0
		
		controller.show()
		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("not.exist.message", ["User"] as Object[], null)
	}

	@Test
	void "Test show when requested user details are not of current user"() {
		controller.params["id"] = dummyUser2.hash
		controller.session.userId = userId
		
		controller.show()
		assert controller.response.json.success
		assert controller.response.json.user.username == "a"
		assert !controller.response.json.user.email
	}

	@Test
	void "Test show when requested user details are of current user"() {
		controller.params["id"] = dummyUser2.hash
		controller.session.userId = dummyUser2.id

		controller.show()
		assert controller.response.json.success
		assert controller.response.json.user.username == "a"
		assert controller.response.json.user.email == 'a@a.com'
	}
	
	@Test
	void "Test add tutorial tags"() {
		controller.params["id"] = dummyUser2.hash
		controller.session.userId = dummyUser2.id
		controller.params["tags[]"] = ['sleep','supplements']

		controller.addTutorialTags()
		assert controller.response.json.success
		assert dummyUser2.hasInterestTag(Tag.look('sleep'))
		assert dummyUser2.hasInterestTag(Tag.look('supplements'))
		def entries = Entry.fetchListData(dummyUser2, "Etc/UTC", new Date(), new Date())
		boolean found = false
		for (entry in entries) {
			if (entry.description == 'sleep quality')
				found = true
		}
		assert found
	}
}

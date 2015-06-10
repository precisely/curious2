package us.wearecurio.controller.integration

import static org.junit.Assert.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Model.Visibility
import us.wearecurio.controller.SearchController
import us.wearecurio.model.*
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils

class SearchControllerTests extends CuriousControllerTestCase {

	SearchController controller
	Sprint sprint1, sprint2, sprint3
	User user2, user3
	Discussion discussion1, discussion2

	@Before
	void setUp() {
		super.setUp()

		controller = new SearchController()
		
		// TO DO: Implement common method in CuriousControllerTestCase to create users 
		Map params = new HashMap()
		params.put("username", "testuser2")
		params.put("email", "test2@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first2")
		params.put("last", "last2")
		params.put("sex", "M")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1991")

		user2 = User.create(params)
		Utils.save(user2, true)

		params.put("username", "testuser3")
		params.put("email", "test3@test.com")

		user3 = User.create(params)
		Utils.save(user3, true)
		
		Date now = new Date()
		
		sprint1 = Sprint.create(now, user, "demo1", Visibility.PRIVATE)
		sprint2 = Sprint.create(now, user, "demo2", Visibility.PRIVATE)
		sprint3 = Sprint.create(now, user, "demo3", Visibility.PRIVATE)
		sprint1.addMember(user2.getId())

		discussion1 = Discussion.create(user, "Discussion1 name")
		Utils.save(discussion1, true)
		discussion2 = Discussion.create(user, "Discussion2 name")
		Utils.save(discussion2, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "Test indexData when user is not in session"() {
		controller.params["type"] = "people"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.indexData()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("auth.error.message", null, null)
	}

	@Test
	void "Test indexData when no type is passed"() {
		controller.session.userId = user.getId()
		controller.params["type"] = null
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.indexData()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Type"] as Object[] , null)
	}

	@Test
	void "Test indexData when wrong type is passed"() {
		controller.session.userId = user.getId()
		controller.params["type"] = "falseType"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.indexData()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Type"] as Object[] , null)
	}

	@Test
	void "Test indexData when type is people"() {
		controller.session.userId = user.getId()
		controller.params["type"] = "people"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.indexData()

		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 2
		assert controller.response.json.listItems[0].id == user2.id
		assert controller.response.json.listItems[1].id == user3.id
	}

	@Test
	void "Test indexData when type is discussions"() {
		controller.session.userId = user.getId()
		controller.params["type"] = "discussions"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.indexData()

		assert controller.response.json.success
		assert controller.response.json.listItems["discussionList"].size() == 2
		assert controller.response.json.listItems["discussionList"][0].id == discussion2.id ||
				controller.response.json.listItems["discussionList"][0].id == discussion1.id
		assert controller.response.json.listItems["discussionList"][1].id == discussion2.id ||
				controller.response.json.listItems["discussionList"][1].id == discussion1.id
	}

	@Test
	void "Test indexData when type is sprints"() {
		controller.session.userId = user.getId()
		controller.params["type"] = "sprints"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.indexData()

		assert controller.response.json.success
		assert controller.response.json.listItems.sprintList.size() == 3
		assert controller.response.json.listItems.sprintList[0].id == sprint1.id ||
				controller.response.json.listItems.sprintList[0].id == sprint2.id ||
				controller.response.json.listItems.sprintList[0].id == sprint3.id
		assert controller.response.json.listItems.sprintList[1].id == sprint1.id ||
				controller.response.json.listItems.sprintList[1].id == sprint2.id ||
				controller.response.json.listItems.sprintList[1].id == sprint3.id
	}

	@Test
	void "Test indexData when type is all"() {
		controller.session.userId = user.getId()
		controller.params["type"] = "all"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.indexData()

		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 7
		assert controller.response.json.listItems[0].id == sprint3.id ||
				controller.response.json.listItems[0].id == sprint2.id ||
				controller.response.json.listItems[0].id == sprint1.id
		assert controller.response.json.listItems[6].id == user2.id ||
				controller.response.json.listItems[6].id == user3.id
	}
}

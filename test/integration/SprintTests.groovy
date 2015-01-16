import grails.test.*

import us.wearecurio.server.Session
import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.Model.Visibility
import us.wearecurio.utility.Utils

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class SprintTests extends CuriousTestCase {
	static transactional = true

	User user1
	User user2

	@Before
	void setUp() {
		super.setUp()
		
		Map params = new HashMap()
		params.put("username", "testuser")
		params.put("email", "test@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first")
		params.put("last", "last")
		params.put("sex", "F")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1990")

		user1 = User.create(params)
		Utils.save(user1, true)

		params = new HashMap()
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
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreateSprint() {
		def sprint = Sprint.create(user1)
		assert sprint.userId == user1.id
		
		sprint = Sprint.create(user2, "Sprint", Visibility.PUBLIC)
		assert sprint.userId == user2.id
		assert sprint.name == "Sprint"
		assert sprint.visibility == Visibility.PUBLIC
	}
}

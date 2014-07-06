import grails.test.*

import us.wearecurio.server.Session
import us.wearecurio.integration.CuriousTestCase;
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class SessionTests extends CuriousTestCase {
	static transactional = true

	User user1
	User user2

	@Before
	void setUp() {
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
	}

	@Test
	void testCreateNewSession() {
		def session = Session.createOrGetSession(user1)
		assert session != null
		assert (session.fetchUuid() != null) && session.fetchUuid().length() > 10 && session.fetchUuid().length() < 50

		def session2 = Session.createOrGetSession(user2)
		assert session2 != null
		assert (session2.fetchUuid() != null) && session2.fetchUuid().length() > 10 && session2.fetchUuid().length() < 50
		assert session.fetchUuid() != session2.fetchUuid()

		def lookupUser = Session.lookupSessionUser(session.fetchUuid())
		assert lookupUser.getId() == session.getUserId()
	}
}

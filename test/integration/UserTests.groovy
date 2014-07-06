import grails.test.*

import us.wearecurio.integration.CuriousTestCase;
import us.wearecurio.model.*
import us.wearecurio.utility.*

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class UserTests extends CuriousTestCase {
	static transactional = true

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreate() {
		Map params = new HashMap()
		params.put("username", "testuser")
		params.put("email", "test@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first")
		params.put("last", "last")
		params.put("sex", "F")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1990")
		params.put("notifyOnComments", "false")
		
		def user = User.create(params)
		
		assert !user.getNotifyOnComments()

		assertTrue(Utils.save(user))
	}

	@Test
	void testCreateDefault() {
		Map params = new HashMap()
		params.put("username", "testuser")
		params.put("email", "test@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first")
		params.put("last", "last")
		params.put("sex", "F")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1990")
		
		def user = User.create(params)
		
		assert user.getNotifyOnComments()

		assertTrue(Utils.save(user))
	}
}

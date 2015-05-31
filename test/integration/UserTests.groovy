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
		params.put("birthdate", "12/1/1990")
		
		def user = User.create(params)
		
		assert user.getNotifyOnComments()

		assertTrue(Utils.save(user))
	}
	
	@Test
	void testCreateVirtual() {
		User user = User.createVirtual()
		
		assert user.virtual
	}

	User user2, user3, user4
	void createUsers() {
		Map params = new HashMap()
		params.put("username", "testuser2")
		params.put("email", "test2@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first2")
		params.put("last", "last2")
		params.put("sex", "M")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1991")
		
		// This is to test the order of the users in the list while fetching the users
		params.put('created', new Date())

		user2 = User.create(params)
		Utils.save(user2, true)
		
		Thread.sleep(100)

		params.put("username", "testuser3")
		params.put("email", "test3@test.com")
		params.put('created', new Date() - 1)

		user3 = User.create(params)
		Utils.save(user3, true)
		
		Thread.sleep(100)

		params.put("username", "testuser4")
		params.put("email", "test4@test.com")
		params.put('created', new Date() - 2)

		user4 = User.create(params)
		Utils.save(user4, true)
		
		Thread.sleep(100)
	}

	@Test
	void "Test getUsersList when userId is null"() {
		createUsers()
		List users = User.getUsersList(5, 0, null)

		assert !users.size()
	}

	@Test
	void "Test getUsersList when userId is passed"() {
		createUsers()
		List users = User.getUsersList(5, 0, user.id)

		assert users.size() == 3
		assert users[0].id == user2.id
		assert users[1].id == user3.id
		assert users[2].id == user4.id
	}

	@Test
	void "Test getUsersList when there are virtual users as well"() {
		User user5 = User.createVirtual()
		Thread.sleep(100)
		User user6 = User.createVirtual()
		Thread.sleep(100)
		createUsers()

		List users = User.getUsersList(5, 0, user.id)

		assert users.size() == 3
		assert users[0].id == user2.id
		assert users[1].id == user3.id
		assert users[2].id == user4.id
	}
}

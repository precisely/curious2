package us.wearecurio.factories.integration

import static org.junit.Assert.*
import org.junit.*
import us.wearecurio.factories.UserFactory
import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.User


class UserFactoryTests extends CuriousTestCase {
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
	void testMake() {
		def user = UserFactory.make()
		assert user.username == 'y'
		assert User.countByUsername('y') == 1
	}
	
	@Test
	void testIdempotent() {
		UserFactory.make()
		def user = UserFactory.make()
		assert user.username == 'y'
		assert User.countByUsername('y') == 1
	}
	
	@Test
	void testMakeANewUserByPassingInUsername() {
		UserFactory.make([username: 'a'])
		UserFactory.make([username: 'b'])
		assert User.count() == 3
		assert User.list()*.username == ["y", "a", "b"]
	}
	
	@Test
	void testUpdateUsingLastValueIfAlreadyExists() {
		UserFactory.make()
		def user = UserFactory.make()
		assert user.username == 'y'
		assert User.countByUsername('y') == 1
		assert user.name == 'y'
		
		UserFactory.make(username: 'y', name: 'asdf')
		assert User.last().name == 'asdf'
	}
}

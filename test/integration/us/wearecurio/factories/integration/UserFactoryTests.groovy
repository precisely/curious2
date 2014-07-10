package us.wearecurio.factories.integration

import static org.junit.Assert.*
import org.junit.*
import us.wearecurio.factories.UserFactory
import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.User


class UserFactoryTests extends CuriousTestCase {
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
		assert User.count() == 2
		assert User.first().username == 'a'
		assert User.last().username == 'b'
	}
	
	@Test
	void testUpdateUsingLastValueIfAlreadyExists() {
		UserFactory.make()
		def user = UserFactory.make()
		assert user.username == 'y'
		assert User.countByUsername('y') == 1
		assert user.first == 'y'
		
		UserFactory.make(username: 'y', first: 'asdf')
		assert User.last().first == 'asdf'
	}
}

package us.wearecurio.integration

import org.junit.After
import org.junit.Before

import us.wearecurio.model.User
import us.wearecurio.utility.Utils

abstract class CuriousTestCase extends GroovyTestCase {

	Long userId
	User user

	@Before
	void setUp() {
		super.setUp()

		Utils.resetForTesting()

		Map params = [username: "y", sex: "F", last: "y", email: "y@y.com", birthdate: "01/01/2001", first: "y", password: "y"]

		user = User.create(params)

		Utils.save(user, true)
		log.debug "New User $user"

		userId = user.getId()
	}

	@After
	void tearDown() {
		User.executeUpdate("delete User u")
		super.tearDown()
	}
}

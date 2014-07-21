package us.wearecurio.integration

import us.wearecurio.model.User
import us.wearecurio.utility.Utils

abstract class CuriousTestCase extends GroovyTestCase {

	Long userId
	User user

	void setUp() {
		super.setUp()

		Utils.resetForTesting()

		Map params = [username: "y", sex: "F", last: "y", email: "y@y.com", birthdate: "01/01/2001", first: "y", password: "y"]

		user = User.create(params)

		Utils.save(user, true)
		log.debug "New User $user"

		userId = user.getId()
	}

	void tearDown() {
		super.tearDown()
	}
}

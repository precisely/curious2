package us.wearecurio.integration

import org.junit.After
import org.junit.Before

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.User
import us.wearecurio.utility.Utils

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

@TestMixin(IntegrationTestMixin)
abstract class CuriousTestCase {

	private static def log = LogFactory.getLog(this)

	def messageSource
	Long userId
	User user

	@Before
	void setUp() {
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
	}
}

package us.wearecurio.integration

import org.junit.After
import org.junit.Before

import org.apache.commons.logging.LogFactory

import us.wearecurio.model.User
import us.wearecurio.utility.Utils
import us.wearecurio.model.Entry
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagUnitStats

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

		Entry.executeUpdate("delete Entry e")
		Discussion.executeUpdate("delete Discussion d")
		DiscussionPost.executeUpdate("delete DiscussionPost p")
		TagStats.executeUpdate("delete TagStats t")
		TagUnitStats.executeUpdate("delete TagUnitStats t")
		
		Map params = [username: "y", sex: "F", email: "y@y.com", birthdate: "01/01/2001", name: "y y", password: "y"]

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

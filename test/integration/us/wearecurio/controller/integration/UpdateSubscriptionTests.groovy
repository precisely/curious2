package us.wearecurio.controller.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import us.wearecurio.controller.UpdateSubscriptionController


class UpdateSubscriptionTests extends CuriousControllerTestCase {
	UpdateSubscriptionController controller

	@Before
	void setUp() {
		super.setUp()

		controller = new UpdateSubscriptionController()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void " Try to save the user-subscription without email"() {
		when: 'Provide parameters without email to the action '
		controller.params.categories = "Autism app, ME/CFS app, Other"
		controller.params.description = "It is integration testing"
		controller.save()

		then: 'Listing user subscription detail successfully'
		assert controller.response.redirectedUrl == "home/login"
	}

	@Test
	void "To save the user-subscription data successfully"() {
		when: 'Provide legal parameters to the action '
		controller.params.email = "dummy2@curious.test"
		controller.params.categories = "Autism app, ME/CFS app, Other"
		controller.params.description = "It is integration testing"
		controller.save()

		then: 'Saved data successfully in database'
		assert controller.response.status == 200

		when: 'Parameters description is given to the action '
		controller.params.email = "dummy2@curious.test"
		controller.params.categories = "Autism app, ME/CFS app, Other"
		controller.save()

		then: 'Saved data successfully in database'
		assert controller.response.status == 200
	}
}
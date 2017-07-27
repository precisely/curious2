package us.wearecurio.controller

import spock.lang.Specification
import grails.test.mixin.TestFor
import us.wearecurio.model.UpdateSubscription
/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@Mock([UpdateSubscription])
@TestFor(UpdateSubscriptionController)
class UpdateSubscriptionSpec extends Specification {

	void "test index action"() {
		when: 'When email is not given to the action '
		params.email = ""
		controller.save()

		then: 'Redirect to login page'
		controller.response.redirectedUrl == "home/login"
	}

	void "test index action for data save" () {
		when: 'Provide legal parameters to the action '
		controller.params.email = "dummy2@curious.test"
		controller.params.categories = "Autism app, ME/CFS app, Other"
		controller.params.description = "It is integration testing"
		controller.save()

		then: 'Saved data successfully in database'
		controller.response.status == 200

		when: 'email and categories is given to the action'
		controller.params.email = "dummy2@curious.test"
		controller.params.description = "It is integration testing"
		controller.save()

		then: 'Saved data successfully in database'
		controller.response.status == 200

		when: 'Parameter email is given to the action'
		params.email = "textemail@unit.text"
		controller.save()

		then: 'Data saved successfully in database'
		controller.response.status == 200
	}
}



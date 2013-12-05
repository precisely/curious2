package us.wearecurio.controller

import grails.test.GrailsMock;
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*
import org.scribe.model.Token

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.controller.AuthenticationController
import us.wearecurio.services.SecurityService
import us.wearecurio.model.User

@TestMixin(GrailsUnitTestMixin)
@TestFor(AuthenticationController)
@Mock([User])
class AuthenticationControllerTests {

	GrailsMock oauthServiceMock
	GrailsMock securityServiceMock
	
	void setUp() {
		/*User userInstance = new User(username: "dummy", email: "dummy@curious.test", sex: "M")
		 assert userInstance.save()*/

		oauthServiceMock = mockFor(OauthService)
		oauthServiceMock.demand.findSessionKeyForAccessToken(1..4) { provider -> return provider }
		controller.oauthService = oauthServiceMock.createMock()
		securityServiceMock = mockFor(SecurityService)
		
		controller.metaClass.toUrl { map ->
			def url = map.controller + '/' + map.action
			if (map.params) {
				url += "?" + map.params.collect { key, value -> "$key=$value" }.join("&")
			}
			return url
		}
		User userInstance = new User([username: "dummy", email: "dummy@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		
		securityServiceMock.demand.getCurrentUser(1..2) { ->
			return User.get(1)
		}
		controller.securityService = securityServiceMock.createMock()

		assert userInstance.save()
	}

	void tearDown() {
		// Tear down logic here
	}

	void testTwenty3AndMeAuthForSuccess() {
		params.provider = "twenty3andmeDummy"
		params.status = "success"

		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert response.redirectedUrl == "login/auth"

		response.reset()

		controller.session.deniedURI = "home/userpreferences"
		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert response.redirectedUrl == "home/userpreferences"

		response.reset()

		controller.session.twenty3andmeDummy = new Token("dummy", "")
		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert !response.redirectedUrl

		response.reset()

		controller.session.returnURIWithToken = "home/registerDummyAPI"
		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert response.redirectedUrl == "home/registerDummyAPI"

		response.reset()
	}

	void testTwenty3AndMeAuthForFailure() {
		params.provider = "twenty3andmeDummy"
		params.status = "fail"

		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert response.redirectedUrl == "login/auth"

		response.reset()

		controller.session.deniedURI = "home/userpreferences"
		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert response.redirectedUrl == "home/userpreferences"
	}

}
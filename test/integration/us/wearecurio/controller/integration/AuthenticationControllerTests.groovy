package us.wearecurio.controller.integration

import static org.junit.Assert.*
import static us.wearecurio.model.ThirdParty.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Token

import us.wearecurio.controller.AuthenticationController
import us.wearecurio.model.OAuthAccount

class AuthenticationControllerTests extends CuriousControllerTestCase {

	def securityService

	AuthenticationController controller

	@Before
	void setUp() {
		super.setUp()
		controller = new AuthenticationController()

		controller.session.userId = userId
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	/**
	 * Integration test by default doesn't call the filters or interceptors.
	 * Hence writing test case for beforeInterceptor method.
	 */
	void testCheckAuthentication() {
		controller.params.provider = "moves"
		controller.params.status = "fail"
		controller.checkAuthentication()
		assert controller.response.redirectedUrl.contains("home/login")	// If authentication denied or failed & no denied URI set

		controller.response.reset()

		controller.params.status = "success"
		controller.metaClass.tokenInstance = null
		controller.checkAuthentication()

		assert controller.response.redirectedUrl.contains("home/login")	// If authentication success but token not generated

		controller.response.reset()

		controller.session.returnURIWithToken = "home/registerDummyAPI"
		controller.checkAuthentication()
		controller.response.redirectUrl.contains("home/registerDummyAPI")	// If authentication failed due to any case & session uri present

		controller.response.reset()

		controller.params.status = "success"
		controller.session["moves:oasAccessToken"] = new Token("", "")
		assert controller.checkAuthentication() == true
	}

	void testMovesAuth() {
		controller.params.provider = "moves"

		controller.session["moves:oasAccessToken"] =  new Token("some-token", "some-secret", """{"user_id": "1314"}""")
		controller.checkAuthentication()
		controller.movesAuth()	// Available raw response for moves authenticated user in token
		assert OAuthAccount.countByTypeId(MOVES) == 1
	}

	void testFitbitAuth() {
		controller.fitBitDataService = [
			getUserProfile: { token->
				return new JSONObject([user: [encodedId: "dummy1234"]])
			}
		]
		controller.params.status = "success"
		controller.params.provider = "fitbit"

		controller.session["fitbit:oasAccessToken"] = new Token("some-token", "some-secret", "")

		controller.checkAuthentication()
		controller.fitbitAuth()
		assert OAuthAccount.countByTypeId(FITBIT) == 1
	}

	void testTwenty3AndMeAuth() {
		controller.twenty3AndMeDataService = [
			getUserProfile: { token->
				return new JSONObject("""{"id": "dummy1234"}""")
			}
		]
		controller.params.status = "success"
		controller.params.provider = "twenty3andme"

		controller.session["twenty3andme:oasAccessToken"] =  new Token("d914a5723ed53e84c58fb376a4cca575", "some-secret", "")
		controller.checkAuthentication()
		controller.twenty3andmeAuth()
		assert OAuthAccount.countByTypeIdAndUserId(TWENTY_THREE_AND_ME, userId) == 1
	}

}
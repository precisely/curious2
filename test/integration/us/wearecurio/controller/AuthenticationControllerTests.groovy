package us.wearecurio.controller

import static org.junit.Assert.*
import static us.wearecurio.model.OAuthAccount.*

import org.junit.*
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User

class AuthenticationControllerTests extends GroovyTestCase {

	def securityService

	AuthenticationController controller
	User userInstance

	@Before
	void setUp() {
		userInstance = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance.save()

		controller = new AuthenticationController()
	}

	@After
	void tearDown() {
	}

	/**
	 * Integration test by default doesn't call the filters or interceptors.
	 * Hence writing test case for beforeInterceptor method.
	 */
	void testAuthRedirect() {
		controller.params.provider = "moves"
		controller.params.status = "fail"
		controller.authRedirect()
		assert controller.response.redirectedUrl.contains("home/login")	// If authentication denied or failed & no denied URI set

		controller.response.reset()

		controller.params.status = "success"
		controller.metaClass.tokenInstance = null
		controller.authRedirect()

		assert controller.response.redirectedUrl.contains("home/login")	// If authentication success but token not generated

		controller.response.reset()

		controller.session.returnURIWithToken = "home/registerDummyAPI"
		controller.authRedirect()
		controller.response.redirectUrl.contains("home/registerDummyAPI")	// If authentication failed due to any case & session uri present

		controller.response.reset()

		controller.params.status = "success"
		controller.session["moves:oasAccessToken"] = new Token("", "")
		assert controller.authRedirect() == true
	}

	void testMovesAuthWithNoLoggedInUser() {
		controller.movesAuth()	// No logged in user & no redirect url
		assert controller.response.redirectedUrl == null

		controller.response.reset()

		controller.session.returnURIWithToken = "home/some-url"
		controller.movesAuth()	// Redirect url set & no logged in user
		assert controller.response.redirectedUrl == "home/some-url"
		assert !controller.session.returnURIWithToken
	}

	void testMovesAuthWithLoggedInUser() {
		// Deleting old entries fetched by actual database to H2 database.
		OAuthAccount.findAllByTypeId(MOVES_ID)*.delete()

		controller.params.provider = "moves"
		controller.session.userId = userInstance.id
		controller.session["moves:oasAccessToken"] = new Token("", "", "")	// Checking if no raw response is available in token

		controller.authRedirect()
		controller.movesAuth()	// No raw response for moves authenticated user
		assert OAuthAccount.countByTypeId(MOVES_ID) == 0

		controller.response.reset()

		controller.session["moves:oasAccessToken"] =  new Token("some-token", "some-secret", """{"user_id": 271314}""")
		controller.authRedirect()
		controller.movesAuth()	// Available raw response for moves authenticated user in token
		assert OAuthAccount.countByTypeId(MOVES_ID) == 1
	}

	void testFitbitAuthIfNoLoggedInUser() {
		controller.fitbitAuth()	// No logged in user & no redirect url
		assert controller.response.redirectedUrl == null

		controller.response.reset()

		controller.session.returnURIWithToken = "home/some-url"
		controller.fitbitAuth()	// Redirect url set & no logged in user
		assert controller.response.redirectedUrl == "home/some-url"
		assert !controller.session.returnURIWithToken
	}

	void testFitbitAuthAuthForIfLoggedInUser() {
		// Deleting old entries fetched by actual database to H2 database.
		OAuthAccount.findAllByTypeId(FITBIT_ID)*.delete()

		controller.params.status = "success"
		controller.params.provider = "fitbit"
		controller.session.userId = userInstance.id

		// If invalid token in provided
		controller.session["fitbit:oasAccessToken"] = new Token("expired-token", "salkdjfsajfdlsa")
		controller.authRedirect()
		controller.fitbitAuth()
		assert OAuthAccount.countByTypeId(FITBIT_ID) == 0

		controller.response.reset()

		// If valid token is provided
		controller.session["fitbit:oasAccessToken"] = new Token("3624f6a423e04fbcc07116ca21a72543", "dd75d01d1e1a54117c933a32f6ed031b")

		controller.authRedirect()
		controller.fitbitAuth()
		assert OAuthAccount.countByTypeId(FITBIT_ID) == 1
	}

	void testTwenty3AndMeAuthIfNoLoggedInUser() {
		controller.twenty3andmeAuth()	// No logged in user & no redirect url
		assert controller.response.redirectedUrl == null

		controller.response.reset()

		controller.session.returnURIWithToken = "home/some-url"
		controller.twenty3andmeAuth()	// Redirect url set & no logged in user
		assert controller.response.redirectedUrl == "home/some-url"
		assert !controller.session.returnURIWithToken
	}

	void testTwenty3AndMeAuthForIfLoggedInUser() {
		// Deleting old entries fetched by actual database to H2 database.
		OAuthAccount account = OAuthAccount.findByTypeIdAndUserId(TWENTY_3_AND_ME_ID, userInstance.id)
		Twenty3AndMeData.findAllByAccount(account)*.delete()
		account?.delete()

		controller.params.status = "success"
		controller.params.provider = "twenty3andme"
		controller.session.userId = userInstance.id
		// Invalid token
		controller.session["twenty3andme:oasAccessToken"] =  new Token("expired-token", "some-secret")

		controller.authRedirect()
		controller.twenty3andmeAuth()	// No there is no information in user profile info
		assert OAuthAccount.countByTypeIdAndUserId(TWENTY_3_AND_ME_ID, userInstance.id) == 0

		controller.response.reset()

		controller.session["twenty3andme:oasAccessToken"] =  new Token("1c908eee69d839f98fa59d4c97e12b1d", "some-secret")
		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert OAuthAccount.countByTypeIdAndUserId(TWENTY_3_AND_ME_ID, userInstance.id) == 1
	}

}
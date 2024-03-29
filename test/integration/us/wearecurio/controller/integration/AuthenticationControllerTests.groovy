package us.wearecurio.controller.integration

import static org.junit.Assert.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Token

import us.wearecurio.controller.AuthenticationController
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.ThirdParty
import us.wearecurio.services.WithingsDataService

class AuthenticationControllerTests extends CuriousControllerTestCase {
	static transactional = true

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
	@Test
	void testCheckAuthentication() {
		// Test when session expired.
		controller.session.userId = null
		controller.checkAuthentication()
		assert controller.response.redirectUrl.contains("home/login")

		controller.response.reset()
		controller.session.userId = userId

		// Test when user deny to authenticate
		controller.params.provider = "moves"
		controller.params.status = "fail"
		controller.checkAuthentication()
		assert controller.response.redirectedUrl.contains("home/login")	// If authentication denied or failed & no denied URI set

		controller.response.reset()

		// Test when user granted access but error creating token
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

	@Test
	void testMovesAuth() {
		controller.movesDataService = [
			getUserProfile: { token->
				return new JSONObject("""{"userId":1314,"profile":{"firstDate":"20121211","currentTimeZone":{"id":"Europe/Helsinki","offset":10800}}}""")
			}
		]
		controller.params.provider = "moves"

		controller.session["moves:oasAccessToken"] =  new Token("some-token", "some-secret")
		controller.checkAuthentication()
		controller.movesAuth()	// Available raw response for moves authenticated user in token
		assert OAuthAccount.countByTypeId(ThirdParty.MOVES) == 1
	}

	@Test
	void testTwitterAuth() {
		Token.getMetaClass().getRawResponse = { -> return "user_id=123" }
		controller.session["twitter:oasAccessToken"] = 'some-token'
		controller.twitterAuth()
		assert OAuthAccount.countByTypeId(ThirdParty.TWITTER) == 1
	}

	@Test
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
		assert OAuthAccount.countByTypeId(ThirdParty.FITBIT) == 1
	}

	@Test
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
		assert OAuthAccount.countByTypeIdAndUserId(ThirdParty.TWENTY_THREE_AND_ME, userId) == 1
	}

	@Test
	void testWithingsAuth() {
		controller.session.withingsUserId = "dummy1234"
		controller.withingsDataService = [
			getUserProfile: { token->
				return new JSONObject("""{"id": "dummy1234"}""")
			},
			fetchActivityData: {token, account, startdate, enddate, intraday ->
				return new JSONObject([status: 0, body: [activities: [[timezone: "Europe/Helsinki"]]]])
			}
		] as WithingsDataService

		controller.params.status = "success"
		controller.params.provider = "withings"

		controller.session["withings:oasAccessToken"] =  new Token("some-token", "some-secret", "")
		controller.checkAuthentication()
		controller.withingsAuth()
		OAuthAccount account = OAuthAccount.findByTypeIdAndUserId(ThirdParty.WITHINGS, userId)
		assert account != null
		assert account.timeZoneId != null
		assert account.accountId == "dummy1234"

		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(account.timeZoneId)
		assert timeZoneIdInstance.name == "Europe/Helsinki"
	}

	@Test
	void testWithingsAuthIfNoTimezone() {
		controller.session.withingsUserId = "dummy1234"
		controller.withingsDataService = [
			getUserProfile: { token->
				return new JSONObject("""{"id": "dummy1234"}""")
			},
			fetchActivityData: {token, account, startdate, enddate, intraday ->
				//returning non 0 status code
				return new JSONObject([status: 500, body: []])
			}
		] as WithingsDataService

		controller.params.status = "success"
		controller.params.provider = "withings"

		controller.session["withings:oasAccessToken"] =  new Token("some-token", "some-secret", "")
		controller.checkAuthentication()
		controller.withingsAuth()
		OAuthAccount account = OAuthAccount.findByTypeIdAndUserId(ThirdParty.WITHINGS, userId)
		assert account != null
		assert account.timeZoneId != null

		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(account.timeZoneId)
		// Will save default timezone
		assert timeZoneIdInstance.name == "Etc/UTC"
		assert account.accountId == "dummy1234"
	}
}
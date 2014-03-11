package us.wearecurio.services.integration

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User

import static org.junit.Assert.*
import grails.test.mixin.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.UrlService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException

class WithingsDataServiceTests extends CuriousServiceTestCase {

	UrlService urlService
	WithingsDataService withingsDataService
	User user2
	OAuthAccount account

	String mockedGeneratedURL
	String mockedGeneratedURLWithParams
	MockedHttpURLConnection mockedConnection = new MockedHttpURLConnection()
	MockedHttpURLConnection mockedConnectionWithParams = new MockedHttpURLConnection()

	def grailsApplication

	@Override
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		account = new OAuthAccount([typeId: ThirdParty.WITHINGS, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/Los_Angeles").id])

		account.save()

		withingsDataService.oauthService = [
			getWithingsResource: { token, url, body, header ->
				mockedGeneratedURL = url
				return new Response(mockedConnection)
			},
			getWithingsResourceWithQuerystringParams: { token, url, p, headers ->
				mockedGeneratedURLWithParams = url
				return new Response(mockedConnectionWithParams)
			}
		]
	}

	@Override
	void tearDown() {
	}

	void testRefreshSubscription() {
		Date now = new Date()
		account.lastSubscribed = new Date(now.getTime() - 10L * 24 * 60 * 60 * 1000)
		account.save()

		mockedConnectionWithParams = new MockedHttpURLConnection("""{"status": 0, "body": {"activities": []}}""")

		withingsDataService.refreshSubscriptions()
		assert account.lastSubscribed > now.clearTime()
	}

	void testSubscribeIfSuccess() {
		mockedConnectionWithParams = new MockedHttpURLConnection("""{"status": 0}""")

		withingsDataService.subscribe(userId)
		assert account.lastSubscribed
	}

	void testSubscribeIfNoOAuthAccount() {
		shouldFail(MissingOAuthAccountException) {
			withingsDataService.subscribe(user2.id)
		}
	}

	void testSubscribeIfFail() {
		// Checking for non zero status code.
		mockedConnectionWithParams = new MockedHttpURLConnection("""{"status": 2554}""")

		withingsDataService.subscribe(userId)
		assert OAuthAccount.findByUserId(userId).accessToken == ""
	}

	void testUnSubscribeIfNoOAuthAccount() {
		shouldFail(MissingOAuthAccountException) {
			withingsDataService.unsubscribe(user2.id)	// Passing user's ID whose oauth account doesn't exist
		}
	}

	void testUnsubscribeWithOAuthAccountExists() {
		mockedConnectionWithParams = new MockedHttpURLConnection("""{"status": 0}""")

		assertNotNull OAuthAccount.findByUserIdAndTypeId(userId, ThirdParty.WITHINGS)
		withingsDataService.unsubscribe(userId)
		assertNull OAuthAccount.findByUserIdAndTypeId(userId, ThirdParty.WITHINGS)
	}

	void testGetDataDefaultWithFailureStatusCode() {
		// When withings returns a non-zero status code
		mockedConnectionWithParams = new MockedHttpURLConnection("""{"status": 2555}""")

		Map result = withingsDataService.getDataDefault(account, new Date(), false)

		assert Entry.count() == 0

		try {
			// When token expires with code 342
			mockedConnectionWithParams = new MockedHttpURLConnection("""{"status": 342}""")
			withingsDataService.getDataDefault(account, new Date(), false)
		} catch(e) {
			e.cause instanceof InvalidAccessTokenException
		}
	}

	void testGetDataDefaultWithUnparsableResponse() {
		// When un-parsable string returned
		mockedConnectionWithParams = new MockedHttpURLConnection("""status = unparsable-response""")

		withingsDataService.getDataDefault(account, new Date(), false)
		assert Entry.count() == 0
	}

	void testGetDataDefault() {
		String mockedResponseData = """{"status":0,"body":{"updatetime":1385535542,"measuregrps":[{"grpid":162026288,"attrib":2,"date":1385386484,"category":1,"comment":"Hello","measures":[{"value":6500,"type":1,"unit":-2}]},{"grpid":162028086,"attrib":2,"date":1385300615,"category":1,"comment":"Nothing to comment","measures":[{"value":114,"type":9,"unit":0},{"value":113,"type":10,"unit":0},{"value":74,"type":11,"unit":0}]},{"grpid":129575271,"attrib":2,"date":1372931328,"category":1,"comment":"sa","measures":[{"value":170,"type":4,"unit":-2}]},{"grpid":129575311,"attrib":2,"date":1372844945,"category":1,"measures":[{"value":17700,"type":1,"unit":-2}]},{"grpid":129575122,"attrib":2,"date":1372844830,"category":1,"comment":"sa","measures":[{"value":6300,"type":1,"unit":-2}]},{"grpid":128995279,"attrib":0,"date":1372706279,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":138,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":128995003,"attrib":0,"date":1372706125,"category":1,"measures":[{"value":65861,"type":1,"unit":-3}]}]}}"""
		mockedConnectionWithParams = new MockedHttpURLConnection(mockedResponseData)
		mockedConnection = new MockedHttpURLConnection(mockedResponseData)

		withingsDataService.getDataDefault(account, new Date(), false)
		assert Entry.count() > 0
	}

	void testGetDataActivityMetrics() {
		Map result = withingsDataService.getDataActivityMetrics(account, null, null)

		assert result.success == false

		String mockedActivityResponse = """{"status":342}"""
		mockedConnectionWithParams = new MockedHttpURLConnection(mockedActivityResponse)

		try {
			withingsDataService.getDataActivityMetrics(account, null, null)
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException
			assert e.cause.provider.toLowerCase() == "withings"
		}

		mockedActivityResponse = """{"status":0,"body":{"activities":[]}}"""
		mockedConnectionWithParams = new MockedHttpURLConnection(mockedActivityResponse)
		result = withingsDataService.getDataActivityMetrics(account, new Date(), new Date())

		assertTrue result.success
		assert Entry.count() == 0	// No entries to process.

		mockedActivityResponse = """{"status":0,"body":{"activities":[{"date":"2014-02-05","steps":17897,"distance":16190.69,"calories":770.51,"elevation":425.03,"timezone":"America/Los_Angeles"},{"date":"2014-02-06","steps":12069,"distance":10300.22,"calories":552.4,"elevation":416.94,"timezone":"America/Los_Angeles"},{"date":"2014-02-07","steps":5182,"distance":4442.82,"calories":186.58,"elevation":99.81,"timezone":"America/Los_Angeles"},{"date":"2014-02-08","steps":13877,"distance":11466.84,"calories":573.63,"elevation":364.43,"timezone":"America/Los_Angeles"},{"date":"2014-02-09","steps":681,"distance":567.73,"calories":25.89,"elevation":14.9,"timezone":"America/Los_Angeles"},{"date":"2014-02-10","steps":13120,"distance":15563.38,"calories":999.1,"elevation":477.41,"timezone":"America/Los_Angeles"},{"date":"2014-02-11","steps":7115,"distance":7068.46,"calories":352.85,"elevation":191.14,"timezone":"America/Los_Angeles"},{"date":"2014-02-12","steps":7,"distance":7.58,"calories":0,"elevation":0,"timezone":"America/Los_Angeles"},{"date":"2014-02-16","steps":2775,"distance":2176.92,"calories":213.64,"elevation":311.58,"timezone":"America/Los_Angeles"},{"date":"2014-02-17","steps":17187,"distance":19381.9,"calories":1138.82,"elevation":427.05,"timezone":"America/Los_Angeles"},{"date":"2014-02-18","steps":5188,"distance":4691.28,"calories":212.18,"elevation":119.42,"timezone":"America/Los_Angeles"},{"date":"2014-02-19","steps":10066,"distance":9490.86,"calories":476.63,"elevation":313.55,"timezone":"America/Los_Angeles"},{"date":"2014-01-17","steps":1198,"distance":1044.62,"calories":42.48,"elevation":16.6,"timezone":"America/Los_Angeles"}]}}"""
		mockedConnectionWithParams = new MockedHttpURLConnection(mockedActivityResponse)
		result = withingsDataService.getDataActivityMetrics(account, startDate: new Date(), endDate: new Date() + 1)
		assert result.success == true
		assert Entry.count() > 0

	}

	void testSubscriptionParamsForHTTP() {
		Map result = withingsDataService.getSubscriptionParameters(account, false)
		assert result.callbackurl.contains("http://") == true

		grailsApplication.config.grails.serverURL = "https://dev.wearecurio.us/"
		result = withingsDataService.getSubscriptionParameters(account, false)
		assert result.callbackurl.contains("http://") == true
	}

}
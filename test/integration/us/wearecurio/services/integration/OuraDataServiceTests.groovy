package us.wearecurio.services.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scribe.model.Response
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.OuraDataService
import us.wearecurio.services.UrlService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

class OuraDataServiceTests  extends CuriousServiceTestCase {
	static transactional = true

	UrlService urlService
	OuraDataService ouraDataService
	OAuthAccount account
	User user2
	TimeZone serverTimezone
	TimeZone defaultTimezone

	@Before
	void setUp() {
		super.setUp()
		serverTimezone = TimeZone.getDefault()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
				password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user2, true)

		account = new OAuthAccount([typeId: ThirdParty.OURA, userId: userId, accessToken: "Dummy-token",
				accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	@After
	void tearDown() {
		super.tearDown()
		TimeZone.setDefault(serverTimezone)
	}

	@Test
	void "Test Subscribe"() {
		Map result = ouraDataService.subscribe(userId)
		assert result.success
	}

	@Test
	void "Test Subscribe if no OAuthAccount"() {
		shouldFail(MissingOAuthAccountException) {
			ouraDataService.subscribe(user2.id)
		}
	}

	@Test
	void "Test unsubscribe if no OAuthAccount"() {
		shouldFail(MissingOAuthAccountException) {
			ouraDataService.unsubscribe(user2.id)
		}
	}

	@Test
	void "Test unsubscribe with OAuthAccount exists"() {
		ouraDataService.oauthService = [
				deleteFitBitResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 204))
				}
		]

		assert OAuthAccount.count() == 1
		ouraDataService.unsubscribe(userId)
		assert OAuthAccount.count() == 0
	}

	@Test
	void "Test NotificationHandler"() {
		String notificationString = """{type: exercise, date: "2015-09-12", userId: 3}"""
		ouraDataService.notificationHandler(notificationString)
		assert ThirdPartyNotification.count() == 1
		assert ThirdPartyNotification.first().typeId == ThirdParty.OURA
	}

	@Test
	void "Test GetDataSleep with failure status code"() {
		ouraDataService.oauthService = [
				getOuraResourceWithQuerystringParams: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]
		// When oura returns a non-zero response code
		try {
			ouraDataService.getDataSleep(account, new Date(), false)
		} catch(e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

	@Test
	void "Test getDataSleep"() {
		String mockedResponseData = """{"someKey": "someValue"}"""
		ouraDataService.oauthService = [
				getOuraResourceWithQuerystringParams: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		Map result = ouraDataService.getDataSleep(account, new Date(), false)
		assert result.success == true
	}

	@Test
	void "Test getDataExercise with failure status code"() {
		ouraDataService.oauthService = [
				getOuraResourceWithQuerystringParams: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]
		// When oura returns a non-zero response code
		try {
			ouraDataService.getDataExercise(account, new Date(), false)
		} catch(e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

	@Test
	void "Test getDataExercise"() {
		String mockedResponseData = """{"someKey": "someValue"}"""
		ouraDataService.oauthService = [
				getOuraResourceWithQuerystringParams: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		Map result = ouraDataService.getDataExercise(account, new Date(), false)
		assert result.success == true
	}

	@Test
	void "Test getDataActivity with failure status code"() {
		ouraDataService.oauthService = [
				getOuraResourceWithQuerystringParams: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]
		// When oura returns a non-zero response code
		try {
			ouraDataService.getDataActivity(account, new Date(), false)
		} catch(e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

	@Test
	void "Test getDataActivity"() {
		String mockedResponseData = """{"someKey": "someValue"}"""
		ouraDataService.oauthService = [
				getOuraResourceWithQuerystringParams: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		Map result = ouraDataService.getDataActivity(account, new Date(), false)
		assert result.success == true
	}
}

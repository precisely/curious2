package us.wearecurio.services.integration

import grails.converters.JSON
import grails.test.GrailsMock
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Response
import org.scribe.model.Token

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.FitbitNotification
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.UrlService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.AuthenticationRequiredException

@TestMixin(GrailsUnitTestMixin)
@Mock([User, OAuthAccount, FitbitNotification])
@TestFor(FitBitDataService)
class FitBitDataServiceTests {

	GrailsMock oauthServiceMock

	void setUp() {
		defineBeans {
			urlService(UrlService)
		}
		User userInstance = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance.save()

		userInstance = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert userInstance.save()

		oauthServiceMock = mockFor(OauthService, true)

		assert new OAuthAccount([typeId: OAuthAccount.FITBIT_ID, userId: 1l, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"]).save()
	}

	void tearDown() {
	}

	void testAuthrorizeAccountIfNoToken() {
		shouldFail(AuthenticationRequiredException) {
			service.authorizeAccount(null, 1)
		}
		shouldFail(AuthenticationRequiredException) {
			service.authorizeAccount(new Token("", ""), 1)
		}
	}

	void testAuthorizeAccountWithToken() {
		Map mockedResponseFromSubscribe = [mockedKey: "mockedValue"]

		// Approach to mock other methods of a class under test which might be called internally during testing
		def mockedService = [
			subscribe: { account, userId ->
				return mockedResponseFromSubscribe
			}
		] as FitBitDataService

		Map response = mockedService.authorizeAccount(new Token("token-dummy-1", "dummy-secret"), 1)
		assert response == mockedResponseFromSubscribe
	}

	void testSubscribeIfSuccess() {
		oauthServiceMock.demand.postFitbitResource { token, url ->
			return new Response(new MockedHttpURLConnection())
		}
		service.oauthService = oauthServiceMock.createMock()
		Map result = service.subscribe(OAuthAccount.get(1), "dummy-id")
		assertTrue result.success
	}

	void testSubscribeIfFail() {
		oauthServiceMock.demand.postFitbitResource { token, url ->
			return new Response(new MockedHttpURLConnection(202))	// Invalid status code
		}
		service.oauthService = oauthServiceMock.createMock()
		Map result = service.subscribe(OAuthAccount.get(1), "dummy-id")
		assertFalse result.success
		assert OAuthAccount.count() == 0
	}

	void testUnSubscribeIfNoOAuthAccount() {
		Map response = service.unSubscribe(3)
		assert response.success == false
		assert response.message == "No subscription found"
	}

	void testUnSubscribeWithOAuthAccountExists() {
		oauthServiceMock.demand.deleteFitbitResource { token, url ->
			return new Response(new MockedHttpURLConnection(204))
		}
		service.oauthService = oauthServiceMock.createMock()

		assert OAuthAccount.count() == 1
		Map response = service.unSubscribe(1)
		assert OAuthAccount.count() == 0
	}

	void testQueueNotifications() {
		String notificationString = """[{"collectionType":"foods","date":"2010-03-01","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"foods","date":"2010-03-02","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"activities","date":"2010-03-01","ownerId":"184X36","ownerType":"user","subscriptionId":"2345"}]"""
		service.queueNotifications(JSON.parse(notificationString))
		assert FitbitNotification.count() == 3
	}

	void testPoll() {
		assert new FitbitNotification([collectionType: "foods", ownerId: "dummy-id", date: new Date(), ownerType: "user", subscriptionId: "1234"]).save()
		assert new FitbitNotification([collectionType: "activities", ownerId: "184X36", date: new Date(), ownerType: "user", subscriptionId: "2345"]).save()
		assert new FitbitNotification([collectionType: "foods", ownerId: "", date: new Date(), ownerType: "user", subscriptionId: "2345"]).save()
		assert FitbitNotification.count() == 3

		// If no accountId
		/*boolean result = service.poll(FitbitNotification.get(3))
		 assertFalse result*/

		// Normal execution
		boolean result = service.poll(FitbitNotification.get(1))
		assert service.lastPollTimestamps["dummy-id"]
		assertTrue result

		result = service.poll(FitbitNotification.get(2))
		assertTrue result
	}

	void testGetDataWithFailureStatusCode() {
		// When fitbit returns a non-zero response code
		oauthServiceMock.demand.getFitbitResource { token, url ->
			return new Response(new MockedHttpURLConnection(401))
		}
		service.oauthService = oauthServiceMock.createMock()
		shouldFail(IllegalArgumentException) {
			service.getData(OAuthAccount.get(1), "http://example.com", false)
		}
	}

	void testGetData() {
		String mockedResponseData = """{"someKey": "someValue"}"""
		oauthServiceMock.demand.getFitbitResource { token, url ->
			return new Response(new MockedHttpURLConnection(mockedResponseData))
		}
		service.oauthService = oauthServiceMock.createMock()
		boolean result = service.getData(OAuthAccount.get(1), "http://example.com", false)

		assertTrue result
	}

}

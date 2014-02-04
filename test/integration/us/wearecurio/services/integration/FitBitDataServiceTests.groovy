package us.wearecurio.services.integration

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Response
import org.scribe.model.Token
import grails.test.mixin.*

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.OAuthAccountService;
import us.wearecurio.services.UrlService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.utility.Utils

class FitBitDataServiceTests extends CuriousServiceTestCase {

	UrlService urlService
	FitBitDataService fitBitDataService
	OAuthAccount account

	void setUp() {
		super.setUp()
		
		account = OAuthAccount.createOrUpdate(OAuthAccount.FITBIT_ID, userId, "dummy-id", "dummy-id", "Dummy-secret")
		
		Utils.save(account, true)
	}

	void tearDown() {
	}

	void testAuthrorizeAccountIfNoToken() {
		shouldFail(AuthenticationRequiredException) {
			fitBitDataService.authorizeAccount(null, userId)
		}
		shouldFail(AuthenticationRequiredException) {
			fitBitDataService.authorizeAccount(new Token("", ""), userId)
		}
	}

	void testAuthorizeAccountWithToken() {
		fitBitDataService.oauthService = [
			postFitbitResource:{ token, url ->
				return new Response(new MockedHttpURLConnection())
			}
		]

		Map result = fitBitDataService.authorizeAccount(new Token("token-dummy-1", "dummy-secret"), userId)
		assert result.success
	}

	void testSubscribeIfSuccess() {
		fitBitDataService.oauthService = [
			postFitbitResource:{ token, url ->
				return new Response(new MockedHttpURLConnection())
			}
		]
		Map result = fitBitDataService.subscribe(account, "dummy-id")
		assert result.success
	}

	void testSubscribeIfFail() {
		fitBitDataService.oauthService = [
			postFitbitResource:{ token, url ->
				return new Response(new MockedHttpURLConnection(202))
			}
		]
		Map result = fitBitDataService.subscribe(account, "dummy-id")
		assert !result.success
		assert OAuthAccount.count() == 0
	}

	void testUnSubscribeIfNoOAuthAccount() {
		Map response = fitBitDataService.unSubscribe(3)
		assert response.success == false
		assert response.message == "No subscription found"
	}

	void testUnSubscribeWithOAuthAccountExists() {
		fitBitDataService.oauthService = [
			deleteFitbitResource: { token, url ->
				return new Response(new MockedHttpURLConnection(204))
			}
		]

		assert OAuthAccount.count() == 1
		Map response = fitBitDataService.unSubscribe(userId)
		assert OAuthAccount.count() == 0
	}

	void testQueueNotifications() {
		String notificationString = """[{"collectionType":"foods","date":"2010-03-01","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"foods","date":"2010-03-02","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"activities","date":"2010-03-01","ownerId":"184X36","ownerType":"user","subscriptionId":"2345"}]"""
		fitBitDataService.queueNotifications(JSON.parse(notificationString))
		assert ThirdPartyNotification.count() == 3
	}

	void testPoll() {
		def fitBit1 = new ThirdPartyNotification([collectionType: "foods", ownerId: "dummy-id", date: new Date(), ownerType: "user", subscriptionId: "1234"]).save()
		def fitBit2 = new ThirdPartyNotification([collectionType: "activities", ownerId: "184X36", date: new Date(), ownerType: "user", subscriptionId: "2345"]).save()
		assert new ThirdPartyNotification([collectionType: "foods", ownerId: "", date: new Date(), ownerType: "user", subscriptionId: "2345"]).save()
		assert ThirdPartyNotification.count() == 3

		// Normal execution
		boolean result = fitBitDataService.poll(fitBit1)
		assert fitBitDataService.lastPollTimestamps["dummy-id"]
		assert result

		result = fitBitDataService.poll(fitBit2)
		assert result
	}

	void testGetDataWithFailureStatusCode() {
		fitBitDataService.oauthService = [
			getFitbitResource: { token, url ->
				return new Response(new MockedHttpURLConnection(401))
			}
		]
		// When fitbit returns a non-zero response code
		shouldFail(IllegalArgumentException) {
			fitBitDataService.getData(account, "http://example.com", false)
		}
	}

	void testGetData() {
		String mockedResponseData = """{"someKey": "someValue"}"""
		fitBitDataService.oauthService = [
			getFitbitResource: { token, url ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		boolean result = fitBitDataService.getData(account, "http://example.com", false)

		assert result
	}

}

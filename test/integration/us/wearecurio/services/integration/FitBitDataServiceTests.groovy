package us.wearecurio.services.integration

import javassist.NotFoundException;
import grails.converters.JSON

import org.scribe.model.Response

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.User
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.UrlService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.AuthenticationRequiredException;
import us.wearecurio.utility.Utils

class FitBitDataServiceTests extends CuriousServiceTestCase {

	UrlService urlService
	FitBitDataService fitBitDataService
	OAuthAccount account
	User user2

	@Override
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		account = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"])

		Utils.save(account, true)

		fitBitDataService.securityService = [
			currentUser: user
		]
	}

	@Override
	void tearDown() {
	}

	void testSubscribeIfSuccess() {
		fitBitDataService.oauthService = [
			postfitbitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection("{}"))
			}
		]
		Map result = fitBitDataService.subscribe()
		assert result.success
	}

	void testSubscribeIfFail() {
		fitBitDataService.oauthService = [
			postfitbitResource:{ token, url, p, header ->
				return new Response(new MockedHttpURLConnection("{}", 202))
			}
		]
		Map result = fitBitDataService.subscribe()
		assert !result.success
		assert OAuthAccount.count() == 0
	}

	void testSubscribeIfNoAuthAccount() {
		fitBitDataService.securityService = [
			currentUser: user2
		]
		shouldFail(AuthenticationRequiredException) {
			fitBitDataService.subscribe()
		}
	}

	void testUnsubscribeIfNoOAuthAccount() {
		fitBitDataService.securityService = [
			currentUser: user2
		]
		Map result = fitBitDataService.unsubscribe()
		assert result.message == "No subscription found"
	}

	void testUnsubscribeWithOAuthAccountExists() {
		fitBitDataService.oauthService = [
			deletefitbitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection("{}", 204))
			}
		]

		assert OAuthAccount.count() == 1
		fitBitDataService.unsubscribe()
		assert OAuthAccount.count() == 0
	}

	void testNotificationHandler() {
		String notificationString = """[{"collectionType":"foods","date":"2010-03-01","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"foods","date":"2010-03-02","ownerId":"228S74","ownerType":"user","subscriptionId":"1234"},{"collectionType":"activities","date":"2010-03-01","ownerId":"184X36","ownerType":"user","subscriptionId":"2345"}]"""
		fitBitDataService.notificationHandler(notificationString)
		assert ThirdPartyNotification.count() == 3
		assert ThirdPartyNotification.first().typeId == ThirdParty.FITBIT
	}

	void testGetDataDefaultWithFailureStatusCode() {
		fitBitDataService.oauthService = [
			getfitbitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(401))
			}
		]
		// When fitbit returns a non-zero response code
		try {
			fitBitDataService.getDataDefault(account, new Date(), false)
		} catch(e) {
			assert e.cause instanceof AuthenticationRequiredException
		}
	}

	void testGetDataDefault() {
		String mockedResponseData = """{"someKey": "someValue"}"""
		fitBitDataService.oauthService = [
			getfitbitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		boolean result = fitBitDataService.getDataDefault(account, new Date(), false)

		assert result
	}

}
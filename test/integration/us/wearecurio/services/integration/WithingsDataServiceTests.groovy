package us.wearecurio.services.integration

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User

import grails.test.mixin.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.User
import us.wearecurio.services.UrlService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection

class WithingsDataServiceTests extends CuriousServiceTestCase {

	UrlService urlService
	WithingsDataService withingsDataService
	User user2
	OAuthAccount account

	def grailsApplication

	@Override
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		account = new OAuthAccount([typeId: ThirdParty.WITHINGS, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"])

		account.save()
	}

	@Override
	void tearDown() {
	}

	void testSubscribeIfSuccess() {
		withingsDataService.oauthService = [
			getwithingsResourceWithQuerystringParams: { token, url, p, headers ->
				return new Response(new MockedHttpURLConnection("""{"status": 0}"""))
			},
			getwithingsResource: { token, url, body, header ->
				return new Response(new MockedHttpURLConnection("""{"status":0,"body":{"updatetime":1385535542,"measuregrps":[]}}"""))
			}
		]

		withingsDataService.subscribe(userId)
		assert account.lastSubscribed
	}

	void testSubscribeIfFail() {
		withingsDataService.oauthService = [
			getwithingsResourceWithQuerystringParams: { token, url, p, headers ->
				return new Response(new MockedHttpURLConnection("""{"status": 2554}"""))
			}
		]

		withingsDataService.subscribe(userId)
		assert OAuthAccount.count() == 0
	}

	void testUnSubscribeIfNoOAuthAccount() {
		Map response = withingsDataService.unsubscribe(user2.id)	// Passing user's ID whose oauth account doesn't exist
		assert response.message == "No subscription found"
	}

	void testUnsubscribeWithOAuthAccountExists() {
		withingsDataService.oauthService = [
			getwithingsResourceWithQuerystringParams: { token, url, p, headers ->
				return new Response(new MockedHttpURLConnection("""{"status": 0}"""))
			}
		]

		assert OAuthAccount.count() == 1
		Map response = withingsDataService.unsubscribe(userId)
		assert OAuthAccount.count() == 0
	}

	void testGetDataDefaultWithFailureStatusCode() {
		// When withings returns a non-zero status code
		withingsDataService.oauthService = [
			getwithingsResource: { token, url, body, header ->
				return new Response(new MockedHttpURLConnection("""{"status": 2555}"""))
			}
		]

		boolean result = withingsDataService.getDataDefault(account, new Date(), false)

		assert Entry.count() == 0
	}

	void testGetDataDefaultWithUnparsableResponse() {
		// When un-parsable string returned
		withingsDataService.oauthService = [
			getwithingsResource: { token, url, body, header ->
				return new Response(new MockedHttpURLConnection("""status = unparsable-response"""))
			}
		]

		withingsDataService.getDataDefault(account, new Date(), false)
		assert Entry.count() == 0
	}

	void testGetDataDefault() {
		String mockedResponseData = """{"status":0,"body":{"updatetime":1385535542,"measuregrps":[{"grpid":162026288,"attrib":2,"date":1385386484,"category":1,"comment":"Hello","measures":[{"value":6500,"type":1,"unit":-2}]},{"grpid":162028086,"attrib":2,"date":1385300615,"category":1,"comment":"Nothing to comment","measures":[{"value":114,"type":9,"unit":0},{"value":113,"type":10,"unit":0},{"value":74,"type":11,"unit":0}]},{"grpid":129575271,"attrib":2,"date":1372931328,"category":1,"comment":"sa","measures":[{"value":170,"type":4,"unit":-2}]},{"grpid":129575311,"attrib":2,"date":1372844945,"category":1,"measures":[{"value":17700,"type":1,"unit":-2}]},{"grpid":129575122,"attrib":2,"date":1372844830,"category":1,"comment":"sa","measures":[{"value":6300,"type":1,"unit":-2}]},{"grpid":128995279,"attrib":0,"date":1372706279,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":138,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":128995003,"attrib":0,"date":1372706125,"category":1,"measures":[{"value":65861,"type":1,"unit":-3}]}]}}"""
		withingsDataService.oauthService = [
			getwithingsResource: { token, url, body, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		withingsDataService.getDataDefault(account, new Date(), false)
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
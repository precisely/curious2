package us.wearecurio.services.integration

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Response
import org.scribe.model.Token
import grails.test.mixin.*

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.FitbitNotification
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.OAuthAccountService;
import us.wearecurio.services.UrlService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.utility.Utils


class WithingsDataServiceTests extends CuriousServiceTestCase {

	UrlService urlService
	WithingsDataService withingsDataService
	User user2
	OAuthAccount account

	void setUp() {
		super.setUp()
		
		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		account = new OAuthAccount([typeId: OAuthAccount.WITHINGS_ID, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"])
		
		account.save()
	}

	void tearDown() {
	}

	void testAuthrorizeAccountIfNoToken() {
		shouldFail(AuthenticationRequiredException) {
			withingsDataService.authorizeAccount(null, userId, "dummy-id")
		}
		shouldFail(AuthenticationRequiredException) {
			withingsDataService.authorizeAccount(new Token("", ""), userId, "dummy-id")
		}
	}

	void testAuthorizeAccountWithToken() {
		// Approach to mock other methods of a class under test which might be called internally during testing
		def mockedService = [subscribe: { account -> [success: true] }, poll: { accountId -> return }] as WithingsDataService

		mockedService.authorizeAccount(new Token("token-dummy-1", "dummy-secret"), userId, "dummy-id-1")
		assert account.accessToken == "token-dummy-1"
		assert OAuthAccount.count() == 1

		mockedService.authorizeAccount(new Token("token-dummy-2", "dummy-secret"), user2.getId(), "dummy-id-2")
		assert OAuthAccount.count() == 2
	}

	void testSubscribeIfSuccess() {
		withingsDataService.oauthService = [
			getWithingsResource: { token, url ->
				return new Response(new MockedHttpURLConnection("""{"status": 0}"""))
			}
		]

		withingsDataService.subscribe(account)
		assert account.lastSubscribed
	}

	void testSubscribeIfFail() {
		withingsDataService.oauthService = [
			getWithingsResource: { token, url ->
				return new Response(new MockedHttpURLConnection("""{"status": 2554}"""))
			}
		]

		withingsDataService.subscribe(account)
		assert OAuthAccount.count() == 0
	}

	void testUnSubscribeIfNoOAuthAccount() {
		Map response = withingsDataService.unSubscribe(user2.getId())	// Passing user's ID whose oauth account doesn't exist 
		assert response.success == false
		assert response.message == "No subscription found"
	}

	void testUnsubscribeWithOAuthAccountExists() {
		withingsDataService.oauthService = [
			getWithingsResource: { token, url ->
				return new Response(new MockedHttpURLConnection("""{"status": 0}"""))
			}
		]

		assert OAuthAccount.count() == 1
		Map response = withingsDataService.unSubscribe(userId)
		assert OAuthAccount.count() == 0
	}

	void testPoll() {
		// When no accoundId is given
		boolean result = withingsDataService.poll(null)
		assert !result

		// Normal execution
		def mockedService = [getData: {account, refresh -> return }] as WithingsDataService
		result = mockedService.poll("dummy-id")
		assert mockedService.lastPollTimestamps["dummy-id"]
		assert result

		// Test Polling faster than 500ms
		result = mockedService.poll("dummy-id")
		assert !result
	}

	void testGetDataWithFailureStatusCode() {
		// When withings returns a non-zero status code
		withingsDataService.oauthService = [
			getWithingsResource: { token, url ->
				return new Response(new MockedHttpURLConnection("""{"status": 2555}"""))
			}
		]

		boolean result = withingsDataService.getData(account, false)

		assert !result
	}

	void testGetDataWithUnparsableResponse() {
		// When un-parsable string returned
		withingsDataService.oauthService = [
			getWithingsResource: { token, url ->
				return new Response(new MockedHttpURLConnection("""status = unparsable-response"""))
			}
		]

		//shouldFail(ConverterException) {}
		boolean result = withingsDataService.getData(account, false)
	}

	void testGetData() {
		String mockedResponseData = """{"status":0,"body":{"updatetime":1385535542,"measuregrps":[{"grpid":162026288,"attrib":2,"date":1385386484,"category":1,"comment":"Hello","measures":[{"value":6500,"type":1,"unit":-2}]},{"grpid":162028086,"attrib":2,"date":1385300615,"category":1,"comment":"Nothing to comment","measures":[{"value":114,"type":9,"unit":0},{"value":113,"type":10,"unit":0},{"value":74,"type":11,"unit":0}]},{"grpid":129575271,"attrib":2,"date":1372931328,"category":1,"comment":"sa","measures":[{"value":170,"type":4,"unit":-2}]},{"grpid":129575311,"attrib":2,"date":1372844945,"category":1,"measures":[{"value":17700,"type":1,"unit":-2}]},{"grpid":129575122,"attrib":2,"date":1372844830,"category":1,"comment":"sa","measures":[{"value":6300,"type":1,"unit":-2}]},{"grpid":128995279,"attrib":0,"date":1372706279,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":138,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":128995003,"attrib":0,"date":1372706125,"category":1,"measures":[{"value":65861,"type":1,"unit":-3}]}]}}"""
		withingsDataService.oauthService = [
			getWithingsResource: { token, url ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		withingsDataService.getData(account, false)
	}

}

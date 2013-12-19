package us.wearecurio.services

import grails.test.GrailsMock
import grails.test.mixin.*
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import groovy.mock.interceptor.MockFor

import org.scribe.model.Response
import org.scribe.model.Token

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.User

import us.wearecurio.services.UrlService;
import us.wearecurio.services.WithingsDataService;

import us.wearecurio.test.common.MockedHttpURLConnection;
import us.wearecurio.thirdparty.AuthenticationRequiredException

@TestMixin([ServiceUnitTestMixin, DomainClassUnitTestMixin])
@Mock([User, OAuthAccount, Entry, Tag, TagStats])
@TestFor(WithingsDataService)
class WithingsDataServiceTests {

	GrailsMock oauthServiceMock
	GrailsMock urlServiceMock

	void setUp() {
		User userInstance = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance.save()

		userInstance = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert userInstance.save()

		oauthServiceMock = mockFor(OauthService, true)
		urlServiceMock = mockFor(UrlService, true)

		assert new OAuthAccount([typeId: OAuthAccount.WITHINGS_ID, userId: 1l, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"]).save()

		urlServiceMock.demand.make { map, req, pubIP ->
			return "http://some-mocked-url.com"
		}
		urlServiceMock.demand.makeQueryString(5..10) { url, params ->
			return "http://some-mocked-url.com?someparams=somevalue"
		}
		service.urlService = urlServiceMock.createMock()
	}

	void tearDown() {
	}

	void testAuthrorizeAccountIfNoToken() {
		shouldFail(AuthenticationRequiredException) {
			service.authorizeAccount(null, 1, "dummy-id")
		}
		shouldFail(AuthenticationRequiredException) {
			service.authorizeAccount(new Token("", ""), 1, "dummy-id")
		}
	}

	void testAuthorizeAccountWithToken() {
		// Approach to mock other methods of a class under test which might be called internally during testing
		def mockedService = [subscribe: { account -> [success: true] }, poll: { accountId -> return }] as WithingsDataService

		mockedService.authorizeAccount(new Token("token-dummy-1", "dummy-secret"), 1, "dummy-id-1")
		assert OAuthAccount.get(1).accessToken == "token-dummy-1"
		assert OAuthAccount.count() == 1

		mockedService.authorizeAccount(new Token("token-dummy-2", "dummy-secret"), 2, "dummy-id-2")
		assert OAuthAccount.count() == 2
	}

	void testSubscribeIfSuccess() {
		oauthServiceMock.demand.getWithingsResource { token, url ->
			return new Response(new MockedHttpURLConnection("""{"status": 0}"""))
		}
		service.oauthService = oauthServiceMock.createMock()
		service.subscribe(OAuthAccount.get(1))
		assert OAuthAccount.get(1).lastSubscribed
	}

	void testSubscribeIfFail() {
		oauthServiceMock.demand.getWithingsResource { token, url ->
			return new Response(new MockedHttpURLConnection("""{"status": 2554}"""))
		}
		service.oauthService = oauthServiceMock.createMock()
		service.subscribe(OAuthAccount.get(1))
		assert OAuthAccount.count() == 0
	}

	void testUnSubscribeIfNoOAuthAccount() {
		Map response = service.unSubscribe(2)	// Passing user's ID whose oauth account doesn't exist 
		assert response.success == false
		assert response.message == "No subscription found"
	}

	void testUnsubscribeWithOAuthAccountExists() {
		oauthServiceMock.demand.getWithingsResource { token, url ->
			return new Response(new MockedHttpURLConnection("""{"status": 0}"""))
		}
		service.oauthService = oauthServiceMock.createMock()

		assert OAuthAccount.count() == 1
		Map response = service.unSubscribe(1)
		assert OAuthAccount.count() == 0
	}

	void testPoll() {
		// When no accoundId is given
		boolean result = service.poll(null)
		assertFalse result

		// Normal execution
		def mockedService = [getData: {account, refresh -> return }] as WithingsDataService
		result = mockedService.poll("dummy-id")
		assert mockedService.lastPollTimestamps["dummy-id"]
		assertTrue result

		// Test Polling faster than 500ms
		result = mockedService.poll("dummy-id")
		assertFalse result
	}

	void testGetDataWithFailureStatusCode() {
		// When withings returns a non-zero status code
		oauthServiceMock.demand.getWithingsResource { token, url ->
			return new Response(new MockedHttpURLConnection("""{"status": 2555}"""))
		}
		service.oauthService = oauthServiceMock.createMock()
		boolean result = service.getData(OAuthAccount.get(1), false)

		assertFalse result
	}

	void testGetDataWithUnparsableResponse() {
		// When un-parsable string returned
		oauthServiceMock.demand.getWithingsResource { token, url ->
			return new Response(new MockedHttpURLConnection("""status = unparsable-response"""))
		}
		service.oauthService = oauthServiceMock.createMock()

		//shouldFail(ConverterException) {}
		boolean result = service.getData(OAuthAccount.get(1), false)
	}

	void testGetData() {
		String mockedResponseData = """{"status":0,"body":{"updatetime":1385535542,"measuregrps":[{"grpid":162026288,"attrib":2,"date":1385386484,"category":1,"comment":"Hello","measures":[{"value":6500,"type":1,"unit":-2}]},{"grpid":162028086,"attrib":2,"date":1385300615,"category":1,"comment":"Nothing to comment","measures":[{"value":114,"type":9,"unit":0},{"value":113,"type":10,"unit":0},{"value":74,"type":11,"unit":0}]},{"grpid":129575271,"attrib":2,"date":1372931328,"category":1,"comment":"sa","measures":[{"value":170,"type":4,"unit":-2}]},{"grpid":129575311,"attrib":2,"date":1372844945,"category":1,"measures":[{"value":17700,"type":1,"unit":-2}]},{"grpid":129575122,"attrib":2,"date":1372844830,"category":1,"comment":"sa","measures":[{"value":6300,"type":1,"unit":-2}]},{"grpid":128995279,"attrib":0,"date":1372706279,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":138,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":128995003,"attrib":0,"date":1372706125,"category":1,"measures":[{"value":65861,"type":1,"unit":-3}]}]}}"""
		oauthServiceMock.demand.getWithingsResource { token, url ->
			return new Response(new MockedHttpURLConnection(mockedResponseData))
		}
		service.oauthService = oauthServiceMock.createMock()

		def entryMock = new MockFor(Entry)
		entryMock.demand.create(10..20) { p1, p2, p3, p4, p5, p6, p7, p8 ,p9 ->
			return null
		}

		// Using groovy's `use` closure because groovy's expando meta class mechanism is not working here in service.
		entryMock.use {
			service.getData(OAuthAccount.get(1), false)
		}
	}

}

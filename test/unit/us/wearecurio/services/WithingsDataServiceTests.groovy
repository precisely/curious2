package us.wearecurio.services

import grails.test.GrailsMock
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.scribe.model.Response

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User

@TestMixin(GrailsUnitTestMixin)
@Mock([User, OAuthAccount])
@TestFor(WithingsDataService)
class WithingsDataServiceTests {

	GrailsMock oauthServiceMock

	void setUp() {
		defineBeans {
			urlService(UrlService)
		}
		User userInstance = new User([username: "dummy", email: "dummy@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance.save()

		oauthServiceMock = mockFor(OauthService, true)
	}

	void tearDown() {
	}

	void testUnSubscribeIfNoOAuth() {
		Map response = service.unSubscribe(1)
		assert response.success == false
		assert response.message == "No subscription found"
	}

	void testUnsubscribeWithOAuthExists() {
		assert new OAuthAccount([typeId: OAuthAccount.WITHINGS_ID, userId: 1l, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"]).save()

		oauthServiceMock.demand.getWithingsResource { token, url ->
			return new Response(new MockedHttpURLConnection())
		}
		service.oauthService = oauthServiceMock.createMock()

		assert OAuthAccount.count() == 1
		Map response = service.unSubscribe(1)
		assert OAuthAccount.count() == 0
	}

}
/**
 * Need to do this, because grails `mockFor()` fails for classes which don't have default constructor.
 * So we can't mock org.scribe.model.Response in our code.
 * 
 * @see http://jira.grails.org/browse/GRAILS-10149
 */
class MockedHttpURLConnection extends HttpURLConnection {

	String responseString

	MockedHttpURLConnection(String responseString) {
		super(new URL("http://dummy-url.com"))
		this.responseString = responseString ?: """{"status": 0}"""
	}

	@Override
	void disconnect() {
	}

	@Override
	boolean usingProxy() {
		return false;
	}

	@Override
	void connect() throws IOException {
	}

	@Override
	int getResponseCode() {
		return 200
	}

	@Override
	InputStream getInputStream() {
		return new ByteArrayInputStream(responseString.bytes)
	}

}
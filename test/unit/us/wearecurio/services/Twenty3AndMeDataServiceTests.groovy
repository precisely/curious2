package us.wearecurio.services

import grails.test.mixin.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.test.common.MockedHttpURLConnection;
import us.wearecurio.thirdparty.AuthenticationRequiredException

@TestFor(Twenty3AndMeDataService)
@Mock([OAuthAccount, Twenty3AndMeData, User])
class Twenty3AndMeDataServiceTests {

	def oauthServiceMock

	void setUp() {
		User userInstance = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance.save()

		assert new OAuthAccount([typeId: OAuthAccount.TWENTY_3_AND_ME_ID, userId: 1l, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"]).save()

		oauthServiceMock = mockFor(OauthService)
	}

	void tearDown() {
	}

	void testGetUserProfiles() {
		String mockedResponseString = """{"profiles": []}"""
		oauthServiceMock.demand.getTwenty3AndMeResource { token, url ->
			return new Response(new MockedHttpURLConnection(mockedResponseString))
		}
		service.oauthService = oauthServiceMock.createMock()

		def response = service.getUserProfiles(new Token("dummy-token", "dummy-secret"))
		response == new JSONObject(mockedResponseString)
	}

	void testStoreGenomesDataWithNoToken() {
		shouldFail(AuthenticationRequiredException) {
			service.storeGenomesData(null, User.get(1))
		}
	}

	void testStoreGenomesData() { return
		def mockedService = [
			getGenomesDataForProfile: { token, profileId ->
				return "some-genome-data"
			},
			getUserProfiles: { token ->
				return new JSONObject([profiles: [[id: "xyz"], [id: "abc"]]])
			}
		] as Twenty3AndMeDataService
		mockedService.getGenomesDataForProfile(new Token("dummy-token", "dummy-object"), User.get(1))
		assert Twenty3AndMeData.count() == 2
	}

}
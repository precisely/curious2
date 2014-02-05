package us.wearecurio.services.integration

import static org.junit.Assert.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Token

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty;
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.model.TimeZoneId
import us.wearecurio.services.Twenty3AndMeDataService
import us.wearecurio.thirdparty.AuthenticationRequiredException

class Twenty3AndMeDataServiceTests extends CuriousServiceTestCase {

	OauthService oauthService
	Twenty3AndMeDataService twenty3AndMeDataService

	OAuthAccount account

	User user2

	@Before
	void setUp() {
		super.setUp()

		TimeZoneId.clearCacheForTesting()
		Twenty3AndMeData.list()*.delete()
		OAuthAccount.list()*.delete()
		Entry.list()*.delete()
		
		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		// This token may expire.
		account = new OAuthAccount([typeId: ThirdParty.TWENTY_THREE_AND_ME, userId: userId,
			accessToken: "d914a5723ed53e84c58fb376a4cca575", accessSecret: "", accountId: "06b53ee811bf5c9f"]).save()
		assert account.save()
	}

	@After
	void tearDown() {
		twenty3AndMeDataService.oauthService = oauthService
		Entry.list()*.delete()
	}

	void testGetUserProfiles() {
		Token tokenInstance = account.tokenInstance
		JSONObject response = twenty3AndMeDataService.getUserProfiles(tokenInstance)
		if (response.error) {
			assert response.error == "invalid_token"
		} else {
			assert response.last_name == "Hadeishi"
		}

		// Checking for invalid token
		tokenInstance = new Token("dummy-expired-token", "")
		response = twenty3AndMeDataService.getUserProfiles(tokenInstance)
		assert response.error == "invalid_token"
		assert response.profiles == null
	}

	void testStoreGenomesDataWithNoToken() {
		shouldFail(AuthenticationRequiredException) {
			twenty3AndMeDataService.storeGenomesData(null, user)
		}
	}

	void testStoreGenomesData() {
		twenty3AndMeDataService.storeGenomesData(account.tokenInstance, user)
		// Assuming current token returns genomes data large than 1MB
		assert Twenty3AndMeData.countByAccount(account) > 0
	}

}
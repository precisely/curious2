package us.wearecurio.services.integration

import static org.junit.Assert.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Token

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.services.Twenty3AndMeDataService
import us.wearecurio.thirdparty.AuthenticationRequiredException

class Twenty3AndMeDataServiceTests {

	OauthService oauthService
	Twenty3AndMeDataService twenty3AndMeDataService

	OAuthAccount account

	User userInstance1, userInstance2

	@Before
	void setUp() {
		Twenty3AndMeData.list()*.delete()
		OAuthAccount.list()*.delete()
		Entry.list()*.delete()

		userInstance1 = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance1.save()

		userInstance2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert userInstance2.save()

		account = new OAuthAccount([typeId: OAuthAccount.TWENTY_3_AND_ME_ID, userId: userInstance1.id,
			accessToken: "10f29af01b937e0c83a074b7388da1cc", accessSecret: "", accountId: "06b53ee811bf5c9f"]).save()
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
		assert response.last_name == "Hadeishi"

		// Checking for invalid token
		tokenInstance = new Token("dummy-expired-token", "")
		response = twenty3AndMeDataService.getUserProfiles(tokenInstance)
		assert response.error == "invalid_token"
		assert response.profiles == null
	}

	void testStoreGenomesDataWithNoToken() {
		try {
			twenty3AndMeDataService.storeGenomesData(null, userInstance1)
		} catch(e) {
			assert e.cause instanceof AuthenticationRequiredException
		}
	}

	void testStoreGenomesData() {
		twenty3AndMeDataService.storeGenomesData(account.tokenInstance, userInstance1)
		// Assuming current token returns genomes data large than 1MB
		assert Twenty3AndMeData.countByAccount(account) == 2
	}

}
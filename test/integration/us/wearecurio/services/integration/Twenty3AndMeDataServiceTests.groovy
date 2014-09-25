package us.wearecurio.services.integration

import static org.junit.Assert.*

import org.junit.*
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.utility.Utils

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.services.Twenty3AndMeDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException

class Twenty3AndMeDataServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	OauthService oauthService
	Twenty3AndMeDataService twenty3AndMeDataService

	OAuthAccount account

	User user2

	@Before
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert Utils.save(user2, true)

		// This token may expire.
		account = new OAuthAccount([typeId: ThirdParty.TWENTY_THREE_AND_ME, userId: userId,
			accessToken: "d914a5723ed53e84c58fb376a4cca575", accessSecret: "", accountId: "06b53ee811bf5c9f"])
		assert Utils.save(account, true)
	}

	@After
	void tearDown() {
		super.tearDown()
		
		twenty3AndMeDataService.oauthService = oauthService
	}

	@Test
	void testGetUserProfiles() {
		Token tokenInstance = account.tokenInstance
		try {
			twenty3AndMeDataService.getUserProfile(tokenInstance)
		} catch (e) {
			assert e.cause instanceof InvalidAccessTokenException
		}
	}

	@Test
	void testStoreGenomesDataWithNoToken() {
		shouldFail(InvalidAccessTokenException) {
			twenty3AndMeDataService.storeGenomesData(userId)
		}
	}

	@Test
	void testGetDataDefaultForValidData() {
		/**
		 * There are 5 activities in the mocked response, out of which four are valid & one is of type "trp" (invalid).
		 * Out of 4 valid entries, three of them having calories & others not. So according to code, 6 entries for each
		 * activity having calories & 5 entry for activity not having calories will be created. Total 23 entries will be created.
		 * 
		 * @see this in http://jsoneditoronline.org/index.html
		 */

		String profileResponse = """[{"profiles":[{"id":1}]}]"""
		byte[] bytes = new byte[1100 * 1024]
		for (int i = 0; i < bytes.length; ++i) {
			bytes[i] = 32
		}
		String genomeResponse = new String(bytes, "UTF-8")
		
		twenty3AndMeDataService.oauthService = [getTwenty3AndMeResource: {token, url, param = [:], header = [:]->
				return new Response(new MockedHttpURLConnection(url.startsWith(Twenty3AndMeDataService.GENOME_URL) ? genomeResponse : profileResponse))
			}]

		Map response = twenty3AndMeDataService.getDataDefault(account, new Date(), false)
		assert response.success == true
		
		assert Twenty3AndMeData.countByAccountAndProfileId(account, 1) == 2
	}
}
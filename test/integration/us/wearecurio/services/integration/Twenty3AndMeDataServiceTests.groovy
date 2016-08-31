package us.wearecurio.services.integration

import org.scribe.model.Response
import org.scribe.model.Token
import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.Twenty3AndMeData
import us.wearecurio.model.User
import us.wearecurio.services.DataService.DataRequestContext
import us.wearecurio.services.Twenty3AndMeDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.utility.Utils

class Twenty3AndMeDataServiceTests extends CuriousServiceTestCase {
	static transactional = true
	
	OauthService oauthService
	Twenty3AndMeDataService twenty3AndMeDataService

	OAuthAccount account

	User user2

	void setup() {

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user2, true)

		// This token may expire.
		account = new OAuthAccount([typeId: ThirdParty.TWENTY_THREE_AND_ME, userId: userId,
			accessToken: "d914a5723ed53e84c58fb376a4cca575", accessSecret: "", accountId: "06b53ee811bf5c9f"])
		assert Utils.save(account, true)
	}

	void cleanup() {
		twenty3AndMeDataService.oauthService = oauthService
	}

	void testGetUserProfiles() {
		given:
		Token tokenInstance = account.tokenInstance

		when:
		twenty3AndMeDataService.getUserProfile(tokenInstance)

		then:
		Exception exception = thrown(Exception)
		exception.cause.toString() == "us.wearecurio.thirdparty.InvalidAccessTokenException: Missing a valid access " +
				"token for [Twenty3AndMe]."
	}

	void testStoreGenomesDataWithNoToken() {
		when:
		twenty3AndMeDataService.storeGenomesData(userId)

		then:
		thrown(InvalidAccessTokenException)
	}

	void testGetDataDefaultForValidData() {
		/**
		 * There are 5 activities in the mocked response, out of which four are valid & one is of type "trp" (invalid).
		 * Out of 4 valid entries, three of them having calories & others not. So according to code, 6 entries for each
		 * activity having calories & 5 entry for activity not having calories will be created. Total 23 entries will be created.
		 * 
		 * @see this in http://jsoneditoronline.org/index.html
		 */

		given:
		String profileResponse = """[{"profiles":[{"id":1}]}]"""
		byte[] bytes = new byte[1100 * 1024]
		for (int i = 0; i < bytes.length; ++i) {
			bytes[i] = 32
		}
		String genomeResponse = new String(bytes, "UTF-8")
		
		twenty3AndMeDataService.oauthService = [getTwenty3AndMeResource: {token, url, param = [:], header = [:]->
				return new Response(new MockedHttpURLConnection(url.startsWith(Twenty3AndMeDataService.GENOME_URL) ? genomeResponse : profileResponse))
			}]

		when:
		Map response = twenty3AndMeDataService.getDataDefault(account, new Date(), null, false, new DataRequestContext())

		then:
		assert response.success == true
		
		assert Twenty3AndMeData.countByAccountAndProfileId(account, 1) == 2
	}
}

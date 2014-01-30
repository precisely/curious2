package us.wearecurio.services.integration

import static org.junit.Assert.*
import static us.wearecurio.model.OAuthAccount.*

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scribe.model.Response

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.HumanDataService
import us.wearecurio.test.common.MockedHttpURLConnection

class HumanDataServiceTests {

	OAuthAccount account1

	HumanDataService humanDataService
	OauthService oauthService
	User userInstance1

	@Before
	void setUp() {
		Entry.list()*.delete()
		/**
		 * Clearing up all existing OAuthAccount records, since grails loaded or
		 * table records from database in integration test. Twenty three and me type
		 * accounts is referred in Twenty3AndMeData domain, hence not deleting that.
		 */
		OAuthAccount.findAllByTypeIdNotEqual(TWENTY_3_AND_ME_ID)*.delete()

		userInstance1 = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance1.save()

		// Actual access token to test live data.
		String accessToken = "sii28Ai_Qed-lAoFi_6jHxJDxsU=OTR1oUDu45ee26a8c66ba0fcabef6b40f68cf1a1e0863e0976ec0ce93746152243a0e565ad439d3e084bd37201e76afd3bc1b0bc102391506bd55fbf49dc41a31a621f8310a91584f07b902de1b9703d531aca7a194d032f772982aa84e154c0fd223a49e9545a4348fc6eee29263804256d2f6a3eb9fa4ab6a24d64fedbeae5dd8f0959"
		// A dummy access token provided by HumanAPI
		accessToken = "demo"
		account1 = OAuthAccount.createOrUpdate(HUMAN_ID, userInstance1.id, "529edac229619a013a005434", accessToken, "")
	}

	@After
	void tearDown() {
	}

	void testPollForActivity() {
		humanDataService.oauthService = [getHumanResource: {token, url ->
				String data = "[]"
				if(url.contains("activities")) {
					data = """[{"id":"1","startTime":"2013-06-23T02:18:14.596Z","endTime":"2013-06-23T02:18:14.596Z","type":"walking","duration":696,"distance":756,"steps":977,"calories":0},{"id":"2","startTime":"2013-06-23T02:18:14.596Z","endTime":"2013-06-23T02:18:14.596Z","type":"cycling","duration":613,"distance":2605,"steps":0,"calories":0},{"id":"3","startTime":"2013-06-23T02:18:14.596Z","endTime":"2013-06-23T02:18:14.596Z","type":"walking","duration":696,"distance":756,"steps":977,"calories":0},{"id":"4","startTime":"2013-06-23T02:18:14.596Z","endTime":"2013-06-23T02:18:14.596Z","type":"cycling","duration":613,"distance":2605,"steps":0,"calories":0}]"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]
		humanDataService.poll(account1)

		assert Entry.count() == 14
	}

	void testPollForBloodGlucose() {
		humanDataService.oauthService = [getHumanResource: {token, url ->
				String data = "[]"
				if(url.contains("blood_glucose")) {
					data = """[{"id":"1","userId":"2","timestamp":"2013-06-21T02:19:14.596Z","value":121,"unit":"mg/dL","source":"withings"},{"id":"2","userId":"2","timestamp":"2013-06-23T02:18:14.596Z","value":74,"unit":"mg/dL","source":"withings"}]"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]
		humanDataService.poll(account1)

		assert Entry.count() == 2
	}

	void testPollForBodyFat() {
		humanDataService.oauthService = [getHumanResource: {token, url ->
				String data = "[]"
				if(url.contains("body_fat")) {
					data = """[{"id":"1","userId":"2","timestamp":"2011-06-21T02:19:14.596Z","value":23,"unit":"percent","source":"fitbit"},{"id":"2","userId":"2","timestamp":"2013-06-23T02:18:14.596Z","value":18.2,"unit":"percent","source":"fitbit"}]"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]
		humanDataService.poll(account1)

		assert Entry.count() == 2
	}

	void testPollForSleeps() {
		humanDataService.oauthService = [getHumanResource: {token, url ->
				String data = "[]"
				if(url.contains("sleeps")) {
					data = """[{"id":"1","userId":"2","day":"2013-06-22","startTime":"2013-06-23T02:21:14.919Z","endTime":"2013-06-23T08:21:14.919Z","timeAsleep":340,"timeAwake":20,"source":"withings"},{"id":"2","userId":"2","day":"2013-06-23","startTime":"2013-06-24T02:21:14.919Z","endTime":"2013-06-24T08:21:14.919Z","timeAsleep":320,"timeAwake":24,"source":"withings"}]"""
				}
				return new Response(new MockedHttpURLConnection(data))
			}]
		humanDataService.poll(account1)

		assert Entry.count() == 4
	}

	@Test
	void testPollWithAPIData() {
		return
		humanDataService.poll(account1)

		assert Entry.count() != 0
		assertNotNull account1.lastPolled
	}

}
package us.wearecurio.services.integration

import static org.junit.Assert.*
import static us.wearecurio.model.OAuthAccount.*

import org.junit.After
import org.junit.Before
import org.junit.Test

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.HumanDataService

class HumanDataServiceTests {

	OAuthAccount account1

	OauthService oauthService
	HumanDataService humanDataService
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

		String accessToken = "sii28Ai_Qed-lAoFi_6jHxJDxsU=OTR1oUDu45ee26a8c66ba0fcabef6b40f68cf1a1e0863e0976ec0ce93746152243a0e565ad439d3e084bd37201e76afd3bc1b0bc102391506bd55fbf49dc41a31a621f8310a91584f07b902de1b9703d531aca7a194d032f772982aa84e154c0fd223a49e9545a4348fc6eee29263804256d2f6a3eb9fa4ab6a24d64fedbeae5dd8f0959"
		accessToken = "demo"
		account1 = OAuthAccount.createOrUpdate(HUMAN_ID, userInstance1.id, "529edac229619a013a005434", accessToken, "")
	}

	@After
	void tearDown() {
	}

	@Test
	void testPoll() {
		humanDataService.poll(account1)

		assert Entry.count() != 0
	}

}
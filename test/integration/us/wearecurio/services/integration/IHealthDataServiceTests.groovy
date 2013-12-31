package us.wearecurio.services.integration

import static org.junit.Assert.*
import static us.wearecurio.model.OAuthAccount.*

import org.junit.*

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.IHealthDataService

class IHealthDataServiceTests {

	OAuthAccount account1

	IHealthDataService IHealthDataService

	User userInstance1

	@Before
	void setUp() {
		Entry.list()*.delete()
		OAuthAccount.findAllByTypeIdNotEqual(TWENTY_3_AND_ME_ID)*.delete()

		userInstance1 = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance1.save()

		String accessToken = ""
		account1 = OAuthAccount.createOrUpdate(IHEALTH_ID, userInstance1.id, "", accessToken, "")
	}

	@After
	void tearDown() {
	}

	@Test
	void testSomething() {
		IHealthDataService.doWithURL(account1, "", "")
	}

}
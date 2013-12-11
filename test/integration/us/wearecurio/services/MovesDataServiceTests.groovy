package us.wearecurio.services

import static org.junit.Assert.*

import org.junit.*
import org.scribe.model.Response

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.test.common.MockedHttpURLConnection

class MovesDataServiceTests {

	User userInstance1, userInstance2

	OauthService oauthService
	MovesDataService movesDataService

	@Before
	void setUp() {
		OAuthAccount.findAllByTypeId(OAuthAccount.MOVES_ID)*.delete()
		userInstance1 = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance1.save()

		userInstance2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert userInstance2.save()

		OAuthAccount account = new OAuthAccount([typeId: OAuthAccount.MOVES_ID, userId: userInstance1.id, accessToken: "hhpmGJV6FZUU_5GY_21Ny0rERs355_7Cwn923k8XMIf2iPD6wZ3ii1690u4VWwSx",
			accessSecret: "6b76f2ebd6e16b5bb5e1672d421241e4d9d1ce37122532f68b30dd735098", accountId: "65828076742279775"]).save()
		assert account.save()

		Entry.list()*.delete()
	}

	@After
	void tearDown() {
		movesDataService.oauthService = oauthService
		Entry.list()*.delete()
	}

	void testUnsubscribe() {
		// If no OAuthAccount Exists
		Map response = movesDataService.unSubscribe(userInstance2.id)	// Passing user id, whose OAuthAccount not exists.
		assertFalse response.success
		assert response.message == "No subscription found"

		assert OAuthAccount.countByTypeId(OAuthAccount.MOVES_ID) == 1

		// If OAuthAccount Exists
		response = movesDataService.unSubscribe(userInstance1.id)
		assertTrue response.success
		assert OAuthAccount.countByTypeId(OAuthAccount.MOVES_ID) == 0
	}

	void testPollIfNullAccount() {
		try {
			movesDataService.poll(null)
			fail "Must throw an exception."
		} catch(NullPointerException e) {
			assert true
		}
	}

	void testPollIfNoData() {
		movesDataService.oauthService = [getMovesResource: {token, url ->
				return new Response(new MockedHttpURLConnection("[]"))
			}]

		// Testing if no data received.
		Map response = movesDataService.poll(OAuthAccount.findByTypeId(OAuthAccount.MOVES_ID))
		assert Entry.count() == 0
	}

	void testPollForValidData() {
		/**
		 * There are 5 activities in the mocked response, out of which four are valid & one is of type "trp" (invalid).
		 * Out of 4 valid entries, three of them having calories & others not. So according to code, 6 entries for each
		 * activity having calories & 5 entry for activity not having calories will be created. Total 23 entries will be created.
		 * 
		 * @see this in http://jsoneditoronline.org/index.html
		 */

		String parsedResponse = """[{"date":"20121212","segments":[{"type":"move","startTime":"20121212T071430Z","endTime":"20121212T074617Z","activities":[{"activity":"run","startTime":"20121212T071430Z","endTime":"20121212T072732Z","duration":782,"distance":1251,"steps":1353}]},{"type":"place","startTime":"20121212T074617Z","endTime":"20121212T100051Z","activities":[{"activity":"wlk","startTime":"20121212T074804Z","endTime":"20121212T075234Z","duration":270,"distance":227,"steps":303,"calories":99}]},{"type":"move","startTime":"20121212T100051Z","endTime":"20121212T100715Z","activities":[{"activity":"wlk","startTime":"20121212T100051Z","endTime":"20121212T100715Z","duration":384,"distance":421,"steps":488,"calories":99}]},{"type":"move","startTime":"20121212T153638Z","endTime":"20121212T160744Z","activities":[{"activity":"trp","startTime":"20121212T153638Z","endTime":"20121212T155321Z","duration":1003,"distance":8058},{"activity":"cyc","startTime":"20121212T155322Z","endTime":"20121212T160744Z","duration":862,"distance":1086,"steps":1257,"calories":99}]}]}]"""

		movesDataService.oauthService = [getMovesResource: {token, url ->
				return new Response(new MockedHttpURLConnection(parsedResponse))
			}]

		Map response = movesDataService.poll(OAuthAccount.findByTypeId(OAuthAccount.MOVES_ID))
		assert response.success == true
		assert Entry.count() == 23

		// Ensuring entries of the same day will be replaced with new entries.
		response = movesDataService.poll(OAuthAccount.findByTypeId(OAuthAccount.MOVES_ID))
		assert response.success == true
		assert Entry.count() == 23

	}

	void testPollIfNullDataInSegments() {
		String parsedResponse = """[{"date":"20121213","segments":null,"caloriesIdle":1785}]"""

		movesDataService.oauthService = [getMovesResource: {token, url ->
				return new Response(new MockedHttpURLConnection(parsedResponse))
			}]

		Map response = movesDataService.poll(OAuthAccount.findByTypeId(OAuthAccount.MOVES_ID))
		assert response.success == true
		assert Entry.count() == 0
	}

	void testPollWithMovesAPIDirectly() {
		Map response = movesDataService.poll(OAuthAccount.findByTypeId(OAuthAccount.MOVES_ID))
		assert response.success == true
	}

	void testPollWithMovesAPIForAuthFail() {
		// Testing with mock
		movesDataService.oauthService = [getMovesResource: {token, url ->
				return new Response(new MockedHttpURLConnection("expired_access_token", 201))
			}]

		Map response = movesDataService.poll(OAuthAccount.findByTypeId(OAuthAccount.MOVES_ID))
		assert response.success == false

		// Testing live after token expires
		OAuthAccount account = OAuthAccount.findByTypeId(OAuthAccount.MOVES_ID)
		account.accessToken = "expired-token"
		account.save()

		movesDataService.oauthService = oauthService
		response = movesDataService.poll(account)
		assert response.success == false
	}

}
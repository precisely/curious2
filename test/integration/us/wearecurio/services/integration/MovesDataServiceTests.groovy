package us.wearecurio.services.integration

import static org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.scribe.model.Response

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.MovesDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException;

class MovesDataServiceTests extends CuriousServiceTestCase {

	User user2
	OAuthAccount account

	OauthService oauthService
	MovesDataService movesDataService

	@Before
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		account = new OAuthAccount([typeId: ThirdParty.MOVES, userId: userId, accessToken: "Z14DRUTWswu66GuptWqQR1b295DikZY77Bfwocqaduku9VKI2t0WTuOJQ7F72DSQ",
			accessSecret: "6b76f2ebd6e16b5bb5e1672d421241e4d9d1ce37122532f68b30dd735098", accountId: "65828076742279775", timeZoneId: TimeZoneId.look("America/Los_Angeles").id])
		assert account.save()
	}

	@After
	void tearDown() {
		movesDataService.oauthService = oauthService
		Entry.list()*.delete()
	}

	void testUnsubscribe() {
		// If no OAuthAccount Exists
		shouldFail(MissingOAuthAccountException) {
			Map response = movesDataService.unsubscribe(user2.id)	// Passing user id, whose OAuthAccount not exists.
		}
		assert OAuthAccount.countByTypeId(ThirdParty.MOVES) == 1

		// If OAuthAccount Exists
		Map response = movesDataService.unsubscribe(userId)
		assertTrue response.success
		assert OAuthAccount.countByTypeId(ThirdParty.MOVES) == 0
	}

	void testGetDataDefaultForValidData() {
		/**
		 * There are 5 activities in the mocked response, out of which four are valid & one is of type "trp" (invalid).
		 * Out of 4 valid entries, three of them having calories & others not. So according to code, 6 entries for each
		 * activity having calories & 5 entry for activity not having calories will be created. Total 23 entries will be created.
		 * 
		 * @see this in http://jsoneditoronline.org/index.html
		 */

		String parsedResponse = """[{"date":"20121212","segments":[{"type":"move","startTime":"20121212T071430Z","endTime":"20121212T074617Z","activities":[{"activity":"run","startTime":"20121212T071430Z","endTime":"20121212T072732Z","duration":782,"distance":1251,"steps":1353}]},{"type":"place","startTime":"20121212T074617Z","endTime":"20121212T100051Z","activities":[{"activity":"wlk","startTime":"20121212T074804Z","endTime":"20121212T075234Z","duration":270,"distance":227,"steps":303,"calories":99}]},{"type":"move","startTime":"20121212T100051Z","endTime":"20121212T100715Z","activities":[{"activity":"wlk","startTime":"20121212T100051Z","endTime":"20121212T100715Z","duration":384,"distance":421,"steps":488,"calories":99}]},{"type":"move","startTime":"20121212T153638Z","endTime":"20121212T160744Z","activities":[{"activity":"trp","startTime":"20121212T153638Z","endTime":"20121212T155321Z","duration":1003,"distance":8058},{"activity":"cyc","startTime":"20121212T155322Z","endTime":"20121212T160744Z","duration":862,"distance":1086,"steps":1257,"calories":99}]}]}]"""

		movesDataService.oauthService = [getMovesResource: {token, url, param, header ->
				return new Response(new MockedHttpURLConnection(parsedResponse))
			}]

		Map response = movesDataService.getDataDefault(account, null, false)
		assert response.success == true
		def entries = Entry.findAllByUserId(user.getId())
		for (def entry in entries) {
			if (entry.getAmount().intValue() == 1353) {
				assert entry.toString().endsWith("datePrecisionSecs:180, timeZoneName:America/Los_Angeles, description:run, amount:1353.000000000, units:steps, amountPrecision:3, comment:(Moves), repeatType:null, repeatEnd:null)")
			}
		}
		assert Entry.count() == 23

		// Ensuring entries of the same day will be replaced with new entries.
		response = movesDataService.getDataDefault(account, null, false)
		assert response.success == true
	}

	void testPollIfNullDataInSegments() {
		String parsedResponse = """[{"date":"20121213","segments":null,"caloriesIdle":1785}]"""

		movesDataService.oauthService = [getMovesResource: {token, url, params, header ->
				return new Response(new MockedHttpURLConnection(parsedResponse))
			}]

		Map response = movesDataService.getDataDefault(account, null, false)
		assert response.success == true
		assert Entry.count() == 0
	}

	void testPollWithMovesAPIDirectly() {
		Map response = movesDataService.getDataDefault(account, null, false)
		assert response.success == true
	}

	void testPollWithMovesAPIForAuthFail() {
		// Testing with mock
		movesDataService.oauthService = [getMovesResource: {token, url, p, header ->
				return new Response(new MockedHttpURLConnection("""{"error": "expired_access_token"}""", 201))
			}]

		Map response = movesDataService.getDataDefault(account, null, false)
		assert response.success == false

		// Testing live after token expires
		account.accessToken = "expired-token"
		account.save()

		movesDataService.oauthService = oauthService
		try {
			movesDataService.getDataDefault(account, null, false)
		} catch (e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

}
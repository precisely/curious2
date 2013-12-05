package us.wearecurio.controller

import grails.test.GrailsMock
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.codehaus.groovy.grails.web.json.JSONObject
import org.junit.*
import org.scribe.model.Token

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.services.FitBitDataService;
import us.wearecurio.services.MovesDataService;
import us.wearecurio.services.Twenty3AndMeDataService;

@TestMixin(GrailsUnitTestMixin)
@TestFor(AuthenticationController)
@Mock([User, OAuthAccount])
class AuthenticationControllerTests {

	GrailsMock fitbitDataServiceMock
	GrailsMock movesDataServiceMock
	GrailsMock oauthServiceMock
	GrailsMock twenty3AndMeDataServiceMock

	void setUp() {
		User userInstance = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance.save()

		userInstance = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert userInstance.save()

		fitbitDataServiceMock = mockFor(FitBitDataService, true)
		movesDataServiceMock = mockFor(MovesDataService, true)
		oauthServiceMock = mockFor(OauthService, true)
		twenty3AndMeDataServiceMock = mockFor(Twenty3AndMeDataService, true)

		controller.metaClass.toUrl { map ->
			def url = map.controller + '/' + map.action
			if (map.params) {
				url += "?" + map.params.collect { key, value -> "$key=$value" }.join("&")
			}
			return url
		}
	}

	void tearDown() {
		// Tear down logic here
	}

	// Testing before interceptor method for failure case
	void testAuthRedirect() {
		params.provider = "someProvider"	// example: fitbit, moves
		oauthServiceMock.demand.findSessionKeyForAccessToken(4) { provider -> return provider }
		controller.oauthService = oauthServiceMock.createMock()

		params.status = "fail"
		controller.authRedirect()
		assert response.redirectedUrl == "home/login"	// If auth denied or failed & no denied URI set

		response.reset()

		params.status = "success"
		controller.metaClass.tokenInstance = null
		controller.oauthService = oauthServiceMock.createMock()
		controller.authRedirect()

		assert response.redirectedUrl == "home/login"	// If authentication success but token not generated

		response.reset()

		controller.session.returnURIWithToken = "home/registerDummyAPI"
		controller.authRedirect()

		response.redirectUrl == "home/registerDummyAPI"	// If authentication failed due to any case & session uri present

		response.reset()

		params.status = "success"
		controller.metaClass.tokenInstance =  new Token("", "")
		controller.oauthService = oauthServiceMock.createMock()
		controller.authRedirect()
	}

	void testMovesAuthWithNoLoggedInUser() {
		controller.metaClass.sessionUser {
			return null
		}
		controller.movesAuth()	// No logged in user & no redirect url
		assert response.redirectedUrl == null

		response.reset()

		controller.session.returnURIWithToken = "home/some-url"
		controller.movesAuth()	// Redirect url set & no logged in user
		assert response.redirectedUrl == "home/some-url"
		assert !controller.session.returnURIWithToken
	}

	void testMovesAuthWithLoggedInUser() {
		params.provider = "moves"
		controller.session["moves"] =  new Token("some-token", "some-secret", "")

		oauthServiceMock.demand.findSessionKeyForAccessToken(2) { provider -> return provider }
		controller.oauthService = oauthServiceMock.createMock()

		controller.metaClass.sessionUser {
			return User.get(1)
		}
		controller.authRedirect()
		controller.movesAuth()	// No raw response for moves authenticated user
		assert OAuthAccount.count() == 0

		response.reset()

		controller.session["moves"] =  new Token("some-token", "some-secret", """{"user_id": 271314}""")
		controller.authRedirect()
		controller.movesAuth()	// Available raw response for moves authenticated user in token
		assert OAuthAccount.count() == 1
		assert OAuthAccount.get(1).typeId == OAuthAccount.MOVES_ID
	}

	void testFitbitAuthIfNoLoggedInUser() {
		controller.metaClass.sessionUser {
			return null
		}
		controller.fitbitAuth()	// No logged in user & no redirect url
		assert response.redirectedUrl == null

		response.reset()

		controller.session.returnURIWithToken = "home/some-url"
		controller.fitbitAuth()	// Redirect url set & no logged in user
		assert response.redirectedUrl == "home/some-url"
		assert !controller.session.returnURIWithToken
	}

	void testFitbitAuthAuthForIfLoggedInUser() {
		String userProfileResponse = "{}"
		fitbitDataServiceMock.demand.getUserInfo(2) { token ->
			return new JSONObject(userProfileResponse)
		}
		controller.fitBitDataService = fitbitDataServiceMock.createMock()

		params.provider = "fitbit"
		controller.session["fitbit"] =  new Token("some-token", "some-secret")

		oauthServiceMock.demand.findSessionKeyForAccessToken(2) { provider -> return provider }
		controller.oauthService = oauthServiceMock.createMock()

		controller.metaClass.sessionUser {
			return User.get(1)
		}
		controller.authRedirect()
		controller.fitbitAuth()	// No there is no information in user profile info
		assert OAuthAccount.count() == 0

		response.reset()

		userProfileResponse = """{"user": {"encodedId": "dummy-id"}}"""
		controller.authRedirect()
		controller.fitbitAuth()
		assert OAuthAccount.count() == 1
		assert OAuthAccount.get(1).typeId == OAuthAccount.FITBIT_ID
	}

	void testTwenty3AndMeAuthIfNoLoggedInUser() {
		controller.metaClass.sessionUser {
			return null
		}
		controller.twenty3andmeAuth()	// No logged in user & no redirect url
		assert response.redirectedUrl == null

		response.reset()

		controller.session.returnURIWithToken = "home/some-url"
		controller.twenty3andmeAuth()	// Redirect url set & no logged in user
		assert response.redirectedUrl == "home/some-url"
		assert !controller.session.returnURIWithToken
	}

	void testTwenty3AndMeAuthForIfLoggedInUser() {
		String userProfileResponse = "{}"
		twenty3AndMeDataServiceMock.demand.getUserProfiles(2) { token ->
			return new JSONObject(userProfileResponse)
		}
		controller.twenty3AndMeDataService = twenty3AndMeDataServiceMock.createMock()

		params.provider = "twenty3andme"
		controller.session["twenty3andme"] =  new Token("some-token", "some-secret")

		oauthServiceMock.demand.findSessionKeyForAccessToken(2) { provider -> return provider }
		controller.oauthService = oauthServiceMock.createMock()

		controller.metaClass.sessionUser {
			return User.get(1)
		}
		controller.authRedirect()
		controller.twenty3andmeAuth()	// No there is no information in user profile info
		assert OAuthAccount.count() == 0

		response.reset()

		userProfileResponse = """{"id": "dummy-id"}"""
		controller.authRedirect()
		controller.twenty3andmeAuth()
		assert OAuthAccount.count() == 1
		assert OAuthAccount.get(1).typeId == OAuthAccount.TWENTY_3_AND_ME_ID
	}

}
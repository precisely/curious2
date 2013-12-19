package us.wearecurio.services

import grails.test.GrailsMock
import grails.test.mixin.*
import grails.test.mixin.support.*
import groovy.mock.interceptor.MockFor

import org.junit.*
import org.scribe.model.Response

import uk.co.desirableobjects.oauth.scribe.OauthService
import us.wearecurio.model.Entry
import us.wearecurio.model.FitbitNotification
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.User
import us.wearecurio.test.common.MockedHttpURLConnection;
import us.wearecurio.thirdparty.moves.MovesTagUnitMap

@TestMixin(GrailsUnitTestMixin)
@Mock([User, OAuthAccount, Entry, Tag, TagStats])
@TestFor(MovesDataService)
class MovesDataServiceTests {

	GrailsMock oauthServiceMock

	void setUp() {
		User userInstance = new User([username: "dummy1", email: "dummy1@curious.test", sex: "M", first: "John", last: "Day",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: false])
		assert userInstance.save()

		userInstance = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert userInstance.save()

		assert new OAuthAccount([typeId: OAuthAccount.MOVES_ID, userId: 1l, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"]).save()

		oauthServiceMock = mockFor(OauthService, true)

	}

	void tearDown() {
	}

	void testUnsubscribe() {
		// If no OAuthAccount Exists
		Map response = service.unSubscribe(2l)	// Passing user id, whose OAuthAccount not exists.
		assertFalse response.success
		assert response.message == "No subscription found"

		assert OAuthAccount.count() == 1

		// If OAuthAccount Exists
		response = service.unSubscribe(1l)
		assertTrue response.success
		assert OAuthAccount.count() == 0
	}

	void testPollIfNoData() {
		oauthServiceMock.demand.getMovesResource { token, url ->
			return new Response(new MockedHttpURLConnection("[]"))
		}
		service.oauthService = oauthServiceMock.createMock()

		// Testing if no data received.
		Map response = service.poll(OAuthAccount.get(1))
		assert Entry.count() == 0
	}

	void testPollForValidData() {
		String parsedResponse = """[{"date":"20121212","segments":[{"type":"move","startTime":"20121212T071430Z","endTime":"20121212T074617Z","activities":[{"activity":"wlk","startTime":"20121212T071430Z","endTime":"20121212T072732Z","duration":782,"distance":1251,"steps":1353,"calories":99},{"activity":"trp","startTime":"20121212T072732Z","endTime":"20121212T074616Z","duration":1124,"distance":8443}]},{"type":"place","startTime":"20121212T074617Z","endTime":"20121212T100051Z","activities":[{"activity":"wlk","startTime":"20121212T074804Z","endTime":"20121212T075234Z","duration":270,"distance":227,"steps":303,"calories":99}]},{"type":"move","startTime":"20121212T100051Z","endTime":"20121212T100715Z","activities":[{"activity":"wlk","startTime":"20121212T100051Z","endTime":"20121212T100715Z","duration":384,"distance":421,"steps":488,"calories":99}]},{"type":"place","startTime":"20121212T100715Z","endTime":"20121212T110530Z","activities":[{"activity":"wlk","startTime":"20121212T101215Z","endTime":"20121212T101255Z","duration":40,"distance":18,"steps":37,"calories":99}]},{"type":"move","startTime":"20121212T110530Z","endTime":"20121212T111129Z","activities":[{"activity":"wlk","startTime":"20121212T110530Z","endTime":"20121212T111128Z","duration":358,"distance":493,"steps":441,"calories":99}]},{"type":"place","startTime":"20121212T111129Z","endTime":"20121212T153638Z","activities":[{"activity":"wlk","startTime":"20121212T111233Z","endTime":"20121212T112203Z","duration":570,"distance":565,"steps":809}]},{"type":"move","startTime":"20121212T153638Z","endTime":"20121212T160744Z","activities":[{"activity":"trp","startTime":"20121212T153638Z","endTime":"20121212T155321Z","duration":1003,"distance":8058},{"activity":"wlk","startTime":"20121212T155322Z","endTime":"20121212T160744Z","duration":862,"distance":1086,"steps":1257,"calories":99}]}],"caloriesIdle":1785},{"date":"20121213","segments":null,"caloriesIdle":1785}]"""

		oauthServiceMock.demand.getMovesResource { token, url ->
			return new Response(new MockedHttpURLConnection(parsedResponse, 200))
		}
		service.oauthService = oauthServiceMock.createMock()

		def tagUnitMapMock = new MockFor(MovesTagUnitMap, true)
		def tagUnitMapConstructorProxy = tagUnitMapMock.proxyInstance()

		tagUnitMapMock.demand.with {
			MovesTagUnitMap() {
				tagUnitMapConstructorProxy
			}
			buildEntry(30..40) { tag, amount, userId, date = new Date(), args = [:] ->
				return null
			}
		}
		tagUnitMapMock.use {
			Map response = service.poll(OAuthAccount.get(1))
			assert response.success == true
		}

	}

}
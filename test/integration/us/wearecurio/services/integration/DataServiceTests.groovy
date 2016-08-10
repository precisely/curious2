package us.wearecurio.services.integration

import spock.lang.Unroll
import us.wearecurio.datetime.DateUtils
import grails.converters.JSON
import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.services.DataService
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.OuraDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.utility.Utils

import java.util.regex.Matcher
import java.util.regex.Pattern


class DataServiceTests extends CuriousServiceTestCase {
	static transactional = true

	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	OuraDataService ouraDataService
	OAuthAccount account
	OAuthAccount account2

	def grailsApplication

	void setup() {
		account = new OAuthAccount([typeId: ThirdParty.WITHINGS, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)

		account2 = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account2, true)
	}

	void cleanup() {
		account.delete()
		account2.delete()
	}

	@Unroll("When notification Date is: #notificationDate then startDate should be: #responseStartDate and endDate should be #responseEndDate")
	@Test
	void testPoll() {
		given: "Mocked service method and OAuthAccount instance"

		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 3,
				type: "sleep", eventTime: 1434440700, data: {bedtime_m: 510, sleep_score: 86,
				awake_m: 52, rem_m: 78, light_m: 220, deep_m: 160}},
				{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 3,
				type: "sleep", eventTime: 1424440700, data: {bedtime_m: 430, sleep_score: 76, awake_m: 42,
				 rem_m: 68, light_m: 320, deep_m: 2.60}}]}"""

		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					String stringWithTimeStamp = url.split("startTimestamp=")[1];
					Pattern pattern = Pattern.compile("\\d+");
					Matcher m = pattern.matcher(stringWithTimeStamp)
					List timeStamps = m.findAll()

					assert !timeStamps[0].compareTo(responseStartDate)
					assert !timeStamps[1].compareTo(responseEndDate)
					if (url.contains("sleep")) {
						return new Response(new MockedHttpURLConnection(mockedResponseData))
					} else {
						return new Response(new MockedHttpURLConnection("{data: []}"))
					}
				}
		]

		when: "Notification date is passed in arguments result should be as expected"
		Boolean result = ouraDataService.poll(account, notificationDate)

		then: "response should be true"
		result

		where:
		notificationDate			||responseStartDate						|responseEndDate
		DateUtils.getStartOfTheDay() - 65	||"${(long)((DateUtils.getStartOfTheDay() - 65).getTime() / 1000)}"|"${(long)(DateUtils.getEndOfTheDay(new Date() - 65).getTime() / 1000)}"
		DateUtils.getStartOfTheDay() - 15	||"${(long)((DateUtils.getStartOfTheDay() - 62).getTime() / 1000)}"|"${(long)(DateUtils.getEndOfTheDay().getTime() / 1000)}"
		DateUtils.getStartOfTheDay() - 5	||"${(long)((DateUtils.getStartOfTheDay() - 62).getTime() / 1000)}"|"${(long)(DateUtils.getEndOfTheDay().getTime() / 1000)}"
	}

	@Test
	void testNotifications() {
		String mockNotificationData = [type: "sleep",
			date: "2016-01-01", userId: userId] as JSON

		ouraDataService.notificationHandler(mockNotificationData)
		ouraDataService.notificationProcessor()
		
		// TODO: Finish this up
	}

	@Test
	void testExpiredToken() {
		// Testing expired token for fitbit.
		fitBitDataService.oauthService = [
			getFitBitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(401))
			}
		]

		try {
			fitBitDataService.getUserProfile(account)
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException
			assert e.cause.provider.toLowerCase() == "fitbit"
		}
	}

	@Test
	void testExpiredTokenWithAPI() {
		try {
			// Testing directly with API.
			fitBitDataService.getUserProfile(account)
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException || e instanceof groovy.lang.MissingMethodException
		}

		account.accessToken = ""
		account.accessSecret = ""
		Utils.save(account, true)	// Will mimic new Token("", "")

		try {
			// Testing directly with API with no token.
			fitBitDataService.getUserProfile(account)
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException
		}
	}

	@Test
	void testPollForUser() {
		boolean polledFitBit = false

		String mockedResponseData = """{"someKey": "someValue"}"""
		fitBitDataService.oauthService = [
			getFitBitResource: { token, url, p, header ->
				polledFitBit = true
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		boolean polledWithings = false

		String mockedResponseData2 = """{"status":0,"body":{"updatetime":1385535542,"measuregrps":[{"grpid":162026288,"attrib":2,"date":1385386484,"category":1,"comment":"Hello","measures":[{"value":6500,"type":1,"unit":-2}]},{"grpid":162028086,"attrib":2,"date":1385300615,"category":1,"comment":"Nothing to comment","measures":[{"value":114,"type":9,"unit":0},{"value":113,"type":10,"unit":0},{"value":74,"type":11,"unit":0}]},{"grpid":129575271,"attrib":2,"date":1372931328,"category":1,"comment":"sa","measures":[{"value":170,"type":4,"unit":-2}]},{"grpid":129575311,"attrib":2,"date":1372844945,"category":1,"measures":[{"value":17700,"type":1,"unit":-2}]},{"grpid":129575122,"attrib":2,"date":1372844830,"category":1,"comment":"sa","measures":[{"value":6300,"type":1,"unit":-2}]},{"grpid":128995279,"attrib":0,"date":1372706279,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":138,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":128995003,"attrib":0,"date":1372706125,"category":1,"measures":[{"value":65861,"type":1,"unit":-3}]}]}}"""
		withingsDataService.oauthService = [
			getWithingsResource: { token, url, body, header ->
				polledWithings = true
				return new Response(new MockedHttpURLConnection(mockedResponseData2))
			},
			getWithingsResourceWithQuerystringParams: { token, url, body, header ->
				String mockedActivityResponse = """{"status":0,"body":{"activities":[]}}"""
				log.debug("xxxxxx " + body.dump())
				if (body['action']?.contains("getintradayactivity"))
					mockedActivityResponse = """{"status":0,"body":{"series":{"1368141046":{"calories":0,"duration":120},"1368141657":{"calories":0.87,"duration":60,"steps":18,"elevation":0.03,"distance":3218.69},"1368141717":{"calories":1.2,"duration":60,"steps":56,"elevation":2.4,"distance":1000}}}}"""	

				return new Response(new MockedHttpURLConnection(mockedActivityResponse))
			}
		]

		DataService.pollAllForUserId(userId)

		assert Entry.count() > 0
		assert polledFitBit && polledWithings
	}
/**/
}

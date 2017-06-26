package us.wearecurio.services.integration

import grails.converters.JSON
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.junit.Test
import org.scribe.model.Response
import us.wearecurio.datetime.DateUtils
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.services.DataService
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.LegacyOuraDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.utility.Utils

class DataServiceTests extends CuriousServiceTestCase {
	static transactional = true

	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	LegacyOuraDataService legacyOuraDataService
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

	void "test Poll method when notification date is passed"() {
		given: "Mocked service method and OAuthAccount instance"
		String startDateTimeStamp
		String endDateTimeStamp

		legacyOuraDataService.oauthService = [getOuraLegacyResource: { token, url, p, header ->
			String stringWithTimeStamp = url.split("startTimestamp=")[1];
			Pattern pattern = Pattern.compile("\\d+");
			Matcher m = pattern.matcher(stringWithTimeStamp)
			List timeStamps = m.findAll()

			startDateTimeStamp = timeStamps[0]
			endDateTimeStamp = timeStamps[1]

			return new Response(new MockedHttpURLConnection("{data: []}"))
		} ]

		when: "Notification date less than OAuthAccounts's lastData date is passed"
		Date notificationDate = DateUtils.getStartOfTheDay(new Date() - 65)
		legacyOuraDataService.poll(account, notificationDate)

		then: "Start and End timestamp should match according to notification date"
		startDateTimeStamp == Long.toString((long)(notificationDate.time / 1000))
		endDateTimeStamp == Long.toString((long)(DateUtils.getEndOfTheDay(notificationDate).time / 1000))

		when: "Notification date is passed but is greater than OAuthAccount's lastData date"
		notificationDate = DateUtils.getStartOfTheDay(new Date() - 15)
		legacyOuraDataService.poll(account, notificationDate)

		then: "Start and End timestamp should match according to OAuthAccount's lastdata date"
		startDateTimeStamp == Long.toString((long)((account.lastData - 1).time / 1000))
	}

	@Test
	void "Test DataRequest implementation"() {
		given: "Mocked Sleep response data"
		Date tenDaysAgo = new Date() - 10
		Long eventTime = tenDaysAgo.getTime() / 1000

		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 3,
				type: "sleep", eventTime: $eventTime, data: {bedtime_m: 430, sleep_score: 76, awake_m: 42, rem_m: 68,
				 light_m: 320, deep_m: 2.60}}]}"""
		legacyOuraDataService.oauthService = [
				getOuraLegacyResource: { token, url, p, header ->
					if (url.contains("sleep")) {
						return new Response(new MockedHttpURLConnection(mockedResponseData))
					} else {
						return new Response(new MockedHttpURLConnection("{data: []}"))
					}
				}]

		when: "Poll is called entries should be created"
		account.lastData = account.lastPolled = new Date() - 17
		Utils.save(account, true)
		Boolean result = legacyOuraDataService.poll(account, new Date() - 15)

		then: "response should be true"
		Entry awakeEntry = Entry.findByUnits("hours awake")
		awakeEntry.amount == 0.700000000
		result

		when: "Mocked data has different amount for sleep awake entry"
		mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 3,
				type: "sleep", eventTime: $eventTime, data: {bedtime_m: 430, sleep_score: 76, awake_m: 40, rem_m: 68,
				 light_m: 320, deep_m: 2.60}}]}"""
		account.lastData = account.lastPolled = new Date() - 17
		Utils.save(account, true)
		result = legacyOuraDataService.poll(account, new Date() - 15)

		int totalSleepAwakeEntries = Entry.countByUnits("hours awake")
		awakeEntry = Entry.findByUnits("hours awake")

		then: "Entry for sleep awake gets modified"
		totalSleepAwakeEntries == 2
		assert awakeEntry.amount == 0.7000
	}

	@Test
	void testNotifications() {
		given:
		String mockNotificationData = [type: "sleep",
			date: "2016-01-01", userId: userId] as JSON

		legacyOuraDataService.notificationHandler(mockNotificationData)
		legacyOuraDataService.notificationProcessor()

		expect:
		assert true
		// TODO: Finish this up
	}

	@Test
	void testExpiredToken() {
		given:
		// Testing expired token for fitbit.
		fitBitDataService.oauthService = [
			getFitBitResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(401))
			}
		]

		expect:
		try {
			fitBitDataService.getUserProfile(account)
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException
			assert e.cause.provider.toLowerCase() == "fitbit"
		}
	}

	@Test
	void testExpiredTokenWithAPI() {
		given:
		try {
			// Testing directly with API.
			fitBitDataService.getUserProfile(account)
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException || e instanceof groovy.lang.MissingMethodException
		}

		account.accessToken = ""
		account.accessSecret = ""
		Utils.save(account, true)	// Will mimic new Token("", "")

		expect:
		try {
			// Testing directly with API with no token.
			fitBitDataService.getUserProfile(account)
		} catch(e) {
			assert e.cause instanceof InvalidAccessTokenException
		}
	}

	@Test
	void testPollForUser() {
		given:
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

		expect:
		assert Entry.count() > 0
		assert polledFitBit && polledWithings
	}


/**/
}
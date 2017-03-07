package us.wearecurio.services.integration

import java.text.DateFormat
import java.text.SimpleDateFormat
import org.scribe.model.Response
import spock.lang.Unroll
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.DataService
import us.wearecurio.services.DataService.DataRequestContext
import us.wearecurio.services.EmailService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.OuraDataService
import us.wearecurio.support.EntryStats
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

import java.math.MathContext

class OuraDataServiceTests extends CuriousServiceTestCase {
	static transactional = true

	OuraDataService ouraDataService
	EntryParserService entryParserService

	OAuthAccount account
	User user2
	TimeZone serverTimezone

	void setup() {
		serverTimezone = TimeZone.getDefault()
		setupTestData()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
				password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true,
				hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user2, true)

		account = new OAuthAccount([typeId: ThirdParty.OURA, userId: userId, accessToken: "Dummy-token",
				accessSecret: "Dummy-secret", accountId: userId, timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	void cleanup() {
		TimeZone.setDefault(serverTimezone)
	}

	void "Test subscribe method when OAuthAccount exists"() {
		when: 'The subscribe method is called'
		Map result = ouraDataService.subscribe(userId)

		then: 'Value of success key in result map should be true'
		result.success
	}

	void "Test subscribe method if OAuthAccount doesn't exist"() {
		when: 'The subscribe method is called'
		ouraDataService.subscribe(user2.id)

		then: 'MissingOAuthAccountException should be thrown'
		thrown(MissingOAuthAccountException)
	}

	void "Test unsubscribe method if OAuthAccount doesn't exist"() {
		when: 'The unsubscribe method is called'
		ouraDataService.unsubscribe(user2.id)

		then: 'MissingOAuthAccountException should be thrown'
		thrown(MissingOAuthAccountException)
	}

	void "Test unsubscribe method when OAuthAccount exists"() {
		given: 'Mocked deleteFitBitResource method of OauthService'
		ouraDataService.oauthService = [deleteFitBitResource: { token, url, p, header ->
			return new Response(new MockedHttpURLConnection("{}", 204))
		} ]

		assert OAuthAccount.count() == 1

		when: 'The unsubscribe method is called'
		ouraDataService.unsubscribe(userId)

		then: 'OAuthAccount instance should be deleted'
		OAuthAccount.count() == 0
	}

	void "Test GetDataSleep with failure status code"() {
		given:
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]

		when:
		// When oura returns a non-zero response code
		ouraDataService.getDataSleep(account, new Date(), false, new DataRequestContext())

		then:
		thrown(InvalidAccessTokenException)
	}

	void "Test getDataSleep success"() {
		given:
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 3,
				type: "sleep", eventTime: 1434440700, data: {bedtime_m: 510, sleep_score: 86, awake_m: 52, rem_m: 78, light_m: 220, deep_m: 160}},
				{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 3,
				type: "sleep", eventTime: 1424440900, data: {bedtime_m: 430, sleep_score: 76, awake_m: 42, rem_m: 68,
				 light_m: 320, deep_m: 2.60}}]}"""
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		/*
		 * Creating the date from entry's eventTime so that startDate and endDate range covers the created entries in 
		 * DataRequestContext when re-importing the same entries.
		 */
		Date mockDate = new Date(1434440700000).clearTime()
		account.lastData = mockDate - 2
		account.lastPolled = account.lastData
		account.save(flush: true)

		DataRequestContext dataRequestContext = new DataRequestContext(mockDate, null, [Identifier.look("Oura")],
				account.userId)
		/*
		 * Setting the max size to 5, so that during re-import only 5 entries will be available for duplicate 
		 * matching in the DataRequestContext, rest 7 entries will be matched by the hasDuplicate() method call in 
		 * Entry.groovy. Thus DataRequestContext and hasDuplicate() both will be tested for preventing 
		 * duplicate entry creation during re-import.
		 */
		dataRequestContext.max = 5

		// Keeping the Entry count zero before first poll.
		Entry.list()*.delete(flush: true)
		assert Entry.count() == 0

		when: 'The polling is done for the first time'
		ouraDataService.getDataSleep(account, mockDate, false, dataRequestContext)

		then: '12 new entries should be created.'
		assert Entry.getCount() == 12

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[0].description == "sleep [time: total]"
		assert entryList[0].setIdentifier.value == "Oura"
		assert entryList[6].timeZoneId == TimeZoneId.look("Asia/Kolkata").id

		when: 'The same data with same DataRequestContext is re-imported.'
		// Reset the lastData and lastPolled to same state.
		account.lastData = mockDate - 2
		account.lastPolled = account.lastData
		account.save(flush: true)

		// Note: The DataRequestContext is re-initialized in every call to getDataSleep.
		ouraDataService.getDataSleep(account, mockDate, false, dataRequestContext)

		then: 'Entry count should be same'
		assert Entry.getCount() == 12

		List<Entry> entryListNew = Entry.getAll()
		assert entryListNew[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryListNew[0].description == "sleep [time: total]"
		assert entryListNew[0].setIdentifier.value == "Oura"
		assert entryListNew[6].timeZoneId == TimeZoneId.look("Asia/Kolkata").id

		when: 'The same data with same DataRequestContext but updated lastData and lastPolled is re-imported.'
		ouraDataService.getDataSleep(account, mockDate, false, dataRequestContext)

		then: 'Entry count should still be same as data is same'
		assert Entry.getCount() == 12

		List<Entry> entryListUpdated = Entry.getAll()
		assert entryListUpdated[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryListUpdated[0].description == "sleep [time: total]"
		assert entryListUpdated[0].setIdentifier.value == "Oura"
		assert entryListUpdated[6].timeZoneId == TimeZoneId.look("Asia/Kolkata").id

		when: 'The same data with new DataRequestContext is re-imported.'
		// Reset the lastData and lastPolled to same state.
		account.lastData = mockDate - 2
		account.lastPolled = account.lastData
		account.save(flush: true)

		// In this case all entries will be matched by the hasDuplicate() method call.
		ouraDataService.getDataSleep(account, mockDate, false, new DataRequestContext())

		then: 'Entry count should still remain the same'
		assert Entry.getCount() == 12
		List<Entry> entryListFinal = Entry.getAll()
		assert entryListFinal[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryListFinal[0].description == "sleep [time: total]"
		assert entryListFinal[0].setIdentifier.value == "Oura"
		assert entryListFinal[6].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
	}

	void "test getDataSleep when same entries are imported from notifications due to new set name"() {
		given:
		DateFormat format = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm:ss a", Locale.ENGLISH);
		Date date = format.parse("Tuesday, Jun 16, 2015 10:15:00 AM");
		String mockedResponseData = """{"data":[{"dateCreated":"2015-11-04T12:42:45.168Z","timeZone":"Europe/Stockholm",
				"user":3,"type":"sleep","eventTime":1434440700,"data":{"bedtime_m":510,"sleep_score":86,"deep_m":160}}]}"""
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		// Given one simple entry
		Entry.create(account.userId, entryParserService.parse(new Date(), "America/Los_Angeles",
				"bread 2pm", null, null, new Date(), true), new EntryStats())

		when: "When first time entries are imported"
		ouraDataService.getDataSleep(account, new Date(), false, new DataRequestContext(date, null,
				[Identifier.look("Oura")], account.userId))

		then: "Then 3 sleep entries should be created (and one simple entry)"
		assert Entry.getCount() == 4
		assert Entry.countByUserId(account.userId) == 4

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "bread"
		assert entryList[0].setIdentifier == null
		assert entryList[0].userId == account.userId

		assert entryList[1].description == "sleep [time: total]"
		assert entryList[1].setIdentifier.value == "Oura"
		assert entryList[1].userId == account.userId

		when: "When same data is re-imported again"
		ouraDataService.getDataSleep(account, new Date(), false, new DataRequestContext(date, null,
				[Identifier.look("Oura")], account.userId))

		then: "Then previously imported entries from Oura should not be unset and no new entries should be created"
		println "entries: ${Entry.findAll()}"
		assert Entry.getCount() == 4
		assert Entry.countByUserId(account.userId) == 4

		List<Entry> newEntryList = Entry.getAll()
		assert newEntryList[0].description == "bread"
		assert newEntryList[0].setIdentifier == null
		assert newEntryList[0].userId == account.userId

		assert newEntryList[1].description == "sleep [time: total]"
		assert newEntryList[1].setIdentifier.value == "Oura"
		assert newEntryList[1].userId == account.userId
	}

	@Unroll
	void "Test #methodName method when access token is invalid"() {
		given: 'Mocked getOuraResource method of OauthService'
		ouraDataService.oauthService = [getOuraResource: { token, url, p, header ->
			return new Response(new MockedHttpURLConnection("{}", 401))
		} ]

		when: 'The #methodName method is called with invalid access token'
		// When oura returns 401 response code
		ouraDataService."${methodName}"(account, new Date(), false, new DataService.DataRequestContext())

		then: 'InvalidAccessTokenException should be thrown'
		thrown(InvalidAccessTokenException)

		where:
		methodName         | _
		'getDataSleep'     | _
		'getDataActivity'  | _
		'getDataReadiness' | _
	}

	void "Test getDataSleep method to successfully create entries for sleep data"() {
		given: 'API response data'
		String mockedResponseData = """{sleep: [
				{summary_date: "2015-11-04", timezone: "60",
				bedtime_start: "2016-09-03T23:50:09+01:00", score: 86, awake: 52, rem: 78, light: 220,
				deep: 160, hr_low_duration: 1170, hypnogram_5min: '42441112424444422222211', rmssd: 54, score_total:
				82, score_alignment: 38, score_efficiency: 98, got_up_count: 1, hr_10min: [255, 75, 54, 54, 56, 56,
				57, 56, 55], "temperature_max_delta": 103, score_disturbances: 76, total: 23820, rmssd_5min:
				[50, 62, 51, 55, 69, 68, 63, 53], bedtime_end: "2016-09-04T08:39:57+01:00", restless: 43,
				duration: 25680, hr_5min: [60, 60, 55, 55, 53, 54, 54], score_rem: 93, period_id: 0,
				wake_up_count: 1, hr_average: 56.875, onset_latency: 630, hr_lowest: 52, is_longest: 1, rem_rmssd: 44,
				nrem_rmssd: 56, midpoint_time: 7, score_deep: 49, score_latency: 93, efficiency: 93},

				{summary_date: "2015-11-03", timezone: "330", bedtime_start: "2016-09-02T23:50:09+05:30", score: 80,
				awake: 51, rem: 68, light: 210, deep: 163, hr_low_duration: 1170, hypnogram_5min:
				'42441112424444422222211', rmssd: 54, score_total: 82, score_alignment: 38, score_efficiency: 98,
				got_up_count: 2, hr_10min: [255, 75, 54, 54, 56, 56, 57, 56, 55], "temperature_max_delta": 102,
				score_disturbances: 76, total: 33823, rmssd_5min: [50, 62, 51, 55, 69, 68, 63, 53], bedtime_end:
				"2016-09-03T08:39:57+01:00", restless: 43, duration: 25680, hr_5min: [60, 60, 55, 55, 53, 54, 54],
				score_rem: 95, period_id: 0, wake_up_count: 2, hr_average: 56.875, onset_latency: 630,
				hr_lowest: 54, is_longest: 1, rem_rmssd: 44, nrem_rmssd: 56, midpoint_time: 7, score_deep: 49,
				score_latency: 93, efficiency: 93}
				]}"""

		and: 'Mocked getOuraResource method of OauthService'
		ouraDataService.oauthService = [getOuraResource: { token, url, p, header ->
			return new Response(new MockedHttpURLConnection(mockedResponseData))
		} ]

		when: 'The getDataSleep method is called'
		ouraDataService.getDataSleep(account, new Date(), false, new DataService.DataRequestContext())

		then: 'Entries should be created successfully'
		List<Entry> entryList = Entry.getAll()
		entryList.size() == 14

		entryList[0].timeZoneId == TimeZoneId.look("+01:00").id
		entryList[0].amount.round(new MathContext(3)) == new BigDecimal(23820 / 3600).round(new MathContext(3))
		entryList[0].units == 'hours total'
		entryList[0].description == "sleep [time: total]"
		entryList[0].setIdentifier.value == "Oura"

		entryList[13].timeZoneId == TimeZoneId.look("+05:30").id
		entryList[13].amount == 54
		entryList[13].units == 'bpm lowest'
		entryList[13].setIdentifier.value == "Oura"
	}

	void "Test getDataActivity method to successfully create entries for activity data"() {
		given: 'API response data'
		String mockedResponseData = """{activity: [
				{"steps": 11, "non_wear": 123, "daily_movement": 54, "class_5min":
				"11000101000022221", "met_1min": [0.1, 0.1, 0.1], "low": 0, "score_training_volume": 97, "met_min_low":
				0, "met_min_medium_plus": 0, "score_recovery_time": 100, "score": 100, "score_move_every_hour": 100,
				"timezone": 120, "met_min_high": 0, "inactive": 22, "cal_total": 1993, "cal_active": 1,
				"score_stay_active": 100, "day_end": "2015-12-23T03:59:59+02:00", "inactivity_alerts": 0,
				"score_meet_daily_targets": 100, "average_met": 1.03125, "met_min_medium": 0, "high": 0,
				"score_training_frequency": 100, "rest": 191, "medium": 0, "day_start": "2015-12-22T04:00:00+02:00",
				"met_min_inactive": 1, "summary_date": "2015-12-22"},

				{"steps": 23, "non_wear": 237,
				"daily_movement": 412, "class_5min": "11000101000022221", "met_1min": [0.1, 0.1, 0.1], "low": 0,
				"score_training_volume": 95, "met_min_low": 0, "met_min_medium_plus": 0, "score_recovery_time": 100,
				"score": 98, "score_move_every_hour": 100, "timezone": 180, "met_min_high": 0, "inactive": 22,
				"cal_total": 2533, "cal_active": 2, "score_stay_active": 100, "day_end": "2015-12-23T03:59:59+03:00",
				 "inactivity_alerts": 0, "score_meet_daily_targets": 100, "average_met": 1.03125, "met_min_medium":
				 0, "high": 0, "score_training_frequency": 100, "rest": 191, "medium": 0, "day_start":
				 "2015-12-22T04:00:00+03:00", "met_min_inactive": 1, "summary_date": "2015-12-23"}]}"""

		and: 'Mocked getOuraResource method of OauthService'
		ouraDataService.oauthService = [getOuraResource: { token, url, p, header ->
			return new Response(new MockedHttpURLConnection(mockedResponseData))
		} ]

		when: 'The getDataActivity method is called'
		ouraDataService.getDataActivity(account, new Date(), false, new DataService.DataRequestContext())

		then: 'New entries should be saved successfully'
		Entry.getCount() == 10

		Entry entry1 = Entry.first()
		Entry entry2 = Entry.last()

		entry1.timeZoneId == TimeZoneId.look("+02:00").id
		entry2.timeZoneId == TimeZoneId.look("+03:00").id

		entry1.amount.round(new MathContext(3)) == new BigDecimal(123 / 60).round(new MathContext(3))
		entry1.units == 'hours nonwear'

		entry2.amount == 2533
		entry2.units == 'kcal total'

		entry1.setIdentifier.value == "Oura"
		entry2.setIdentifier.value == "Oura"
	}

	void "Test getDataReadiness method to successfully create entries for readiness data"() {
		given: 'API response data'
		String mockedResponseData = """{readiness: [
				{"score_sleep_balance": 85, "score": 77, "score_activity_balance": 93, "period_id": 0,
				"score_previous_night": 58, "score_resting_hr": 80, "score_temperature": 100, "score_previous_day":
				50, "summary_date": "2017-02-13", "score_recovery_index": 100},

				{"score_sleep_balance": 90, "score": 82, "score_activity_balance": 96, "period_id": 0,
				"score_previous_night": 77, "score_resting_hr": 82, "score_temperature": 98, "score_previous_day":
				57, "summary_date": "2017-02-15", "score_recovery_index": 97}]}"""

		and: 'Mocked getOuraResource method of OauthService'
		ouraDataService.oauthService = [getOuraResource: { token, url, p, header ->
			return new Response(new MockedHttpURLConnection(mockedResponseData))
		} ]

		when: 'The getDataReadiness method is called with valid token'
		ouraDataService.getDataReadiness(account, new Date(), false, new DataService.DataRequestContext())

		then: 'New entries should be saved properly'
		Entry.getCount() == 2

		Entry entry1 = Entry.first()
		Entry entry2 = Entry.last()

		entry1.amount == 77
		entry1.units == 'score'

		entry2.amount == 82
		entry2.units == 'score'
	}

	void '''Test checkSyncHealth method to send email if there are no OAuthAccounts for oura with lastData greater
			than 24 hours'''() {
		given: 'An OAuthAccount instance for Oura with lastData within last 24 hours'
		OAuthAccount.list()*.delete(flush: true)
		assert OAuthAccount.count() == 0

		account = new OAuthAccount([typeId: ThirdParty.OURA, userId: userId, accessToken: "Dummy-token",
				accessSecret: "Dummy-secret", accountId: userId, timeZoneId: TimeZoneId.look("America/New_York").id])
		account.lastData = new Date()
		account.save(flush: true)

		assert account.id
		assert OAuthAccount.count() == 1

		and: "Mocked email service to verify emails"
		int mailCount = 0
		String subject
		String messageBody

		ouraDataService.emailService = [send: { String toString, String subjectString, String bodyString ->
			mailCount++
			subject = subjectString
			messageBody = bodyString
		}] as EmailService

		when: "OAuthAccount instance's lastData is within last 24 hours"
		ouraDataService.checkSyncHealth()

		then: "Emails will not be sent"
		mailCount == 0
		!messageBody
		!subject

		when: "OAuthAccount instance's lastData is older than 24 hours"
		account.lastData = new Date() - 2
		account.save(flush: true)
		ouraDataService.checkSyncHealth()

		then: "Emails will be sent"
		mailCount == 3
		subject == '[Curious] - Oura Sync Issue'
		messageBody == 'Not a single sync happened in the last 24 hours.'
	}
}

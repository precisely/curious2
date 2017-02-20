package us.wearecurio.services.integration

import org.scribe.model.Response
import spock.lang.Unroll
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.DataService
import us.wearecurio.services.EmailService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.OuraDataService
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
		methodName | _
		'getDataSleep' | _
		'getDataActivity' | _
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
				[50, 62, 51, 55, 69, 68, 63, 53], bedtime_end: "2017-02-14T08:39:57+01:00", restless: 43,
				duration: 25680, hr_5min: [60, 60, 55, 55, 53, 54, 54], awake: 1860, score_rem: 93, period_id: 0,
				wake_up_count: 1, hr_average: 56.875, onset_latency: 630, hr_lowest: 52, is_longest: 1, rem_rmssd: 44,
				nrem_rmssd: 56, midpoint_time: 7, score_deep: 49, score_latency: 93, efficiency: 93},

				{summary_date: "2015-11-03", timezone: "330", bedtime_start: "2016-09-03T23:50:09+05:30", score: 80,
				awake: 51, rem: 68, light: 220, deep: 160, hr_low_duration: 1170, hypnogram_5min:
				'42441112424444422222211', rmssd: 54, score_total: 82, score_alignment: 38, score_efficiency: 98,
				got_up_count: 2, hr_10min: [255, 75, 54, 54, 56, 56, 57, 56, 55], "temperature_max_delta": 102,
				score_disturbances: 76, total: 33823, rmssd_5min: [50, 62, 51, 55, 69, 68, 63, 53], bedtime_end:
				"2017-02-14T08:39:57+01:00", restless: 43, duration: 25680, hr_5min: [60, 60, 55, 55, 53, 54, 54],
				awake: 1860, score_rem: 95, period_id: 0, wake_up_count: 2, hr_average: 56.875, onset_latency: 630,
				hr_lowest: 52, is_longest: 1, rem_rmssd: 44, nrem_rmssd: 56, midpoint_time: 7, score_deep: 49,
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
		entryList[13].amount == 52
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

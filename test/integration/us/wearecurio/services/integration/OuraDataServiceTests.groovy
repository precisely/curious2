package us.wearecurio.services.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scribe.model.Response
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.OuraDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

class OuraDataServiceTests  extends CuriousServiceTestCase {
	static transactional = true

	OuraDataService ouraDataService
	OAuthAccount account
	User user2
	TimeZone serverTimezone

	@Before
	void setUp() {
		super.setUp()
		serverTimezone = TimeZone.getDefault()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
				password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user2, true)

		account = new OAuthAccount([typeId: ThirdParty.OURA, userId: userId, accessToken: "Dummy-token",
				accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	@After
	void tearDown() {
		super.tearDown()
		TimeZone.setDefault(serverTimezone)
	}

	@Test
	void "Test subscribe sucess"() {
		Map result = ouraDataService.subscribe(userId)
		assert result.success
	}

	@Test
	void "Test subscribe if no OAuthAccount"() {
		shouldFail(MissingOAuthAccountException) {
			ouraDataService.subscribe(user2.id)
		}
	}

	@Test
	void "Test unsubscribe if no OAuthAccount"() {
		shouldFail(MissingOAuthAccountException) {
			ouraDataService.unsubscribe(user2.id)
		}
	}

	@Test
	void "Test unsubscribe with OAuthAccount exists"() {
		ouraDataService.oauthService = [
				deleteFitBitResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 204))
				}
		]

		assert OAuthAccount.count() == 1
		ouraDataService.unsubscribe(userId)
		assert OAuthAccount.count() == 0
	}

	@Test
	void "Test ouraNotificationHandler success"() {
		String notificationString = """{type: exercise, date: "2015-09-12", userId: 3}"""
		ouraDataService.notificationHandler(notificationString)
		assert ThirdPartyNotification.count() == 1
		assert ThirdPartyNotification.first().typeId == ThirdParty.OURA
	}

	@Test
	void "Test GetDataSleep with failure status code"() {
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]
		// When oura returns a non-zero response code
		try {
			ouraDataService.getDataSleep(account, new Date(), false)
		} catch(e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

	@Test
	void "Test getDataSleep success"() {
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 3,
				type: "sleep", eventTime: 1434440700, data: {bedtime_m: 510, sleep_score: 86, awake_m: 52, rem_m: 78, light_m: 220, deep_m: 160}},
				{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 3,
				type: "sleep", eventTime: 1424440700, data: {bedtime_m: 430, sleep_score: 76, awake_m: 42, rem_m: 68, light_m: 320, deep_m: 260}}]}"""
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		ouraDataService.getDataSleep(account, new Date(), false)
		assert Entry.getCount() == 12

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[0].description == "sleep [time: total]"
		assert entryList[0].setIdentifier.value.startsWith("OURA sleep ")
		assert entryList[6].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
	}

	@Test
	void "Test getDataExercise with failure status code"() {
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]
		// When oura returns a non-zero response code
		try {
			ouraDataService.getDataExercise(account, new Date(), false)
		} catch(e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

	@Test
	void "Test getDataExercise with success as response and entries get created"() {
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 1,
				type: "exercise", eventTime: 1434440700, data: {duration_m: 10, classification: "sedentary"}},
				{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 1,
				type: "exercise", eventTime: 1424440700, data: {duration_m: 20, classification: "light"}}]}"""
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		ouraDataService.getDataExercise(account, new Date(), false)
		assert Entry.getCount() == 2

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[1].description == "light exercise [time]"
		assert entryList[1].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
	}

	@Test
	void "Test getDataExercise with success as response and entries get merged if two contiguous same entries are encountered"() {
		String mockedResponseData = """{data: [
					{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 1,
						type: "exercise", eventTime: 1434440700, data: {duration_m: 15, classification: "sedentary"}},
					{dateCreated: "2015-11-04T12:43:45.168Z", timeZone: "Europe/Stockholm", user: 1,
						type: "exercise", eventTime: 1434441600, data: {duration_m: 10, classification: "sedentary"}},
					{dateCreated: "2015-11-04T12:44:45.168Z", timeZone: "Asia/Kolkata", user: 1,
						type: "exercise", eventTime: 1434442200, data: {duration_m: 20, classification: "light"}}
				]}"""
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					// TODO Extend MockedHttpURLConnection to MockedHttpURLConnectionResponse to read directly from a file
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		ouraDataService.getDataExercise(account, new Date(), false)
		assert Entry.getCount() == 2

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].amount == 0.416666667		// Minutes converted to hours
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id

		assert entryList[1].amount == 0.333333333
		assert entryList[1].description == "light exercise [time]"
		assert entryList[1].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
	}

	@Test
	void "Test getDataExercise when entries get merged due to contiguous entries (not at the starting)"() {
		String mockedResponseData = new File("./test/integration/test-files/oura/exercise-contiguous-1.json").text
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		ouraDataService.getDataExercise(account, new Date(), false)
		// 6 entries should be created as pair of 2-2 entries will be merged into 1
		assert Entry.getCount() == 6

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].amount == 0.333333333		// Minutes converted to hours
		assert entryList[0].timeZoneId == TimeZoneId.look("America/New_York").id
		assert entryList[0].setIdentifier.value.startsWith("OURA exercise ")

		assert entryList[1].amount == 0.166666667
		assert entryList[1].description == "light exercise [time]"

		assert entryList[2].amount == 1.666666667
		assert entryList[2].description == "sedentary exercise [time]"

		assert entryList[3].amount == 0.333333333
		assert entryList[3].description == "light exercise [time]"

		assert entryList[4].amount == 0.833333333
		assert entryList[4].description == "sedentary exercise [time]"

		assert entryList[5].amount == 0.083333333
		assert entryList[5].description == "light exercise [time]"
		assert entryList[0].setIdentifier.value.startsWith("OURA exercise ")
	}

	@Test
	void "Test getDataExercise when contiguous entries do not get merged because of large gap between them"() {
		String mockedResponseData = """{data: [
					{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 1,
						type: "exercise", eventTime: 1434440700, data: {duration_m: 15, classification: "sedentary"}},
					{dateCreated: "2015-11-04T12:43:45.168Z", timeZone: "Europe/Stockholm", user: 1,
						type: "exercise", eventTime: 1434460700, data: {duration_m: 10, classification: "sedentary"}},
					{dateCreated: "2015-11-04T12:44:45.168Z", timeZone: "Asia/Kolkata", user: 1,
						type: "exercise", eventTime: 1434470700, data: {duration_m: 20, classification: "light"}}
				]}"""
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					// TODO Extend MockedHttpURLConnection to MockedHttpURLConnectionResponse to read directly from a file
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		ouraDataService.getDataExercise(account, new Date(), false)
		assert Entry.getCount() == 3

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].amount == 0.250000000
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[0].comment == "(Oura)"
		assert entryList[0].setIdentifier.value.startsWith("OURA exercise ")

		assert entryList[1].description == "sedentary exercise [time]"
		assert entryList[1].amount == 0.166666667
		assert entryList[1].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[1].comment == "(Oura)"

		assert entryList[2].amount == 0.333333333
		assert entryList[2].description == "light exercise [time]"
		assert entryList[2].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
		assert entryList[2].comment == "(Oura)"
	}

	@Test
	void "Test getDataExercise when false entry is passed with timeZone and user missing"() {
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", type: "activity",
				eventTime: 1434440700, data: {non_wear_m: 481, steps: 9800, eq_meters: 1478, active_cal: 204, total_cal: 2162}}]}"""
		ouraDataService.oauthService = [
			getOuraResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		try {
			ouraDataService.getDataExercise(account, new Date(), false)
		} catch (e) {
			assert e instanceof NumberFormatException
		}
	}

	@Test
	void "Test getDataActivity with failure status code"() {
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]
		// When oura returns a non-zero response code
		try {
			ouraDataService.getDataActivity(account, new Date(), false)
		} catch(e) {
			assert e instanceof InvalidAccessTokenException
		}
	}

	@Test
	void "Test getDataActivity success with two entries being created"() {
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 2,
				type: "activity", eventTime: 1434440700, data: {non_wear_m: 481, steps: 9800, eq_meters: 1478, active_cal: 204, total_cal: 2162}},
				{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 2,
				type: "activity", eventTime: 1424440700, data: {non_wear_m: 441, steps: 10000, eq_meters: 1778, active_cal: 254, total_cal: 2262}}]}"""

		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		ouraDataService.getDataActivity(account, new Date(), false)
		assert Entry.getCount() == 10

		Entry entry1 = Entry.first()
		Entry entry2 = Entry.last()
		assert entry1.timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entry2.timeZoneId == TimeZoneId.look("Asia/Kolkata").id
		assert entry1.setIdentifier.value.startsWith("OURA activity ")
	}
}
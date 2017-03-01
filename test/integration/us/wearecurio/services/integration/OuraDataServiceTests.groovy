package us.wearecurio.services.integration

import org.scribe.model.Response
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.DataService.DataRequestContext
import us.wearecurio.services.EmailService
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.OuraDataService
import us.wearecurio.support.EntryStats
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.utility.Utils

import java.text.DateFormat
import java.text.SimpleDateFormat

class OuraDataServiceTests  extends CuriousServiceTestCase {
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
				password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user2, true)

		account = new OAuthAccount([typeId: ThirdParty.OURA, userId: userId, accessToken: "Dummy-token",
				accessSecret: "Dummy-secret", accountId: userId, timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	void cleanup() {
		TimeZone.setDefault(serverTimezone)
	}

	void "Test subscribe success"() {
		when:
		Map result = ouraDataService.subscribe(userId)

		then:
		assert result.success
	}

	void "Test subscribe if no OAuthAccount"() {
		when:
		ouraDataService.subscribe(user2.id)

		then:
		thrown(MissingOAuthAccountException)
	}

	void "Test unsubscribe if no OAuthAccount"() {
		when:
		ouraDataService.unsubscribe(user2.id)

		then:
		thrown(MissingOAuthAccountException)
	}

	void "Test unsubscribe with OAuthAccount exists"() {
		given:
		ouraDataService.oauthService = [
				deleteFitBitResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 204))
				}
		]

		assert OAuthAccount.count() == 1

		when:
		ouraDataService.unsubscribe(userId)

		then:
		assert OAuthAccount.count() == 0
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

	void "Test getDataExercise with failure status code"() {
		given:
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]

		when:
		// When oura returns a non-zero response code
		ouraDataService.getDataExercise(account, new Date(), false, new DataRequestContext())

		then:
		thrown(InvalidAccessTokenException)
	}

	void "Test getDataExercise with success as response and entries get created"() {
		given:
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 1,
				type: "exercise", eventTime: 1434440700, data: {duration_m: 10, classification: "sedentary"}},
				{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 1,
				type: "exercise", eventTime: 1424440700, data: {duration_m: 20, classification: "light"}}]}"""
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		when:
		ouraDataService.getDataExercise(account, new Date(), false, new DataRequestContext())

		then:
		assert Entry.getCount() == 2

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[1].description == "light exercise [time]"
		assert entryList[1].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
	}

	void "Test getDataExercise with success as response and entries get merged if two contiguous same entries are encountered"() {
		given:
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

		when:
		ouraDataService.getDataExercise(account, new Date(), false, new DataRequestContext())

		then:
		assert Entry.getCount() == 2

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].amount == 0.416666667		// Minutes converted to hours
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id

		assert entryList[1].amount == 0.333333333
		assert entryList[1].description == "light exercise [time]"
		assert entryList[1].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
	}

	void "Test getDataExercise when entries get merged due to contiguous entries (not at the starting)"() {
		given:
		String mockedResponseData = new File("./test/integration/test-files/oura/exercise-contiguous-1.json").text
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		when:
		ouraDataService.getDataExercise(account, new Date(), false, new DataRequestContext())
		// 6 entries should be created as pair of 2-2 entries will be merged into 1

		then:
		assert Entry.getCount() == 6

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].amount == 0.333333333		// Minutes converted to hours
		assert entryList[0].timeZoneId == TimeZoneId.look("America/New_York").id
		assert entryList[0].setIdentifier.value == "Oura"

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
		assert entryList[0].setIdentifier.value == "Oura"
	}

	void "Test getDataExercise when contiguous entries do not get merged because of large gap between them"() {
		given:
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

		when:
		ouraDataService.getDataExercise(account, new Date(), false, new DataRequestContext())

		then:
		assert Entry.getCount() == 3

		List<Entry> entryList = Entry.getAll()
		assert entryList[0].description == "sedentary exercise [time]"
		assert entryList[0].amount == 0.250000000
		assert entryList[0].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[0].comment == "(Oura)"
		assert entryList[0].setIdentifier.value == "Oura"

		assert entryList[1].description == "sedentary exercise [time]"
		assert entryList[1].amount == 0.166666667
		assert entryList[1].timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entryList[1].comment == "(Oura)"

		assert entryList[2].amount == 0.333333333
		assert entryList[2].description == "light exercise [time]"
		assert entryList[2].timeZoneId == TimeZoneId.look("Asia/Kolkata").id
		assert entryList[2].comment == "(Oura)"
	}

	void "Test getDataExercise when false entry is passed with timeZone and user missing"() {
		given:
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", type: "activity",
				eventTime: 1434440700, data: {non_wear_m: 481, steps: 9800, eq_meters: 1478, active_cal: 204, total_cal: 2162}}]}"""
		ouraDataService.oauthService = [
			getOuraResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		ouraDataService.getDataExercise(account, new Date(), false, new DataRequestContext())

		then:
		thrown(NumberFormatException)
	}

	void "Test getDataActivity with failure status code"() {
		given:
		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection("{}", 401))
				}
		]

		when:
		// When oura returns a non-zero response code
		ouraDataService.getDataActivity(account, new Date(), false, new DataRequestContext())

		then:
		thrown(InvalidAccessTokenException)
	}

	void "Test getDataActivity success with two entries being created"() {
		given:
		String mockedResponseData = """{data: [{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Europe/Stockholm", user: 2,
				type: "activity", eventTime: 1434440700, data: {non_wear_m: 481, steps: 9800, eq_meters: 1478, active_cal: 204, total_cal: 2162}},
				{dateCreated: "2015-11-04T12:42:45.168Z", timeZone: "Asia/Kolkata", user: 2,
				type: "activity", eventTime: 1424440700, data: {non_wear_m: 441, steps: 10000, eq_meters: 1778, active_cal: 254, total_cal: 2262}}]}"""

		ouraDataService.oauthService = [
				getOuraResource: { token, url, p, header ->
					return new Response(new MockedHttpURLConnection(mockedResponseData))
				}
		]

		when:
		ouraDataService.getDataActivity(account, new Date(), false, new DataRequestContext())

		then:
		assert Entry.getCount() == 10

		Entry entry1 = Entry.first()
		Entry entry2 = Entry.last()
		assert entry1.timeZoneId == TimeZoneId.look("Europe/Stockholm").id
		assert entry2.timeZoneId == TimeZoneId.look("Asia/Kolkata").id
		assert entry1.setIdentifier.value == "Oura"
	}

	void "Test ouraNotificationHandler success when request body is passed as string"() {
		given:
		String notificationString = """{type: exercise, date: "2015-09-12", userId: ${userId}, subscriptionId: "4"}"""

		when:
		ouraDataService.notificationHandler(notificationString)

		then:
		assert ThirdPartyNotification.count() == 1
		assert ThirdPartyNotification.first().typeId == ThirdParty.OURA
	}

	void "Test ouraNotificationHandler success when request body is passed as Object Array"() {
		given:
		def notificationList = [[type: 'exercise', date: "2015-09-12", userId: userId, subscriptionId: "4"]]

		when:
		ouraDataService.notificationHandler(notificationList)

		then:
		assert ThirdPartyNotification.count() == 1
		assert ThirdPartyNotification.first().typeId == ThirdParty.OURA
	}

	void "Test ouraNotificationHandler success when request body is passed as Object"() {
		given:
		Object notification = [type: 'exercise', date: "2015-09-12", userId: userId, subscriptionId: "4"]

		when:
		ouraDataService.notificationHandler(notification)

		then:
		assert ThirdPartyNotification.count() == 1
		assert ThirdPartyNotification.first().typeId == ThirdParty.OURA
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
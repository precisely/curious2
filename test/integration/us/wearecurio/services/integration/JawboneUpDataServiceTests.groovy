package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.scribe.model.Response
import us.wearecurio.datetime.DateUtils
import us.wearecurio.hashids.DefaultHashIDGenerator
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.ThirdPartyNotification.Status
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.services.DataService.DataRequestContext
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat

class JawboneUpDataServiceTests extends IntegrationSpec {

	// Example: ./test/integration/test-files/jawboneup/body-data.js		For single data file
	// Example: ./test/integration/test-files/jawboneup/sleep-data-1.js		For multiple data file
	private static final String BASE_DATA_PATH = "./test/integration/test-files/jawboneup/%s-data%s.js"

	OAuthAccount account
	def grailsApplication
	JawboneUpDataService jawboneUpDataService
	User user

	Long userId

	private String testDataPath(String fileName, int number = 0) {
		String fileNumber = number ? "-$number" : ""
		return String.format(BASE_DATA_PATH, fileName, fileNumber)
	}

	def cleanup() {
	}

	def setup() {
		// Adding mixin to class like meta class for testing
		BigDecimal.mixin(Roundable)

		Utils.resetForTesting()

		User.list()*.delete()	// Deleting existing records temporary to create default user.
		Entry.list()*.delete(flush: true)

		user = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", name: "Mark Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true, hash: new DefaultHashIDGenerator().generate(12)])
		assert Utils.save(user, true)

		userId = user.id

		account = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	void "test get data sleep"() {
		String mockedResponseData = new File(testDataPath("sleep", 1)).text

		Date startDate = new Date()
		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				String startTime = Long.toString((long)(startDate.getTime() / 1000))
				String endTime = Long.toString((long)(DateUtils.getEndOfTheDay(startDate).getTime() / 1000))

				assert url == "https://jawbone.com/nudge/api/v.1.1/users/@me/sleeps?start_time=${startTime}&end_time=${endTime}&updated_after=${startTime}"
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataSleep(account, startDate, false, new DataRequestContext())

		then:
		result.success == true

		List<Entry> userEntries = Entry.findAllByUserId(userId)
		userEntries.size() == 4

		userEntries.find { it.tag.description == "sleep [time]" } != null
		userEntries.find { it.tag.description == "sleep [time]" }.amount.round(2) == 672.58g
		userEntries.find { it.tag.description == "sleep [time]" }.units == "mins"

		userEntries.find { it.tag.description == "sleep interruptions" } != null
		userEntries.find { it.tag.description == "sleep interruptions" }.amount.round(0) == 2.0g
		userEntries.find { it.tag.description == "sleep interruptions" }.units == ""

		userEntries.find { it.tag.description == "sleep [time: awake]" } != null
		userEntries.find { it.tag.description == "sleep [time: awake]" }.amount.round(2) == 53.30g
		userEntries.find { it.tag.description == "sleep [time: awake]" }.units == "mins awake"

		userEntries.find { it.tag.description == "sleep quality" } != null
		userEntries.find { it.tag.description == "sleep quality" }.amount.round(2) == 80.00g.round(2)
		userEntries.find { it.tag.description == "sleep quality" }.units == "%"
	}

	void "test get data body"() {
		String mockedResponseData = new File(testDataPath("body")).text

		Date startDate = new Date()
		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				String startTime = Long.toString((long)(startDate.getTime() / 1000))
				String endTime = Long.toString((long)(DateUtils.getEndOfTheDay(startDate).getTime() / 1000))

				assert url == "https://jawbone.com/nudge/api/v.1.1/users/@me/body_events?start_time=${startTime}&end_time=${endTime}&updated_after=${startTime}"
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataBody(account, startDate, false, new DataRequestContext())

		then:
		result.success == true

		List<Entry> userEntries = Entry.findAllByUserId(userId)
		userEntries.size() == 2

		userEntries[0].baseTag.description == "measurement"
		userEntries[0].tag.description == "measurement [amount]"
		userEntries[0].amount.setScale(2, BigDecimal.ROUND_HALF_UP) == 124.0g
		userEntries[0].setIdentifier.toString() == "Jawbone Up"

		userEntries[1].baseTag.description == "measurement"
		userEntries[1].tag.description == "measurement [amount]"
		userEntries[1].amount.setScale(2, BigDecimal.ROUND_HALF_UP) == 124.0g
		userEntries[1].setIdentifier.toString() == "Jawbone Up"
	}

	void "test get data body if response is not success"() {
		String mockedResponseData = """{"meta":{"user_xid":"COX79cB0-dR-owR18F3dvw","message":"NOT_AUTHORIZED","code":401,"time":1412828466}}"""

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataBody(account, new Date(), false, new DataRequestContext())

		then:
		result.success == false
		Entry.count() == 0
	}

	void "test get data move"() {
		String mockedResponseData = new File(testDataPath("moves")).text

		Date startDate = new Date()
		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				String startTime = Long.toString((long)(startDate.getTime() / 1000))
				String endTime = Long.toString((long)(DateUtils.getEndOfTheDay(startDate).getTime() / 1000))

				assert url == "https://jawbone.com/nudge/api/v.1.1/users/@me/moves?start_time=${startTime}&end_time=${endTime}&updated_after=${startTime}"
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)

		when:
		Map result = jawboneUpDataService.getDataMove(account, startDate, false, new DataRequestContext())

		then:
		result.success == true

		List<Entry> userEntries = Entry.findAllByUserId(userId)
		userEntries.size() == 19

		userEntries.find { it.tag.description == "activity [distance]" } != null
		userEntries.find { it.tag.description == "activity [time]" } != null
		userEntries.find { it.tag.description == "activity [steps]" } != null

		// High activity entries. First for timed group of hour 12th & second for 14th, 15th
		List<Entry> highActivityEntries = userEntries.findAll { it.tag.description == "high activity [steps]" }
		// Average of steps
		// First is for hour 12th
		highActivityEntries[0].amount.round(0) == 1244.toBigDecimal().round(0)
		highActivityEntries[0].units == "steps"

		// Second for hour 14th & 15th (grouped)
		highActivityEntries[1].amount.round(0) == 878.toBigDecimal().round(0)
		highActivityEntries[1].units == "steps"

		List<Entry> highActivityDurationEntry = userEntries.findAll { it.tag.description == "high activity [time]" }
		// Average of durations
		// First is for hour 12th
		highActivityDurationEntry[0].amount.round(2) == 10.82g.round(2)
		highActivityDurationEntry[0].units == "mins"

		// Second for hour 14th & 15th (grouped)
		highActivityDurationEntry[1].amount.round(2) == 7.48g.round(2)
		highActivityDurationEntry[1].units == "mins"

		List<Entry> highActivityDistanceEntry = userEntries.findAll { it.tag.description == "high activity [distance]" }
		// Average of distance
		// First is for hour 12th
		highActivityDistanceEntry[0].amount.round(3) == 0.566g.round(3)
		highActivityDistanceEntry[0].units == "miles"

		// Second for hour 14th & 15th (grouped)
		highActivityDistanceEntry[1].amount.round(3) == 0.406g.round(3)
		highActivityDistanceEntry[1].units == "miles"

		List<Entry> highActivityCaloriesEntry = userEntries.findAll { it.tag.description == "high activity [calories]" }
		// Average of distance
		// First is for hour 12th
		highActivityCaloriesEntry[0].amount.round(3) == 1244.0g.round(3)
		highActivityCaloriesEntry[0].units == "cal"

		// Second for hour 14th & 15th (grouped)
		highActivityCaloriesEntry[1].amount.round(3) == 878.0g.round(3)
		highActivityCaloriesEntry[1].units == "cal"

		// Light activities. First for timed group of hours 08th, 09th, 10th, 11th & second for 13th
		List <Entry> lightActivityEntries = userEntries.findAll { it.tag.description == "light activity [steps]" }
		lightActivityEntries.size() == 2

		// For timed group of hours 08th, 09th, 10th, 11th
		lightActivityEntries[0].amount.round(0) == 212.0g.round(0)
		lightActivityEntries[0].units == "steps"

		// For hour 13th
		lightActivityEntries[1].amount.round(0) == 82.0g.round(0)
		lightActivityEntries[1].units == "steps"

		List <Entry> lightActivityDurationEntries = userEntries.findAll { it.tag.description == "light activity [time]" }
		// Average of durations
		// For timed group of hours 08th, 09th, 10th, 11th
		lightActivityDurationEntries[0].amount.round(2) == 1.89g.round(2)
		lightActivityDurationEntries[0].units == "mins"

		// For hour 13th
		lightActivityDurationEntries[1].amount.round(4) == 0.7167g.round(4)
		lightActivityDurationEntries[1].units == "mins"

		List<Entry> lightActivityDistanceEntries = userEntries.findAll { it.tag.description == "light activity [distance]" }
		// Average of distance
		// For timed group of hours 08th, 09th, 10th, 11th
		lightActivityDistanceEntries[0].amount.round(4) == 0.0951g.round(4)
		lightActivityDistanceEntries[0].units == "miles"

		// For hour 13th
		lightActivityDistanceEntries[1].amount.round(4) == 0.0373g.round(4)
		lightActivityDistanceEntries[1].units == "miles"
	}

	void "test get data sleep for pagination"() {
		String mockedResponseData = new File(testDataPath("sleep", 1)).text
		String mockedResponsePaginationData = new File(testDataPath("sleep", 2)).text

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, String url, p, header ->
				String response = mockedResponsePaginationData
				if (url.contains("page_token")) {
					response = mockedResponseData
				}

				return new Response(new MockedHttpURLConnection(response))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataSleep(account, new Date(), false, new DataRequestContext())

		then:
		result.success == true
		Entry.count() == 8
	}

	void "test get data sleep if response is not success"() {
		String mockedResponseData = """{"meta":{"user_xid":"COX79cB0-dR-owR18F3dvw","message":"NOT_AUTHORIZED","code":401,"time":1412828466}}"""

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataSleep(account, new Date(), false, new DataRequestContext())

		then:
		result.success == false
		Entry.count() == 0
	}

	void "test notification handler for null data"() {
		when: "notification handler is called with no data"
		jawboneUpDataService.notificationHandler("")

		then: "No notification will be created"
		ThirdPartyNotification.count() == 0
	}

	void "test notification handler with invalid json data"() {
		expect: "invalid json string is passed"
		try {
			jawboneUpDataService.notificationHandler("{notification_timestamp: ")
		} catch(ConverterException e) {
			assert e != null
		}
	}

	void "test notification handler with no type or timestamp"() {
		when: "notification data does not contains type or timestamp"
		jawboneUpDataService.notificationHandler("""{"notification_timestamp":1379364954,"events":[{"action":"enter_sleep_mode","timestamp":1379364951,"user_xid":"RGaCBFg9CsB83FsEcMY44A","secret_hash":"e570b3071a0964f9e2e69d13nd9ba19535392aaa"}]}""")

		then: "No notification will be created"
		ThirdPartyNotification.count() == 0
	}

	void "test notification handler when secret hash does not match"() {
		when: "Invalid secret hash received"
		jawboneUpDataService.notificationHandler("""{"notification_timestamp":"1372787949","events":[
				{"user_xid":"RGaCBFg9CsB83FsEcMY44A","event_xid":"EJpCkyAtwoO0XTdkYyuTNw","type":"move",
				"action":"creation","timestamp":"1372787849"}],"secret_hash":"e570b3071a0964f9e2e69d13nd9ba19535392aaa"}""")

		then: "No instance will be created"
		ThirdPartyNotification.count() == 0
	}

	void "test notification handler"() {
		ConfigObject config = grailsApplication.config.oauth.providers.jawboneup
		String cliendSecretID = config.key + config.secret
		String hash = cliendSecretID.encodeAsSHA256()

		when:
		jawboneUpDataService.notificationHandler("""{"notification_timestamp":"1372787949","events":[
				{"user_xid":"RGaCBFg9CsB83FsEcMY44A","event_xid":"EJpCkyAtwoO0XTdkYyuTNw","type":"move",
				"action":"creation","timestamp":"1372787849","secret_hash":"$hash"},{"user_xid":"RGaCBFg9CsB83FsEcMY44A",
				"event_xid":"blaHyAtwoO0XTdkYyuTNw","type":"sleep","action":"updation","timestamp":"1372787859"}],
				"secret_hash":"$hash"}""")

		then: "Two notification will be created"
		ThirdPartyNotification.count() == 2

		ThirdPartyNotification firstNotification = ThirdPartyNotification.first()
		firstNotification.collectionType == "move"
		firstNotification.ownerId == "RGaCBFg9CsB83FsEcMY44A"
		firstNotification.status == Status.UNPROCESSED
		firstNotification.typeId == ThirdParty.JAWBONE
	}
/**/
}

class Roundable {
	String toString() {
		BigDecimal.getMethod('toString').invoke(this.metaClass.owner)
	}

	BigDecimal round(int n) {
		return setScale(n, BigDecimal.ROUND_HALF_UP)
	}
}

package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.scribe.model.Response

import spock.lang.*
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.ThirdPartyNotification.Status
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils

class JawboneUpDataServiceSpec extends IntegrationSpec {

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

		user = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user.save()

		userId = user.id

		account = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	void "test get data body"() {
		String mockedResponseData = new File(testDataPath("body")).text

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataBody(account, new Date(), false)

		then:
		result.success == true

		List<Entry> userEntries = Entry.findAllByUserId(userId)
		userEntries.size() == 1
		userEntries[0].baseTag.description == "measurement"
		userEntries[0].tag.description == "measurement weight"
		userEntries[0].amount.round(2) == 120.toBigDecimal().round(2)
	}

	void "test get data body if response is not success"() {
		String mockedResponseData = """{"meta":{"user_xid":"COX79cB0-dR-owR18F3dvw","message":"NOT_AUTHORIZED","code":401,"time":1412828466}}"""

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataBody(account, new Date(), false)

		then:
		result.success == false
		Entry.count() == 0
	}

	void "test get data move"() {
		String mockedResponseData = new File(testDataPath("moves")).text

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)

		when:
		Map result = jawboneUpDataService.getDataMove(account, new Date(), false)

		then:
		result.success == true

		List<Entry> userEntries = Entry.findAllByUserId(userId)
		userEntries.size() == 9

		userEntries.find { it.tag.description == "activity distance" } != null
		userEntries.find { it.tag.description == "activity duration" } != null
		userEntries.find { it.tag.description == "activity steps" } != null

		Entry highActivityEntry = userEntries.find { it.tag.description == "high activity steps" }
		highActivityEntry != null
		// Average of steps
		highActivityEntry.amount == 1000.toBigDecimal()
		highActivityEntry.units == "steps"

		Entry highActivityDurationEntry = userEntries.find { it.tag.description == "high activity duration" }
		highActivityDurationEntry != null
		// Average of durations
		highActivityDurationEntry.amount == (8.59).toBigDecimal()
		highActivityDurationEntry.units == "mins"

		Entry highActivityDistanceEntry = userEntries.find { it.tag.description == "high activity distance" }
		highActivityDistanceEntry != null
		// Average of distance
		highActivityDistanceEntry.amount == (0.459).toBigDecimal()
		highActivityDistanceEntry.units == "miles"

		Entry lightActivityEntry = userEntries.find { it.tag.description == "light activity steps" }
		lightActivityEntry != null
		// Average of steps
		lightActivityEntry.amount.round(3) == (209.286).toBigDecimal().round(3)
		lightActivityEntry.units == "steps"

		Entry lightActivityDurationEntry = userEntries.find { it.tag.description == "light activity duration" }
		lightActivityDurationEntry != null
		// Average of durations
		lightActivityDurationEntry.amount.round(2) == (1.880).toBigDecimal().round(2)
		lightActivityDurationEntry.units == "mins"

		Entry lightActivityDistanceEntry = userEntries.find { it.tag.description == "light activity distance" }
		lightActivityDistanceEntry != null
		// Average of distance
		lightActivityDistanceEntry.amount.round(3) == (0.094).toBigDecimal().round(3)
		lightActivityDistanceEntry.units == "miles"
	}

	void "test get data sleep"() {
		String mockedResponseData = new File(testDataPath("sleep", 1)).text

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataSleep(account, new Date(), false)

		then:
		result.success == true

		List<Entry> userEntries = Entry.findAllByUserId(userId)
		userEntries.size() == 4

		userEntries.find { it.tag.description == "sleep duration" } != null
		userEntries.find { it.tag.description == "sleep duration" }.amount.round(2) == 673
		userEntries.find { it.tag.description == "sleep duration" }.units == "mins"

		userEntries.find { it.tag.description == "sleep interruptions" } != null
		userEntries.find { it.tag.description == "sleep interruptions" }.amount.round(0) == 2.toBigDecimal()
		userEntries.find { it.tag.description == "sleep interruptions" }.units == ""

		userEntries.find { it.tag.description == "sleep awake" } != null
		userEntries.find { it.tag.description == "sleep awake" }.amount.round(2) == (53.30).toBigDecimal()
		userEntries.find { it.tag.description == "sleep awake" }.units == "mins"

		userEntries.find { it.tag.description == "sleep quality" } != null
		userEntries.find { it.tag.description == "sleep quality" }.amount.round(2) == 80.toBigDecimal().round(2)
		userEntries.find { it.tag.description == "sleep quality" }.units == "%"
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
		Map result = jawboneUpDataService.getDataSleep(account, new Date(), false)

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
		Map result = jawboneUpDataService.getDataSleep(account, new Date(), false)

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
				"action":"creation","timestamp":"1372787849","secret_hash":"e570b3071a0964f9e2e69d13nd9ba19535392aaa"}]}""")

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
				"event_xid":"blaHyAtwoO0XTdkYyuTNw","type":"sleep","action":"updation","timestamp":"1372787859",
				"secret_hash":"$hash"}]}""")

		then: "Two notification will be created"
		ThirdPartyNotification.count() == 2

		ThirdPartyNotification firstNotification = ThirdPartyNotification.first()
		firstNotification.collectionType == "move"
		firstNotification.ownerId == "RGaCBFg9CsB83FsEcMY44A"
		firstNotification.status == Status.UNPROCESSED
		firstNotification.typeId == ThirdParty.JAWBONE
	}
}

class Roundable {
	BigDecimal round(int n) {
		return setScale(n, BigDecimal.ROUND_HALF_UP)
	}
}
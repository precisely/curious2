package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec

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

	private static final String BASE_DATA_PATH = "./test/integration/test-files"
	private static final String BODY_DATA_PATH = "$BASE_DATA_PATH/jawbone-up-body-data.js"
	private static final String DATA_SPLITER = "-----"
	private static final String MOVES_DATA_PATH = "$BASE_DATA_PATH/jawbone-up-moves-data.js"
	private static final String SLEEP_DATA_PATH = "$BASE_DATA_PATH/jawbone-up-sleep-data.js"

	OAuthAccount account
	def grailsApplication
	JawboneUpDataService jawboneUpDataService
	User user

	Long userId

	def setup() {
		user = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user.save()

		userId = user.id

		account = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	void "test get data body"() {
		String mockedResponseData = new File(BODY_DATA_PATH).text.split(DATA_SPLITER)[0]

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataBody(account, new Date(), false)

		then:
		result.success == true

		Entry.withCriteria() {
			baseTag { eq("description", "weight") }
		}.size() == 2
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

	void "test get data moves"() {
		String mockedResponseData = new File(MOVES_DATA_PATH).text.split(DATA_SPLITER)[0]

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataMove(account, new Date(), false)

		then:
		result.success == true

		Entry.count() == 5
	}

	void "test get data sleep"() {
		String mockedResponseData = new File(SLEEP_DATA_PATH).text.split(DATA_SPLITER)[0]

		jawboneUpDataService.oauthService = [
			getJawboneupResource: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataSleep(account, new Date(), false)

		then:
		result.success == true
		Entry.count() == 2

		Entry entryInstance = Entry.withCriteria(uniqueResult: true) {
			baseTag { eq("description", "sleep") }
		}

		entryInstance != null
	}

	void "test get data sleep for pagination"() {
		String mockedResponseData = new File(SLEEP_DATA_PATH).text.split(DATA_SPLITER)[0]
		String mockedResponsePaginationData = new File(SLEEP_DATA_PATH).text.split(DATA_SPLITER)[1]

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
		Entry.count() == 4
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
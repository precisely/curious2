package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec

import org.scribe.model.Response

import spock.lang.*
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Tag
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils

class JawboneUpDataServiceSpec extends IntegrationSpec {

	private static final String BASE_DATA_PATH = "./test/integration/test-files"
	private static final String BODY_DATA_PATH = "$BASE_DATA_PATH/jawbone-up-body-data.js"
	private static final String DATA_SPLITER = "-----"
	private static final String SLEEP_DATA_PATH = "$BASE_DATA_PATH/jawbone-up-sleep-data.js"

	OAuthAccount account
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
}
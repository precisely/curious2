package us.wearecurio.services.integration

import grails.test.spock.IntegrationSpec

import org.scribe.model.Response

import spock.lang.*
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.JawboneUpDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.utility.Utils

class JawboneUpDataServiceSpec extends IntegrationSpec {

	OAuthAccount account
	User user
	Long userId

	JawboneUpDataService jawboneUpDataService

	def setup() {
		user = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user.save()

		userId = user.id

		account = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)
	}

	void "test get data sleep"() {
		String mockedResponseData = new File("./test/integration/test-files/jawbone-up-sleep-data").text
		println mockedResponseData

		jawboneUpDataService.oauthService = [
			getJawboneupResourceWithQuerystringParams: { token, url, p, header ->
				return new Response(new MockedHttpURLConnection(mockedResponseData))
			}
		]

		when:
		Map result = jawboneUpDataService.getDataSleep(account, new Date(), false)

		then:
		result.success == true
		Entry.count() == 2
	}
}
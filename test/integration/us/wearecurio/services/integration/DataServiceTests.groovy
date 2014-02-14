package us.wearecurio.services.integration

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

import grails.test.mixin.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.User
import us.wearecurio.services.DataService
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.UrlService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection

class DataServiceTests extends CuriousServiceTestCase {

	WithingsDataService withingsDataService
	FitBitDataService fitBitDataService
	OAuthAccount account
	OAuthAccount account2

	def grailsApplication

	@Override
	void setUp() {
		super.setUp()

		account = new OAuthAccount([typeId: ThirdParty.WITHINGS, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"])

		Utils.save(account, true)
		
		account2 = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id"])

		Utils.save(account2, true)
	}

	@Override
	void tearDown() {
	}

	void testPollForUser() {
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
			}
		]

		DataService.pollAllForUserId(userId)
		
		assert Entry.count() > 0
		assert polledFitBit && polledWithings
	}
}
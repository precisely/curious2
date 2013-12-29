package us.wearecurio.services

import static us.wearecurio.model.OAuthAccount.*
import grails.converters.JSON
import grails.util.Environment

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount

class IHealthDataService {

	static final String BASE_URL = "http://sandboxapi.ihealthlabs.com%s%s%s.json/?client_id=%s&client_secret=%s&access_token=%s"
	static final String MOVES_SET_NAME = "ihealth import"
	static final String SET_NAME = "ihealth import"

	def oauthService

	JSONObject doWithURL(Token tokenInstance, String url, String type) {
		Response apiResponse = oauthService.getIHealthResource(tokenInstance, url)

		log.debug "Received [$type] response from IHealth API with code [$apiResponse.code]"
		if (Environment.current == Environment.DEVELOPMENT) {
			log.debug "Received [$type] response from IHealth API with body [$apiResponse.body]"
		}

		JSON.parse(apiResponse.body)
	}

	JSONObject getDataForBloodOxygen(OAuthAccount account) {
		String url = String.format(BASE_URL, "/openapiv2/user/", "userId", "bp")
		doWithURL(account.tokenInstance, url, "Blood Pressure Data")
	}

	JSONObject getDataForBloodPressure(OAuthAccount account) {
		String url = String.format(BASE_URL, "/openapiv2/user/", "userId", "spo2")
		doWithURL(account.tokenInstance, url, "Blood Pressure Data")
	}

	JSONObject getDataForSleep(OAuthAccount account) {
		String url = String.format(BASE_URL, "/openapiv2/user/", "userId", "sleep")
		doWithURL(account.tokenInstance, url, "Sleep Data")
	}

	JSONObject getDataForWeight(OAuthAccount account) {
		String url = String.format(BASE_URL, "/openapiv2/user/", "userId", "weight")
		doWithURL(account.tokenInstance, url, "Weight Data")
	}

	JSONObject getUserProfile(OAuthAccount account) {
		getUserProfile(account.tokenInstance)
	}

	JSONObject getUserProfile(Token tokenInstance) {
		String url = String.format(BASE_URL, "/openapiv2/user/", "userId", "")
		doWithURL(tokenInstance, url, "Profile Info")
	}

	void poll() {
		OAuthAccount.findAllByTypeId(IHEALTH_ID).each {
			poll(it)
		}
	}

	Map poll(OAuthAccount account) {
		boolean success = true
		long curiousUserId = account.userId

		JSONObject bloodPressureData = getDataForBloodPressure(account)
		JSONObject bloodPressureDataList = bloodPressureData.BPDataList
		int unit = bloodPressureData.BPUnit
		String description
		String date
		BigDecimal value
		String units
		int amountPrecision

		Entry entry = Entry.create(curiousUserId, date, 0, description, value, units, "(IHealth)", SET_NAME, amountPrecision)

		return [success: success]
	}

}
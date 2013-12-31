package us.wearecurio.services

import static us.wearecurio.model.OAuthAccount.*
import static us.wearecurio.thirdparty.TagUnitMap.*
import grails.converters.JSON
import grails.util.Environment

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.TimeZoneId

class IHealthDataService {

	static final String BASE_URL = "http://sandboxapi.ihealthlabs.com/openapiv2/user/%s%s.json/?client_id=%s&client_secret=%s&access_token=%s&redirect_uri=%s&sc=&sv=&locale=en_US"
	static final String COMMENT = "(IHealth)"
	static final String MOVES_SET_NAME = "ihealth import"
	static final String SET_NAME = "ihealth import"

	def grailsApplication
	def oauthService

	JSONObject doWithURL(OAuthAccount account, String type, Map args = [:]) {
		Token tokenInstance = account.tokenInstance
		ConfigObject ihealthConfig = grailsApplication.config.oauth.providers.ihealth

		String pathName = args.pathName
		String userId = account.accountId
		String clientId = ihealthConfig.key
		String clientSecret = ihealthConfig.secret
		String callbackURL = ihealthConfig.callback

		String url = String.format(BASE_URL, userId, pathName, clientId, clientSecret, tokenInstance.token, callbackURL)

		if(pathName && account.lastPolled) {	// If asking for other than profile resource
			url += "&START_TIME=" + account.lastPolled.time
		}

		Response apiResponse = oauthService.getIHealthResource(tokenInstance, url)

		log.debug "Received [$type] response from IHealth API with code [$apiResponse.code]"
		if (Environment.current == Environment.DEVELOPMENT) {
			log.debug "Received [$type] response from IHealth API with body [$apiResponse.body]"
		}

		JSON.parse(apiResponse.body)
	}

	JSONObject getDataForActivity(OAuthAccount account) {
		doWithURL(account, "Activity Report Data", [pathName: "activity"])
	}

	JSONObject getDataForBloodGlucose(OAuthAccount account) {
		doWithURL(account, "Blood Glucose Data", [pathName: "glucose"])
	}

	JSONObject getDataForBloodOxygen(OAuthAccount account) {
		doWithURL(account, "Blood Pressure Data", [pathName: "spo2"])
	}

	JSONObject getDataForBloodPressure(OAuthAccount account) {
		doWithURL(account, "Blood Pressure Data", [pathName: "bp"])
	}

	JSONObject getDataForSleep(OAuthAccount account) {
		doWithURL(account, "Sleep Data", [pathName: "sleep"])
	}

	JSONObject getDataForWeight(OAuthAccount account) {
		doWithURL(account, "Weight Data", [pathName: "weight"])
	}

	JSONObject getUserProfile(OAuthAccount account) {
		doWithURL(account, "Profile Info", [pathName: ""])
	}

	void poll() {
		OAuthAccount.findAllByTypeId(IHEALTH_ID).each {
			poll(it)
		}
	}

	Map poll(OAuthAccount account) {
		boolean success = true
		int amountPrecision = 2
		long curiousUserId = account.userId

		BigDecimal value
		Date date, now = new Date()
		Entry entry
		String description, units = ""
		TimeZoneId timeZoneId = TimeZoneId.get(1)

		JSONObject bloodGlucoseDataList = getDataForBloodGlucose(account)
		bloodGlucoseDataList.BGDataList.each { bloodGlucoseData ->
			log.debug "BG Data: [$bloodGlucoseData.MDate] [$bloodGlucoseData.BG]"

			units = "mg/dl"
			description = BLOOD_GLUCOSE
			date = new Date(bloodGlucoseData.MDate)
			value = bloodGlucoseData.BG.toBigDecimal()

			entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)
		}

		JSONObject bloodOxygenDataList = getDataForBloodOxygen(account)
		bloodOxygenDataList.BODataList.each { bloodOxygenData ->
			log.debug "BO Data: [$bloodOxygenData.MDate] [$bloodOxygenData.BO]"

			units = ""
			description = BLOOD_OXYGEN
			date = new Date(bloodOxygenData.MDate)
			value = bloodOxygenData.BO.toBigDecimal()

			entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)
		}

		JSONObject bloodPressureDataList = getDataForBloodPressure(account)
		bloodPressureDataList.BPDataList.each { bloodPressureData ->
			log.debug "BP Data: [$bloodPressureData.MDate] [$bloodPressureData.HP] [$bloodPressureData.LP]"

			units = "mmHg"
			description = BLOOD_PRESSURE_SYSTOLIC
			date = new Date(bloodPressureData.MDate)
			value = bloodPressureData.HP.toBigDecimal()
			entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)

			description = BLOOD_PRESSURE_DIASTOLIC
			value = bloodPressureData.LP.toBigDecimal()
			entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)
		}

		JSONObject sleepReportList = getDataForSleep(account)
		sleepReportList.SRDataList.each { sleepData ->
			log.debug "Sleep Data: [$sleepData.StartTime] [$sleepData.FellSleep] [$sleepData.HoursSlept] [$sleepData.Awaken] [$sleepData.SleepEfficiency]"

			units = "mins"
			description = SLEEP
			date = new Date(sleepData.StartTime)
			value = new BigDecimal(sleepData.FellSleep + sleepData.HoursSlept + sleepData.Awaken)

			entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)

			if (sleepData.Awaken) {	// Checking for 0 also
				units = ""
				description = "$SLEEP interruptions"
				value = sleepData.Awaken.toBigDecimal()

				entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)
			}

			if (sleepData.SleepEfficiency) {
				units = ""
				description = "$SLEEP efficiency"
				value = sleepData.SleepEfficiency.toBigDecimal()
				entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)
			}
		}

		JSONObject weightDataList = getDataForWeight(account)
		weightDataList.WeightDataList.each { weightData ->
			log.debug "Weight Data: [$weightData.MDate] [$weightData.WeightValue]"

			units = "lbs"
			description = "weight"
			date = new Date(weightData.MDate)
			value = weightData.WeightValue.toBigDecimal()
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			entry = Entry.create(curiousUserId, date, timeZoneId, description, value, units, COMMENT, SET_NAME, amountPrecision)
		}

		account.lastPolled = now
		account.save()
		return [success: success]
	}

}
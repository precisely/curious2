package us.wearecurio.services

import static us.wearecurio.model.OAuthAccount.*
import grails.converters.JSON
import grails.util.Environment

import java.text.DateFormat
import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.TimeZoneId

class HumanDataService {

	static final String BASE_URL = "https://api.humanapi.co/v1/human/%s"
	static final BigDecimal KG_TO_POUNDS = new BigDecimal(220462, 5)
	static final BigDecimal MM_TO_FEET = new BigDecimal(328084, 8)

	def oauthService

	def doWithURL(Token tokenInstance, String url, String type) {
		Response apiResponse = oauthService.getHumanResource(tokenInstance, url)

		log.debug "Received response from Human API for type [$type] with code [$apiResponse.code]"
		if (Environment.current in [Environment.DEVELOPMENT, Environment.TEST]) {
			log.debug "Received response from Human API for type [$type] with body [$apiResponse.body]"
			println "Received response from Human API for type [$type] with body [$apiResponse.body]"
		}

		JSON.parse(apiResponse.body)
	}

	JSONArray getDataForBloodGlucode(OAuthAccount account) {
		String url = String.format(BASE_URL, "blood_glucose/readings")
		doWithURL(account.tokenInstance, url, "Blood Glucose")
	}

	JSONArray getDataForBloodPressure(OAuthAccount account) {
		String url = String.format(BASE_URL, "blood_pressure/readings")
		doWithURL(account.tokenInstance, url, "Blood Pressure")
	}

	JSONArray getDataForBodyFat(OAuthAccount account) {
		String url = String.format(BASE_URL, "body_fat/readings")
		doWithURL(account.tokenInstance, url, "Body Fat")
	}

	JSONArray getDataForBodyMassIndex(OAuthAccount account) {
		String url = String.format(BASE_URL, "bmi/readings")
		doWithURL(account.tokenInstance, url, "Body Mass Index")
	}

	JSONArray getDataForHeartRate(OAuthAccount account) {
		String url = String.format(BASE_URL, "heart_rate/readings")
		doWithURL(account.tokenInstance, url, "Heart Rate")
	}

	JSONArray getDataForHeight(OAuthAccount account) {
		String url = String.format(BASE_URL, "height/readings")
		doWithURL(account.tokenInstance, url, "Height")
	}

	JSONObject getDataForSleeps(OAuthAccount account) {
		String url = String.format(BASE_URL, "sleeps/summary")
		doWithURL(account.tokenInstance, url, "Sleeps")
	}

	JSONArray getDataForWeight(OAuthAccount account) {
		String url = String.format(BASE_URL, "weight/readings")
		doWithURL(account.tokenInstance, url, "Weight")
	}

	JSONObject getUserProfile(OAuthAccount account) {
		getUserProfile(account.tokenInstance)
	}

	JSONObject getUserProfile(Token tokenInstance) {
		doWithURL(tokenInstance, String.format(BASE_URL, "profile"), "User Profile")
	}

	void poll() {
		OAuthAccount.findAllByTypeId(HUMAN_ID).each {
			poll(it)
		}
	}

	Map poll(OAuthAccount account) {
		boolean success = true
		int amountPrecision = 2
		Date lastPolled = new Date()
		long curiousUserId = account.userId
		TimeZoneId timeZoneId = TimeZoneId.get(1)
		Token tokenInstance = account.tokenInstance

		DateFormat startEndTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

		// Done
		JSONArray bloodGlucoseDataList = getDataForBloodGlucode(account)
		bloodGlucoseDataList.each { bgData ->
			String source = bgData.source
			log.debug "BG Data: [$source] [$bgData.timestamp] [$bgData.value] [$bgData.unit]"

			String units = "mg/dL"
			String description = "blood glucose"
			Date entryDate = startEndTimeFormat.parse(bgData.timestamp)
			BigDecimal value = new BigDecimal(bgData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		// Done
		JSONArray bloodPressureDataList = getDataForBloodPressure(account)
		bloodPressureDataList.each { bpData ->
			String source = bpData.source
			log.debug "BP Data: [$source] [$bpData.timestamp] [$bpData.value] [$bpData.unit]"

			String units = "mmHg"
			String description = "blood pressure diastolic"
			Date entryDate = startEndTimeFormat.parse(bpData.timestamp)
			BigDecimal value = new BigDecimal(bpData.value.diastolic)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)

			description = "blood pressure systolic"
			value = new BigDecimal(bpData.value.systolic)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		// Done
		JSONArray bodyFatDataList = getDataForBodyFat(account)
		bodyFatDataList.each { fatData ->
			String source = fatData.source
			log.debug "Body Fat Data: [$source] [$fatData.timestamp] [$fatData.value] [$fatData.unit]"
			String units = "percent"
			String description = "fat ratio"
			Date entryDate = startEndTimeFormat.parse(fatData.timestamp)
			BigDecimal value = new BigDecimal(fatData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		// Done
		JSONArray bmiDataList = getDataForBodyMassIndex(account)
		bmiDataList.each { bmiData ->
			String source = bmiData.source
			log.debug "BMI Data: [$source] [$bmiData.timestamp] [$bmiData.value] [$bmiData.unit]"

			String units = "kg/m2"
			String description = "body mass index"
			Date entryDate = startEndTimeFormat.parse(bmiData.timestamp)
			BigDecimal value = new BigDecimal(bmiData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		// Done
		JSONArray heartRateDataList = getDataForHeartRate(account)
		heartRateDataList.each { heartRateData ->
			String source = heartRateData.source
			log.debug "Heart Rate Data: [$source] [$heartRateData.timestamp] [$heartRateData.value] [$heartRateData.unit]"

			String units = "bpm"
			String description = "heart rate"
			Date entryDate = startEndTimeFormat.parse(heartRateData.timestamp)
			BigDecimal value = new BigDecimal(heartRateData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		// Done
		JSONArray heightDataList = getDataForHeight(account)
		bmiDataList.each { heartData ->
			String source = heartData.source
			log.debug "Height Data: [$source] [$heartData.timestamp] [$heartData.value] [$heartData.unit]"

			String units = "feet"
			String description = "height"
			Date entryDate = startEndTimeFormat.parse(heartData.timestamp)
			BigDecimal value = new BigDecimal(heartData.value)
			value = value.multiply(MM_TO_FEET).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		JSONObject sleepData = getDataForSleeps(account)
		if(!sleepData.isEmpty()) {
			String source = sleepData.source
			log.debug "Sleep Data: [$source] [$sleepData.startTime] [$sleepData.endTime] [$sleepData.timeAsleep] [$sleepData.timeAwake]"

			String units = "mins"
			String description = "sleep"
			Date entryDate = startEndTimeFormat.parse(sleepData.startTime)
			BigDecimal value = new BigDecimal(sleepData.timeAsleep + sleepData.timeAwake)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)

			units = ""
			description = "sleep interruptions"
			value = new BigDecimal(sleepData.timeAwake)

			entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		// Done
		JSONArray weightDataList = getDataForWeight(account)
		weightDataList.each { weightData ->
			String source = weightData.source
			log.debug "Weight: [$weightData.timestamp] [$weightData.value] [$weightData.unit]"

			String units = "lbs"
			String description = "weight"

			Date entryDate = startEndTimeFormat.parse(weightData.timestamp)
			BigDecimal value = new BigDecimal(weightData.value)
			value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneId, description, value, units, "(${source.capitalize()})", "$source import", amountPrecision)
		}

		account.lastPolled = lastPolled
		account.save()
		[success: success]
	}

}
package us.wearecurio.services

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
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.thirdparty.moves.MovesTagUnitMap

class HumanDataService {

	static final String BASE_URL = "https://api.humanapi.co/v1/human/%s"
	static final BigDecimal KG_TO_POUNDS = new BigDecimal(220462, 5)
	static final BigDecimal MM_TO_FEET = new BigDecimal(328084, 8)
	static final String COMMENT = "(Human-%s)"
	static final String SET_NAME = "human %s import"

	def movesDataService
	def oauthService

	def doWithURL(Token tokenInstance, String url, String type, Map args = [:]) {
		if (args.createdSince) {
			url += "?created_since=" + args.createdSince.format("yyyyMMdd'T'HHmmss'Z'")
		}
		Response apiResponse = oauthService.getHumanResource(tokenInstance, url)

		log.debug "Received response from Human API for type [$type] with code [$apiResponse.code]"
		if (Environment.current in [Environment.DEVELOPMENT, Environment.TEST]) {
			log.debug "Received response from Human API for type [$type] with body [$apiResponse.body]"
		}

		JSON.parse(apiResponse.body)
	}

	JSONArray getDataForActivities(OAuthAccount account) {
		String url = String.format(BASE_URL, "activities")
		doWithURL(account.tokenInstance, url, "Activity", [createdSince: account.lastPolled])
	}

	JSONArray getDataForBloodGlucode(OAuthAccount account) {
		String url = String.format(BASE_URL, "blood_glucose/readings")
		doWithURL(account.tokenInstance, url, "Blood Glucose", [createdSince: account.lastPolled])
	}

	JSONArray getDataForBloodPressure(OAuthAccount account) {
		String url = String.format(BASE_URL, "blood_pressure/readings")
		doWithURL(account.tokenInstance, url, "Blood Pressure", [createdSince: account.lastPolled])
	}

	JSONArray getDataForBodyFat(OAuthAccount account) {
		String url = String.format(BASE_URL, "body_fat/readings")
		doWithURL(account.tokenInstance, url, "Body Fat", [createdSince: account.lastPolled])
	}

	JSONArray getDataForBodyMassIndex(OAuthAccount account) {
		String url = String.format(BASE_URL, "bmi/readings")
		doWithURL(account.tokenInstance, url, "Body Mass Index", [createdSince: account.lastPolled])
	}

	JSONArray getDataForHeartRate(OAuthAccount account) {
		String url = String.format(BASE_URL, "heart_rate/readings")
		doWithURL(account.tokenInstance, url, "Heart Rate", [createdSince: account.lastPolled])
	}

	JSONArray getDataForHeight(OAuthAccount account) {
		String url = String.format(BASE_URL, "height/readings")
		doWithURL(account.tokenInstance, url, "Height", [createdSince: account.lastPolled])
	}

	JSONArray getDataForSleeps(OAuthAccount account) {
		String url = String.format(BASE_URL, "sleeps")
		doWithURL(account.tokenInstance, url, "Sleeps", [createdSince: account.lastPolled])
	}

	JSONArray getDataForWeight(OAuthAccount account) {
		String url = String.format(BASE_URL, "weight/readings")
		doWithURL(account.tokenInstance, url, "Weight", [createdSince: account.lastPolled])
	}

	JSONObject getUserProfile(OAuthAccount account) {
		getUserProfile(account.tokenInstance)
	}

	JSONObject getUserProfile(Token tokenInstance) {
		doWithURL(tokenInstance, String.format(BASE_URL, "profile"), "User Profile")
	}

	void poll() {
		OAuthAccount.findAllByTypeId(ThirdParty.HUMAN).each {
			poll(it)
		}
	}

	Map poll(OAuthAccount account) {
		boolean success = true
		int amountPrecision = 2
		long curiousUserId = account.userId

		Date lastPolled = new Date()
		String comment, description, setName, source, units

		Integer timeZoneId = account.timeZoneId ?: User.getTimeZoneId(curiousUserId)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneId)

		Token tokenInstance = account.tokenInstance

		MovesTagUnitMap tagUnitMap = new MovesTagUnitMap()

		DateFormat startEndTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
		startEndTimeFormat.setTimeZone(TimeZoneId.getTimeZoneInstance(timeZoneId))

		JSONArray activitiesDataList = getDataForActivities(account)
		activitiesDataList.each { activityData ->
			source = activityData.sourceType
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())
			log.debug "Activity Data: [$source] [$activityData.startTime] [$activityData.endTime] [$activityData.type] [$activityData.duration] [$activityData.distance] [$activityData.steps]"

			Map args = [setName: setName, comment: comment]
			activityData.distance = activityData.distance * 1000	// Converting to Meters to support MovesTagUnitMap

			movesDataService.processActivity(activityData, curiousUserId, timeZoneIdInstance, activityData.type, startEndTimeFormat, args)
		}

		// Done
		JSONArray bloodGlucoseDataList = getDataForBloodGlucode(account)
		bloodGlucoseDataList.each { bgData ->
			source = bgData.source
			log.debug "BG Data: [$source] [$bgData.timestamp] [$bgData.value] [$bgData.unit]"

			units = "mg/dL"
			description = "blood glucose"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())

			Date entryDate = startEndTimeFormat.parse(bgData.timestamp)
			BigDecimal value = new BigDecimal(bgData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		// Done
		JSONArray bloodPressureDataList = getDataForBloodPressure(account)
		bloodPressureDataList.each { bpData ->
			source = bpData.source
			log.debug "BP Data: [$source] [$bpData.timestamp] [$bpData.value] [$bpData.unit]"

			units = "mmHg"
			description = "blood pressure diastolic"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())

			Date entryDate = startEndTimeFormat.parse(bpData.timestamp)
			BigDecimal value = new BigDecimal(bpData.value.diastolic)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)

			description = "blood pressure systolic"
			value = new BigDecimal(bpData.value.systolic)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		// Done
		JSONArray bodyFatDataList = getDataForBodyFat(account)
		bodyFatDataList.each { fatData ->
			source = fatData.source
			log.debug "Body Fat Data: [$source] [$fatData.timestamp] [$fatData.value] [$fatData.unit]"

			units = "percent"
			description = "fat ratio"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())

			Date entryDate = startEndTimeFormat.parse(fatData.timestamp)
			BigDecimal value = new BigDecimal(fatData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		// Done
		JSONArray bmiDataList = getDataForBodyMassIndex(account)
		bmiDataList.each { bmiData ->
			source = bmiData.source
			log.debug "BMI Data: [$source] [$bmiData.timestamp] [$bmiData.value] [$bmiData.unit]"

			units = "kg/m2"
			description = "body mass index"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())

			Date entryDate = startEndTimeFormat.parse(bmiData.timestamp)
			BigDecimal value = new BigDecimal(bmiData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		// Done
		JSONArray heartRateDataList = getDataForHeartRate(account)
		heartRateDataList.each { heartRateData ->
			source = heartRateData.source
			log.debug "Heart Rate Data: [$source] [$heartRateData.timestamp] [$heartRateData.value] [$heartRateData.unit]"

			units = "bpm"
			description = "heart rate"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())

			Date entryDate = startEndTimeFormat.parse(heartRateData.timestamp)
			BigDecimal value = new BigDecimal(heartRateData.value)
			value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		// Done
		JSONArray heightDataList = getDataForHeight(account)
		bmiDataList.each { heartData ->
			source = heartData.source
			log.debug "Height Data: [$source] [$heartData.timestamp] [$heartData.value] [$heartData.unit]"

			units = "feet"
			description = "height"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())

			Date entryDate = startEndTimeFormat.parse(heartData.timestamp)
			BigDecimal value = new BigDecimal(heartData.value)
			value = value.multiply(MM_TO_FEET).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		JSONArray sleepsDataList = getDataForSleeps(account)
		sleepsDataList.each { sleepData ->
			source = sleepData.source
			log.debug "Sleep Data: [$source] [$sleepData.startTime] [$sleepData.endTime] [$sleepData.timeAsleep] [$sleepData.timeAwake]"

			units = "mins"
			description = "sleep"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())
			Date entryDate = startEndTimeFormat.parse(sleepData.startTime)
			BigDecimal value = new BigDecimal(sleepData.timeAsleep + sleepData.timeAwake)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)

			units = ""
			description = "sleep interruptions"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())
			value = new BigDecimal(sleepData.timeAwake)

			entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		// Done
		JSONArray weightDataList = getDataForWeight(account)
		weightDataList.each { weightData ->
			source = weightData.source
			log.debug "Weight: [$weightData.timestamp] [$weightData.value] [$weightData.unit]"

			units = "lbs"
			description = "weight"
			setName = String.format(SET_NAME, source)
			comment = String.format(COMMENT, source?.capitalize())

			Date entryDate = startEndTimeFormat.parse(weightData.timestamp)
			BigDecimal value = new BigDecimal(weightData.value)
			value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)

			Entry entry = Entry.create(curiousUserId, entryDate, timeZoneIdInstance, description, value, units, comment, setName, amountPrecision)
		}

		account.lastPolled = lastPolled
		account.save()
		[success: success]
	}

}
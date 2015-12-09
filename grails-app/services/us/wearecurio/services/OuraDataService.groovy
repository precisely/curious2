package us.wearecurio.services

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scribe.model.Token
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.oura.OuraTagUnitMap
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat

class OuraDataService extends DataService {

	static final String BASE_URL = "http://localhost:2000"
	static final Map requestHeaderParams = [Authorization: "Bearer 1d49fc35-2af6-477e-8fd4-ab0353a4a76f"]
	static final String SET_NAME = "OURA"
	static final String COMMENT = "(Oura)"
	OuraTagUnitMap tagUnitMap = new OuraTagUnitMap()

	OuraDataService() {
		provider = "Oura"
		typeId = ThirdParty.OURA
		profileURL = BASE_URL + "/api/userProfile/me"
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		return null
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		return null
	}

	@Override
	@Transactional
	void notificationHandler(String notificationData) {
		JSONObject notification = JSON.parse(notificationData)
		if (!notification.userId) {	// At time of subscription
			return
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		// TODO: Check if an object of notification for given date is already present
		Date notificationDate = simpleDateFormat.parse(notification.date);

		Utils.save(new ThirdPartyNotification([collectionType: notification.type, date: notificationDate, ownerId: notification.userId, subscriptionId: "",
				ownerType: "user", typeId: ThirdParty.OURA]))
	}

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataSleep(): account ${account.id} forDay: $forDay refreshAll: $refreshAll"

		String requestUrl = "/api/sleep"

		Date startDate
		return getDataSleep(account, refreshAll, requestUrl, forDay)
	}

	// Overloaded method to support pagination
	Map getDataSleep(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate) {

		// TODO: revisit timezoneId calculation
		Integer timeZoneIdNumber = getTimeZoneId(account)

		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		Long startTime = startDate.getTime()
		Calendar cal = Calendar.getInstance()
		cal.setTime(startDate)
		cal.set(Calendar.HOUR_OF_DAY, 23)
		cal.set(Calendar.MINUTE, 59)
		cal.set(Calendar.SECOND, 59)
		cal.set(Calendar.MILLISECOND, 0)
		Long endTime = cal.getTimeInMillis()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL, "get",
				[dataType: "sleep", startTimestamp: Long.toString((long)(startTime/1000)), endTimestamp: Long.toString((long)(endTime/1000))], requestHeaderParams)

		JSONArray sleepData = apiResponse["data"]

		sleepData.each { sleepEntry ->
			String setName = SET_NAME + "s" + sleepEntry["dateCreated"]

			Date entryDate = new Date(new Long(sleepEntry["eventTime"]) * 1000)

			Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
					e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

			def sleepEntryData = sleepEntry["data"]

			if (sleepEntryData) {
				if (sleepEntryData["bedtime_m"]) {
					tagUnitMap.buildEntry(creationMap, stats, "bedtime_m", Long.parseLong(sleepEntryData["bedtime_m"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}

				if (sleepEntryData["sleep_score"]) {
					tagUnitMap.buildEntry(creationMap, stats, "sleep_score", Long.parseLong(sleepEntryData["sleep_score"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}

				if (sleepEntryData["awake_m"]) {
					tagUnitMap.buildEntry(creationMap, stats, "awake_m", Long.parseLong(sleepEntryData["awake_m"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}

				if (sleepEntryData["rem_m"]) {
					tagUnitMap.buildEntry(creationMap, stats, "rem_m", Long.parseLong(sleepEntryData["rem_m"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}

				if (sleepEntryData["light_m"]) {
					tagUnitMap.buildEntry(creationMap, stats, "light_m", Long.parseLong(sleepEntryData["light_m"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}

				if (sleepEntryData["deep_m"]) {
					tagUnitMap.buildEntry(creationMap, stats, "deep_m", Long.parseLong(sleepEntryData["deep_m"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}
			}
		}

		stats.finish()

		if (sleepData["links"] && sleepData["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			getDataSleep(account, refreshAll, sleepData["links"]["next"])
		}

		return [success: true]
	}

	Map getDataExercise(OAuthAccount account, Date forDay, boolean refreshAll) {
		getDataExercise(account, refreshAll, "/api/exercise", forDay)
	}

	Map getDataExercise(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate) {
		// TODO: revisit timezoneId calculation
		Integer timeZoneIdNumber = getTimeZoneId(account)

		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		Long startTime = startDate.getTime()
		Calendar cal = Calendar.getInstance()
		cal.setTime(startDate)
		cal.set(Calendar.HOUR_OF_DAY, 23)
		cal.set(Calendar.MINUTE, 59)
		cal.set(Calendar.SECOND, 59)
		cal.set(Calendar.MILLISECOND, 0)
		Long endTime = cal.getTimeInMillis()
		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL, "get",
				[dataType: "exercise", startTimestamp: Long.toString((long)(startTime/1000)), endTimestamp: Long.toString((long)(endTime/1000))], requestHeaderParams)

		JSONArray exerciseData = apiResponse["data"]

		exerciseData.each { exerciseEntry ->
			String setName = SET_NAME + "e" + exerciseEntry["dateCreated"]

			Date entryDate = new Date(new Long(exerciseEntry["eventTime"]) * 1000)

			Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
					e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

			def exerciseEntryData = exerciseEntry["data"]
			if (exerciseEntryData) {
				Long duration = Long.parseLong(exerciseEntryData["duration_m"].toString())
				if (exerciseEntryData["classification"] == "rest") {
					tagUnitMap.buildEntry(creationMap, stats, "classification_rest", duration, userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				} else if (exerciseEntryData["classification"] == "light") {
					tagUnitMap.buildEntry(creationMap, stats, "classification_light", duration, userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				} else if (exerciseEntryData["classification"] == "sedentary") {
					tagUnitMap.buildEntry(creationMap, stats, "classification_sedentary", duration, userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}  else if (exerciseEntryData["classification"] == "moderate") {
					tagUnitMap.buildEntry(creationMap, stats, "classification_moderate", duration, userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}
			}
		}

		stats.finish()

		if (exerciseData["links"] && exerciseData["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			getDataExercise(account, refreshAll, exerciseData["links"]["next"])
		}

		return [success: true]
	}

	Map getDataActivity(OAuthAccount account, Date forDay, boolean refreshAll) {
		getDataActivity(account, refreshAll, "/api/activity", forDay)
	}

	Map getDataActivity(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate) {
		// TODO: revisit timezoneId calculation
		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat shortDateParser = new SimpleDateFormat("yyyyMMdd", Locale.US)
		shortDateParser.setTimeZone(timeZone)

		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		Long startTime = startDate.getTime()
		Calendar cal = Calendar.getInstance()
		cal.setTime(startDate)
		cal.set(Calendar.HOUR_OF_DAY, 23)
		cal.set(Calendar.MINUTE, 59)
		cal.set(Calendar.SECOND, 59)
		cal.set(Calendar.MILLISECOND, 0)
		Long endTime = cal.getTimeInMillis()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL, "get",
				[dataType: "activity", startTimestamp: Long.toString((long)(startTime/1000)), endTimestamp: Long.toString((long)(endTime/1000))], requestHeaderParams)

		JSONArray activityData = apiResponse["data"]

		activityData.each { activityEntry ->
			String setName = SET_NAME + "ac" + activityEntry["dateCreated"]

			Date entryDate = new Date(new Long(activityEntry["eventTime"]) * 1000)

			Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
					e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

			def exerciseEntryData = activityEntry["data"]
			if (exerciseEntryData) {
				if (exerciseEntryData["non_wear_m"]) {
					tagUnitMap.buildEntry(creationMap, stats, "non_wear_m", Long.parseLong(exerciseEntryData["non_wear_m"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}
				if (exerciseEntryData["steps"]) {
					tagUnitMap.buildEntry(creationMap, stats, "steps", Long.parseLong(exerciseEntryData["steps"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}
				if (exerciseEntryData["eq_meters"]) {
					tagUnitMap.buildEntry(creationMap, stats, "eq_meters", Long.parseLong(exerciseEntryData["eq_meters"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}
				if (exerciseEntryData["active_cal"]) {
					tagUnitMap.buildEntry(creationMap, stats, "active_cal", Long.parseLong(exerciseEntryData["active_cal"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}
				if (exerciseEntryData["total_cal"]) {
					tagUnitMap.buildEntry(creationMap, stats, "total_cal", Long.parseLong(exerciseEntryData["total_cal"].toString()), userId, timeZoneIdNumber,
							entryDate, COMMENT, setName)
				}
			}
		}

		stats.finish()

		if (activityData["links"] && activityData["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			getDataActivity(account, refreshAll, activityData["links"]["next"])
		}

		return [success: true]
	}
}

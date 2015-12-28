package us.wearecurio.services

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scribe.model.Token
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.datetime.DateUtils
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

	static final String BASE_URL = "https://ouracloud.ouraring.com"
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
	List<ThirdPartyNotification> notificationHandler(String notificationData) {
		JSONObject notification = JSON.parse(notificationData)
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date notificationDate = simpleDateFormat.parse(notification.date);

		ThirdPartyNotification thirdPartyNotification = new ThirdPartyNotification([collectionType: notification.type, date: notificationDate, ownerId: notification.userId, subscriptionId: "",
				ownerType: "user", typeId: ThirdParty.OURA])
		if (Utils.save(thirdPartyNotification)) {
			return [thirdPartyNotification]
		}
		return []
	}

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		log.debug "getDataSleep(): account ${account.id} forDay: $forDay refreshAll: $refreshAll"

		String requestUrl = "/api/sleep"

		return getDataSleep(account, refreshAll, requestUrl, forDay, 1)
	}

	// Overloaded method to support pagination
	Map getDataSleep(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate, int pageNumber) throws InvalidAccessTokenException {
		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		Long startTime = startDate.getTime()
		Long endTime = DateUtils.getEndOfTheDay(startDate)

		// Dividing by 1000 to convert time into seconds, oura cloud stores data times in seconds
		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL, "get",
				[dataType: "sleep", startTimestamp: Long.toString((long)(startTime/1000)), endTimestamp: Long.toString((long)(endTime/1000))])

		JSONArray sleepData = apiResponse["data"]

		// pageNumber is only being used for changing the setName so that previous entries for the same date do not get deleted because of the pagination
		String setName = SET_NAME + "s" + startDate + "-" + pageNumber
		Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
				e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

		sleepData.each { sleepEntry ->
			Date entryDate = new Date(new Long(sleepEntry["eventTime"]) * 1000)
			Integer timeZoneIdNumber = sleepEntry["timeZone"] == "null" ? getTimeZoneId(account) : TimeZoneId.look(sleepEntry["timeZone"]).id
			def sleepEntryData = sleepEntry["data"]

			if (sleepEntryData) {
				["bedtime_m", "sleep_score", "awake_m", "rem_m", "light_m", "deep_m"].each { key ->
					if (sleepEntryData[key]) {
						tagUnitMap.buildEntry(creationMap, stats, key, Long.parseLong(sleepEntryData[key].toString()), userId, timeZoneIdNumber,
								entryDate, COMMENT, setName)
					}
				}
			}
		}

		if (!stats.finish()) {
			return [success: false]
		}

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get sleep data for paginated URL"
			return getDataSleep(account, refreshAll, apiResponse["links"]["nextPageURL"].toString(), startDate, ++pageNumber)
		}

		return [success: true]
	}

	Map getDataExercise(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		getDataExercise(account, refreshAll, "/api/exercise", forDay, 1)
	}

	Map getDataExercise(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate, int pageNumber) throws InvalidAccessTokenException {
		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		Long startTime = startDate.getTime()
		Long endTime = DateUtils.getEndOfTheDay(startDate)

		// Dividing by 1000 to convert time into seconds, oura cloud stores data times in seconds
		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL, "get",
				[dataType: "exercise", startTimestamp: Long.toString((long)(startTime/1000)), endTimestamp: Long.toString((long)(endTime/1000))])
		JSONArray exerciseData = apiResponse["data"]

		// pageNumber is only being used for changing the setName so that previous entries for the same date do not get deleted because of the pagination
		String setName = SET_NAME + "e" + startDate + "-" + pageNumber
		Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
				e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

		JSONObject previousEntry = null

		exerciseData.each { exerciseEntry ->
			if (exerciseEntry["data"]) {
				if (!previousEntry) {
					previousEntry = exerciseEntry
					previousEntry["duration_m"] = Long.parseLong(exerciseEntry["data"]["duration_m"].toString())
				} else {
					Long duration = Long.parseLong(exerciseEntry["data"]["duration_m"].toString())
					if (previousEntry["data"]["classification"] == exerciseEntry["data"]["classification"]) {
						previousEntry["data"]["duration_m"] += duration
					} else {
						buildExerciseEntry(creationMap, stats, previousEntry, userId, setName, account)
						previousEntry = exerciseEntry
						previousEntry["data"]["duration_m"] = duration
					}
				}
			}
		}

		if (previousEntry) {
			buildExerciseEntry(creationMap, stats, previousEntry, userId, setName, account)
			previousEntry = null
		}

		if (!stats.finish()) {
			return [success: false]
		}

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get exercise data for paginated URL"
			return getDataExercise(account, refreshAll, apiResponse["links"]["nextPageURL"].toString(), startDate, ++pageNumber)
		}

		return [success: true]
	}


	private void buildExerciseEntry(EntryCreateMap creationMap, EntryStats stats, JSONObject exerciseEntry, Long userId, String setName, OAuthAccount account) {
		def exerciseEntryData = exerciseEntry["data"]
		Date entryDate = new Date(new Long(exerciseEntry["eventTime"]) * 1000)
		Integer timeZoneIdNumber = exerciseEntry["timeZone"] == "null" ? getTimeZoneId(account) : TimeZoneId.look(exerciseEntry["timeZone"]).id

		if (exerciseEntryData["classification"] == "rest") {
			tagUnitMap.buildEntry(creationMap, stats, "classification_rest", exerciseEntryData["duration_m"], userId, timeZoneIdNumber,
					entryDate, COMMENT, setName)
		} else if (exerciseEntryData["classification"] == "light") {
			tagUnitMap.buildEntry(creationMap, stats, "classification_light", exerciseEntryData["duration_m"], userId, timeZoneIdNumber,
					entryDate, COMMENT, setName)
		} else if (exerciseEntryData["classification"] == "sedentary") {
			tagUnitMap.buildEntry(creationMap, stats, "classification_sedentary", exerciseEntryData["duration_m"], userId, timeZoneIdNumber,
					entryDate, COMMENT, setName)
		}  else if (exerciseEntryData["classification"] == "moderate") {
			tagUnitMap.buildEntry(creationMap, stats, "classification_moderate", exerciseEntryData["duration_m"], userId, timeZoneIdNumber,
					entryDate, COMMENT, setName)
		}
	}

	Map getDataActivity(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		getDataActivity(account, refreshAll, "/api/activity", forDay, 1)
	}

	Map getDataActivity(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate, int pageNumber) throws InvalidAccessTokenException {
		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		Long startTime = startDate.getTime()

		Long endTime = DateUtils.getEndOfTheDay(startDate)

		// Dividing by 1000 to convert time into seconds, oura cloud stores data times in seconds
		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL, "get",
				[dataType: "activity", startTimestamp: Long.toString((long)(startTime/1000)), endTimestamp: Long.toString((long)(endTime/1000))])

		JSONArray activityData = apiResponse["data"]

		// pageNumber is only being used for changing the setName so that previous entries for the same date do not get deleted because of the pagination
		String setName = SET_NAME + "ac" + startDate + "-" + pageNumber
		Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
				e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

		activityData.each { activityEntry ->
			Date entryDate = new Date(new Long(activityEntry["eventTime"]) * 1000)
			Integer timeZoneIdNumber = activityEntry["timeZone"] == "null" ? getTimeZoneId(account): TimeZoneId.look(activityEntry["timeZone"]).id

			def exerciseEntryData = activityEntry["data"]
			if (exerciseEntryData) {
				["non_wear_m", "steps", "eq_meters", "active_cal", "total_cal"].each { key ->
					if (exerciseEntryData[key]) {
						tagUnitMap.buildEntry(creationMap, stats, key, Long.parseLong(exerciseEntryData[key].toString()), userId, timeZoneIdNumber,
								entryDate, COMMENT, setName)
					}
				}
			}
		}

		if (!stats.finish()) {
			return [success: false]
		}

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get activity data for paginated URL"
			getDataActivity(account, refreshAll, apiResponse["links"]["nextPageURL"].toString(), startDate, ++pageNumber)
		}

		return [success: true]
	}
}

package us.wearecurio.services
import grails.converters.JSON
import groovyx.net.http.URIBuilder
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
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
import us.wearecurio.thirdparty.oura.OuraApi
import us.wearecurio.thirdparty.oura.OuraTagUnitMap
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat

class OuraDataService extends DataService {

	static final String BASE_URL = OuraApi.BASE_URL
	static final String SET_NAME = "OURA"
	static final String SOURCE_NAME = "Oura Data"
	static final String COMMENT = "(Oura)"
	OuraTagUnitMap tagUnitMap = new OuraTagUnitMap()

	OuraDataService() {
		provider = "Oura"
		typeId = ThirdParty.OURA
		profileURL = BASE_URL + "/api/userProfile/me"
		TagUnitMap.addSourceSetIdentifier(SET_NAME, SOURCE_NAME)
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
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
		Date notificationDate = simpleDateFormat.parse(notification.date)

		ThirdPartyNotification thirdPartyNotification = new ThirdPartyNotification([collectionType: notification.type,
				date: notificationDate, ownerId: notification.userId, subscriptionId: "",
				ownerType: "user", typeId: ThirdParty.OURA])

		if (Utils.save(thirdPartyNotification)) {
			return [thirdPartyNotification]
		}
		return []
	}

	void getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		getDataSleep(account, refreshAll, getRequestURL("sleep", forDay), forDay, 1)
	}

	// Overloaded method to support pagination
	void getDataSleep(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate, int pageNumber) throws InvalidAccessTokenException {
		log.debug "Import sleep data for user id $account.userId for day $startDate"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray sleepData = apiResponse["data"]

		// pageNumber is only being used for changing the setName so that previous entries for the same date do not get deleted because of the pagination
		String setName = SET_NAME + "s" + startDate + "-" + pageNumber
		unsetOldEntries(userId, setName)

		sleepData.each { sleepEntry ->
			Date entryDate = convertTimeToDate(sleepEntry)
			Integer timeZoneIdNumber = getTimeZoneId(account, sleepEntry)
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

		stats.finish()

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get sleep data for paginated URL"
			getDataSleep(account, refreshAll, apiResponse["links"]["nextPageURL"].toString(), startDate, ++pageNumber)
		}
	}

	void getDataExercise(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		getDataExercise(account, refreshAll, getRequestURL("exercise", forDay), forDay, 1)
	}

	Map getDataExercise(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate, int pageNumber) throws InvalidAccessTokenException {
		log.debug "Import extercise data for user id $account.userId for day $startDate"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray exerciseData = apiResponse["data"]

		// pageNumber is only being used for changing the setName so that previous entries for the same date do not get deleted because of the pagination
		String setName = SET_NAME + "e" + startDate + "-" + pageNumber
		unsetOldEntries(userId, setName)

		JSONObject previousEntry = null

		int thresholdDuration = 15 * 60		// 15 minutes or 900 seconds

		exerciseData.each { currentEntry ->
			if (!currentEntry["data"]) {
				return		// Continue looping
			}

			Long currentEntryDuration = Long.parseLong(currentEntry["data"]["duration_m"].toString())  // in minutes
			currentEntry["data"]["duration_m"] = currentEntryDuration

			if (!previousEntry) {
				previousEntry = currentEntry
				return		// Continue looping
			}

			// If type of previous entry is same as the current entry
			if (previousEntry["data"]["classification"] == currentEntry["data"]["classification"]) {
				// Then we have to merge the data of both the entries

				// In seconds
				long previousEntryStartTime = previousEntry["eventTime"]
				// In seconds
				long previousEntryEndTime = (previousEntryStartTime + (previousEntry["data"]["duration_m"] * 60))
				// In seconds
				long currentEntryStartTime = currentEntry["eventTime"]
				// In seconds
				long durationBeEntries = currentEntryStartTime - previousEntryEndTime

				/*
				 * There could be a situation that we receive two contiguous entries where previous entry
				 * might be of morning and the current entry might be of evening. So adding a threshold value
				 * of 15 minutes that the duration between end of the previous entry and start of current entry
				 * should be between 15 minutes.
				 */
				if (durationBeEntries <= thresholdDuration) {
					previousEntry["data"]["duration_m"] += currentEntryDuration
					return		// Continue looping
				}
			}

			buildExerciseEntry(creationMap, stats, previousEntry, userId, setName, account)
			previousEntry = currentEntry
		}

		if (previousEntry) {
			buildExerciseEntry(creationMap, stats, previousEntry, userId, setName, account)
			previousEntry = null
		}

		stats.finish()

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get exercise data for paginated URL"
			getDataExercise(account, refreshAll, apiResponse["links"]["nextPageURL"].toString(), startDate, ++pageNumber)
		}
	}

	private void buildExerciseEntry(EntryCreateMap creationMap, EntryStats stats, JSONObject exerciseEntry, Long userId, String setName, OAuthAccount account) {
		def exerciseEntryData = exerciseEntry["data"]
		Date entryDate = convertTimeToDate(exerciseEntry)
		Integer timeZoneIdNumber = getTimeZoneId(account, exerciseEntry)

		Long amount = exerciseEntryData["duration_m"]
		String tagName = "classification_" + exerciseEntryData["classification"]
		tagUnitMap.buildEntry(creationMap, stats, tagName, amount, userId, timeZoneIdNumber, entryDate, COMMENT, setName)
	}

	void getDataActivity(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		getDataActivity(account, refreshAll, getRequestURL("activity", forDay), forDay, 1)
	}

	void getDataActivity(OAuthAccount account, boolean refreshAll, String requestURL, Date startDate, int pageNumber) throws InvalidAccessTokenException {
		log.debug "Import activity data for user id $account.userId for day $startDate"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray activityData = apiResponse["data"]

		// pageNumber is only being used for changing the setName so that previous entries for the same date do not get deleted because of the pagination
		String setName = SET_NAME + "ac" + startDate + "-" + pageNumber
		unsetOldEntries(userId, setName)

		activityData.each { activityEntry ->
			Date entryDate = convertTimeToDate(activityEntry)
			Integer timeZoneIdNumber = getTimeZoneId(account, activityEntry)

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

		stats.finish()

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get activity data for paginated URL"
			getDataActivity(account, refreshAll, apiResponse["links"]["nextPageURL"].toString(), startDate, ++pageNumber)
		}
	}

	/**
	 * Construct request URL with query string for API call for the given date.
	 * @param dataType One of "sleep", "activity", "exercise"
	 * @param startDate Date for which Ouracloud data needs to be pulled
	 */
	String getRequestURL(String dataType, Date startDate) {
		Long startTime = startDate.getTime()
		Long endTime = DateUtils.getEndOfTheDay(startDate)

		URIBuilder builder = new URIBuilder("/api/$dataType")
		builder.addQueryParam("max", 1000)
		builder.addQueryParam("offset", 0)
		// Ouracloud API accepts the timestamp in EPOCH time (i.e. in seconds)
		builder.addQueryParam("startTimestamp", Long.toString((long)(startTime / 1000)))
		builder.addQueryParam("endTimestamp", Long.toString((long)(endTime / 1000)))

		return builder.toString()
	}

	Integer getTimeZoneId(OAuthAccount account, Map rawEntry) {
		return rawEntry["timeZone"] ? TimeZoneId.look(rawEntry["timeZone"]).id : getTimeZoneId(account)
	}

	/**
	 * For a specific record from Ouracloud, the time of that record/event is the EPOCH time. This helper method
	 * converts that time to a Date object. Keeping it as a method to keep it DRY and also prevent it from missing at
	 * ony place.
	 * @param rawEntry Raw event/entry data received from Ouracloud API.
	 * @return Converted date object
	 */
	Date convertTimeToDate(Map rawEntry) {
		return new Date(new Long(rawEntry["eventTime"]) * 1000)
	}
}

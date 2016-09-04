package us.wearecurio.services

import grails.converters.JSON
import groovy.time.TimeCategory
import groovyx.net.http.URIBuilder
import java.text.SimpleDateFormat
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.datetime.DateUtils
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.services.DataService.DataRequestContext
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.thirdparty.oura.OuraApi
import us.wearecurio.thirdparty.oura.OuraTagUnitMap
import us.wearecurio.utility.Utils

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
	Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug("getDataDefault $account.id startDate: $startDate endDate: $endDate refreshAll: $refreshAll")

		if (!startDate) {
			DateTimeZone timezone = TimeZoneId.fromId(account.timeZoneId).toDateTimeZone()
			DateTime now = new DateTime().withZone(timezone)
			LocalDateTime localTime = now.toLocalDateTime()
			startDate = localTime.toDate().clearTime()
			log.debug "Start date $startDate"
		}
		endDate = endDate ?: DateUtils.getEndOfTheDay()

		getDataSleep(account, startDate, endDate, false, context)
		getDataExercise(account, startDate, endDate, false, context)
		getDataActivity(account, startDate, endDate, false, context)

		Utils.save(account, true)
		[success: true]
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		return null
	}

	@Override
	@Transactional
	List<ThirdPartyNotification> notificationHandler(String notificationData) {
		notificationHandler(JSON.parse(notificationData))
	}

	@Transactional
	List<ThirdPartyNotification> notificationHandler(Object notifications) {
		List thirdPartyNotificationList = []

		if (!(notifications instanceof JSONArray)) {
			notifications = [notifications]
		}
		
		ThirdPartyNotification.withNewSession {
			notifications.each { notification ->

				thirdPartyNotificationList.add(saveThirdPartyNotification(notification))
			}
		}
		
		return thirdPartyNotificationList
	}

	ThirdPartyNotification saveThirdPartyNotification(Object notification) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
		Date notificationDate = simpleDateFormat.parse(notification.date)

		ThirdPartyNotification thirdPartyNotification = new ThirdPartyNotification([collectionType: notification.type,
				date: notificationDate, ownerId: notification.userId, subscriptionId: notification.subscriptionId ?: "",
				ownerType: "user", typeId: ThirdParty.OURA])

		if (!Utils.save(thirdPartyNotification)) {
			return
		}

		thirdPartyNotification
	}

	void getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Get sleep data for account $account.id for $forDay"

		getDataSleep(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	void getDataSleep(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug "Get sleep data account $account.id startDate: $startDate endDate $endDate refreshAll $refreshAll"

		endDate = endDate ?: DateUtils.getEndOfTheDay()

		if (refreshAll) {
			// Unsetting all historical data. Starting with the device set name prefix
			unsetAllOldEntries(account.userId, SET_NAME)
		}

		// Backward support for old set name
		unsetOldEntries(account.userId, getOldSetNames("s", startDate, endDate), context.alreadyUnset)

		getDataSleep(account, getRequestURL("sleep", startDate, endDate), context)
	}

	// Overloaded method to support pagination
	void getDataSleep(OAuthAccount account, String requestURL, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Import sleep data for user id $account.userId"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray sleepData = apiResponse["data"]

		sleepData.each { sleepEntry ->
			Date entryDate = convertTimeToDate(sleepEntry)
			String setName = getSetName("sleep", entryDate)

			Integer timeZoneIdNumber = getTimeZoneId(account, sleepEntry)
			def sleepEntryData = sleepEntry["data"]

			if (!sleepEntryData) {
				return
			}

			// In Minutes
			Integer totalSlept = sleepEntryData["bedtime_m"].toInteger()
			Date sleepEnd
			use(TimeCategory) {
				sleepEnd = entryDate + totalSlept.minutes
			}

			log.debug "Sleep start $entryDate, total bedtime $totalSlept minutes, sleep end $sleepEnd"

			["bedtime_m", "sleep_score", "awake_m", "rem_m", "light_m", "deep_m"].each { key ->
				if (sleepEntryData[key]) {
					tagUnitMap.buildEntry(creationMap, stats, key, new BigDecimal(sleepEntryData[key].toString()),
							userId, timeZoneIdNumber, sleepEnd, COMMENT, setName, context)
				}
			}
		}

		account.markLastPolled(stats.lastDate)
		stats.finish()

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get sleep data for paginated URL"
			getDataSleep(account, apiResponse["links"]["nextPageURL"].toString(), context)
		}
	}

	void getDataExercise(OAuthAccount account, Date forDay, boolean refreshAll, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Get exercise data for account $account.id for $forDay"

		getDataExercise(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	void getDataExercise(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug "Get exercise data account $account.id startDate: $startDate endDate $endDate refreshAll $refreshAll"

		endDate = endDate ?: DateUtils.getEndOfTheDay()

		if (refreshAll) {
			// Unsetting all historical data. Starting with the device set name prefix
			unsetAllOldEntries(account.userId, SET_NAME)
		}

		// Backward support for old set name
		unsetOldEntries(account.userId, getOldSetNames("e", startDate, endDate), context.alreadyUnset)

		getDataExercise(account, getRequestURL("exercise", startDate, endDate), context)
	}

	Map getDataExercise(OAuthAccount account, String requestURL, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Import extercise data for user id $account.userId"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray exerciseData = apiResponse["data"]

		JSONObject previousEntry = null

		int thresholdDuration = 15 * 60		// 15 minutes or 900 seconds

		exerciseData.each { currentEntry ->
			if (!currentEntry["data"]) {
				log.debug "No data found in current entry"
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
					log.debug "Found two contiguous entries. Merging"
					previousEntry["data"]["duration_m"] += currentEntryDuration
					return		// Continue looping
				}
			}

			buildExerciseEntry(creationMap, stats, previousEntry, userId, account, context)
			previousEntry = currentEntry
		}

		if (previousEntry) {
			buildExerciseEntry(creationMap, stats, previousEntry, userId, account, context)
			previousEntry = null
		}

		account.markLastPolled(stats.lastDate)
		stats.finish()

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get exercise data for paginated URL"
			getDataExercise(account, apiResponse["links"]["nextPageURL"].toString(), context)
		}
	}

	private void buildExerciseEntry(EntryCreateMap creationMap, EntryStats stats, JSONObject exerciseEntry,
			Long userId, OAuthAccount account, DataRequestContext context) {
		def exerciseEntryData = exerciseEntry["data"]
		Date entryDate = convertTimeToDate(exerciseEntry)
		Integer timeZoneIdNumber = getTimeZoneId(account, exerciseEntry)

		String setName = getSetName("exercise", entryDate)

		Long amount = exerciseEntryData["duration_m"]
		String tagName = "classification_" + exerciseEntryData["classification"]
		tagUnitMap.buildEntry(creationMap, stats, tagName, amount, userId, timeZoneIdNumber, entryDate, COMMENT, setName, context)
	}

	void getDataActivity(OAuthAccount account, Date forDay, boolean refreshAll, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Get activity data for account $account.id for $forDay"

		getDataActivity(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	void getDataActivity(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug "Get activity data account $account.id startDate: $startDate endDate $endDate refreshAll $refreshAll"

		endDate = endDate ?: DateUtils.getEndOfTheDay()

		if (refreshAll) {
			// Unsetting all historical data. Starting with the device set name prefix
			unsetAllOldEntries(account.userId, SET_NAME)
		}

		// Backward support for old set name
		unsetOldEntries(account.userId, getOldSetNames("ac", startDate, endDate), context.alreadyUnset)

		getDataActivity(account, getRequestURL("activity", startDate, endDate), context)
	}

	void getDataActivity(OAuthAccount account, String requestURL, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Import activity data for user id $account.userId"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray activityData = apiResponse["data"]

		activityData.each { activityEntry ->
			Date entryDate = convertTimeToDate(activityEntry)
			String setName = getSetName("activity", entryDate)
			Integer timeZoneIdNumber = getTimeZoneId(account, activityEntry)

			def exerciseEntryData = activityEntry["data"]
			if (exerciseEntryData) {
				["non_wear_m", "steps", "eq_meters", "active_cal", "total_cal"].each { key ->
					if (exerciseEntryData[key]) {
						tagUnitMap.buildEntry(creationMap, stats, key, Long.parseLong(exerciseEntryData[key].toString()), userId, timeZoneIdNumber,
								entryDate, COMMENT, setName, context)
					}
				}
			}
		}

		account.markLastPolled(stats.lastDate)
		stats.finish()

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get activity data for paginated URL"
			getDataActivity(account, apiResponse["links"]["nextPageURL"].toString(), context)
		}
	}

	/**
	 * Get set name which will be used while creating entries. This set name is constructed with the
	 * device name prefix + the type of the data (like sleep, activity or exercise) + the event date of the summary
	 * data received from the API.
	 *
	 * @param type Type of the data to use in the set name
	 * @param summaryDate Event date of the summary data
	 * @return The new set name as described above like "OURA activity 03/01/2016 00:00:00" for activity or
	 * "OURA sleep 03/02/2016 00:00:00" for sleep data
	 */
	String getSetName(String type, Date summaryDate) {
		return SET_NAME + " " + type + " " + summaryDate.format("MM/dd/yyyy HH:mm:ss")
	}

	/**
	 * Get old set name which will is used for backward compatibility by removing old entries with this set name.
	 * This set name is not used for creating new entries. Use the method {@link "getSetName" getSetName} instead.
	 *
	 * @param type Type of the data to use in the set name (like "s", "ac", "e")
	 * @return The old set name like "OURAac2016-03-01 00:00:00.0-1" for activity or "OURAs2016-03-02 00:00:00.0-1"
	 * for sleep data.
	 */
	String getOldSetName(String type, Date forDay) {
		/*
		 * In old set name, we were appending the pagination number to the set name to prevent data from deleting
		 * when we process the next page. We are hard coding pagination to just "1" here since all the previous
		 * entries (with old set name) were imported in the first page only (as we are pulling 1000 records from Oura
		 * at a time).
		 */
		return SET_NAME + type + forDay.clearTime().format("yyyy-MM-dd HH:mm:ss.S") + "-1"
	}

	/**
	 * Get a list of old set names for the given date range. This method basically constructs the set name of each
	 * individual date lies between the given range so that we can remove old entries when we pull data for a same
	 * date again. This is to support backward compatibility.
	 *
	 * @param type Type of the data for old set like "s", "ac", "e"
	 * @param startDate Start date to get list of set names from
	 * @param endDate End date for the list of set names
	 * @return The list of old set names as described above
	 */
	List<String> getOldSetNames(String type, Date startDate, Date endDate) {
		List setNames = []

		for (Date date = startDate; date <= endDate; date = date + 1) {
			setNames << getOldSetName(type, date)
		}
		return setNames
	}

	/**
	 * Construct request URL with query string for API call for the given date.
	 * @param dataType One of "sleep", "activity", "exercise"
	 * @param startDate Start date from which Ouracloud data needs to be pulled
	 * @param endDate End date upto which Ouracloud data needs to be pulled
	 */
	String getRequestURL(String dataType, Date startDate, Date endDate) {
		if (!startDate || !endDate) {
			throw new IllegalArgumentException("Both start & end date are required")
		}

		Long startTime = startDate.getTime()
		Long endTime = endDate.getTime()

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

/**
 * TODO Replace the use of String like "sleep", "s", "acitivity", "a" etc. with this enum to make the code more
 * readable. This enum is not used anywhere currently.
 */
enum OuraDataType {
	SLEEP("s"),
	ACTIVITY("ac"),
	EXERCISE("e")

	final oldSetName

	OuraDataType(String oldSetName) {
		this.oldSetName = oldSetName
	}
}

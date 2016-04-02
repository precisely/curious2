package us.wearecurio.services

import grails.converters.JSON
import groovyx.net.http.URIBuilder
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scribe.model.Token
import us.wearecurio.datetime.DateUtils
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.thirdparty.jawbone.JawboneUpTagUnitMap
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat

class JawboneUpDataService extends DataService {

	static final String BASE_URL = "https://jawbone.com"
	static final String COMMON_BASE_URL = "/nudge/api%s"
	static final String COMMENT = "(Jawbone Up)"
	static final String SET_NAME = "JUP"
	static final String SOURCE_NAME = "Jawbone Up Data"
	
	/**
	 * Helper constant to represent an high activity data means the hourly data which has
	 * active_time greater than 300.
	 *
	 * Used for moves endpoint of JawboneUP
	 */
	static final int NO_ACTIVITY_DATA = 0

	/**
	 * Helper constant to represent an high activity data means the hourly data which has
	 * active_time greater than 300.
	 *
	 * Used for moves endpoint of JawboneUP
	 */
	static final int HIGH_ACTIVITY_DATA = 1

	/**
	 * Helper constant to represent an light or mild activity data means the hourly data which has
	 * active_time less than or equal to 300.
	 *
	 * Used for moves endpoint of JawboneUP
	 */
	static final int LIGHT_ACTIVITY_DATA = 2

	JawboneUpTagUnitMap tagUnitMap = new JawboneUpTagUnitMap()

	JawboneUpDataService() {
		provider = "Jawboneup"
		typeId = ThirdParty.JAWBONE
		profileURL = String.format(BASE_URL + COMMON_BASE_URL, "/users/@me")
		TagUnitMap.addSourceSetIdentifier(SET_NAME, SOURCE_NAME)
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll) throws
			InvalidAccessTokenException {
		log.debug "getDataDefault(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"
		if (!account) {
			throw new IllegalArgumentException("No OAuthAccount instance was passed")
		}

		startDate = startDate ?: account.lastPolled
		endDate = endDate ?: new Date()

		getDataBody(account, startDate, endDate, refreshAll)
		getDataMove(account, startDate, endDate, refreshAll)
		getDataSleep(account, startDate, endDate, refreshAll)

		return [success: true]
	}

	private boolean isRequestSucceeded(JSONObject response) {
		return response && response["meta"] && response["meta"]["code"] == 200
	}

	Map getDataBody(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataBody(): ${account} forDay: $forDay refreshAll: $refreshAll"

		return getDataBody(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll)
	}

	Map getDataBody(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll) {
		log.debug "getDataBody(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"

		beforeGetData(account, startDate, endDate, refreshAll)
		return getDataBody(account, getRequestURL(JawboneUpDataType.BODY, startDate, endDate))
	}

	// Overloaded method to support pagination
	Map getDataBody(OAuthAccount account, String requestURL) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat shortDateParser = new SimpleDateFormat("yyyyMMdd", Locale.US)
		shortDateParser.setTimeZone(timeZone)

		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceeded(apiResponse)) {
			return [success: false]
		}

		JSONObject bodyData = apiResponse["data"]

		bodyData["items"].find { bodyEntry ->
			String setName = getSetName(JawboneUpDataType.BODY, bodyEntry)

			Date entryDate = shortDateParser.parse(bodyEntry["date"].toString())
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			unsetOldEntries(userId, setName)

			tagUnitMap.buildEntry(creationMap, stats, "weight", new BigDecimal((String)bodyEntry["weight"]), userId, timeZoneIdNumber,
					entryDate, COMMENT, setName)

			if (bodyEntry["body_fat"]) {
				tagUnitMap.buildEntry(creationMap, stats, "fatRatio", bodyEntry["body_fat"], userId, timeZoneIdNumber,
						entryDate, COMMENT, setName)
			}

			return false	// continue looping
		}

		stats.finish()

		if (bodyData["links"] && bodyData["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			return getDataBody(account, bodyData["links"]["next"])
		}

		return [success: true]
	}

	Map getDataMove(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataMoves(): ${account} forDay: $forDay refreshAll: $refreshAll"

		return getDataMove(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll)
	}

	Map getDataMove(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll) {
		log.debug "getDataMove(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"

		beforeGetData(account, startDate, endDate, refreshAll)
		return getDataMove(account, getRequestURL(JawboneUpDataType.MOVE, startDate, endDate))
	}

	// Overloaded method to support pagination
	Map getDataMove(OAuthAccount account, String requestURL) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat dateOnlyFormatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
		SimpleDateFormat dateHourFormatter = new SimpleDateFormat("yyyyMMddHH", Locale.US)
		dateOnlyFormatter.setTimeZone(timeZone)
		dateHourFormatter.setTimeZone(timeZone)

		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceeded(apiResponse)) {
			return [success: false]
		}

		JSONObject moveData = apiResponse["data"]

		moveData["items"].find { movesEntry ->
			JSONObject movesDetails = movesEntry["details"]

			if (!movesDetails) {
				return false	// continue looping
			}

			// Raw date received in yyyyMMdd format. Example: 20140910
			String rawDate = movesEntry["date"].toString()

			String setName = getSetName(JawboneUpDataType.MOVE, movesEntry)

			Date entryDate = dateOnlyFormatter.parse(rawDate)
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			unsetOldEntries(userId, setName)

			if (movesDetails["distance"]) {
				tagUnitMap.buildEntry(creationMap, stats, "miles", movesDetails["distance"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}

			if (movesDetails["active_time"]) {
				tagUnitMap.buildEntry(creationMap, stats, "minutes", movesDetails["active_time"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}

			if (movesDetails["steps"]) {
				tagUnitMap.buildEntry(creationMap, stats, "steps", movesDetails["steps"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}

			if (movesDetails["hourly_totals"]) {
				// Used to hold continuous adjacent same type hourly data
				Map<String, JSONObject> sameDataTypeBucket = [:]

				int previousActivityDataType = NO_ACTIVITY_DATA

				(1..23).find { int hour ->
					// Construct key of the hourly_total field. Example: 2014091014
					String key = rawDate + String.format("%02d", hour)	// Two digit hours. Example: 01, 03, 14

					JSONObject hourlyData = movesDetails["hourly_totals"][key]

					// If hourly data of current hour is not available or its active time is 0
					if (!hourlyData || hourlyData == JSONObject.NULL || hourlyData["active_time"] == 0) {
						// Then create entry from the bucket (if any)

						createHourlyEntry(creationMap, stats, userId, timeZoneIdNumber, sameDataTypeBucket,
								previousActivityDataType, dateHourFormatter, COMMENT, setName)

						// And empty the bucket
						sameDataTypeBucket = [:]

						return false	// continue looping
					}

					// Else there is hourly data available for current hour

					int activeTime = hourlyData["active_time"]

					// If active time is greater than 300 then it is high activity else is a mild/light activity
					int currentActivityDataType = activeTime > 300 ? HIGH_ACTIVITY_DATA : LIGHT_ACTIVITY_DATA

					// Grouping continuous data of same type if previous data group is same as current one
					// Or this is first activity data type i.e. previous data type was not available
					if ((previousActivityDataType == NO_ACTIVITY_DATA) || (previousActivityDataType == currentActivityDataType)) {
						sameDataTypeBucket[key] = hourlyData
					} else {
						// Else create entry

						// First create entry for the previous grouped data (if any)
						createHourlyEntry(creationMap, stats, userId, timeZoneIdNumber, sameDataTypeBucket,
								previousActivityDataType, dateHourFormatter, COMMENT, setName)

						// Then empty the bucket
						sameDataTypeBucket = [:]

						// And then add current hourly data to the bucket for next match
						sameDataTypeBucket[key] = hourlyData
					}

					// Set current data type to previous for next iteration
					previousActivityDataType = currentActivityDataType

					return false // continue looping
				}
			}

			return false	// continue looping
		}

		stats.finish()

		if (moveData["links"] && moveData["links"]["next"]) {
			log.debug "Processing get moves data for paginated URL"

			return getDataMove(account, moveData["links"]["next"])
		}

		return [success: true]
	}

	void createHourlyEntry(creationMap, stats, Long userId, timeZoneIdNumber, Map hourlyGroupedData,
			int activityDataType, SimpleDateFormat dateHourFormatter, String comment, String setName) {

		// Either there is no data in bucket or activity data type is not defined
		if (!hourlyGroupedData || activityDataType == NO_ACTIVITY_DATA) {
			log.debug "No hourly data: $hourlyGroupedData, type: $activityDataType"
			return
		}

		// Collect all keys from map i.e. hourly dates: ["2014091013", "2014091014"]
		List<String> groupedHourlyDates = hourlyGroupedData.collect { it.key }

		log.debug "Groped hourly dates: " + groupedHourlyDates

		// Collect all their values
		List<JSONObject> groupHourlyData = hourlyGroupedData.collect { it.value }

		log.debug "Grouped hourly data: " + groupHourlyData

		String tagType = activityDataType == HIGH_ACTIVITY_DATA ? "high" : "lightly"

		// Create entry date for first hour for same continuous data
		Date entryDate = dateHourFormatter.parse(groupedHourlyDates.sort()[0])

		log.debug "Date for grouped data: " + entryDate

		def averageDistance = getAverage(groupHourlyData, "distance")

		Entry entry = tagUnitMap.buildEntry(creationMap, stats, "${tagType}ActiveDistance", averageDistance, userId,
				timeZoneIdNumber, entryDate, COMMENT, setName)

		if (!entry) {
			log.warn "Error creating entry for ${tagType}ActiveDistance"
		}

		def averageActiveTime = getAverage(groupHourlyData, "active_time")

		entry = tagUnitMap.buildEntry(creationMap, stats, "${tagType}ActiveMinutes", averageActiveTime, userId,
				timeZoneIdNumber, entryDate, COMMENT, setName)

		if (!entry) {
			log.warn "Error creating entry for ${tagType}ActiveMinutes"
		}

		def averageStep = getAverage(groupHourlyData, "steps")

		entry = tagUnitMap.buildEntry(creationMap, stats, "${tagType}ActiveSteps", averageStep, userId,
				timeZoneIdNumber, entryDate, COMMENT, setName)

		if (!entry) {
			log.warn "Error creating entry for ${tagType}ActiveSteps"
		}

		entry = tagUnitMap.buildEntry(creationMap, stats, "${tagType}ActiveCalories", averageStep, userId,
				timeZoneIdNumber, entryDate, COMMENT, setName)

		if (!entry) {
			log.warn "Error creating entry for ${tagType}ActiveCalories"
		}
	}

	/**
	 * Get average of any key in the given list of data. It ensures the average if key is missing.
	 * @param groupData the list of map for data
	 * @param key key to collect & get average
	 * @return
	 * 
	 * @example
	 * 
	 * assert getAverage([[name: 'A', count: 2], [name: 'B', count: 3]], "count") == 2.5
	 * If any key is missing
	 * assert getAverage([[name: 'A', count: 2], [name: 'B']], "count") == 1
	 */
	def getAverage(List groupData, String key) {
		return groupData.collect { it[key] ?: 0 }.sum() / groupData.size()
	}

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataSleep(): ${account} forDay: $forDay refreshAll: $refreshAll"

		return getDataSleep(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll)
	}

	Map getDataSleep(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll) {
		log.debug "getDataSleep(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"

		beforeGetData(account, startDate, endDate, refreshAll)
		return getDataSleep(account, getRequestURL(JawboneUpDataType.SLEEP, startDate, endDate))
	}

	// Overloaded method to support pagination
	Map getDataSleep(OAuthAccount account, String requestURL) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
		formatter.setTimeZone(timeZone)

		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceeded(apiResponse)) {
			return [success: false]
		}

		JSONObject sleepData = apiResponse["data"]

		sleepData["items"].find { sleepEntry ->
			JSONObject sleepDetails = sleepEntry["details"]

			if (!sleepDetails) {
				return false	// continue looping
			}

			String setName = getSetName(JawboneUpDataType.SLEEP, sleepEntry)

			Date entryDate = new Date(sleepDetails["asleep_time"].toLong() * 1000)
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			unsetOldEntries(userId, setName)

			if (sleepDetails["duration"]) {
				tagUnitMap.buildEntry(creationMap, stats, "duration", sleepDetails["duration"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}
			if (sleepDetails["awakenings"]) {
				tagUnitMap.buildEntry(creationMap, stats, "awakeningsCount", sleepDetails["awakenings"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}
			if (sleepDetails["awake"]) {
				tagUnitMap.buildEntry(creationMap, stats, "awake", sleepDetails["awake"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}
			if (sleepDetails["quality"]) {
				tagUnitMap.buildEntry(creationMap, stats, "quality", sleepDetails["quality"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}

			return false	// continue looping
		}

		stats.finish()

		if (sleepData["links"] && sleepData["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			return getDataSleep(account, sleepData["links"]["next"])
		}
		return [success: true]
	}

	void beforeGetData(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll) {
		if (!account || !startDate || !endDate) {
			throw new IllegalArgumentException("One of the required argument is missing")
		}

		if (refreshAll) {
			// Un-setting all historical data. Starting with the device set name prefix
			unsetAllOldEntries(account.userId, SET_NAME)
		}
	}

	/**
	 * Overriding default implementation so to send accept header to all requests
	 */
	@Override
	JSONElement getResponse(Token tokenInstance, String requestUrl, String method = "get", Map queryParams = [:],
			Map requestHeaders = [:]) throws InvalidAccessTokenException {
		requestHeaders << ["Accept": "application/json"]
		super.getResponse(tokenInstance, requestUrl, method, queryParams, requestHeaders)
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		getTimeZoneName(account.tokenInstance)
	}

	/**
	 * Construct request URL with query string to make API call for the given date range.
	 * @param type Type of the data
	 * @param startDate Start date from which data needs to be pulled
	 * @param endDate End date upto which data needs to be pulled
	 */
	String getRequestURL(JawboneUpDataType type, Date startDate, Date endDate) {
		if (!startDate || !endDate || !type) {
			throw new IllegalArgumentException("One of the required argument is missing")
		}

		Long startTime = startDate.getTime()
		Long endTime = endDate.getTime()

		URIBuilder builder = new URIBuilder("/users/@me/${type.endpointSuffix}")
		// API accepts the timestamp in EPOCH time (i.e. in seconds)
		builder.addQueryParam("startTimestamp", Long.toString((long)(startTime / 1000)))
		builder.addQueryParam("endTimestamp", Long.toString((long)(endTime / 1000)))

		return String.format(COMMON_BASE_URL, builder.toString())
	}

	/**
	 * Get set name which will be used while creating entries. This set name is constructed with the
	 * device name prefix + the type of the data (like sleep, move or body) + the event date of the summary
	 * data received from the API.
	 *
	 * @param type Type of the data to use in the set name
	 * @param summaryDate Event date of the summary data
	 * @return The new set name as described above like "JUP sleep 20131121" for sleep
	 */
	String getSetName(JawboneUpDataType type, Map summaryData) {
		return SET_NAME + " " + type.name().toLowerCase() + " " + summaryData["date"].toString()
	}

	// Overloaded method
	String getTimeZoneName(Token tokenInstance) throws MissingOAuthAccountException, InvalidAccessTokenException {
		String url = String.format(BASE_URL + COMMON_BASE_URL, "/users/@me/timezone")
		JSONObject timezoneResponse = getResponse(tokenInstance, url)

		if (!isRequestSucceeded(timezoneResponse)) {
			return null
		}

		return timezoneResponse["data"][0]?.tz
	}

	@Override
	List<ThirdPartyNotification> notificationHandler(String notificationData) {
		log.debug "Received notification data: $notificationData"

		if (!notificationData) {
			return
		}

		// Jawbone sends a hash of client id & app secret encoded with SHA256 for security
		ConfigObject config = grailsApplication.config.oauth.providers.jawboneup
		String clientSecretID = config.key + config.secret
		String hash = clientSecretID.encodeAsSHA256()

		JSONObject notifications = JSON.parse(notificationData)

		if (notifications["secret_hash"] != hash) {
			log.debug "Hash do not match. Received: [${notifications["secret_hash"]}], generated: [$hash]"
			return []
		}

		List<ThirdPartyNotification> notificationsList = []
		notifications["events"].find { notification ->
			log.debug "Saving $notification."

			// Jawbone also sends notification when band buttons are pressed (has no type)
			if (!notification["type"] || !notification["timestamp"]) {
				log.debug "Notification type or timestamp not received."
				return false	// continue looping
			}

			Map params = [:]
			params["typeId"] = typeId
			params["ownerType"] = "user"
			params["ownerId"] = notification["user_xid"]
			params["collectionType"] = notification["type"]
			params["subscriptionId"] = notification["event_xid"]
			params["date"] = new Date(notification["timestamp"].toLong() * 1000)

			ThirdPartyNotification.withTransaction {
				ThirdPartyNotification thirdPartyNotificationInstance = new ThirdPartyNotification(params)
				if (Utils.save(thirdPartyNotificationInstance, true)) {
					notificationsList.add(thirdPartyNotificationInstance)
				}
			}

			return false	// continue looping
		}
		return notificationsList
	}
}

enum JawboneUpDataType {
	BODY("body_events"),
	MOVE("moves"),
	SLEEP("sleeps")

	/**
	 * Suffix used in the generic endpoint to pull data from the API. Like: "/nudge/api/v.1.1/users/@me/sleeps" or
	 * "/nudge/api/v.1.1/users/@me/body_events"
	 */
	final String endpointSuffix

	JawboneUpDataType(endpointSuffix) {
		this.endpointSuffix = endpointSuffix
	}
}
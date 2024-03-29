package us.wearecurio.services

import java.util.Date
import org.springframework.transaction.annotation.Transactional

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
import us.wearecurio.services.DataService.DataRequestContext
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
	static final String COMMON_BASE_URL = "/nudge/api/v.1.1%s"
	static final String COMMENT = "(Jawbone Up)"
	static final String SET_NAME = "Jawbone Up"
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
	Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug "getDataDefault(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"
		if (!account) {
			throw new IllegalArgumentException("No OAuthAccount instance was passed")
		}

		startDate = startDate ?: account.fetchLastDataDate()
		endDate = endDate ?: DateUtils.getEndOfTheDay()

		getDataBody(account, startDate, endDate, refreshAll, context)
		getDataMove(account, startDate, endDate, refreshAll, context)
		getDataSleep(account, startDate, endDate, refreshAll, context)

		return [success: true]
	}

	private boolean isRequestSucceeded(JSONObject response) {
		return response && response["meta"] && response["meta"]["code"] == 200
	}

	Map getDataBody(OAuthAccount account, Date forDay, boolean refreshAll, DataRequestContext context) {
		log.debug "getDataBody(): ${account} forDay: $forDay refreshAll: $refreshAll"

		return getDataBody(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	Map getDataBody(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) {
		log.debug "getDataBody(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"

		validateParams(account, startDate, endDate, refreshAll)
		return getDataBody(account, getRequestURL(JawboneUpDataType.BODY, startDate, endDate), context)
	}

	// Overloaded method to support pagination
	Map getDataBody(OAuthAccount account, String requestURL, DataRequestContext context) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat shortDateParser = new SimpleDateFormat("yyyyMMdd", Locale.US)
		shortDateParser.setTimeZone(timeZone)

		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceeded(apiResponse)) {
			return [success: false]
		}

		JSONObject bodyData = apiResponse["data"]

		bodyData["items"].find { bodyEntry ->
			String setName = SET_NAME

			Date entryDate = shortDateParser.parse(bodyEntry["date"].toString())
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			tagUnitMap.buildEntry(creationMap, stats, "weight", new BigDecimal((String)bodyEntry["weight"]), userId, timeZoneIdNumber,
					entryDate, COMMENT, setName, context)

			if (bodyEntry["body_fat"]) {
				tagUnitMap.buildEntry(creationMap, stats, "fatRatio", bodyEntry["body_fat"], userId, timeZoneIdNumber,
						entryDate, COMMENT, setName, context)
			}

			return false	// continue looping
		}

		stats.finish()

		account.markLastPolled(stats.lastDate)

		if (bodyData["links"] && bodyData["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			return getDataBody(account, bodyData["links"]["next"], context)
		}

		return [success: true]
	}

	Map getDataMove(OAuthAccount account, Date forDay, boolean refreshAll, DataRequestContext context) {
		log.debug "getDataMoves(): ${account} forDay: $forDay refreshAll: $refreshAll"

		return getDataMove(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	Map getDataMove(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) {
		log.debug "getDataMove(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"

		validateParams(account, startDate, endDate, refreshAll)
		return getDataMove(account, getRequestURL(JawboneUpDataType.MOVE, startDate, endDate), context)
	}

	// Overloaded method to support pagination
	Map getDataMove(OAuthAccount account, String requestURL, DataRequestContext context) {
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
		context.initEntrylist()

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

			String setName = SET_NAME

			Date entryDate = dateOnlyFormatter.parse(rawDate)
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			if (movesDetails["distance"]) {
				tagUnitMap.buildEntry(creationMap, stats, "miles", movesDetails["distance"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName, context)
			}

			if (movesDetails["active_time"]) {
				tagUnitMap.buildEntry(creationMap, stats, "minutes", movesDetails["active_time"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName, context)
			}

			if (movesDetails["steps"]) {
				tagUnitMap.buildEntry(creationMap, stats, "steps", movesDetails["steps"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName, context)
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

		account.markLastPolled(stats.lastDate)

		if (moveData["links"] && moveData["links"]["next"]) {
			log.debug "Processing get moves data for paginated URL and:" + moveData["links"]["next"]

			return getDataMove(account, moveData["links"]["next"], context)
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

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll, DataRequestContext context) {
		log.debug "getDataSleep(): ${account} forDay: $forDay refreshAll: $refreshAll"

		return getDataSleep(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	Map getDataSleep(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) {
		log.debug "getDataSleep(): ${account} start: $startDate, end: $endDate refreshAll: $refreshAll"

		validateParams(account, startDate, endDate, refreshAll)
		return getDataSleep(account, getRequestURL(JawboneUpDataType.SLEEP, startDate, endDate), context)
	}

	// Overloaded method to support pagination
	Map getDataSleep(OAuthAccount account, String requestURL, DataRequestContext context) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
		formatter.setTimeZone(timeZone)

		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

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

			String setName = SET_NAME

			Date entryDate = new Date(sleepDetails["asleep_time"].toLong() * 1000)
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			if (sleepDetails["duration"]) {
				tagUnitMap.buildEntry(creationMap, stats, "duration", sleepDetails["duration"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName, context)
			}
			if (sleepDetails["awakenings"]) {
				tagUnitMap.buildEntry(creationMap, stats, "awakeningsCount", sleepDetails["awakenings"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName, context)
			}
			if (sleepDetails["awake"]) {
				tagUnitMap.buildEntry(creationMap, stats, "awake", sleepDetails["awake"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName, context)
			}
			if (sleepDetails["quality"]) {
				tagUnitMap.buildEntry(creationMap, stats, "quality", sleepDetails["quality"], userId,
						timeZoneIdNumber, entryDate, COMMENT, setName, context)
			}

			return false	// continue looping
		}

		stats.finish()

		account.markLastPolled(stats.lastDate)

		if (sleepData["links"] && sleepData["links"]["next"]) {
			log.debug "Processing get sleep data for paginated URL"

			return getDataSleep(account, sleepData["links"]["next"], context)
		}
		return [success: true]
	}

	void validateParams(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll) {
		if (!account) {
			throw new IllegalArgumentException("OAuth account is missing")
		}

		if (!startDate) {
			throw new IllegalArgumentException("Start date is missing")
		}

		if (!endDate) {
			throw new IllegalArgumentException("End date is missing")
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
		if (!type) {
			throw new IllegalArgumentException("Type parameter is missing")
		}

		if (!startDate) {
			throw new IllegalArgumentException("Start date is missing")
		}

		if (!endDate) {
			throw new IllegalArgumentException("End date is missing")
		}

		Long startTime = startDate.getTime()
		Long endTime = endDate.getTime()

		URIBuilder builder = new URIBuilder("/users/@me/${type.endpointSuffix}")
		// API accepts the timestamp in EPOCH time (i.e. in seconds)
		builder.addQueryParam("start_time", Long.toString((long)(startTime / 1000)))
		builder.addQueryParam("end_time", Long.toString((long)(endTime / 1000)))
		builder.addQueryParam("updated_after", Long.toString((long)(startTime / 1000)))

		return String.format(COMMON_BASE_URL, builder.toString())
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
		List supportedNotificationTypes = ["body", "sleep", "move"]
		notifications["events"].find { notification ->
			log.debug "Saving $notification."

			// Jawbone also sends notification when band buttons are pressed (has no type)
			if (!notification["type"] || !(notification["type"] in supportedNotificationTypes) ||
					!notification["timestamp"]) {
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

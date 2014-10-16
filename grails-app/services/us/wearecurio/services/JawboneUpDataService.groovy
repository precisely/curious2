package us.wearecurio.services

import grails.converters.JSON

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.ThirdPartyNotification.Status
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.jawbone.JawboneUpTagUnitMap

class JawboneUpDataService extends DataService {

	static final String BASE_URL = "https://jawbone.com"
	static final String COMMON_BASE_URL = "/nudge/api%s"
	static final String COMMENT = "(Jawbone Up)"
	static final String SET_NAME = "JUP"

	JawboneUpTagUnitMap tagUnitMap = new JawboneUpTagUnitMap()

	JawboneUpDataService() {
		provider = "Jawboneup"
		typeId = ThirdParty.JAWBONE
		profileURL = String.format(BASE_URL + COMMON_BASE_URL, "/users/@me")
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		return null;
	}

	boolean isRequestSucceded(JSONObject response) {
		return response && response["meta"] && response["meta"]["code"] == 200
	}

	Map getDataBody(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataBody(): account ${account.id} forDay: $forDay refreshAll: $refreshAll"

		String requestUrl = String.format(COMMON_BASE_URL, "/users/@me/body_events")

		if (forDay) {
			Integer timeZoneIdNumber = getTimeZoneId(account)
			TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
			DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
			TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
			formatter.setTimeZone(timeZone)

			String forDate = formatter.format(forDay)
			requestUrl += "?date=$forDate"
		}

		return getDataBody(account, refreshAll, requestUrl)
	}

	// Overloaded method to support pagination
	Map getDataBody(OAuthAccount account, boolean refreshAll, String requestURL) {

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

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceded(apiResponse)) {
			return [success: false, message: "Received non 200 response"]
		}

		JSONObject bodyData = apiResponse["data"]

		bodyData["items"].find { bodyEntry ->
			JSONObject bodyDetails = bodyEntry["details"]

			String setName = SET_NAME + " " + bodyDetails["date"]
			Map args = [comment: COMMENT, setName: setName]

			Date entryDate = shortDateParser.parse(bodyEntry["date"].toString())
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
					e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

			tagUnitMap.buildEntry(creationMap, stats, "weight", bodyEntry["weight"], userId, timeZoneIdNumber,
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

			getDataBody(account, refreshAll, bodyData["links"]["next"])
		}

		return [success: true]
	}

	Map getDataMove(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "getDataMoves(): account ${account.id} forDay: $forDay refreshAll: $refreshAll"

		String requestUrl = String.format(COMMON_BASE_URL, "/users/@me/moves")

		if (forDay) {
			Integer timeZoneIdNumber = getTimeZoneId(account)
			TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
			DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
			TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
			formatter.setTimeZone(timeZone)

			String forDate = formatter.format(forDay)
			requestUrl += "?date=$forDate"
		}

		return getDataMove(account, refreshAll, requestUrl)
	}

	// Overloaded method to support pagination
	Map getDataMove(OAuthAccount account, boolean refreshAll, String requestURL) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
		formatter.setTimeZone(timeZone)

		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceded(apiResponse)) {
			return [success: false, message: "Received non 200 response"]
		}

		JSONObject moveData = apiResponse["data"]

		moveData["items"].find { movesEntry ->
			JSONObject movesDetails = movesEntry["details"]

			if (!movesDetails) {
				return false	// continue looping
			}

			String setName = SET_NAME + " " + movesEntry["date"]
			Map args = [comment: COMMENT, setName: setName]

			Date entryDate = formatter.parse(movesEntry["date"].toString())
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
					e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

			tagUnitMap.buildEntry(creationMap, stats, "miles", movesDetails["distance"], userId,
					timeZoneIdNumber, entryDate, COMMENT, setName)

			tagUnitMap.buildEntry(creationMap, stats, "minutes", movesDetails["active_time"], userId,
					timeZoneIdNumber, entryDate, COMMENT, setName)

			tagUnitMap.buildEntry(creationMap, stats, "steps", movesDetails["steps"], userId,
					timeZoneIdNumber, entryDate, COMMENT, setName)

			// no data for hours
			List noActivityGroup = []
			// active_time <= 300
			List mildActivityGroup = []
			// active_time > 300
			List highActivityGroup = []

			movesDetails["hourly_totals"].each { datetime, hourlyData ->
				if (hourlyData["active_time"] <= 300) {
					mildActivityGroup << hourlyData
				} else {
					highActivityGroup << hourlyData
				}
			}

			args["isSummary"] = true	// Indicating that these entries are summary entries

			if (mildActivityGroup) {
				def averageDistance = getAverage(mildActivityGroup, "distance")

				tagUnitMap.buildEntry(creationMap, stats, "lightlyActiveDistance", averageDistance, userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)

				def averageActiveTime = getAverage(mildActivityGroup, "active_time")

				tagUnitMap.buildEntry(creationMap, stats, "lightlyActiveMinutes", averageActiveTime, userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)

				def averageStep = getAverage(mildActivityGroup, "steps")

				tagUnitMap.buildEntry(creationMap, stats, "lightlyActiveSteps", averageStep, userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}

			if (highActivityGroup) {
				def averageDistance = getAverage(highActivityGroup, "distance")

				tagUnitMap.buildEntry(creationMap, stats, "highActiveDistance", averageDistance, userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)

				def averageActiveTime = getAverage(highActivityGroup, "active_time")

				tagUnitMap.buildEntry(creationMap, stats, "highActiveMinutes", averageActiveTime, userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)

				def averageStep = getAverage(highActivityGroup, "steps")

				tagUnitMap.buildEntry(creationMap, stats, "highActiveSteps", averageStep, userId,
						timeZoneIdNumber, entryDate, COMMENT, setName)
			}

			return false	// continue looping
		}

		stats.finish()

		if (moveData["links"] && moveData["links"]["next"]) {
			log.debug "Processing get moves data for paginated URL"

			getDataMove(account, refreshAll, moveData["links"]["next"])
		}

		return [success: true]
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
		log.debug "getDataSleep(): account ${account.id} forDay: $forDay refreshAll: $refreshAll"

		String requestUrl = String.format(COMMON_BASE_URL, "/users/@me/sleeps")

		if (forDay) {
			Integer timeZoneIdNumber = getTimeZoneId(account)
			TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
			DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
			TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
			formatter.setTimeZone(timeZone)

			String forDate = formatter.format(forDay)
			requestUrl += "?date=$forDate"
		}

		return getDataSleep(account, refreshAll, requestUrl)
	}

	// Overloaded method to support pagination
	Map getDataSleep(OAuthAccount account, boolean refreshAll, String requestURL) {

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.US)
		formatter.setTimeZone(timeZone)

		String accountId = account.accountId
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)

		if (!isRequestSucceded(apiResponse)) {
			return [success: false, message: "Received non 200 response"]
		}

		JSONObject sleepData = apiResponse["data"]

		sleepData["items"].find { sleepEntry ->
			JSONObject sleepDetails = sleepEntry["details"]

			if (!sleepDetails) {
				return false	// continue looping
			}

			String setName = SET_NAME + " " + sleepEntry["date"]
			Map args = [comment: COMMENT, setName: setName]

			Date entryDate = new Date(sleepDetails["asleep_time"].toLong() * 1000)
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
					e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])

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

			getDataSleep(account, refreshAll, sleepData["links"]["next"])
		}

		return [success: true]
	}

	/**
	 * Overriding default implementation so to send accept header to all requests
	 */
	@Override
	JSONElement getResponse(Token tokenInstance, String requestUrl, String method = "get", Map queryParams = [:],
			Map requestHeaders = [:]) {
		requestHeaders << ["Accept": "application/json"]
		// TODO Remove this. This should work automatically.
		requestHeaders << ["Authorization": "Bearer ${tokenInstance?.token}"]
		super.getResponse(tokenInstance, requestUrl, method, queryParams, requestHeaders)
	}

	@Override
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		getTimeZoneName(account.tokenInstance)
	}

	// Overloaded method
	String getTimeZoneName(Token tokenInstance) throws MissingOAuthAccountException, InvalidAccessTokenException {
		String url = String.format(BASE_URL + COMMON_BASE_URL, "/users/@me/timezone")
		JSONObject timezoneResponse = getResponse(tokenInstance, url)

		if (!isRequestSucceded(timezoneResponse)) {
			return null
		}

		return timezoneResponse["data"][0]?.tz
	}

	@Override
	void notificationHandler(String notificationData) {
		log.debug "Received notification data: $notificationData"

		if (!notificationData) {
			return
		}

		// Jawbone sends a hash of client id & app secret encoded with SHA256 for security
		ConfigObject config = grailsApplication.config.oauth.providers.jawboneup
		String cliendSecretID = config.key + config.secret
		String hash = cliendSecretID.encodeAsSHA256()

		JSONObject notifications = JSON.parse(notificationData)

		notifications["events"].find { notification ->
			log.debug "Saving $notification."

			// Jawbone also sends notification when band buttons are pressed (has no type)
			if (!notification["type"] || !notification["timestamp"]) {
				log.debug "Notification type or timestamp not received."
				return false	// continue looping
			}

			if (notification["secret_hash"] != hash) {
				log.debug "Notification hash does not match"
				return false
			}

			Map params = [:]
			params["typeId"] = typeId
			params["ownerType"] = "user"
			params["ownerId"] = notification["user_xid"]
			params["collectionType"] = notification["type"]
			params["subscriptionId"] = notification["event_xid"]
			params["date"] = new Date(notification["timestamp"].toLong() * 1000)

			ThirdPartyNotification.withTransaction {
				new ThirdPartyNotification(params).save(failOnError: true)
			}

			return false	// continue looping
		}
	}
}
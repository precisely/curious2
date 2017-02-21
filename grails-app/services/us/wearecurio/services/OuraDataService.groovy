package us.wearecurio.services

import grails.converters.JSON
import groovy.time.TimeCategory
import groovyx.net.http.URIBuilder
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.IllegalInstantException
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.springframework.transaction.annotation.Transactional
import us.wearecurio.datetime.DateUtils
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.TagUnitMap
import us.wearecurio.thirdparty.oura.OuraApi
import us.wearecurio.thirdparty.oura.OuraTagUnitMap
import us.wearecurio.utility.Utils

import java.text.SimpleDateFormat

class OuraDataService extends DataService {

	static final String BASE_URL = OuraApi.BASE_URL
	static final String SET_NAME = "Oura"
	static final String SOURCE_NAME = "Oura Data"
	static final String COMMENT = "(Oura)"
	OuraTagUnitMap tagUnitMap = new OuraTagUnitMap()

	EmailService emailService

	OuraDataService() {
		provider = "Oura"
		typeId = ThirdParty.OURA
		profileURL = BASE_URL + "/v1/userinfo"
		TagUnitMap.addSourceSetIdentifier(SET_NAME, SOURCE_NAME)
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataService.DataRequestContext context) throws InvalidAccessTokenException {
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
		// getDataExercise(account, startDate, endDate, false, context)
		getDataActivity(account, startDate, endDate, false, context)
		getDataReadiness(account, startDate, endDate, false, context)

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

	void getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll, DataService.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Get sleep data for account $account.id for $forDay"

		getDataSleep(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	void getDataSleep(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataService.DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug "Get sleep data account $account.id startDate: $startDate endDate $endDate refreshAll $refreshAll"

		endDate = endDate ?: DateUtils.getEndOfTheDay()

		getDataSleep(account, getRequestURL("sleep", startDate, endDate), context)
	}

	// Overloaded method to support pagination
	void getDataSleep(OAuthAccount account, String requestURL, DataService.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Import sleep data for user id $account.userId"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray sleepData = apiResponse["sleep"]

		sleepData.each { Map sleepEntry ->
			String setName = SET_NAME

			Integer timeZoneIdNumber = getTimeZoneId(account, sleepEntry)

			Date sleepEnd = convertTimeToDate(sleepEntry['bedtime_end'])

			// Trying to calculate sleepEnd using bedtime_start and duration parameters if bedtime_end is missing.
			if (!sleepEnd) {
				Date sleepStart = convertTimeToDate(sleepEntry['bedtime_start'])

				/*
				* Total duration of the sleep period (sleep.duration = sleep.bedtime_end - sleep.bedtime_start)
				* in seconds.
				*/
				Integer sleepDuration = sleepEntry['duration']?.toInteger()

				if (sleepDuration && sleepStart) {
					use(TimeCategory) {
						sleepEnd = sleepStart + sleepDuration.seconds
					}
				}

				// Not creating entry if sleepEnd could not be calculated.
				if (!sleepEnd) {
					log.warn 'Response does not contain data to calculate entry date.'

					return
				}
			}

			log.debug "Total bedtime ${sleepEntry['duration']} seconds, Sleep end ${sleepEnd}, "

			["total", "score", "awake", "rem", "light", "deep", 'hr_lowest'].each { key ->
				if (sleepEntry[key]) {
					tagUnitMap.buildEntry(creationMap, stats, key, new BigDecimal(sleepEntry[key].toString()),
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

	void getDataExercise(OAuthAccount account, Date forDay, boolean refreshAll, DataService.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Get exercise data for account $account.id for $forDay"

		getDataExercise(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	void getDataExercise(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataService.DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug "Get exercise data account $account.id startDate: $startDate endDate $endDate refreshAll $refreshAll"

		endDate = endDate ?: DateUtils.getEndOfTheDay()

		getDataExercise(account, getRequestURL("exercise", startDate, endDate), context)
	}

	Map getDataExercise(OAuthAccount account, String requestURL, DataService.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Import exercise data for user id $account.userId"
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
			Long userId, OAuthAccount account, DataService.DataRequestContext context) {
		def exerciseEntryData = exerciseEntry["data"]
		Date entryDate = convertTimeToDate(exerciseEntry)
		Integer timeZoneIdNumber = getTimeZoneId(account, exerciseEntry)

		String setName = SET_NAME

		Long amount = exerciseEntryData["duration_m"]
		String tagName = "classification_" + exerciseEntryData["classification"]
		tagUnitMap.buildEntry(creationMap, stats, tagName, amount, userId, timeZoneIdNumber, entryDate, COMMENT, setName, context)
	}

	void getDataActivity(OAuthAccount account, Date forDay, boolean refreshAll, DataService.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Get activity data for account $account.id for $forDay"

		getDataActivity(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	void getDataActivity(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataService.DataRequestContext context) throws
			InvalidAccessTokenException {
		log.debug "Get activity data account $account.id startDate: $startDate endDate $endDate refreshAll $refreshAll"

		endDate = endDate ?: DateUtils.getEndOfTheDay()

		getDataActivity(account, getRequestURL("activity", startDate, endDate), context)
	}

	void getDataActivity(OAuthAccount account, String requestURL, DataService.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Import activity data for user id $account.userId"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray activityData = apiResponse["activity"]

		activityData.each { Map activityEntry ->
			Date entryDate = convertTimeToDate(activityEntry['day_start'])

			if (!entryDate) {
				log.warn 'Response does not contain data to calculate entry date.'

				return
			}

			String setName = SET_NAME
			Integer timeZoneIdNumber = getTimeZoneId(account, activityEntry)

			["non_wear", "steps", "daily_movement", "cal_active", "cal_total"].each { key ->
				if (activityEntry[key]) {
					tagUnitMap.buildEntry(creationMap, stats, key, Long.parseLong(activityEntry[key].toString()), userId,
							timeZoneIdNumber, entryDate, COMMENT, setName, context)
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

	void getDataReadiness(OAuthAccount account, Date forDay, boolean refreshAll, DataService.DataRequestContext
			context) throws InvalidAccessTokenException {
		log.debug "Get readiness data for account ${account.id} for ${forDay}"

		getDataReadiness(account, forDay, DateUtils.getEndOfTheDay(forDay), refreshAll, context)
	}

	void getDataReadiness(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataService
			.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Get readiness data account: ${account} startDate: ${startDate} endDate: ${endDate} refreshAll: " +
				"${refreshAll}"

		endDate = endDate ?: DateUtils.getEndOfTheDay()

		getDataReadiness(account, getRequestURL('readiness', startDate, endDate), context)
	}

	void getDataReadiness(OAuthAccount account, String requestURL, DataService.DataRequestContext context) throws
			InvalidAccessTokenException {
		Long userId = account.userId
		log.debug "Import readiness data for user id ${userId}"

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		JSONArray readinessData = apiResponse['readiness']

		readinessData.each { Map readinessEntry ->
			// Summary Date received as String. Format - yyyy-MM-dd
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat('yyyy-MM-dd')
			Date entryDate = simpleDateFormat.parse(readinessEntry['summary_date'])

			if (!entryDate) {
				log.warn 'Response does not contain data to calculate entry date.'

				return
			}

			String setName = SET_NAME
			Integer timeZoneIdNumber = getTimeZoneId(account, readinessEntry)

			if (readinessEntry['score']) {
				tagUnitMap.buildEntry(creationMap, stats, 'readiness_score',
						new BigDecimal(readinessEntry['score'].toString()), userId, timeZoneIdNumber, entryDate,
						COMMENT, setName, context)
			}
		}

		account.markLastPolled(stats.lastDate)
		stats.finish()

		if (apiResponse["links"] && apiResponse["links"]["nextPageURL"]) {
			log.debug "Processing get readiness data for paginated URL"
			getDataReadiness(account, apiResponse["links"]["nextPageURL"].toString(), context)
		}
	}

	/**
	 * Get old set name which will is used for backward compatibility by removing old entries with this set name.
	 * This set name is not used for creating new entries. Use SET_NAME instead.
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

		String startTime = startDate.format('yyyy-MM-dd')
		String endTime = endDate.format('yyyy-MM-dd')

		URIBuilder builder = new URIBuilder("/v1/$dataType")
		builder.addQueryParam("max", 1000)
		builder.addQueryParam("offset", 0)
		builder.addQueryParam("start", startTime)
		builder.addQueryParam("end", endTime)

		return builder.toString()
	}

	Integer getTimeZoneId(OAuthAccount account, Map rawEntry) {
		// Timezone offset in minutes
		String timezone = rawEntry['timezone']
		Integer offsetInMilliSeconds

		if (timezone) {
			try {
				offsetInMilliSeconds = (timezone.toInteger()) * 60 * 1000
			} catch (NumberFormatException e) {
				log.warn 'Could not parse timezone', e
			}
		}

		String timezoneOffset

		if (offsetInMilliSeconds) {
			// Getting timezone offset in String format. Ex: 180 converted to +03:00, -420 converted to -07:00
			timezoneOffset = DateTimeZone.forOffsetMillis(offsetInMilliSeconds)?.toString()
		}

		// Finally return the id of TimeZoneId instance found/created for this offset.
		return timezoneOffset ? TimeZoneId.look(timezoneOffset).id : getTimeZoneId(account)
	}

	/**
	 * For a specific record from Ouracloud, the time of that record/event is the Date time. This helper method
	 * converts that time to a Date object. Keeping it as a method to keep it DRY and also prevent it from missing at
	 * ony place.
	 * @param rawEntry Raw event/entry data received from Ouracloud API.
	 * @return Converted date object
	 */
	Date convertTimeToDate(String dateTimeString) {
		if (!dateTimeString) {
			return
		}

		String pattern = "yyyy-MM-dd'T'HH:mm:ssZ"
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(pattern)

		Date convertedDate
		try {
			convertedDate = dateTimeFormatter.parseDateTime(dateTimeString).toDate()
		} catch (IllegalArgumentException | UnsupportedOperationException | IllegalInstantException e) {
			log.warn "Could not parse date ${dateTimeString}"
		}

		return convertedDate
	}

	@Override
	void checkSyncHealth() {
		int accountsCount = OAuthAccount.withCriteria {
			eq('typeId', ThirdParty.OURA)
			gt('lastData', new Date() - 1)

			projections {
				rowCount()
			}
		}[0]

		if (accountsCount > 0) {
			log.info "There were $accountsCount accounts that were synced in the last 24 hours."

			return
		}

		String warnMessage = 'Not a single sync happened in the last 24 hours.'

		log.warn warnMessage

		['vishesh@causecode.com', 'mitsu@wearecurio.us', 'developers@causecode.com'].each { String toEmail ->
			emailService.send(toEmail, '[Curious] - Oura Sync Issue', warnMessage)
		}
	}
}

/**
 * TODO Replace the use of String like "sleep", "s", "activity", "a" etc. with this enum to make the code more
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


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
	OauthAccountService oauthAccountService

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
		// Now exercise data is also coming in the activity data.
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

	// Overloaded method to support pagination. TODO Add pagination support based on date range.
	void getDataSleep(OAuthAccount account, String requestURL, DataService.DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "Import sleep data for user id $account.userId"
		Long userId = account.userId

		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)
		context.initEntrylist()

		JSONObject apiResponse

		// Handling exceptions for very large data from Oura Server until we implement pagination.
		try {
			apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		} catch (InvalidAccessTokenException e) {
			return
		} catch (Exception e1) {
			Utils.reportError("Could not get response from Oura Server", e1)
			return
		}

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

		JSONObject apiResponse

		// Handling exceptions for very large data from Oura Server until we implement pagination.
		try {
			apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		} catch (InvalidAccessTokenException e) {
			return
		} catch (Exception e1) {
			Utils.reportError("Could not get response from Oura Server", e1)
			return
		}

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

		JSONObject apiResponse

		// Handling exceptions for very large data from Oura Server until we implement pagination.
		try {
			apiResponse = getResponse(account.tokenInstance, BASE_URL + requestURL)
		} catch (InvalidAccessTokenException e) {
			return
		} catch (Exception e1) {
			Utils.reportError("Could not get response from Oura Server", e1)
			return
		}

		JSONArray readinessData = apiResponse['readiness']

		readinessData.each { Map readinessEntry ->
			// Summary Date received in yyyy-MM-dd format.
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

	void refreshAllOAuthAccounts() {
		OAuthAccount.withCriteria {
			eq('typeId', ThirdParty.OURA)

			isNotNull('refreshToken')
			ne('refreshToken', 'null')
			ne('refreshToken', '')
		}.each { account ->
			try {
				oauthAccountService.refreshToken(account)
			} catch (Throwable t) {
				log.error("Error while refreshing token for account " + account)
				t.printStackTrace()
				account.setAccountFailure()
			}
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


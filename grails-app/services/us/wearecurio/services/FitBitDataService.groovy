package us.wearecurio.services

import grails.converters.JSON

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
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
import us.wearecurio.thirdparty.fitbit.FitBitTagUnitMap
import us.wearecurio.utility.Utils


class FitBitDataService extends DataService {

	static final String API_VERSION = "1"
	static final String BASE_URL = "https://api.fitbit.com/$API_VERSION/user%s"
	static final String COMMENT = "(FitBit)"
	static final String SET_NAME = "fitbit import"

	static transactional = true

	FitBitTagUnitMap fitBitTagUnitMap = new FitBitTagUnitMap()

	def urlService

	FitBitDataService() {
		provider = "FitBit"
		typeId = ThirdParty.FITBIT
		profileURL = String.format(BASE_URL, "/-/profile.json")
	}

	@Transactional
	Map getDataActivities(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug("FitBitDataServices.getDataActivities(): account " + account.getId() + " forDay: " + forDay + " refreshAll: " + refreshAll)
		String accountId = account.accountId
		Long userId = account.userId

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		formatter.setTimeZone(TimeZoneId.getTimeZoneInstance(timeZoneIdNumber))
		String forDate = formatter.format(forDay)
		String oldSetName = forDate + "activityfitbit"	// Backward support
		String setName = SET_NAME
		String requestUrl = String.format(BASE_URL, "/${accountId}/activities/date/${forDate}.json")

		Map args = [setName: setName, comment: COMMENT]
		
		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject activityData = getResponse(account.tokenInstance, requestUrl)

		boolean veryActiveD, moderatelyActiveD, lightlyActiveD, sedentaryActiveD, fairlyActiveD
		veryActiveD = moderatelyActiveD = lightlyActiveD = sedentaryActiveD = fairlyActiveD = true

		forDay = new DateTime(forDay.time, dateTimeZoneInstance).withTime(23, 59, 0, 0).toDate()
		log.debug "Entry Date for Fitbit activity set to: [$forDay]"

		if (activityData.summary) {
			fitBitTagUnitMap.buildEntry(creationMap, stats, "steps", activityData.summary.steps, userId, timeZoneIdNumber, forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.fairlyActiveMinutes <= 0) {
			fairlyActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry(creationMap, stats, "fairlyActiveMinutes", activityData.summary.fairlyActiveMinutes, userId, timeZoneIdNumber,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.lightlyActiveMinutes <= 0) {
			lightlyActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry(creationMap, stats, "lightlyActiveMinutes", activityData.summary.lightlyActiveMinutes, userId, timeZoneIdNumber,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.sedentaryMinutes <= 0) {
			sedentaryActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry(creationMap, stats, "sedentaryMinutes", activityData.summary.sedentaryMinutes, userId, timeZoneIdNumber,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.veryActiveMinutes <= 0) {
			veryActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry(creationMap, stats, "veryActiveMinutes", activityData.summary.veryActiveMinutes, userId, timeZoneIdNumber,
					forDay, COMMENT, setName)
		}

		activityData.summary?.distances.each { distance ->
			if (!distance.activity.equals('loggedActivities')) { //Skipping loggedActivities
				log.debug "Importing activity: " + distance.activity

				if ((!veryActiveD && distance.activity.equals("veryActive"))
				||(!moderatelyActiveD && distance.activity.equals("moderatelyActive"))
				||(!lightlyActiveD && distance.activity.equals("lightlyActive"))
				||(!sedentaryActiveD && distance.activity.equals("sedentaryActive"))) {
					log.debug "Discarding activity with 0 time"
				} else {
					fitBitTagUnitMap.buildEntry(creationMap, stats, distance.activity, distance.distance, userId, timeZoneIdNumber, forDay, COMMENT, setName)
				}
			}
		}
		
		stats.finish()
		
		[success: true]
	}

	@Transactional
	Map getDataBody(OAuthAccount account, Date forDay, boolean refreshAll) {
		// Getting body measurements
		//requestUrl = String.format(BASE_URL, "/${accountId}/body/date/${forDate}.json")
		//getResponse(account.tokenInstance, requestUrl)

		// Getting body weight
		//requestUrl = String.format(BASE_URL, "/${accountId}/body/log/weight/date/${forDate}.json")
		//getResponse(account.tokenInstance, requestUrl)

		// Getting body fat
		//requestUrl = String.format(BASE_URL, "/${accountId}/body/log/fat/date/${forDate}.json")
		//getResponse(account.tokenInstance, requestUrl)
		[success: true]
	}

	@Override
	@Transactional
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		log.debug("FitBitDataService.getDataDefault() account " + account.getId() + " startDate: " + startDate + " refreshAll: " + refreshAll)
		String accountId = account.accountId
		startDate = startDate ?: account.getLastPolled() ?: earlyStartDate
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		formatter.setTimeZone(TimeZoneId.getTimeZoneInstance(getTimeZoneId(account)))
		String forDate = formatter.format(startDate)
		String setName = SET_NAME + " " + forDate

		Map args = [setName: setName, comment: COMMENT]

		Long userId = account.userId

		getDataActivities(account, startDate, false)
		getDataBody(account, startDate, false)
		getDataFoods(account, startDate, false)
		getDataSleep(account, startDate, false)

		[success: true]
	}

	@Transactional
	Map getDataFoods(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug("FitBitDataService.getDataFoods() account " + account.getId() + " forDay: " + forDay + " refreshAll: " + refreshAll)
		//requestUrl = String.format(BASE_URL, "/${accountId}/${collectionType}/log/date/${forDate}.json")
		log.debug("This method is stubbed out, not doing anything at present")
		[success: true]
	}

	@Transactional
	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		log.debug("FitBitDataService.getDataSleep() account " + account.getId() + " forDay: " + forDay + " refreshAll: " + refreshAll)
		Long userId = account.userId

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()
		TimeZone timeZone = dateTimeZoneInstance.toTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		formatter.setTimeZone(timeZone)
		SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
		dateTimeParser.setTimeZone(timeZone)
		
		String accountId = account.accountId
		String forDate = formatter.format(forDay)
		String setName = SET_NAME + " " + forDate
		String requestUrl = String.format(BASE_URL, "/${accountId}/sleep/date/${forDate}.json")

		Map args = [setName: setName, comment: COMMENT]
		
		EntryCreateMap creationMap = new EntryCreateMap()
		EntryStats stats = new EntryStats(userId)

		JSONObject sleepData = getResponse(account.tokenInstance, requestUrl)

		sleepData.sleep.each { logEntry ->
			Date entryDate = dateTimeParser.parse(logEntry.startTime)
			/**
			 * Converting received user's local date-time to actual date-time in user's timezone.
			 * Since Java's Date is independent on Timezone & incoming date-time is in user's local
			 * time hence using Joda's DateTime$withZoneRetainFields method which converts only timezone
			 * keeping fields same.
			 */
			entryDate = new DateTime(entryDate.time).withZoneRetainFields(dateTimeZoneInstance).toDate()

			String oldSetName = logEntry.logId	// Backward support

			Entry.executeUpdate("update Entry e set e.userId = null where e.setIdentifier in :setIdentifiers and e.userId = :userId",
					[setIdentifiers: [Identifier.look(setName), Identifier.look(oldSetName)], userId: account.userId])

			fitBitTagUnitMap.buildEntry(creationMap, stats, "duration", logEntry.duration, userId, timeZoneIdNumber, entryDate, COMMENT, setName)
			fitBitTagUnitMap.buildEntry(creationMap, stats, "awakeningsCount", logEntry.awakeningsCount, userId, timeZoneIdNumber, entryDate, COMMENT, setName)

			if (logEntry.efficiency > 0 )
				fitBitTagUnitMap.buildEntry(creationMap, stats, "efficiency", logEntry.efficiency, userId, timeZoneIdNumber, entryDate, COMMENT, setName)
		}
		
		stats.finish()
		
		[success: true]
	}

	/**
	 * Overriding default implementation so to send accept-language header for proper units.
	 */
	@Override
	@Transactional
	JSONElement getResponse(Token tokenInstance, String requestUrl) {
		Map requestHeader = ["Accept-Language": "en_US"]
		super.getResponse(tokenInstance, requestUrl, "get", [:], requestHeader)
	}

	@Transactional
	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		getUserProfile(account).user.timezone
	}

	@Override
	@Transactional
	JSONElement listSubscription(Long userId) {
		super.listSubscription(userId, String.format(BASE_URL, "/-/apiSubscriptions.json"), "get", [:])
	}

	@Override
	List<ThirdPartyNotification> notificationHandler(String notificationData) {
		log.debug "Received fitbit notification data: $notificationData"

		JSONArray notifications = JSON.parse(notificationData)

		List<ThirdPartyNotification> notificationsList = []
		notifications.each { notification ->
			log.debug "Saving " + notification.dump()
			notification.typeId = typeId
			List<OAuthAccount> accounts = getAllOAuthAccounts(notification.ownerId)
			TimeZone timeZone = null
			for (OAuthAccount account in accounts) {
				timeZone = getTimeZone(account)
				if (timeZone != null) break
			}
			if (timeZone == null) {
				log.debug "No time zone found for fitbit accountId " + notification.ownerId
				timeZone = TimeZone.getTimeZone("UTC")
			}
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
			formatter.setTimeZone(timeZone)
			notification.date = formatter.parse(notification.date)
			ThirdPartyNotification.withTransaction {
				ThirdPartyNotification thirdPartyNotificationInstance = new ThirdPartyNotification(notification)
				if (Utils.save(thirdPartyNotificationInstance, true)) {
					notificationsList.add(thirdPartyNotificationInstance)
				}
			}
		}

		return notificationsList
	}

	@Override
	@Transactional
	Map subscribe(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		log.debug("FitBitDataService.subscribe() userId " + userId)

		String subscriptionURL = String.format(BASE_URL, "/-/apiSubscriptions/${userId}.json")

		Map result = super.subscribe(userId, subscriptionURL, "post", [:])
		OAuthAccount account = result.account
		result.success = true

		switch(result["code"]) {
			case 200:
			case 409:
				log.debug("Already subscribed.")
				// TODO: Shouldn't this be internationalized, for consistency?
				result.message = "You've already been subscribed."
				break;
			case 201:
				log.debug("Successfully subscribed.")
				result.message = "" // Successfully subscribed.
				break;
			default:
				log.debug("Subscription failed.")
				result.success = false
				account.removeAccessToken()			// confirms that subscription is not successful.
				Utils.save(account, true)
		}

		result
	}

	@Override
	@Transactional
	Map unsubscribe(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		String unsubscribeURL = String.format(BASE_URL, "/-/apiSubscriptions/${userId}.json")

		Map result = super.unsubscribe(userId, unsubscribeURL, "delete", [:])

		if (result["code"] in [204, 404]) {
			return [success: true]
		}

		[success: false]
	}

}

package us.wearecurio.services

import grails.converters.JSON

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.fitbit.FitBitTagUnitMap

class FitBitDataService extends DataService {

	static final String API_VERSION = "1"
	static final String BASE_URL = "http://api.fitbit.com/$API_VERSION/user%s"
	static final String COMMENT = "(FitBit)"
	static final String SET_NAME = "fitbit import"

	static transactional = true

	FitBitTagUnitMap fitBitTagUnitMap = new FitBitTagUnitMap()

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
	SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")

	def urlService

	FitBitDataService() {
		provider = "FitBit"
		typeId = ThirdParty.FITBIT
		profileURL = String.format(BASE_URL, "/-/profile.json")
	}

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

		JSONObject activityData = getResponse(account.tokenInstance, requestUrl)

		boolean veryActiveD, moderatelyActiveD, lightlyActiveD, sedentaryActiveD, fairlyActiveD
		veryActiveD = moderatelyActiveD = lightlyActiveD = sedentaryActiveD = fairlyActiveD = true

		forDay = new DateTime(forDay.time, dateTimeZoneInstance).withTime(23, 59, 0, 0).toDate()
		log.debug "Entry Date for Fitbit activity set to: [$forDay]"

		if (activityData.summary) {
			fitBitTagUnitMap.buildEntry("steps", activityData.summary.steps, userId, timeZoneIdNumber, forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.fairlyActiveMinutes <= 0) {
			fairlyActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("fairlyActiveMinutes", activityData.summary.fairlyActiveMinutes, userId, timeZoneIdNumber,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.lightlyActiveMinutes <= 0) {
			lightlyActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("lightlyActiveMinutes", activityData.summary.lightlyActiveMinutes, userId, timeZoneIdNumber,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.sedentaryMinutes <= 0) {
			sedentaryActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("sedentaryMinutes", activityData.summary.sedentaryMinutes, userId, timeZoneIdNumber,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.veryActiveMinutes <= 0) {
			veryActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("veryActiveMinutes", activityData.summary.veryActiveMinutes, userId, timeZoneIdNumber,
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
					fitBitTagUnitMap.buildEntry(distance.activity, distance.distance, userId, timeZoneIdNumber, forDay, COMMENT, setName)
				}
			}
		}
		[success: true]
	}

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
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		log.debug("FitBitDataService.getDataDefault() account " + account.getId() + " startDate: " + startDate + " refreshAll: " + refreshAll)
		String accountId = account.accountId
		startDate = startDate ?: account.getLastPolled() ?: earlyStartDate
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

	Map getDataFoods(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug("FitBitDataService.getDataFoods() account " + account.getId() + " forDay: " + forDay + " refreshAll: " + refreshAll)
		//requestUrl = String.format(BASE_URL, "/${accountId}/${collectionType}/log/date/${forDate}.json")
		log.debug("This method is stubbed out, not doing anything at present")
		[success: true]
	}

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) throws InvalidAccessTokenException {
		log.debug("FitBitDataService.getDataSleep() account " + account.getId() + " forDay: " + forDay + " refreshAll: " + refreshAll)
		Long userId = account.userId

		Integer timeZoneIdNumber = getTimeZoneId(account)
		TimeZoneId timeZoneIdInstance = TimeZoneId.fromId(timeZoneIdNumber)
		DateTimeZone dateTimeZoneInstance = timeZoneIdInstance.toDateTimeZone()

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
		formatter.setTimeZone(TimeZoneId.getTimeZoneInstance(timeZoneIdNumber))
		SimpleDateFormat dateTimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")

		String accountId = account.accountId
		String forDate = formatter.format(forDay)
		String setName = SET_NAME + " " + forDate
		String requestUrl = String.format(BASE_URL, "/${accountId}/sleep/date/${forDate}.json")

		Map args = [setName: setName, comment: COMMENT]

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

			Entry.executeUpdate("delete Entry e where e.setName in :setNames and e.userId = :userId",
					[setNames: [setName, oldSetName], userId: account.userId])

			fitBitTagUnitMap.buildEntry("duration", logEntry.duration, userId, timeZoneIdNumber, entryDate, COMMENT, setName)
			fitBitTagUnitMap.buildEntry("awakeningsCount", logEntry.awakeningsCount, userId, timeZoneIdNumber, entryDate, COMMENT, setName)

			if (logEntry.efficiency > 0 )
				fitBitTagUnitMap.buildEntry("efficiency", logEntry.efficiency, userId, timeZoneIdNumber, entryDate, COMMENT, setName)
		}
		[success: true]
	}

	/**
	 * Overriding default implementation so to send accept-language header for proper units.
	 */
	@Override
	JSONElement getResponse(Token tokenInstance, String requestUrl) {
		Map requestHeader = ["Accept-Language": "en_US"]
		super.getResponse(tokenInstance, requestUrl, "get", [:], requestHeader)
	}

	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		getUserProfile(account).user.timezone
	}

	@Override
	JSONElement listSubscription(Long userId) {
		super.listSubscription(userId, String.format(BASE_URL, "/-/apiSubscriptions.json"), "get", [:])
	}

	@Override
	void notificationHandler(String notificationData) {
		log.debug "Received fitbit notification data: $notificationData"

		JSONArray notifications = JSON.parse(notificationData)

		notifications.each { notification ->
			log.debug "Saving " + notification.dump()
			notification.typeId = typeId
			notification.date = formatter.parse(notification.date)
			new ThirdPartyNotification(notification).save(failOnError: true)
		}
	}

	@Override
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
				account.save()
		}

		result
	}

	@Override
	Map unsubscribe(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		String unsubscribeURL = String.format(BASE_URL, "/-/apiSubscriptions/${userId}.json")

		Map result = super.unsubscribe(userId, unsubscribeURL, "delete", [:])

		if (result["code"] in [204, 404]) {
			return [success: true]
		}

		[success: false]
	}

}

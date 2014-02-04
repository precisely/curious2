package us.wearecurio.services

import grails.converters.JSON

import java.text.SimpleDateFormat

import javassist.NotFoundException

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.User
import us.wearecurio.thirdparty.AuthenticationRequiredException
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
		provider = "fitbit"
		typeId = OAuthAccount.FITBIT_ID
		profileURL = String.format(BASE_URL, "/-/profile.json")
	}

	Map getDataActivities(OAuthAccount account, Date forDay, boolean refreshAll) {
		String accountId = account.accountId
		String forDate = formatter.format(forDay)
		String oldSetName = forDate + "activityfitbit"	// Backward support
		String setName = SET_NAME + " " + forDate
		String requestUrl = String.format(BASE_URL, "/${accountId}/activities/date/${forDate}.json")

		Map args = [setName: setName, comment: COMMENT]

		Long userId = account.userId

		Integer timeZoneId = User.getTimeZoneId(userId)

		JSONObject activityData = getResponse(account.tokenInstance, requestUrl)

		Entry.executeUpdate("delete Entry e where e.setName in :setNames and e.userId = :userId",
				[setNames: [setName, oldSetName], userId: userId])

		boolean veryActiveD, moderatelyActiveD, lightlyActiveD, sedentaryActiveD, fairlyActiveD
		veryActiveD = moderatelyActiveD = lightlyActiveD = sedentaryActiveD = fairlyActiveD = true

		if (activityData.summary) {
			fitBitTagUnitMap.buildEntry("steps", activityData.summary.steps, userId, timeZoneId, forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.fairlyActiveMinutes <= 0) {
			fairlyActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("fairlyActiveMinutes", activityData.summary.fairlyActiveMinutes, userId, timeZoneId,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.lightlyActiveMinutes <= 0) {
			lightlyActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("lightlyActiveMinutes", activityData.summary.lightlyActiveMinutes, userId, timeZoneId,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.sedentaryMinutes <= 0) {
			sedentaryActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("sedentaryMinutes", activityData.summary.sedentaryMinutes, userId, timeZoneId,
					forDay, COMMENT, setName)
		}

		if (!activityData.summary || activityData.summary.veryActiveMinutes <= 0) {
			veryActiveD = false
		} else {
			fitBitTagUnitMap.buildEntry("veryActiveMinutes", activityData.summary.veryActiveMinutes, userId, timeZoneId,
					forDay, COMMENT, setName)
		}

		activityData.summary.distances.each { distance ->
			Date entryDate = forDay
			try {
				activityData.activities.each { activity ->
					if (activity.name.equals(distance.activity) && activity.hasStartTime) {
						entryDate = inputFormat.parse(formatter.format(entryDate) + "T" + activity.startTime + ":00.000")
						throw new Exception("return from closure")
					}
				}
			} catch(Exception e) {
				//do nothing
			}

			if (!distance.activity.equals('loggedActivities')) { //Skipping loggedActivities
				log.debug "Importing activity: " + distance.activity

				if ((!veryActiveD && distance.activity.equals("veryActive"))
				||(!moderatelyActiveD && distance.activity.equals("moderatelyActive"))
				||(!lightlyActiveD && distance.activity.equals("lightlyActive"))
				||(!sedentaryActiveD && distance.activity.equals("sedentaryActive"))) {
					log.debug "Discarding activity with 0 time"
				} else {
					fitBitTagUnitMap.buildEntry(distance.activity, distance.distance, userId, timeZoneId, entryDate, COMMENT, setName)
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
	Map getDataDefault(OAuthAccount account, Date forDay, boolean refreshAll) {
		String accountId = account.accountId
		String forDate = formatter.format(forDay)
		String setName = SET_NAME + " " + forDate

		Map args = [setName: setName, comment: COMMENT]

		Long userId = account.userId

		Integer timeZoneId = User.getTimeZoneId(userId)

		getDataActivities(account, forDay)
		getDataBody(account, forDay)
		getDataFoods(account, forDay)
		getDataSleep(account, forDay)

		[success: true]
	}

	Map getDataFoods(OAuthAccount account, Date forDay, boolean refreshAll) {
		//requestUrl = String.format(BASE_URL, "/${accountId}/${collectionType}/log/date/${forDate}.json")
		[success: true]
	}

	Map getDataSleep(OAuthAccount account, Date forDay, boolean refreshAll) {
		String accountId = account.accountId
		String forDate = formatter.format(forDay)
		String setName = SET_NAME + " " + forDate
		String requestUrl = String.format(BASE_URL, "/${accountId}/sleep/date/${forDate}.json")

		Map args = [setName: setName, comment: COMMENT]

		Long userId = account.userId

		Integer timeZoneId = User.getTimeZoneId(userId)

		JSONObject sleepData = getResponse(account.tokenInstance, requestUrl)

		sleepData.sleep.each { logEntry ->
			Date entryDate = inputFormat.parse(logEntry.startTime)
			String oldSetName = logEntry.logId	// Backward support

			Entry.executeUpdate("delete Entry e where e.setName in :setNames and e.userId = :userId",
					[setNames: [setName, oldSetName], userId: account.userId])

			fitBitTagUnitMap.buildEntry("duration", logEntry.duration, userId, timeZoneId, entryDate, COMMENT, setName)
			fitBitTagUnitMap.buildEntry("awakeningsCount", logEntry.awakeningsCount, userId, timeZoneId, entryDate, COMMENT, setName)

			if (logEntry.efficiency > 0 )
				fitBitTagUnitMap.buildEntry("efficiency", logEntry.efficiency, userId, timeZoneId, entryDate, COMMENT, setName)
		}
		[success: true]
	}

	@Override
	JSONObject getResponse(Token tokenInstance, String requestUrl) {
		Map requestHeader = ["Accept-Language": "en_US"]
		super.getResponse(tokenInstance, requestUrl, "get", [:], requestHeader)
	}

	@Override
	JSONObject listSubscription() {
		super.listSubscription(String.format(BASE_URL, "/-/apiSubscriptions.json"), "get", [:])
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
	Map subscribe() throws AuthenticationRequiredException {
		Long userId = securityService.currentUser.id
		String subscriptionURL = String.format(BASE_URL, "/-/apiSubscriptions/${userId}.json")

		Map result = super.subscribe(subscriptionURL, "post", [:])
		result.success = true

		switch(result["code"]) {
			case 200:
			case 409:
				result.message = "You've already been subscribed."
				break;
			case 201:
				result.message = "" // Successfully subscribed.
				break;
			default:
				result.success = false
				getOAuthAccountInstance().delete()
		}

		//listSubscription(account) // Test after subscribe
		result
	}

	@Override
	Map unsubscribe() throws NotFoundException, AuthenticationRequiredException {
		Map result

		String unsubscribeURL = String.format(BASE_URL, "/-/apiSubscriptions/${currentUserId}.json")

		try {
			result = super.unsubscribe(unsubscribeURL, "delete", [:])
		} catch (NotFoundException e) {
			log.info "No subscription found for userId [$currentUserId]"
			return [success: false, message: "No subscription found"]
		}

		if (result["code"] in [204, 404]) {
			getOAuthAccountInstance().delete()
			return [success: true]
		}

		//listSubscription(account)	// Test after un-subscribe
		[success: false]
	}

}
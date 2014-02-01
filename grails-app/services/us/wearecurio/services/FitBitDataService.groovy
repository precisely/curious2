package us.wearecurio.services

import grails.converters.JSON

import java.text.SimpleDateFormat

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.model.FitbitNotification
import us.wearecurio.model.OAuthAccount
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.thirdparty.fitbit.FitBitTagUnitMap

class FitBitDataService {

	def grailsApplication

	def oauthService // From OAuth plugin
	def urlService

	private static def log = LogFactory.getLog(this)
	private static final String FITBIT_SET_NAME = "fitbit import"

	static debug(str) {
		log.debug(str)
	}

	static transactional = true

	Map lastPollTimestamps = new HashMap<Long,Long>() // prevent DOS attacks

	Map authorizeAccount(Token tokenInstance, Long userId) throws AuthenticationRequiredException {
		if (!tokenInstance || !tokenInstance.token)
			throw new AuthenticationRequiredException("fitbit")

		OAuthAccount fitbitAccount = OAuthAccount.findByUserIdAndTypeId(userId, OAuthAccount.FITBIT_ID)
		return subscribe(fitbitAccount, userId)
	}

	JSONObject getUserInfo(Token accessToken) {
		String apiVersion = grailsApplication.config.oauth.providers.fitbit.apiVersion
		String userInfoURL = "http://api.fitbit.com/${apiVersion}/user/-/profile.json"

		Response response = oauthService.getFitbitResource(accessToken, userInfoURL)

		log.info "User info return with code: $response.code"
		return JSON.parse(response.getBody())
	}

	Map subscribe(OAuthAccount account, def subscriptionId) {
		Token accessToken = account.tokenInstance
		String apiVersion = grailsApplication.config.oauth.providers.fitbit.apiVersion
		String subscriptionURL = "http://api.fitbit.com/${apiVersion}/user/-/apiSubscriptions/${subscriptionId}.json"

		Response response = oauthService.postFitbitResource(accessToken, subscriptionURL)
		debug "Added a subscription returns with code: [$response.code] & body [$response.body]"

		Map result = [success: false, status: response.code]

		if(response.code in [200, 201, 409]) {
			result.success = true
			switch(response.code) {
				case 200:
					result.message = "You've already been subscribed."
					break;
				case 201:
					result.message = "" // Successfully subscribed.
					break;
				case 409:
					result.message = "You have already been subscribed"
					break;
			}
		} else {
			account.delete()	// confirms that subscription is not successful.
		}

		//listSubscription(account) // Test after subscribe
		result
	}

	Map unSubscribe(Long userId) throws AuthenticationRequiredException {
		OAuthAccount account = OAuthAccount.findByUserIdAndTypeId(userId, OAuthAccount.FITBIT_ID)
		if (!account) {
			log.info "No subscription found for userId [$userId]"
			return [success: false, message: "No subscription found"]
		}
		String apiVersion = grailsApplication.config.oauth.providers.fitbit.apiVersion
		String subscriptionURL = "http://api.fitbit.com/${apiVersion}/user/-/apiSubscriptions/${userId}.json"

		Response response = oauthService.deleteFitbitResource(account.tokenInstance, subscriptionURL)
		debug "Removed a subscription returns with code: [$response.code] & body [$response.body]"

		if (response.code in [204, 404]) {
			account.delete()
			return [success: true]
		} else if (response.code == 401) {
			throw new AuthenticationRequiredException("fitbit")
		}
		//listSubscription(account)	// Test after un-subscribe
		[success: false]
	}
	
	/**
	 * Method to list the subscriptions for the current account
	 * @param account
	 */
	def listSubscription(OAuthAccount account) {
		String apiVersion = grailsApplication.config.oauth.providers.fitbit.apiVersion
		String subscriptionListURL = "http://api.fitbit.com/${apiVersion}/user/-/apiSubscriptions.json"

		Response responseSubscriptions = oauthService.postFitbitResource(account.tokenInstance, subscriptionListURL)
		debug "Listing subscriptions with code: [$responseSubscriptions.code] & body [$responseSubscriptions.body]"
		return responseSubscriptions
	}

	def queueNotifications(def notifications) {
		debug "Iterating through notifications"
		SimpleDateFormat formatter = new SimpleDateFormat('yyyy-MM-dd', Locale.US)

		notifications.each { notification ->
			debug "Saving " + notification.dump()
			notification.date = formatter.parse(notification.date)
			new FitbitNotification(notification).save(failOnError:true)
		}
	}

	def poll(def notification) {
		String accountId = notification.ownerId
		debug "poll() accountId: [$accountId] & notification: [${notification.dump()}"

		def fitBitTagUnitMap = new FitBitTagUnitMap()
		SimpleDateFormat formatter = new SimpleDateFormat('yyyy-MM-dd', Locale.US)
		SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		if (accountId == null)	// TODO This can never be the case. Remove this. FitbitNotification have not null field
			return false

		long now = new Date().getTime()

		Long lastPoll = lastPollTimestamps.get(accountId)

		if (lastPoll && now - lastPoll < 500) { // don't allow polling faster than once every 500ms
			log.warn "Polling faster than 500ms for fitbit accountid: [$accountId]"
			return false
		}

		lastPollTimestamps.put(accountId, now)
		
		String comment = "(FitBit)"
		String setName = FITBIT_SET_NAME
		String apiVersion = grailsApplication.config.oauth.providers.fitbit.apiVersion
		def accounts = OAuthAccount.findAllByAccountIdAndTypeId(accountId, OAuthAccount.FITBIT_ID)

		try {
			for (OAuthAccount account in accounts) {
				def collectionType = notification.collectionType
				def requestUrl
				Map args = [setName: setName + " " + notification.date, comment: comment]
				Long userId = account.getUserId()
				Integer timeZoneId = User.getTimeZoneId(userId)

				if (collectionType.equals("foods")) {
					requestUrl = "http://api.fitbit.com/${apiVersion}/user"+
						"/${notification.ownerId}/${collectionType}/log/date/${formatter.format(notification.date)}.json"
				} else if (collectionType.equals('body')) {
					//Getting body measurements
					requestUrl = "http://api.fitbit.com/${apiVersion}/user"+
						"/${notification.ownerId}/${collectionType}/date/${formatter.format(notification.date)}.json"
					return false

					//Getting body weight
					/*requestUrl = "http://api.fitbit.com/${apiVersion}/user"+
						"/${notification.ownerId}/${collectionType}/log/weight/date/${formatter.format(notification.date)}.json"
					this.getData(account, requestUrl, false)

					//Getting body fat
					requestUrl = "http://api.fitbit.com/${apiVersion}/user"+
						"/${notification.ownerId}/${collectionType}/log/fat/date/${formatter.format(notification.date)}.json"
					this.getData(account, requestUrl, false)*/
				} else if (collectionType.equals('activities')) {
					requestUrl = "http://api.fitbit.com/${apiVersion}/user" +
						"/${notification.ownerId}/${collectionType}/date/${formatter.format(notification.date)}.json"
					def activityData = this.getData(account, requestUrl, false)
					String oldSetName = formatter.format(notification.date) + "activityfitbit"	// Backward support

					Entry.executeUpdate("delete Entry e where e.setName in :setNames and e.userId = :userId",
						[setNames: [setName, oldSetName], userId: account.userId])

					boolean veryActiveD, moderatelyActiveD, lightlyActiveD, sedentaryActiveD, fairlyActiveD
					veryActiveD = moderatelyActiveD = lightlyActiveD = sedentaryActiveD = fairlyActiveD = true

					if (activityData.summary) {
						fitBitTagUnitMap.buildEntry("steps", activityData.summary?.steps.toBigDecimal(), userId, timeZoneId,
							notification.date, args)
					}

					if(!activityData.summary || activityData.summary?.fairlyActiveMinutes <= 0) {
						fairlyActiveD = false
					} else {
						fitBitTagUnitMap.buildEntry("fairlyActiveMinutes",
						activityData.summary.fairlyActiveMinutes.toBigDecimal(), userId, timeZoneId,
						notification.date, args)
					}

					if(!activityData.summary || activityData.summary.lightlyActiveMinutes <= 0) {
						lightlyActiveD = false
					} else {
						fitBitTagUnitMap.buildEntry("lightlyActiveMinutes",
							activityData.summary.lightlyActiveMinutes.toBigDecimal(), userId, timeZoneId,
							notification.date, args)
					}

					if(!activityData.summary || activityData.summary.sedentaryMinutes <= 0) {
						sedentaryActiveD = false
					} else {
						fitBitTagUnitMap.buildEntry("sedentaryMinutes",
							activityData.summary.sedentaryMinutes.toBigDecimal(), userId, timeZoneId,
							notification.date, args)
					}

					if(!activityData.summary || activityData.summary.veryActiveMinutes <= 0) {
						veryActiveD = false
					} else {
						fitBitTagUnitMap.buildEntry("veryActiveMinutes",
							activityData.summary.veryActiveMinutes.toBigDecimal(), userId, timeZoneId,
							notification.date, args)
					}

					activityData.summary.distances.each { distance ->
						def entryDate = notification.date
						try {
							activityData.activities.each { activity ->
								if (activity.name.equals(distance.activity) && activity.hasStartTime) {
									entryDate = inputFormat.parse(formatter.format(notification.date)+"T"+activity.startTime+":00.000")
									throw new Exception("return from closure")
								}
							}
						} catch(Exception e) {
							//do nothing
						}
						if (!distance.activity.equals('loggedActivities')) { //Skipping loggedActivities
							debug "Importing activity: " + distance.activity
							if ((!veryActiveD && distance.activity.equals("veryActive"))
								||(!moderatelyActiveD && distance.activity.equals("moderatelyActive"))
								||(!lightlyActiveD && distance.activity.equals("lightlyActive"))
								||(!sedentaryActiveD && distance.activity.equals("sedentaryActive"))) {
								println "Discarding activity with 0 time"
							} else {
								fitBitTagUnitMap.buildEntry(distance.activity, distance.distance.toBigDecimal(), userId, timeZoneId,
								entryDate, args)
							}
						}
					}


				} else if (collectionType.equals('sleep')) {
					debug "Fetch sleep data next."
					requestUrl = "http://api.fitbit.com/${apiVersion}/user"+
					"/${notification.ownerId}/${collectionType}/date/${formatter.format(notification.date)}.json"
					def sleepData = this.getData(account, requestUrl, false)
					sleepData.sleep.each { logEntry ->
						Date entryDate = inputFormat.parse(logEntry.startTime)
						String oldSetName = logEntry.logId	// Backward support

						Entry.executeUpdate("delete Entry e where e.setName in :setNames and e.userId = :userId",
							[setNames: [setName, oldSetName], userId: account.userId])

						fitBitTagUnitMap.buildEntry("duration", logEntry.duration.toBigDecimal(), userId, timeZoneId,
							entryDate, args)
						fitBitTagUnitMap.buildEntry("awakeningsCount", logEntry.awakeningsCount.toBigDecimal(), userId, timeZoneId,
							entryDate, args)
						if (logEntry.efficiency > 0 )
							fitBitTagUnitMap.buildEntry("efficiency", logEntry.efficiency.toBigDecimal(), userId, timeZoneId,
							entryDate, args)
					}
				}

			}
		} catch(Exception e) {
			e.printStackTrace()
			return false
		}

		return true
	}

	def getData(OAuthAccount account, def requestUrl, def refreshAll) {
		debug "Fetching data from fitbit with request URL: [$requestUrl]"
		//request.addHeader("Accept-Language","en_US");
		Response response = oauthService.getFitbitResource(account.tokenInstance, requestUrl)

		debug "Fetched collectionType data from fitbit with code: [$response.code] & body: [$response.body]"
		def jsonResponse = JSON.parse(response.getBody())

		if (response.getCode() == 401) {
			return false
		} else {
			return jsonResponse
		}
	}

}
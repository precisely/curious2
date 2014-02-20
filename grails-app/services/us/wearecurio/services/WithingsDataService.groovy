package us.wearecurio.services

import grails.converters.JSON

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.thirdparty.withings.WithingsTagUnitMap
import us.wearecurio.utility.Utils

class WithingsDataService extends DataService {

	static final String BASE_URL = "http://wbsapi.withings.net"
	static final String COMMENT = "(Withings)"
	static final String SET_NAME = "withings import"

	static transactional = true

	WithingsDataService() {
		provider = "Withings"
		typeId = ThirdParty.WITHINGS
	}

	WithingsTagUnitMap tagUnitMap = new WithingsTagUnitMap()

	@Override
	void notificationHandler(String notificationData) {
		JSONObject notification = JSON.parse(notificationData)
		if (!notification.userid) {	// At time of subscription
			return
		}
		Date notificationDate = notification.startdate ? new Date(notification.startdate.toLong() * 1000L) : new Date()

		new ThirdPartyNotification([collectionType: "default", date: notificationDate, ownerId: notification.userid, subscriptionId: "",
			ownerType: "user", typeId: typeId]).save()
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) {
		log.debug "WithingsDataService.getData() account:" + account + " refreshAll: " + refreshAll

		Integer offset = 0
		boolean more = true
		long serverTimestamp = 0
		Long userId = account.getUserId()
		startDate = startDate ?: account.getLastPolled() ?: earlyStartDate

		if (refreshAll)
			Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
					[setName: SET_NAME, userId: userId])

		Map queryParameters = ["action": "getmeas"]
		queryParameters.put("userid", account.getAccountId())

		while (more) {
			if (offset > 0)
				queryParameters.put("offset", offset.toString())

			Long lastPolled = startDate.time / 1000L
			if (!refreshAll)
				queryParameters.put("startdate", lastPolled)

			String subscriptionURL = urlService.makeQueryString(BASE_URL + "/measure", queryParameters)

			JSONObject data = getResponse(account.tokenInstance, subscriptionURL)

			if (data.status != 0) {
				log.warn "Error status returned from withings. Body: ${data}"
				return [success: false, status: data.status]
			}

			Integer timeZoneId = account.timeZoneId ?: User.getTimeZoneId(userId)

			JSONArray groups = data.body.measuregrps
			offset = groups.size()
			more = data.body.more ? true : false
			serverTimestamp = data.body.updatetime * 1000L

			for (group in groups) {
				Date date = new Date(group.date * 1000L)
				JSONArray measures = group.measures
				for (measure in measures) {
					BigDecimal value = new BigDecimal(measure.value, -measure.unit)
					log.debug "type: " + measure.type + " value: " + value
					String tagKey

					switch (measure.type) {
						case 1: // weight (kg)
							tagKey = "weight"
							break

						case 4: // height (m)
							tagKey = "height"
							break

						case 5: // fat free mass (kg)
							tagKey = "fatFreeMass"
							break

						case 6: // fat ratio (%)
							tagKey = "fatRatio"
							break

						case 8: // fat mass weight (kg)
							tagKey = "fatMassWeight"
							break

						case 9: // blood pressure diastolic (mmHg)
							tagKey = "bpDiastolic"
							break

						case 10: // blood pressure systolic (mmHg)
							tagKey = "bpSystolic"
							break

						case 11: // pulse (bpm)
							tagKey = "heartRate"
							break
					}
					tagUnitMap.buildEntry(tagKey, value, userId, timeZoneId, date, COMMENT, SET_NAME)
				}
			}
		}

		if(account.lastPolled) {
			getDataActivityMetrics(account, null, [startDate: account.lastPolled + 1, endDate: new Date()])
		} else {
			getDataActivityMetrics(account, startDate, [:])
		}

		if (serverTimestamp > 0) {
			account.setLastPolled(new Date(serverTimestamp))
			Utils.save(account, true)
		}

		[success: true]
	}

	/**
	 * Used to get & store activity metrics summary for an account.
	 * @param account REQUIRED Account instance for activity data.
	 * @param forDay The date for the activity data. Optional if date range is given.
	 * @return dateRange Date range for to retrieve data against. Optional if forDay param is given
	 *
	 * @see Activity Metrics documentation at http://www.withings.com/en/api
	 */
	Map getDataActivityMetrics(OAuthAccount account, Date forDay, Map dateRange) {
		BigDecimal value

		String description, units, queryDateFormat = "yyyy-MM-dd"

		Map queryParameters = ["action": "getactivity", userid: account.accountId]

		if (forDay) {
			queryParameters["date"] = forDay.format(queryDateFormat)
		} else if (dateRange) {
			queryParameters["startdateymd"] = dateRange.startDate.format(queryDateFormat)
			queryParameters["enddateymd"] = dateRange.endDate.format(queryDateFormat)
		} else {
			// @see Activity Metrics documentation at http://www.withings.com/en/api
			log.debug "Either forDay or dateRange parameter required to pull activity data."
			return [success: false]
		}

		JSONObject data = getResponse(account.tokenInstance, BASE_URL + "/v2/measure", "get", queryParameters)

		if (data.status != 0) {
			log.error "Error status [$data.status] returned while getting withings activity data. [$data]"
			return false
		}

		Long userId = account.userId

		SimpleDateFormat dateFormat = new SimpleDateFormat(queryDateFormat)

		JSONArray activities = data["body"]["activities"]

		activities.each { JSONObject activity ->
			log.debug "Parsing entry with data: $activity"
			Date entryDate = dateFormat.parse(activity["date"])

			Integer timeZoneId = TimeZoneId.look(activity["timezone"]).id

			if (activity["steps"]) {
				tagUnitMap.buildEntry("activitySteps", activity["steps"], userId, timeZoneId, entryDate, COMMENT, SET_NAME)
			}
			if (activity["distance"]) {
				tagUnitMap.buildEntry("activityDistance", activity["distance"], userId, timeZoneId, entryDate, COMMENT, SET_NAME)
			}
			if (activity["calories"]) {
				tagUnitMap.buildEntry("activitySteps", activity["calories"], userId, timeZoneId, entryDate, COMMENT, SET_NAME)
			}
			if (activity["elevation"]) {
				tagUnitMap.buildEntry("activityElevation", activity["elevation"], userId, timeZoneId, entryDate, COMMENT, SET_NAME)
			}
		}

		[success: false]
	}

	def getRefreshSubscriptionsTask() {
		return { refreshSubscriptions() }
	}

	/**
	 * Common method to provide common parameters required at the time of subscribe / unsubscribe.
	 * @param account Instance of OAuthAccount
	 * @param subscription Boolean field to represent if user is subscribing or unsubscribing.
	 * @return Returns common parameters as map.
	 */
	Map getSubscriptionParameters(OAuthAccount account, boolean subscription) {
		String notifyURL = urlService.make([controller: "home", action: "notifywithings"], null, true)
		notifyURL = notifyURL.replace("https://", "http://")	// Providing Non SSL url to work with withings callback.

		Map queryParameters = ["action": subscription ? "subscribe" : "revoke"]
		queryParameters.put("userid", account.accountId)
		queryParameters.put("comment", "Notify Curious app of new data")
		queryParameters.put("callbackurl", notifyURL)	// Not encoding url since, OAuth plugin do it.

		queryParameters
	}

	String getUsersTimeZone(Token tokenInstance, String accountId) {
		log.debug "Getting timezone for accoundId [$accountId] from last day activity data."
		JSONObject data = fetchActivityData(tokenInstance, accountId, new Date() - 1, new Date())	// Getting data for last day
		if (data.status != 0) {
			log.error "Error status [$data.status] returned while getting timezone using activity data. [$data]"
			return null
		}

		if (data["body"]["activities"].size()) {
			return data["body"]["activities"][0].timezone
		}

		log.debug "Getting timezone for accoundId [$accountId] from last 3 months activity data."
		data = fetchActivityData(tokenInstance, accountId, new Date() - 90, new Date())		// Getting data for last 3 months
		if (data["body"]["activities"].size()) {
			return data["body"]["activities"][0].timezone
		}

		log.debug "No timezone found for accountId [$accountId] from activity data."
		return null
	}

	JSONObject fetchActivityData(Token tokenInstance, String accountId, Date startDate, Date endDate) {
		String queryDateFormat = "yyyy-MM-dd"
		Map queryParameters = ["action": "getactivity", userid: accountId]
		queryParameters["startdateymd"] = startDate.format(queryDateFormat)
		queryParameters["enddateymd"] = endDate.format(queryDateFormat)

		getResponse(tokenInstance, BASE_URL + "/v2/measure", "get", queryParameters)
	}

	void listSubscription(OAuthAccount account) {
		Response response = oauthService.getWithingsResource(account.tokenInstance, "http://wbsapi.withings.net/notify?action=list&userid=$account.accountId")
		log.info "Subscription list response, code: [$response.code], body: [$response.body]"
	}

	def refreshSubscriptions() {
		log.debug "refreshSubscriptions()"
		def c = OAuthAccount.createCriteria()

		def now = new Date()
		def weekAgo = new Date(now.getTime() - 7L * 24 * 60 * 60 * 1000)

		def results = c {
			eq("typeId", ThirdParty.WITHINGS)
			ne("accessToken", "")
			lt("lastSubscribed", weekAgo)
		}

		for (OAuthAccount account in results) {
			try {
				subscribe(account)
			} catch (AuthenticationRequiredException e) {
				// Nothing to do.
			}
		}
	}

	@Override
	Map subscribe(Long userId) throws AuthenticationRequiredException {
		log.debug "WithingsDataService.subscribe(): For userId: [$userId]"
		OAuthAccount account = getOAuthAccountInstance(userId)

		if (!account) {
			debug "Authentication required exception"
			throw new AuthenticationRequiredException(provider)
		}

		subscribe(account)
	}

	// Overloaded method.
	Map subscribe(OAuthAccount account) {
		Long userId = account.userId
		Map result = super.subscribe(userId, BASE_URL + "/notify", "get", getSubscriptionParameters(account, true))

		if (result["body"].status == 0) {
			log.debug "Subscription successfull for account: $account"
			account.lastSubscribed = new Date()
			account.save()
			/**
			 * Fetch & store previous data.
			 * @TODO Needs to be done either by events or by grails async processing,
			 * because this will keep user redirect blocked for longer time if there
			 * are greater number of entries in previous data.
			 */
			log.info "Getting user's previous data for account: $account"
			getDataDefault(account, null, false)
			return [success: true]
		}

		log.warn "Subscription failed for account: $account with status: " + result["body"].status

		account.removeAccessToken()		// confirms that subscription is not successful.
		account.save()
		[success: false]
	}

	@Override
	Map unsubscribe(Long userId) {
		debug "WithingsDataService.unsubscribe():" + userId
		OAuthAccount account = getOAuthAccountInstance(userId)

		if (!account) {
			log.info "No OAuthAccount found."
			return [success: false, message: "No subscription found"]
		}

		Map result = super.unsubscribe(userId, BASE_URL + "/notify", "get", getSubscriptionParameters(account, false))

		// 294 status code is for 'no such subscription available to delete'.
		if (result["body"].status in [0, 294]) {
			debug "Unsubscribe succeeded, status: " + result["body"].status
			return [success: true]
		}

		debug "Unsubscribe failed, status: " + result["body"].status
		[success: false]
	}

}
package us.wearecurio.services

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.utils.OAuthEncoder

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty;
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
		provider = "withings"
		typeId = ThirdParty.WITHINGS
	}

	WithingsTagUnitMap tagUnitMap = new WithingsTagUnitMap()

	def urlService

	@Override
	void notificationHandler(String accountId) {
		if (!accountId) {	// At time of subscription
			return
		}

		new ThirdPartyNotification([collectionType: "default", date: new Date(), ownerId: accountId, subscriptionId: "",
			ownerType: "user", typeId: typeId]).save()
	}

	@Override
	Map getDataDefault(OAuthAccount account, Date forDay, boolean refreshAll) {
		log.debug "WithingsDataService.getData() account:" + account + " refreshAll: " + refreshAll

		Integer offset = 0
		boolean more = true
		long serverTimestamp = 0
		Long userId = account.getUserId()

		if (refreshAll)
			Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
					[setName: SET_NAME, userId: userId])

		Map queryParameters = ["action": "getmeas"]
		queryParameters.put("userid", account.getAccountId())

		while (more) {
			if (offset > 0)
				queryParameters.put("offset", offset.toString())

			Long lastPolled = account.lastPolled ? account.lastPolled.time / 1000L : null
			if (lastPolled && !refreshAll)
				queryParameters.put("startdate", lastPolled)

			String subscriptionURL = urlService.makeQueryString(BASE_URL + "/measure", queryParameters)

			JSONObject data = getResponse(account.tokenInstance, subscriptionURL)

			if (data.status != 0) {
				log.warn "Error status returned from withings. Body: ${data}"
				return [success: false, status: data.status]
			}

			Integer timeZoneId = User.getTimeZoneId(userId)

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
			//getDataForActivityMetrics(account, null, [startDate: account.lastPolled + 1, endDate: new Date()])
		} else {
			//getDataForActivityMetrics(account, new Date())
		}

		if (serverTimestamp > 0) {
			account.setLastPolled(new Date(serverTimestamp))
			Utils.save(account, true)
		}

		[success: true]
	}

	/**
	 * Used to get & store activity metrics summary for an account.
	 * @param account Account instance for activity data.
	 * @param forDay The date for the activity data. Optional if date range is given.
	 * @return dateRange Date range for to retrieve data against. Optional if forDay param is given
	 *
	 * @see Activity Metrics documentation at http://www.withings.com/en/api
	 */
	boolean getDataForActivityMetrics(OAuthAccount account, Date forDay, Map dateRange) {
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
			return false
		}

		String dataURL = urlService.makeQueryString(BASE_URL + "/v2/measure", queryParameters)

		JSONObject data = getResponse(account.tokenInstance, dataURL)

		if(data.status != 0) {
			log.error "Something went wrong with withings activity api. [$data]"
			return false
		}

		Long userId = account.userId

		Integer timeZoneId = User.getTimeZoneId(userId)

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd")

		JSONArray activities = data["body"]["activities"]

		activities.each { JSONObject activity ->
			log.debug "Parsing entry for day [$activity.date] with [$activity.steps] steps, [$activity.distance] distance, [$activity.calories] calorie, [$activity.elevation] elevation"
			Date entryDate = dateFormat.parse(activity["date"])

			if(activity["steps"]) {
				tagUnitMap.buildEntry("steps", activity["steps"], userId, timeZoneId, entryDate, COMMENT, SET_NAME)
			}
			if(activity["distance"]) {
				units = "km"
				description = "activity distance"
				value = activity["distance"] / 1000		// Converting to KM
				value = value.toBigDecimal()

				Entry.create(account.userId, entryDate, TimeZoneId.look(activity["timezone"]), description, value,
						units, "(Withings)", SET_NAME)
			}
			if(activity["calories"]) {
				units = "kcal"
				description = "activity calories"
				value = activity["calories"].toBigDecimal()

				Entry.create(account.userId, entryDate, TimeZoneId.look(activity["timezone"]), description, value,
						units, "(Withings)", SET_NAME)
			}
			if(activity["elevation"]) {
				units = "km"
				description = "activity elevation"
				value = activity["elevation"] / 1000		// Converting to KM
				value = value.toBigDecimal()

				Entry.create(account.userId, entryDate, TimeZoneId.look(activity["timezone"]), description, value,
						units, "(Withings)", SET_NAME)
			}
		}

		return true
	}

	def getRefreshSubscriptionsTask() {
		return { refreshSubscriptions() }
	}

	Map getSubscriptionParameteres(OAuthAccount account, boolean isSubscribing) {
		String notifyURL = urlService.make([controller: "home", action: "notifywithings"], null, true)

		Map queryParameters = ["action": isSubscribing ? "subscribe" : "revoke"]
		queryParameters.put("userid", account.accountId)
		queryParameters.put("comment", OAuthEncoder.encode("Notify Curious app of new data"))
		queryParameters.put("callbackurl", notifyURL)

		queryParameters
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
			lt("lastSubscribed", weekAgo)
		}

		for (OAuthAccount account in results) {
			this.subscribe(account)
		}
	}

	@Override
	Map subscribe() throws AuthenticationRequiredException {
		OAuthAccount account = getOAuthAccountInstance()

		if (!account) {
			throw new AuthenticationRequiredException(provider)
		}

		Map result = super.subscribe(BASE_URL + "/notify", "get", getSubscriptionParameteres(account, true))

		if (result["body"].status == 0) {
			account.lastSubscribed = new Date()
			account.save()
			return [success: true]
		}

		account.delete()	// confirms that subscription is not successful.
		[success: false]
	}

	@Override
	Map unsubscribe() {
		OAuthAccount account = getOAuthAccountInstance()

		if (!account) {
			log.info "No OAuthAccount found."
			return [success: false, message: "No subscription found"]
		}

		Map result = super.unsubscribe(BASE_URL + "/notify", "get", getSubscriptionParameteres(account, false))

		// 294 status code is for 'no such subscription available to delete'.
		if (result["body"].status in [0, 294]) {
			account.delete()
			return [success: true]
		}

		//listSubscription(account)	// Test after unsubscribe
		[success: false]
	}

}
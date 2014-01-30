package us.wearecurio.services

import grails.converters.JSON

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token
import org.scribe.utils.OAuthEncoder

import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.TimeZoneId
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.utility.Utils

class WithingsDataService {

	def oauthService
	def urlService

	private static def log = LogFactory.getLog(this)
	private static final BigDecimal KG_TO_POUNDS = new BigDecimal(220462, 5)
	private static final BigDecimal M_TO_FEET = new BigDecimal(328084, 5)
	private static final String WITHINGS_SET_NAME = "withings import"
	Map lastPollTimestamps = new HashMap<Long,Long>() // prevent DOS attacks

	static transactional = true

	static debug(str) {
		log.debug(str)
	}

	Map authorizeAccount(Token tokenInstance, Long userId, String withingsUserId) throws AuthenticationRequiredException {
		debug "Authorizing WithingsDataService for user " + userId + " and userId " + withingsUserId
		if (!tokenInstance || !tokenInstance.token)
			throw new AuthenticationRequiredException("withings")

		OAuthAccount withingsAccount = OAuthAccount.createOrUpdate(OAuthAccount.WITHINGS_ID, userId, withingsUserId,
				tokenInstance.token, tokenInstance.secret)

		Utils.save(withingsAccount, true)

		debug "Created withingsAccount " + withingsAccount

		Map result = subscribe(withingsAccount)

		if(result.success) {
			poll(withingsUserId)
		}
		result
	}

	def poll() {
		def accounts = OAuthAccount.findAllByTypeId(OAuthAccount.WITHINGS_ID)

		for (OAuthAccount account in accounts) {
			this.getData(account, false)
		}
	}

	boolean poll(String accountId) {
		debug "poll() accountId:" + accountId

		if (accountId == null)
			return false

		long now = new Date().getTime()

		Long lastPoll = lastPollTimestamps.get(accountId)

		if (lastPoll != null && now - lastPoll < 500) { // don't allow polling faster than once every 500ms
			log.warn "Polling faster than 500ms for withings accountid: [$accountId]"
			return false
		}

		lastPollTimestamps.put(accountId, now)

		def accounts = OAuthAccount.findAllByAccountIdAndTypeId(accountId, OAuthAccount.WITHINGS_ID)

		for (OAuthAccount account in accounts) {
			this.getData(account, false)
		}

		return true
	}

	Map subscribe(OAuthAccount account) {
		debug "subscribe() account:" + account

		Long userId = account.getUserId()
		String notifyURL = urlService.make([controller: "home", action: "notifywithings"], null, true)

		Map queryParameters = ["action": "subscribe"]
		queryParameters.put("userid", account.accountId)
		queryParameters.put("comment", OAuthEncoder.encode("Notify Curious app of new data"))
		queryParameters.put("callbackurl", OAuthEncoder.encode(notifyURL))

		String subscriptionURL = urlService.makeQueryString("http://wbsapi.withings.net/notify", queryParameters)

		Response response = oauthService.getWithingsResource(account.tokenInstance, subscriptionURL)

		log.info "Subscribe return with code: [$response.code] & body: [$response.body]"
		JSONObject parsedResponse = JSON.parse(response.body)

		if (parsedResponse.status == 0) {
			account.setLastSubscribed(new Date())
			debug "set last subscribed: " + account.getLastSubscribed()
			Utils.save(account)
			return [success: true]
		}
		account.delete()	// confirms that subscription is not successful.
		[success: false]
	}

	Map unSubscribe(Long userId) {
		debug "unSubscribe() account:" + userId
		OAuthAccount account = OAuthAccount.findByUserIdAndTypeId(userId, OAuthAccount.WITHINGS_ID)
		if(!account) {
			log.info "No subscription found for userId [$userId]"
			return [success: false, message: "No subscription found"]
		}

		String notifyURL = urlService.make([controller: "home", action: "notifywithings"], null, true)
		
		Map queryParameters = ["action": "revoke"]
		queryParameters.put("userid", account.accountId)
		queryParameters.put("callbackurl", OAuthEncoder.encode(notifyURL))
		//listSubscription(account)	// Test before un-subscribe

		String subscriptionURL = urlService.makeQueryString("http://wbsapi.withings.net/notify", queryParameters)

		Response response = oauthService.getWithingsResource(account.tokenInstance, subscriptionURL)

		log.info "Unsubscribe return with code: [$response.code] & body: [$response.body]"
		JSONObject parsedResponse = JSON.parse(response.body)
		
		// 294 status code is for 'no such subscription available to delete'.
		if (parsedResponse.status in [0, 294]) {
			account.delete()
			return [success: true]
		}
		//listSubscription(account)	// Test after un-subscribe
		[success: false]
	}

	/**
	 * Method to list the subscriptions for the current account
	 * @param account
	 */
	void listSubscription(OAuthAccount account) {
		Response response = oauthService.getWithingsResource(account.tokenInstance, "http://wbsapi.withings.net/notify?action=list&userid=$account.accountId")
		log.info "Subscription list response, code: [$response.code], body: [$response.body]"
	}

	def getData(OAuthAccount account) {
		return getData(account, false)
	}

	def refreshSubscriptions() {
		debug "refreshSubscriptions()"
		def c = OAuthAccount.createCriteria()

		def now = new Date()
		def weekAgo = new Date(now.getTime() - 7L * 24 * 60 * 60 * 1000)

		def results = c {
			eq("typeId", OAuthAccount.WITHINGS_ID)
			lt("lastSubscribed", weekAgo)
		}

		for (OAuthAccount account in results) {
			this.subscribe(account)
		}
	}

	def getRefreshSubscriptionsTask() {
		return { refreshSubscriptions() }
	}

	def getData(OAuthAccount account, boolean refreshAll) {
		debug "WithingsDataService.getData() account:" + account + " refreshAll: " + refreshAll

		Integer offset = 0
		boolean more = true
		long serverTimestamp = 0
		Long userId = account.getUserId()

		if (refreshAll)
			Entry.executeUpdate("delete Entry e where e.setName = :setName and e.userId = :userId",
					[setName: WITHINGS_SET_NAME, userId: userId])

		while (more) {
			Map queryParameters = ["action": "getmeas"]
			queryParameters.put("userid", account.getAccountId().toString())
			if (offset > 0)
				queryParameters.put("offset", offset.toString())

			Long lastPolled = account.getLastPolled() ? account.getLastPolled().getTime() / 1000L : null
			if (lastPolled != null && (!refreshAll))
				queryParameters.put("startdate", lastPolled .toString())

			String dataURL = urlService.makeQueryString("http://wbsapi.withings.net/measure", queryParameters)
			Response response = oauthService.getWithingsResource(account.tokenInstance, dataURL)

			log.info "Got it! Lets see what we found... Code: [$response.code] & Body: [$response.body]"

			def data
			try {
				data = JSON.parse(response.getBody())
			} catch (Exception e) {
				debug "WithingsDataService.getData(): Exception while parsing response " + e
				return false
			}
			if (data.status != 0) {
				return false
			}
			def groups = data.body.measuregrps
			offset = groups.size()
			more = data.body.more ? true : false
			serverTimestamp = data.body.updatetime * 1000L
			for (group in groups) {
				Date date = new Date(group.date * 1000L)
				def measures = group.measures
				for (measure in measures) {
					BigDecimal value = new BigDecimal(measure.value, -measure.unit)
					System.out.println("type: " + measure.type + " value: " + value)
					int amountPrecision = 2
					String description
					String units

					switch (measure.type) {
						case 1: // weight (kg)
						description = "weight"
						amountPrecision = 2
						value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "lbs"
						break

						case 4: // height (m)
						description = "height"
						amountPrecision = 5
						value = value.multiply(M_TO_FEET).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "feet"
						break

						case 5: // fat free mass (kg)
						description = "fat free mass"
						amountPrecision = 2
						value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "lbs"
						break

						case 6: // fat ratio (%)
						description = "fat ratio"
						units = "%"
						break

						case 8: // fat mass weight (kg)
						description = "fat mass weight"
						amountPrecision = 2
						value = value.multiply(KG_TO_POUNDS).setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "lbs"
						break

						case 9: // blood pressure diastolic (mmHg)
						description = "blood pressure diastolic"
						value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "mmHg"
						break

						case 10: // blood pressure systolic (mmHg)
						description = "blood pressure systolic"
						value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "mmHg"
						break

						case 11: // pulse (bpm)
						description = "heart rate"
						value = value.setScale(amountPrecision, BigDecimal.ROUND_HALF_UP)
						units = "bpm"
						break
					}
					def entry = Entry.create(userId, date, TimeZoneId.look("America/Los_Angeles"), description, value, units, "(Withings)", WITHINGS_SET_NAME, amountPrecision)
				}
			}
		}

		if (serverTimestamp > 0) {
			account.setLastPolled(new Date(serverTimestamp))
			Utils.save(account, true)
		}
	}

}
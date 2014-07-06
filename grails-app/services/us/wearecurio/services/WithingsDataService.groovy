package us.wearecurio.services

import grails.converters.JSON

import org.springframework.transaction.annotation.Transactional

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.*
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException
import us.wearecurio.thirdparty.TooManyRequestsException
import us.wearecurio.thirdparty.withings.WithingsTagUnitMap
import us.wearecurio.utility.Utils
import java.util.concurrent.PriorityBlockingQueue

class WithingsDataService extends DataService {

	static final String BASE_URL = "http://wbsapi.withings.net"
	static final String COMMENT = "(Withings)"
	static final String SET_NAME = "WI"
	static Date intraDayOverQueryRateTimestamp = null
	static PriorityBlockingQueue<IntraDayQueueItem> intraDayQueue = new PriorityBlockingQueue<IntraDayQueueItem>(600)
	static transactional = false

	WithingsDataService() {
		provider = "Withings"
		typeId = ThirdParty.WITHINGS
	}

	WithingsTagUnitMap tagUnitMap = new WithingsTagUnitMap()

	@Override
	@Transactional
	void notificationHandler(String notificationData) {
		JSONObject notification = JSON.parse(notificationData)
		if (!notification.userid) {	// At time of subscription
			return
		}
		Date notificationDate = notification.startdate ? new Date(notification.startdate.toLong() * 1000L) : new Date()

		saveNotification(notificationDate, notification.userid)
	}

	@Transactional
	void saveNotification(Date notificationDate, String accountId) {
		new ThirdPartyNotification([collectionType: "default", date: notificationDate, ownerId: accountId, subscriptionId: "",
			ownerType: "user", typeId: typeId]).save()
	}

	@Transactional
	void saveNotificationForPreviousData(OAuthAccount account) {
		saveNotification(earlyStartDate, account.accountId)
	}

	@Override
	@Transactional
	Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException {
		log.debug "WithingsDataService.getData() account:" + account + " refreshAll: " + refreshAll

		Integer offset = 0
		boolean more = true
		long serverTimestamp = 0
		def setName = SET_NAME
		Long userId = account.getUserId()
		startDate = startDate ?: account.getLastPolled() ?: earlyStartDate
		log.debug "WithingsDataService.getData() start date:" + startDate
		if (refreshAll)
			Entry.executeUpdate("delete Entry e where e.setIdentifier like :setIdentifier and e.userId = :userId",
					[setIdentifier: Identifier.look(SET_NAME), userId: userId]) // Using like for backward compatibility

		Integer timeZoneId = getTimeZoneId(account)

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
				if (data.status == 342) {	// Token expired or invalid
					throw new InvalidAccessTokenException("Withings", account)
				}
				return [success: false, status: data.status]
			}

			JSONArray groups = data.body.measuregrps
			offset = groups.size()
			more = data.body.more ? true : false
			serverTimestamp = data.body.updatetime * 1000L

			for (group in groups) {
				Date date = new Date(group.date * 1000L)
				setName = SET_NAME + "m" + date
				JSONArray measures = group.measures
				Entry.executeUpdate("delete Entry e where e.setIdentifier = :setIdentifier and e.userId = :userId",
					[setIdentifier: Identifier.look(setName) , userId: userId]) 

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
					tagUnitMap.buildEntry(tagKey, value, userId, timeZoneId, date, COMMENT, setName)
				}
			}
		}

		if(account.lastPolled) {
			getDataActivityMetrics(account, account.lastPolled - 1, new Date() + 1)
		} else {			
			getDataActivityMetrics(account, startDate - 1, new Date() + 1)
		}

		if (serverTimestamp > 0) {
			DatabaseService.retry(account) {
				account.setLastPolled(new Date(serverTimestamp))
				account.merge()
				account.save()
			}
		}

		[success: true]
	}
	
	protected static final String QUERY_DATE_FORMAT = "yyyy-MM-dd"

	/**
	 * Used to get & store activity metrics summary for an account.
	 * @param account REQUIRED Account instance for activity data.
	 * @param forDay The date for the activity data. Optional if date range is given.
	 * @return dateRange Date range for to retrieve data against. Optional if forDay param is given
	 *
	 * @see Activity Metrics documentation at http://www.withings.com/en/api
	 */
	@Transactional
	Map getDataActivityMetrics(OAuthAccount account, Date startDate, Date endDate) throws InvalidAccessTokenException {
		log.debug "WithingsDataService.getDataActivityMetrics() account:" + account + " dateRange:" + (startDate?:'null') + ":" + (endDate?:'null')
		
		BigDecimal value

		String description, units
		
		if (startDate == null || endDate == null) {
			return [success: false]
		}

		Map queryParameters = getActivityDataParameters(account.accountId, startDate, endDate, false)

		JSONObject data = getResponse(account.tokenInstance, BASE_URL + "/v2/measure", "get", queryParameters)

		if (data.status != 0) {
			log.error "Error status [$data.status] returned while getting withings activity data. [$data]"
			if (data.status == 342) {	// Token expired or invalid
				throw new InvalidAccessTokenException("Withings", account)
			}
			return [success: false]
		}

		Long userId = account.userId

		SimpleDateFormat dateFormat = new SimpleDateFormat(QUERY_DATE_FORMAT)

		JSONArray activities = data["body"]["activities"]

		def datesWithSummaryData = []
		def setName = SET_NAME

		activities.each { JSONObject activity ->
			log.debug "Parsing entry with data: $activity"
			
			TimeZoneId timeZoneId = TimeZoneId.look(activity["timezone"])
			Integer timeZoneIdNumber = timeZoneId.getId()
			TimeZone timeZone = timeZoneId.toTimeZone()
			
			dateFormat.setTimeZone(timeZone)
			
			Date entryDate = dateFormat.parse(activity["date"])
			if (activity['steps'] > 0) {
				datesWithSummaryData.push(entryDate)
			}
			entryDate = new Date(entryDate.getTime() + 12 * 60 * 60000L) // move activity time 12 hours later to make data appear at noon
			setName = SET_NAME + "a" + entryDate.getTime()/1000
			Entry.executeUpdate("delete Entry e where e.setIdentifier = :setIdentifier and e.userId = :userId",
				[setIdentifier: Identifier.look(setName), userId: userId]) 
			def args = ['isSummary':true] // Indicating that these entries are summary entries
			
			if (activity["steps"]) {
				tagUnitMap.buildEntry("activitySteps", activity["steps"], userId, 
					timeZoneIdNumber, entryDate, COMMENT, setName, args)
			}
			if (activity["distance"]) {
				tagUnitMap.buildEntry("activityDistance", activity["distance"], userId, 
					timeZoneIdNumber, entryDate, COMMENT, setName, args)
			}
			if (activity["calories"]) {
				tagUnitMap.buildEntry("activityCalorie", activity["calories"], userId, 
					timeZoneIdNumber, entryDate, COMMENT, setName, args)
			}
			if (activity["elevation"]) {
				tagUnitMap.buildEntry("activityElevation", activity["elevation"], userId, 
					timeZoneIdNumber, entryDate, COMMENT, setName, args)
			}
		}
		
		if (datesWithSummaryData.size() > 0) {
			log.debug "WithingsDataService: Adding items to the intra day queue"
			datesWithSummaryData.each { summaryDate ->
				def intraDayQueueItem = new IntraDayQueueItem(oauthAccountId: account.id, queryDate: summaryDate)
				Utils.save(intraDayQueueItem, true)
				try {
					intraDayQueue.add(intraDayQueueItem)
				} catch (IllegalStateException queueFullException) {
					log.debug "WithingsDataService: intraDayQueue is full" 
				}
			}
			synchronized(intraDayQueue) {
				intraDayQueue.notify()
			}
		}
		[success: true]
	}


	@Transactional
	Map getDataIntraDayActivity(OAuthAccount account, Date startDate, Date endDate) 
		throws InvalidAccessTokenException, TooManyRequestsException {
			def intraDayResponse = fetchActivityData(account, account.accountId, startDate, endDate, true)
			def userId = account.userId
			def entryDate
			def setName
			Integer timeZoneIdNumber = account.timeZoneId

			if (intraDayResponse.status == 601) {
				log.debug "WithingsDataService.getDataIntraDayActivity: TooManyRequestsException code 601" 
				throw new TooManyRequestsException(provider)	
			}
			
			log.debug("WithingsDataService.getDataIntraDayActivity: Processing intra day data size " 
				+ intraDayResponse.body.series?.size())
			def aggregatedData = resetAggregatedData()
			def lastEntryTimestamp = 0
			def intraDayData = intraDayResponse.body?.series?.sort()
			def index = 1
			intraDayData?.each {  timestamp, data ->
				log.debug("WithingsDataService.getDataIntraDayActivity: " + timestamp)
				def entryTimestamp = Long.parseLong(timestamp)
				entryDate = new Date(entryTimestamp * 1000L)
				setName = SET_NAME + "i" + timestamp
				try {
					Entry.executeUpdate("delete Entry e where e.setIdentifier = :setIdentifier and e.userId = :userId",
							[setIdentifier: Identifier.look(setName), userId: userId]) 
				
				} catch (org.springframework.dao.CannotAcquireLockException le) {
					log.debug("WithingsDataService.getDataIntraDayActivity: CannotAcquireLockException")
					le.printStackTrace()
				}
				log.debug("WithingsDataService.getDataIntraDayActivity: Starting to aggregate data")
				
				if (data.size() > 1) {
					data.each { metric, amount ->
						log.debug("WithingsDataService.getDataIntraDayActivity: ${metric} ${amount} for ${timestamp}")
						aggregatedData[metric] += amount
					}
				}

				log.debug("WithingsDataService.getDataIntraDayActivity: entryTimestamp - ${entryTimestamp}, lastEntryTimestamp -  ${lastEntryTimestamp}")
				log.debug("WithingsDataService.getDataIntraDayActivity: timestamp difference: ${entryTimestamp - lastEntryTimestamp}")
				if (lastEntryTimestamp == 0) {
					lastEntryTimestamp = entryTimestamp
				}
				if ((entryTimestamp - lastEntryTimestamp) < 301 && index < intraDayData.size()) {
					log.debug("WithingsDataService.getDataIntraDayActivity: Next timestamp is too close continuing to aggregate")
					//continue aggregating
				} else {
					log.debug("WithingsDataService.getDataIntraDayActivity: Creating New Chunk ${entryTimestamp}")
					aggregatedDataToEntries(aggregatedData, userId, timeZoneIdNumber, entryDate, COMMENT, setName)
					aggregatedData = resetAggregatedData()
				}
				lastEntryTimestamp = entryTimestamp
				index++
			}

			[success: true]
	}

	def resetAggregatedData() {
		return ['steps': 0, 'distance': 0, 'calories': 0, 'elevation': 0,'duration': 0 ]
	}
	
	def aggregatedDataToEntries(def data, def userId, def timeZoneIdNumber, def entryDate, def comment, def setName) {
		data.each { metric, amount ->
			log.debug("WithingsDataService.getDataIntraDayActivity: Creating entry for ${metric} ${amount}")
			if (amount == 0)
				return
			if (metric.equals("steps")) {
				tagUnitMap.buildEntry("activitySteps", amount, userId, 
					timeZoneIdNumber, entryDate, comment, setName)
			}
			if (metric.equals("distance")) {
				tagUnitMap.buildEntry("activityDistance", amount, userId, 
					timeZoneIdNumber, entryDate, comment, setName)
			}
			if (metric.equals("calories")) {
				tagUnitMap.buildEntry("activityCalorie", amount, userId, 
					timeZoneIdNumber, entryDate, comment, setName)
			}
			if (metric.equals("elevation")) {
				tagUnitMap.buildEntry("activityElevation", amount, userId, 
					timeZoneIdNumber, entryDate, comment, setName)
			}
			if (metric.equals("duration")) {
				tagUnitMap.buildEntry("activityDuration", amount, userId, 
					timeZoneIdNumber, entryDate, comment, setName)
			}
		}
	}

	def populateIntraDayQueue() {
		def intraDayQueueItems = IntraDayQueueItem.withCriteria([max: 300]) {
			order("oauthAccountId", "asc")
		}

		try {
			if (intraDayQueueItems.size() > 0) {
				intraDayQueue.addAll(intraDayQueueItems as List)
			}
		} catch (IllegalStateException queueFullException) {
			log.debug "WithingsDataService: intraDayQueue is full" 
		}
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
	@Transactional
	Map getSubscriptionParameters(OAuthAccount account, boolean subscription) {
		String notifyURL = urlService.make([controller: "home", action: "notifywithings"], null, true)
		notifyURL = notifyURL.replace("https://", "http://")	// Providing Non SSL url to work with withings callback.

		Map queryParameters = ["action": subscription ? "subscribe" : "revoke"]
		queryParameters.put("userid", account.accountId)
		queryParameters.put("comment", "Notify Curious app of new data")
		queryParameters.put("callbackurl", notifyURL)	// Not encoding url since, OAuth plugin do it.

		queryParameters
	}
	
	@Transactional
	Map getActivityDataParameters(String accountId, Date startDate, Date endDate, boolean intraDay) {
		log.debug "WithingsDataService.getActivityDataParameters() accountId:" + 
			accountId + " startDate: " + startDate + " endDate: " + endDate + " intraDay " + intraDay
		
		Map queryParameters
		
		if (intraDay) {
			queryParameters = ["action": "getintradayactivity", userid: accountId]
			queryParameters["startdate"] = (startDate.getTime()/1000).toString()
			queryParameters["enddate"] = (endDate.getTime()/1000).toString()
		} else {
			queryParameters = ["action": "getactivity", userid: accountId]
			if (endDate) {
				queryParameters["startdateymd"] = startDate.format(QUERY_DATE_FORMAT)
				queryParameters["enddateymd"] = endDate.format(QUERY_DATE_FORMAT)
			} else {
				queryParameters["date"] = startDate.format(QUERY_DATE_FORMAT)
			}
		}
		return queryParameters
	}

	String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		getUsersTimeZone(account.tokenInstance, account.accountId)
	}

	/**
	 * Hack of Withings API for getting users timezone.
	 * @param tokenInstance
	 * @param accountId
	 * @return
	 */
	@Transactional
	String getUsersTimeZone(Token tokenInstance, String accountId) {
		log.debug "Getting timezone for accoundId [$accountId] from last day activity data."
		JSONObject data = fetchActivityData(tokenInstance, accountId, new Date() - 1, new Date(), false)	// Getting data for last day
		if (data.status != 0) {
			log.error "Error status [$data.status] returned while getting timezone using activity data. [$data]"
			return null
		}

		if (data["body"]["activities"].size()) {
			return data["body"]["activities"][0].timezone
		}

		log.debug "Getting timezone for accoundId [$accountId] from last 3 months activity data."
		data = fetchActivityData(tokenInstance, accountId, new Date() - 90, new Date(), false)		// Getting data for last 3 months
		if (data["body"]["activities"].size()) {
			return data["body"]["activities"][0].timezone
		}

		log.debug "No timezone found for accountId [$accountId] from activity data."
		return null
	}

	@Transactional
	JSONObject fetchActivityData(Token tokenInstance, String accountId, Date startDate, Date endDate, boolean intraDay) {
		log.debug "WithingsDataService.fetchActivityData() accountId:" + accountId + " startDate: " + startDate + " endDate: " + endDate
		
		Map queryParameters = getActivityDataParameters(accountId, startDate, endDate, intraDay)

		getResponse(tokenInstance, BASE_URL + "/v2/measure", "get", queryParameters)
	}
	
	@Transactional
	JSONObject fetchActivityData(OAuthAccount account, String accountId, Date startDate, Date endDate, boolean intraDay) {
		fetchActivityData(account.tokenInstance, accountId, startDate, endDate, intraDay)
	}

	@Transactional
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
			subscribe(account)
		}
	}

	@Override
	@Transactional
	Map subscribe(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		log.debug "WithingsDataService.subscribe(): For userId: [$userId]"
		OAuthAccount account = getOAuthAccountInstance(userId)

		subscribe(account)
	}

	// Overloaded method.
	@Transactional
	Map subscribe(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException {
		checkNotNull(account)
		Long userId = account.userId
		Map response = super.subscribe(userId, BASE_URL + "/notify", "get", getSubscriptionParameters(account, true))

		int withingsResponseStatus = response["body"].status

		return DatabaseService.retry(account) {
			if (withingsResponseStatus == 0) {
				log.debug "Subscription successful for account: $account"
				account.lastSubscribed = new Date()
				account.save(flush:true)
				return [success: true, account: account]
			}
			log.warn "Subscription failed for account: $account with status: " + withingsResponseStatus
			Map result = [success: false, status: withingsResponseStatus, account: account]
	
			switch (withingsResponseStatus) {
				case 293:	// Notification URL is not responding.
				case 2555:	// Notification URL not found.
					result.message = "Please try again after some time"
					break
				case 342:
					throw new InvalidAccessTokenException("withings", account)
					break
			}
	
			account.removeAccessToken()		// confirms that subscription is not successful.
			account.save(flush:true)
			
			return result
		}
	}

	@Override
	@Transactional
	Map unsubscribe(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		debug "WithingsDataService.unsubscribe():" + userId
		OAuthAccount account = getOAuthAccountInstance(userId)
		checkNotNull(account)

		Map result = super.unsubscribe(userId, BASE_URL + "/notify", "get", getSubscriptionParameters(account, false))

		int withingsResponseCode = result["body"].status

		// 294 status code is for 'no such subscription available to delete'.
		if (withingsResponseCode in [0, 294]) {
			debug "Unsubscribe succeeded, status: " + result["body"].status
			return [success: true]
		}
		if (withingsResponseCode == 342) {
			throw new InvalidAccessTokenException("Withings", account)
		}

		log.warn "Unsubscribe failed, status: $withingsResponseCode"
		[success: false]
	}

}

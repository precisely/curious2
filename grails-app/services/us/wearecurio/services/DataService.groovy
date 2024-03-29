package us.wearecurio.services

import grails.converters.JSON
import grails.util.Environment
import groovy.transform.Synchronized
import us.wearecurio.datetime.DateUtils
import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import javax.annotation.PostConstruct

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token
import org.springframework.transaction.annotation.Transactional

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.utility.Utils
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.thirdparty.MissingOAuthAccountException

abstract class DataService {

	static transactional = false

	static def log = LogFactory.getLog(this)

	static debug(str) {
		log.debug(str)
	}

	def grailsApplication
	def oauthService
	def urlService
	def databaseService

	Map lastPollTimestamps = new HashMap<Long,Long>() // prevent DOS attacks

	String listSubscriptionURL
	String profileURL
	String provider		// Provider name as configured in Config.groovy to authenticate & query.
	String subscribeURL

	/**
	 * Integer field to represent the third party type. Used to query OAuthAccount
	 * instances for respective services.
	 */
	ThirdParty typeId

	String unsubscribeURL

	private static Map<ThirdParty, DataService> dataServiceMap = [:]

	// very early start date
	static final Date earlyStartDate

	static {
		if (Environment.current != Environment.PRODUCTION) {
			earlyStartDate = new Date() - 60
		} else {
			earlyStartDate = new Date(-5364658800L)
		}
	}

	static class DataRequestContext {
		Set<String> alreadyUnset
		List<Entry> entriesInPollRange = null
		Integer totalEntries, currentOffset = 0, max = 5000
		Date pollStartDate, pollEndDate
		List<Identifier> setIdentifiers
		Long pollingUserId

		DataRequestContext() {
			alreadyUnset = new HashSet<String>()
		}

		DataRequestContext(Date startDate, Date endDate, List<Identifier> setIdentifiers, Long userId) {
			alreadyUnset = new HashSet<String>()
			pollStartDate = startDate
			pollEndDate = endDate ?: DateUtils.getEndOfTheDay()
			this.setIdentifiers = setIdentifiers
			pollingUserId = userId
		}

		void initEntrylist() {
			entriesInPollRange = null
			totalEntries = currentOffset = 0
		}

		Entry entryAlreadyExists(Map entryMap) {
			boolean entriesAvailableInPollRange = entriesInPollRange
			Entry entry
			if (!entriesInPollRange || entriesInPollRange.last().date.compareTo(entryMap.date) < 0) {
				entriesAvailableInPollRange = getNextEntriesInPollRange()
			}
			if (entriesAvailableInPollRange) {
				entry = entriesInPollRange.find {
					(it.units == entryMap.amount["units"] && !it.date.compareTo(entryMap.date) &&
							it.setIdentifier.toString() == entryMap.setName && it.tag == entryMap.amount.tag)
				}
			}
			return entry
		}

		boolean getNextEntriesInPollRange() {
			if (entriesInPollRange && currentOffset >= totalEntries) {
				return false
			}
			Map resultSet = Entry.getImportedEntriesWithinRange(pollStartDate, pollEndDate, setIdentifiers, pollingUserId,
					max, currentOffset)
			entriesInPollRange = resultSet.entries
			totalEntries = resultSet.totalEntries
			currentOffset += max
			return true
		}
	}

	// register data service for lookup
	@PostConstruct
	void registerDataService() {
		dataServiceMap[typeId] = this
	}

	static getDataServiceForTypeId(ThirdParty typeId) {
		return dataServiceMap[typeId]
	}

	/**
	 * Used to check if certain instances are not null. And if null
	 * throw appropriate exception.
	 * @param instance Can be one of OAuthAccount or Token
	 * 
	 * @throws MissingOAuthAccountException, InvalidAccessTokenException
	 */
	void checkNotNull(def instance) throws MissingOAuthAccountException, InvalidAccessTokenException {
		if (!instance) {
			log.debug "Authentication required. Null instance received"
			throw new MissingOAuthAccountException(provider)
		}

		if (((instance instanceof Token) && !instance.token) || ((instance instanceof OAuthAccount) && !instance.accessToken)) {
			log.debug "Authentication required. Either null or blank token received."
			throw new InvalidAccessTokenException()
		}
	}

	/**
	 * Get a list of all OAuthAccount instances based on accountId, since there may
	 * be multiple instance of OAuthAccount for a same accountId. For example: Withings.
	 * @param accountId Account Id of respective third party.
	 * @return Returns list of OAuthAccount.
	 */
	@Transactional
	List<OAuthAccount> getAllOAuthAccounts(String accountId) {
		OAuthAccount.findAllByTypeIdAndAccountId(typeId, accountId)
	}

	/**
	 * Used to process actual data which may depends on service to service for different
	 * third party. Needs to be implemented on each services.
	 * @param account Instance of OAuthAccount
	 * @param startDate Start date for polling or null to get all new records
	 * @param endDate End date for polling or null to get all new records
	 * @param refreshAll Boolean field used to clear all existing records.
	 * @return	Returns a map with required data.
	 */
	@Transactional
	abstract Map getDataDefault(OAuthAccount account, Date startDate, Date endDate, boolean refreshAll, DataRequestContext context) throws
			InvalidAccessTokenException

	Map getDataDefault(OAuthAccount account, Date forDate, boolean refreshAll, DataRequestContext context) throws InvalidAccessTokenException {
		log.debug "getDataDefault(): account ${account?.id} forDay: $forDate refreshAll: $refreshAll"
		return getDataDefault(account, forDate, null, refreshAll, context)
	}

	/**
	 * Returns the OAuthAccount instance for given userId.
	 * @param userId User id for which instance needs to be fetched.
	 */
	@Transactional
	OAuthAccount getOAuthAccountInstance(Long userId) {
		OAuthAccount.findByTypeIdAndUserId(typeId, userId)
	}

	@Transactional
	JSONElement getResponse(OAuthAccount account, String requestURL, String method = "get", Map queryParams = [:], Map requestHeaders = [:])
	throws InvalidAccessTokenException {
		try {
			return getResponse(account.tokenInstance, requestURL, method, queryParams, requestHeaders)
		} catch (InvalidAccessTokenException e) {
			throw new InvalidAccessTokenException(e.provider, account)
		}
	}

	/**
	 * Generic method to hit ThirdParty API resources & returns parsed API response along with
	 * an `getCode()` method to represent the status of the API call.
	 * @param tokenInstance REQUIRED Instance of Token to call against.
	 * @param requestURL REQUIRED URL of the API resource.
	 * @param method OPTIONAL HTTP method for API resource. Default "get". Can be: "post", "delete" etc.
	 * @param queryParams OPTIONAL A map containing key value data as query string parameter.
	 * 		  queryParams must not be url encoded. Since OAuth plugin do this.
	 * @param requestHeader OPTIONAL A map containing key value data for request headers to be send.
	 * @return Returns the parsed response in JSON format along with an additional dynamic method
	 * `getCode()` to get status of the API call. Returned response can be either JSONObject or JSONArray
	 * 
	 * @throws InvalidAccessTokenException if any API call returns 401 response code.
	 */
	@Transactional
	JSONElement getResponse(Token tokenInstance, String requestURL, String method = "get", Map queryParams = [:], Map requestHeaders = [:])
			throws InvalidAccessTokenException {
		long currentTime = System.currentTimeMillis()

		log.debug "[$currentTime] About to request data for [$provider] with request URL: [$requestURL] & method: " +
				"$method"

		checkNotNull(tokenInstance)

		String methodSuffix = queryParams ? "ResourceWithQuerystringParams" : "Resource"

		Response response = oauthService."${method}${provider}${methodSuffix}"(tokenInstance, requestURL, queryParams, requestHeaders)
		String responseBody = ""

		try {
			responseBody = response.body
		} catch (IllegalArgumentException e) {
			// Okay. Nothing to do. Thrown when response code are like 204, means there are no response body.
			Utils.reportError("Error while getting response for data service", e)
		}

		log.debug "[$currentTime] Recieved response for [$provider] with response code: [$response.code]"

		if (response.code == 401) {
			throw new InvalidAccessTokenException(provider)
		} else if ((response.code - 200) >= 100 || (response.code - 200) < 0) {
			// Logging response body only if the response code is non 200
			log.debug "Response body received: $responseBody"
		}

		JSONElement parsedResponse
		try {
			if (responseBody) {
				parsedResponse = JSON.parse(responseBody)
			}
		} catch (ConverterException e) {
			Utils.reportError("CURIOUS OAUTH DATA SERVICE ERROR", e)
		} finally {
			if (!parsedResponse) {
				parsedResponse = new JSONObject()
			}
		}

		if (Environment.current != Environment.PRODUCTION) {
			log.debug "DataService.getResponse() Response data: ${parsedResponse.toString(4)}"
		}

		// Helper dynamic methods.
		parsedResponse.getMetaClass().getCode = { return response.code }
		parsedResponse.getMetaClass().getRawBody = { return responseBody }
		parsedResponse
	}

	/**
	 * Used to get timezone instance id for current account. First check in account instance
	 * itself for timezone otherwise get from userProfileData.
	 * @param account
	 * @return Returns identity of TimeZone for given account.
	 */
	@Transactional
	Integer getTimeZoneId(OAuthAccount account) {
		log.debug "Get timeZoneId for Account: [$account]"
		checkNotNull(account)
		if (!account.timeZoneId) {	// Checking if timezoneId already exists.
			Integer timeZoneId
			String timeZoneName
			log.debug "TimeZoneId not found for account: [$account]. Try getting from user profile information."

			try {
				timeZoneName = getTimeZoneName(account)	// Getting timezone name from userInfo.
			} catch (InvalidAccessTokenException e) {
				log.warn "Found expired token while getting timezone for [$account]"
				Utils.reportError("Expired token while getting timezone for [$account]", e)
			}

			if (!timeZoneName) {	// Using user's last accessed timezone if timezone name not found from userInfo.
				log.debug "TimeZone Name not found for account: [$account] from user information. Using last accessed timezone."
				timeZoneId = User.getTimeZoneId(account.userId)
			} else {
				log.debug "Found timezone name [$timeZoneName] for account: [$account]."
				timeZoneId = TimeZoneId.look(timeZoneName).id
			}
			account.timeZoneId = timeZoneId
			Utils.save(account, true)
		} else {
			log.debug "Found timeZoneId [${account.timeZoneId}] in account itself for account [$account]"
		}

		account.timeZoneId
	}
	
	@Transactional
	TimeZone getTimeZone(OAuthAccount account) {
		Long timeZoneId = getTimeZoneId(account)
		return timeZoneId == null ? null : TimeZoneId.getTimeZoneInstance(timeZoneId as Integer)
	}

	/**
	 * Used to retrieve timezone name for given account.
	 * @param account
	 * @return Returns timezone name of null if not found.
	 * @throws MissingOAuthAccountException	If OAuthAccount instance is null.
	 * @throws InvalidAccessTokenException	If stored access token expired.
	 */
	@Transactional
	abstract String getTimeZoneName(OAuthAccount account) throws MissingOAuthAccountException, InvalidAccessTokenException

	/**
	 * Returns the parsed response of user profile data based on configured profileURL
	 * using an instance of OAuthAccount.
	 * @param account Instance of OAuthAccount 
	 * @return Returns parsed user profile data.
	 */
	@Transactional
	JSONObject getUserProfile(OAuthAccount account) {
		getResponse(account, profileURL)
	}

	/**
	 * Returns the parsed response of user profile data based on configured profileURL
	 * using an existing token instance.
	 * @param tokenInstance Instance of a token.
	 * @return Returns parsed user profile data.
	 */
	@Transactional
	JSONObject getUserProfile(Token tokenInstance) {
		getResponse(tokenInstance, profileURL)
	}

	/**
	 * Used to list all subscriptions of current user based on configured subscription URL.
	 * @return
	 */
	@Transactional
	JSONElement listSubscription(Long userId) {
		listSubscription(userId, listSubscriptionURL, "get", [:])
	}

	/**
	 * Overloaded method to list all subscriptions for current user based on 
	 * configurable URL. Used where subscription URL can't be same for all users,
	 * or subscription URL may contain data according to different users.
	 * @param url	
	 * @param method
	 * @param queryParams
	 * @return
	 */
	@Transactional
	JSONElement listSubscription(Long userId, String url, String method, Map queryParams) {
		OAuthAccount account = getOAuthAccountInstance(userId)
		getResponse(account, listSubscriptionURL)
	}

	/**
	 * Used to process notifications data received from third party API.
	 * @param notificationData String data received.
	 */
	@Transactional
	abstract List<ThirdPartyNotification> notificationHandler(String notificationData)

	/**
	 * This method iterates every unprocessed notification of respective type (third party)
	 * and call appropriate method based on collection type.
	 * 
	 * For example: If notificationHandler receives a notification of type *sleep*
	 * then its corresponding service must define a method `getDataSleep()` which must
	 * accept three parameters for accountInstance, notification date & boolean field.
	 */
	void notificationProcessor() {
		log.info "Processing ${provider}'s notifications."

		List<ThirdPartyNotification> pendingNotifications = ThirdPartyNotification.createCriteria().list() {
			eq("status", ThirdPartyNotification.Status.UNPROCESSED)
			eq("typeId", typeId)
			order("date", "asc")
			maxResults(1000)
		}

		log.debug "Found ${pendingNotifications.size()} pending notifications for $provider."

		pendingNotifications.each { notification ->
			log.debug "Processing $notification"
			int processedOAuthAccounts = 0

			OAuthAccount.withCriteria {
				eq("typeId", typeId)
				eq("accountId", notification.ownerId)
				// Only pull those records which have an access token
				ne("accessToken", "")
			}.each { account ->
				log.debug "Processing $notification for account id [$account.id]"
				processedOAuthAccounts++
				
				Date saveLastPolled = account.lastPolled
				Date saveLastData = account.lastData
				
				try {
					OAuthAccount.withTransaction {
						if (this.poll(account, notification.date)) {
							notification.status = ThirdPartyNotification.Status.PROCESSED
							Utils.save(notification, true)
						}
					}
				} catch (MissingMethodException e) {
					log.warn "No method implementation found for collection type: [$account.typeId.providerName] for $provider.", e
					account.setAccountFailure()
				} catch (InvalidAccessTokenException e) {
					log.warn "Token expired while processing notification of type: [$account.typeId.providerName] for $provider.", e
					// Clear the access token to not process this notification again until the user re-link the account
					account.setAccountFailure()
				} catch (Throwable t) {
					Utils.reportError("Unknown exception thrown during notification processing ", t)
				}
				return notification
			}

			/*
			 * If there were no OAuthAccounts for a particular notification (when user has unsubscribed) then mark it
			 * as ALREADY_UNSUBSCRIBED otherwise the job will always be picking those records and new notifications
			 * will never be processed.
			 */
			if (processedOAuthAccounts == 0) {
				log.debug "No linked account found for $notification"
				notification.status = ThirdPartyNotification.Status.ALREADY_UNSUBSCRIBED
				Utils.save(notification, true)
			}
		}

		log.info "Finished processing ${provider}'s notifications."
	}

	/**
	 * Used to poll all connected account of a given user Id.
	 * @param userId Curious user id.
	 * @return
	 */
	static boolean pollAllForUserId(Long userId) {
		log.debug "Polling all devices for userId: [$userId]"
		def accounts = OAuthAccount.findAllByUserId(userId)

		for (OAuthAccount account in accounts) {
			try {
				DataService dataService = account.getDataService()
	
				dataService.poll(account)
			} catch (Throwable t) {
				Utils.reportError("Error while polling " + account, t)
			}
		}
	}

	/**
	 * Used to poll all connected account of a given user Id.
	 * @param userId Curious user id.
	 * @return
	 */
	static boolean pollAllDataServices() {
		log.debug "Polling all oauth accounts"
		def accounts = OAuthAccount.withCriteria {
			not {'in'("typeId", [ThirdParty.WITHINGS])}
		}

		for (OAuthAccount account in accounts) {
			DataService dataService = account.getDataService()

			try {
				dataService.poll(account)
			} catch (Throwable t) {
				Utils.reportError("Error while polling " + account, t)
			}
		}
	}

	/**
	 * @param account
	 * @return
	 */
	@Synchronized
	@Transactional
	boolean poll(OAuthAccount account, Date notificationDate = null) {
		String accountId = account.accountId

		Long nowTime = new Date().getTime()

		Long lastPollDOS = lastPollTimestamps.get(accountId)

		Date lastDataDate, dateToPollFrom;

		// Ensuring that no data is missed/left unpolled if lastDataDate is going to be the start date for polling
		lastDataDate = dateToPollFrom = account.fetchLastDataDate() - 1
		Date pollEndDate = null;

		if ((lastPollDOS) && (nowTime - lastPollDOS < 60000) &&
				Environment.current != Environment.TEST) {
			// don't allow polling faster than once every minute
			log.warn "Polling faster than 1 minute for $provider with accountId: [$accountId]"
			return false
		}

		if (notificationDate && lastDataDate && notificationDate < lastDataDate) {
			dateToPollFrom = notificationDate
			pollEndDate = DateUtils.getEndOfTheDay(notificationDate)
		}

		try {
			getDataDefault(account, dateToPollFrom, pollEndDate, false,
					new DataRequestContext(dateToPollFrom, pollEndDate, COMMENT.contains("Human") ? Identifier.findAllByValueLike("Human %") :
					[Identifier.findByValue(SET_NAME)], account.userId))
		} catch (InvalidAccessTokenException e) {
			log.warn "Token expired while polling for $account"
			account.setAccountFailure()
		} catch (Throwable t) {
			Utils.reportError("Error while polling account " + account, t)
		}
		lastPollTimestamps[accountId] = account.lastPolled.getTime()
		return true
	}

	/**
	 * Used to poll all accounts data for respective API's.
	 * Must be called from API data services.
	 */
	void pollAll(Boolean refreshAll = false) {
		log.debug "DataService.pollAll Running poll All for provider: $provider"
		OAuthAccount.findAllByTypeId(typeId).each { OAuthAccount account ->
			poll(account)
		}
	}

	/**
	 * Used to subscribe to third party end points for notification based on configured URL.
	 * @param userId User identity to process against.
	 * @return Returns a map containing response code & parsed response data.
	 * throws AuthenticationRequiredException
	 */
	@Transactional
	Map subscribe(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		// If no URL means, API does not require any subscription
		if (!subscribeURL) {
			OAuthAccount account = getOAuthAccountInstance(userId)
			checkNotNull(account)

			return [success: true]
		}

		subscribe(userId, subscribeURL, "get", [:])
	}

	/**
	 * Overloaded method to subscribe to third party end points for notification based on given
	 * url, method. Useful where subscription url may vary based on different user or where request
	 * method may differ.
	 * 
	 * @Example Withings & FitBit
	 * @param userId User identity to process against.
	 * @param url Subscription URL.
	 * @param method Resource HTTP method to call.
	 * @param queryParams OPTIONAL query parameters to pass on.
	 * @return Returns a map containing response code & parsed response data.
	 * throws AuthenticationRequiredException
	 */
	@Transactional
	Map subscribe(Long userId, String url, String method, Map queryParams) throws MissingOAuthAccountException, InvalidAccessTokenException {
		debug "DataService.subscribe() userId:" + userId + ", url:" + url + ", method: " + method + ", queryParams: " + queryParams

		OAuthAccount account = getOAuthAccountInstance(userId)
		checkNotNull(account)
		def parsedResponse = getResponse(account.tokenInstance, url, method, queryParams)

		[code: parsedResponse.getCode(), body: parsedResponse, account: account]
	}

	/**
	 * Used to unsubscribe a user from third party end points for notification based on configured URL.
	 * @param userId User identity to process against.
	 * @return Returns a map containing response code & parsed response data.
	 * throws NotFoundException
	 * throws AuthenticationRequiredException
	 */
	@Transactional
	Map unsubscribe(Long userId) throws MissingOAuthAccountException, InvalidAccessTokenException {
		// If no URL means, API does not require any unsubscription
		if (!unsubscribeURL) {
			OAuthAccount account = getOAuthAccountInstance(userId)
			if (!account) {
				//Sanity check. This should never ever happen
				throw new MissingOAuthAccountException()
			}

			OAuthAccount.delete(account)

			return [success: true]
		}

		unsubscribe(userId, unsubscribeURL, "get", [:])
	}

	/**
	 * Overloaded method to unsubscribe a user from third party notification based on given
	 * url & method. Useful where unsubscribe url differs with user to user.
	 * @param userId User identity to process against.
	 * @param url Unsubscribe URL end point.
	 * @param method HTTP method to call with.
	 * @param queryParams OPTIONAL query string parameters.
	 * @return
	 * @throws MissingOAuthAccountException if instance of OAuthAccount not found.
	 * throws AuthenticationRequiredException if token expires during unsubsribe to api.
	 */
	@Transactional
	Map unsubscribe(Long userId, String url, String method, Map queryParams) throws MissingOAuthAccountException, InvalidAccessTokenException {
		debug "DataService.unsubscribe() userId:" + userId + ", url:" + url + ", method: " + method + ", queryParams: " + queryParams

		OAuthAccount account = getOAuthAccountInstance(userId)
		if (!account) {
			//Sanity check. This should never ever happen
			throw new MissingOAuthAccountException()
		}

		def parsedResponse = getResponse(account.tokenInstance, url, method, queryParams)

		// regardless of the response, delete account so user can re-link it if needed

		debug "OAuthAccount deleted regardless of response code: " + parsedResponse.getCode()
		OAuthAccount.delete(account)

		[code: parsedResponse.getCode(), body: parsedResponse]
	}

	/**
	 * Unset the userId from the older entries for the given setName to remove duplicity from the data import
	 * across the API.
	 * @param userId Identifier of the user for which older entries need to be unset
	 * @param setName Set name of the entries
	 */
	void unsetOldEntries(Long userId, String setName, Set<String> alreadyUnset) {
		if (alreadyUnset.contains(setName))
			return
		log.debug "Unset old entries for user $userId with set name $setName"
		Entry.executeUpdate("""UPDATE Entry e SET e.userId = null WHERE e.setIdentifier = :setIdentifier AND
				e.userId = :userId""", [setIdentifier: Identifier.look(setName), userId: userId])
		alreadyUnset.add(setName)
	}

	/**
	 * Unset the userId from the older entries for the given setNames to remove duplicity from the data import
	 * across the API.
	 * @param userId Identifier of the user for which older entries need to be unset
	 * @param setNames List of different set names of the entries
	 */
	void unsetOldEntries(Long userId, List<String> setNames, Set<String> alreadyUnset) {
		log.debug "Unset old entries for user $userId with set names $setNames"
		if (!setNames) {
			return
		}
		
		Set<String> unsetSetNames = new HashSet<String>()
		
		for (String setName in setNames) {
			if (!alreadyUnset.contains(setName)) {
				unsetSetNames.add(setName)
				alreadyUnset.add(setName)
			}
		}

		Entry.executeUpdate("update Entry e set e.userId = null where e.userId = :userId and e.setIdentifier in " +
				"(select i.id from Identifier i where value in (:values))",
				[values: unsetSetNames, userId: userId])
	}

	/**
	 * Unset the userId from all the older entries for the given setName.
	 * @param userId Identifier of the user for which older entries need to be unset
	 * @param setNamePrefix Prefix of set name of the entries
	 */
	void unsetAllOldEntries(Long userId, String setNamePrefix) {
		Entry.executeUpdate("update Entry e set e.userId = null where e.userId = :userId and e.setIdentifier in " +
				"(select i.id from Identifier i where value like :value)",
				[value: "${setNamePrefix}%", userId: userId])
	}

	/**
	 * This method checks whether there is continuous sync happening with the device by checking the lastData field
	 * in the OAuth accounts to be greater than last 24 hours.
	 */
	void checkSyncHealth() {
		return
	}
}
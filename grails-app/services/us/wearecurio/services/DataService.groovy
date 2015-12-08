package us.wearecurio.services

import grails.converters.JSON
import grails.util.Environment

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

	private static def log = LogFactory.getLog(this)

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

		if ((instance instanceof Token) && !instance.token) {
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
	 * @param startDate  Start date for polling or null to get all new records
	 * @param refreshAll Boolean field used to clear all existing records.
	 * @return	Returns a map with required data.
	 */
	@Transactional
	abstract Map getDataDefault(OAuthAccount account, Date startDate, boolean refreshAll) throws InvalidAccessTokenException

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

		log.debug "[$currentTime] Fetching data for [$provider] with request URL: [$requestURL] & method: $method"

		checkNotNull(tokenInstance)

		String methodSuffix = queryParams ? "ResourceWithQuerystringParams" : "Resource"

		Response response = oauthService."${method}${provider}${methodSuffix}"(tokenInstance, requestURL, queryParams, requestHeaders)
		String responseBody = ""

		try {
			responseBody = response.body
		} catch (IllegalArgumentException e) {
			// Okay. Nothing to do. Thrown when response code are like 204, means there are no response body.
		}

		log.debug "[$currentTime] Fetched data for [$provider] with response code: [$response.code] & body: [${response.body}]"

		if (response.code == 401) {
			throw new InvalidAccessTokenException(provider)
		}

		JSONElement parsedResponse
		try {
			if (responseBody) {
				parsedResponse = JSON.parse(responseBody)
			}
		} catch (ConverterException e) {
			log.error "Error parsing response data.", e
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
	abstract void notificationHandler(String notificationData)

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
			order ("date", "asc")
			maxResults(100)
		}

		log.debug "Found ${pendingNotifications.size()} pending notifications for $provider."

		pendingNotifications.each { notification ->
			OAuthAccount.findAllByTypeIdAndAccountId(typeId, notification.ownerId).each { account ->
				try {
					DatabaseService.retry(notification) {
						this."getData${notification.collectionType.capitalize()}"(account, notification.date, false)
						notification.status = ThirdPartyNotification.Status.PROCESSED
						Utils.save(notification, true)
					}
				} catch (MissingMethodException e) {
					log.warn "No method implementation found for collection type: [$account.typeId.providerName] for $provider.", e
				} catch (InvalidAccessTokenException e) {
					log.warn "Token expired while processing notification of type: [$account.typeId.providerName] for $provider."
				} catch (Throwable t) {
					log.error "Unknown exception thrown during notification processing " + t
					t.printStackTrace()
				}
				return notification
			}
		}

		log.info "Finished processing ${provider}'s notifications."
	}

	/**
	 * Used to poll all connected account of a given user Id.
	 * @param userId Curious user id.
	 * @return
	 */
	@Transactional
	static boolean pollAllForUserId(Long userId) {
		log.debug "Polling all devices for userId: [$userId]"
		def accounts = OAuthAccount.findAllByUserId(userId)

		for (OAuthAccount account in accounts) {
			DataService dataService = account.getDataService()

			dataService.poll(account)
		}
	}

	/**
	 * @param account
	 * @return
	 */
	boolean poll(OAuthAccount account) {
		String accountId = account.accountId

		Long nowTime = new Date().getTime()

		Long lastPoll = lastPollTimestamps.get(accountId)

		if (lastPoll && nowTime - lastPoll < 500) { // don't allow polling faster than once every 500ms
			log.warn "Polling faster than 500ms for $provider with accountId: [$accountId]"
			return false
		}

		OAuthAccount.withTransaction {
			try {
				getDataDefault(account, null, false)
			} catch (InvalidAccessTokenException e) {
				log.warn "Token expired while polling for & account: [$account]"
			}
		}
	}

	/**
	 * Used to poll all accounts data for respective API's.
	 * Must be called from API data services.
	 */
	void pollAll(def refreshAll = false) {
		OAuthAccount.findAllByTypeId(typeId).each { account ->
			DatabaseService.retry(account) {
				try {
					getDataDefault(account, null, refreshAll)
				} catch (InvalidAccessTokenException e) {
					log.warn "Token expired while polling account: [$account] for $typeId."
				}
				return account
			}
		}
	}

	/**
	 * Used to subscribe to third party end points for notification based on configured URL.
	 * @param userId User identity to process against.
	 * @return Returns a map containing response code & parsed response data.
	 * @throws AuthenticationRequiredException
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
	 * @throws AuthenticationRequiredException
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
	 * @throws NotFoundException
	 * @throws AuthenticationRequiredException
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
	 * @throws AuthenticationRequiredException if token expires during unsubsribe to api.
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

	boolean isRequestSucceded(JSONObject response) {
		return response && response["meta"] && response["meta"]["code"] == 200
	}
}

package us.wearecurio.services

import grails.converters.JSON
import grails.util.Environment
import javassist.NotFoundException

import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.thirdparty.AuthenticationRequiredException

abstract class DataService {

	private static def log = LogFactory.getLog(this)

	static debug(str) {
		log.debug(str)
	}

	def grailsApplication
	def oauthService
	def urlService

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

	/**
	 * Used to check if certain instances are not null. And if null
	 * throw AuthenticationRequiredException for authentication.
	 * @param instance Can be one of OAuthAccount or Token
	 * 
	 * @throws AuthenticationRequiredException
	 */
	void checkNotNull(def instance) throws AuthenticationRequiredException {
		if (!instance) {
			debug "authentication required"
			throw new AuthenticationRequiredException(provider)
		}

		if ((instance instanceof Token) && !instance.token) {
			debug "authentication required"
			throw new AuthenticationRequiredException(provider)
		}
	}

	/**
	 * Get a list of all OAuthAccount instances based on accountId, since there may
	 * be multiple instance of OAuthAccount for a same accountId. For example: Withings.
	 * @param accountId Account Id of respective third party.
	 * @return Returns list of OAuthAccount.
	 */
	List<OAuthAccount> getAllOAuthAccounts(String accountId) {
		OAuthAccount.findAllByTypeIdAndAccountId(typeId, accountId)
	}

	/**
	 * Used to process actual data which may depends on service to service for different
	 * third party. Needs to be implemented on each services.
	 * @param account Instance of OAuthAccount
	 * @param notificationDate	Date for which data needs to be polled.
	 * @param refreshAll Boolean field used to clear all existing records.
	 * @return	Returns a map with required data.
	 */
	abstract Map getDataDefault(OAuthAccount account, Date notificationDate, boolean refreshAll)

	/**
	 * Returns the OAuthAccount instance for given userId.
	 * @param userId User id for which instance needs to be fetched.
	 */
	OAuthAccount getOAuthAccountInstance(Long userId) {
		OAuthAccount.findByTypeIdAndUserId(typeId, userId)
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
	 * @throws AuthenticationRequiredException if any API call returns 401 response code.
	 */
	JSONElement getResponse(Token tokenInstance, String requestURL, String method = "get", Map queryParams = [:], Map requestHeaders = [:])
	throws AuthenticationRequiredException {
		log.debug "Fetching data for [$provider] with request URL: [$requestURL]"

		checkNotNull(tokenInstance)

		String methodSuffix = queryParams ? "ResourceWithQuerystringParams" : "Resource"

		Response response = oauthService."${method}${provider}${methodSuffix}"(tokenInstance, requestURL, queryParams, requestHeaders)
		String responseBody = ""

		try {
			responseBody = response.body
		} catch (IllegalArgumentException e) {
			// Okay. Nothing to do. Thrown when response code are like 204, means there are no response body.
		}

		if (Environment.current == Environment.DEVELOPMENT) {
			log.debug "Fetched data for [$provider] with response code: [$response.code] & body: [$responseBody]"
		} else {
			log.debug "Fetched data for [$provider] with response code: [$response.code]."
		}

		if (response.code == 401) {
			log.warn "Token expired for provider [$provider]"
			throw new AuthenticationRequiredException(provider)
		} else {
			JSONElement parsedResponse
			try {
				parsedResponse = JSON.parse(responseBody)
			} catch (ConverterException e) {
				log.error "Error parsing response data.", e
				parsedResponse = new JSONObject()
			}
			// Helper dynamic methods.
			parsedResponse.getMetaClass().getCode = { return response.code }
			parsedResponse.getMetaClass().getRawBody = { return responseBody }
			parsedResponse
		}
	}

	/**
	 * Returns the parsed response of user profile data based on configured profileURL
	 * using an instance of OAuthAccount.
	 * @param account Instance of OAuthAccount 
	 * @return Returns parsed user profile data.
	 */
	JSONObject getUserProfile(OAuthAccount account) {
		getResponse(account.tokenInstance, profileURL)
	}

	/**
	 * Returns the parsed response of user profile data based on configured profileURL
	 * using an existing token instance.
	 * @param tokenInstance Instance of a token.
	 * @return Returns parsed user profile data.
	 */
	JSONObject getUserProfile(Token tokenInstance) {
		getResponse(tokenInstance, profileURL)
	}

	/**
	 * Used to list all subscriptions of current user based on configured subscription URL.
	 * @return
	 */
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
	JSONElement listSubscription(Long userId, String url, String method, Map queryParams) {
		OAuthAccount account = getOAuthAccountInstance(userId)
		getResponse(account.tokenInstance, listSubscriptionURL)
	}

	/**
	 * Used to process notifications data received from third party API.
	 * @param notificationData String data received.
	 */
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

		pendingNotifications.each {
			OAuthAccount.findAllByTypeIdAndAccountId(typeId, it.ownerId).each { account ->
				try {
					this."getData${it.collectionType.capitalize()}"(account, it.date, false)
				} catch (MissingMethodException e) {
					log.warn "No method implementation found for collection type: [$it.collectionType] for $provider."
				}
			}
			it.status = ThirdPartyNotification.Status.PROCESSED
			it.save(flush: true)
		}

		log.info "Finished processing ${provider}'s notifications."
	}

	/**
	 * @param account
	 * @return
	 */
	boolean poll(OAuthAccount account) {
		String accountId = account.accountId

		Long now = new Date().getTime()

		Long lastPoll = lastPollTimestamps.get(accountId)

		if (lastPoll && now - lastPoll < 500) { // don't allow polling faster than once every 500ms
			log.warn "Polling faster than 500ms for $provider with accountId: [$accountId]"
			return false
		}

		getDataDefault(account, false)
	}

	/**
	 * Used to poll all accounts data for respective API's.
	 */
	void pollAll() {
		OAuthAccount.findAllByTypeId(typeId).each {
			getDataDefault(it, it.lastPolled, false)
		}
	}

	/**
	 * Used to subscribe to third party end points for notification based on configured URL.
	 * @param userId User identity to process against.
	 * @return Returns a map containing response code & parsed response data.
	 * @throws AuthenticationRequiredException
	 */
	Map subscribe(Long userId) throws AuthenticationRequiredException {
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
	Map subscribe(Long userId, String url, String method, Map queryParams) throws AuthenticationRequiredException {
		debug "DataService.subscribe() userId:" + userId + ", url:" + url + ", method: " + method + ", queryParams: " + queryParams
		OAuthAccount account = getOAuthAccountInstance(userId)
		checkNotNull(account)

		def parsedResponse = getResponse(account.tokenInstance, url, method, queryParams)

		[code: parsedResponse.getCode(), body: parsedResponse]
	}

	/**
	 * Used to unsubscribe a user from third party end points for notification based on configured URL.
	 * @param userId User identity to process against.
	 * @return Returns a map containing response code & parsed response data.
	 * @throws NotFoundException
	 * @throws AuthenticationRequiredException
	 */
	Map unsubscribe(Long userId) throws NotFoundException, AuthenticationRequiredException {
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
	 * @throws NotFoundException if instance of OAuthAccount not found.
	 * @throws AuthenticationRequiredException if token expires during unsubsribe to api.
	 */
	Map unsubscribe(Long userId, String url, String method, Map queryParams) throws NotFoundException, AuthenticationRequiredException {
		debug "DataService.unsubscribe() userId:" + userId + ", url:" + url + ", method: " + method + ", queryParams: " + queryParams
		OAuthAccount account = getOAuthAccountInstance(userId)
		if (!account) {
			throw new NotFoundException("No oauth account found.")
		}

		def parsedResponse = getResponse(account.tokenInstance, url, method, queryParams)

		// regardless of the response, delete account so user can re-link it if needed

		debug "OAuthAccount deleted regardless of response code: " + parsedResponse.getCode()
		OAuthAccount.delete(account)

		[code: parsedResponse.getCode(), body: parsedResponse]
	}

}

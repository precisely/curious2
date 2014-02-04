package us.wearecurio.services

import grails.converters.JSON
import javassist.NotFoundException

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdPartyNotification
import us.wearecurio.thirdparty.AuthenticationRequiredException

abstract class DataService {

	def oauthService
	def securityService

	Map lastPollTimestamps = new HashMap<Long,Long>() // prevent DOS attacks

	String listSubscriptionURL
	String profileURL
	String provider
	String subscribeURL

	int typeId

	String unsubscribeURL

	List<OAuthAccount> getAllOAuthAccounts(String accountId) {
		OAuthAccount.findAllByTypeIdAndAccountId(typeId, accountId)
	}

	Long getCurrentUserId() {
		securityService.currentUser.id
	}

	abstract Map getDataDefault(OAuthAccount account, Date notificationDate, boolean refreshAll)

	OAuthAccount getOAuthAccountInstance() {
		getOAuthAccountInstance(securityService.currentUser.id)
	}

	OAuthAccount getOAuthAccountInstance(Long userId) {
		OAuthAccount.findByTypeIdAndUserId(typeId, userId)
	}

	JSONObject getResponse(Token tokenInstance, String requestURL, String method = "get", Map queryParams = [:], Map requestHeaders = [:])
	throws AuthenticationRequiredException {
		log.debug "Fetching data for [$provider] with request URL: [$requestURL]"

		Response response = oauthService."${method}${provider}Resource"(tokenInstance, requestURL, queryParams, requestHeaders)

		if (grails.util.Environment.current == grails.util.Environment.DEVELOPMENT) {
			log.debug "Fetched data for [$provider] with response code: [$response.code] & body: [$response.body]"
		} else {
			log.debug "Fetched data for [$provider] with response code: [$response.code]."
		}

		if (response.code == 401) {
			log.warn "Token expired for provider [$provider]"
			throw new AuthenticationRequiredException(provider)
		} else {
			def parsedResponse = JSON.parse(response.getBody())
			parsedResponse.getMetaClass().getCode = { return response.code }
			parsedResponse
		}
	}

	JSONObject getUserProfile(OAuthAccount account) {
		getResponse(account.tokenInstance, profileURL)
	}

	JSONObject getUserProfile(Token tokenInstance) {
		getResponse(tokenInstance, profileURL)
	}

	JSONObject listSubscription() {
		listSubscription(listSubscriptionURL, "get", [:])
	}

	JSONObject listSubscription(String url, String method, Map queryParams) {
		OAuthAccount account = getOAuthAccountInstance()
		getResponse(account.tokenInstance, listSubscriptionURL)
	}

	abstract void notificationHandler(String notificationData)

	void notificationProcessor() {
		List<ThirdPartyNotification> pendingNotifications = ThirdPartyNotification.createCriteria().list() {
			eq("status", ThirdPartyNotification.Status.UNPROCESSED)
			eq("typeId", typeId)
			order ("date", "asc")
			maxResults(100)
		}

		pendingNotifications.each {
			OAuthAccount.findAllByTypeIdAndAccountId(typeId, it.ownerId).each { account ->
				this."getData${it.collectionType.capitalize()}"(account, it.date, false)
			}
			it.status = ThirdPartyNotification.Status.PROCESSED
			it.save(flush: true)
		}
	}

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

	void pollAll() {
		OAuthAccount.findAllByTypeId(typeId).each {
			getDataDefault(it, false)
		}
	}

	void pollAll(String accountId) {
		getAllOAuthAccounts(accountId).each {
			poll(it)
		}
	}

	Map subscribe() throws AuthenticationRequiredException {
		subscribe(subscribeURL, "get", [:])
	}

	Map subscribe(String url, String method, Map queryParams) throws AuthenticationRequiredException {
		OAuthAccount account = getOAuthAccountInstance()

		if (!account) {
			throw new AuthenticationRequiredException(provider)
		}

		def parsedResponse = getResponse(account.tokenInstance, url, method, queryParams)

		[code: parsedResponse.getCode(), body: parsedResponse]
	}

	Map unsubscribe() throws NotFoundException, AuthenticationRequiredException {
		unsubscribe(unsubscribeURL, "get", [:])
	}

	Map unsubscribe(String url, String method, Map queryParams) throws NotFoundException, AuthenticationRequiredException {
		OAuthAccount account = getOAuthAccountInstance()
		if (!account) {
			throw new NotFoundException("No oauth account found.")
		}

		def parsedResponse = getResponse(account.tokenInstance, url, method, queryParams)

		[code: parsedResponse.getCode(), body: parsedResponse]
	}

}
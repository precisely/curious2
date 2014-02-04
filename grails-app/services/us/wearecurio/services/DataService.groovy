package us.wearecurio.services

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Response
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
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

	abstract Map getData(OAuthAccount account, boolean refreshAll)

	OAuthAccount searchOAuthAccount() {
		searchOAuthAccount(securityService.currentUser.id)
	}

	OAuthAccount searchOAuthAccount(Long userId) {
		OAuthAccount.findByTypeIdAndUserId(typeId, userId)
	}

	List<OAuthAccount> searchOAuthAccounts(String accountId) {
		OAuthAccount.findAllByTypeIdAndAccountId(typeId, accountId)
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
			throw new AuthenticationRequiredException("provider")
		} else if (![200, 204].contains(response.code)) {
			log.warn "Got error response for provider: [$provider] with body: [$response.body]"
			new JSONObject()
		} else {
			JSON.parse(response.getBody())
		}
	}

	JSONObject getUserProfile(OAuthAccount account) {
		getResponse(account.tokenInstance, profileURL)
	}

	void listSubscription() {
		OAuthAccount account = searchOAuthAccount()
		getResponse(account.tokenInstance, listSubscriptionURL)
	}

	void poll() {
		OAuthAccount.findAllByTypeId(typeId).each {
			getData(it, false)
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

		getData(account, true)
	}

	void poll(String accountId) {
		searchOAuthAccounts(accountId).each {
			poll(it)
		}
	}

	Map subscribe() throws AuthenticationRequiredException {
		subscribe(securityService.currentUser.id)
	}

	Map subscribe(Long userId) throws AuthenticationRequiredException {

	}

	Map unsubscribe() {
		unsubscribe(securityService.currentUser.id)
	}

	Map unsubscribe(Long userId) {

	}
}
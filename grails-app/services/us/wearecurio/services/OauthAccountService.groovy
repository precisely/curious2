package us.wearecurio.services

import grails.converters.JSON

import org.springframework.transaction.annotation.Transactional

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Token
import org.scribe.oauth.OAuthService

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.thirdparty.RefreshTokenVerifier
import us.wearecurio.utility.Utils


class OauthAccountService {
	static transactional = false

	def oauthService
	SecurityService securityService
	WithingsDataService withingsDataService

	/**
	 * Accepts instance of OAuthAccount & Token to update account's token & expiry related informations.
	 * @param account
	 * @param tokenInstance
	 * @return Instance of OAuthAccount.
	 */
	@Transactional
	OAuthAccount createOrUpdate(OAuthAccount account, Token tokenInstance) {
		String rawResponse

		try {
			rawResponse = tokenInstance.rawResponse
		} catch (IllegalStateException e) {
			e.printStackTrace()
			account.setAccountFailure()
		}
		
		def refreshToken
		def expiresOn

		if (rawResponse) {
			try {
				JSONObject parsedRawResponse = JSON.parse(rawResponse)
				refreshToken = parsedRawResponse["refresh_token"]
				if (parsedRawResponse["expires_in"]) {
					Date now = new Date()
					expiresOn = new Date(now.time + parsedRawResponse["expires_in"] * 1000)
				}
			} catch (ConverterException e) {
				log.error "Error parsing raw response: [$rawResponse].", e
				return account
			}
		}

		return DatabaseService.retry(account) {
			account.accessToken = tokenInstance.token
			account.accessSecret = tokenInstance.secret ?: ""
			account.refreshToken = refreshToken
			account.expiresOn = expiresOn
			
			Utils.save(account, true)
			
			return account
		}
	}

	/**
	 * Used to create or update OAuthAccount instance for given provider/type for current loggedIn user.
	 * @param type Type of the provider. See ThirdParty enum.
	 * @param accountId Account Identity for current provider.
	 * @param tokenInstance Instance of scribe token.
	 * @param userId Identity of precise.ly user.
	 * @param timeZoneId Identity of TimeZoneId domain. Default Null.
	 * @return OAuthAccount instance for given parameters.
	 */
	OAuthAccount createOrUpdate(ThirdParty type, String accountId, Token tokenInstance, Long userId, Integer timeZoneId = null) {
		OAuthAccount account = OAuthAccount.findOrCreateByUserIdAndTypeId(userId, type)
		DatabaseService.retry(account) {
			account.accountId = accountId
			account.timeZoneId = timeZoneId
			createOrUpdate(account, tokenInstance)
		}
		
		return account
	}

	/**
	 * Refresh all tokens which will expire before five days from now
	 */
	void refreshAll() {
		OAuthAccount.withCriteria {
			lt("expiresOn", new Date() + 5)
		}.each { account ->
			try {
				refreshToken(account)
			} catch (Throwable t) {
				log.error("Error while refreshing token for account " + account)
				t.printStackTrace()
				account.setAccountFailure()
			}
		}
		withingsDataService.refreshSubscriptions()
	}

	/**
	 * Used to renew access token of a particular OAuthAccount instance.
	 * @param account Instance of OAuthAccount
	 */
	@Transactional
	void refreshToken(OAuthAccount account) {
		log.debug "Attempting to refresh OAuthAccount: " + account
		if (!account.typeId.supportsOAuth2()) {
			log.warn "Can't renew access token for account: [$account] since associated thirdparty doesn't supports OAuth2."
			return
		}
		if (!account.refreshToken) {
			log.warn "Can't renew access token for account: [$account] since no associated refresh token was found."
			return
		}

		RefreshTokenVerifier tokenVerifier = new RefreshTokenVerifier(account.refreshToken)

		OAuthService service = oauthService.findService(account.typeId.providerName)

		Token newTokenInstance
		
		try {
			newTokenInstance = service.getAccessToken(null, tokenVerifier)
		} catch (Throwable t) {
			log.error("Error while refreshing token for account " + account)
			t.printStackTrace()
			account.refreshToken = ""
			account.setAccountFailure()
			return
		}

		if (newTokenInstance.token) {
			createOrUpdate(account, newTokenInstance)
			log.debug "New expiredOn for acccont " + account + " is: " + account.expiresOn
		} else {
			log.error "Error refreshing access token for account: [$account]. Response body: $newTokenInstance.rawResponse"
		}
	}
}
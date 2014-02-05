package us.wearecurio.services

import grails.converters.JSON

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Token
import org.scribe.oauth.OAuthService

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.User
import us.wearecurio.thirdparty.RefreshTokenVerifier
import us.wearecurio.utility.Utils

class OAuthAccountService {

	def oauthService
	def securityService

	/**
	 * Accepts instance of OAuthAccount & Token to update account's token & expiry related informations.
	 * @param account
	 * @param tokenInstance
	 * @return Instance of OAuthAccount.
	 */
	OAuthAccount createOrUpdate(OAuthAccount account, Token tokenInstance) {
		account.accessToken = tokenInstance.token
		account.accessSecret = tokenInstance.secret ?: ""

		String rawResponse

		try {
			rawResponse = tokenInstance.rawResponse
		} catch (IllegalStateException e) {
			// Okay. Nothing to do. Thrown when there is no rawResponse.
		}

		if (rawResponse) {
			try {
				JSONObject parsedRawResponse = JSON.parse(rawResponse)
				account.refreshToken = parsedRawResponse["refresh_token"]
				if (parsedRawResponse["expires_in"]) {
					Date now = new Date()
					account.expiresOn = new Date(now.time + parsedRawResponse["expires_in"] * 1000)
				}
			} catch (ConverterException e) {
				log.error "Error parsing raw response: [$rawResponse].", e
			}
		}

		if (Utils.save(account)) {
			return account
		}

		return null
	}

	/**
	 * Used to create or update OAuthAccount instance for given provider/type for current loggedIn user.
	 * @param type Type of the provider. See ThirdParty enum.
	 * @param accountId Account Identity for current provider.
	 * @param tokenInstance Instance of scribe token.
	 * @return OAuthAccount instance for given parameters.
	 */
	OAuthAccount createOrUpdate(ThirdParty type, String accountId, Token tokenInstance) {
		User currentUser = securityService.currentUser

		OAuthAccount account = OAuthAccount.findOrCreateByUserIdAndTypeId(currentUser.id, type)
		account.accountId = accountId
		createOrUpdate(account, tokenInstance)
	}

	boolean isLinked(ThirdParty type) {
		User currentUser = securityService.currentUser

		if (OAuthAccount.findByTypeIdAndUserId(type, currentUser.id)) {
			return true
		}
		return false
	}

	void refreshAllToken() {
		OAuthAccount.findAll().each {
			refreshTokn(it)
		}
	}

	/**
	 * Used to renew access token of a particular OAuthAccount instance.
	 * @param account Instance of OAuthAccount
	 */
	void refreshTokn(OAuthAccount account) {
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

		Token newTokenInstance = service.getAccessToken(null, tokenVerifier)

		if (newTokenInstance.token) {
			createOrUpdate(account, newTokenInstance)
		} else {
			log.error "Error refreshing access token for account: [$account]. Response body: $newTokenInstance.rawResponse"
		}
	}

}
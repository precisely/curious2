package us.wearecurio.services

import grails.converters.JSON

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.scribe.model.Token

import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty;
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

class OAuthAccountService {

	def securityService

	/**
	 * Used to create OAuthAccount instance for given provider/type for current loggedIn user.
	 * @param typeId Type of the provider. See constants in OAuthAccount.groovy.
	 * @param accountId Account Identity for current provider.
	 * @param accessToken Access Token after authorization.
	 * @param accessSeret Access Secret after authorization.
	 * @return OAuthAccount instance for given parameters.
	 */
	OAuthAccount createOrUpdate(ThirdParty type, String accountId, String accessToken, String accessSecret, String refreshToken, Date expiresOn) {
		User currentUser = securityService.currentUser

		OAuthAccount account = OAuthAccount.findOrCreateByUserIdAndTypeId(currentUser.id, type)
		account.accountId = accountId
		account.accessToken = accessToken
		account.accessSecret = accessSecret ?: ""
		account.refreshToken = refreshToken
		account.expiresOn = expiresOn

		if (Utils.save(account)) {
			return account
		}

		return null
	}

	OAuthAccount createOrUpdate(ThirdParty type, String accountId, Token tokenInstance) {
		Date expiresOn

		String refreshToken = ""

		if (tokenInstance.rawResponse) {
			try {
				JSONObject parsedRawResponse = JSON.parse(tokenInstance.rawResponse)
				refreshToken = parsedRawResponse["refresh_token"]
				if (parsedRawResponse["expires_in"]) {
					Date now = new Date()
					expiresOn = new Date(now.time + parsedRawResponse["expires_in"] * 1000)
				}
			} catch (ConverterException e) {
				log.error "Error parsing raw response: [$tokenInstance.rawResponse].", e
			}
		}
		createOrUpdate(type, accountId, tokenInstance.token, tokenInstance.secret, refreshToken, expiresOn)
	}

	boolean isLinked(ThirdParty type) {
		User currentUser = securityService.currentUser

		if (OAuthAccount.findByTypeIdAndUserId(type, currentUser.id)) {
			return true
		}
		return false
	}

}
package us.wearecurio.thirdparty

import us.wearecurio.model.OAuthAccount

/**
 * Thrown when an access token is missing
 * @author vishesh
 *
 */
class InvalidAccessTokenException extends Exception {

	String provider

	InvalidAccessTokenException() {
		super("Missing a valid access token");
	}

	@Deprecated
	InvalidAccessTokenException(String provider) {
		super("Missing a valid access token for [$provider].");
		log.warn "Token expired for provider [$provider]."
		this.provider = provider
	}

	InvalidAccessTokenException(String provider, OAuthAccount account) {
		super("Missing a valid access token for [$provider].")
		log.debug "Token expired for [$account]. Old token was: [$account.accessToken]"
		this.provider = provider
		account.accessToken = ""
		account.save(flush: true)
	}

}

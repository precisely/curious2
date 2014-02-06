package us.wearecurio.thirdparty

import org.scribe.model.Verifier

/**
 * Class to represent a refresh token verifier for oauth request.
 */
class RefreshTokenVerifier extends Verifier {

	static final String REFRESH_TOKEN = "refresh_token"

	RefreshTokenVerifier(String value) {
		super(value)
	}
}

package us.wearecurio.thirdparty

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

	InvalidAccessTokenException(String provider) {
		super("Missing a valid access token for [$provider].");
		this.provider = provider
	}

}

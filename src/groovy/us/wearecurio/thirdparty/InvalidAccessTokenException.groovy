package us.wearecurio.thirdparty

/**
 * Thrown when an access token is missing
 * @author vishesh
 *
 */
class InvalidAccessTokenException extends Exception {
	InvalidAccessTokenException() {
		super("Missing a valid access token");
	}
}

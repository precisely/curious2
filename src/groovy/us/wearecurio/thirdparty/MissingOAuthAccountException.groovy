package us.wearecurio.thirdparty

class MissingOAuthAccountException extends Exception {

	MissingOAuthAccountException() {
		super()
	}

	MissingOAuthAccountException(String provider) {
		super("Missing OAuthAccount Instance for $provider")
	}
}

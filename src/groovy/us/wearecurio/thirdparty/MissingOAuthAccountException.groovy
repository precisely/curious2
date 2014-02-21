package us.wearecurio.thirdparty

class MissingOAuthAccountException extends Exception {

	MissingOAuthAccountException() {
		super("Missing OAuthAccount instance");
	}
}

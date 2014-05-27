package us.wearecurio.thirdparty

class TooManyRequestsException extends Exception {

	TooManyRequestsException() {
		super()
	}

	TooManyRequestsException(String provider) {
		super("Query rate limit reached for:  $provider")
	}
}

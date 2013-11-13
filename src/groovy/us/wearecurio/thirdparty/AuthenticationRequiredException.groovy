package us.wearecurio.thirdparty

class AuthenticationRequiredException extends Exception {

	AuthenticationRequiredException() {
		super()
	}

	AuthenticationRequiredException(String provider) {
		super(provider)
	}

}
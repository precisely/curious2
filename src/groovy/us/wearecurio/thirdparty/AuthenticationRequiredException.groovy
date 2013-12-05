package us.wearecurio.thirdparty

/**
 * To get access token of any third party API using grails-oauth plugin,
 * they all need to redirect at `/oauth/$provider/authenticate` URL.
 * Throwing this exception will automatically redirects user to above
 * URL based on the given provider by the use of grails feature of 
 * mapping between exceptions & controller action.
 * 
 * @see Declarative Error handling in http://grails.org/doc/latest/guide/theWebLayer.html#mappingToResponseCodes
 */
class AuthenticationRequiredException extends Exception {

	String provider

	AuthenticationRequiredException() {
		super()
	}

	AuthenticationRequiredException(String provider) {
		super("Authentication required for [$provider] since this is a protected resource. Ignore this, it has already been handled by grails url mapping.")
		this.provider = provider
	}

}
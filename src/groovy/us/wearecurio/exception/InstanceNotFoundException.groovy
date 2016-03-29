package us.wearecurio.exception

/**
 * An exception class used to represent that no record found of a domain instance with any given identifier or criteria.
 * @author Shashank Agrawal
 */
class InstanceNotFoundException extends Exception {

	InstanceNotFoundException(String message = "") {
		super(message)
	}
}
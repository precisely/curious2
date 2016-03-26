package us.wearecurio.exception

/**
 * Exception class used to represent that a certain operation is not allowed.
 *
 * @author Shashank Agrawal
 */
class OperationNotAllowedException extends Exception {

	OperationNotAllowedException(String message = "") {
		super(message)
	}
}

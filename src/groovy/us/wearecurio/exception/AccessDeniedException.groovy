package us.wearecurio.exception

/**
 * Exception class used when a user does not have any privileges to perform any operation. For example, when user does
 * not have write privileges in a group to create a discussion.
 *
 * @author Shashank Agrawal
 */
class AccessDeniedException extends Exception {

	AccessDeniedException(String message = "") {
		super(message)
	}
}

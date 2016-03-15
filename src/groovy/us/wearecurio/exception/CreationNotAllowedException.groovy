package us.wearecurio.exception

/**
 * Exception class represents that creation of certain entity is not allowed for any reason.
 */
class CreationNotAllowedException extends Exception {

    CreationNotAllowedException() {
    }

    CreationNotAllowedException(String message) {
        super(message)
    }
}

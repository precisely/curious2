package us.wearecurio.controller

import us.wearecurio.utility.Utils
/**
 * A controller to deal with the exceptions and errors.
 */
class ErrorController {

	/**
	 * An action which is called whenever an error of type OutOfMemoryError is thrown.
	 * See URLMappings.groovy
	 */
	def memoryError() {
		log.debug 'Sending error report to curious support.'

		Throwable throwable = request.exception
		String title = 'MEMORY ALLOCATION ERROR - OutOfMemoryError:'

		Utils.reportError(title, throwable)

		render(view: 'error')
	}
}

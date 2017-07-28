package us.wearecurio.controller

import us.wearecurio.model.UpdateSubscription
import grails.converters.JSON

class UpdateSubscriptionController extends LoginController {
	/**
	 * An endpoint to delete user subscription .
	 *
	 * @return success is true or false.
	 */
	def save() {
		if(params.email) {
			UpdateSubscription subscriptionDetails = new UpdateSubscription([categories : params.categories,
					description: params.description, email: params.email]).save()
			renderJSONGet([success: true])
			return
		} else {
			renderJSONGet([success: false])
			return
		}
	}
}
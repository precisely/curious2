package us.wearecurio.controller

import us.wearecurio.model.UpdateSubscription

class UpdateSubscriptionController {

	def save() {
		if(params.email) {
			UpdateSubscription subscriptionDetails = new UpdateSubscription([categories : params.categories,
					description: params.description, email: params.email]).save()
		} else {
			redirect uri: "home/login"
		}
	}
}
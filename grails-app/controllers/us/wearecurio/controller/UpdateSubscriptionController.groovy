package us.wearecurio.controller

import us.wearecurio.model.UpdateSubscription

class UpdateSubscriptionController extends LoginController {

	def save() {
		UpdateSubscription subscriptionDetails = new UpdateSubscription([categories: params.categories,
				 description: params.description, email: params.email]).save()
	}

}
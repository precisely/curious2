package us.wearecurio.model

class UpdateSubscription {
	String categories
	String description
	String email

	static constraints = {
		categories(nullable:true)
		description(nullable:true)
	}
}

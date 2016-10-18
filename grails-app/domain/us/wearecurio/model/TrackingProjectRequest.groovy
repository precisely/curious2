package us.wearecurio.model

class TrackingProjectRequest {
	String email
	String topic

	static constraints = {
		email blank: false, nullable: false, email: true
		topic blank: false, nullable: false
	}
}

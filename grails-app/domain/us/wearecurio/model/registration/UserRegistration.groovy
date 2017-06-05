package us.wearecurio.model.registration

import us.wearecurio.utility.Utils

/**
 * A domain class that holds user's registration details.
 */
class UserRegistration {

	Long userId

	String promoCode

	Date dateCreated
	Date lastUpdated

	static constraints = {
		promoCode nullable: true, unique: true
		userId unique: true
		dateCreated bindable: false
		lastUpdated bindable: false
	}

	static mapping = {
		version false
		userId column: 'user_id', index: 'user_id_index'
	}

	String toString() {
		return "UserRegistration(id: ${id}, promoCode: ${promoCode}, dateCreated: ${dateCreated}, lastUpdated: ${lastUpdated}"
	}

	static UserRegistration create(Long userId, String promoCode = null) {
		UserRegistration userRegistration = new UserRegistration(userId: userId, promoCode: promoCode)
		Utils.save(userRegistration, true)

		return userRegistration
	}
}

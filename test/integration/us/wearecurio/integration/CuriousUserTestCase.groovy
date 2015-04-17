package us.wearecurio.integration
import grails.test.*
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

abstract class CuriousUserTestCase extends CuriousTestCase {
	static transactional = true
	
	User user
	Long userId
	
	void setUp() {
		super.setUp()
		
		User.list()*.delete()	// Deleting existing records temporary to create default user.
		def users = User.list(max:1)
		if (users.size() == 0) {
			def params = [username:'y', sex:'F', \
                name:'y y', email:'y@y.com', birthdate:'01/01/2001', \
				password:'y', action:'doregister', \
                controller:'home']

			user = User.create(params)

			Utils.save(user, true)
			println "new user " + user
		} else {
			user = users.get(0)
			println "user " + user
		}
		
		userId = user.getId()
	}

	void tearDown() {
		super.tearDown()
		
		user.delete()
	}
}

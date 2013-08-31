import grails.test.*
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

abstract class CuriousTestCase extends GroovyTestCase {
	User user
	Long userId
	
	void setUp() {
		super.setUp()

		def users = User.list(max:1)
		if (users.size() == 0) {
			def params = [username:'y', sex:'F', \
                last:'y', email:'y@y.com', birthdate:'01/01/2001', \
                first:'y', password:'y', action:'doregister', \
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
	}
}

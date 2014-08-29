package us.wearecurio.services.integration

import junit.framework.AssertionFailedError

import org.codehaus.groovy.runtime.ScriptBytecodeAdapter
import org.junit.After
import org.junit.Before

import us.wearecurio.integration.CuriousUserTestCase
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

/**
 * Superclass for service tests
 * 
 * @author mitsu
 */
abstract class CuriousServiceTestCase {
	static transactional = true
	
	User user
	Long userId
	
	boolean isClose(double a, double b, double range) {
		return Math.abs(a - b) < range
	}
	
	String shouldFail(Class<?> clazz, Closure code) {
		Throwable th = null
		try {
			code.call()
		} catch (GroovyRuntimeException gre) {
			th = ScriptBytecodeAdapter.unwrap(gre)
		} catch (Throwable e) {
			th = e
		}
		
		if (th == null) {
			throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name")
		}
		
		if (!clazz.isInstance(th)) {
			throw new AssertionFailedError("Closure $code should have failed with an exception of type $clazz.name, instead got Exception $th")
		}
		
		return th.message
	}
	
	@Before
	void setUp() {
		Utils.resetForTesting()
		
		User.list()*.delete()	// Deleting existing records temporary to create default user.
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
	
	@After
	void tearDown() {
		user.delete()
	}
}
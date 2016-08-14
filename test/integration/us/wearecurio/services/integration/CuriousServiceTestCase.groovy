package us.wearecurio.services.integration

import junit.framework.AssertionFailedError

import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.runtime.ScriptBytecodeAdapter
import org.junit.After
import org.junit.Before

import us.wearecurio.model.User
import us.wearecurio.utility.Utils
import us.wearecurio.model.Entry
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagUnitStats
import us.wearecurio.model.UserGroup
import us.wearecurio.model.GroupMemberReader
import spock.lang.Specification

/**
 * Superclass for service tests
 * 
 * @author mitsu
 */
abstract class CuriousServiceTestCase extends Specification {

	protected static def log = LogFactory.getLog(this)

	boolean isClose(double a, double b, double range) {
		return Math.abs(a - b) < range
	}
	
	Long userId
	User user
	Long userId2
	User user2

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
	
	void setup() {
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		Utils.resetForTesting()
		
		Entry.executeUpdate("delete Entry e")
		User.executeUpdate("delete User u")
		Discussion.executeUpdate("delete Discussion d")
		DiscussionPost.executeUpdate("delete DiscussionPost p")
		TagStats.executeUpdate("delete TagStats t")
		TagUnitStats.executeUpdate("delete TagUnitStats t")
		
		def entries = Entry.list(max:1)
		
		assert entries.size() == 0
		
		def users = User.list(max:1)
		
		assert users.size() == 0
		
		def params = [username:'y', sex:'F', \
			name:'y y', email:'y@y.com', birthdate:'01/01/2001', \
			password:'y', action:'doregister', \
			controller:'home']

        def params2 = [username:'z', sex:'M', \
            name:'z z', email:'z@z.com', birthdate:'01/01/2005', \
            password:'z', action:'doregister', \
            controller:'home']

		user = User.create(params)
		user2 = User.create(params2)
		
		println "new user " + user
		println "new user " + user2
		
		userId = user.getId()
		userId2 = user2.getId()
	}
	
	void cleanup() {
		User.executeUpdate("delete User u")	// Deleting existing records temporary to create default user.
		Entry.executeUpdate("delete Entry e")
	}
}

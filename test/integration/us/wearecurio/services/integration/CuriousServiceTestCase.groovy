package us.wearecurio.services.integration

import java.util.Map;
import junit.framework.AssertionFailedError
import org.springframework.web.servlet.ModelAndView

import grails.test.*

import org.codehaus.groovy.runtime.ScriptBytecodeAdapter

import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils
import grails.util.GrailsWebUtil
import org.springframework.web.util.WebUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import us.wearecurio.model.User;

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

/**
 * Superclass for service tests
 * 
 * @author mitsu
 */
public abstract class CuriousServiceTestCase {
	static transactional = true

	User user
	Long userId

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
		def params = [username:'y', sex:'F', \
            last:'y', email:'y@y.com', birthdate:'01/01/2001', \
            first:'y', password:'y', action:'doregister', \
            controller:'home']

		user = User.create(params)

		Utils.save(user, true)
		println "new user " + user
		
		userId = user.getId()
	}
	
	@After
	void tearDown() {
	}
}

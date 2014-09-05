package us.wearecurio.services.integration

import junit.framework.AssertionFailedError

import org.codehaus.groovy.runtime.ScriptBytecodeAdapter
import org.junit.After
import org.junit.Before

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.User

/**
 * Superclass for service tests
 * 
 * @author mitsu
 */
abstract class CuriousServiceTestCase extends CuriousTestCase {

	static transactional = true

	boolean isClose(double a, double b, double range) {
		return Math.abs(a - b) < range
	}
	
	/*String shouldFail(Class<?> clazz, Closure code) {
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
	}*/
	
	@Before
	void setUp() {
		super.setUp()
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}
}
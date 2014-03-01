import static org.junit.Assert.*

import org.junit.*
import grails.test.mixin.*
import us.wearecurio.utility.Utils

import us.wearecurio.model.UniqueTimedToken

class UniqueTimedTokenTests extends GroovyTestCase {
	static transactional = true

	@Before
	void setUp() {
	}

	@After
	void tearDown() {
	}

	@Test
	void testAcquire() {
		assert UniqueTimedToken.acquire("foofoo", new Date())
		assert !UniqueTimedToken.acquire("foofoo", new Date())
	}

	@Test
	void testAcquireTimeout() {
		assert UniqueTimedToken.acquire("barbar", new Date(), 1)
		
		Thread.sleep(1000)
		
		assert UniqueTimedToken.acquire("barbar", new Date(), 1)
	}
}

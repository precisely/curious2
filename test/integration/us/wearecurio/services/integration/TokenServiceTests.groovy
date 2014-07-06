package us.wearecurio.services.integration

import static org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.scribe.model.Response
import us.wearecurio.integration.CuriousTestCase

class TokenServiceTests extends CuriousTestCase {
	static transactional = false
	
	def simulatedSession
	def tokenService

	@Before
	void setUp() {
		super.setUp()
		
		simulatedSession = [:]
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	void testAcquire() {
		assert tokenService.acquire(simulatedSession, "foofoo")
		assert !tokenService.acquire(simulatedSession, "foofoo")
	}
}

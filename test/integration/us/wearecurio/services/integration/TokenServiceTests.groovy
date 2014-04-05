package us.wearecurio.services.integration

import static org.junit.Assert.*

import org.junit.After
import org.junit.Before
import org.scribe.model.Response

class TokenServiceTests {
	static transactional = false
	
	def simulatedSession
	def tokenService

	@Before
	void setUp() {
		simulatedSession = [:]
	}

	@After
	void tearDown() {
	}

	void testAcquire() {
		assert tokenService.acquire(simulatedSession, "foofoo")
		assert !tokenService.acquire(simulatedSession, "foofoo")
	}
}

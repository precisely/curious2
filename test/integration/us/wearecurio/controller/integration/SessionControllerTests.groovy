package us.wearecurio.controller.integration

import static org.junit.Assert.*
import grails.test.*

import java.text.DateFormat

import org.junit.*

import us.wearecurio.controller.SessionController
import us.wearecurio.model.*
import us.wearecurio.utility.Utils

class SessionControllerTests extends GroovyTestCase {

	static transactional = true

	DateFormat dateFormat
	TimeZone timeZone // simulated server time zone
	Date baseDate
	Date currentTime
	Date noRepeatTime

	SessionController controller

	@Before
	void setUp() {
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		timeZone = Utils.createTimeZone(-5 * 60 * 60, "GMTOFFSET5", true)
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		noRepeatTime = new Date(currentTime.getTime() - (1000L * 60 * 60 * 24))

		controller = new SessionController()

	}

	@After
	void tearDown() {
	}

	@Test
	void testSessionCache() {
		assert "bar".equals(controller.setSessionCache("foo", "bar", 100000))

		assert "bar".equals(controller.getSessionCache("foo", { return "expired" }))

		assert "blah".equals(controller.setSessionCache("blee", "blah", 1))

		Thread.sleep(3000)

		assert "expired".equals(controller.getSessionCache("blee", { return "expired" }))
	}
}

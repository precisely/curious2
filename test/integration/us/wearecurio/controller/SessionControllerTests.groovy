package us.wearecurio.controller
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import grails.test.*

import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils
import us.wearecurio.model.User
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

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
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8")
		timeZone = Utils.createTimeZone(-5 * 60 * 60, "GMTOFFSET5")
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
	def testSessionCache() {
		assert "bar".equals(controller.setSessionCache("foo", "bar", 100000))
		
		assert "bar".equals(controller.getSessionCache("foo", { return "expired" }))
		
		assert "blah".equals(controller.setSessionCache("blee", "blah", 1))

		Thread.sleep(3000)
				
		assert "expired".equals(controller.getSessionCache("blee", { return "expired" }))
	}
}

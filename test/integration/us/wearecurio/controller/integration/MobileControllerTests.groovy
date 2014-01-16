package us.wearecurio.controller.integration
import grails.test.*

import us.wearecurio.controller.MobileController;
import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class MobileControllerTests extends CuriousControllerTestCase {
	static transactional = true
	
	MobileController controller

	@Before
	void setUp() {
		super.setUp()
		
		controller = new MobileController()
	}

	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void testIndex() {
		controller.session.userId = user.getId()
		
		def retVal = controller.index()
		
		assert retVal == null
    }
}

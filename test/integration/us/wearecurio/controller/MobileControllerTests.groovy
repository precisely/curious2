package us.wearecurio.controller
import grails.test.*

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
	void testCacheManifest() {
		controller.session.userId = user.getId()
		
		controller.cachemanifest()
		
		def modelAndView = controller.modelAndView
		
		assert modelAndView.getViewName().equals("/mobile/cachemanifest")
    }

	@Test
	void testIndex() {
		controller.session.userId = user.getId()
		
		def retVal = controller.index()
		
		def modelAndView = controller.modelAndView
		
		assert retVal['login'] == 0
    }

	@Test
	void testLogin() {
		controller.session.userId = user.getId()
		
		def retVal = controller.login()
		
		def modelAndView = controller.modelAndView
		
		assert modelAndView.getViewName().equals("/mobile/index")
		
		assert modelAndView.model.toString().startsWith("[login:1")
    }
}

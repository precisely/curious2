package us.wearecurio.controller.integration

import static org.junit.Assert.*
import grails.test.*
import grails.test.mixin.*

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.junit.*
import org.springframework.web.util.WebUtils

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.*
import us.wearecurio.utility.Utils

import grails.test.mixin.integration.IntegrationTestMixin

/**
 * Superclass for controller web tests
 * 
 * Includes support for testing filters, as well as automatic user and userId setup
 * 
 * @author mitsu
 */

@TestMixin(IntegrationTestMixin)
abstract class CuriousControllerTestCase extends CuriousTestCase {
	static transactional = true

	def filterInterceptor

	@Before
	void setUp() {
		super.setUp()
		/*mockLogging(controller.getClass(), true)
		
		setLogger(controller)*/
	}
	
	@After
	void tearDown() {
		super.tearDown()
		
		user.delete()
	}
	
	/*
	void setLogger(controller) {
		def aClass = controller.class
		def logger = controller.log
		
		while (true) {
			aClass.log = logger
			aClass = aClass.superclass
			if (aClass == Object.class)
				break
		}
	}*/
	
	def filterLoggedIn(def controller, Map params, controllerName, actionName) {
		def request = controller.request
		def response = controller.response
		def session = controller.session
		
		controller.params.putAll(params)
		controller.params['controller'] = controllerName
		controller.params['action'] = actionName
		
		session.userId = userId
		
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/" + controllerName + "/" + actionName)
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, actionName)

		filterInterceptor.preHandle(request, response, null)
	}
	
	def filterNotLoggedIn(def controller, Map params, controllerName, actionName) {
		def request = controller.request
		def response = controller.response
		def session = controller.session
		
		controller.params.putAll(params)
		controller.params['controller'] = controllerName
		controller.params['action'] = actionName
		
        request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/" + controllerName + "/" + actionName)
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, controllerName)
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, actionName)
		
		filterInterceptor.preHandle(request, response, null)
	}
}


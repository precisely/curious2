package us.wearecurio.controller.integration

import java.util.Map;
import org.springframework.web.servlet.ModelAndView

import grails.test.*
import grails.util.GrailsWebUtil

import us.wearecurio.controller.MobiledataController;
import us.wearecurio.model.*
import grails.util.GrailsUtil
import grails.util.GrailsWebUtil
import us.wearecurio.utility.Utils
import us.wearecurio.server.Session
import org.springframework.web.util.WebUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

/**
 */
class MobiledataControllerTests extends CuriousControllerTestCase {
	static transactional = true

	Session mobileSession
	
	@Before
	void setUp() {
		super.setUp()

		mobileSession = Session.createOrGetSession(user)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testIndexLoggedIn() {
		MobiledataController controller = new MobiledataController()
		
		def result = filterNotLoggedIn(controller, [mobileSessionId:mobileSession.fetchUuid()], "mobiledata", "index")

		assert controller.session.userId == user.getId()
		
		/*
		// Check that we have been redirected to the expected URL.
		ModelAndView mv = new ModelAndView()
		
		filterInterceptor.postHandle(getRequest(), getResponse(), null, mv)
		
		def rUrl = response.redirectedUrl
		def vName = mv.viewName
		
		vName=vName
		
		*/
	}

	@Test
	void testIndexNotLoggedIn() {
		MobiledataController controller = new MobiledataController()
		
		def result = filterNotLoggedIn(controller, [:], "mobiledata", "index")

		assert controller.session.userId == null
	}

	@Test
	void testDologinDataSuccess() {
		MobiledataController controller = new MobiledataController()
		
		controller.session.userId = null
		controller.params['callback'] = 'callback'
		controller.params['username'] = 'y'
		controller.params['password'] = 'y'
		
		controller.dologinData()

		assert controller.response.contentAsString.startsWith('callback({"user":{"class":"us.wearecurio.model.User","id"')
	}

	@Test
	void testDologinDataFailure() {
		MobiledataController controller = new MobiledataController()
		
		controller.session.userId = null
		controller.params['callback'] = 'callback'
		controller.params['username'] = 'y'
		controller.params['password'] = 'z'
		
		controller.dologinData()

		assert controller.response.contentAsString.equals('callback({"success":false})')
	}

	@Test
	void testDoregisterDataSuccess() {
		MobiledataController controller = new MobiledataController()
		
		UserGroup curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
				[isReadOnly:false, defaultNotify:false])
		UserGroup announce = UserGroup.create("announce", "Curious Announcements", "Announcements for Curious users",
				[isReadOnly:true, defaultNotify:true])
		
		controller.session.userId = null
		
		controller.params.clear()
		controller.params.putAll([
			username:'q',
			email:'q@q.com',
			password:'q',
			groups:"['curious','announce']"
		])
		
		controller.doregisterData()
		
		def q = User.findByUsername('q')
		
		assert curious.hasWriter(q)
		assert !announce.hasWriter(q)
		assert announce.hasReader(q)
		
		assert controller.response.contentAsString.startsWith('{"success":true,"persistentSessionId":')
	}

	@Test
	void testDoregisterDataNoUsername() {
		MobiledataController controller = new MobiledataController()
		
		controller.session.userId = null
		
		controller.params.clear()
		controller.params.putAll([
			email:'q@q.com',
			password:'q',
		])
		
		controller.doregisterData()
		
		assert controller.response.contentAsString.startsWith('{"success":false')
	}

	@Test
	void testDoregisterDataNoPassword() {
		MobiledataController controller = new MobiledataController()
		
		controller.session.userId = null
		
		controller.params.clear()
		controller.params.putAll([
			email:'q@q.com',
			username:'q',
		])
		
		controller.doregisterData()
		
		assert controller.response.contentAsString.startsWith('{"success":false')
	}
	
	@Test
	void testRegisterNotification() {
		MobiledataController controller = new MobiledataController()
		controller.session.userId = userId
		
		controller.params.clear()
		controller.params.putAll([
			token:'asdfsdfsdfalsdjflskjdfsalkfj',
			deviceType:'1',
		])
		
		controller.registerForPushNotificationData()
		
		assert controller.response.contentAsString.startsWith('null({"success":true')
	}
	
	void testunregisterNotification() {
		MobiledataController controller = new MobiledataController()
		controller.session.userId = userId
		
		def pushNotificationDeviceInstance = 
			new PushNotificationDevice(token:'asdfsdfasdfsadfas',
				deviceType:PushNotificationDevice.IOS_DEVICE,
				userId: user.id)
			
		controller.params.clear()
		controller.params.putAll([
			token:'asdfsdfasdfsadfas',
		])
		
		controller.unregisterPushNotification()
		
		assert controller.response.contentAsString.startsWith('null({"success":true')
	}
	
}

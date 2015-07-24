package us.wearecurio.controller.integration
import java.util.Map;
import org.springframework.web.servlet.ModelAndView

import grails.test.*

import us.wearecurio.controller.LoginController;
import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils
import grails.util.GrailsWebUtil
import org.springframework.web.util.WebUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

public class LoginControllerTests extends CuriousControllerTestCase {
	static transactional = true

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void testDologinDataSuccess() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		controller.params['callback'] = 'callback'
		controller.params['username'] = user.getUsername()
		controller.params['password'] = 'y'
		
		controller.dologinData()

		assert controller.response.contentAsString.startsWith('callback({"user":{"class":"us.wearecurio.model.User","id"')
	}

	@Test
	void testDologinDataFailure() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		controller.params['callback'] = 'callback'
		controller.params['username'] = user.getUsername()
		controller.params['password'] = 'z'
		
		controller.dologinData()

		assert controller.response.contentAsString.equals('callback({"success":false})')
	}

	@Test
	void testLogin() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		controller.login()
		
		def model = controller.modelAndView.model
		
		assert model.precontroller.equals('login')
		assert model.preaction.equals('index')
		assert model.message == null
	}

	@Test
	void testDologinSuccess() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		controller.params['username'] = user.getUsername()
		controller.params['password'] = 'y'
		controller.params['precontroller'] = 'login'
		controller.params['preaction'] = 'index'

		controller.dologin()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/login/index")
	}

	@Test
	void testDologinFailure() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		controller.params['username'] = user.getUsername()
		controller.params['password'] = 'z'
		controller.params['precontroller'] = 'login'
		controller.params['preaction'] = 'index'

		controller.dologin()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/home/login")
	}

	@Test
	void testForgot() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null

		def m = controller.doforgot()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/login/forgot")
	}

	@Test
	void testDoforgotSuccess() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		controller.params['email'] = user.getEmail()

		controller.doforgot()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/login/login")
	}

	@Test
	void testDoforgotFailure() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		controller.params['email'] = 'aergaergaerh@esrijgfaergaeth.com'

		controller.doforgot()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/login/forgot")
	}
	
	@Test
	void testDoforgotDataEmailSuccess() {
		LoginController controller = new LoginController()
		
		controller.params['callback'] = 'callback'
		controller.params['email'] = user.getEmail()
		
		controller.doforgotData()
		
		def x = controller.response.contentAsString
		x = x

		assert x == 'callback({"success":true})'
	}

	@Test
	void testDoforgotDataUsernameSuccess() {
		LoginController controller = new LoginController()
		
		controller.params['callback'] = 'callback'
		controller.params['username'] = user.getUsername()
		
		controller.doforgotData()
		
		def x = controller.response.contentAsString
		x = x

		assert x == 'callback({"success":true})'
	}

	@Test
	void testDoforgotDataEmailFailures() {
		LoginController controller = new LoginController()
		
		controller.params['callback'] = 'callback'
		controller.params['email'] = "aetnatnatnathfavsdvsd"
		
		controller.doforgotData()
		
		def x = controller.response.contentAsString
		x = x

		assert x.endsWith('"success":false})')
	}

	@Test
	void testDoforgotDataUsernameFailure() {
		LoginController controller = new LoginController()
		
		controller.params['callback'] = 'callback'
		controller.params['username'] = "abtarbaergaerg"
		
		controller.doforgotData()
		
		def x = controller.response.contentAsString
		x = x

		assert x.endsWith('"success":false})')
	}

	@Test
	void testRecoverSuccess() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		def recovery = PasswordRecovery.create(user.getId())
		
		controller.params.code = recovery.getCode()
		
		controller.recover()
		
		def mV = controller.modelAndView
		def model = controller.modelAndView.model
		def view = controller.modelAndView.viewName
		
		assert view.equals("/login/recover")
		assert model.code?.length() > 0
	}

	@Test
	void testRecoverFailure() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		def recovery = PasswordRecovery.create(user.getId())
		
		controller.params.code = recovery.getCode() + 'foobar'
		
		controller.recover()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/login/forgot")
	}

	@Test
	void testDorecoverSuccess() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		def recovery = PasswordRecovery.create(user.getId())
		
		controller.params.code = recovery.getCode()
		controller.params.password = "w"
		
		controller.dorecover()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/home/index")
		
		assert user.checkPassword('w')
	}

	@Test
	void testDorecoverFailure() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		def recovery = PasswordRecovery.create(user.getId())
		
		controller.params.code = recovery.getCode() + 'foobar'
		controller.params.password = "w"
		
		controller.dorecover()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/login/recover")
	}

	@Test
	void testDorecoverNoPassword() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		def recovery = PasswordRecovery.create(user.getId())
		
		controller.params.code = recovery.getCode()
		
		controller.dorecover()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/login/recover")
	}

	@Test
	void testRegister() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		controller.register()
		
		def mV = controller.modelAndView
		def model = controller.modelAndView.model
		def view = controller.modelAndView.viewName
		
		assert view.equals("/login/register")
	}

	@Test
	void testDoregisterSuccess() {
		LoginController controller = new LoginController()
		
		UserGroup curious = UserGroup.create("curious", "Curious Discussions", "Discussion topics for Curious users",
				[isReadOnly:false, defaultNotify:false])
		UserGroup announce = UserGroup.create("announce", "Curious Announcements", "Announcements for Curious users",
				[isReadOnly:true, defaultNotify:true])
		
		controller.session.userId = null
		
		controller.params.clear()
		controller.params.putAll([
			username:'q',
			email:'q@q.com',
			confirm_email: 'q@q.com',
			password:'q',
			name:'q q',
			sex:'F',
			groups:"['curious','announce']"
		])
		
		def c = controller.doregister()
		
		def q = User.findByUsername('q')
		
		assert curious.hasWriter(q)
		assert !announce.hasWriter(q)
		assert announce.hasReader(q)
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith('/home/index')
	}

	@Test
	void testDoregisterNoUsername() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		controller.params.clear()
		controller.params.putAll([
			email:'q@q.com',
			password:'q',
		])
		
		controller.doregister()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith('/login/register')
	}

	@Test
	void testDoregisterDataSuccess() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		controller.params.clear()
		controller.params.putAll([
			username:'q',
			email:'q@q.com',
			confirm_email: 'q@q.com',
			password:'q',
			name:'q q',
			sex:'F'
		])
		
		controller.doregisterData()
		
		assert controller.response.contentAsString.startsWith('{"success":true')
	}

	@Test
	void testDoregisterDataNoUsername() {
		LoginController controller = new LoginController()
		
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
	void testDoregisterDifferentEmailIds() {
		LoginController controller = new LoginController()
		
		controller.session.userId = null
		
		controller.params.clear()
		controller.params.putAll([
			username: 'q',
			email: 'q@q.com',
			confirm_email: 'qq@q.com',
			password: 'q',
			name: 'q q',
			sex:' F',
			groups: "['curious','announce']"
		])
		
		controller.doregister()
		
		def rU = controller.response.redirectedUrl
		
		assert controller.flash.message == "Error registering user - email and confirm email fields have different values"
		assert rU.endsWith('/login/register')
	}

	@Test
	void testDoregisterDataNoPassword() {
		LoginController controller = new LoginController()
		
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
	void testLogout() {
		LoginController controller = new LoginController()
		
		controller.session.userId = userId
		
		controller.params.clear()
		
		controller.logout()
		
		assert controller.session.userId == null
	}
}

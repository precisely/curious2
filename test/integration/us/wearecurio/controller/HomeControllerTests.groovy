package us.wearecurio.controller
import java.util.Map;
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.multipart.MultipartHttpServletRequest

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest

import grails.test.*

import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils
import grails.util.GrailsWebUtil
import org.springframework.web.util.WebUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

public class HomeControllerTests extends CuriousControllerTestCase {
	static transactional = true

	PlotData plotData
	Discussion discussion

	@Before
	void setUp() {
		super.setUp()

		plotData = PlotData.create(user, "Plot", "{}", true)
		Utils.save(plotData, true)
		discussion = Discussion.create(user, "Discussion name")
		discussion.createPost(DiscussionAuthor.create(user), plotData.getId(), "comment")
		Utils.save(discussion, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testIndexNotLoggedIn() {
		HomeController controller = new HomeController()
		
		def result = filterNotLoggedIn(controller, [:], "home", "index")

		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith("/home/login")
	}

	@Test
	void testIndexLoggedIn() {
		HomeController controller = new HomeController()
		
		def result = filterLoggedIn(controller, [:], "home", "index")

		def rU = controller.response.redirectedUrl
		
		assert rU == null
	}

	@Test
	void testUserPreferences() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		controller.params.userId = user.getId().toString()
		
		controller.userpreferences()
		
		def modelAndView = controller.modelAndView
		
		assert modelAndView.model.toString().startsWith("[precontroller:home, preaction:index, user:User(")
		assert modelAndView.model.toString().contains("username: y email: y@y.com remindEmail: null password: set first: y last: y sex: F location: null birthdate:")
		assert modelAndView.model.toString().endsWith("twitterAccountName: null twitterDefaultToNow: true), prefs:[twitterAccountName:null, twitterDefaultToNow:true, displayTimeAfterTag:true, webDefaultToNow:true], templateVer:main]")
		assert modelAndView.getViewName().equals("/home/userpreferences")
    }
	
	@Test
	void testDoUpdateUserPreferences() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		controller.params.clear()
		controller.params.putAll([
			displayTimeAfterTag:'on',
			username:'y', sex:'F',
			preaction:'index',
			twitterDefaultToNow:'on',
			precontroller:'home',
			first:'x',
			last:'y',
			email:'x@x.x',
			userId:user.getId().toString(),
			birthdate:'01/01/2001',
			password:'',
			action:'doupdateuserpreferences',
			controller:'home'
		])
		
		def retVal = controller.doupdateuserpreferences()
		
		def rU = controller.response.redirectedUrl
		
		assert rU.endsWith('/home/index')
	}

	@Test
	void testDoUpload() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		controller.request.addFile('csvFile', "\"export_userdemo\",\"GMT\",\"0\"\r\n\"06/17/2012 GMT\",\"betaine hcl 648.000000000 mg \"\r\n\"06/17/2012 GMT\",\"lychee import test 44mg\"\r\n\"06/17/2012 GMT\",\"brownies \"\r\n\"06/17/2012 GMT\",\"b-6 100.000000000 mg \"".getBytes())
				
		controller.doUpload()
		
		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", user.getId())
				eq("tag", Tag.look("lychee import test"))
				eq("amount", new BigDecimal(44))
				eq("units", "mg")
				eq("setName", "export_userdemo")
			}
			maxResults(1)
		}
		
		def r = null
		for (result in results) {
			r = result
			break
		}
		
		assert r != null
	}
	
	@Test
	void testIndex() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		def model = controller.index()
		
		assert model.prefs != null	
	}

	@Test
	void testLoad() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		def model = controller.load()

		assert model.prefs != null	
	}

	@Test
	void testGraph() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		def model = controller.graph()

		assert model.prefs != null	
	}

	@Test
	void testCommunity() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		def model = controller.community()

		assert model.prefs != null	
	}

	@Test
	void testUpload() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		def model = controller.upload()

		assert model.prefs != null	
	}

	@Test
	void testDownload() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		def model = controller.download()

		assert controller.response.contentType.equals('text/csv')
	}
	
	@Test
	void testViewgraph() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		controller.params['plotDataId'] = plotData.getId().toString()

		controller.viewgraph()

		def modelAndView = controller.modelAndView
		
		assert modelAndView.model['plotDataId'].equals(plotData.getId().toString())
		assert modelAndView.getViewName().equals("/home/graph")
	}
	
	@Test
	void testDiscuss() {
		HomeController controller = new HomeController()
		
		controller.session.userId = user.getId()
		
		controller.params['discussionId'] = discussion.getId().toString()

		controller.discuss()

		def modelAndView = controller.modelAndView
		
		assert modelAndView.model['discussionId'].toString().equals(discussion.getId().toString())
		assert modelAndView.model['username'].equals(user.getUsername())
		assert modelAndView.getViewName().equals("/home/discuss")
	}
}

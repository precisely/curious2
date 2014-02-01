package us.wearecurio.controller.integration
import java.util.Map;
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.multipart.MultipartHttpServletRequest

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest

import grails.test.*

import us.wearecurio.controller.HomeController;
import us.wearecurio.model.*
import grails.util.GrailsUtil
import us.wearecurio.utility.Utils
import grails.util.GrailsWebUtil
import org.springframework.web.util.WebUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.thirdparty.AuthenticationRequiredException

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

public class HomeControllerTests extends CuriousControllerTestCase {
	static transactional = true

	PlotData plotData
	Discussion discussion
	def withingsDataServiceMock
	
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
		
		controller.request.addFile(
			'csvFile', ('"Date (GMT) for exporttest","Tag","Amount","Units","Comment","RepeatType","Amount Precision","Date Precision","Time Zone"\n'
					+ '"2010-07-01 12:39:00 GMT","rererg",122.000000000,"undefined","undefined",-1,3,180,"America/New_York"\n'
					+ '"2010-07-03 12:20:00 GMT","raspberry ginger smoothie",2.000000000,"mg","",-1,3,180,"America/New_York"\n'
					+ '"2010-07-03 17:00:00 GMT","hello kitty",1.000000000,"","",-1,3,180,"America/New_York"').getBytes())

		controller.doUpload()
		
		def c = Entry.createCriteria()
		def results = c {
			and {
				eq("userId", user.getId())
				eq("tag", Tag.look("raspberry ginger smoothie"))
				eq("amount", new BigDecimal(2))
				eq("units", "mg")
				eq("setName", "Date (GMT) for exporttest")
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
	
	@Test
	void testUnregisterwithings() {
		HomeController controller = new HomeController()
		controller.withingsDataService = [
			unSubscribe: { userId -> return [success:true] }
		] as WithingsDataService
		controller.session.userId = userId
		
		controller.unregisterwithings()
		assert controller.flash.message.contains("Successfully un-linked Withings account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testUnregisterfitbit() {
		HomeController controller = new HomeController()
		controller.fitBitDataService = [
			unSubscribe: { userId -> return [success:true] }
		] as FitBitDataService
		controller.session.userId = userId
		
		controller.unregisterfitbit()
		assert controller.flash.message.contains("Successfully un-linked Fitbit account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testUnregistermoves() {
		HomeController controller = new HomeController()
		controller.movesDataService = [
			unSubscribe: { userId -> return [success:true] }
		] as MovesDataService
		controller.session.userId = userId
		
		controller.unregistermoves()
		assert controller.flash.message.contains("Successfully un-linked Moves account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}
}

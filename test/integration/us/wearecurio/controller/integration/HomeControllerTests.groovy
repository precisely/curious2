package us.wearecurio.controller.integration

import static org.junit.Assert.*

import org.junit.*

import us.wearecurio.controller.HomeController
import us.wearecurio.model.*
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.utility.Utils

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
	void testPollDevices() {
		def account = new OAuthAccount([typeId: ThirdParty.WITHINGS, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account, true)

		def account2 = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "Dummy-token",
			accessSecret: "Dummy-secret", accountId: "dummy-id", timeZoneId: TimeZoneId.look("America/New_York").id])

		Utils.save(account2, true)

		HomeController controller = new HomeController()

		controller.session.userId = user.getId()

		controller.polldevices()

		def modelAndView = controller.modelAndView

		def rU = controller.response.redirectedUrl

		assert rU.endsWith("/home/index")
	}

	@Test
	void testRegisterwithings() {
		OAuthAccount account = new OAuthAccount([typeId: ThirdParty.WITHINGS, userId: userId, accessToken: "token",
			accessSecret: "secret", accountId: "id"])
		account.save()

		HomeController controller = new HomeController()
		controller.withingsDataService = [
			subscribe: { Long userId -> return [success:true, account: account] }
		] as WithingsDataService
		controller.session.userId = userId

		controller.registerwithings()

		assert controller.flash.message.contains("Successfully linked Withings account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testUnregisterwithings() {
		HomeController controller = new HomeController()
		controller.withingsDataService = [
			unsubscribe: { Long userId -> return [success:true] }
		] as WithingsDataService
		controller.session.userId = userId

		controller.unregisterwithings()
		assert controller.flash.message.contains("Successfully unlinked Withings account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testRegisterfitbit() {
		HomeController controller = new HomeController()
		controller.fitBitDataService = [
			subscribe: { Long userId -> return [success:true] }
		] as FitBitDataService
		controller.session.userId = userId

		controller.registerfitbit()
		assert controller.flash.message.contains("Successfully linked FitBit account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testUnregisterfitbit() {
		HomeController controller = new HomeController()
		controller.fitBitDataService = [
			unsubscribe: { Long userId -> return [success:true] }
		] as FitBitDataService
		controller.session.userId = userId

		controller.unregisterfitbit()
		assert controller.flash.message.contains("Successfully unlinked FitBit account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testRegistermoves() {
		HomeController controller = new HomeController()
		controller.movesDataService = [
			subscribe: { Long userId -> return [success:true] }
		] as MovesDataService
		controller.session.userId = userId

		controller.registermoves()
		assert controller.flash.message.contains("Successfully linked Moves account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testUnregistermoves() {
		HomeController controller = new HomeController()
		controller.movesDataService = [
			unsubscribe: { Long userId -> return [success:true] }
		] as MovesDataService
		controller.session.userId = userId

		controller.unregistermoves()
		assert controller.flash.message.contains("Successfully unlinked Moves account")
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}
}

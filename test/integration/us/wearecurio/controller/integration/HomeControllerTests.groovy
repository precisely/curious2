package us.wearecurio.controller.integration

import static org.junit.Assert.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.controller.HomeController
import us.wearecurio.model.*
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.utility.Utils

public class HomeControllerTests extends CuriousControllerTestCase {

	PlotData plotData
	Discussion discussion
	def withingsDataServiceMock

	HomeController controller
	FitBitDataService fitBitDataService
	WithingsDataService withingsDataService

	User user2

	@Before
	void setUp() {
		super.setUp()

		user2 = new User([username: "dummy2", email: "dummy2@curious.test", sex: "M", first: "Mark", last: "Leo",
			password: "Dummy password", displayTimeAfterTag: false, webDefaultToNow: true])
		assert user2.save()

		plotData = PlotData.create(user, "Plot", "{}", true)
		Utils.save(plotData, true)
		discussion = Discussion.create(user, "Discussion name")
		discussion.createPost(DiscussionAuthor.create(user), plotData.getId(), "comment")
		Utils.save(discussion, true)

		controller = new HomeController()
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
		assertNotNull OAuthAccount.findByUserIdAndTypeId(userId, ThirdParty.WITHINGS)

		controller.session.userId = user2.id

		shouldFail(AuthenticationRequiredException) {
			// When no OAuthAccount exist.
			controller.registerwithings()
		}
		controller.response.reset()

		controller.session.userId = userId
		withingsDataService.oauthService = [
			getWithingsResourceWithQuerystringParams: { token, url, method, p ->
				return new Response(new MockedHttpURLConnection("""{"status": 342}"""))
			}
		]

		shouldFail(AuthenticationRequiredException) {
			// When token expired & re-subscribing.
			controller.registerwithings()
		}

		controller.response.reset()
		account.accessToken = "some-token"
		account.save()

		withingsDataService.oauthService = [
			getWithingsResourceWithQuerystringParams: { token, url, method, p ->
				return new Response(new MockedHttpURLConnection("""{"status": 293}"""))
			}
		]
		controller.registerwithings()
		assert account.accessToken == ""
		assert controller.flash.message.contains("Unable to link Withings account. Please try again")
		assert controller.response.redirectUrl.contains("home/userpreferences")

		account.accessToken = "some-token"
		account.save()

		controller.response.reset()

		withingsDataService.oauthService = [
			getWithingsResourceWithQuerystringParams: { token, url, method, p ->
				return new Response(new MockedHttpURLConnection("""{"status": 0}"""))
			}
		]

		controller.registerwithings()
		assert controller.flash.message.contains("Successfully linked Withings account.")
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
		OAuthAccount account = new OAuthAccount([typeId: ThirdParty.FITBIT, userId: userId, accessToken: "token",
			accessSecret: "secret", accountId: "id"])
		account.save()
		assertNotNull OAuthAccount.findByUserIdAndTypeId(userId, ThirdParty.FITBIT)

		controller.session.userId = user2.id

		shouldFail(AuthenticationRequiredException) {
			// When no OAuthAccount exist.
			controller.registerfitbit()
		}
		controller.response.reset()

		controller.session.userId = userId
		fitBitDataService.oauthService = [
			postFitBitResource: { token, url, p, h ->
				return new Response(new MockedHttpURLConnection(401))
			}
		]

		shouldFail(AuthenticationRequiredException) {
			// When token expired & re-subscribing.
			controller.registerfitbit()
		}

		controller.response.reset()
		account.accessToken = "some-token"
		account.save()

		fitBitDataService.oauthService = [
			postFitBitResource: { token, url, p, h ->
				return new Response(new MockedHttpURLConnection(409))
			}
		]
		controller.registerfitbit()
		assert controller.flash.message.contains("already been subscribed.")
		assert controller.response.redirectUrl.contains("home/userpreferences")

		controller.response.reset()

		account.accessToken = "some-token"
		account.save()

		fitBitDataService.oauthService = [
			postFitBitResource: { token, url, p, h ->
				return new Response(new MockedHttpURLConnection("""{}"""))
			}
		]

		controller.registerfitbit()
		assert controller.flash.message.contains("Successfully linked FitBit account.")
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
		OAuthAccount account = new OAuthAccount([typeId: ThirdParty.MOVES, userId: userId, accessToken: "token",
			accessSecret: "secret", accountId: "id"])
		account.save()
		assertNotNull OAuthAccount.findByUserIdAndTypeId(userId, ThirdParty.MOVES)

		controller.session.userId = user2.id

		shouldFail(AuthenticationRequiredException) {
			// When no OAuthAccount exist.
			controller.registermoves()
		}

		controller.session.userId = userId
		controller.response.reset()

		controller.registermoves()
		assert controller.flash.message.contains("Successfully linked Moves account.")
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

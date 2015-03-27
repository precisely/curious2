package us.wearecurio.controller.integration

import static org.junit.Assert.*

import org.junit.*
import org.scribe.model.Response

import us.wearecurio.controller.HomeController
import us.wearecurio.model.*
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.MovesDataService
import us.wearecurio.services.UrlService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.AuthenticationRequiredException
import us.wearecurio.utility.Utils

public class HomeControllerTests extends CuriousControllerTestCase {
	static transactional = true

	PlotData plotData
	Discussion discussion
	def withingsDataServiceMock

	HomeController controller
	FitBitDataService fitBitDataService
	WithingsDataService withingsDataService
	UrlService urlService

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
		String mockedResponseData = """{"status":0,"body":{"updatetime":1385535542,"measuregrps":[{"grpid":162026288,"attrib":2,"date":1385386484,"category":1,"comment":"Hello","measures":[{"value":6500,"type":1,"unit":-2}]},{"grpid":162028086,"attrib":2,"date":1385300615,"category":1,"comment":"Nothing to comment","measures":[{"value":114,"type":9,"unit":0},{"value":113,"type":10,"unit":0},{"value":74,"type":11,"unit":0}]},{"grpid":129575271,"attrib":2,"date":1372931328,"category":1,"comment":"sa","measures":[{"value":170,"type":4,"unit":-2}]},{"grpid":129575311,"attrib":2,"date":1372844945,"category":1,"measures":[{"value":17700,"type":1,"unit":-2}]},{"grpid":129575122,"attrib":2,"date":1372844830,"category":1,"comment":"sa","measures":[{"value":6300,"type":1,"unit":-2}]},{"grpid":128995279,"attrib":0,"date":1372706279,"category":1,"measures":[{"value":84,"type":9,"unit":0},{"value":138,"type":10,"unit":0},{"value":77,"type":11,"unit":0}]},{"grpid":128995003,"attrib":0,"date":1372706125,"category":1,"measures":[{"value":65861,"type":1,"unit":-3}]}]}}"""
		MockedHttpURLConnection mockedConnection = new MockedHttpURLConnection(mockedResponseData)
		String mockedFitBitResponseData = """{"someKey": "someValue"}"""
		MockedHttpURLConnection mockedFitBitConnection = new MockedHttpURLConnection(mockedFitBitResponseData)
		withingsDataService.oauthService = [
				getWithingsResource: { token, url, body, header ->
					return new Response(mockedConnection)
				},
				getWithingsResourceWithQuerystringParams: { token, url, body, header ->
					return new Response(new MockedHttpURLConnection("""{"status": 0, "body": {"activities": []}}"""))
				},
			]
		withingsDataService.urlService = urlService
		fitBitDataService.oauthService = [
				getFitBitResource: { token, url, body, header ->
					return new Response(mockedFitBitConnection)
				},
				getFitBitResourceWithQuerystringParams: { token, url, body, header ->
					return new Response(mockedFitBitConnection)
				},
			]
		controller = new HomeController()
		controller.withingsDataService = withingsDataService
		controller.fitBitDataService = fitBitDataService
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
		assert modelAndView.model.toString().contains("username: y email: y@y.com remindEmail: null password: set first: y last: y sex: F birthdate:")
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
				eq("setIdentifier", Identifier.look("Date (GMT) for exporttest"))
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
	void testFeed() {
		HomeController controller = new HomeController()

		controller.session.userId = user.getId()

		def model = controller.feed()

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
		assert controller.flash.message == "thirdparty.subscribe.failure.message"
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
		assert controller.flash.message == "thirdparty.subscribe.success.message"
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testUnregisterwithings() {
		HomeController controller = new HomeController()
		controller.withingsDataService = [
			unsubscribe: { Long userId -> return [success:true] }
		] as WithingsDataService
		controller.withingsDataService.urlService = urlService
		controller.session.userId = userId

		controller.unregisterwithings()
		assert controller.flash.message == "thirdparty.unsubscribe.success.message"
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
		assert controller.flash.message == "thirdparty.subscribe.success.message"
		assert controller.flash.args[1].contains("already been subscribed")
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
		assert controller.flash.message == "thirdparty.subscribe.success.message"
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
		assert controller.flash.message == "thirdparty.unsubscribe.success.message"
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
		assert controller.flash.message == "thirdparty.subscribe.success.message"
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
		assert controller.flash.message == "thirdparty.unsubscribe.success.message"
		assert controller.response.redirectUrl.contains("home/userpreferences")
	}

	@Test
	void testDeleteComment() {
		HomeController controller = new HomeController()

		Discussion discussionInstance = Discussion.create(user, "dummyDiscussion")
		def discussionPostInstance = discussionInstance.createPost(DiscussionAuthor.create(user), "comment")

		// When the user isn't authorized to delete comment
		controller.params['discussionId'] = discussionInstance.id
		controller.params['clearPostId'] = discussionPostInstance.getId()
		controller.session.userId = user2.getId()
		controller.discuss()
		assert controller.flash.message == ("Can't delete that post")
		controller.flash.message = null

		//When user is authorized
		controller.params['discussionId'] = discussionInstance.id
		controller.params['clearPostId'] = discussionPostInstance.getId()
		controller.session.userId = user.getId()
		controller.discuss()
		assert controller.flash.message == null
	}

	@Test
	void testCreateComment() {
		HomeController controller = new HomeController()
		controller.session.userId = user.getId()
		
		// When the user doesn't have the write permission
		controller.params['discussionId'] = discussion.getId().toString()
		controller.params['message'] = "dummyMessage"
		controller.session.userId = user2.getId()
		controller.discuss()
		assert controller.flash.message == ("You don't have permission to add a comment to this discussion")
		controller.response.reset()
		
		// When the user have the write permission
		controller.params['discussionId'] = discussion.getId().toString()
		controller.params['message'] = "dummyMessage"
		controller.session.userId = user.getId()
		controller.discuss()
		assert controller.response.redirectUrl.contains("home/discuss")
	}

	Sprint dummySprint
	User dummyUser2
	void mockSprintData() {
		dummySprint = Sprint.create(user, "demo", Model.Visibility.PRIVATE)
				
				Map params = [username: "a", sex: "F", last: "y", email: "a@a.com", birthdate: "01/01/2001", first: "a", password: "y"]
						dummyUser2 = User.create(params)
						
						Utils.save(dummyUser2, true)
	}

	@Test 
	void "Test sprint when wrong id is passed"() {
		HomeController controller = new HomeController()
		
		mockSprintData()
		controller.params["id"] = 0
		controller.session.userId = user.getId()
		
		controller.sprint()
		assert controller.flash.message == ("Sprint does not exist.")
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test sprint when null id is passed"() {
		HomeController controller = new HomeController()
		
		mockSprintData()
		controller.params["id"] = null
		controller.session.userId = user.getId()
		
		controller.sprint()
		assert controller.flash.message == ("Sprint does not exist.")
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test sprint when a non member tries to open the private sprint"() {
		HomeController controller = new HomeController()

		mockSprintData()
		controller.params["id"] = dummySprint.id
		controller.session.userId = user2.getId()
		
		controller.sprint()
		assert controller.flash.message == ("You are not permitted to see that sprint.")
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test sprint"() {
		HomeController controller = new HomeController()

		mockSprintData()
		controller.params["id"] = dummySprint.id
		controller.session.userId = user.getId()
		
		controller.sprint()

		def modelAndView = controller.modelAndView
		assert modelAndView.model['participants'][0].id == user.getId()
		assert modelAndView.getViewName().equals("/home/sprint")
	}


	@Test
	void "Test leaveSprint when wrong sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = 0
		
		controller.leaveSprint()
		assert controller.flash.message == ("Sprint does not exist.")
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test leaveSprint when null sprintId is passed"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = null
		
		controller.leaveSprint()
		assert controller.flash.message == ("Sprint does not exist.")
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test leaveSprint when non member user tries to leave the sprint"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.leaveSprint()
		assert controller.flash.message == ("You are not a member of this sprint.")
		assert controller.response.redirectUrl.contains("sprint")
	}

	@Test
	void "Test leaveSprint when a member leaves the sprint"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.leaveSprint()
		assert controller.response.redirectUrl.contains("sprint")
		assert dummySprint.hasMember(user.getId()) == false
	}

	@Test
	void "Test joinSprint when wrong sprintId is passed"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = 0
		
		controller.joinSprint()
		assert controller.flash.message == ("Sprint does not exist.")
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test joinSprint when null sprintId is passed"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = null
		
		controller.joinSprint()
		assert controller.flash.message == ("Sprint does not exist.")
		assert controller.response.redirectUrl.contains("feed")
	}

	@Test
	void "Test joinSprint when member user tries to join the sprint"() {
		mockSprintData()
		controller.session.userId = user.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.joinSprint()
		assert controller.flash.message == ("You have already joined this sprint.")
		assert controller.response.redirectUrl.contains("sprint")
	}

	@Test
	void "Test joinSprint when a non member joins the sprint"() {
		mockSprintData()
		controller.session.userId = dummyUser2.getId()
		controller.params["sprintId"] = dummySprint.id
		
		controller.joinSprint()
		assert controller.response.redirectUrl.contains("sprint")
		assert dummySprint.hasMember(dummyUser2.getId()) == true
	}
}

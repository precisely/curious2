package us.wearecurio.controller.integration

import org.junit.After
import org.junit.Before
import org.junit.Test
import us.wearecurio.controller.UserController
import us.wearecurio.model.Entry
import us.wearecurio.model.Model
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.EntryParserService
import us.wearecurio.services.SearchService
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils
import java.text.DateFormat

class UserControllerTests extends CuriousControllerTestCase {
	static transactional = true

	UserController controller
	User dummyUser2
	EntryParserService entryParserService
	DateFormat dateFormat
	SearchService searchService

	@Before
	void setUp() {
		super.setUp()
		
		controller = new UserController()
		
		Map params = [username: "a", sex: "F", last: "y", email: "a@a.com", birthdate: "01/01/2001", name: "a", password: "y"]
		dummyUser2 = User.create(params)

		Utils.save(dummyUser2, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "Test show when wrong hash id is passed"() {
		controller.params["id"] = 0
		
		controller.show()
		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("not.exist.message", ["User"] as Object[], null)
	}

	@Test
	void "Test show when requested user details are not of current user"() {
		controller.params["id"] = dummyUser2.hash
		controller.session.userId = userId
		
		controller.show()
		assert controller.response.json.success
		assert controller.response.json.user.username == "a"
		assert !controller.response.json.user.email
	}

	@Test
	void "Test show when requested user details are of current user"() {
		controller.params["id"] = dummyUser2.hash
		controller.session.userId = dummyUser2.id

		controller.show()
		assert controller.response.json.success
		assert controller.response.json.user.username == "a"
		assert controller.response.json.user.email == 'a@a.com'
	}
	
	@Test
	void "Test add tutorial tags"() {
		controller.params["id"] = dummyUser2.hash
		controller.session.userId = dummyUser2.id
		controller.params["tags[]"] = 'sleep'

		controller.addTutorialTags()
		assert controller.response.json.success
		assert dummyUser2.hasInterestTag(Tag.look('sleep'))
		def entries = Entry.fetchListData(dummyUser2, "Etc/UTC", new Date(), new Date())
		int found = 0
		for (entry in entries) {
			if (entry.description == 'sleep')
				++found
		}
		assert found == 1
		
		controller.params["id"] = dummyUser2.hash
		controller.session.userId = dummyUser2.id
		controller.params["tags[]"] = ['sleep','supplements']

		controller.addTutorialTags()
		assert controller.response.json.success
		assert dummyUser2.hasInterestTag(Tag.look('sleep'))
		assert dummyUser2.hasInterestTag(Tag.look('supplements'))
		entries = Entry.fetchListData(dummyUser2, "Etc/UTC", new Date(), new Date())
		found = 0
		for (entry in entries) {
			if (entry.description == 'sleep')
				++found
		}
		assert found == 1
	}

	@Test
	void "test markFirstChartPlotted for correctly setting the FIRST_CHART_PLOT bit"() {
		controller.session.userId = dummyUser2.id

		int FIRST_CHART_PLOT = 24

		// First clearing the previously set bits. (Just to ensure that this call sets the bits correctly)
		dummyUser2.settings.clear(FIRST_CHART_PLOT)

		assert !dummyUser2.settings.get(FIRST_CHART_PLOT)

		controller.markFirstChartPlotted()

		assert dummyUser2.settings.get(FIRST_CHART_PLOT)
	}

	@Test
	void "test deleteAccount endpoint for deleting User account"() {
		given: "User and some random data for this User"
		Map params = [username: "testuser", sex: "M", last: "last", email: "a@a.com", birthdate: "01/01/2001",
				name: "a", password: "y"]
		User userInstance = User.create(params)
		Utils.save(userInstance, true)

		assert userInstance.id

		controller.session.userId = userInstance.id
		Date currentTime = new Date()
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		Date baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		EntryStats stats = new EntryStats(userInstance.id)
		Sprint.create(currentTime, userInstance, "Caffeine + Sugar", Model.Visibility.PUBLIC)

		Entry entry = Entry.create(userInstance.id, entryParserService.parse(currentTime, "America/Los_Angeles",
				"headache pinned", null, null, baseDate, true), stats)
		stats.finish()
		assert entry.id

		OAuthAccount account = new OAuthAccount([typeId: ThirdParty.TWENTY_THREE_AND_ME, userId: userInstance.id,
				accessToken: "Dummy-token", accessSecret: "Dummy-secret", accountId: userId,
				timeZoneId: TimeZoneId.look("America/Los_Angeles").id])
		Utils.save(account, true)
		assert account.id

		when: "the deleteAccount endpoint is hit and Operation is successful"
		controller.deleteAccount()

		then: "All user data gets deleted, user is marked deleted."
		assert Entry.findByUserId(userInstance.id) == null
		assert OAuthAccount.findByUserId(userInstance.id) == null
		assert Sprint.findByUserId(userInstance.id) == null
		assert userInstance.deleted
		assert controller.response.json.success
	}
}

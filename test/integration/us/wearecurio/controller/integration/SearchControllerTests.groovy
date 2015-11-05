package us.wearecurio.controller.integration
import org.grails.plugins.elasticsearch.ElasticSearchAdminService
import org.grails.plugins.elasticsearch.ElasticSearchService
import org.junit.After
import org.junit.Before
import org.junit.Test
import us.wearecurio.controller.SearchController
import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery

class SearchControllerTests extends CuriousControllerTestCase {

	SearchController controller
	Sprint sprint1, sprint2, sprint3
	User user2, user3
	Discussion discussion1, discussion2
	
	ElasticSearchService elasticSearchService
	def searchService
	ElasticSearchAdminService elasticSearchAdminService
	def elasticSearchHelper
	
	@Before
	void setUp() {
		assert searchService
		searchService.elasticSearchService = elasticSearchService

		elasticSearchHelper.withElasticSearch{ client ->
			client.prepareDeleteByQuery("us.wearecurio.model_v0").setQuery(matchAllQuery()).execute().actionGet()
			client.admin().indices().prepareRefresh().execute().actionGet()
		}
		
		super.setUp()

		controller = new SearchController()
		
		// TO DO: Implement common method in CuriousControllerTestCase to create users 
		Map params = new HashMap()
		params.put("username", "testuser2")
		params.put("email", "test2@test.com")
		params.put("password", "eiajrvaer")
		params.put("first", "first2")
		params.put("last", "last2")
		params.put("sex", "M")
		params.put("location", "New York, NY")
		params.put("birthdate", "12/1/1991")

		user2 = User.create(params)
		user2.settings.makeNamePublic()
		user2.settings.makeBioPublic()
		Utils.save(user2, true)
		
		params.put("username", "testuser3")
		params.put("email", "test3@test.com")

		user3 = User.create(params)
		user3.settings.makeNamePublic()
		user3.settings.makeBioPublic()
		Utils.save(user3, true)
		
		Date now = new Date()
		
		sprint1 = Sprint.create(now, user, "demo1", Visibility.PRIVATE)
		sprint2 = Sprint.create(now, user, "demo2", Visibility.PRIVATE)
		sprint3 = Sprint.create(now, user, "demo3", Visibility.PRIVATE)
		sprint1.addMember(user2.getId())

		discussion1 = Discussion.create(user, "Discussion1 name")
		Utils.save(discussion1, true)
		discussion2 = Discussion.create(user, "Discussion2 name")
		Utils.save(discussion2, true)
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		println ""
		println "user.hash: $user.hash"
		println "user2.hash: $user2.hash"
		println "user3.hash: $user3.hash"
		println ""
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "Test indexData when user is not in session"() {
		controller.getPeopleSocialData(0, 5)

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("auth.error.message", null, null)
	}

	//obsolete
	//@Test
	void "Test indexData when no type is passed"() {
		controller.session.userId = user.getId()
		controller.params["type"] = null
		controller.params["max"] = 5
		controller.params["offset"] = 0
		
		controller.index()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Type"] as Object[] , null)
	}

	//obsolete
	//@Test
	void "Test indexData when wrong type is passed"() {
		controller.session.userId = user.getId()
		controller.params["type"] = "falseType"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.index()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Type"] as Object[] , null)
	}

	@Test
	void "Test indexData when type is people"() {
		controller.session.userId = user.getId()

		controller.getPeopleSocialData(0,5)

		println "json.listItems after index: " + controller.response.json.listItems.toString()
		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 2
		assert controller.response.json.listItems.find{ it.hash == user2.hash && it.type == "usr" }
		assert controller.response.json.listItems.find{ it.hash == user3.hash && it.type == "usr" }
	}

	@Test
	void "Test indexData when type is discussions"() {
		controller.session.userId = user.getId()
		controller.params["type"] = SearchService.DISCUSSION_TYPE
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.getOwnedSocialData(0,5)

		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 2
		assert controller.response.json.listItems.find{ it.hash == discussion1.hash && it.type == "dis" }
		assert controller.response.json.listItems.find{ it.hash == discussion2.hash && it.type == "dis" }
	}

	@Test
	void "Test indexData when type is sprints"() {
		controller.session.userId = user.getId()
		controller.params["type"] = SearchService.SPRINT_TYPE
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.getOwnedSprintData(0,5)

		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 3
		assert controller.response.json.listItems.find{ it.hash == sprint1.hash && it.type == "spr" }
		assert controller.response.json.listItems.find{ it.hash == sprint2.hash && it.type == "spr" }
		assert controller.response.json.listItems.find{ it.hash == sprint3.hash && it.type == "spr" }
	}

	@Test
	void "Test indexData to get all users and discussions"() {
		controller.session.userId = user.getId()

		controller.getAllSocialData(0,10,0)

		println ""
		for( def i : controller.response.json.listItems) {
			println ""
			println i.toString()
		}
		println ""
		
		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 4
		assert controller.response.json.listItems.find{ it.hash == discussion1.hash && it.type == "dis" }
		assert controller.response.json.listItems.find{ it.hash == discussion2.hash && it.type == "dis" }
		assert controller.response.json.listItems.find{ it.hash == user2.hash && it.type == "usr" }
		assert controller.response.json.listItems.find{ it.hash == user3.hash && it.type == "usr" }
	}
}

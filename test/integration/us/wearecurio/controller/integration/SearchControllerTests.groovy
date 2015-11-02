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

		params.put("username", "testuser3")
		params.put("email", "test3@test.com")

		user3 = User.create(params)
		
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
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "Test indexData when user is not in session"() {
		controller.params["type"] = "people"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.feedData()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("auth.error.message", null, null)
	}

	@Test
	void "Test indexData when no type is passed"() {
		controller.session.userId = user.getId()
		controller.params["type"] = null
		controller.params["max"] = 5
		controller.params["offset"] = 0
		
		controller.feedData()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Type"] as Object[] , null)
	}

	@Test
	void "Test indexData when wrong type is passed"() {
		controller.session.userId = user.getId()
		controller.params["type"] = "falseType"
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.feedData()

		assert !controller.response.json.success
		assert controller.response.json.message == messageSource.getMessage("default.blank.message", ["Type"] as Object[] , null)
	}

	@Test
	void "Test indexData when type is people"() {
		controller.session.userId = user.getId()
		controller.params["type"] = SearchService.USER_TYPE
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.feedData()

		println "json.listItems after index: " + controller.response.json.listItems.toString()
		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 2
		assert (controller.response.json.listItems[0].id == user2.id && controller.response.json.listItems[1].id == user3.id) || \
				(controller.response.json.listItems[0].id == user3.id && controller.response.json.listItems[1].id == user2.id)
		assert controller.response.json.listItems[0].type == "usr"
		assert controller.response.json.listItems[1].type == "usr"
	}

	@Test
	void "Test indexData when type is discussions"() {
		controller.session.userId = user.getId()
		controller.params["type"] = SearchService.DISCUSSION_TYPE
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.feedData()

		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 2
		assert controller.response.json.listItems[0].id == discussion2.id ||
				controller.response.json.listItems[0].id == discussion1.id
		assert controller.response.json.listItems[1].id == discussion2.id ||
				controller.response.json.listItems[1].id == discussion1.id
		assert controller.response.json.listItems[0].type == "dis"
		assert controller.response.json.listItems[1].type == "dis"
	}

	@Test
	void "Test indexData when type is sprints"() {
		controller.session.userId = user.getId()
		controller.params["type"] = SearchService.SPRINT_TYPE
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.feedData()

		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 3
		assert controller.response.json.listItems[0].id == sprint1.id ||
				controller.response.json.listItems[0].id == sprint2.id ||
				controller.response.json.listItems[0].id == sprint3.id
		assert controller.response.json.listItems[1].id == sprint1.id ||
				controller.response.json.listItems[1].id == sprint2.id ||
				controller.response.json.listItems[1].id == sprint3.id
		assert controller.response.json.listItems[0].type == "spr"
		assert controller.response.json.listItems[1].type == "spr"
		assert controller.response.json.listItems[2].type == "spr"
	}

	@Test
	void "Test indexData to get all users and discussions"() {
		controller.session.userId = user.getId()
		controller.params["type"] = (SearchService.USER_TYPE | SearchService.DISCUSSION_TYPE)
		controller.params["max"] = 5
		controller.params["offset"] = 0

		controller.feedData()

		println ""
		for( def i : controller.response.json.listItems) {
			println ""
			println i.toString()
		}
		println ""
		
		assert controller.response.json.success
		assert controller.response.json.listItems.size() == 5
		assert controller.response.json.listItems[0].hash == discussion2.hash
		assert controller.response.json.listItems[1].hash == discussion1.hash
		assert controller.response.json.listItems[2].type == "usr"
		assert controller.response.json.listItems[3].type == "usr"
		assert controller.response.json.listItems[4].type == "usr"
	}
}

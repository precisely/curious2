import static org.elasticsearch.index.query.QueryBuilders.*
import static org.elasticsearch.node.NodeBuilder.*
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

import java.text.DateFormat

import org.grails.plugins.elasticsearch.ElasticSearchAdminService
import org.grails.plugins.elasticsearch.ElasticSearchService
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils

@TestMixin(IntegrationTestMixin)
class DiscussionTests extends CuriousTestCase {

	static transactional = true

	DateFormat dateFormat
	Date currentTime
	String timeZone // simulated server time zone
	TimeZoneId timeZoneId
	DateTimeZone dateTimeZone
	Date baseDate
	UserGroup testGroup
	
	ElasticSearchService elasticSearchService
	ElasticSearchAdminService elasticSearchAdminService
	def elasticSearchHelper

	@Before
	void setUp() {
		super.setUp()
		
		elasticSearchHelper.withElasticSearch{ client ->
			client.prepareDeleteByQuery("us.wearecurio.model_v0").setQuery(matchAllQuery()).execute().actionGet()
			client.admin().indices().prepareRefresh().execute().actionGet()
		}
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		timeZone = "America/Los_Angeles"
		timeZoneId = TimeZoneId.look(timeZone)
		dateTimeZone = timeZoneId.toDateTimeZone()
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		
		testGroup = UserGroup.create("group", "Curious Group", "Discussion topics for test users",
				[isReadOnly:false, defaultNotify:false])
		testGroup.addWriter(user)
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreateDiscussionInGroups() {
		Thread.sleep(1000)
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
			[isReadOnly:false, defaultNotify:false])
		groupA.addMember(user)
		Thread.sleep(1000)
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
			[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user)

		def userGroups = UserGroup.getConcreteGroupsForWriter(user)
		assert userGroups[0].id == groupB.id
		assert userGroups[1].id == groupA.id
		
		Thread.sleep(1000)
		
		Discussion discussion = Discussion.create(user, "Topic name", groupA, null)
		groupB.addDiscussion(discussion)
		
		userGroups = UserGroup.getConcreteGroupsForWriter(user)
		assert userGroups[1].id == groupB.id
		assert userGroups[0].id == groupA.id
		
		DiscussionPost post = discussion.createPost(user, "Test post")
		
		Utils.save(discussion, true)
		Utils.save(post, true)
		
		def groupIds = discussion.groupIds
		
		elasticSearchService.index()
		
		Thread.sleep(2000)
		
		// test elastic search here		
		def results = Discussion.search(searchType:'query_and_fetch') {
		  bool {
		      must {
				  query_string(query: "name:name bar")
		          //query_string(query: "groupIds:5")
		      }
	          //must {
	          //    term(name: "name")
	          //}
		  }
		}
		
		/* QueryBuilder query = QueryBuilders.termQuery("name", "Topic name")
		
		def results = elasticSearchService.search(query)*/
		
		System.out.println "RESULT SIZE: " + results.searchResults.size()
		//assert results.searchResults[0].id == discussion.id
	}

/*	@Test
	void testCreateDiscussion() {
		Discussion discussion = Discussion.create(user, "testCreateDiscussion", testGroup, null)
		DiscussionPost post = discussion.createPost(user, "Test post")
		
		post = post
		
		elasticSearchService.index()	
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		// test elastic search here
		def results = elasticSearchService.search(searchType:'query_and_fetch') {
		  query_string(query: "name: " + discussion.name)
		}
		
		assert results.searchResults[0].id == discussion.id
		assert results.searchResults[0].name == discussion.name
	}

	@Test
	void testIndexDiscussionId() {
		Discussion discussion1 = Discussion.create(user, "testIndexDiscussionId1", null, currentTime)
		Discussion discussion2 = Discussion.create(user, "testIndexDiscussionId2", null, currentTime + 1)
		Discussion discussion3 = Discussion.create(user, "testIndexDiscussionId3", null, currentTime + 2)

		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def name1Orig = discussion1.name
		def name3Orig = discussion3.name
		
		discussion1.name += "newstuff"		
		discussion2.name += "newstuff"		
		discussion3.name += "newstuff"
			
		elasticSearchService.index(discussion2)
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		def results = Discussion.search(searchType:'query_and_fetch') {
			ids ( values: [discussion1.id, discussion2.id, discussion3.id] )
		}

		assert results.searchResults.size() == 3
		assert results.searchResults.find{ 
			it.id == discussion1.id &&
			it.name == discussion1.name
		} == null
		assert results.searchResults.find{ 
			it.id == discussion1.id &&
			it.name == name1Orig
		}
		assert results.searchResults.find{
			it.id == discussion2.id &&
			it.name == discussion2.name
		}
		assert results.searchResults.find{
			it.id == discussion3.id &&
			it.name == discussion1.name
		} == null
		assert results.searchResults.find{
			it.id == discussion3.id &&
			it.name == name3Orig
		}
	}
	
	@Test
	void "test getFollowupPosts when there is first post but that is not associated with any graph"() {
		Discussion discussion = Discussion.create(user, "First discussion", testGroup, null)
		DiscussionPost post1 = discussion.createPost(user, "Post 1")
		DiscussionPost post2 = discussion.createPost(user, "Post 2")
		DiscussionPost post3 = discussion.createPost(user, "Post 3")

		assert discussion.firstPostId == post1.id

		List<DiscussionPost> posts = discussion.getFollowupPosts([max: 2])
		assert posts*.id == [post1.id, post2.id]

		Map discussionModel = discussion.getJSONDesc()
		assert discussionModel.totalPostCount == 3		// Including first post
		assert discussionModel.firstPost != null
		assert discussionModel.firstPost.id == post1.id
	}

	@Test
	void "test getFollowupPosts when there is a first post which is associated with a graph or plot ID"() {
		Discussion discussion = Discussion.create(user, "First discussion", testGroup, null)
		DiscussionPost post1 = DiscussionPost.createComment("Post 1", user, discussion, 1, [:])
		DiscussionPost post2 = discussion.createPost(user, "Post 2")
		DiscussionPost post3 = discussion.createPost(user, "Post 3")

		assert discussion.firstPostId == post1.id

		List<DiscussionPost> posts = discussion.getFollowupPosts([max: 2])
		assert posts*.id == [post2.id, post3.id]

		Map discussionModel = discussion.getJSONDesc()
		assert discussionModel.totalPostCount == 2		// Excluding first post
		assert discussionModel.firstPost != null
		assert discussionModel.firstPost.id == post1.id
	}
/**/
}
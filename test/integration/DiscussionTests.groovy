import static org.junit.Assert.*

import java.math.MathContext
import java.text.DateFormat

import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.grails.plugins.elasticsearch.ElasticSearchService
import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.TagValueStats;
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.UserGroup;
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.services.DatabaseService
import groovy.transform.TypeChecked

import org.joda.time.DateTimeZone
import org.junit.*
import org.joda.time.*

import grails.test.mixin.*
import us.wearecurio.utility.Utils
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

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
	
	@Before
	void setUp() {
		super.setUp()
		
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		Utils.resetForTesting()
		
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
	void testCreateDiscussion() {
		Discussion discussion = Discussion.create(user, "testCreateDiscussion", testGroup)
		DiscussionPost post = discussion.createPost(user, "Test post")
		
		post = post
		
		elasticSearchService.index()		
		Thread.sleep(2000)
		
		// test elastic search here
		def results = elasticSearchService.search(searchType:'query_and_fetch') {
		  query_string(query: "name: " + discussion.name)
		}
		
		assert results.searchResults[0].id == discussion.id
		assert results.searchResults[0].name == discussion.name
	}

	@Test
	void testIndexDiscussionId() {
		Discussion discussion1 = Discussion.create(user, "testIndexDiscussionId1")
		Thread.sleep(1000)
		Discussion discussion2 = Discussion.create(user, "testIndexDiscussionId2")
		Thread.sleep(1000)
		Discussion discussion3 = Discussion.create(user, "testIndexDiscussionId3")
		
		def id1 = 38
		def id2 = 93
		def id3 = 4397
		
		discussion1.id = id1
		discussion2.id = id2
		discussion3.id = id3
		
		elasticSearchService.index(discussion2)
		Thread.sleep(1000)

		def results = elasticSearchService.search(searchType:'query_and_fetch') {
			ids ( values: [id1, id2, id3] )
		}

		assert results.searchResults.size() == 1
		assert results.searchResults.find{ it.id == id1 } == null
		assert results.searchResults.find{ it.id == id2 }
		assert results.searchResults.find{ it.id == id2 }.id == id2
		assert results.searchResults.find{ it.id == id2 }.getId() == id2
		assert results.searchResults.find{ it.id == id2 }.name == discussion2.name
		assert results.searchResults.find{ it.id == id3 } == null
				
		discussion2.id = id3 //changing id has no effect on existing index
		
		elasticSearchService.index(discussion3)
		Thread.sleep(1000)

		results = elasticSearchService.search(searchType:'query_and_fetch') {
			ids ( values: [id1, id2, id3] )
		}

		assert results.searchResults.size() == 2
		assert results.searchResults.find{ it.id == id1 } == null
		assert results.searchResults.find{ it.id == id2 }
		assert results.searchResults.find{ it.id == id2 }.id == id2
		assert results.searchResults.find{ it.id == id2 }.getId() == id2
		assert results.searchResults.find{ it.id == id2 }.name == discussion2.name
		assert results.searchResults.find{ it.id == id3 }
		assert results.searchResults.find{ it.id == id3 }.id == id3
		assert results.searchResults.find{ it.id == id3 }.getId() == id3
		assert results.searchResults.find{ it.id == id3 }.name == discussion3.name
		
		discussion3.id = id1 //changing id has no effect on files already indexed
		
		elasticSearchService.index(discussion1)
		Thread.sleep(1000)

		results = elasticSearchService.search(searchType:'query_and_fetch') {
			ids ( values: [id1, id2, id3] )
		}

		assert results.searchResults.size() == 3
		assert results.searchResults.find{ it.id == id1 }
		assert results.searchResults.find{ it.id == id1 }.id == id1
		assert results.searchResults.find{ it.id == id1 }.getId() == id1
		assert results.searchResults.find{ it.id == id1 }.name == discussion1.name
		assert results.searchResults.find{ it.id == id2 }
		assert results.searchResults.find{ it.id == id2 }.id == id2
		assert results.searchResults.find{ it.id == id2 }.getId() == id2
		assert results.searchResults.find{ it.id == id2 }.name == discussion2.name
		assert results.searchResults.find{ it.id == id3 }
		assert results.searchResults.find{ it.id == id3 }.id == id3
		assert results.searchResults.find{ it.id == id3 }.getId() == id3
		assert results.searchResults.find{ it.id == id3 }.name == discussion3.name
	}
	
	@Test
	void testCreateDiscussionInGroups() {
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
			[isReadOnly:false, defaultNotify:false])
		groupA.addMember(user)
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
			[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user)

		Discussion discussion = Discussion.create(user, "Topic name", groupA)
		groupB.addDiscussion(discussion)
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
}

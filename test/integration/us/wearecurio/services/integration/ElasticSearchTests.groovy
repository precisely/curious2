package us.wearecurio.services.integration

import grails.test.mixin.integration.Integration
import java.text.DateFormat
import org.elasticsearch.action.count.CountResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.grails.plugins.elasticsearch.ElasticSearchAdminService
import org.grails.plugins.elasticsearch.ElasticSearchService
import org.joda.time.DateTimeZone
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Entry
import us.wearecurio.model.GroupMemberAdmin
import us.wearecurio.model.GroupMemberDiscussion
import us.wearecurio.model.GroupMemberReader
import us.wearecurio.model.GroupMemberWriter
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils

import static org.elasticsearch.index.query.QueryBuilders.boolQuery
import static org.elasticsearch.index.query.QueryBuilders.filtered
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery
import static org.elasticsearch.index.query.QueryBuilders.matchQuery
import static org.elasticsearch.index.query.QueryBuilders.queryString
import static org.elasticsearch.index.query.QueryBuilders.termQuery

@Integration
class ElasticSearchTests extends CuriousServiceTestCase {
	static transactional = false
	
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
	
	void setup() {
		//NOTE: Cannot use elasticSearchAdminService.deleteIndex() as that removes index entirely and an IndexMissingException is thrown
		//the next time an ES search is attempted. Instead, do a "delete by  query" to remove all data in all indexes, while keeping the
		//indexes themselves.  Could have used, elasticSearchAdminService to do the refresh, but since have client anyhow, chose to do use
		//prepareRefresh with closure client.
		elasticSearchHelper.withElasticSearch{ client ->
			client.prepareDeleteByQuery("us.wearecurio.model_v0").setQuery(matchAllQuery()).execute().actionGet()
			client.admin().indices().prepareRefresh().execute().actionGet()
		}
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		GroupMemberReader.executeUpdate("delete GroupMemberReader r")
		UserGroup.executeUpdate("delete UserGroup g")

		setupTestData()

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
	
	void cleanup() {
        for (GroupMemberReader o in GroupMemberReader.list()) {
            o.delete(flush:true)
        }
        for (GroupMemberWriter o in GroupMemberWriter.list()) {
            o.delete(flush:true)
        }
        for (GroupMemberAdmin o in GroupMemberAdmin.list()) {
            o.delete(flush:true)
        }
        for (GroupMemberDiscussion o in GroupMemberDiscussion.list()) {
            o.delete(flush:true)
        }
        for (UserGroup o in UserGroup.list()) {
            o.delete(flush:true)
        }
        for (DiscussionPost o in DiscussionPost.list()) {
            o.delete(flush:true)
        }
        for (Discussion o in Discussion.list()) {
            o.delete(flush:true)
        }
        for (Sprint o in Sprint.list()) {
            o.delete(flush:true)
        }
        for (User o in User.list()) {
            o.delete(flush:true)
        }
        for (Entry o in Entry.list()) {
            o.delete(flush:true)
        }
        for (Tag o in Tag.list()) {
            o.delete(flush:true)
        }
		GroupMemberReader.executeUpdate("delete GroupMemberReader r")
		UserGroup.executeUpdate("delete UserGroup g")
		
		elasticSearchService.index()
		
		elasticSearchHelper.withElasticSearch{ client ->
			client.prepareDeleteByQuery("us.wearecurio.model_v0").setQuery(matchAllQuery()).execute().actionGet()
			client.admin().indices().prepareRefresh().execute().actionGet()
		}
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
	}
	
	private void testSimpleSearch(Object container, String fieldName) {
		testSimpleSearch(container, fieldName, null)
	}
	
	private void testSimpleSearch(Object container, String fieldName, String extraFieldNameToCheck) {
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		String fieldValue
		def val = container[fieldName]
		if (Date.class.isAssignableFrom(val.class) ) {
			fieldValue = Utils.elasticSearchDate(val).replace(":","\\:")
		} else {
			fieldValue = val.toString()
		}
		
		def results = container.search(searchType:'query_and_fetch') {
			query_string(query: fieldName + ":" + fieldValue)
		}
		
		assert results.searchResults
		assert results.searchResults.size() == 1
		if (extraFieldNameToCheck) {
			assert results.searchResults[0][extraFieldNameToCheck] == container[extraFieldNameToCheck]
		}
		
		System.out.println "container.id: " + container.id + " container.getId(): " + container.getId() + " results.searchResults[0].id: " + results.searchResults[0].id + " results.searchResults[0].getId(): " + results.searchResults[0].getId()
	}
	
	void testSimplePostSearch(String fieldName) {
		testSimplePostSearch(fieldName, null)
	}
	
	void testSimplePostSearch(String fieldName, Object fieldValue) {
		Discussion discussion = Discussion.create(user)
		DiscussionPost post = discussion.createPost(user, "Test post")
		
		if (fieldValue) {
			java.lang.reflect.Field f = post.class.getDeclaredField(fieldName)
			f.setAccessible(true)
			f.set(post, fieldValue.class.cast(fieldValue))
		}
		
		Utils.save(post, true)
		
		testSimpleSearch(post, fieldName, "message")
	}

	void "Test Get Discussion Post Info for User Belonging to One Group with One Discussion"() {
		given:
		def groups = UserGroup.list()
		def groups2 = GroupMemberReader.list()
		
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addMember(user)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user2)
		
		Discussion discussionA = Discussion.create(user, "groupA discussion", groupA, currentTime - 2)
		Discussion discussionB = Discussion.create(user2, "groupB discussion", groupB, currentTime - 1)
		assert discussionA
		assert discussionB
		
		DiscussionPost postA1 = discussionA.createPost(user, "postA1", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "postA2", currentTime + 1)
		DiscussionPost postB1 = discussionB.createPost(user2, "postB1", currentTime + 2)
		DiscussionPost postB2 = discussionB.createPost(user2, "postB2", currentTime + 3)
		
		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then:
		def dbGroupIds = GroupMemberReader.lookupGroupIds(userId)
		assert dbGroupIds.size() == 3
		assert dbGroupIds.contains(user.virtualUserGroupIdDiscussions)
        assert dbGroupIds.contains(discussionA.virtualUserGroupIdFollowers)
		assert dbGroupIds.contains(groupA.id)
        assert !dbGroupIds.contains(discussionB.virtualUserGroupIdFollowers)
		assert !dbGroupIds.contains(groupB.id)

		when:
		def discussionResults = Discussion.search(searchType:'query_and_fetch') {
			query_string(query:  "groupIds:" + dbGroupIds[0].toString())
		}

		then:
		assert discussionResults
		assert discussionResults.searchResults.size() == 1
		assert discussionResults.searchResults[0].name == discussionA.name

		when:
		SearchResponse sr
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(
					boolQuery()
					.must(
					queryString("discussionId:" + discussionResults.searchResults[0].id.toString()))
					.mustNot(
					termQuery("flags",DiscussionPost.FIRST_POST_BIT.toString())
					)
					)
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.setSize(1)  // number of post documents to show PER discussion id
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()
		}

		then:
		def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
		assert bucket.docCount == 1

		def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
		assert hits
		assert hits.size() == 1
		assert hits[0].getSource().message == postA2.message // second post to discussionA
	}

	void "Test Search Discussion by userId"() {
		expect:
		testSimpleSearch(Discussion.create(user), "userId")
	}

	void "Test Search Discussion by firstPostId"() {
		given:
		Discussion discussion = Discussion.create(user)
		DiscussionPost post = discussion.createPost(user, "Test post")

		Utils.save(post, true)
		expect:
		testSimpleSearch(discussion, "firstPostId", null)
	}

	void "Test Search Discussion by name"() {
		expect:
		testSimpleSearch(Discussion.create(user, "TestSearchDiscussionByName"), "name", "name")
	}

	void "Test Search Discussion by created"() {
		expect:
		testSimpleSearch(Discussion.create(user, "TestSearchDiscussionByCreated"), "created", "name")
	}

	void "Test Search Discussion by updated"() {
		expect:
		testSimpleSearch(Discussion.create(user, "TestSearchDiscussionByUpdated"), "updated", "name")
	}

	void "Test Search Discussion by visibility"() {
		expect:
		testSimpleSearch(Discussion.create(user, "TestSearchDiscussionByVisibility"), "visibility", "name")
	}

	void "Test Search Discussion by groupIds"() {
		given:
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addMember(user)
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user)
		
		Discussion discussion = Discussion.create(user, "TestSearchDiscussionByBroupIds", groupA)
		groupB.addDiscussion(discussion)

		when:
		elasticSearchService.index()
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch') {
			bool {
				must {
					query_string(query: "groupIds:(" + groupA.id + " OR " + groupB.id + ")")
				}
			}
		}

		then:
		assert results.searchResults
		assert results.searchResults.size() == 1
		assert results.searchResults[0].name == discussion.name
	}

	void "Test Search Discussion Post by discussionId"() {
		expect:
		testSimplePostSearch("discussionId")
	}

	void "Test Search Discussion Post by authorUserId"() {
		expect:
		testSimplePostSearch("authorUserId")
	}

	void "Test Search Discussion Post by created"() {
		testSimplePostSearch("created")
	}

	void "Test Search Discussion Post by updated"() {
		expect:
		testSimplePostSearch("updated")
	}

	void "Test Search Discussion Post by plotDataId"() {
		expect:
		testSimplePostSearch("plotDataId", 100L)
	}

	void "Test Search Discussion Post by message"() {
		expect:
		testSimplePostSearch("message", "Test message")
	}

	void "Test Search Discussions Created in Past Week"() {
		given:
		Discussion discussionRecent = Discussion.create(user, "TestSearchDiscussionsCreatedinPastWeek")
		Discussion discussionRecent2 = Discussion.create(user, "TestSearchDiscussionsCreatedinPastWeek 2")
		Discussion discussionOld = Discussion.create(user, "TestSearchDiscussionsCreatedinPastWeek old")
		
		use (groovy.time.TimeCategory){
			discussionOld.created = (new Date()) - 8.days
			discussionRecent2.created = (new Date()) -  6.days
		}
		
		Utils.save(discussionOld, true)
		Utils.save(discussionRecent, true)
		Utils.save(discussionRecent2, true)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch') {
			filtered {
				query { query_string(query: "name:TestSearchDiscussionsCreatedinPastWeek") }
				filter {
					range{ created(gte: "now-7d/d") }
				}
			}
		}
		
		System.out.println "size: " + results.searchResults.size()

		then:
		assert results
		assert results.searchResults.size() == 2
		assert results.searchResults.find { d -> d.name == discussionRecent.name }
		assert results.searchResults.find { d -> d.name == discussionRecent2.name }
	}

	void "Test Search Discussion Posts Created in Past Week"() {
		given:
		Discussion discussion = Discussion.create(user, "test")
		DiscussionPost postOld = discussion.createPost(user, "TestSearchDiscussionPostsCreatedInPastWeek old")
		DiscussionPost postRecent = discussion.createPost(user, "TestSearchDiscussionPostsCreatedInPastWeek recent")
		
		use (groovy.time.TimeCategory){
			postOld.created = (new Date()) - 8.days
		}
		
		Utils.save(postOld, true)
		Utils.save(postRecent, true)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = DiscussionPost.search(searchType:'query_and_fetch') {
			filtered {
				query { query_string(query: "message:TestSearchDiscussionPostsCreatedInPastWeek") }
				filter {
					range{ created(gte: "now-7d/d") }
				}
			}
		}
		
		System.out.println "size: " + results.searchResults.size()

		then:
		assert results
		assert results.searchResults.size() == 1
		assert results.searchResults[0].message ==  postRecent.message
	}

	void "Test Search Discussions Updated in Past Week"() {
		given:
		Discussion discussion = Discussion.create(user, "TestSearchDiscussionsUpdatedInPastWeek")
		DiscussionPost post = discussion.createPost(user, "Test post")
		
		Utils.save(post, true)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		Date dt1, dt2
		use (groovy.time.TimeCategory){
			dt1 = discussion.updated - 4.days
			dt2 = discussion.updated + 3.days
		}
		
		String dts1 = Utils.elasticSearchDate(dt1)
		String dts2 = Utils.elasticSearchDate(dt2)
		
		def results = Discussion.search(searchType:'query_and_fetch') {
			range {
				updated(gte:dts1,lte:dts2)
			}
		}

		then:
		assert results.searchResults
		assert results.searchResults.size() == 1
		assert results.searchResults[0].name == discussion.name
	}

	void "Test Search Discussions Using List of Keywords"() {
		given:
		Discussion discussion1 = Discussion.create(user, "word1", null, currentTime - 6)
		Discussion discussion2 = Discussion.create(user, "word2", null, currentTime - 5)
		Discussion discussion3 = Discussion.create(user, "word3", null, currentTime - 4)
		Discussion discussion4 = Discussion.create(user, "word1 word2", null, currentTime - 3)
		Discussion discussion5 = Discussion.create(user, "word1 word3", null, currentTime - 2)
		Discussion discussion6 = Discussion.create(user, "word2 word3", null, currentTime - 1)
		Discussion discussion7 = Discussion.create(user, "word1 word2 word3", null, currentTime)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch') { query_string(query: "name:(word3 OR word1 OR word2)") }

		then:
		assert results.searchResults
		assert results.searchResults.size() == 7
		assert results.searchResults.find { d -> d.name == discussion1.name }
		assert results.searchResults.find { d -> d.name == discussion2.name }
		assert results.searchResults.find { d -> d.name == discussion3.name }
		assert results.searchResults.find { d -> d.name == discussion4.name }
		assert results.searchResults.find { d -> d.name == discussion5.name }
		assert results.searchResults.find { d -> d.name == discussion6.name }
		assert results.searchResults.find { d -> d.name == discussion7.name }
		
		System.out.println ""
		System.out.println "keys returned:"
		
		for (String s : results.keySet()) {
			System.out.println s
		}
		
		System.out.println ""
		System.out.println "checking for scores"
		
		if (results.containsKey("scores")) {
			System.out.println "Scores: "
			for (def score: results.scores) {
				System.out.println score
			}
		} else {
			System.out.println "scores not included in the returned map"
		}
	}

	void "Test Search Discussions Using Exact Phrase"() {
		given:
		Discussion discussion1 = Discussion.create(user, "word1 word2 word3", null, currentTime - 2)
		Discussion discussion2 = Discussion.create(user, "word3 word2 word1", null, currentTime - 1)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch') { match_phrase(name:"word1 word2 word3") }

		then:
		assert results.searchResults
		assert results.searchResults.size() == 1
		assert results.searchResults[0].name == discussion1.name
	}

	void "Test Search Discussion in Order of Most Hits"() {
		given:
		Discussion discussion1 = Discussion.create(user, "word1 word2", null, currentTime - 2)
		Discussion discussion2 = Discussion.create(user, "word1 word2 word3", null, currentTime - 1)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch') { query_string(query: "name:(word3 OR word1)") }

		then:
		assert results.searchResults
		assert results.searchResults.size() == 2
		assert results.searchResults[0].name == discussion2.name
		assert results.searchResults[1].name == discussion1.name
	}

	void "Test Search Discussions Multiple Results in Chronological order"() {
		given:
		Discussion discussion1 = Discussion.create(user, "ChonologicalOrderAscTest 1", null, currentTime)
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		Discussion discussion2 = Discussion.create(user, "ChonologicalOrderAscTest 2", null, currentTime + 1)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch', sort:'created', order:'asc') { query_string(query: "name:ChonologicalOrderAscTest") }

		then:
		assert results.searchResults
		assert results.searchResults.size() == 2
		assert results.searchResults[0].name == discussion1.name
		assert results.searchResults[1].name == discussion2.name
	}

	void "Test Search Discussions Multiple Results in Reverse Chronological order"() {
		given:
		Discussion discussion1 = Discussion.create(user, "ChonologicalOrderDescTest 1", null, currentTime)
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		Discussion discussion2 = Discussion.create(user, "ChonologicalOrderDescTest 2", null, currentTime + 1)
		
		System.out.println "discussion1.creation: " + discussion1.created
		System.out.println "discussion2.creation: " + discussion2.created

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch', sort:'created', order:'desc') { query_string(query: "name:ChonologicalOrderDescTest") }

		then:
		//results in chronological order by default
		assert results.searchResults
		assert results.searchResults.size() == 2
		assert results.searchResults[0].name == discussion2.name
		assert results.searchResults[1].name == discussion1.name
	}

	void "Test Search Discussions Match Two Keywords"() {
		given:
		Discussion discussion1 = Discussion.create(user, "word1 word2 word3", null, currentTime)
		Discussion discussion2 = Discussion.create(user, "word1 word2", null, currentTime + 1)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch') { query_string(query: "name:(word3 AND word1)") }

		then:
		assert results.searchResults
		assert results.searchResults.size() == 1
		assert results.searchResults[0].name == discussion1.name
	}

	void "Test Search Discussions Match Three Keywords"() {
		given:
		Discussion discussion1 = Discussion.create(user, "word1 word2 word3", null, currentTime)
		Discussion discussion2 = Discussion.create(user, "word1 word2", null, currentTime + 1)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def results = Discussion.search(searchType:'query_and_fetch') { query_string(query: "name:(word3 AND word1 AND word2)") }

		then:
		assert results.searchResults
		assert results.searchResults.size() == 1
		assert results.searchResults[0].name == discussion1.name
	}

	void "Test Scoring Function"() {
		given:
		Discussion discussion1 = Discussion.create(user, "TestScoringFunction word2 word3", null, currentTime)
		Discussion discussion2 = Discussion.create(user, "TestScoringFunction word4 word5", null, currentTime + 1)
		
		discussion1.firstPostId = 30
		discussion2.firstPostId = 0
		
		Utils.save(discussion1, true)
		Utils.save(discussion2, true)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		FunctionScoreQueryBuilder fsqb = functionScoreQuery(matchQuery("name","TestScoringFunction"))
		fsqb.add(ScoreFunctionBuilders.linearDecayFunction("firstPostId", 0, 20))

		SearchResponse sr
		elasticSearchHelper.withElasticSearch { client ->
			sr = client.prepareSearch("us.wearecurio.model_v0").setTypes("discussion").setQuery(fsqb).execute().actionGet()
		}

		then:
		assert sr
		assert sr.getHits()
		if (sr.getHits().getTotalHits() == 0) System.out.println "no hits found!"
		for( SearchHit hit : sr.getHits().getHits() ) {
			System.out.println "hit: " + hit.getIndex() + ": " + hit.getType() + ": " + hit.getId() + " (score: " + hit.getScore() + ")"
		}
		assert (sr.getHits().getHits()[0].getSource().name == discussion2.name)
		assert (sr.getHits().getHits()[1].getSource().name == discussion1.name)
		assert (sr.getHits().getHits()[0].getScore() > sr.getHits().getHits()[1].getScore())
	}

	void "Test Search Discussion For Which User Is a Reader"() {
		given:
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addMember(user)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user2)
		
		Discussion discussionReadByUser = Discussion.create(user, "groupA discussion", groupA, currentTime - 2)
		Discussion discussionNotReadByUser = Discussion.create(user2, "groupB discussion", groupB, currentTime - 1)
		
		DiscussionPost postA = discussionReadByUser.createPost(user, "postA", currentTime)
		DiscussionPost postB = discussionNotReadByUser.createPost(user, "postB", currentTime + 1)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then:
		def dbGroupIds = GroupMemberReader.lookupGroupIds(userId)
		assert dbGroupIds.size() == 3
		assert dbGroupIds.contains(user.virtualUserGroupIdDiscussions)
        assert dbGroupIds.contains(discussionReadByUser.virtualUserGroupIdFollowers)
		assert dbGroupIds.contains(groupA.id)
        assert !dbGroupIds.contains(discussionNotReadByUser.virtualUserGroupIdFollowers)
		assert !dbGroupIds.contains(groupB.id)
		
		def discussionResults = Discussion.search(searchType:'query_and_fetch') {
			query_string(query: "groupIds:" + dbGroupIds[0].toString())
		}
		assert discussionResults
		assert discussionResults.searchResults.size() == 1
		assert discussionResults.searchResults[0].name == discussionReadByUser.name
		
		def discussionPostResults = DiscussionPost.search(searchType:'query_and_fetch') {
			query_string(query: "discussionId:" + discussionResults.searchResults[0].id.toString() + " AND message:" + postA.message)
		}
		
		assert discussionPostResults
		assert discussionPostResults.searchResults.size() == 1
		assert discussionPostResults.searchResults[0].message == postA.message

		when:
		discussionPostResults = DiscussionPost.search(searchType:'query_and_fetch') {
			query_string(query: "discussionId:" + discussionResults.searchResults[0].id.toString() + " AND message:" + postB.message)
		}

		then:
		assert discussionPostResults
		assert discussionPostResults.searchResults.size() == 0
	}

	void "Test Search Discussion Post Counts"() {
		given:
		Discussion discussionA = Discussion.create(user, "groupA discussion", null, currentTime - 2)
		Discussion discussionB = Discussion.create(user, "groupB discussion", null, currentTime - 1)
		
		DiscussionPost postA1 = discussionA.createPost(user, "post1 for discussion A", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "post2 for discussion A", currentTime + 1)
		DiscussionPost postB1 = discussionB.createPost(user, "post1 for discussion B", currentTime + 2)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		CountResponse cr
		elasticSearchHelper.withElasticSearch{ client ->
			cr = client
					.prepareCount("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(queryString("discussionId:(" + discussionA.id + " OR " + discussionB.id + ")"))
					.execute()
					.actionGet()
		}

		then:
		assert cr.count == 3

		when:
		SearchResponse sr
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(matchAllQuery())
					.setSize(0)   // prevent documents from showing up; only interested in count stats
					.addAggregation(AggregationBuilders.terms("by_discussionId").field("discussionId"))
					.execute()
					.actionGet()
		}

		then:
		assert sr
		assert sr.getHits()
		System.out.println "Total Hits: " + sr.getHits().getTotalHits()
		assert sr.getHits().getHits().size() == 0
		assert sr.getAggregations()
		assert sr.getAggregations().get("by_discussionId")
		assert sr.getAggregations().get("by_discussionId").getBuckets()
		assert sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }.docCount == 2
		assert sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }.docCount == 1

		DiscussionPost postB2 = discussionB.createPost(user, "post2 for discussion B", currentTime + 3)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		elasticSearchHelper.withElasticSearch{ client ->
			cr = client
					.prepareCount("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(queryString("discussionId:(" + discussionA.id + " OR " + discussionB.id + ")"))
					.execute()
					.actionGet()
		}

		then:
		assert cr.count == 4

		when:
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(matchAllQuery())
					.setSize(0)   // prevent documents from showing up; only interested in count stats
					.addAggregation(AggregationBuilders.terms("by_discussionId").field("discussionId"))
					.execute()
					.actionGet()
		}

		then:
		assert sr
		System.out.println "Total Hits: " + sr.getHits().getTotalHits()
		assert sr.getHits().getHits().size() == 0
		assert sr.getAggregations()
		assert sr.getAggregations().get("by_discussionId")
		assert sr.getAggregations().get("by_discussionId").getBuckets()
		assert sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }.docCount == 2
		assert sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }.docCount == 2
	}

	void "Test Group Discussion Posts and Counts"() {
		given:
		Discussion discussionA = Discussion.create(user, "discussion A", null, currentTime - 2)
		Discussion discussionB = Discussion.create(user, "discussion B", null, currentTime - 1)
		
		DiscussionPost postA1 = discussionA.createPost(user, "post1 for discussion A", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "post2 for discussion A", currentTime + 1)
		DiscussionPost postB1 = discussionB.createPost(user, "post1 for discussion B", currentTime + 2)
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		elasticSearchHelper.withElasticSearch{ client ->
			SearchResponse sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(matchAllQuery())
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.setSize(1)  // number of post documents to show PER discussion id
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()
			
			assert sr
			assert sr.getAggregations()
			assert sr.getAggregations().get("by_discussionId")
			assert sr.getAggregations().get("by_discussionId").getBuckets()
			
			//DiscussionA has 2 posts, but only 1st hit should show up
			def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
			assert bucket
			assert bucket.docCount == 2
			def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
			assert hits
			assert hits.size() == 1
			def hit = hits[0]
			assert hit
			assert hit.getSource()
			assert hit.getSource().message == postA1.message
			
			//DiscussionB has 1 post
			bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
			assert bucket
			assert bucket.docCount == 1
			hits = bucket.getAggregations().get("top_hits").getHits().getHits()
			assert hits
			assert hits.size() == 1
			hit = hits[0]
			assert hit
			assert hit.getSource()
			assert hit.getSource().message == postB1.message
			
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(matchAllQuery())
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					.setSize(2)  // number of post documents to show PER discussion id
					)
					)
					.execute()
					.actionGet()
			
			assert sr
			assert sr.getAggregations()
			assert sr.getAggregations().get("by_discussionId")
			assert sr.getAggregations().get("by_discussionId").getBuckets()
			
			//DiscussionA has 2 posts and both should now show up
			bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
			assert bucket
			assert bucket.docCount == 2
			hits = bucket.getAggregations().get("top_hits").getHits().getHits()
			assert hits
			assert hits.size() == 2
			assert hits.find{ h -> h.getSource().message == postA1.message }
			assert hits.find{ h -> h.getSource().message == postA2.message }
			
			//DiscussionB still only has 1 post
			bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
			assert bucket
			assert bucket.docCount == 1
			hits = bucket.getAggregations().get("top_hits").getHits().getHits()
			assert hits
			assert hits.size() == 1
			hit = hits[0]
			assert hit
			assert hit.getSource()
			assert hit.getSource().message == postB1.message
			
			DiscussionPost postB2 = discussionB.createPost(user, "post2 for discussion B")
			
			elasticSearchService.index()
			elasticSearchAdminService.refresh("us.wearecurio.model_v0")
			
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(matchAllQuery())
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					.setSize(2)  // number of post documents to show PER discussion id
					)
					)
					.execute()
					.actionGet()
			
			assert sr
			assert sr.getAggregations()
			assert sr.getAggregations().get("by_discussionId")
			assert sr.getAggregations().get("by_discussionId").getBuckets()
			
			//DiscussionA still has 2 posts and both should still show up
			bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
			assert bucket
			assert bucket.docCount == 2
			hits = bucket.getAggregations().get("top_hits").getHits().getHits()
			assert hits
			assert hits.size() == 2
			assert hits.find{ h -> h.getSource().message == postA1.message }
			assert hits.find{ h -> h.getSource().message == postA2.message }
			
			//DiscussionB now has 2 posts and both should show up
			bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
			assert bucket
			assert bucket.docCount == 2
			hits = bucket.getAggregations().get("top_hits").getHits().getHits()
			assert hits
			assert hits.size() == 2
			assert hits.find{ h -> h.getSource().message == postB1.message }
			assert hits.find{ h -> h.getSource().message == postB2.message }
		}
	}

	void "Test Sort Order of Discussion Posts"() {
		given:
		Discussion discussionA = Discussion.create(user, "discussion A", null, currentTime - 2)
		Discussion discussionB = Discussion.create(user, "discussion B", null, currentTime - 1)
		
		DiscussionPost postA1 = discussionA.createPost(user, "post1 for discussion A", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "post2 for discussion A", currentTime + 1)
		DiscussionPost postA3 = discussionA.createPost(user, "post3 for discussion A", currentTime + 2)
		DiscussionPost postB1 = discussionB.createPost(user, "post1 for discussion B", currentTime + 3)
		DiscussionPost postB2 = discussionB.createPost(user, "post2 for discussion B", currentTime + 4)
		DiscussionPost postB3 = discussionB.createPost(user, "post3 for discussion B", currentTime + 5)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		SearchResponse sr1
		SearchResponse sr2
		elasticSearchHelper.withElasticSearch { client ->
			sr1 = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(matchAllQuery())
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()

			sr2 = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(matchAllQuery())
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.DESC))
					)
					)
					.execute()
					.actionGet()
		}

		then:
		//sorted by ascending created date
		def bucket = sr1.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
		assert bucket.docCount == 3

		def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
		assert hits
		assert hits.size() == 3
		assert hits[0].getSource().message == postA1.message
		assert hits[1].getSource().message == postA2.message
		assert hits[2].getSource().message == postA3.message

		def bucket2 = sr1.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
		assert bucket2.docCount == 3

		def hits2 = bucket2.getAggregations().get("top_hits").getHits().getHits()
		assert hits2
		assert hits2.size() == 3
		assert hits2[0].getSource().message == postB1.message
		assert hits2[1].getSource().message == postB2.message
		assert hits2[2].getSource().message == postB3.message

		//sorted by descending created date
		def bucket3 = sr2.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
		assert bucket3.docCount == 3

		def hits3 = bucket3.getAggregations().get("top_hits").getHits().getHits()
		assert hits3
		assert hits3.size() == 3
		assert hits3[0].getSource().message == postA3.message
		assert hits3[1].getSource().message == postA2.message
		assert hits3[2].getSource().message == postA1.message

		def bucket4 = sr2.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
		assert bucket4.docCount == 3

		def hits4 = bucket4.getAggregations().get("top_hits").getHits().getHits()
		assert hits4
		assert hits4.size() == 3
		assert hits4[0].getSource().message == postB3.message
		assert hits4[1].getSource().message == postB2.message
		assert hits4[2].getSource().message == postB1.message
	}

	void "Test Get Second Discussion Post for All Discussions"() {
		given:
		Discussion discussionA = Discussion.create(user, "discussion A", null, currentTime - 2)
		Discussion discussionB = Discussion.create(user, "discussion B", null, currentTime - 1)
		
		DiscussionPost postA1 = discussionA.createPost(user, "post1 for discussion A", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "post2 for discussion A", currentTime + 1)
		DiscussionPost postA3 = discussionA.createPost(user, "post3 for discussion A", currentTime + 2)
		DiscussionPost postB1 = discussionB.createPost(user, "post1 for discussion B", currentTime + 3)
		DiscussionPost postB2 = discussionB.createPost(user, "post2 for discussion B", currentTime + 4)
		DiscussionPost postB3 = discussionB.createPost(user, "post3 for discussion B", currentTime + 5)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		SearchResponse sr
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(
					boolQuery()
					.mustNot(
					termQuery("flags",DiscussionPost.FIRST_POST_BIT.toString())
					)
					)
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.setSize(1)  // number of post documents to show PER discussion id
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()
		}

		then:
		def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
		assert bucket.docCount == 2

		def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
		assert hits
		assert hits.size() == 1
		assert hits[0].getSource().message == postA2.message // second post to discussionA

		def bucket2 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
		assert bucket2.docCount == 2

		def hits2 = bucket2.getAggregations().get("top_hits").getHits().getHits()
		assert hits2
		assert hits2.size() == 1
		assert hits2[0].getSource().message == postB2.message // second post to discussionB
	}

	void "Test Get Second Discussion Post for Specific Discussions"() {
		given:
		Discussion discussionA = Discussion.create(user, "discussion A", null, currentTime - 3)
		Discussion discussionB = Discussion.create(user, "discussion B", null, currentTime - 2)
		Discussion discussionC = Discussion.create(user, "discussion C", null, currentTime - 1)
		
		DiscussionPost postA1 = discussionA.createPost(user, "post1 for discussion A", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "post2 for discussion A", currentTime + 1)
		DiscussionPost postA3 = discussionA.createPost(user, "post3 for discussion A", currentTime + 2)
		DiscussionPost postB1 = discussionB.createPost(user, "post1 for discussion B", currentTime + 3)
		DiscussionPost postB2 = discussionB.createPost(user, "post2 for discussion B", currentTime + 4)
		DiscussionPost postB3 = discussionB.createPost(user, "post3 for discussion B", currentTime + 5)
		DiscussionPost postC1 = discussionC.createPost(user, "post1 for discussion C", currentTime + 6)
		DiscussionPost postC2 = discussionC.createPost(user, "post2 for discussion C", currentTime + 7)
		DiscussionPost postC3 = discussionC.createPost(user, "post3 for discussion C", currentTime + 8)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		SearchResponse sr
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(
					boolQuery()
					.must(
					queryString("discussionId:(" + discussionA.id + " OR " + discussionC.id + ")"))
					.mustNot(
					termQuery("flags",DiscussionPost.FIRST_POST_BIT.toString())
					)
					)
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.setSize(1)  // number of post documents to show PER discussion id
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()
		}

		then:
		def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
		assert bucket.docCount == 2

		def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
		assert hits
		assert hits.size() == 1
		assert hits[0].getSource().message == postA2.message // second post to discussionA

		def bucket2 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
		assert bucket2 == null

		def bucket3 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionC.id.toString() }
		assert bucket3.docCount == 2

		def hits3 = bucket3.getAggregations().get("top_hits").getHits().getHits()
		assert hits3
		assert hits3.size() == 1
		assert hits3[0].getSource().message == postC2.message // second post to discussionC
	}

	void "Test Get Discussion Post Info for User Belonging to Two Groups One Discussion Each"() {
		given:
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addMember(user)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user2)
		
		UserGroup groupC = UserGroup.create("curious C", "Group C", "Discussion topics for Sprint C",
				[isReadOnly:false, defaultNotify:true])
		groupC.addMember(user)
		
		Discussion discussionA = Discussion.create(user, "groupA discussion", groupA, currentTime - 3)
		Discussion discussionB = Discussion.create(user2, "groupB discussion", groupB, currentTime - 2)
		Discussion discussionC = Discussion.create(user, "groupC discussion", groupC, currentTime - 1)
		assert discussionA
		assert discussionB
		assert discussionC
		
		DiscussionPost postA1 = discussionA.createPost(user, "postA1", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "postA2", currentTime + 1)
		DiscussionPost postB1 = discussionB.createPost(user2, "postB1", currentTime + 2)
		DiscussionPost postB2 = discussionB.createPost(user2, "postB2", currentTime + 3)
		DiscussionPost postC1 = discussionC.createPost(user, "postC1", currentTime + 4)
		DiscussionPost postC2 = discussionC.createPost(user, "postC2", currentTime + 5)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then:
		def dbGroupIds = GroupMemberReader.lookupGroupIds(userId)
		assert dbGroupIds.size() == 5
		assert dbGroupIds.contains(user.virtualUserGroupIdDiscussions)
		assert dbGroupIds.contains(discussionA.virtualUserGroupIdFollowers)
		assert dbGroupIds.contains(discussionC.virtualUserGroupIdFollowers)
		assert dbGroupIds.contains(groupA.id)
		assert dbGroupIds.contains(groupC.id)

		assert !dbGroupIds.contains(discussionB.virtualUserGroupIdFollowers)
        assert !dbGroupIds.contains(groupB.id)

		def discussionResults = Discussion.search(searchType:'query_and_fetch') {
			query_string(query:  "groupIds:(" + dbGroupIds.iterator().join(" OR ") + ")")
		}
		assert discussionResults
		assert discussionResults.searchResults.size() == 2
		assert discussionResults.searchResults.find{ d -> d.name == discussionA.name }
		assert discussionResults.searchResults.find{ d -> d.name == discussionC.name }

		when:
		SearchResponse sr
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(
					boolQuery()
					.must(
					queryString("discussionId:(" + discussionA.id + " OR " + discussionC.id + ")")
					)
					.mustNot(
					termQuery("flags",DiscussionPost.FIRST_POST_BIT.toString())
					)
					)
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.setSize(1)  // number of post documents to show PER discussion id
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()
		}

		then:
		def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
		assert bucket.docCount == 1

		def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
		assert hits
		assert hits.size() == 1
		assert hits[0].getSource().message == postA2.message // second post to discussionA

		def bucket2 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
		assert bucket2 == null

		def bucket3 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionC.id.toString() }
		assert bucket3.docCount == 1

		def hits3 = bucket3.getAggregations().get("top_hits").getHits().getHits()
		assert hits3
		assert hits3.size() == 1
		assert hits3[0].getSource().message == postC2.message // second post to discussionC
	}

	void "Test Get Discussion Post Info Including None"() {
		given:
		Discussion discussionA = Discussion.create(user, "discussion A", null, currentTime - 4)
		Discussion discussionB = Discussion.create(user, "discussion B", null, currentTime - 3)
		Discussion discussionC = Discussion.create(user, "discussion C", null, currentTime - 2)
		Discussion discussionD = Discussion.create(user, "discussion D", null, currentTime - 1)
		
		DiscussionPost postA1 = discussionA.createPost(user, "postA1", currentTime)
		DiscussionPost postA2 = discussionA.createPost(user, "postA2", currentTime + 1)
		DiscussionPost postA3 = discussionA.createPost(user, "postA3", currentTime + 2)
		DiscussionPost postB1 = discussionB.createPost(user, "postB1", currentTime + 3)
		DiscussionPost postB2 = discussionB.createPost(user, "postB2", currentTime + 4)
		DiscussionPost postC1 = discussionC.createPost(user, "postC1", currentTime + 5)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		SearchResponse sr
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(
					boolQuery()
					.must(
					queryString("discussionId:("
					+ discussionA.id.toString() + " OR "
					+ discussionB.id.toString() + " OR "
					+ discussionC.id.toString() + " OR "
					+ discussionD.id.toString()
					+  ")")
					)
					.mustNot(
					termQuery("flags",DiscussionPost.FIRST_POST_BIT.toString())
					)
					)
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.setSize(1)  // number of post documents to show PER discussion id
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()
		}

		then:
		def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA.id.toString() }
		assert bucket.docCount == 2

		def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
		assert hits
		assert hits.size() == 1
		assert hits[0].getSource().message == postA2.message // second post to discussionA

		def bucket2 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB.id.toString() }
		assert bucket2.docCount == 1

		def hits2 = bucket2.getAggregations().get("top_hits").getHits().getHits()
		assert hits2
		assert hits2.size() == 1
		assert hits2[0].getSource().message == postB2.message // second post to discussionC

		def bucket3 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionC.id.toString() }
		assert bucket3 == null

		def bucket4 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionD.id.toString() }
		assert bucket4 == null
	}

	void "Test Get Discussion Post Info for User Belonging to Two Groups Multiple Discussions Each"() {
		given:
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addMember(user)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user)
		
		UserGroup groupC = UserGroup.create("curious C", "Group C", "Discussion topics for Sprint C",
				[isReadOnly:false, defaultNotify:true])
		groupC.addMember(user2)
		
		Discussion discussionA1 = Discussion.create(user, "groupA discussion 1", groupA, currentTime - 5)
		Discussion discussionA2 = Discussion.create(user, "groupA discussion 2", groupA, currentTime - 4)
		Discussion discussionB1 = Discussion.create(user, "groupB discussion 1", groupB, currentTime - 3)
		Discussion discussionB2 = Discussion.create(user, "groupB discussion 2", groupB, currentTime - 2)
		Discussion discussionC1 = Discussion.create(user2, "groupC discussion 1", groupC, currentTime - 1)
		
		DiscussionPost postA11 = discussionA1.createPost(user, "postA11", currentTime)
		DiscussionPost postA12 = discussionA1.createPost(user, "postA12", currentTime + 1)
		DiscussionPost postA21 = discussionA2.createPost(user, "postA21", currentTime + 2)
		DiscussionPost postA22 = discussionA2.createPost(user, "postA22", currentTime + 3)
		DiscussionPost postB11 = discussionB1.createPost(user, "postB11", currentTime + 4)
		DiscussionPost postB12 = discussionB1.createPost(user, "postB12", currentTime + 5)
		DiscussionPost postB21 = discussionB2.createPost(user, "postB21", currentTime + 6)
		DiscussionPost postB22 = discussionB2.createPost(user, "postB22", currentTime + 7)
		DiscussionPost postC11 = discussionC1.createPost(user2, "postC11", currentTime + 8)
		DiscussionPost postC12 = discussionC1.createPost(user2, "postC12", currentTime + 9)

		when:
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
        println "user.virtualUserGroupIdDiscussions: ${user.virtualUserGroupIdDiscussions}"
        println "user.virtualUserGroupIdFollowers: ${user.virtualUserGroupIdFollowers}"
        println "discussionA1.virtualUserGroupIdFollowers: ${discussionA1.virtualUserGroupIdFollowers}"
        println "discussionA2.virtualUserGroupIdFollowers: ${discussionA2.virtualUserGroupIdFollowers}"
        println "discussionB1.virtualUserGroupIdFollowers: ${discussionB1.virtualUserGroupIdFollowers}"
        println "discussionB2.virtualUserGroupIdFollowers: ${discussionB2.virtualUserGroupIdFollowers}"
        println "groupA.id: ${groupA.id}"
        println "groupB.id: ${groupB.id}"
        println "groupC.id: ${groupC.id}"

		then:
		def dbGroupIds = GroupMemberReader.lookupGroupIds(userId)
		assert dbGroupIds.size() == 7
		assert dbGroupIds.contains(user.virtualUserGroupIdDiscussions)
        assert dbGroupIds.contains(discussionA1.virtualUserGroupIdFollowers)
        assert dbGroupIds.contains(discussionA2.virtualUserGroupIdFollowers)
        assert dbGroupIds.contains(discussionB1.virtualUserGroupIdFollowers)
        assert dbGroupIds.contains(discussionB2.virtualUserGroupIdFollowers)
		assert dbGroupIds.contains(groupA.id)
		assert dbGroupIds.contains(groupB.id)
        
        assert !dbGroupIds.contains(discussionC1.virtualUserGroupIdFollowers)
        assert !dbGroupIds.contains(user.virtualUserGroupIdFollowers)
		assert !dbGroupIds.contains(groupC.id)
		
		def discussionResults = Discussion.search(searchType:'query_and_fetch') {
			query_string(query:  "groupIds:(" + dbGroupIds.iterator().join(" OR ") + ")")
		}

		when:
		SearchResponse sr
		elasticSearchHelper.withElasticSearch{ client ->
			sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost")
					.setQuery(
					boolQuery()
					.must(
					queryString(
					"discussionId:(" +
					discussionA1.id + " OR " +
					discussionA2.id + " OR " +
					discussionB1.id + " OR " +
					discussionB2.id +
					")"
					)
					)
					.mustNot(
					termQuery("flags",DiscussionPost.FIRST_POST_BIT.toString())
					)
					)
					.setSize(0)   // prevent documents from showing up; only interested in count stats and top posts PER discussionId
					.addAggregation(
					AggregationBuilders
					.terms("by_discussionId")
					.field("discussionId")
					.subAggregation(
					AggregationBuilders
					.topHits("top_hits")
					.setSize(1)  // number of post documents to show PER discussion id
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
					)
					)
					.execute()
					.actionGet()
		}

		then:
		def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA1.id.toString() }
		assert bucket.docCount == 1

		def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
		assert hits
		assert hits.size() == 1
		assert hits[0].getSource().message == postA12.message // second post to discussionA1

		def bucket2 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionA2.id.toString() }
		assert bucket2.docCount == 1

		def hits2 = bucket2.getAggregations().get("top_hits").getHits().getHits()
		assert hits2
		assert hits2.size() == 1
		assert hits2[0].getSource().message == postA22.message // second post to discussionA2

		def bucket3 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB1.id.toString() }
		assert bucket3.docCount == 1

		def hits3 = bucket3.getAggregations().get("top_hits").getHits().getHits()
		assert hits3
		assert hits3.size() == 1
		assert hits3[0].getSource().message == postB12.message // second post to discussionB1

		def bucket4 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionB2.id.toString() }
		assert bucket4.docCount == 1

		def hits4 = bucket4.getAggregations().get("top_hits").getHits().getHits()
		assert hits4
		assert hits4.size() == 1
		assert hits4[0].getSource().message == postB22.message // second post to discussionB2

		def bucket5 = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == discussionC1.id.toString() }
		assert bucket5 == null
	}
/**/
}

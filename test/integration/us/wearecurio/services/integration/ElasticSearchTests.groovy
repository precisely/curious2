package us.wearecurio.services.integration;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.search.SearchHit
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.functionscore.*

import static org.elasticsearch.index.query.QueryBuilders.*

import org.elasticsearch.index.query.QueryBuilders

import static org.elasticsearch.node.NodeBuilder.*

import java.util.Arrays;

import static org.elasticsearch.client.Requests.searchRequest;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.factorFunction;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;


import us.wearecurio.model.OAuthAccount



import java.text.DateFormat;
import java.util.Date;

import grails.test.mixin.*

import org.grails.plugins.elasticsearch.ElasticSearchService;
import org.joda.time.DateTimeZone;
import org.junit.*
import org.scribe.model.Response

import us.wearecurio.model.Model
import us.wearecurio.model.Entry
import us.wearecurio.model.OAuthAccount
import us.wearecurio.model.ThirdParty
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.services.DataService
import us.wearecurio.services.FitBitDataService
import us.wearecurio.services.WithingsDataService
import us.wearecurio.test.common.MockedHttpURLConnection
import us.wearecurio.thirdparty.InvalidAccessTokenException
import us.wearecurio.utility.Utils
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.UserGroup
import us.wearecurio.model.GroupMemberReader


class ElasticSearchTests extends CuriousServiceTestCase {
    static transactional = true
    
    DateFormat dateFormat
    Date currentTime
    String timeZone // simulated server time zone
    TimeZoneId timeZoneId
    DateTimeZone dateTimeZone
    Date baseDate
    UserGroup testGroup

    ElasticSearchService elasticSearchService
    def elasticSearchHelper

    @Before
    void setUp() {
        super.setUp()

        Locale.setDefault(Locale.US)    // For to run test case in any country.
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

    //helpers
    private void testSimpleSearch(Object container, String fieldName) {
        testSimpleSearch(container, fieldName, true)
    }

    private void testSimpleSearch(Object container, String fieldName, Boolean saveContainer) {
        if (saveContainer) {
            Utils.save(container, true)
        }

        elasticSearchService.index()
        Thread.sleep(2000)

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
        assert results.searchResults.find { d -> d.id == container.id }
    }

      @Test
      void "Test Search Discuussion by userId"() {
          testSimpleSearch(Discussion.create(user), "userId")
      }

          @Test
      void "Test Search Discuussion by firstPostId"() {
          Discussion discussion = Discussion.create(user)
          DiscussionPost post = discussion.createPost(user, "Test post")
          Utils.save(discussion, true)
          Utils.save(post, true)
          testSimpleSearch(discussion, "firstPostId", false)
      }

      @Test
      void "Test Search Discuussion by name"() {
          testSimpleSearch(Discussion.create(user, "Topic name"), "name")
      }

      @Test
      void "Test Search Discuussion by created"() {
          testSimpleSearch(Discussion.create(user), "created")
      }

      @Test
      void "Test Search Discuussion by updated"() {
          testSimpleSearch(Discussion.create(user), "updated")
      }

      @Test
      void "Test Search Discuussion by visibility"() {
          testSimpleSearch(Discussion.create(user), "visibility")
      }

      @Test
      void "Test Search Discussion by groupIds"() {
          UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
              [isReadOnly:false, defaultNotify:false])
          groupA.addMember(user)
          UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
              [isReadOnly:false, defaultNotify:true])
          groupB.addMember(user)

          Discussion discussion = Discussion.create(user, "Topic name", groupA)
          groupB.addDiscussion(discussion)

          Utils.save(discussion, true)

          elasticSearchService.index()

          Thread.sleep(2000)

          def results = Discussion.search(searchType:'query_and_fetch') {
            bool {
                must {
                    query_string(query: "groupIds:(" + groupA.id + " OR " + groupB.id + ")")
                }
            }
          }

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion.id }
      }

      //discussion posts
        //helpers
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

          Utils.save(discussion, true)
          Utils.save(post, true)

          testSimpleSearch(post, fieldName, false)
      }

      @Test
      void "Test Search Discussion Post by discussionId"() {
          testSimplePostSearch("discussionId")
      }

      @Test
      void "Test Search Discussion Post by authorUserId"() {
          testSimplePostSearch("authorUserId")
      }

      @Test
      void "Test Search Discussion Post by created"() {
          testSimplePostSearch("created")
      }

      @Test
      void "Test Search Discussion Post by updated"() {
          testSimplePostSearch("updated")
      }

      @Test
      void "Test Search Discussion Post by plotDataId"() {
          testSimplePostSearch("plotDataId", 100L)
      }

      @Test
      void "Test Search Discussion Post by message"() {
          testSimplePostSearch("message", "Test message")
      }

        @Test
        void "Test Search Discussions Created in Past Week"(){
            Discussion discussionRecent = Discussion.create(user, "common")
            Discussion discussionRecent2 = Discussion.create(user, "common")
            Discussion discussionOld = Discussion.create(user, "common")

            use (groovy.time.TimeCategory){
                discussionOld.created = (new Date()) - 8.days
                discussionRecent2.created = (new Date()) -  6.days
            }

            Utils.save(discussionOld, true)
            Utils.save(discussionRecent, true)
            Utils.save(discussionRecent2, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch') {
                filtered {
                    query {
                        query_string(query: "name:common")
                    }
                    filter {
                        range{
                            created(gte: "now-7d/d")
                        }
                    }
                }
            }

            System.out.println "size: " + results.searchResults.size()
            assert results
            assert results.searchResults.find { d -> d.id == discussionRecent.id }
            assert results.searchResults.find { d -> d.id == discussionRecent2.id }
            assert results.searchResults.find { d -> d.id == discussionOld.id } == null
        }

        @Test
        void "Test Search Discussion Posts Created in Past Week"(){
            Discussion discussion = Discussion.create(user, "test")
            DiscussionPost postOld = discussion.createPost(user, "common")
            DiscussionPost postRecent = discussion.createPost(user, "common")

            use (groovy.time.TimeCategory){
                postOld.created = (new Date()) - 8.days
            }

            Utils.save(discussion, true)
            Utils.save(postOld, true)
            Utils.save(postRecent, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = DiscussionPost.search(searchType:'query_and_fetch') {
                filtered {
                    query {
                        query_string(query: "message:common")
                    }
                    filter {
                        range{
                            created(gte: "now-7d/d")
                        }
                    }
                }
            }

            System.out.println "size: " + results.searchResults.size()
            assert results
            assert results.searchResults.find { dp -> dp.id == postRecent.id }
            assert results.searchResults.find { dp -> dp.id == postOld.id } == null
        }


      @Test
      void "Test Search Discussions Updated in Past Week"(){
            Discussion discussion = Discussion.create(user)
            DiscussionPost post = discussion.createPost(user, "Test post")

            Utils.save(discussion, true)
            Utils.save(post, true)

            elasticSearchService.index()
            Thread.sleep(2000)

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

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion.id }
      }

      @Test
      void "Test Search Discussions Using List of Keywords"()
      {
            Discussion discussion1 = Discussion.create(user, "word1")
            Discussion discussion2 = Discussion.create(user, "word2")
            Discussion discussion3 = Discussion.create(user, "word3")
            Discussion discussion4 = Discussion.create(user, "word1 word2")
            Discussion discussion5 = Discussion.create(user, "word1 word3")
            Discussion discussion6 = Discussion.create(user, "word2 word3")
            Discussion discussion7 = Discussion.create(user, "word1 word2 word3")

            Utils.save(discussion1, true)
            Utils.save(discussion2, true)
            Utils.save(discussion3, true)
            Utils.save(discussion4, true)
            Utils.save(discussion5, true)
            Utils.save(discussion6, true)
            Utils.save(discussion7, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch') {
                query_string(query: "name:(word3 OR word1 OR word2)")
            }

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion1.id }
            assert results.searchResults.find { d -> d.id == discussion2.id }
            assert results.searchResults.find { d -> d.id == discussion3.id }
            assert results.searchResults.find { d -> d.id == discussion4.id }
            assert results.searchResults.find { d -> d.id == discussion5.id }
            assert results.searchResults.find { d -> d.id == discussion6.id }
            assert results.searchResults.find { d -> d.id == discussion7.id }

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

      @Test
      void "Test Search Discussions Using Exact Phrase"()
      {
            Discussion discussion1 = Discussion.create(user, "word1 word2 word3")
            Discussion discussion2 = Discussion.create(user, "word3 word2 word1")

            Utils.save(discussion1, true)
            Utils.save(discussion2, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch') {
                match_phrase(name:"word1 word2 word3")
            }

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion1.id }
            assert results.searchResults.find { d -> d.id == discussion2.id } == null
      }

        @Test
        void "Test Search Discussion in Order of Most Hits"()
        {
            Discussion discussion1 = Discussion.create(user, "word1 word2")
            Discussion discussion2 = Discussion.create(user, "word1 word2 word3")

            Utils.save(discussion1, true)
            Utils.save(discussion2, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch') {
                query_string(query: "name:(word3 OR word1)")
            }

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion1.id }
            assert results.searchResults.find { d -> d.id == discussion2.id }
            assert results.searchResults[0].id == discussion2.id
            assert results.searchResults[1].id == discussion1.id
        }

      @Test
      void "Test Search Discussions Multiple Results in Chronological order"()
      {
            Discussion discussion1 = Discussion.create(user, "ChonologicalOrderAscTest")
            Thread.sleep(2000)
            Discussion discussion2 = Discussion.create(user, "ChonologicalOrderAscTest")

            Utils.save(discussion1, true)
            Utils.save(discussion2, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch', sort:'created', order:'asc') {
                query_string(query: "name:ChonologicalOrderAscTest")
            }

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion1.id }
            assert results.searchResults.find { d -> d.id == discussion2.id }
            assert results.searchResults[0].id == discussion1.id
            assert results.searchResults[1].id == discussion2.id
      }

      @Test
      void "Test Search Discussions Multiple Results in Reverse Chronological order"()
      {
            Discussion discussion1 = Discussion.create(user, "ChonologicalOrderDescTest")
            Thread.sleep(2000)
            Discussion discussion2 = Discussion.create(user, "ChonologicalOrderDescTest")

            Utils.save(discussion1, true)
            Utils.save(discussion2, true)

            System.out.println "discussion1.creation: " + discussion1.created
            System.out.println "discussion2.creation: " + discussion2.created

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch', sort:'created', order:'desc') {
                query_string(query: "name:ChonologicalOrderDescTest")
            }

            //results in chronological order by default
            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion1.id }
            assert results.searchResults.find { d -> d.id == discussion2.id }
            assert results.searchResults[0].id == discussion2.id
            assert results.searchResults[1].id == discussion1.id
      }

      @Test
      void "Test Search Discussions Match Two Keywords"()
      {
            Discussion discussion1 = Discussion.create(user, "word1 word2 word3")
            Discussion discussion2 = Discussion.create(user, "word1 word2")

            Utils.save(discussion1, true)
            Utils.save(discussion2, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch') {
                query_string(query: "name:(word3 AND word1)")
            }

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion1.id }
            assert results.searchResults.find { d -> d.id == discussion2.id } == null
      }

      @Test
      void "Test Search Discussions Match Three Keywords"()
      {
            Discussion discussion1 = Discussion.create(user, "word1 word2 word3")
            Discussion discussion2 = Discussion.create(user, "word1 word2")

            Utils.save(discussion1, true)
            Utils.save(discussion2, true)

            elasticSearchService.index()
            Thread.sleep(2000)

            def results = Discussion.search(searchType:'query_and_fetch') {
                query_string(query: "name:(word3 AND word1 AND word2)")
            }

            assert results.searchResults
            assert results.searchResults.find { d -> d.id == discussion1.id }
            assert results.searchResults.find { d -> d.id == discussion2.id } == null

      }

    @Test
    void "Test Scoring Function"() {
        Discussion discussion1 = Discussion.create(user, "TestScoringFunction word2 word3")
        Discussion discussion2 = Discussion.create(user, "TestScoringFunction word4 word5")

        discussion1.firstPostId = 30
        discussion2.firstPostId = 0

        Utils.save(discussion1, true)
        Utils.save(discussion2, true)

        elasticSearchService.index()
        Thread.sleep(2000)

        FunctionScoreQueryBuilder fsqb = functionScoreQuery(matchQuery("name","TestScoringFunction"))
        fsqb.add(ScoreFunctionBuilders.linearDecayFunction("firstPostId", 0, 20))

        elasticSearchHelper.withElasticSearch{ client ->
            SearchResponse sr = client.prepareSearch("us.wearecurio.model_v0").setTypes("discussion").setQuery(fsqb).execute().actionGet()
            //          SearchResponse sr = client.prepareSearch("us.wearecurio.model_v0").setTypes("discussion").setQuery(matchQuery("name", "word1 word2")).execute().actionGet()
            assert sr
            assert sr.getHits()
            if (sr.getHits().getTotalHits() == 0) System.out.println "no hits found!"
            for( SearchHit hit : sr.getHits().getHits() ) {
                System.out.println "hit: " + hit.getIndex() + ": " + hit.getType() + ": " + hit.getId() + " (score: " + hit.getScore() + ")"
            }
            assert (sr.getHits().getHits()[0].getId() == discussion2.id.toString())
            assert (sr.getHits().getHits()[1].getId() == discussion1.id.toString())
            assert (sr.getHits().getHits()[0].getScore() > sr.getHits().getHits()[1].getScore())
        }
    }

    String listToString( ArrayList values, String sep )
    {
        String ret = ""
        Boolean first = true
        for (int groupId: values) {
            if(!first) ret += " " + sep + " "
            else first = false
            ret += groupId.toString()
        }

        if (values.size() > 1) ret = "(" + ret + ")"

        return ret
    }

    @Test
    void "Test Search Discussion For Which User Is a Reader"() {
        UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
          [isReadOnly:false, defaultNotify:false])
        groupA.addMember(user)

        UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
          [isReadOnly:false, defaultNotify:true])
        groupB.addMember(user2)

        Discussion discussionReadByUser = Discussion.create(user, "groupA discussion", groupA)
        Discussion discussionNotReadByUser = Discussion.create(user2, "groupB discussion", groupB)

        DiscussionPost postA = discussionReadByUser.createPost(user, "postA")
        DiscussionPost postB = discussionNotReadByUser.createPost(user, "postB")

        Utils.save(discussionReadByUser, true)
        Utils.save(discussionNotReadByUser, true)
        Utils.save(postA, true)
        Utils.save(postB, true)

        elasticSearchService.index()
        Thread.sleep(2000)

        def dbGroupIds = GroupMemberReader.lookupGroupIds(userId)
        assert dbGroupIds.size() > 0
        assert dbGroupIds.contains(groupA.id)
        assert !dbGroupIds.contains(groupB.id)
        String groupIdsOr = listToString(dbGroupIds, "OR")

        def discussionResults = Discussion.search(searchType:'query_and_fetch') {
            query_string(query:  "groupIds:" + groupIdsOr)
        }
        assert discussionResults
        assert discussionResults.searchResults.size() > 0
        assert discussionResults.searchResults.find { d -> d.id == discussionReadByUser.id }
        assert discussionResults.searchResults.find { d -> d.id == discussionNotReadByUser.id } == null

        def discussionIds = new ArrayList()
        for (Discussion d in discussionResults.searchResults) {
            discussionIds.add(d.id)
        }

        String discussionIdsOr = listToString(discussionIds, "OR")
        def discussionPostResults = DiscussionPost.search(searchType:'query_and_fetch') {
            query_string(query: "discussionId:" + discussionIdsOr + " AND message:postA")
        }

        assert discussionPostResults
        assert discussionPostResults.searchResults.size() > 0
        assert discussionPostResults.searchResults.find { dp -> dp.id == postA.id }
        assert discussionPostResults.searchResults.find { dp -> dp.id == postB.id } == null

        discussionPostResults = DiscussionPost.search(searchType:'query_and_fetch') {
            query_string(query: "discussionId:" + discussionIdsOr + " AND message:postB")
        }

        assert discussionPostResults
        assert discussionPostResults.searchResults.size() == 0
    }    
}

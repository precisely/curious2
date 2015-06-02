package us.wearecurio.services.integration

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
import us.wearecurio.services.SearchService

class SearchServiceTests extends CuriousServiceTestCase {
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
    void "Test getDiscussionsList"()
    {
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

        def searchService = SearchService.get()
        assert searchService
    }
}

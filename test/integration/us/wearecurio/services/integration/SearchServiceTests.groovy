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

import grails.test.mixin.integration.Integration
import grails.transaction.*
import spock.lang.*

@Integration
@Rollback
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
    //def elasticSearchHelper
    def searchService

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
        
        assert searchService
        searchService.elasticSearchService = elasticSearchService

        Utils.save(discussionReadByUser, true)
        Utils.save(discussionNotReadByUser, true)

        assert Discussion.findByName(discussionReadByUser.name).id == discussionReadByUser.id
        assert Discussion.findByName(discussionNotReadByUser.name).id == discussionNotReadByUser.id
        assert UserGroup.findByName(groupA.name).id == groupA.id
        assert UserGroup.findByName(groupB.name).id == groupB.id
        
        elasticSearchService.index()
        Thread.sleep(2000)        
        
        Map result = searchService.getDiscussionsList(user, 0, 10)
        assert result
        assert result.success
        assert result.listItems.userId == user.id
        assert result.listItems.totalDiscussionCount == 1
        //groupMemberships should return 1 row. 
        //  first column: UserGroup
        //  second column: Datetime that GroupMemberReader object was created
        assert result.listItems.groupMemberships
        assert result.listItems.groupMemberships.size() == 1
        assert result.listItems.groupMemberships[0][0].id == groupA.id
        assert result.listItems.groupMemberships[0][0].name == groupA.name
        assert result.listItems.groupMemberships[0][0].fullName == groupA.fullName
        //need to round milliseconds
        assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[0][1].getTime()) >= Utils.elasticSearchRoundMs(groupA.created.getTime())
        assert result.listItems.discussionList
        assert result.listItems.discussionList.size() == 1
        assert result.listItems.discussionList[0].id == discussionReadByUser.id
        assert result.listItems.discussionList[0].name == discussionReadByUser.name
        assert !result.listItems.discussionPostData        
    }

    @Test
    void "Test getDiscussionsList with offset and max"()
    {
        UserGroup group = UserGroup.create("curious", "Group", "Discussion topics for Sprint",
          [isReadOnly:false, defaultNotify:false])
        group.addMember(user)

        Discussion discussion1 = Discussion.create(user, "discussion 1", group)
        Discussion discussion2 = Discussion.create(user, "discussion 2", group)
        
        assert searchService
        searchService.elasticSearchService = elasticSearchService

        Utils.save(discussion1, true)
        Utils.save(discussion2, true)

        assert Discussion.findByName(discussion1.name).id == discussion1.id
        assert Discussion.findByName(discussion2.name).id == discussion2.id
        assert UserGroup.findByName(group.name).id == group.id
        
        elasticSearchService.index()
        Thread.sleep(2000)
        
        //first no offset, max set to 10 so all 2 results are included        
        Map result = searchService.getDiscussionsList(user, 0, 10)
        assert result
        assert result.success
        assert result.listItems.userId == user.id
        assert result.listItems.totalDiscussionCount == 2
        //groupMemberships should return 1 row.
        //  first column: UserGroup
        //  second column: Datetime that GroupMemberReader object was created
        assert result.listItems.groupMemberships
        assert result.listItems.groupMemberships.size() == 1
        assert result.listItems.groupMemberships[0][0].id == group.id
        assert result.listItems.groupMemberships[0][0].name == group.name
        assert result.listItems.groupMemberships[0][0].fullName == group.fullName
        //need to round milliseconds
        assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[0][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
        assert result.listItems.discussionList
        assert result.listItems.discussionList.size() == 2
        assert result.listItems.discussionList[0].id == discussion1.id
        assert result.listItems.discussionList[0].name == discussion1.name
        assert result.listItems.discussionList[1].id == discussion2.id
        assert result.listItems.discussionList[1].name == discussion2.name
        assert !result.listItems.discussionPostData

        //limit max to 1, no offset
        result = searchService.getDiscussionsList(user, 0, 1)
        assert result
        assert result.success
        assert result.listItems.userId == user.id
        assert result.listItems.totalDiscussionCount == 1
        assert result.listItems.groupMemberships
        assert result.listItems.groupMemberships.size() == 1
        assert result.listItems.groupMemberships[0][0].id == group.id
        assert result.listItems.groupMemberships[0][0].name == group.name
        assert result.listItems.groupMemberships[0][0].fullName == group.fullName
        assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[0][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
        assert result.listItems.discussionList
        assert result.listItems.discussionList.size() == 1
        assert result.listItems.discussionList[0].id == discussion1.id
        assert result.listItems.discussionList[0].name == discussion1.name
        assert !result.listItems.discussionPostData

        //limit max to 1, offset to second item (index=1)
        result = searchService.getDiscussionsList(user, 1, 1)
        assert result
        assert result.success
        assert result.listItems.userId == user.id
        assert result.listItems.totalDiscussionCount == 1
        assert result.listItems.groupMemberships
        assert result.listItems.groupMemberships.size() == 1
        assert result.listItems.groupMemberships[0][0].id == group.id
        assert result.listItems.groupMemberships[0][0].name == group.name
        assert result.listItems.groupMemberships[0][0].fullName == group.fullName
        assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[0][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
        assert result.listItems.discussionList
        assert result.listItems.discussionList.size() == 1
        assert result.listItems.discussionList[0].id == discussion2.id
        assert result.listItems.discussionList[0].name == discussion2.name
        assert !result.listItems.discussionPostData
    }
}

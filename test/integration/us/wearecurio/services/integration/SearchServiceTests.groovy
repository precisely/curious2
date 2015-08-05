package us.wearecurio.services.integration

import java.util.Arrays;

import static org.elasticsearch.index.query.QueryBuilders.*
import org.grails.plugins.elasticsearch.ElasticSearchAdminService;
import org.grails.plugins.elasticsearch.ElasticSearchService

import us.wearecurio.model.OAuthAccount

import java.text.DateFormat
import java.util.Date

import grails.test.mixin.*

import org.grails.plugins.elasticsearch.ElasticSearchAdminService
import org.grails.plugins.elasticsearch.ElasticSearchService
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
import us.wearecurio.model.Sprint
import us.wearecurio.services.SearchService
import grails.test.mixin.integration.Integration
import org.springframework.transaction.annotation.*
import spock.lang.*

@Integration
class SearchServiceTests extends CuriousServiceTestCase {
	static transactional = false
	
	DateFormat dateFormat
	Date currentTime
	String timeZone // simulated server time zone
	TimeZoneId timeZoneId
	DateTimeZone dateTimeZone
	Date baseDate
	UserGroup testGroup
	
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
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		GroupMemberReader.executeUpdate("delete GroupMemberReader r")
		UserGroup.executeUpdate("delete UserGroup g")
		
		super.setUp()
		
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
		
		elasticSearchService.index()
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void "Test getSprintsList"()
	{
		def sprint = Sprint.create(new Date(), user, "Test getSprintsList", Model.Visibility.PUBLIC)
		Utils.save(sprint, true)
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		Map result = searchService.getSprintsList(user, 0, 10)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.sprintList
		assert result.listItems.sprintList.size() == 1
		assert result.listItems.sprintList[0].id == sprint.id
		assert result.listItems.sprintList[0].name == sprint.name
	}

	@Test
	void "Test getSprintsList with offset and max"()
	{
		def sprint1 = Sprint.create(currentTime + 1, user, "Test getSprintsList 1", Model.Visibility.PUBLIC)
		def sprint2 = Sprint.create(currentTime, user, "Test getSprintsList 2", Model.Visibility.PUBLIC)
		Utils.save(sprint1, true)
		Utils.save(sprint2, true)
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		Map result = searchService.getSprintsList(user, 0, 10)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.sprintList
		assert result.listItems.sprintList.size() == 2
		assert result.listItems.sprintList[0].id == sprint1.id
		assert result.listItems.sprintList[0].name == sprint1.name
		assert result.listItems.sprintList[1].id == sprint2.id
		assert result.listItems.sprintList[1].name == sprint2.name
		
		result = searchService.getSprintsList(user, 0, 1)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.sprintList
		assert result.listItems.sprintList.size() == 1
		assert result.listItems.sprintList[0].id == sprint1.id
		assert result.listItems.sprintList[0].name == sprint1.name

		result = searchService.getSprintsList(user, 1, 1)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.sprintList
		assert result.listItems.sprintList.size() == 1
		assert result.listItems.sprintList[0].id == sprint2.id
		assert result.listItems.sprintList[0].name == sprint2.name
	}

	@Test
	void "Test getPeopleList"()
	{
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def result = searchService.getPeopleList(user, 0, 10)
		
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.size() == 1
		assert result.listItems[0].id == user2.id
		assert result.listItems[0].username == user2.username
		assert result.listItems[0].name == user2.name
	}
	
	@Test
	void "Test getPeopleList with offset and max"()
	{
		def params = [username:'third', sex:'F', \
			name:'third third', email:'third@third.com', birthdate:'01/01/1990', \
			password:'third', action:'doregister', \
			controller:'home']

		def user3 = User.create(params)

		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		def result = searchService.getPeopleList(user, 0, 10)
		
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.size() == 2
		assert result.listItems.find{ u -> u.id == user2.id } 
		assert result.listItems.find{ u -> u.id == user2.id }.username == user2.username
		assert result.listItems.find{ u -> u.id == user2.id }.name == user2.name
		assert result.listItems.find{ u -> u.id == user3.id }
		assert result.listItems.find{ u -> u.id == user3.id }.username == user3.username
		assert result.listItems.find{ u -> u.id == user3.id }.name == user3.name

		result = searchService.getPeopleList(user, 0, 1)
		
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.size() == 1
		assert (result.listItems.find{ u -> u.id == user2.id } || result.listItems.find{ u -> u.id == user3.id })
		assert (result.listItems.find{ u -> u.username == user2.username } || result.listItems.find{ u -> u.username == user3.username })
		assert (result.listItems.find{ u -> u.name == user2.name } || result.listItems.find{ u -> u.name == user3.name })

		result = searchService.getPeopleList(user, 1, 1)
		
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.size() == 1
		assert (result.listItems.find{ u -> u.id == user2.id } || result.listItems.find{ u -> u.id == user3.id })
		assert (result.listItems.find{ u -> u.username == user2.username } || result.listItems.find{ u -> u.username == user3.username })
		assert (result.listItems.find{ u -> u.name == user2.name } || result.listItems.find{ u -> u.name == user3.name })
	}

	@Test
	void "Test getDiscussionsList"()
	{
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addAdmin(user)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user2)
		
		Discussion discussionReadByUser = Discussion.create(user, "groupA discussion", groupA, currentTime)
		Discussion discussionNotReadByUser = Discussion.create(user2, "groupB discussion", groupB, currentTime + 1)
		DiscussionPost readByUser1 = discussionReadByUser.createPost(user, "readByUser1", currentTime + 2)
		DiscussionPost readByUser2 = discussionReadByUser.createPost(user, "readByUser2", currentTime + 3)
		
		Utils.save(discussionReadByUser, true)
		Utils.save(discussionNotReadByUser, true)
		
		assert Discussion.findByName(discussionReadByUser.name).id == discussionReadByUser.id
		assert Discussion.findByName(discussionNotReadByUser.name).id == discussionNotReadByUser.id
		assert UserGroup.findByName(groupA.name).id == groupA.id
		assert UserGroup.findByName(groupB.name).id == groupB.id
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		Map result = searchService.getDiscussionsList(user, 0, 10)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.userId == user.id
		assert result.listItems.totalDiscussionCount == 1
		//groupMemberships should return 1 row.
		//  first column: UserGroup
		//  second column: DateTime that GroupMemberReader object was created
		assert result.listItems.groupMemberships
		assert result.listItems.groupMemberships.size() == 2
		assert result.listItems.groupMemberships[1][0].id == groupA.id
		assert result.listItems.groupMemberships[1][0].name == groupA.name
		assert result.listItems.groupMemberships[1][0].fullName == groupA.fullName
		//need to round milliseconds.
		assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(groupA.created.getTime())
		assert result.listItems.discussionList
		assert result.listItems.discussionList.size() == 1
		assert result.listItems.discussionList[0].id == discussionReadByUser.id
		assert result.listItems.discussionList[0].name == discussionReadByUser.name
		assert result.listItems.discussionList[0].userId == discussionReadByUser.userId
		assert result.listItems.discussionList[0].isPublic == discussionReadByUser.isPublic()
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].created) == Utils.elasticSearchDate(discussionReadByUser.created)
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].updated) == Utils.elasticSearchDate(discussionReadByUser.updated)
		assert result.listItems.discussionList[0].type == "dis"
		assert result.listItems.discussionList[0].totalComments == 2
		assert result.listItems.discussionList[0].isPlot == false
		assert result.listItems.discussionList[0].firstPost
		assert result.listItems.discussionList[0].firstPost.message == readByUser1.message
		// the discussion item should show the non-virtual user group for UI purposes
		assert result.listItems.discussionList[0].groupId == groupA.id
		assert result.listItems.discussionList[0].isAdmin
		assert result.listItems.discussionList[0].groupName == groupA.fullName
		assert result.listItems.discussionPostData
		assert result.listItems.discussionPostData[discussionReadByUser.id].secondPost
		assert result.listItems.discussionPostData[discussionReadByUser.id].secondPost.message == readByUser2.message
		assert result.listItems.discussionPostData[discussionReadByUser.id].totalPosts == 2
	}
	
	@Test
	void "Test getDiscussionsList with offset and max"()
	{
		UserGroup group = UserGroup.create("curious", "Group", "Discussion topics for Sprint",
				[isReadOnly:false, defaultNotify:false])
		group.addMember(user)
		
		Discussion discussion1 = Discussion.create(user, "discussion 1", group, currentTime)
		Discussion discussion2 = Discussion.create(user, "discussion 2", group, currentTime + 1)
		
		System.out.println "discussion1.created: " + discussion1.created
		System.out.println "discussion2.created: " + discussion2.created
		
		Utils.save(discussion1, true)
		Utils.save(discussion2, true)
		
		assert Discussion.findByName(discussion1.name).id == discussion1.id
		assert Discussion.findByName(discussion2.name).id == discussion2.id
		assert UserGroup.findByName(group.name).id == group.id
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		//first no offset, max set to 10 so all 2 results are included
		Map result = searchService.getDiscussionsList(user, 0, 10)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.userId == user.id
		assert result.listItems.totalDiscussionCount == 2
		assert result.listItems.groupMemberships
		assert result.listItems.groupMemberships.size() == 2 // one for explicit group and group created automatically for each user
		assert result.listItems.groupMemberships[1][0].id == group.id
		assert result.listItems.groupMemberships[1][0].name == group.name
		assert result.listItems.groupMemberships[1][0].fullName == group.fullName
		assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
		assert result.listItems.discussionList
		assert result.listItems.discussionList.size() == 2
		assert result.listItems.discussionList[0].id == discussion2.id
		assert result.listItems.discussionList[0].name == discussion2.name
		assert result.listItems.discussionList[0].userId == discussion2.userId
		assert result.listItems.discussionList[0].isPublic == discussion2.isPublic()
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].created) == Utils.elasticSearchDate(discussion2.created)
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].updated) == Utils.elasticSearchDate(discussion2.updated)
		assert result.listItems.discussionList[0].type == "dis"
		assert result.listItems.discussionPostData[discussion2.id].totalPosts == 0
		
		//limit max to 1, no offset
		result = searchService.getDiscussionsList(user, 0, 1)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.userId == user.id
		assert result.listItems.totalDiscussionCount == 2
		assert result.listItems.groupMemberships
		assert result.listItems.groupMemberships.size() == 2
		assert result.listItems.groupMemberships[1][0].id == group.id
		assert result.listItems.groupMemberships[1][0].name == group.name
		assert result.listItems.groupMemberships[1][0].fullName == group.fullName
		assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
		assert result.listItems.discussionList
		assert result.listItems.discussionList.size() == 1
		assert result.listItems.discussionList[0].id == discussion2.id
		assert result.listItems.discussionList[0].name == discussion2.name
		assert result.listItems.discussionList[0].userId == discussion2.userId
		assert result.listItems.discussionList[0].isPublic == discussion2.isPublic()
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].created) == Utils.elasticSearchDate(discussion2.created)
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].updated) == Utils.elasticSearchDate(discussion2.updated)
		assert result.listItems.discussionList[0].type == "dis"
		assert result.listItems.discussionPostData[discussion2.id].totalPosts == 0
		
		//limit max to 1, offset to second item (index=1)
		result = searchService.getDiscussionsList(user, 1, 1)
		assert result
		assert result.success
		assert result.listItems
		assert result.listItems.userId == user.id
		assert result.listItems.totalDiscussionCount == 2
		assert result.listItems.groupMemberships
		assert result.listItems.groupMemberships.size() == 2
		assert result.listItems.groupMemberships[1][0].id == group.id
		assert result.listItems.groupMemberships[1][0].name == group.name
		assert result.listItems.groupMemberships[1][0].fullName == group.fullName
		assert Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
		assert result.listItems.discussionList
		assert result.listItems.discussionList.size() == 1
		assert result.listItems.discussionList[0].id == discussion1.id
		assert result.listItems.discussionList[0].name == discussion1.name
		assert result.listItems.discussionList[0].userId == discussion1.userId
		assert result.listItems.discussionList[0].isPublic == discussion1.isPublic()
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].created) == Utils.elasticSearchDate(discussion1.created)
		assert Utils.elasticSearchDate(result.listItems.discussionList[0].updated) == Utils.elasticSearchDate(discussion1.updated)
		assert result.listItems.discussionList[0].type == "dis"
		assert result.listItems.discussionPostData[discussion1.id].totalPosts == 0
	}
	
	@Test
	void "Test getDiscussionsList Admin Groups"()
	{
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addAdmin(user)
		groupA.addWriter(user2)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user2)
		
		Discussion discussionReadByUser = Discussion.create(user2, "groupA discussion", groupA, currentTime)
		Discussion discussionNotReadByUser = Discussion.create(user2, "groupB discussion", groupB, currentTime + 1)
		DiscussionPost readByUser1 = discussionReadByUser.createPost(user, "readByUser1", currentTime + 2)
		DiscussionPost readByUser2 = discussionReadByUser.createPost(user, "readByUser2", currentTime + 3)
		
		Utils.save(discussionReadByUser, true)
		Utils.save(discussionNotReadByUser, true)
		
		assert Discussion.findByName(discussionReadByUser.name).id == discussionReadByUser.id
		assert Discussion.findByName(discussionNotReadByUser.name).id == discussionNotReadByUser.id
		assert UserGroup.findByName(groupA.name).id == groupA.id
		assert UserGroup.findByName(groupB.name).id == groupB.id
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		Map result = searchService.getDiscussionsList(user, 0, 10)
		
		assert result.listItems.discussionList[0].isAdmin
		
		groupA.removeAdmin(user.id)
		groupA.addMember(user)
		Utils.save(discussionReadByUser, true)
		Utils.save(groupA, true)
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		result = searchService.getDiscussionsList(user, 0, 10)
		
		assert !(result.listItems.discussionList[0].isAdmin)
	}
}
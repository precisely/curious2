package us.wearecurio.services.integration
import grails.test.spock.IntegrationSpec
import org.grails.plugins.elasticsearch.ElasticSearchAdminService
import org.grails.plugins.elasticsearch.ElasticSearchService
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.GroupMemberReader
import us.wearecurio.model.Model
import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

import java.text.DateFormat

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery

class SearchServiceIntegrationSpec extends IntegrationSpec {
	
	Date currentTime
	UserGroup testGroup
	
	ElasticSearchService elasticSearchService
	def searchService
	ElasticSearchAdminService elasticSearchAdminService
	def elasticSearchHelper

	User user1
	User user2

    def setup() {
		searchService
		searchService.elasticSearchService = elasticSearchService

		elasticSearchHelper.withElasticSearch{ client ->
			client.prepareDeleteByQuery("us.wearecurio.model_v0").setQuery(matchAllQuery()).execute().actionGet()
			client.admin().indices().prepareRefresh().execute().actionGet()
		}
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		GroupMemberReader.executeUpdate("delete GroupMemberReader r")
		UserGroup.executeUpdate("delete UserGroup g")
		
		user1 = User.create(
			[	username:'shane', 
				sex:'F',
				name:'shane macgowen', 
				email:'shane@pogues.com', 
				birthdate:'01/01/1960',
				password:'shanexyz',
				action:'doregister',
				controller:'home'	]
		)
		user2 = User.create(
			[	username:'spider', 
				sex:'F',
				name:'spider stacy', 
				email:'spider@pogues.com', 
				birthdate:'01/01/1961',
				password:'spiderxyz', 
				action:'doregister',
				controller:'home'	]
		)
		
		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		def dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		
		testGroup = UserGroup.create("group", "Curious Group", "Discussion topics for test users",
				[isReadOnly:false, defaultNotify:false])
		testGroup.addWriter(user1)
		
		elasticSearchService.index()
    }

    def cleanup() {
    }

	//@spock.lang.Ignore
    void "Test getSprintsList"() {
 		given: "a newly created sprint"
		def sprint = Sprint.create(new Date(), user1, "Test getSprintsList", Model.Visibility.PUBLIC)
		Utils.save(sprint, true)
		
		when: "elasticSearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintList returns sprint"
		Map result = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 0, 10)
		result
		result.success
		result.listItems
		result.listItems.size() == 1
		result.listItems[0].id == sprint.id
		result.listItems[0].name == sprint.name
	}

	//@spock.lang.Ignore
	void "Test getSprintsList with offset and max"()
	{
		given: "two newly created sprints"
		def sprint1 = Sprint.create(currentTime + 1, user1, "Test getSprintsList 1", Model.Visibility.PUBLIC)
		def sprint2 = Sprint.create(currentTime, user1, "Test getSprintsList 2", Model.Visibility.PUBLIC)
		Utils.save(sprint1, true)
		Utils.save(sprint2, true)
		
		when: "elasticSearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintList returns all sprint"
		Map result = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 0, 10)
		result
		result.success
		result.listItems
		result.listItems.size() == 2
		result.listItems[0].id == sprint1.id
		result.listItems[0].name == sprint1.name
		result.listItems[1].id == sprint2.id
		result.listItems[1].name == sprint2.name
		
		when: "getSprintsList is called with offset 0 and max 1"
		result = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 0, 1)
		
		then: "it returns first sprint only"
		result
		result.success
		result.listItems
		result.listItems.size() == 1
		result.listItems[0].id == sprint1.id
		result.listItems[0].name == sprint1.name

		when: "getSprintsList is called with offset 1 and max 1"
		result = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 1, 1)

		then: "it returns second sprint only"
		result
		result.success
		result.listItems
		result.listItems.size() == 1
		result.listItems[0].id == sprint2.id
		result.listItems[0].name == sprint2.name
	}

	void "Test getPeopleList"() {
		when: "elasticSearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getPeopleList for user 1 returns user 2"
		def result = searchService.getFeed(SearchService.USER_TYPE, user1, 0, 10)
		result
		result.success
		result.listItems
		result.listItems.size() == 1
		result.listItems[0].id == user2.id
		result.listItems[0].username == user2.username
		result.listItems[0].name == user2.name
	}
	
	//@spock.lang.Ignore
	void "Test getPeopleList with offset and max"() {
		given: "a third user in addition to the two for this class"
		def user3 = User.create(
			[	username:'james', 
				sex:'F',
				name:'james fearnley', 
				email:'james@pogues.com', 
				birthdate:'01/01/1990',
				password:'jf999', 
				action:'doregister',
				controller:'home']
		)

		when: "elasticSearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getPeopleList returns all but the first user"
		def result = searchService.getFeed(SearchService.USER_TYPE, user1, 0, 10)
		result
		result.success
		result.listItems
		result.listItems.size() == 2
		result.listItems.find{ u -> u.id == user2.id }
		result.listItems.find{ u -> u.id == user2.id }.username == user2.username
		result.listItems.find{ u -> u.id == user2.id }.name == user2.name
		result.listItems.find{ u -> u.id == user3.id }
		result.listItems.find{ u -> u.id == user3.id }.username == user3.username
		result.listItems.find{ u -> u.id == user3.id }.name == user3.name

		when: "getPeopleList is called with offset 0 and max 1"		
		result = searchService.getFeed(SearchService.USER_TYPE, user1, 0, 1)
		
		then: "it returns user2 or user3 only"
		result
		result.success
		result.listItems
		result.listItems.size() == 1
		(result.listItems.find{ u -> u.id == user2.id } || result.listItems.find{ u -> u.id == user3.id })
		(result.listItems.find{ u -> u.username == user2.username } || result.listItems.find{ u -> u.username == user3.username })
		(result.listItems.find{ u -> u.name == user2.name } || result.listItems.find{ u -> u.name == user3.name })

		when: "getPeopleList is called with offset 1 and max 1"
		result = searchService.getFeed(SearchService.USER_TYPE, user1, 1, 1)

		then: "it returns user2 or user3 only"
		result
		result.success
		result.listItems
		result.listItems.size() == 1
		(result.listItems.find{ u -> u.id == user2.id } || result.listItems.find{ u -> u.id == user3.id })
		(result.listItems.find{ u -> u.username == user2.username } || result.listItems.find{ u -> u.username == user3.username })
		(result.listItems.find{ u -> u.name == user2.name } || result.listItems.find{ u -> u.name == user3.name })
	}

	//@spock.lang.Ignore
	void "Test getDiscussionsList"() {
		when: "given two usergroups, two discussions and two discussion posts"
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addAdmin(user1)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user2)
		
		Discussion discussionReadByUser = Discussion.create(user1, "groupA discussion for getDiscussionList", groupA, currentTime)
		Discussion discussionNotReadByUser = Discussion.create(user2, "groupB discussion for getDiscussionList", groupB, currentTime + 1)
		DiscussionPost readByUser1 = discussionReadByUser.createPost(user1, "readByUser1", currentTime + 2)
		DiscussionPost readByUser2 = discussionReadByUser.createPost(user1, "readByUser2", currentTime + 3)
		
		Utils.save(discussionReadByUser, true)
		Utils.save(discussionNotReadByUser, true)
		
		then: "discussions can be found in db"
		Discussion.findByName(discussionReadByUser.name).id == discussionReadByUser.id
		Discussion.findByName(discussionNotReadByUser.name).id == discussionNotReadByUser.id
		UserGroup.findByName(groupA.name).id == groupA.id
		UserGroup.findByName(groupB.name).id == groupB.id
		
		when: "elasticSearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionList returns both discussions"
		Map result = searchService.getFeed(SearchService.DISCUSSION_TYPE, user1, 0, 10)
		result
		result.success
		result.listItems
		result.listItems.userId == user1.id
		result.listItems.totalDiscussionCount == 1
		//groupMemberships should return 1 row.
		//  first column: UserGroup
		//  second column: DateTime that GroupMemberReader object was created
		result.listItems.groupMemberships
		result.listItems.groupMemberships.size() == 2
		result.listItems.groupMemberships[1][0].id == groupA.id
		result.listItems.groupMemberships[1][0].name == groupA.name
		result.listItems.groupMemberships[1][0].fullName == groupA.fullName
		//need to round milliseconds.
		Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(groupA.created.getTime())
		result.listItems
		result.listItems.size() == 1
		result.listItems[0].id == discussionReadByUser.id
		result.listItems[0].name == discussionReadByUser.name
		result.listItems[0].userId == discussionReadByUser.userId
		result.listItems[0].isPublic == discussionReadByUser.isPublic()
		Utils.elasticSearchDate(result.listItems[0].created) == Utils.elasticSearchDate(discussionReadByUser.created)
		Utils.elasticSearchDate(result.listItems[0].updated) == Utils.elasticSearchDate(discussionReadByUser.updated)
		result.listItems[0].type == "dis"
		result.listItems[0].totalComments == 2
		result.listItems[0].isPlot == false
		result.listItems[0].firstPost
		result.listItems[0].firstPost.message == readByUser1.message
		// the discussion item should show the non-virtual user1 group for UI purposes
		result.listItems[0].groupId == groupA.id
		result.listItems[0].isAdmin
		result.listItems[0].groupName == groupA.fullName
		result.listItems.discussionPostData
		result.listItems.discussionPostData[discussionReadByUser.id].secondPost
		result.listItems.discussionPostData[discussionReadByUser.id].secondPost.message == readByUser2.message
		result.listItems.discussionPostData[discussionReadByUser.id].totalPosts == 2
	}
	
	//@spock.lang.Ignore
	void "Test getDiscussionsList with offset and max"()
	{
		when: "given a usergroup and two discussions"
		UserGroup group = UserGroup.create("curious", "Group", "Discussion topics for Sprint",
				[isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		
		Discussion discussion1 = Discussion.create(user1, "discussion 1", group, currentTime)
		Discussion discussion2 = Discussion.create(user1, "discussion 2", group, currentTime + 1)
		
		System.out.println "discussion1.created: " + discussion1.created
		System.out.println "discussion2.created: " + discussion2.created
		
		Utils.save(discussion1, true)
		Utils.save(discussion2, true)
		
		then: "discussions and groups are in the db"
		Discussion.findByName(discussion1.name).id == discussion1.id
		Discussion.findByName(discussion2.name).id == discussion2.id
		UserGroup.findByName(group.name).id == group.id
		
		when: "elasticSearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionsList returns both discussions"
		//first no offset, max set to 10 so all 2 results are included
		Map result = searchService.getFeed(SearchService.DISCUSSION_TYPE, user1, 0, 10)
		result
		result.success
		result.listItems
		result.listItems.userId == user1.id
		result.listItems.totalDiscussionCount == 2
		result.listItems.groupMemberships
		result.listItems.groupMemberships.size() == 2 // one for explicit group and group created automatically for each user1
		result.listItems.groupMemberships[1][0].id == group.id
		result.listItems.groupMemberships[1][0].name == group.name
		result.listItems.groupMemberships[1][0].fullName == group.fullName
		Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
		result.listItems
		result.listItems.size() == 2
		result.listItems[0].id == discussion2.id
		result.listItems[0].name == discussion2.name
		result.listItems[0].userId == discussion2.userId
		result.listItems[0].isPublic == discussion2.isPublic()
		Utils.elasticSearchDate(result.listItems[0].created) == Utils.elasticSearchDate(discussion2.created)
		Utils.elasticSearchDate(result.listItems[0].updated) == Utils.elasticSearchDate(discussion2.updated)
		result.listItems[0].type == "dis"
		result.listItems.discussionPostData[discussion2.id].totalPosts == 0
		
		when: "getDiscussionList is called with offset 0 and max 1"
		result = searchService.getFeed(SearchService.DISCUSSION_TYPE, user1, 0, 1)
		
		then: "it returns first discussion only"
		result
		result.success
		result.listItems
		result.listItems.userId == user1.id
		result.listItems.totalDiscussionCount == 2
		result.listItems.groupMemberships
		result.listItems.groupMemberships.size() == 2
		result.listItems.groupMemberships[1][0].id == group.id
		result.listItems.groupMemberships[1][0].name == group.name
		result.listItems.groupMemberships[1][0].fullName == group.fullName
		Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
		result.listItems
		result.listItems.size() == 1
		result.listItems[0].id == discussion2.id
		result.listItems[0].name == discussion2.name
		result.listItems[0].userId == discussion2.userId
		result.listItems[0].isPublic == discussion2.isPublic()
		Utils.elasticSearchDate(result.listItems[0].created) == Utils.elasticSearchDate(discussion2.created)
		Utils.elasticSearchDate(result.listItems[0].updated) == Utils.elasticSearchDate(discussion2.updated)
		result.listItems[0].type == "dis"
		result.listItems.discussionPostData[discussion2.id].totalPosts == 0
		
		when: "getDiscussionList is called with offset 1 and max 1"
		result = searchService.getFeed(SearchService.DISCUSSION_TYPE, user1, 1, 1)
		
		then: "it returns second discussion only"
		result
		result.success
		result.listItems
		result.listItems.userId == user1.id
		result.listItems.totalDiscussionCount == 2
		result.listItems.groupMemberships
		result.listItems.groupMemberships.size() == 2
		result.listItems.groupMemberships[1][0].id == group.id
		result.listItems.groupMemberships[1][0].name == group.name
		result.listItems.groupMemberships[1][0].fullName == group.fullName
		Utils.elasticSearchRoundMs(result.listItems.groupMemberships[1][1].getTime()) >= Utils.elasticSearchRoundMs(group.created.getTime())
		result.listItems
		result.listItems.size() == 1
		result.listItems[0].id == discussion1.id
		result.listItems[0].name == discussion1.name
		result.listItems[0].userId == discussion1.userId
		result.listItems[0].isPublic == discussion1.isPublic()
		Utils.elasticSearchDate(result.listItems[0].created) == Utils.elasticSearchDate(discussion1.created)
		Utils.elasticSearchDate(result.listItems[0].updated) == Utils.elasticSearchDate(discussion1.updated)
		result.listItems[0].type == "dis"
		result.listItems.discussionPostData[discussion1.id].totalPosts == 0
	}
	
	//@spock.lang.Ignore
	void "Test getDiscussionsList Admin Groups"() {
		when: "given a group with an admin, a group without an admin, two discussions and two posts"
		UserGroup groupA = UserGroup.create("curious A", "Group A", "Discussion topics for Sprint A",
				[isReadOnly:false, defaultNotify:false])
		groupA.addAdmin(user1)
		groupA.addWriter(user2)
		
		UserGroup groupB = UserGroup.create("curious B", "Group B", "Discussion topics for Sprint B",
				[isReadOnly:false, defaultNotify:true])
		groupB.addMember(user2)
		
		Discussion discussionReadByUser = Discussion.create(user2, "groupA discussion for getDiscussionsList Admin", groupA, currentTime)
		Discussion discussionNotReadByUser = Discussion.create(user2, "groupB discussion for getDiscussionsList Admin", groupB, currentTime + 1)
		DiscussionPost readByUser1 = discussionReadByUser.createPost(user1, "readByUser1", currentTime + 2)
		DiscussionPost readByUser2 = discussionReadByUser.createPost(user1, "readByUser2", currentTime + 3)
		
		Utils.save(discussionReadByUser, true)
		Utils.save(discussionNotReadByUser, true)
		
		then: "discussions and usergrops are in the db"
		Discussion.findByName(discussionReadByUser.name).id == discussionReadByUser.id
		Discussion.findByName(discussionNotReadByUser.name).id == discussionNotReadByUser.id
		UserGroup.findByName(groupA.name).id == groupA.id
		UserGroup.findByName(groupB.name).id == groupB.id
		
		when: "elasticSearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getDiscussionList returns isAdmin true"
		Map result = searchService.getFeed(SearchService.DISCUSSION_TYPE, user1, 0, 10)
		
		result.listItems[0].isAdmin
		
		when: "user is removed as admin, elasticsearch is re-indexed and getDiscussionsList is called"
		groupA.removeAdmin(user1.id)
		groupA.addMember(user1)
		Utils.save(discussionReadByUser, true)
		Utils.save(groupA, true)
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		result = searchService.getFeed(SearchService.DISCUSSION_TYPE, user1, 0, 10)
		
		then: "getDiscussionsList returns isAdmin false"
		!(result.listItems[0].isAdmin)
	}
}

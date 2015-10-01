package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import org.elasticsearch.action.count.CountResponse
import org.elasticsearch.action.search.SearchResponse

import org.elasticsearch.index.query.functionscore.*
import static org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.index.query.QueryBuilders

import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.sort.*

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Entry
import us.wearecurio.model.GroupMemberReader
import us.wearecurio.model.Model
import us.wearecurio.model.Model.Visibility;
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.UserActivity
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils

class SearchService {
	
	private static def log = LogFactory.getLog(this)

	def elasticSearchService
	def elasticSearchHelper
	
	static transactional = true
	
	static final Long DISCUSSION_TYPE 			= 1
	static final Long SPRINT_TYPE				= 2
	static final Long DISCUSSION_POST_TYPE		= 4
	static final Long USER_TYPE					= 8
	
	static final Long CREATE_ACTIVITY_TYPE		= 1
	static final Long FOLLOW_ACTIVITY_TYPE		= 2
	
	static SearchService service
	
	static set(SearchService s) {
		service = s
	}
	
	static SearchService get() {
		service
	}
	
	void index(Object obj) {
		elasticSearchService.index(obj)
	}
	
	static enum Role {
		SPRINT_READER(0),
		SPRINT_ADMIN(1),
		DISCUSSION_OWNER(2),
		DISCUSSION_ADMIN(3),
		DISCUSSION_READER(4),
		USER_FOLLOWER(5)
		
		final Integer id
		
		static Role get(int id) {
			switch(id) {
				case 0:
					return SPRINT_READER
				case 1:
					return SPRINT_ADMIN
				case 2:
					return DISCUSSION_OWNER
				case 3:
					return DISCUSSION_ADMIN
				case 4:
					return DISCUSSION_READER
				case 5:
					return USER_FOLLOWER
				default:
					return DISCUSSION_READER
			}
		}
		
		Role(int id) {
			this.id = id
		}
		
		int getId() {
			return id
		}
	}
	
	Boolean checkVisibility( Role role, Model.Visibility visibility ) {
		switch(role) {
			case Role.SPRINT_READER:
				return (visibility == Model.Visibility.PUBLIC)
			case Role.SPRINT_ADMIN:
				return (visibility == Model.Visibility.PUBLIC)
			case Role.DISCUSSION_ADMIN:
				return true
			case Role.DISCUSSION_READER:
				return (visibility == Model.Visibility.PUBLIC)
			case Role.USER_FOLLOWER:
				return (visibility == Model.Visibility.PUBLIC)
		}
		
		return false
	}
	
	static def getVisibilityForDiscussion( Role role ) {
		def visibility = []
		visibility << Model.Visibility.PUBLIC
		
		switch (role) {
			case Role.DISCUSSION_READER:
				visibility << Model.Visibility.UNLISTED
				break;
			case Role.DISCUSSION_ADMIN:
				visibility << Model.Visibility.UNLISTED
				visibility << Model.Visibility.NEW
				break;
			case Role.DISCUSSION_OWNER:
				visibility << Model.Visibility.PRIVATE
				visibility << Model.Visibility.UNLISTED
				visibility << Model.Visibility.NEW
				break;
		}
		
		return visibility
	}

	static def getVisibilityForSprint( Role role ) {
		def visibility = []
		
		switch (role) {
			case Role.USER_FOLLOWER:
				visibility << Model.Visibility.PUBLIC
				break;
			case Role.SPRINT_READER:
				visibility << Model.Visibility.PUBLIC
				visibility << Model.Visibility.UNLISTED
				break;
			case Role.SPRINT_ADMIN:
				visibility << Model.Visibility.PUBLIC
				visibility << Model.Visibility.PRIVATE
				visibility << Model.Visibility.UNLISTED
				visibility << Model.Visibility.NEW
				break;
		}
		
		return visibility
	}
	
	String getDiscussionActivityQueryString(User user, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def followingUserUserGroupIds = followedUsers.collect{ it.virtualUserGroupIdFollowers }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def readerGroupsSansFollowingGroups = (readerGroups.collect{ it[0].id } - followingUserUserGroupIds) - followingSprintUserGroupIds
						
		def visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_OWNER).collect{ it.toString()})
		discussionQueries << ("(objectUserId:" + user.id + " AND objectVisibility:" + visibilitiesOr + ")")
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(readerGroupsSansFollowingGroups)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:" + groupIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(adminGroups.collect{ it[0].id })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:" + groupIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.USER_FOLLOWER).collect{ it.toString()})
		groupIdsOr =  Utils.orifyList(followingUserUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:" + groupIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_READER).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followingSprintUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:" + groupIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followedSprints.find{ it.userId == user.id }.collect{ it.virtualGroupId })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:" + groupIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}

		def tags = user.getInterestTags()
		if (tags?.size() > 0) {
			discussionQueries << ("(objectDescription:" + Utils.orifyList(tags.collect{ it.description }) + " AND objectVisibility:PUBLIC)")
		}
		
		return "(${Utils.orifyList(discussionQueries)} AND objectType:DISCUSSION AND ((activityType:CREATE) OR (activityType:(ADD OR REMOVE) AND otherType:(ADMIN OR READER)) OR (activityType:(COMMENT OR UNCOMMENT) AND otherType:DISCUSSION_POST)))"
	}
	
	String getSprintActivityQueryString(User user, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def sprintQueries = []
		
		def followingUserIds = followedUsers.collect{ it.id }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def sprintReaderGroupIds = readerGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		def sprintAdminGroupIds = adminGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		
		def visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.SPRINT_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(sprintReaderGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			sprintQueries << ("(sprintVirtualGroupId:" + groupIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(sprintAdminGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			sprintQueries << ("(sprintVirtualGroupId:" + groupIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.USER_FOLLOWER).collect{ it.toString()})
		def userIdsOr =  Utils.orifyList(followingUserIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && userIdsOr != null && userIdsOr != "") {
			sprintQueries << ("(objectUserId:" + userIdsOr + " AND objectVisibility:" + visibilitiesOr + ")")
		}
		
		def tags = user.getInterestTags()
		if (tags?.size() > 0) {
			def orTags = Utils.orifyList(tags.collect{ it.description })
			sprintQueries << ("((objectDescription:" + orTags + " OR sprintDescription:" + orTags + " OR sprintTagName:" + orTags + ") AND objectVisibility:PUBLIC)")
		}
		
		if (sprintQueries.size() > 0) {
			return "(${Utils.orifyList(sprintQueries)} AND objectType:SPRINT AND activityType:(CREATE OR DELETE))"
		} else {
			return ""
		}
	}

	String getDiscussionsCreatedQueryString(User user) {
		return  "(userId:" + user.id + " AND activityType:CREATE AND objectType:DISCUSSION)"
	}

	String getDiscussionPostsCreatedQueryString(User user) {
		return  "(userId:" + user.id + " AND activityType:CREATE AND objectType:DISCUSSION_POST)"
	}	
	
	String getSprintsCreatedQueryString(User user) {
		return  "(userId:" + user.id + " AND activityType:CREATE AND objectType:SPRINT)"
	}
	
	String getDiscussionsFollowedQueryString(User user, String readerGroupsOr)	{
		return  "(groupIds:" + readerGroupsOr + ")"
	}
	
	String getSprintsFollowedQueryString(User user, String readerGroupsOr) {
		return  "(virtualGroupId:" + readerGroupsOr + ")"
	}
	
	String getUsersFollowedQueryString(User user, String readerGroupsOr) {
		return  "(virtualUserGroupIdFollowers:" + readerGroupsOr + ")"
	}
	
	String getDiscussionSearchQueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def followingUserUserGroupIds = followedUsers.collect{ it.virtualUserGroupIdFollowers }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def readerGroupsSansFollowingGroups = (readerGroups.collect{ it[0].id } - followingUserUserGroupIds) - followingSprintUserGroupIds
						
		def visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_OWNER).collect{ it.toString()})
		discussionQueries << ("(discussionUserId:${user.id} AND discussionVisibility:${visibilitiesOr})")
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(readerGroupsSansFollowingGroups)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:${groupIdsOr} AND discussionVisibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(adminGroups.collect{ it[0].id })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:${groupIdsOr} AND discussionVisibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.USER_FOLLOWER).collect{ it.toString()})
		groupIdsOr =  Utils.orifyList(followingUserUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:${groupIdsOr} AND discussionVisibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_READER).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followingSprintUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:${groupIdsOr} AND discussionVisibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followedSprints.find{ it.userId == user.id }.collect{ it.virtualGroupId })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(discussionGroupIds:${groupIdsOr} AND discussionVisibility:${visibilitiesOr})")
		}
		
		//how to specify type? if not, sprints or users could be returned when matching userid and visibility
		//how to get posts? ugh!
		return "((${Utils.orifyList(discussionQueries)} AND ((discussionName:(${query})) OR (message:(${query})))) OR (discussionVisibility:PUBLIC AND ((discussionName:(${query})) OR (message:(${query})))))"
	}

	String getSprintSearchQueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def sprintQueries = []
		
		def followingUserIds = followedUsers.collect{ it.id }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def sprintReaderGroupIds = readerGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		def sprintAdminGroupIds = adminGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		
		def visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.SPRINT_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(sprintReaderGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			sprintQueries << ("(virtualGroupId:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(sprintAdminGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			sprintQueries << ("(virtualGroupId:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.USER_FOLLOWER).collect{ it.toString()})
		def userIdsOr =  Utils.orifyList(followingUserIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && userIdsOr != null && userIdsOr != "") {
			sprintQueries << ("(userId:${userIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		if (sprintQueries.size() > 0) {
			return "(${Utils.orifyList(sprintQueries)} AND name:(${query}))"
		} else {
			return ""
		}
	}

	String getUserSearchQueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		return "(name:(${query}))"
	}

	private Map getCreateActivity(User user, Long type, int offset = 0, int max = 10) {
		if (user == null) {
			return [listItems: false, success: false]
		}
		
		def queries = []
		def query
		if ((type & DISCUSSION_TYPE) > 0) {
			query = getDiscussionsCreatedQueryString(user)
			if (query != null && query != "") {
				queries << query
			}
		}
		
		if ((type & SPRINT_TYPE) > 0) {
			query = getSprintsCreatedQueryString(user)
			if (query != null && query != "") {
				queries << query
			}
		}

		if ((type & DISCUSSION_POST_TYPE) > 0) {
			query = getDiscussionPostsCreatedQueryString(user)
			if (query != null && query != "") {
				queries << query
			}
		}

		def result = [listItems: [], success: true]
		
		if (queries.size() > 0) {
			def ua = UserActivity.search( 
				searchType:'query_and_fetch', 
				sort:'created', 
				order:'desc', 
				size: max.toString(), 
				from: offset.toString() 
			) {
				query_string(query: Utils.orifyList(queries))
			}

			if (ua.searchResults.size() > 0) {
				def activities = ua.searchResults.collect {
					[
						userId				: it.userId,
						userName			: it.userName,
						type 				: UserActivity.getTypeString(it.typeId),
						objectId			: it.objectId,
						objectDescription	: it.objectDescription,
						otherId				: it.otherId,
						otherDescription	: it.otherDescription,
						created				: it.created,
						score				: null
					]
				}
				
				return [listItems: activities, success: true]
			}
		}
		
		return result
	}
	
	Map getDiscussionsCreated(User user, int offset = 0, int max = 10) {
		return getCreateActivity(user, DISCUSSION_TYPE, offset, max)
	}
	
	Map getDiscussionPostsCreated(User user, int offset = 0, int max = 10) {
		return getCreateActivity(user, DISCUSSION_POST_TYPE, offset, max)
	}

	Map getSprintsCreated(User user, int offset = 0, int max = 10) {
		return getCreateActivity(user, SPRINT_TYPE, offset, max)
	}
	
	Map getAllCreated(User user, int offset = 0, int max = 10) {
		return getCreateActivity(user, DISCUSSION_TYPE | DISCUSSION_POST_TYPE | SPRINT_TYPE, offset, max)
	}
	
	private Map getFollowActivity(User user, Long type, int offset = 0, int max = 10) {
		if (user == null) {
			return [listItems: false, success: false]
		}
		
		def readerGroups = UserGroup.getGroupsForReader(user.id)
		def readerGroupsOr = Utils.orifyList(readerGroups.collect{ it[0].id })
		
		if (readerGroupsOr == null || readerGroupsOr == "") {
			return [listItems: [], success: true]
		}
		
		def queries = []
		def query
		if ((type & DISCUSSION_TYPE) > 0) {
			query = getDiscussionsFollowedQueryString(user, readerGroupsOr)	
			if (query != null && query != "") {
				queries << query
			}
		}
		
		if ((type & SPRINT_TYPE) > 0) {
			query = getSprintsFollowedQueryString(user, readerGroupsOr)
			if (query != null && query != "") {
				queries << query
			}
		}

		if ((type & USER_TYPE) > 0) {
			query = getUsersFollowedQueryString(user, readerGroupsOr)
			if (query != null && query != "") {
				queries << query
			}
		}		

		def result = [listItems: [], success: true]
		if (queries.size() > 0) {
			elasticSearchHelper.withElasticSearch{ client ->
				SearchResponse sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussion", "sprint", "user")
					.setQuery(queryString(Utils.orifyList(queries)))
					.setExplain(false)
					.setSize(max)
					.setFrom(offset)
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.DESC))
					.execute()
					.actionGet()

				if (sr.hits.hits.size() > 0) {
					def activities = []
					for( def hit : sr.hits.hits ) {
						switch(hit.getType()) {
							case "discussion":
								activities << [
									type 	: "dis",
									id 		: hit.getId(),
									name 	: hit.getSource().name
								]
								break;
							case "sprint":
								activities << [
									type	: "spr",
									id 		: hit.getId(),
									name	: hit.getSource().name
								]
								break;
							case "user":
								activities << [
									type 	: "usr",
									id 		: hit.getId(),
									name 	: hit.getSource().name
								]
								break;
						}
					}
					
					result = [listItems: activities, success: true]
				}
			}
		}
		
		return result
	}
		
	Map getDiscussionsFollowed(User user, int offset = 0, int max = 10) {
		return getFollowActivity(user, DISCUSSION_TYPE, offset, max)
	}
	
	Map getSprintsFollowed(User user, int offset = 0, int max = 10) {
		return getFollowActivity(user, SPRINT_TYPE, offset, max)
	}
	
	Map getUsersFollowed(User user, int offset = 0, int max = 10) {
		return getFollowActivity(user, USER_TYPE, offset, max)
	}
	
	Map getAllFollowed(User user, int offset = 0, int max = 10) {
		return getFollowActivity(user, DISCUSSION_TYPE | SPRINT_TYPE | USER_TYPE, offset, max)
	}
		
	private Map getActivity(User user, Long type, int offset = 0, int max = 10) {
		if (user == null) {
			return [listItems: false, success: false]
		}
		
		def readerGroups = UserGroup.getGroupsForReader(user.id)
		def adminGroups = UserGroup.getGroupsForAdmin(user.id)
		def followedUsers = User.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualUserGroupIdFollowers: " + Utils.orifyList(readerGroups.collect{ it[0].id }))
		}
		def followedSprints = Sprint.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualGroupId: " + Utils.orifyList(readerGroups.collect{ it[0].id }))
		}

		def queries = []
		def query
		if ((type & DISCUSSION_TYPE) > 0) {
			query = getDiscussionActivityQueryString(user, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (query != null && query != "") {
				queries << query
			}
		}
		
		if ((type & SPRINT_TYPE) > 0) {
			query = getSprintActivityQueryString(user, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (query != null && query != "") {
				queries << query
			}
		}
		
		def result = [listItems: [], success: true]
		if (queries.size() > 0) {			
			elasticSearchHelper.withElasticSearch{ client ->
				FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
				fsqb.add(ScoreFunctionBuilders.gaussDecayFunction("created", "1d"))	
				
				SearchResponse sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("userActivity")
					.setQuery(fsqb)
					.setExplain(false)
					.setSize(max)
					.setFrom(offset)
					.execute()
					.actionGet()

				if (sr.hits.hits.size() > 0) {
					def activities = sr.hits.hits.collect { 
						[
							userId				: it.getSource().userId,
							userName			: it.getSource().userName,
							type 				: UserActivity.getTypeString(it.getSource().typeId),
							objectId			: it.getSource().objectId,
							objectDescription	: it.getSource().objectDescription,
							otherId				: it.getSource().otherId,
							otherDescription	: it.getSource().otherDescription,
							created				: it.getSource().created,
							score				: it.getScore()
						]
					}
					
					result = [listItems: activities, success: true]
				}
			}
		}
		
		return result
	}
		
	Map getDiscussionActivity(User user, int offset = 0, int max = 10) {
		return getActivity(user, DISCUSSION_TYPE, offset, max)
	}
	
	Map getSprintActivity(User user, int offset = 0, int max = 10) {
		return getActivity(user, SPRINT_TYPE, offset, max)
	}
	
	Map getUserActivity(User user, int offset = 0, int max = 10) {
		return [listItems: false, success: false]
	}
	
	Map getAllActivity(User user, int offset = 0, int max = 10) {
		return getActivity(user, DISCUSSION_TYPE | SPRINT_TYPE, offset, max)
	}

	private Map search(User user, Long type, String query, int offset = 0, int max = 10) {
		if (user == null) {
			return [listItems: false, success: false]
		}
						
		String queryAnd = query.replaceAll("[oO][rR]","").replaceAll("[aA][nN][dD]"," ").replaceAll("\\s+", " AND ")
		def readerGroups = UserGroup.getGroupsForReader(user.id)
		def adminGroups = UserGroup.getGroupsForAdmin(user.id)
		def followedUsers = User.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualUserGroupIdFollowers: " + Utils.orifyList(readerGroups.collect{ it[0].id }))
		}
		def followedSprints = Sprint.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualGroupId: " + Utils.orifyList(readerGroups.collect{ it[0].id }))
		}

		def queries = []
		def queryString
		if ((type & DISCUSSION_TYPE) > 0) {
			queryString = getDiscussionSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (queryString != null && queryString != "") {
				queries << queryString
			}
		}
		
		if ((type & SPRINT_TYPE) > 0) {
			queryString = getSprintSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (queryString != null && queryString != "") {
				queries << query
			}
		}
		
		if ((type & USER_TYPE) > 0) {
			queryString = getUserSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (queryString != null && queryString != "") {
				queries << query
			}
		}
		
		def result = [listItems: [], success: true]
		if (queries.size() > 0) {
			println "Utils.orifyList(queries): " + Utils.orifyList(queries)
			elasticSearchHelper.withElasticSearch{ client ->
				FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
				fsqb.add(ScoreFunctionBuilders.gaussDecayFunction("created", "1d"))
				
				SearchResponse sr = client
					.prepareSearch("us.wearecurio.model_v0")
					.setTypes("discussionPost", "user", "sprint")
					.setQuery(fsqb)
					.setExplain(false)
					.setSize(max)
					.setFrom(offset)
					.execute()
					.actionGet()

				if (sr.hits.hits.size() > 0) {
					def activities = sr.hits.hits.collect {
						[
							userId				: it.getSource().userId,
							userName			: it.getSource().userName,
							type 				: UserActivity.getTypeString(it.getSource().typeId),
							objectId			: it.getSource().objectId,
							objectDescription	: it.getSource().objectDescription,
							otherId				: it.getSource().otherId,
							otherDescription	: it.getSource().otherDescription,
							created				: it.getSource().created,
							score				: it.getScore()
						]
					}
					
					result = [listItems: activities, success: true]
				}
			}
		}
		
		return result
	}
				
	Map searchDiscussions(User user, String query, int offset = 0, int max = 10) {
		return search(user, DISCUSSION_TYPE, query, offset, max)
	}
	
	Map searchSprints(User user, String query, int offset = 0, int max = 10) {
		return search(user, SPRINT_TYPE, query, offset, max)
	}
	
	Map searchUsers(User user, String query, int offset = 0, int max = 10) {
		return search(user, USER_TYPE, query, offset, max)
	}
	
	Map searchAll(User user, String query, int offset = 0, int max = 10) {
		return search(user, DISCUSSION_TYPE | SPRINT_TYPE | USER_TYPE, query, offset, max)
	}
			
	Map getSprintsList(User user, int offset, int max) {
		try {
						
			def results = Sprint.search(searchType:'query_and_fetch', sort:'created', order:'desc', size: max.toString(), from: offset.toString() ) {
				query_string(query:  "userId:" + user.id)
			}
	
			if (results == null || results.searchResults == null || results.searchResults.size() < 1) {
				return [listItems: false, success: true]
			}
			
			Map model = [sprintList: results.searchResults*.getJSONDesc()]
			return [listItems: model, success: true]
			
		} catch (RuntimeException e) {
			log.debug(e.message)
			return [listItems: false, success: false]
		}
	}
	
	Map getDiscussionsList(User user, int offset, int max, def groupIds = null) {
		log.debug "getDiscussionsList: user:" + user + " offset:" + offset + " max:" + max + " groupIds:" + groupIds
		try {
			def userReaderGroups = UserGroup.getGroupsForReader(user)
			if (userReaderGroups == null || userReaderGroups.size() < 1) {
				return [listItems: false, success: false]
			}
			
			String groupIdsOr = userReaderGroups.collect{ it[0].id }.findAll{ groupIds == null ? true : it in groupIds }.join(" OR ")
			if (userReaderGroups.size() > 1) {
				groupIdsOr = "(" + groupIdsOr + ")"
			}
			
			def totalDiscussionCount = 0
			elasticSearchHelper.withElasticSearch{ client ->
				CountResponse cr = client
						.prepareCount("us.wearecurio.model_v0")
						.setTypes("discussion")
						.setQuery(queryString("groupIds:" + groupIdsOr))
						.execute()
						.actionGet()
				
				totalDiscussionCount = cr.count
			}
			
			if (totalDiscussionCount < 1) {
				return [listItems: false, success: true]
			}
			
			def readerGroupDiscussions = Discussion.search(searchType:'query_and_fetch', sort:'created', order:'desc', size: max.toString(), from: offset.toString() ) {
				query_string(query:  "groupIds:" + groupIdsOr)
			}
			
			if (readerGroupDiscussions == null || readerGroupDiscussions.searchResults == null || readerGroupDiscussions.searchResults.size() < 1) {
				return [listItems: false, success: false]
			}
			
			String discussionIdsOr = readerGroupDiscussions.searchResults.collect{ it.id }.join(" OR ")
			if (discussionIdsOr.size() > 1) discussionIdsOr = "(" + discussionIdsOr + ")"
			
			def adminGroupIds = user.getAdminGroupIds()
			
			return elasticSearchHelper.withElasticSearch{ client ->
				SearchResponse sr = client
						.prepareSearch("us.wearecurio.model_v0")
						.setTypes("discussionPost")
						.setQuery(
						boolQuery()
						.must(
						queryString("discussionId:" + discussionIdsOr)
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
						.setSize(2)  // number of post documents to show PER discussion id
						.addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
						)
						)
						.execute()
						.actionGet()
				
				Map model = [
					userId: user.id,
					groupMemberships: userReaderGroups,
					totalDiscussionCount: totalDiscussionCount,
					discussionList: [],
					discussionPostData: [:]
				]
				
				for (def d : readerGroupDiscussions.searchResults ) {
					log.debug "readerGroupDiscussion: " + d.toString()
					model["discussionList"] << [
						id: d.id,
						hash: d.hash,
						name: d.name,
						userId: d.userId,
						userName: User.get(d.userId).username,
						isPublic: d.isPublic(),
						created: d.created,
						updated: d.updated,
						type: "dis"
					]
					
					def discussionItem = model["discussionList"].find{ it.id == d.id}
					
					def bucket = sr.getAggregations().get("by_discussionId").getBuckets().find{ b -> b.key == d.id.toString() }
					def firstPost
					if (bucket) {
						def secondPost
						def hits = bucket.getAggregations().get("top_hits").getHits().getHits()
						if (hits.size() < 1 || hits.size() > 2 || (d.firstPostId == null || hits[0].id != d.firstPostId.toString())) {
							//should always have either 1 or 2 hits in agg list
							//first hit should always match first postid of this discussion
							return [listItems: false, success: false] // better to throw exception?
						}
						
						firstPost = hits[0].getSource()
						if (hits.size() > 1 ) {
							secondPost = hits[1].getSource()
						}
						
						model["discussionPostData"][d.id] = [
							secondPost: secondPost,
							totalPosts: bucket.docCount
						]
						discussionItem["totalComments"] = bucket.docCount
					} else {
						model["discussionPostData"][d.id] = [
							secondPost: null,
							totalPosts: 0
						]
						discussionItem["totalComments"] = 0
					}
					
					if (firstPost) {
						discussionItem["isPlot"] = (firstPost.plotDataId != null && firstPost.plotDataId > 0)
						discussionItem["firstPost"] = firstPost
					}
					
					//eventually, this code will need to be refactored to account for discussions with multiple groups
					//for now, arbitrarily grab first group for which user is an admin and which is associated with this discussion
					//if user is not admin of any group associated with this discussion, then arbitrarily select first group associated with this discussion.
					//def groupIds = d.groupIds
					if (d.groupIds == null || d.groupIds.length < 1) {
						//all discussions should have at least one group
						return [listItems: false, success: false] // better to throw exception?
					}
					
					discussionItem["isAdmin"] = (d.userId == user.id)
					boolean firstGroup = true
					Long anyGid = null
					String anyGName = null
					for (Long gid : d.groupIds) {
						//NOT ALL of discussion's groups are necessarily included in userReaderGroups
						//BUT at least ONE of the discussion's groupIds MUST be, use first non-hidden user group
						def groups = userReaderGroups.find{ it[0].id == gid }
						if (groups && groups.length > 0) {
							UserGroup group = groups[0]
							anyGid = gid
							anyGName = group.fullName
							if (firstGroup && (!group.isHidden)) {
								//use first group data, unless admin group is found further down in the list.
								discussionItem["groupId"] = gid
								discussionItem["groupName"] = group.fullName
								if (model["discussionList"].find{ it.id == d.id}["isAdmin"]) {
									break //admin already true, so no need to continue
								}
								firstGroup = false
							}
							if (d.userId == user.id || (adminGroupIds != null && adminGroupIds.contains(gid))) {
								//found first admin group
								if (!firstGroup) {
									//override first group data with current, non-first-group-but-first-admin-group data
									discussionItem["groupId"] = gid
									discussionItem["groupName"] = groups[0].fullName
								}
								discussionItem["isAdmin"] = true
								break // found admin, so no need to proceed
							}
						}
					}
					// make sure at least one group ID and name are set for discussion even if they are hidden/user-virtual
					if (!discussionItem["groupId"]) {
						discussionItem["groupId"] = anyGid
						discussionItem['groupName'] = anyGName
					}
				}
				
				log.debug "Done processing search result information: " + model
				
				return [listItems: model, success: true]
			}
		
		} catch (RuntimeException e) {
			log.debug(e.message)
			return [listItems: false, success: false]
		}
	}
	
	Map getPeopleList(User user, int offset, int max) {
		try {
			def results = User.search(searchType:'query_and_fetch', sort:'created', order:'desc', size: max.toString(), from: offset.toString() ) {
				bool {
					must {
						query_string(query: "virtual:false")
					}
					must_not {
						ids(values: [user.id.toString()])
					}
				}
			}
			
			if (results == null || results.searchResults == null || results.searchResults.size() < 1) {
				return [listItems: false, success: true]
			}
			
			return [listItems: results.searchResults*.getPeopleJSONDesc(), success: true]
		
		} catch (RuntimeException e) {
			log.debug(e.message)
			return [listItems: false, success: false]
		}
	}
}

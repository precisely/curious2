package us.wearecurio.services
import org.apache.commons.logging.LogFactory
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import us.wearecurio.model.GroupMemberDiscussion
import us.wearecurio.model.Model
import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils


import org.elasticsearch.action.count.CountResponse

import static org.elasticsearch.index.query.FilterBuilders.andFilter
import static org.elasticsearch.index.query.FilterBuilders.queryFilter
import static org.elasticsearch.index.query.FilterBuilders.termsFilter
import static org.elasticsearch.index.query.FilterBuilders.typeFilter
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery
import static org.elasticsearch.index.query.QueryBuilders.queryString
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.weightFactorFunction
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.linearDecayFunction
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.randomFunction

class SearchService {
	
	private static def log = LogFactory.getLog(this)

	def elasticSearchService
	def elasticSearchHelper
	def messageSource
	
	static transactional = true

	/*
	 * Any changes made here to these types must be reflected in the client side code. See "feeds.js" file.
	 */
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
		USER_FOLLOWER(5),
		SPRINT_OWNER(6)
		
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
				case 6:
					return SPRINT_OWNER	
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
			case Role.SPRINT_OWNER:
				visibility << Model.Visibility.PUBLIC
				visibility << Model.Visibility.PRIVATE
				visibility << Model.Visibility.UNLISTED
				visibility << Model.Visibility.NEW
				break;
		}
		
		return visibility
	}

	private Map toJSON(def hit, List adminDiscussionIds) {
		switch (hit.type) {
			case "discussion":
				return [
					type: "dis",
					id: hit.id.toLong(),
					hash: hit.source.hash,
					name: hit.source.name,
					userHash: hit.source.userHash,
					publicUserName: hit.source.publicUserName,
					userName: hit.source.username,
					userAvatarURL: hit.source.userAvatarURL,
					isPublic: (hit.source.visibility == "PUBLIC"),
					created: hit.source.created,
					updated: hit.source.updated,
					totalComments: hit.source.postCount,
					isPlot: hit.source.isFirstPostPlot,
					firstPost: hit.source.firstPostId,
					isAdmin: adminDiscussionIds.contains(hit.id.toLong()),
					groupId: null,
					groupName: null,
					score: hit.score,
					firstPostMessage: hit.source.firstPostMessage,
					posts: hit.source.posts,
				]
			case "sprint":
				return [
					type: "spr",
					id: hit.id.toLong(),
					hash: hit.source.hash,
					name: hit.source.name,
					userId: hit.source.userId,
					description: hit.source.description,
					totalParticipants: 0,
					totalTags: 0,
					virtualUserId: hit.source.virtualUserId,
					virtualGroupId: hit.source.virtualGroupId,
					virtualGroupName: hit.source.virtualGroupName,
					created: hit.source.created,
					updated: hit.source.updated,
					score: hit.score,
				]
			case "user":
				return [
					type: "usr",
					hash: hit.source.hash,
					remindEmail: hit.source.remindEmail,
					username: hit.source.username,
					name: hit.source.publicName,
					sex: hit.source.sex,
					birthdate: hit.source.birthdate,
					website: hit.source.created,
					notifyOnComments: hit.source.notifyOnComments,
					created: hit.source.created,
					score: hit.score,
					interestTagsString: hit.source.interestTagsString,
				]
		}
		
		return [:]
	}

	String getDiscussionActivityQueryString(User user, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def followingUserUserGroupIds = followedUsers.collect{ it.virtualUserGroupIdFollowers }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def readerGroupsSansFollowingGroups = (readerGroups.collect{ it[0].id } - followingUserUserGroupIds) - followingSprintUserGroupIds
						
		def visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_OWNER).collect{ it.toString()})
		discussionQueries << ("(userId:${user.id} AND visibility:${visibilitiesOr})")
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(readerGroupsSansFollowingGroups)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(adminGroups.collect{ it[0].id })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.USER_FOLLOWER).collect{ it.toString()})
		groupIdsOr =  Utils.orifyList(followingUserUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_READER).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followingSprintUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followedSprints.find{ it.userId == user.id }.collect{ it.virtualGroupId })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		return "(${Utils.orifyList(discussionQueries)} AND _type:discussion and hasRecentPost:true)"
	}
	
	String getSprintActivityQueryString(User user, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def sprintQueries = []
		
		def followingUserIds = followedUsers.collect{ it.id }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def sprintReaderGroupIds = readerGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		def sprintAdminGroupIds = adminGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		
		def visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.SPRINT_OWNER).collect{ it.toString()})
		sprintQueries << ("(userId:${user.id} AND visibility:${visibilitiesOr})")
		
		visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.SPRINT_READER).collect{ it.toString()})
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
			return "(${Utils.orifyList(sprintQueries)} AND _type:sprint AND hasRecentPost:true)"
		} else {
			return ""
		}
	}

	String getDiscussionSearchGroup1QueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def followingUserUserGroupIds = followedUsers.collect{ it.virtualUserGroupIdFollowers }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def readerGroupsSansFollowingGroups = (readerGroups.collect{ it[0].id } - followingUserUserGroupIds) - followingSprintUserGroupIds
						
		def visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_OWNER).collect{ it.toString()})
		discussionQueries << ("(userId:${user.id} AND visibility:${visibilitiesOr})")
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(readerGroupsSansFollowingGroups)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(adminGroups.collect{ it[0].id })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.USER_FOLLOWER).collect{ it.toString()})
		groupIdsOr =  Utils.orifyList(followingUserUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_READER).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followingSprintUserGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followedSprints.findAll{ it.userId == user.id }.collect{ it.virtualGroupId })
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		return "( (${Utils.orifyList(discussionQueries)}) AND ( name:($query) OR posts:($query) OR firstPostMessage:($query)) AND _type:discussion )"
	}
	
	String getSprintSearchGroup1QueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
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
			return "((${Utils.orifyList(sprintQueries)}) AND ((name:($query)) OR (description:($query))) AND _type:sprint)"
		}
	}
	
	String getUserSearchGroup1QueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		def followedSansUser = followedUsers.findAll{ it.id != user.id }.collect{ it.id }
		if (followedSansUser.size > 0) {
			return "(((publicName:($query)) OR (publicBio:($query)) OR (interestTagsString:($query))) AND _type:user AND (_id:${Utils.orifyList(followedSansUser)}) AND virtual:false)"
		} else {
			return ""
		}
	}
			
	String getDiscussionSearchGroup2QueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def followingUserUserGroupIds = followedUsers.collect{ it.virtualUserGroupIdFollowers }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def readerGroupsSansFollowingGroups = (readerGroups.collect{ it[0].id } - followingUserUserGroupIds) - followingSprintUserGroupIds
						
		discussionQueries << ("(userId:${user.id})")
		
		def groupIds = []
		groupIds += readerGroupsSansFollowingGroups
		groupIds += adminGroups.collect{ it[0].id }
		groupIds += followingUserUserGroupIds
		groupIds += followingSprintUserGroupIds
		groupIds += followedSprints.findAll{ it.userId == user.id }.collect{ it.virtualGroupId }
		if (groupIds.size > 0) {
			discussionQueries << ("(groupIds:${Utils.orifyList(groupIds)})")
		}
		
		return "( ( NOT (${Utils.orifyList(discussionQueries)}) ) AND visibility:PUBLIC AND ( name:($query) OR posts:($query) OR firstPostMessage:($query)) AND _type:discussion )"
	}
	
	String getSprintSearchGroup2QueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def followingUserIds = followedUsers.collect{ it.id }
		def followingSprintUserGroupIds = followedSprints.collect{ it.virtualGroupId }
		def sprintReaderGroupIds = readerGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		def sprintAdminGroupIds = adminGroups.collect{ it[0].id }.intersect( followingSprintUserGroupIds )
		
		def sprintQueries = []
		def groupIds = []
		
		groupIds += sprintReaderGroupIds
		groupIds += sprintAdminGroupIds
		
		if (groupIds.size > 0) {
			sprintQueries << "(virtualGroupId:${Utils.orifyList(groupIds)})"
		}
		
		if (followingUserIds.size > 0) {
			sprintQueries << "(userId:${Utils.orifyList(followingUserIds)})"
		}
		
		if (sprintQueries.size > 0) {
			return "((NOT (${Utils.orifyList(sprintQueries)})) AND visibility:PUBLIC AND (name:($query) OR description:($query)) AND _type:sprint)"
		} else {
			return "(visibility:PUBLIC AND (name:($query) OR description:($query)) AND _type:sprint)"
		}
	}
	
	String getUserSearchGroup2QueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		def ignoreUserIds = followedUsers.collect{ it.id }
		if (ignoreUserIds.find{ it == user.id } == null) {
			ignoreUserIds << user.id
		}
		
		return "(((publicName:($query)) OR (publicBio:($query)) OR (interestTagsString:($query)))  AND virtual:false AND _type:user AND NOT (_id:${Utils.orifyList(ignoreUserIds)}))"
	}
			
	Map getFeed(Long type, User user, int offset = 0, int max = 10, int suggestionOffset = 0, def sessionId = null) {
		//TODO: make suggestion count change, but for now returning 3 suggestions per max activity OR enough to meet max count, whichever is greater
		
		log.debug "SearchService.getFeed called with type: $type; user: $user; offset: $offset; max: $max; suggestionOffset: $suggestionOffset; sessionId: $sessionId"
		
		if (type != DISCUSSION_TYPE && type != (DISCUSSION_TYPE | USER_TYPE) && type != SPRINT_TYPE) {
			return [success: false, listItems: false, nextSuggestionOffset: false]
		}
		
		Map results = getActivity((type == (DISCUSSION_TYPE | USER_TYPE)) ? DISCUSSION_TYPE : type, user, offset, max)
		
		if (!results.success) {
			return [success: false, listItems: false, nextSuggestionOffset: false]
		}
		
		results.suggestionCount = 0
		def defaultSuggestionMax = 3
		def suggestionMax = defaultSuggestionMax
		if (!(results.listItems) || results.listItems.size() == 0) {
			suggestionMax = max
		} else if (results.listItems && results.listItems.size() < max) {
			suggestionMax = max - results.listItems.size()
			if (suggestionMax < defaultSuggestionMax) {
				suggestionMax = defaultSuggestionMax
			}
		}
		
		def nextSuggestionOffset = suggestionOffset
		def nextSuggestionMax = suggestionMax
		def nonActivitySuggestions = []
		while (nextSuggestionMax > 0) {
			def suggestions = getSuggestions(type, user, nextSuggestionOffset, nextSuggestionMax, sessionId)
			if( suggestions.success && suggestions.listItems && suggestions.listItems.size() > 0) {
				for (def s : suggestions.listItems) {
					if (results.listItems.find{ it.type == s.type && it.hash == s.hash } == null) {
						nonActivitySuggestions << s
					}
				}
			} else {
				break;
			}
			
			nextSuggestionOffset += suggestions.listItems.size()
			
			if (suggestions.listItems.size() < nextSuggestionMax) {
				break; // suggestions exhausted
			}
			
			nextSuggestionMax = suggestions.listItems.size() - nonActivitySuggestions.size()
		}
		
		results.success = true
		results.listItems.addAll(nonActivitySuggestions)
		results.nextSuggestionOffset = nextSuggestionOffset
		
		return results
	}

	Map getActivity(Long type, User user, int offset = 0, int max = 10) {
		log.debug "SearchService.getActivity called with type: $type; user: $user; offset: $offset; max: $max"
		
		if (user == null || !((type == DISCUSSION_TYPE) || (type == SPRINT_TYPE)) ) {
			return [listItems: false, success: false]
		}
		
		def readerGroups = UserGroup.getGroupsForReader(user.id)
		def adminGroups = UserGroup.getGroupsForAdmin(user.id)
		def followedUsers = User.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualUserGroupIdFollowers:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}
		def followedSprints = Sprint.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualGroupId:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}

		def queries = []
		def query
		if ((type & DISCUSSION_TYPE) > 0) {
			query = getDiscussionActivityQueryString(user, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (query != null && query != "") {
				queries << query
			}
		} else if((type & SPRINT_TYPE) > 0) {
			query = getSprintActivityQueryString(user, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (query != null && query != "") {
				queries << query
			}
		}
		
		def result = [listItems: [], success: true]
		if (queries.size() > 0) {
			
			def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
			
			elasticSearchHelper.withElasticSearch{ client ->
				FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
				//FunctionScoreQueryBuilder fsqb = functionScoreQuery(matchAllQuery())
				
				fsqb.add(ScoreFunctionBuilders.gaussDecayFunction("recentPostCreated", "1d"))
								
				def temp = client.prepareSearch("us.wearecurio.model_v0")
				if ((type & DISCUSSION_TYPE) > 0) {
					temp.setTypes("discussion")
				} else if((type & SPRINT_TYPE) > 0) {
					temp.setTypes("sprint")
				}
				SearchResponse sr = temp
					.setQuery(fsqb)
					.setExplain(false)
					.setSize(max)
					.setFrom(offset)
					.execute()
					.actionGet()

				if (sr.hits.hits.size() > 0) {
					for ( def hit : sr.hits.hits ) {
						result.listItems << toJSON(hit, adminDiscussionIds)
					}
				}
			}
		}
		
		return result
	}

	Map getSuggestions(Long type, User user, int offset = 0, int max = 10, def sessionId = null) {
		log.debug "SearchService.getSuggestions called with type: $type; user: $user; offset: $offset; max: $max; sessionId: $sessionId"
		
		if (user == null || ((type & (DISCUSSION_TYPE | USER_TYPE | SPRINT_TYPE)) == 0)) {
			return [listItems: false, success: false]
		}
		long seed = (sessionId != null && sessionId.isNumber()) ? sessionId.toLong() : 44 //arbitrary constant seed, needs to be same with each call for pagination
		def queries = []
		def filters = []
		if( (type & DISCUSSION_TYPE) > 0) {
			queries << "(_type:discussion AND visibility:PUBLIC)"
		}
		if( (type & SPRINT_TYPE) > 0) {
			queries << "(_type:sprint AND visibility:PUBLIC)"
		}
		if ((type & USER_TYPE) > 0) {
			queries << "(_type:user AND _id:(NOT ${user.id}) AND virtual:false)"
		}
		
		def tags = user.getInterestTags()
		if (tags?.size() > 0) {
			def tagsOr = Utils.orifyList(tags.collect{ it.description })
			if ((type & DISCUSSION_TYPE) > 0) {
				filters << "(_type:discussion AND visibility:PUBLIC AND ((name:${tagsOr}) OR (posts:${tagsOr})))"
			}
			if ((type & SPRINT_TYPE) > 0) {
				filters << "(_type:sprint AND visibility:PUBLIC AND ((name:${tagsOr}) OR (description:${tagsOr})))"
			}
			if ((type & USER_TYPE) > 0) {
				filters << "(_type:user AND _id:(NOT ${user.id}) AND ((publicName:${tagsOr}) OR (publicBio:${tagsOr}) OR (interestTagsString:${tagsOr})))"
			}
		}
		
		queries.addAll(filters) 
		
		def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
		def result = [listItems: [], success: true]
		elasticSearchHelper.withElasticSearch{ client ->
			FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
			fsqb.scoreMode("sum") 
			fsqb.add(linearDecayFunction("created", "60d"))
			if (filters.size > 0) {
				fsqb.add(queryFilter(queryString(Utils.orifyList(filters))), weightFactorFunction(6)) 
			}
			fsqb.add(randomFunction(seed))
						
			def temp = client.prepareSearch("us.wearecurio.model_v0")
			if (type == (DISCUSSION_TYPE | USER_TYPE)) {
				temp.setTypes("discussion", "user")
			} else if (type == DISCUSSION_TYPE) {
				temp.setTypes("discussion")
			} else if (type == USER_TYPE) {
				temp.setTypes("user")
			} else if (type == SPRINT_TYPE) {
				temp.setTypes("sprint")
			}
			SearchResponse sr = temp
				.setQuery(fsqb)
				.setExplain(false)
				.setSize(max)
				.setFrom(offset)
				.execute()
				.actionGet()

			if (sr.hits.hits.size() > 0) {
				for ( def hit : sr.hits.hits ) {
					result.listItems << toJSON(hit, adminDiscussionIds)
				}
			}
		}
		
		return result
	}

	Map getOwned(Long type, User user, int offset = 0, int max = 10) {
		log.debug "SearchService.getOwned called with type: $type; user: $user; offset: $offset; max: $max"
		
		if (user == null || ((type & (DISCUSSION_TYPE | SPRINT_TYPE)) == 0)) {
			return [listItems: false, success: false]
		}
		
		def query
		if ((type & DISCUSSION_TYPE) > 0) {
			query = "_type:discussion AND userIdFinal:${user.id}"
		} 
		if ((type & SPRINT_TYPE) > 0){
			query = "_type:sprint AND userId:${user.id}"
		}
		
		def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
		
		def result = [listItems: [], success: true]
		elasticSearchHelper.withElasticSearch{ client ->
			SearchResponse sr = client
				.prepareSearch("us.wearecurio.model_v0")
				.setTypes("discussion", "sprint")
				.setQuery(queryString(query))
				//.setQuery(matchAllQuery())
				.setExplain(false)
				.setSize(max)
				.setFrom(offset)
				.addSort(SortBuilders.fieldSort("created").order(SortOrder.DESC))
				.execute()
				.actionGet()

			if (sr.hits.hits.size() > 0) {
				for ( def hit : sr.hits.hits ) {
					result.listItems << toJSON(hit, adminDiscussionIds)
				}
			}
		}
		
		return result
	}

	static final int SHOW_FIRST_COUNT = 5
	
	static Map getSearchOrder(int offset, int max, int group1Tot, int group2Tot) {
		if (group1Tot == 0 && group2Tot == 0) {
			return [
				"offset1"	: 0,
				"max1" 		: 0,
				"offset2" 	: 0,
				"max2" 		: 0,
				"orderList"	: []
			]
		}
		
		int offset1 = 0
		int offset2 = 0
		int max1 = 0
		int max2 = 0
		int cnt1 = group1Tot
		int cnt2 = group2Tot
		ArrayList orderList = []
		for (int i = 0; i < (offset + max); ++i) {
			if (i < SHOW_FIRST_COUNT) {
				if (group1Tot > i) {
					orderList << 1
					--cnt1
					(i >= offset) ? ++max1 : ++offset1
				} else if (cnt2 > 0) {
					orderList << 2
					--cnt2
					(i >= offset) ? ++max2 : ++offset2
				} else {
					break
				}
			} else if (i%2 == 0) {
				if (cnt1 > 0) {
					orderList << 1
					--cnt1
					(i >= offset) ? ++max1 : ++offset1
				} else if (cnt2 > 0) {
					orderList << 2
					--cnt2
					(i >= offset) ? ++max2 : ++offset2
				} else {
					break
				}
			} else {
				if (cnt2 > 0) {
					orderList << 2
					--cnt2
					(i >= offset) ? ++max2 : ++offset2
				} else if (cnt1 > 0) {
					orderList << 1
					--cnt1
					(i >= offset) ? ++max1 : ++offset1
				} else {
					break
				}
			}
		}
		
		def retList = []
		int lastValidIndex = orderList.size() < (offset + max) ? orderList.size() - 1 : offset + max - 1
		for (int i = offset; i <= lastValidIndex; ++i) {
			retList << orderList[i]
		}
		
		return [
			"offset1"	: offset1,
			"max1" 		: max1,
			"offset2" 	: offset2,
			"max2" 		: max2,
			"orderList"	: retList
		]
	}
	
	private int searchCount(String query, type = (DISCUSSION_TYPE | USER_TYPE | SPRINT_TYPE)) {
		int count = 0
		elasticSearchHelper.withElasticSearch{ client ->
			def temp = client.prepareCount("us.wearecurio.model_v0")
			
			if (type == DISCUSSION_TYPE) {
				temp.setTypes("discussion")
			} else if (type == SPRINT_TYPE) {
				temp.setTypes("sprint")
			} else if (type == USER_TYPE) {
				temp.setTypes("user")
			} else if (type == (DISCUSSION_TYPE | SPRINT_TYPE)) {
				temp.setTypes("discussion", "sprint")
			} else if (type == (DISCUSSION_TYPE | USER_TYPE)) {
				temp.setTypes("discussion", "user")
			} else if (type == (SPRINT_TYPE | USER_TYPE)) {
				temp.setTypes("sprint", "user")
			} else {
				temp.setTypes("discussion", "sprint", "user")
			}
			
			CountResponse cr = temp.setQuery(queryString(query))
					.execute()
					.actionGet()
			
			count = cr.count
		}
		
		return count
	}
	
	private String getSearchQuery(
		int group,
		User user,
		String query, 
		def readerGroups, 
		def adminGroups, 
		def followedUsers, 
		def followedSprints,
		def type ) {
				
		def queries = []
		def searchQueryString
		if ((type & DISCUSSION_TYPE) > 0) {
			searchQueryString =
				group == 1 ?
				getDiscussionSearchGroup1QueryString(user, query, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults) :
				getDiscussionSearchGroup2QueryString(user, query, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
				
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & SPRINT_TYPE) > 0) {
			searchQueryString =
				group == 1 ?
				getSprintSearchGroup1QueryString(user, query, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults) :
				getSprintSearchGroup2QueryString(user, query, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
				
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & USER_TYPE) > 0) {
			searchQueryString =
				group == 1 ?
				getUserSearchGroup1QueryString(user, query, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults) :
				getUserSearchGroup2QueryString(user, query, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
				
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if (queries.size() > 0) {
			return Utils.orifyList(queries)
		}
		
		return ""
	}
	
	private void scoreSearch(FunctionScoreQueryBuilder fsqb, User user, String userQuery) {
		//fsqb.add(linearDecayFunction("created", "7d").setWeight(8))
	}
	
	private void scoreDiscussionSearch(FunctionScoreQueryBuilder fsqb, User user, String userQuery) {
		fsqb.add(andFilter(typeFilter("discussion"), queryFilter(queryString("name:($userQuery)"))), linearDecayFunction("recentActivityDate", "7d").setWeight(100.0f))
		fsqb.add(andFilter(typeFilter("discussion"), queryFilter(queryString("firstPostMessage:($userQuery)"))), linearDecayFunction("recentActivityDate", "7d").setWeight(100.0f))
		fsqb.add(andFilter(typeFilter("discussion"), queryFilter(queryString("name:($userQuery)"))), weightFactorFunction(20.0f))
		fsqb.add(andFilter(typeFilter("discussion"), queryFilter(queryString("firstPostMessage:($userQuery)"))), weightFactorFunction(20.0f))
		fsqb.add(andFilter(typeFilter("discussion"), queryFilter(queryString("posts:($userQuery)"))), linearDecayFunction("recentActivityDate", "7d").setWeight(80.0f))
		//fsqb.add(andFilter(typeFilter("discussion"), termsFilter("visibility", "public", "private")), weightFactorFunction(100.0f))
	}

	private void scoreSprintSearch(FunctionScoreQueryBuilder fsqb, User user, String userQuery) {
		//fsqb.add(typeFilter("sprint"), weightFactorFunction(8.0f))
		fsqb.add(andFilter(typeFilter("sprint"), queryFilter(queryString("name:($userQuery)"))), linearDecayFunction("created", "7d").setWeight(100.0f))
		fsqb.add(andFilter(typeFilter("sprint"), queryFilter(queryString("name:($userQuery)"))), weightFactorFunction(20.0f))
		fsqb.add(andFilter(typeFilter("sprint"), queryFilter(queryString("description:($userQuery)"))), linearDecayFunction("created", "7d").setWeight(80.0f))
		fsqb.add(andFilter(typeFilter("sprint"), queryFilter(queryString("description:($userQuery)"))), weightFactorFunction(10.0f))
	}
	
	private void scoreUserSearch(FunctionScoreQueryBuilder fsqb, User user, String userQuery) {
		//fsqb.add(typeFilter("user"), weightFactorFunction(6.0f))
		fsqb.add(andFilter(typeFilter("user"), queryFilter(queryString("username:($userQuery)"))), weightFactorFunction(60.0f))
		fsqb.add(andFilter(typeFilter("user"), queryFilter(queryString("publicName:($userQuery)"))), weightFactorFunction(60.0f))
		fsqb.add(andFilter(typeFilter("user"), queryFilter(queryString("interestTagsString:($userQuery)"))), weightFactorFunction(40.0f))
		fsqb.add(andFilter(typeFilter("user"), queryFilter(queryString("publicBio:($userQuery)"))), weightFactorFunction(40.0f))
	}
		
	private Map searchWithESQuery(User user, String fullQuery, String userQuery, int offset, int max, def type) {
		def result = [listItems: [], success: true]
		
		if (fullQuery == null || fullQuery == "" || userQuery == null || userQuery == "") {
			return result
		}
		
		elasticSearchHelper.withElasticSearch{ client ->
			//TODO: work on scoring
			FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(fullQuery))
			//FunctionScoreQueryBuilder fsqb = functionScoreQuery(matchAllQuery())
			fsqb.scoreMode("sum")
			fsqb.boostMode("replace")
			
			scoreSearch(fsqb, user, userQuery)
			
			if ((type & DISCUSSION_TYPE) > 0) {
				scoreDiscussionSearch(fsqb, user, userQuery)
			}
			if ((type & SPRINT_TYPE) > 0) {
				scoreSprintSearch(fsqb, user, userQuery)
			}
			if ((type & USER_TYPE) > 0) {
				scoreUserSearch(fsqb, user, userQuery)
			}
			
			def temp = client.prepareSearch("us.wearecurio.model_v0")
			
			if (type == DISCUSSION_TYPE) {
				temp.setTypes("discussion")
			} else if (type == SPRINT_TYPE) {
				temp.setTypes("sprint")
			} else if (type == USER_TYPE) {
				temp.setTypes("user")
			} else if (type == (DISCUSSION_TYPE | SPRINT_TYPE)) {
				temp.setTypes("discussion", "sprint")
			} else if (type == (DISCUSSION_TYPE | USER_TYPE)) {
				temp.setTypes("discussion", "user")
			} else if (type == (SPRINT_TYPE | USER_TYPE)) {
				temp.setTypes("sprint", "user")
			} else {
				temp.setTypes("discussion", "sprint", "user")
			}

			SearchResponse sr = temp
				.setQuery(fsqb)
				//.setQuery(queryString(Utils.orifyList(queries)))
				.setExplain(false)
				.setSize(max)
				.setFrom(offset)
				.execute()
				.actionGet()

			def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
			if (sr.hits.hits.size() > 0) {
				for(def hit : sr.hits.hits) {
					result.listItems << toJSON(hit, adminDiscussionIds)
				}
			}
		}
		
		return result
	}	
	
	static String normalizeQuery(String query) {
		if (!query || query == "") {
			return ""
		}
		
	    return query.trim()
	        .replaceAll($/ ([oO][rR] )+|^[oO][rR]$| [oO][rR]$|^([oO][rR] )+[oO][rR]$|^([oO][rR] )+/$," ")
	        .replaceAll($/ ([aA][nN][dD] )+|^[aA][nN][dD]$| [aA][nN][dD]$|^([aA][nN][dD] )+[aA][nN][dD]$|^([aA][nN][dD] )+/$," ")
	        .trim()
	        .replaceAll("\\s+", " AND ")
	}
	
	Map search(User user, String query, int offset = 0, int max = 10, type = (DISCUSSION_TYPE | USER_TYPE | SPRINT_TYPE)) {
		log.debug "SearchService.search called with user: $user; query: $query; offset: $offset; max: $max; type: $type"
		
		if (user == null) {
			return [listItems: false, success: false]
		}

		String queryAnd = normalizeQuery(query)
		// If no query is entered or empty string is passed
		if (!queryAnd || !queryAnd.trim()) {
			return [success: false, message: messageSource.getMessage("search.query.empty", null, null)]
		}

		def readerGroups = UserGroup.getGroupsForReader(user.id)
		def adminGroups = UserGroup.getGroupsForAdmin(user.id)
		def followedUsers = User.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualUserGroupIdFollowers:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}
		def followedSprints = Sprint.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualGroupId:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}
		
		def queryGroup1 = getSearchQuery(1, user, queryAnd, readerGroups, adminGroups, followedUsers, followedSprints, type)
		def queryGroup2 = getSearchQuery(2, user, queryAnd, readerGroups, adminGroups, followedUsers, followedSprints, type)
		
//		println "=============================================="
//		println "=============================================="
//		println "queryGroup1: $queryGroup1"
//		println "queryGroup2: $queryGroup2"
		
		def group1Count = searchCount(queryGroup1, type)
		def group2Count = searchCount(queryGroup2, type)
		
//		println "group1Count: $group1Count"
//		println "group2Count: $group2Count"
		
		def searchOrderMap = getSearchOrder(offset, max, group1Count, group2Count)
		
//		println "searchOrderMap.offset1: $searchOrderMap.offset1"
//		println "searchOrderMap.max1: $searchOrderMap.max1"
//		println "searchOrderMap.offset2: $searchOrderMap.offset2"
//		println "searchOrderMap.max2: $searchOrderMap.max2"
//		println "searchOrderMap.orderList: $searchOrderMap.orderList"
		
		def resultsGroup1 = searchWithESQuery(user, queryGroup1, queryAnd, searchOrderMap.offset1, searchOrderMap.max1, type)
		def resultsGroup2 = searchWithESQuery(user, queryGroup2, queryAnd, searchOrderMap.offset2, searchOrderMap.max2, type)
		
//		println "resultsGroup1.success: $resultsGroup1.success"
//		println "resultsGroup1.listItems: $resultsGroup1.listItems"
//		println "resultsGroup2.success: $resultsGroup2.success"
//		println "resultsGroup2.listItems: $resultsGroup2.listItems"
//		println "=============================================="
//		println "=============================================="
		
		if (!resultsGroup1.success || !resultsGroup2.success) {
			return [listItems: false, success: false]
		}
		
		def result = [listItems: [], success: true]
		
		int it1 = 0
		int it2 = 0
		searchOrderMap.orderList.each {
			if (it == 1) {
				if (resultsGroup1.listItems[it1] != null) {
					result.listItems << resultsGroup1.listItems[it1]
				}
				++it1
			} else { //i == 2
				if (resultsGroup2.listItems[it2] != null) {
					result.listItems << resultsGroup2.listItems[it2]
				}
				++it2
			}
		}
		
		return result
	}
	
	Map searchOwned(User user, String query, int offset = 0, int max = 10, type = (DISCUSSION_TYPE | USER_TYPE | SPRINT_TYPE)) {
		log.debug "SearchService.searchOwned called with user: $user; query: $query; offset: $offset; max: $max; type: $type"
		
		return search(user, query, offset, max, type)
	}
	
	Map getSprintDiscussions(Sprint sprint, User user, int offset = 0, int max = 10) {
		log.debug "SearchService.getSprintDiscussions called with sprint: $sprint; user: $user; offset: $offset; max: $max"
		
		if (user == null || sprint == null) {
			return [listItems: false, success: false]
		}
		
		def discussionIds = GroupMemberDiscussion.lookupMemberIds(sprint.virtualGroupId)
		if (discussionIds == null || discussionIds.size == 0) {
			return [listItems: false, success: false]
		}
		
		def result = [listItems: [], success: true]
		//TODO: should filter based on user permissions, etc., but for now return all sprint discussions, ordered by date
		elasticSearchHelper.withElasticSearch{ client ->
			SearchResponse sr = client
				.prepareSearch("us.wearecurio.model_v0")
				.setTypes("discussion")
				.setQuery(queryString("_id:${Utils.orifyList(discussionIds)}")) //TODO: to add scoring function, uncomment this line and remove line below this one
				.setExplain(false)
				.setSize(max)
				.setFrom(offset)
				.addSort(SortBuilders.fieldSort("created").order(SortOrder.DESC))
				.execute()
				.actionGet()

			def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
			if (sr.hits.hits.size() > 0) {
				for(def hit : sr.hits.hits) {
					result.listItems << toJSON(hit, adminDiscussionIds)
				}
			}
		}
		
		return result
	}

/*	
	String getDiscussionsCreatedQueryString(User user) {
		return  "(userId:${user.id} AND activityType:CREATE AND objectType:DISCUSSION)"
	}

	String getDiscussionPostsCreatedQueryString(User user) {
		return  "(userId:${user.id} AND activityType:CREATE AND objectType:DISCUSSION_POST)"
	}	
	
	String getSprintsCreatedQueryString(User user) {
		return  "(userId:${user.id} AND activityType:CREATE AND objectType:SPRINT)"
	}
	
	String getDiscussionsFollowedQueryString(User user, String readerGroupsOr)	{
		return  "(groupIds:${readerGroupsOr})"
	}
	
	String getSprintsFollowedQueryString(User user, String readerGroupsOr) {
		return  "(virtualGroupId:${readerGroupsOr})"
	}
	
	String getUsersFollowedQueryString(User user, String readerGroupsOr) {
		return  "(virtualUserGroupIdFollowers:${readerGroupsOr})"
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
			query_string(query:  "virtualUserGroupIdFollowers:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}
		def followedSprints = Sprint.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualGroupId:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
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
*/
}

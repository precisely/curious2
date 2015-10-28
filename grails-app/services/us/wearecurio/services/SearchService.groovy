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

import static org.elasticsearch.index.query.FilterBuilders.andFilter
import static org.elasticsearch.index.query.FilterBuilders.queryFilter
import static org.elasticsearch.index.query.FilterBuilders.termsFilter
import static org.elasticsearch.index.query.FilterBuilders.typeFilter
import static org.elasticsearch.index.query.QueryBuilders.functionScoreQuery
import static org.elasticsearch.index.query.QueryBuilders.queryString
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.factorFunction
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.linearDecayFunction
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.randomFunction

class SearchService {
	
	private static def log = LogFactory.getLog(this)

	def elasticSearchService
	def elasticSearchHelper
	
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
					userName: hit.source.publicUserName,
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
					updated: hit.source.updated
				]
			case "user":
				return [
					type: "usr",
					hash: hit.source.hash,
					email: hit.source.email,
					remindEmail: hit.source.remindEmail,
					name: hit.source.publicName,
					sex: hit.source.sex,
					birthdate: hit.source.birthdate,
					website: hit.source.created,
					notifyOnComments: hit.source.notifyOnComments,
					created: hit.source.created,
					score: hit.score,
					interestTagsString: hit.source.interestTagsString
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

	String getDiscussionSearchQueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
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
		
		return "( ( (  (${Utils.orifyList(discussionQueries)}) AND ( name:(${query}) OR posts:(${query}) ) ) OR ( visibility:PUBLIC AND ( name:(${query}) OR posts:(${query}) ) ) ) AND _type:discussion )"
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
			return "(((${Utils.orifyList(sprintQueries)} AND name:(${query})) OR (visibility:PUBLIC AND name:(${query}) ) ) AND _type:sprint)"
		} else {
			return "(visibility:PUBLIC AND name:(${query}) AND _type:sprint)"
		}
	}

	String getUserSearchQueryString(User user, String query, List readerGroups, List adminGroups, List followedUsers, List followedSprints) {
		return "(((publicName:(${query})) OR (publicBio:(${query})) OR (interestTagsString:(${query}))) AND _type:user AND _id:(NOT ${user.id}))"
	}
			
	Map getFeed(Long type, User user, int offset = 0, int max = 10, int suggestionOffset = 0, def sessionId = null) {
		//TODO: make suggestion count change, but for now returning 3 suggestions per max activity OR enough to meet max count, whichever is greater
		
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
				
				if ((type & DISCUSSION_TYPE) > 0) {
					fsqb.add(ScoreFunctionBuilders.gaussDecayFunction("created", "1d"))
				} else if((type & SPRINT_TYPE) > 0) {
					fsqb.add(ScoreFunctionBuilders.gaussDecayFunction("recentPostCreated", "1d"))
				}
								
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
			queries << "(_type:user AND _id:(NOT ${user.id}))"
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
				fsqb.add(queryFilter(queryString(Utils.orifyList(filters))), factorFunction(6)) 
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

	private void scoreSearch(FunctionScoreQueryBuilder fsqb, User user, String query) {
		fsqb.add(linearDecayFunction("created", "7d").setWeight(8))
	}
	
	private void scoreDiscussionSearch(FunctionScoreQueryBuilder fsqb, User user, String query) {
		fsqb.add(andFilter(typeFilter("discussion"), termsFilter("visibility", "public", "private")), factorFunction(100))
	}

	private void scoreSprintSearch(FunctionScoreQueryBuilder fsqb, User user, String query) {
		fsqb.add(typeFilter("sprint"), factorFunction(8.0f))
	}
	
	private void scoreUserSearch(FunctionScoreQueryBuilder fsqb, User user, String query) {
		fsqb.add(typeFilter("user"), factorFunction(6.0f))
	}
		
	Map search(User user, String query, int offset = 0, int max = 10, type = (DISCUSSION_TYPE | USER_TYPE | SPRINT_TYPE)) {
		if (user == null) {
			return [listItems: false, success: false]
		}
						
		String queryAnd = query.replaceAll("[oO][rR]","").replaceAll("[aA][nN][dD]"," ").replaceAll("\\s+", " AND ")
		def readerGroups = UserGroup.getGroupsForReader(user.id)
		def adminGroups = UserGroup.getGroupsForAdmin(user.id)
		def followedUsers = User.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualUserGroupIdFollowers:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}
		def followedSprints = Sprint.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualGroupId:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}

		def queries = []
		def searchQueryString
		if ((type & DISCUSSION_TYPE) > 0) {
			searchQueryString = getDiscussionSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & SPRINT_TYPE) > 0) {
			searchQueryString = getSprintSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & USER_TYPE) > 0) {
			searchQueryString = getUserSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		def result = [listItems: [], success: true]
		if (queries.size() > 0) {
			elasticSearchHelper.withElasticSearch{ client ->
				//TODO: work on scoring
				FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
				//FunctionScoreQueryBuilder fsqb = functionScoreQuery(matchAllQuery())
				
				scoreSearch(fsqb, user, query)
				
				if ((type & DISCUSSION_TYPE) > 0) {
					scoreDiscussionSearch(fsqb, user, query)
				}
				if ((type & SPRINT_TYPE) > 0) {
					scoreSprintSearch(fsqb, user, query)
				}
				if ((type & USER_TYPE) > 0) {
					scoreUserSearch(fsqb, user, query)
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
		}
		
		return result
	}
	
	Map searchOwned(User user, String query, int offset = 0, int max = 10, type = (DISCUSSION_TYPE | USER_TYPE | SPRINT_TYPE)) {
		return search(user, query, offset, max, type)
	}
	
	Map getSprintDiscussions(Sprint sprint, User user, int offset = 0, int max = 10) {
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

	private Map search(User user, Long type, String query, int offset = 0, int max = 10) {
		if (user == null) {
			return [listItems: false, success: false]
		}
						
		String queryAnd = query.replaceAll("[oO][rR]","").replaceAll("[aA][nN][dD]"," ").replaceAll("\\s+", " AND ")
		def readerGroups = UserGroup.getGroupsForReader(user.id)
		def adminGroups = UserGroup.getGroupsForAdmin(user.id)
		def followedUsers = User.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualUserGroupIdFollowers:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}
		def followedSprints = Sprint.search(searchType:'query_and_fetch') {
			query_string(query:  "virtualGroupId:${Utils.orifyList(readerGroups.collect{ it[0].id })}")
		}

		def queries = []
		def searchQueryString
		if ((type & DISCUSSION_TYPE) > 0) {
			searchQueryString = getDiscussionSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & SPRINT_TYPE) > 0) {
			searchQueryString = getSprintSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & USER_TYPE) > 0) {
			searchQueryString = getUserSearchQueryString(user, queryAnd, readerGroups, adminGroups, followedUsers.searchResults, followedSprints.searchResults)
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		def result = [listItems: [], success: true]
		if (queries.size() > 0) {
			elasticSearchHelper.withElasticSearch{ client ->
				//TODO: add scoring. for now, sort descending by date
				//FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
				//fsqb.add(ScoreFunctionBuilders.gaussDecayFunction("created", "7d"))
				
				SearchResponse sr = client
					.prepareSearch("us.wearecurio.model_v0")
					//.setTypes("discussion", "discussionPost", "user", "sprint")
					.setTypes("discussion", "user", "sprint")
					//.setQuery(fsqb) //TODO: to add scoring function, uncomment this line and remove line below this one
					.setQuery(queryString(Utils.orifyList(queries)))
					.setExplain(false)
					.setSize(max)
					.setFrom(offset)
					.addSort(SortBuilders.fieldSort("created").order(SortOrder.DESC))
					.execute()
					.actionGet()

				if (sr.hits.hits.size() > 0) {
					def activities = []
					for(def hit : sr.hits.hits) {
						switch(hit.type) {
							case "discussion":
								activities << [
									type	: "dis",
									id		: hit.id,
									name	: hit.source.name,
									created	: hit.source.created,
									score	: hit.score
								]
								break;
							case "user":
								activities << [
									type	: "usr",
									id		: hit.id,
									name	: hit.source.name,
									created	: hit.source.created,
									score	: hit.score
								]
								break;
							case "sprint":
								activities << [
									type	: "spr",
									id		: hit.id,
									name	: hit.source.name,
									created	: hit.source.created,
									score	: hit.score
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
*/
}

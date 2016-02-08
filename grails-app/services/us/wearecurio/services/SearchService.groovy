package us.wearecurio.services
import org.apache.commons.logging.LogFactory
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder

import us.wearecurio.model.Discussion
import us.wearecurio.model.GroupMemberDiscussion
import us.wearecurio.model.Model
import us.wearecurio.model.Sprint
import us.wearecurio.model.User
import us.wearecurio.model.UserActivity
import us.wearecurio.model.UserActivity.ActivityType
import us.wearecurio.model.UserActivity.ObjectType
import us.wearecurio.model.UserGroup
import us.wearecurio.utility.Utils


import org.elasticsearch.action.count.CountResponse

import static org.elasticsearch.index.query.FilterBuilders.andFilter
import static org.elasticsearch.index.query.FilterBuilders.existsFilter
import static org.elasticsearch.index.query.FilterBuilders.notFilter
import static org.elasticsearch.index.query.FilterBuilders.orFilter
import static org.elasticsearch.index.query.FilterBuilders.queryFilter
import static org.elasticsearch.index.query.FilterBuilders.termsFilter
import static org.elasticsearch.index.query.FilterBuilders.typeFilter
import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery
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
    
    void deindex(Object obj) {
		if (obj == null || obj.id == null) {
			return
		}
		elasticSearchHelper.withElasticSearch{ client ->
            client.prepareDelete("us.wearecurio.model_v0", obj.class.simpleName.toLowerCase(),obj.id.toString()).setRefresh(true).execute().actionGet()
		}
    }
	
	// user is the user viewing this search hit
	private Map toJSON(def hit, List adminDiscussionIds, User user) {
		switch (hit.type) {
			case "discussion":
                return toJSON(hit.id, hit.source, adminDiscussionIds, false, hit.score, user.id)
			case "sprint":
				return [
					type: "spr",
					id: hit.id.toLong(),
					hash: hit.source.hash,
					name: hit.source.name,
					userId: hit.source.userId,
					description: hit.source.description,
					totalParticipants: hit.source.participantsCount,
					totalTags: hit.source.entriesCount,
					virtualUserId: hit.source.virtualUserId,
					virtualGroupId: hit.source.virtualGroupId,
					virtualGroupName: hit.source.virtualGroupName,
					created: hit.source.created,
					updated: hit.source.updated,
					score: hit.score,
				]
			case "user":
				boolean followed = user.followsId(hit.id.toLong());
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
					followed: followed,
					avatarURL: hit.source.avatarURL
				]
		}
		
		return [:]
	}

	private Map toJSON(def id, def discussion, List adminDiscussionIds, boolean isNew, Float score = 0, Long userId) {
        return [
            type: "dis",
            id: id.toLong(),
            hash: discussion.hash,
            name: discussion.name,
            userHash: discussion.userHash,
            userName: discussion.publicUserName,
            userAvatarURL: discussion.userAvatarURL,
            isPublic: (discussion.visibility == "PUBLIC"),
            created: discussion.created,
            updated: discussion.updated,
            totalComments: discussion.postCount,
            isPlot: discussion.isFirstPostPlot,
            firstPost: discussion.firstPostId,
            isAdmin: adminDiscussionIds.contains(id.toLong()),
            groupId: null,
            groupName: null,
            score: score,
            firstPostMessage: discussion.firstPostMessage,
            posts: discussion.posts,
            isNew: isNew,
			// TODO: May be this can be optimized for ElasticSearch
			isFollower: Discussion.get(id.toLong())?.isFollower(userId)
        ]
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
		}
		if (suggestionMax < defaultSuggestionMax) {
			suggestionMax = defaultSuggestionMax
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

	private String getActivityQuery(Long type, User user) {
		if (user == null || !((type == DISCUSSION_TYPE) || (type == SPRINT_TYPE)) ) {
			return ""
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
            
            query = SearchQueryService.getDiscussionActivityQueryString(
                user.id,
                readerGroups.collect{ it[0].id },
                adminGroups.collect{ it[0].id },
                followedUsers.searchResults.collect{ it.virtualUserGroupIdFollowers },
                followedSprints.searchResults.collect{ it.virtualGroupId },
                followedSprints.searchResults.find{ it.userId == user.id }.collect{ it.virtualGroupId }
            )
			if (query != null && query != "") {
				queries << query
			}
		} else if((type & SPRINT_TYPE) > 0) {
            query = SearchQueryService.getSprintActivityQueryString(
                user.id,
                readerGroups.collect{ it[0].id },
                adminGroups.collect{ it[0].id },
                followedUsers.searchResults.collect{ it.id },
                followedSprints.searchResults.collect{ it.virtualGroupId }
            )
			if (query != null && query != "") {
				queries << query
			}
		}
		
		String ret = ""
		if (queries.size() > 0) {
			ret = Utils.orifyList(queries)
		}
		
		//println "getActivityQuery returns '$ret'"
		return ret
	}
	
	Map getActivity(Long type, User user, int offset = 0, int max = 10) {
		log.debug "SearchService.getActivity called with type: $type; user: $user; offset: $offset; max: $max"
		
		if (user == null || !((type == DISCUSSION_TYPE) || (type == SPRINT_TYPE)) ) {
			return [listItems: false, success: false]
		}
		
		def result = [listItems: [], success: true]
		def query = getActivityQuery(type, user)
		if (query != null && query != "") {
			def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
			
			elasticSearchHelper.withElasticSearch{ client ->
				//FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
				FunctionScoreQueryBuilder fsqb = functionScoreQuery(constantScoreQuery(queryString(query)))
				//FunctionScoreQueryBuilder fsqb = functionScoreQuery(matchAllQuery())
                //fsqb.scoreMode("sum")
                //fsqb.scoreMode("first")
				
                if ((type & DISCUSSION_TYPE) > 0) {
                    fsqb.add(
                        andFilter(
                            typeFilter("discussion"), 
                            queryFilter(queryString("hasRecentPost:true AND followers:${user.id}"))
                        ), linearDecayFunction("recentPostCreated", "7d").setWeight(100.0f)
                    )

                    fsqb.add(
                        andFilter(
                            typeFilter("discussion"),
                            orFilter(
                                queryFilter(queryString("hasRecentPost:false")),
                                andFilter(
                                    queryFilter(queryString("hasRecentPost:true")),
                                    queryFilter(queryString("NOT followers:${user.id}"))
                                )
                            )
                        ), linearDecayFunction("created", "7d").setWeight(100.0f)
                    )
                } else if((type & SPRINT_TYPE) > 0) {
                    fsqb.add(
                        typeFilter("sprint"), 
                        linearDecayFunction("lastInterestingActivityDate", "60d").setWeight(100.0f)
                    )
    //                fsqb.add(
    //                    andFilter(
    //                        typeFilter("sprint"), 
    //                        existsFilter("userDiscusionAcivityDates.${user.id}")
    //                    ), linearDecayFunction("created", "7d").setWeight(100.0f)
    //                )
                }
                //fsqb.add(ScoreFunctionBuilders.gaussDecayFunction("recentPostCreated", "1d"))
								
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
						result.listItems << toJSON(hit, adminDiscussionIds, user)
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
		
		//arbitrary constant seed, needs to be same with each call for pagination
		Long seed
		try {
			seed = (sessionId as Long)
		} catch (e) {
			seed = 44 //arbitrary default seed
		}
		
		//long seed = (sessionId != null && sessionId.isNumber()) ? sessionId.toLong() : 44 //arbitrary constant seed, needs to be same with each call for pagination
		def queries = []
		def filters = []
		if( (type & DISCUSSION_TYPE) > 0) {
			queries << "(_type:discussion AND visibility:PUBLIC AND followers:(NOT $user.id))"
		}
		if( (type & SPRINT_TYPE) > 0) {
			queries << "(_type:sprint AND visibility:PUBLIC AND _exists_:description AND ((NOT _exists_:deleted) OR deleted:false))"
		}
		if ((type & USER_TYPE) > 0) {
			queries << "(_type:user AND _id:(NOT $user.id) AND virtual:false)"
		}
		
		def tags = user.getInterestTags()
		if (tags?.size() > 0) {
			def tagsOr = Utils.orifyList(tags.collect{ it.description })
			if ((type & DISCUSSION_TYPE) > 0) {
				filters << "(_type:discussion AND visibility:PUBLIC AND ((name:$tagsOr) OR (posts:$tagsOr)))"
			}
			if ((type & SPRINT_TYPE) > 0) {
				filters << "(_type:sprint AND visibility:PUBLIC AND _exists_:description AND ((NOT _exists_:deleted) OR deleted:false) AND ((name:$tagsOr) OR (description:$tagsOr)))"
			}
			if ((type & USER_TYPE) > 0) {
				filters << "(_type:user AND _id:(NOT $user.id) AND virtual:false AND ((publicName:$tagsOr) OR (publicBio:$tagsOr) OR (interestTagsString:$tagsOr)))"
			}
		}
		
		queries.addAll(filters)
		String activityQuery = getActivityQuery(type, user)
		String fullQuery
		if (activityQuery == null || activityQuery == ""){
			fullQuery = Utils.orifyList(queries)
		} else {
			fullQuery = "(${Utils.orifyList(queries)}) AND NOT ($activityQuery)"
		}
		
		def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
		def result = [listItems: [], success: true]
		elasticSearchHelper.withElasticSearch{ client ->
			FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(fullQuery))
			//FunctionScoreQueryBuilder fsqb = functionScoreQuery(queryString(Utils.orifyList(queries)))
			fsqb.scoreMode("sum") 
			fsqb.add(linearDecayFunction("created", "60d"))
			if (filters.size > 0) {
				fsqb.add(queryFilter(queryString(Utils.orifyList(filters))), weightFactorFunction(6)) 
			}
			//disabling until we can figure out how to make work properly
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
					result.listItems << toJSON(hit, adminDiscussionIds, user)
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
			query = "_type:sprint AND ((NOT _exists_:deleted) OR deleted:false) AND userId:${user.id} AND _exists_:description"
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
					result.listItems << toJSON(hit, adminDiscussionIds, user)
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
		
        return SearchQueryService.getSearchQuery(
            group,
            user.id,
            query,
            readerGroups.collect{ it[0].id },
            adminGroups.collect{ it[0].id },
            followedUsers.searchResults.collect{ it.id },
            followedUsers.searchResults.collect{ it.virtualUserGroupIdFollowers },
            followedSprints.searchResults.collect{ it.virtualGroupId },
            followedSprints.searchResults.findAll{ it.userId == user.id }.collect{ it.virtualGroupId },
            type
        )
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
					result.listItems << toJSON(hit, adminDiscussionIds, user)
				}
			}
		}
		
		return result
	}	
	
	Map search(User user, String query, int offset = 0, int max = 10, type = (DISCUSSION_TYPE | USER_TYPE | SPRINT_TYPE)) {
		log.debug "SearchService.search called with user: $user; query: $query; offset: $offset; max: $max; type: $type"
		
		if (user == null) {
			return [listItems: false, success: false]
		}

		String queryAnd = SearchQueryService.normalizeQuery(query)
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
//		println "=============================================="
//		println "queryGroup2: $queryGroup2"
//		println "=============================================="
//		println "=============================================="
		
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
					result.listItems << toJSON(hit, adminDiscussionIds, user)
				}
			}
		}
		
		return result
	}
    
    Long getSprintNotificationCount(User user, Date end=null, Date begin=null) {
        return -1
    }
    
    Long getNotificationCount(User user, Long type, Date begin=null, Date end=null) {
		if (type == null || type != DISCUSSION_TYPE || user == null) {
			return null
		}
        
        String query
        if (type == DISCUSSION_TYPE) {
            query = SearchQueryService.getDiscussionNotificationQuery(user.id, begin, end)
        //} else if (type == SPRINT_TYPE) {
        }
        
        if (query == null || query == "") {
            return null
        }
        
        Long count = null
                    
		elasticSearchHelper.withElasticSearch{ client ->			
			CountResponse cr = client.prepareCount("us.wearecurio.model_v0")
                .setTypes("discussion")
                .setQuery(queryString(query))
				.execute()
				.actionGet()
            
			count = cr.count
		}

        return count
    }    
    
    Long getNewNotificationCount(User user, Long type = DISCUSSION_TYPE) {
		if (type == null || type != DISCUSSION_TYPE || user == null) {
			return null
		}
        
        Long uaType = UserActivity.toType(
            UserActivity.ActivityType.CHECKED_NOTIFICATIONS, 
            UserActivity.ObjectType.USER,
            type==DISCUSSION_TYPE ? ObjectType.DISCUSSION : ObjectType.SPRINT
        )
        
        def checkedActivity = UserActivity.search(searchType:'query_and_fetch') {
            query_string(query:"typeId:$uaType AND objectId:${user.id}")
		}
        
        Date begin = null
        if (checkedActivity.searchResults.size > 0) {
            begin = checkedActivity.searchResults[0].created
        }
        
        return getNotificationCount(user, type, begin)
    }
    
    def getNotifications(User user, Long type, Date curDate, int offset=0, int max=10, Date lastCheckedDate=null){
		//println "getNotifications: user: $user, type: $type, curDate: $curDate, offset: $offset, max: $max, lastCheckedDate: $lastCheckedDate"
        //write query to find all sprint and discussion notifications (as per notes)
		//if (type == null || (type != DISCUSSION_TYPE && type != SPRINT_TYPE) || user == null || curDate == null) {
		if (type == null || type != DISCUSSION_TYPE || user == null || curDate == null) {
            return [listItems: false, success: false]
		}
        
        String query
        if (type == DISCUSSION_TYPE) {
            query = SearchQueryService.getDiscussionNotificationQuery(user.id)
        //} else if (type == SPRINT_TYPE) {
        }
        
//		println "======================"
//		println "query: $query"
//		println "======================"
        def results = Discussion.search(
            searchType:'query_and_fetch', 
            sort:'recentPostCreated', 
            order:'desc',
            from:offset,
            size:max
        ) {
            query_string(query:query)
        }

		def ret = [listItems: [], success: true]
        def adminDiscussionIds = User.getAdminDiscussionIds(user.id)
        
        results.searchResults.each {
            boolean isNew = true
            if (curDate && lastCheckedDate) {
                isNew = (it.recentPostCreated >= lastCheckedDate && it.recentPostCreated <= curDate)
            } else if (curDate) {
                isNew = (it.recentPostCreated <= curDate)
            } else if (lastCheckedDate) {
                isNew = (it.recentPostCreated >= lastCheckedDate)
            }
            
            ret.listItems << toJSON(it.id, it, adminDiscussionIds, isNew, 0, user.id)
        }
        
        return ret
    }
    
    def getNotifications(User user, Long type=DISCUSSION_TYPE, int offset=0, int max=10) {
		//if ((type != DISCUSSION_TYPE && type != SPRINT_TYPE) || user == null) {
		if (type != DISCUSSION_TYPE || user == null) {
			return [listItems: false, success: false]
		}
        
        Long uaType = UserActivity.toType(
            ActivityType.CHECKED_NOTIFICATIONS, 
            ObjectType.USER,
            type==DISCUSSION_TYPE ? ObjectType.DISCUSSION : ObjectType.SPRINT
        )

        def results = UserActivity.search(searchType:'query_and_fetch') {
            query_string(query:"typeId:$uaType AND objectId:${user.id}")
        }
        
        return getNotifications(
                user, 
                type, 
                new Date(),
                offset,
                max,
                (results.searchResults.size > 0) ? results.searchResults[0].created : null )
    }
    
    void resetNotificationsCheckDate(User user, Long type, Date checkDate){
		//if ((type != DISCUSSION_TYPE && type != SPRINT_TYPE) || user == null || checkDate == null) {
		if (type != DISCUSSION_TYPE || user == null || checkDate == null) {
			return
		}
        
        Long uaType = UserActivity.toType(
            ActivityType.CHECKED_NOTIFICATIONS, 
            ObjectType.USER,
            type==DISCUSSION_TYPE ? ObjectType.DISCUSSION : ObjectType.SPRINT
        )
        
        UserActivity ua = UserActivity.findByTypeIdAndObjectId(uaType, user.id)
        
        if (ua != null) {
            ua.created = checkDate
            Utils.save(ua, true)
        } else {
            UserActivity.create(
                checkDate,
                null,
                ActivityType.CHECKED_NOTIFICATIONS,
                ObjectType.USER,
                user.id,
                type==DISCUSSION_TYPE ? ObjectType.DISCUSSION : ObjectType.SPRINT
            )
        }
    }
    
    void resetNotificationsCheckDate(User user, Long type=DISCUSSION_TYPE){
        resetNotificationsCheckDate(user, type, new Date())
    }
	
	Map getStartedSprints(Long userId, int offset=0, int max=10) {
		if ( max == 0 ) {
			return [success: true, listItems: []]
		}
		
		Long startType = UserActivity.toType(ActivityType.START, ObjectType.SPRINT)
		Long stopType = UserActivity.toType(ActivityType.STOP, ObjectType.SPRINT)
		def activities = UserActivity.search(searchType:'query_and_fetch', sort:'created', order:'desc') {
			query_string(query: "typeId:($startType OR $stopType) AND userId:$userId")
		}
		
		Map foundItems = [:]
		List startedSprintIds = []
		for (UserActivity ua : activities.searchResults) {
			if (foundItems.containsKey(ua.objectId)) {
				continue
			}
			
			if (ua.typeId == startType) {
				startedSprintIds << ua.objectId
			}
			foundItems[ua.objectId] = ua
		}
		
		//println "startedSprintIds: $startedSprintIds"
	
		if (startedSprintIds.size == 0 || offset >= startedSprintIds.size) {
			return [success: true, listItems: []]
		}
		
		int last = (max > (startedSprintIds.size - offset)) ? (startedSprintIds.size - 1) : (offset + max - 1)
//		println "last: $last"
//		println "max: $max"
//		println "offset: $offset"
//		println "startedSprintIds.size: ${startedSprintIds.size}"
		List resizedStartedSprintIds = startedSprintIds.getAt(offset..last)
		
		def sprints = Sprint.search(
			searchType:'query_and_fetch'
		) {
			query_string(query: "_id:${Utils.orifyList(resizedStartedSprintIds)}")
		}
		
		//can't control order of sprint search results, so manually adjust order of results
		Map results = [success: true, listItems: []]
		for (Long id : resizedStartedSprintIds) {
			Sprint s = sprints.searchResults.find{ it.id == id }
			if (s != null) {
				results.listItems << s.getJSONDesc()
			}
		}
		
		return results
	}
}

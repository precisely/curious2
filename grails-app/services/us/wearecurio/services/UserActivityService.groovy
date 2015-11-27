package us.wearecurio.services

/////////////////////////////////////////////////////////////////////
// NOTE: This is a placeholder class to store code not yet in use 
// having to do with tracking user activity for administrative 
// purposes. For initial release this code will not be used
/////////////////////////////////////////////////////////////////////

class UserActivityService {

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

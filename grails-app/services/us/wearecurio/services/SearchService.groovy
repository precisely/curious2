package us.wearecurio.services

import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.Sprint
import us.wearecurio.model.UserGroup
import us.wearecurio.model.GroupMemberReader
import us.wearecurio.model.Discussion
import org.elasticsearch.action.search.SearchResponse
import static org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.*
import org.elasticsearch.action.count.CountResponse
import org.elasticsearch.search.aggregations.AggregationBuilders

class SearchService {
	
	def elasticSearchService
	def elasticSearchHelper
	
	static transactional = true
	
	static SearchService service
	
	static set(SearchService s) {
		service = s
	}
	
	static SearchService get() {
		service
	}
	
	// params has the structure:
	// [ key:[offset:<offset>, max:<max>], ... ]
	//
	// returns:
	// [ key:[<json description of item>, ... ], ... ]
	
	def list(Long userId, String searchString, Map params) {
		def retVal = [:]
		for (e in params) {
			String k = e.key
			Map v = e.value
			int offset = v['offset']
			int max = v['max']
			
			def results
			
			if (k.equals('discussions')) {
				retVal[k] = this.listDiscussions(searchString, offset, max)
			} else if (k.equals('users')) {
				retVal[k] = this.listUsers(searchString, offset, max)
			} else if (k.equals('sprints')) {
				retVal[k] = this.listSprints(searchString, offset, max)
			} else if (k.equals('groups')) {
				// not searching groups for now
				// retVal[k] = this.listGroups(searchString, offset, max)
			}
		}
	}
	
	def listDiscussions(User user, String searchString, int offset, int max) {
		
	}
	
	def listUsers(User user, String searchString, int offset, int max) {
		
	}
	
	def listSprints(User user, String searchString, int offset, int max) {
		
	}
	
	def listGroups(User user, String searchString, int offset, int max) {
		
	}
	
	Map getSprintsList(User user, int offset, int max) {
		List sprintList = Sprint.getSprintListForUser(user.id, max, offset)
		if (!sprintList) {
			return [listItems: false, success: true]
		}
		Map model = [sprintList: sprintList]
		return [listItems: model, success: true]
	}
	
	Map getDiscussionsList(User user, int offset, int max) {
		def userReaderGroups = UserGroup.getGroupsForReader(user)
		if (userReaderGroups == null || userReaderGroups.size() < 1) {
			return [listItems: false, success: false]
		}
		
		String groupIdsOr = userReaderGroups.collect{ it[0].id }.join(" OR ")
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
		
		def readerGroupDiscussions = Discussion.search(searchType:'query_and_fetch', size: max.toString(), from: offset.toString() ) {
			query_string(query:  "groupIds:" + groupIdsOr)
		}
		
		if (readerGroupDiscussions.searchResults == null || readerGroupDiscussions.searchResults.size() < 1) {
			return [listItems: false, success: false]
		}
		
		String discussionIdsOr = readerGroupDiscussions.searchResults.collect{ it.id }.join(" OR ")
		if (discussionIdsOr.size() > 1) discussionIdsOr = "(" + discussionIdsOr + ")"
		
		def adminGroupIds = user.getAdminGroupIds()
		
		elasticSearchHelper.withElasticSearch{ client ->
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
				System.out.println "readerGroupDiscussion: " + d.toString()
				model["discussionList"] << [
					id: d.id,
					hash: null,
					name: d.name,
					userId: d.userId,
					isPublic: d.isPublic(),
					created: d.created,
					updated: d.updated,
					type: "dis"
				]
				
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
					model["discussionList"].find{ it.id == d.id}["totalComments"] = bucket.docCount
				} else {
					model["discussionPostData"][d.id] = [
						secondPost: null,
						totalPosts: 0
					]
					model["discussionList"].find{ it.id == d.id}["totalComments"] = 0
				}
				
				if (firstPost) {
					model["discussionList"].find{ it.id == d.id}["isPlot"] = (firstPost.plotDataId != null && firstPost.plotDataId > 0)
					model["discussionList"].find{ it.id == d.id}["firstPost"] = firstPost
				}
				
				//eventually, this code will need to be refactored to account for discussions with multiple groups
				//for now, arbitrarily grab first group for which user is an admin and which is associated with this discussion
				//if user is not admin of any group associated with this discussion, then arbitrarily select first group associated with this discussion
				//def groupIds = d.groupIds
				if (d.groupIds == null || d.groupIds.length < 1) {
					//all discussions should have at least one group
					return [listItems: false, success: false] // better to throw exception?
				}
				
				model["discussionList"].find{ it.id == d.id}["isAdmin"] = (d.userId == user.id)
				def firstGroup = true
				for (Long gid : d.groupIds) {
					//NOT ALL of discussion's groups are necessarily included in userReaderGroups
					//BUT at least ONE of the discussion's groupIds MUST be
					def groups = userReaderGroups.find{ it[0].id == gid }
					if (groups && groups.length > 0) {
						if (firstGroup) {
							//use first group data, unless admin group is found further down in the list
							model["discussionList"].find{ it.id == d.id}["groupId"] = gid
							model["discussionList"].find{ it.id == d.id}["groupName"] = groups[0].fullName
							if (model["discussionList"].find{ it.id == d.id}["isAdmin"]) {
								break; //admin already true, so no need to continue
							}
						}
						if (d.userId == user.id || (adminGroupIds != null && adminGroupIds.contains(gid))) {
							//found first admin group
							if (!firstGroup) {
								//override first group data with current, non-first-group-but-first-admin-group data
								model["discussionList"].find{ it.id == d.id}["groupId"] = gid
								model["discussionList"].find{ it.id == d.id}["groupName"] = groups[0].fullName
							}
							model["discussionList"].find{ it.id == d.id}["isAdmin"] = true
							break; // found admin, so no need to proceed
						}
						firstGroup = false
					}
				}
			}
			
			return [listItems: model, success: true]
		}
	}
	
	Map getPeopleList(User user, int offset, int max) {
		List usersList = User.getUsersList(max, offset, user.id)
		if (usersList) {
			return [listItems: usersList, success: true]
		}
		return [listItems: false, success: true]
	}
}

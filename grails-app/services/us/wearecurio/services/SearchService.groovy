package us.wearecurio.services

import org.grails.plugins.elasticsearch.ElasticSearchService;

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


class SearchService {
	
    def elasticSearchService
    def elasticSearchHelper

	static transactional = true
	
	static SearchService service
	
	static set(SearchService s) { service = s }
	
	static SearchService get() { service }

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
//		def groupMemberships = UserGroup.getGroupsForReader(user)
//		List groupNameList = []
//		Map discussionData = groupNameList ? UserGroup.getDiscussionsInfoForGroupNameList(user, groupNameList, [offset: offset, max: max]) :
//			UserGroup.getDiscussionsInfoForUser(user, true, false, [offset: offset, max: max])
//
//		Map discussionPostData = discussionData["discussionPostData"]
//		discussionData["dataList"].each {data ->
//				data.totalComments = discussionPostData[data.id].totalPosts
//			}
//
//		Map model = [userId: user.getId(), groupMemberships: groupMemberships, totalDiscussionCount: discussionData["totalCount"], 
//			discussionList: discussionData["dataList"], discussionPostData: discussionData["discussionPostData"]]
//
//		if (!model.discussionList) {
//			return [listItems: false, success: true]
//		} else {
//			return [listItems: model, success: true]
//		}
        
        def groupMemberships = UserGroup.getGroupsForReader(user)
        String groupIdsOr = groupMemberships.collect{ it[0].id }.join(" OR ")
        if (groupMemberships.size() > 1) {
            groupIdsOr = "(" + groupIdsOr + ")"
        }
        
        def discussionResults = Discussion.search(searchType:'query_and_fetch', size: max.toString(), from: offset.toString() ) {
            query_string(query:  "groupIds:" + groupIdsOr)
        }
        
        String discussionIdsOr = discussionResults.searchResults.collect{ it.id }.join(" OR ")
        if (discussionIdsOr.size() > 1) discussionIdsOr = "(" + discussionIdsOr + ")"
        
        // allPostCountOrderdByDiscussion seems to use some sort of built-in groovy/hibernate/GORM thing
        //   called 'withCriteria' to count all discussions posts per discussionid
        //   it uses a discussionid list that is all the discussions for which the user is a reader
        //   OH MY GOD IS THIS COMPLICATED!
        //   couple parameters can expand above results to include owned, which includes discussions for 
        //   which user is the owner (that is, the userid of the discussion (the one passed in at construction))
        //   or limit it to ONLY groups for which the user is owner AND a reader (if intention was to not
        //   include readers that are not also owners, query is wrong, but I think the intention is readers that
        //   that are also owners.)
        //   don't understand the GROUP BY-- if grouped by discussionid, wouldn't that mean only one discussionid per row
        //   and since groupid and userid not in the group by clause, you should have to have a max or something
        //   i wonder if GORM works differently or if this would generate a run-time error
        //paginatedData["totalCount"] = total number of discussions for which given user is a reader
        //paginatedData["discussionPostData"] = map of discussionids, each item maps to another map of
        //      ["secondPost"] = message from 2nd post 
        //      ["totalPosts] = total number of posts to this discussion
        // result stores all the discussionid, groupId and userName for discussions associated with groups for which user is a reader
        //    (limited or expanded based on owned or onlyOwned flags, also limited by the paging parameters offset and max
        // discussionIdList makes a collection of all the discussionids from the result
        // allPostCountOrderdByDiscussion is a collection of discussionids and the total number of discussionposts associated with each
        // secondPostList seems to be list of discussionpost.id, message and discussionId for each
        //   discussionpost that is the second discussion post for a discussion
        //   this is used to populate discussionPostData, which is a map, mapping discussionid to another map of
        //       secondPost (message from second post) and totalPosts (total posts for that discussion)
        //       this is the data that is what paginatedData["discussionPostData"] is set to (that is,
        //       discussionPostData is a map entry to a map of discussionIds each mapping to another map of 
        //       second post and totalposts
        //paginatedData["dataList"] = UserGroup.addAdminPermissions:
        //    discussionid is passed in, it is used to fetch a discussion from db
        //    if discussion is new and the fetchUserId() is not same as current user, ignore
        //       discussion.fetchUserId returns the userId property of the discussion (how is that set?)
        //       if userId is null, then instead, it uses the user id of the author of the first post
        //           discussion.userId is, of course set at discussion construction (user object is passed in)
        //               note, fetchUserId is inefficient, it calls user.getAuthor, which uses the user id to look up
        //               author in db only to then just return the same userid. could skip call to db, unless point 
        //               to validate that it is there
        //       retval begins as map of all the discussion properties (getJSONvalues) and then stuff is added
        //              to begin:  id, hash, name, userId, isPublic, created, updated, type
        //         Okay, this could be an issue, because not all of these will come back with the ES search
        //              ES: userid, 'firstPostId', 'name', 'created', 'updated', 'visibility', 'groupIds'
        //              ES (transients): 'groups', 'groupIds', 'isPublic'
        //              MISSING: id, hash, type (hard-coded to 'dis'), name has a hard-coded default when null
        //       retval[groupId] = groupid passed in via info map, that is the discussion list
        //          actually, nothing is passed in, but maybe that is just a temporary thing
        //       retval[isAdmin] = true if either the discussion user id matches userid passed in OR the user passsed in is an admin of the
        //           user group passed in for the discussion (groupid is passed in  and then that is used for db call)
        //       retval[groupName] = full name of the group if group is not null, otherwise, "PRIVATE"
        //       retval[userName] = the username passed in in the map (see below)
        //       retval[isPlot] = first, fetches discussion from db (AGAIN!) then fetches first post from db (AGAIN)
        //                     then checks to see if that first post has a plotid, if it does, this is true, otherwise false
        //                     not sure how the ? operator works, but I believe if there is no post, this value will be null, not false
        //       retval[firstPost] = first post (fetched from db from last step)
        //       okay, actually, this retval map is actually inserted into the retvals list which is the return value
        //          of the entire method, that is, the return value is an array list (declared []) of these retval maps
        //          there is one retval per discussioninfo passed in (described below)
        //          basically, what is passed in is the result of a db call, returning all discussions for the user
        //          it is all discussions for which this user is a reader - there are left joins in the query,
        //          but since the where clause requires the userid matches a record in the left joined
        //          usergroup reader table, they are effectively inner joins
        //       note that there is also a discussionMap that is declared, but, while it is populated locally,
        //       it is never returned. obviously, this method is incomplete
        //
        //       the info passed in is expected to be a list of maps, the map needs to have:
        //          info[discussionId] -- used to look up a discussion in db, which is where the getJSONValues comes from
        //          info[groupId] -- so, I guess you would have one entry per groupid, so a discussionid with many groups would have many entries here
        //          info[userName] -- username of the user associated with the discussion (i.e., the owner of group)
        //        this is pulled from the db via a query as described above (basically these are all discussion for groups for which user is a reader)
        //        it is passed in via the "results" variable
        // so, the final structure of the Map returned by UserGroup.getDiscussionsInfoForUser is as follows:
        // 
        // info["totalCount"]  -- 
        // info["discussionPostData"] -- ES (discussionPost aggregate query) + add missing discussion ids and count of 1 or 0, depending on whether they have first post id
        //      [discussionId] (i.e., 1 per discussionId)
        //          ["secondPost"]
        //          ["totalPosts"]  (add one from aggregate total returned, if first post ignored, otherwise, use total returned.)
        // info["dataList"]
        //      [discussionId] (i.e., 1 per discussionId) (ES discussion query)
        //          ["id"]          -- this is just the discussionid again
        //          ["hash"]        -- will only have if made searchable, but will leave null for now talk to Mitsu
        //          ["name"]        -- ES 
        //          ["userId"]      -- ES
        //          ["isPublic"]    -- ES (transient)
        //          ["created"]     -- ES
        //          ["updated"]     -- ES
        //          ['type"]        -- hardcode
        //              there can be multiple values for the following. shouldn't this be an array, mapped to say, "groups" or "groupInfo" ?
        //              discuss with Mitsu
        //              ['groupId']     -- userGroup (getGroupsForReader) current implementation, if user has multiple groups, the last in the list wins
        //              ['isAdmin']     -- if user passed in owns discussion or userGroup says it's admin (requires db call) can we make GroupMemberAdmin ES searchable?
        //              ['groupName']   -- userGroup (getGroupsForReader) fullname of group
        //          ['userName']    -- username of the owner of the discussion
        //          ['isPlot']      -- need first post for this, maybe aggregate should pull first 2 posts since need it (plotId is searchable)
        //          ['firstPost']   -- as above, makes sense to get first post along with 2nd
        //
        // result:
        // result["listItems"] 
        //      if info["dataList"] null
        //          set to false
        //      else
        //          ["userId"] = user.getId() (from passed in user)
        //          ["groupMemberships"] = UserGroup.getGroupsForReader, db call i.e., usergroups for which user is a reader
        //          ["totalDiscussionCount"] = info["totalCount"] -- this is total count, not restricted by offset and max
        //          ["discussionList"] = info["dataList"]
        //          ["discussionPostData"] = info["discussionPostData"]
        //  
        // result["success"] = true/false, but always true
        //  
        //  Is there a reason for above format? Is it something to do with the UI?
        //  If not, I propose this:
        //  result[listItems"]
        //      ["success"]
        //      ["userId"]
        //      ["groupMemberships"]
        //      ["totalDiscussionCount"]
        //      ["discussionsData"] //arraylist (or possibly map with discussionId as key)
        //          ["discussionId"]
        //          ["name"]        -- ES 
        //          ["userId"]      -- ES
        //          ["userName"]
        //          ["isPublic"]    -- ES (transient)
        //          ["created"]     -- ES
        //          ["updated"]     -- ES
        //          ['type"]        -- hardcode
        //          ['isPlot']      -- need first post for this, maybe aggregate should pull first 2 posts since need it (plotId is searchable)
        //          ["totalPosts"]  (add one from aggregate total returned, if first post ignored, otherwise, use total returned.)
        //          ['firstPost']   -- as above, makes sense to get first post along with 2nd
        //          ["secondPost"]
        //              ['groupId']     -- userGroup (getGroupsForReader) current implementation, if user has multiple groups, the last in the list wins
        //              ['isAdmin']     -- if user passed in owns discussion or userGroup says it's admin (requires db call) can we make GroupMemberAdmin ES searchable?
        //              ['groupName']   -- userGroup (getGroupsForReader) fullname of group

        elasticSearchHelper.withElasticSearch{ client ->
            SearchResponse sr = client
                .prepareSearch("us.wearecurio.model_v0")
                .setTypes("discussionPost")
                .setQuery(
                    boolQuery()
                        .must(
                            queryString("discussionId:" + discussionIdsOr))
                        .mustNot(
                            termQuery("flags",DiscussionPost.FIRST_POST_BIT.toString())
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
                                .setSize(1)  // number of post documents to show PER discussion id
                                .addSort(SortBuilders.fieldSort("created").order(SortOrder.ASC))
                            )
                    )
                .execute()
                .actionGet()
                
            Map model = [
                userId: user.id, 
                groupMemberships: groupMemberships, 
                totalDiscussionCount: discussionResults.searchResults ? discussionResults.searchResults.size() : 0,
                discussionList: discussionResults.searchResults, 
                discussionPostData: false]

//      Map discussionPostData = [:]
//      
//          //separating & populating required data for every discussion
//          discussionIdList.each() { discussionId ->
//              discussionPostData[discussionId] = 
//                  [
//                      "secondPost": secondPostsList.find{ it.discussion_id == discussionId }?.message,
//                      "totalPosts": allPostCountOrderdByDiscussion.find{ it.discussionId == discussionId }?.totalPosts
//                  ]
//          }
//  
//          paginatedData["discussionPostData"] = discussionPostData
  
      
            
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

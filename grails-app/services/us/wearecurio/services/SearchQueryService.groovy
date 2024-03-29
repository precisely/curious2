package us.wearecurio.services

import java.util.List

import us.wearecurio.model.Model
import us.wearecurio.model.User
import us.wearecurio.utility.Utils

class SearchQueryService {
	static String escapeString(String s) {
		if (!s || s == "") {
			return ""
		}
		
		return  s.replaceAll(/([\+\-=><!&\|"\(\)\{\}\[\]\^~\*\?:\/])/,$/\\$1/$)		
	}
	
	static String normalizeQuery(String query) {
		if (!query) {
			return ""
		} 
		String trimmed = query.trim()
		
		if (trimmed == "" || trimmed.matches(/^""$/)) {
			return ""
		}
		
		if (trimmed.matches(/^".*"$/)) {
			return "\"${escapeString(trimmed[1..(trimmed.size()-2)])}\""
//			return  query.replaceAll(/([\+\-=><!&\|\(\)\{\}\[\]\^~\*\?:\/])/,$/\\$1/$)
		}
		
		//def tokens = query.replaceAll(/([\+\-=><!&\|"\(\)\{\}\[\]\^~\*\?:\/])/,$/\\$1/$).split().toUnique()
		def tokens = escapeString(query.trim()).split().toUnique()
		def normalizedTokens = []
		tokens.each {
			if (it.matches($/(^[oO][rR]$|^[aA][nN][dD]$|^[nN][oO][tT]$)/$)) {
				def andOrNot = $/(^[oO][rR]$|^[aA][nN][dD]$|^[nN][oO][tT]$)/$
				normalizedTokens << "(${it.replaceAll(andOrNot, $/"$1"*/$)} OR ${it.replaceAll(andOrNot, $/#$1*/$)})"
			} else if (it.matches($/(^#.*$)/$)){
				if (!tokens.contains(it.substring(1))) {
					normalizedTokens << /${"$it*"}/
				}
			} else {
				normalizedTokens << /(${"$it*"} OR ${"#$it*"})/
			}
		}
	
		if (normalizedTokens.size == 0) {
			return ""
		}
		String ret = normalizedTokens.join(" AND ")
		
		if (normalizedTokens.size == 1) {
			return ret
		} else {
			return "($ret)"		}	
	}

	static enum Role {
		SPRINT_READER(0),
		SPRINT_ADMIN(1),
		DISCUSSION_OWNER(2),
		DISCUSSION_ADMIN(3),
		DISCUSSION_READER(4),
		USER_FOLLOWER(5),
		SPRINT_OWNER(6),
		DISCUSSION_FOLLOWER(7)
		
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
				case 7:
					return DISCUSSION_FOLLOWER
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
				visibility << Model.Visibility.PRIVATE
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
		
	static String getDiscussionActivityQueryString(
		Long userId, 
		List readerGroupIds, 
		List adminGroupIds, 
		List followedUsersGroupIds, 
		List followedSprintsGroupIds,
		List ownedSprintsGroupIds) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def readerGroupsSansFollowingGroups = (readerGroupIds - followedUsersGroupIds) - followedSprintsGroupIds
		def visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_OWNER).collect{ it.toString()})
		discussionQueries << ("(userId:${userId} AND visibility:${visibilitiesOr})")
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(readerGroupsSansFollowingGroups)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(adminGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.USER_FOLLOWER).collect{ it.toString()})
		groupIdsOr =  Utils.orifyList(followedUsersGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_READER).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followedSprintsGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(ownedSprintsGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_FOLLOWER).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(readerGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(virtualUserGroupIdFollowers:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		return "(${Utils.orifyList(discussionQueries)} AND _type:discussion)"
		//return "(${Utils.orifyList(discussionQueries)} AND _type:discussion and hasRecentPost:true)"
	}
	
	static String getSprintActivityQueryString(
		Long userId, 
		List readerGroupIds, 
		List adminGroupIds, 
		List followedUsersIds, 
		List followedSprintsGroupIds) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def sprintQueries = []
		
		def sprintReaderGroupIds = readerGroupIds.intersect( followedSprintsGroupIds )
		def sprintAdminGroupIds = adminGroupIds.intersect( followedSprintsGroupIds )
        
		def visibilitiesOr = Utils.orifyList(getVisibilityForSprint(Role.SPRINT_OWNER).collect{ it.toString()})
		sprintQueries << ("(userId:${userId} AND visibility:${visibilitiesOr})")
		
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
		//def userIdsOr =  Utils.orifyList(followingUserIds)
		def userIdsOr = Utils.orifyList(followedUsersIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && userIdsOr != null && userIdsOr != "") {
			sprintQueries << ("(userId:${userIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		if (sprintQueries.size() > 0) {
            return "(${Utils.orifyList(sprintQueries)} AND _type:sprint AND _exists_:description AND ((NOT _exists_:deleted) OR deleted:false))"
			//return "(${Utils.orifyList(sprintQueries)} AND _type:sprint AND hasRecentPost:true)"
		} else {
			return ""
		}
	}

	static String getDiscussionSearchGroup1QueryString(
		Long userId, 
		String query, 
		List readerGroupIds, 
		List adminGroupIds, 
		List followedUsersGroupIds, 
		List followedSprintsGroupIds,
		List ownedSprintsGroupIds) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def readerGroupsSansFollowingGroups = (readerGroupIds - followedUsersGroupIds) - followedSprintsGroupIds
								
		def visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_OWNER).collect{ it.toString()})
		discussionQueries << ("(userId:${userId} AND visibility:${visibilitiesOr})")
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_READER).collect{ it.toString()})
		def groupIdsOr = Utils.orifyList(readerGroupsSansFollowingGroups)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.DISCUSSION_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(adminGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.USER_FOLLOWER).collect{ it.toString()})
		//groupIdsOr =  Utils.orifyList(followingUserUserGroupIds)
		groupIdsOr =  Utils.orifyList(followedUsersGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_READER).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(followedSprintsGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}

		visibilitiesOr = Utils.orifyList(getVisibilityForDiscussion(Role.SPRINT_ADMIN).collect{ it.toString()})
		groupIdsOr = Utils.orifyList(ownedSprintsGroupIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && groupIdsOr != null && groupIdsOr != "") {
			discussionQueries << ("(groupIds:${groupIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		return "((${Utils.orifyList(discussionQueries)}) AND (name:($query) OR posts:($query) OR firstPostMessage:($query) OR username:($query) OR postUsernames:($query)) AND _type:discussion)"
	}
	
	static String getSprintSearchGroup1QueryString(
		Long userId, 
		String query, 
		List readerGroupIds, 
		List adminGroupIds, 
		List followedUsersIds, 
		List followedSprintsGroupIds) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def sprintQueries = []
		
		def sprintReaderGroupIds = readerGroupIds.intersect( followedSprintsGroupIds )
		def sprintAdminGroupIds = adminGroupIds.intersect( followedSprintsGroupIds )
		
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
		def userIdsOr = Utils.orifyList(followedUsersIds)
		if (visibilitiesOr != null && visibilitiesOr != "" && userIdsOr != null && userIdsOr != "") {
			sprintQueries << ("(userId:${userIdsOr} AND visibility:${visibilitiesOr})")
		}
		
		if (sprintQueries.size() > 0) {
			return "((${Utils.orifyList(sprintQueries)}) AND (name:($query) OR description:($query) OR username:($query) OR discussionsUsernames:($query)) AND _type:sprint AND ((NOT _exists_:deleted) OR deleted:false))"
		}
	}
	
	static String getUserSearchGroup1QueryString(Long userId, String query, List followedUsersIds) {
        def followedAndUser = followedUsersIds
        if (followedAndUser.find{ it == userId } == null) {
            followedAndUser << userId
        }

		if (followedAndUser.size > 0) {
			return "(((publicName:($query)) OR (publicBio:($query)) OR (publicInterestTagsString:($query)) OR" +
					" (username:($query))) AND _type:user AND _id:${Utils.orifyList(followedAndUser)} AND" +
					" virtual:false AND ((NOT _exists_:deleted) OR deleted:false))"
		} else {
			return ""
		}

		//def followedSansUser = followedUsersIds.findAll{ it != userId }
        
//		if (followedSansUser.size > 0) {
//			return "(((publicName:($query)) OR (publicBio:($query)) OR (publicInterestTagsString:($query)) OR (username:($query))) AND _type:user AND (_id:${Utils.orifyList(followedSansUser)}) AND virtual:false)"
//		} else {
//			return ""
//		}
	}
			
	static String getDiscussionSearchGroup2QueryString(
		Long userId, 
		String query, 
		List readerGroupIds, 
		List adminGroupIds, 
		List followedUsersGroupIds, 
		List followedSprintsGroupIds,
		List ownedSprintsGroupIds) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def discussionQueries = []
		
		def readerGroupsSansFollowingGroups = (readerGroupIds - followedUsersGroupIds) - followedSprintsGroupIds
								
		discussionQueries << ("(userId:${userId})")
		
		def groupIds = []
		groupIds += readerGroupsSansFollowingGroups
		groupIds += adminGroupIds
		groupIds += followedUsersGroupIds
		groupIds += followedSprintsGroupIds
		groupIds += ownedSprintsGroupIds
		if (groupIds.size > 0) {
			discussionQueries << ("(groupIds:${Utils.orifyList(groupIds)})")
		}
		
		return "((NOT (${Utils.orifyList(discussionQueries)})) AND visibility:PUBLIC AND (name:($query) OR posts:($query) OR firstPostMessage:($query) OR username:($query) OR postUsernames:($query)) AND _type:discussion)"
	}
	
	static String getSprintSearchGroup2QueryString(
		Long userId, 
		String query, 
		List readerGroupIds, 
		List adminGroupIds, 
		List followedUsersIds, 
		List followedSprintsGroupIds) {
		// collect queries to be used for an ES query to find all discussions associated with user
		def sprintReaderGroupIds = readerGroupIds.intersect( followedSprintsGroupIds )
		def sprintAdminGroupIds = adminGroupIds.intersect( followedSprintsGroupIds )
				
		def sprintQueries = []
		def groupIds = []
		
		groupIds += sprintReaderGroupIds
		groupIds += sprintAdminGroupIds
		
		if (groupIds.size > 0) {
			sprintQueries << "(virtualGroupId:${Utils.orifyList(groupIds)})"
		}
		
		if (followedUsersIds.size > 0) {
			sprintQueries << "(userId:${Utils.orifyList(followedUsersIds)})"
		}
		
		if (sprintQueries.size > 0) {
			return "((NOT (${Utils.orifyList(sprintQueries)})) AND visibility:PUBLIC AND (name:($query) OR description:($query) OR username:($query) OR discussionsUsernames:($query)) AND _type:sprint AND ((NOT _exists_:deleted) OR deleted:false))"
		} else {
			return "(visibility:PUBLIC AND (name:($query) OR description:($query) OR username:($query) OR discussionsUsernames:($query)) AND _type:sprint AND ((NOT _exists_:deleted) OR deleted:false))"
		}
	}
	
	static String getUserSearchGroup2QueryString(Long userId, String query, List followedUsersIds) {
		def ignoreUserIds = followedUsersIds
		if (ignoreUserIds.find{ it == userId } == null) {
			ignoreUserIds << userId
		}
		
		return "(((publicName:($query)) OR (publicBio:($query)) OR (publicInterestTagsString:($query)) OR" +
				" (username:($query))) AND ((NOT _exists_:deleted) OR deleted:false) AND virtual:false AND _type:user" +
				" AND NOT (_id:${Utils.orifyList(ignoreUserIds)}))"
	}
			
	static String getSearchQuery(
		int group,
		Long userId,
		String query, 
		List readerGroupIds, 
		List adminGroupIds, 
		List followedUsersIds, 
		List followedUsersGroupIds, 
		List followedSprintsGroupIds,
		List ownedSprintsGroupIds,
		def type ) {
				
		def queries = []
		def searchQueryString
		if ((type & SearchService.DISCUSSION_TYPE) > 0) {
			searchQueryString =
				group == 1 ?
				getDiscussionSearchGroup1QueryString(userId, query, readerGroupIds, adminGroupIds, followedUsersGroupIds, followedSprintsGroupIds, ownedSprintsGroupIds) :
				getDiscussionSearchGroup2QueryString(userId, query, readerGroupIds, adminGroupIds, followedUsersGroupIds, followedSprintsGroupIds, ownedSprintsGroupIds)
				
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & SearchService.SPRINT_TYPE) > 0) {
			searchQueryString =
				group == 1 ?
				getSprintSearchGroup1QueryString(userId, query, readerGroupIds, adminGroupIds, followedUsersIds, followedSprintsGroupIds) :
				getSprintSearchGroup2QueryString(userId, query, readerGroupIds, adminGroupIds, followedUsersIds, followedSprintsGroupIds)
				
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if ((type & SearchService.USER_TYPE) > 0) {
			searchQueryString =
				group == 1 ?
				getUserSearchGroup1QueryString(userId, query, followedUsersIds) :
				getUserSearchGroup2QueryString(userId, query, followedUsersIds)
				
			if (searchQueryString != null && searchQueryString != "") {
				queries << searchQueryString
			}
		}
		
		if (queries.size() > 0) {
			return Utils.orifyList(queries)
		}
		
		return ""
	}
                                                      
    static String getSprintNotificationQuery(Long userId, Date begin=null, Date end=null) {
        //TODO: finish implementing
        if (userId == null || userId <= 0) {
            return ""
        }
        
        return "userId:$userId _type:sprint AND ((NOT _exists_:deleted) OR deleted:false)"
    }
    
    
    static String getAllDiscussionNotificationQuery(Long userId) {
        return getDiscussionNotificationQuery(userId, null, null)
    }
                                                      
    static String getDiscussionNotificationQuery(Long userId, Date begin=null, Date end=null) {
        if (userId == null || userId <= 0 || (begin!=null && end!=null && begin>end)) {
            return ""
        }
        
        String query = 
            "(followers:$userId OR userId:$userId) AND hasRecentPost:true AND _type:discussion"
        
        if (begin != null && end != null) {
            //[] for inclusive, {} for exclusive
            query += " AND recentPostCreated:[${Utils.elasticSearchDate(begin)} TO ${Utils.elasticSearchDate(end)}]"
        } else if (begin != null) {
            query += " AND recentPostCreated:[${Utils.elasticSearchDate(begin)} TO *]"
        } else if (end != null) {
            query += " AND recentPostCreated:[* TO ${Utils.elasticSearchDate(end)}]"            
        }
        
        return query
    }
}

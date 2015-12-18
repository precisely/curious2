package us.wearecurio.services.unit

import java.util.List;

import spock.lang.Specification

import us.wearecurio.services.SearchQueryService
import us.wearecurio.utility.Utils

class SearchQueryServiceDiscussionNotificationSpec extends Specification {
  
    static Date beginDate
    static String beginFmt
    static Date endDate
    static String endFmt
    static String base
    
    def setup(){
        beginDate = (new Date()) - 1
        beginFmt = Utils.elasticSearchDate(beginDate)
        endDate = beginDate + 1
        endFmt = Utils.elasticSearchDate(endDate)
        base = "(followers:1 OR userId:1) AND hasRecentPost:true AND _type:discussion"
    }
    
	@spock.lang.Unroll    
    void "test getDiscussionNotificationQuery(#userId, #begin, #end) returns empty string"() {
        when: "SearchQueryService.getDiscussionNotificationQuery is called"
        String query = SearchQueryService.getDiscussionNotificationQuery(userId, end, begin)
        
        then: "valid normalized query is produced"
		query == ""
		
		where:
		userId  |   begin	            |   end
        -1      |   null                |   null
        -1      |   (new Date()) - 1    |   null  
        -1      |   null                |   new Date()  
        -1      |   (new Date()) - 1    |   new Date()   
        0       |   null                |   null    
        0       |   (new Date()) - 1    |   null    
        0       |   null                |   new Date()   
        0       |   (new Date()) - 1    |   new Date()   
        0       |   null                |   null    
        0       |   (new Date()) - 1    |   null     
        0       |   null                |   new Date()    
        0       |   (new Date()) - 1    |   new Date()
        1       |   new Date()          |   (new Date()) - 1
    }
    
	@spock.lang.Unroll    
    void "test getDiscussionNotificationQuery(#userId, #begin, #end) returns correct query string"() {
        when: "SearchQueryService.getDiscussionNotificationQuery is called"
        String query = SearchQueryService.getDiscussionNotificationQuery(userId, end, begin)
        
        then: "valid normalized query is produced"
		query == expected
		
		where:
		userId  | begin      | end      | expected
        1       | null       | null     | base     
        1       | beginDate  | null     | "$base AND recentPostCreated:>=$beginFmt"
        1       | null       | endDate  | "$base AND recentPostCreated:<=$endFmt"
        1       | beginDate  | endDate  | "$base AND recentPostCreated:[$beginFmt TO $endFmt]"
    }    
}

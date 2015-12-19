package us.wearecurio.search.integration

import us.wearecurio.model.Discussion

class FollowDiscussionsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    Discussion discussion
    
    def setup() {
        discussion = Discussion.create(user1, uniqueName)
    }

    void "Test owner follows discussion"() {
        when: "a discussion created by user1"
        def d = Discussion.create(user1, uniqueName)
        
        then: "isFollower returns true for user1"
        d.isFollower(user1.id)
    }
    
    void "Test non-following, non-owners do not follow discussion"() {
        when: "a discussion created by user1"
        def d = Discussion.create(user1, uniqueName)
        
        then: "isFollower returns false for user2"
        !(discussion.isFollower(user2.id))
        
        and: "isFollower returns false for user3"
        !(discussion.isFollower(user3.id))
    }
    
    void "Test user follows discussion"() {
        when: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        then: "isFollower returns true for user2"
        discussion.isFollower(user2.id)
    }
    
    void "Test user unfollow discussion"() {
        given: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        when: "user2 unfollows discussion"
        discussion.removeFollower(user2.id)
        
        then: "isFollower returns false for user2"
        !(discussion.isFollower(user2.id))
    }
    
    void "Test two users follow discussion"() {
        when: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "user3 follows discussion"
        discussion.addFollower(user3.id)
        
        then: "isFollower returns true for user2"
        discussion.isFollower(user2.id)
        
        and: "isFollower returns true for user3"
        discussion.isFollower(user3.id)
    }
    
    void "Test two users follow discussion then one unfollows"() {
        given: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "user3 follows discussion"
        discussion.addFollower(user3.id)
        
        when: "user3 unfollows discussion"
        discussion.removeFollower(user3.id)
        
        then: "isFollower returns true for user2"
        discussion.isFollower(user2.id)
        
        and: "isFollower returns false for user3"
        !(discussion.isFollower(user3.id))
    }
    
    void "Test getAllFollowers with one follower"() {
        when: "user2 follows discussion"
        discussion.addFollower(user2.id)
    
        then: "getAllFollowers returns two users (owner and follower, user2)"
        def followers = discussion.getAllFollowers()
        followers.size == 2
        
        and: "user2 is the one of the followers"
        followers.find{it.hash == user2.hash}
    }
    
    void "Test getAllFollowers with two followers"() {
        when: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "user3 follows discussion"
        discussion.addFollower(user3.id)
    
        then: "getAllFollowers returns three users (owner and followers, user2 and user3)"
        def followers = discussion.getAllFollowers()
        followers.size == 3
        
        and: "user2 is the one of the followers"
        followers.find{it.hash == user2.hash}
        
        and: "user3 is the one of the followers"
        followers.find{it.hash == user3.hash}
    }
    
	//@spock.lang.IgnoreRest
    void "Test getAllFollowers does not include unfollower when only follower (other than owner"() {
        given: "user2 follows discussion"
        discussion.addFollower(user2.id)
            
        when: "user2 unfollows discussion"
        discussion.removeFollower(user2.id)
                                
        then: "getAllFollowers returns one user (owner)"
        def followers = discussion.getAllFollowers()
        followers.size == 1
        
        and: "user2 is not one of the followers"
        followers.find{ it.hash == user2.hash } == null
    }
    
    void "Test getAllFollowers does not include unfollower when starting with two followers"() {
        given: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        and: "user3 follows discussion"
        discussion.addFollower(user3.id)
    
        when: "user3 unfollows discussion"
        discussion.removeFollower(user3.id)
                                
        then: "getAllFollowers returns two users (owner and follower, user2)"
        def followers = discussion.getAllFollowers()
        followers.size == 2
        
        and: "user2 is one of the followers"
        followers.find{ it.hash == user2.hash }
        
        and: "user3 is not one of the followers"
        followers.find{ it.hash == user3.hash } == null
    }  
}

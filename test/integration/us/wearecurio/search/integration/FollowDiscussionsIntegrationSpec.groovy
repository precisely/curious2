package us.wearecurio.search.integration

import us.wearecurio.model.Discussion

class FollowDiscussionsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    Discussion discussion
    
    def setup() {
        discussion = Discussion.create(user1, uniqueName)
    }

    void "Test user follows discussion"() {
        when: "user1 follows discussion"
        discussion.addFollower(user1.id)
        
        then: "isFollower returns true for user1"
        discussion.isFollower(user1.id)
    }
    
    void "Test user unfollow discussion"() {
        given: "user1 follows discussion"
        discussion.addFollower(user1.id)
        
        when: "user1 unfollows discussion"
        discussion.removeFollower(user1.id)
        
        then: "isFollower returns true for user1"
        !(discussion.isFollower(user1.id))
    }
    
    void "Test two users follow discussion"() {
        when: "user1 follows discussion"
        discussion.addFollower(user1.id)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        then: "isFollower returns true for user1"
        discussion.isFollower(user1.id)
        
        and: "isFollower returns true for user2"
        discussion.isFollower(user2.id)
    }
    
    void "Test two users follow discussion then one unfollows"() {
        given: "user1 follows discussion"
        discussion.addFollower(user1.id)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
        
        when: "user2 unfollows discussion"
        discussion.removeFollower(user2.id)
        
        then: "isFollower returns true for user1"
        discussion.isFollower(user1.id)
        
        and: "isFollower returns false for user2"
        !(discussion.isFollower(user2.id))
    }
    
    void "Test isFollowing returns false for non-follower"() {
        when: "only user1 follows discussion"
        discussion.addFollower(user1.id)
    
        then: "isFollower returns false for user2"
        !(discussion.isFollower(user2.id))
        
        and: "isFollower returns false for user3"
        !(discussion.isFollower(user3.id))
    }
    
    void "Test getAllFollowers with one follower"() {
        when: "user1 follows discussion"
        discussion.addFollower(user1.id)
    
        then: "getAllFollowers returns single user"
        def followers = discussion.getAllFollowers()
        followers.size == 1
        
        and: "user1 is the one and only follower"
        followers[0].hash == user1.hash
    }
    
    void "Test getAllFollowers with two followers"() {
        when: "user1 follows discussion"
        discussion.addFollower(user1.id)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
    
        then: "getAllFollowers returns two users"
        def followers = discussion.getAllFollowers()
        followers.size == 2
        
        and: "user1 is the one of the followers"
        followers.find{it.hash == user1.hash}
        
        and: "user2 is the one of the followers"
        followers.find{it.hash == user2.hash}
    }
    
    void "Test getAllFollowers does not include unfollower"() {
        given: "user1 follows discussion"
        discussion.addFollower(user1.id)
        
        and: "user2 follows discussion"
        discussion.addFollower(user2.id)
    
        when: "user2 unfollows discussion"
        discussion.removeFollwer(user2.id)
                                
        then: "getAllFollowers returns single user"
        def followers = discussion.getAllFollowers()
        followers.size == 1
        
        and: "user1 is the one and only follower"
        followers[0].hash == user1.hash
    }  
}

package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Entry
import us.wearecurio.model.GroupMemberReader
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.UserGroup

import us.wearecurio.services.SearchService

import us.wearecurio.support.EntryStats

import us.wearecurio.utility.Utils

import spock.lang.*

import org.springframework.transaction.annotation.Transactional

class GetActivitySprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {

    Sprint sprint1
    Sprint sprint2
    Sprint sprint3
    Discussion discussion1
    Discussion discussion2
    Discussion discussion3
    DiscussionPost post1
    DiscussionPost post2
    DiscussionPost post3

	def setup() {
        Date curDate = new Date()
        
        sprint1 = Sprint.create(curDate, user1, "$uniqueName", Visibility.PUBLIC)
        sprint2 = Sprint.create(curDate-1, user1, "$uniqueName", Visibility.PUBLIC)
        sprint3 = Sprint.create(curDate-2, user1, "$uniqueName", Visibility.PUBLIC)
        
        sprint1.addWriter(user2.id)
        sprint2.addWriter(user2.id)
        sprint3.addWriter(user2.id)
		
        discussion1 = Discussion.create(user2, "$uniqueName", sprint1.fetchUserGroup(), curDate-6, Visibility.PUBLIC)
		discussion2 = Discussion.create(user2, "$uniqueName", sprint2.fetchUserGroup(), curDate-7, Visibility.PUBLIC)
		discussion3 = Discussion.create(user2, "$uniqueName", sprint3.fetchUserGroup(), curDate-8, Visibility.PUBLIC)
        
        post1 = discussion1.createPost(user2, "$uniqueName", curDate - 5)
        post2 = discussion2.createPost(user2, "$uniqueName", curDate - 4)
        post3 = discussion3.createPost(user2, "$uniqueName", curDate - 3)
                
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")        
    }
    
    def cleanup() {
        post3.delete(flush: true)
        post2.delete(flush: true)
        post1.delete(flush: true)
        
        discussion3.delete(flush: true)
        discussion2.delete(flush: true)
        discussion1.delete(flush: true)
        
        Entry.findAllByUserId(sprint1.virtualUserId).each{ it.delete(flush: true) }
        Entry.findAllByUserId(sprint2.virtualUserId).each{ it.delete(flush: true) }
        Entry.findAllByUserId(sprint3.virtualUserId).each{ it.delete(flush: true) }
        
        sprint3.delete(flush: true)
        sprint2.delete(flush: true)
        sprint1.delete(flush: true)
    }
        
    def print() {
        println ""
        println "sprint1: hash:${sprint1.hash} userId:${sprint1.userId} hasRecentPost:${sprint1.hasRecentPost} visibility:${sprint1.visibility}"
        println "sprint3: hash:${sprint2.hash} userId:${sprint2.userId} hasRecentPost:${sprint2.hasRecentPost} visibility:${sprint2.visibility}"
        println "sprint3: hash:${sprint3.hash} userId:${sprint3.userId} hasRecentPost:${sprint3.hasRecentPost} visibility:${sprint3.visibility}"
        println ""
        println "discussion1.hash: ${discussion1.hash}"
        println "discussion2.hash: ${discussion2.hash}"
        println "discussion3.hash: ${discussion3.hash}"    
        println ""
        println "post1.hash: ${post1.message}"
        println "post2.hash: ${post2.message}"
        println "post3.hash: ${post3.message}"    
    }
    
    def print(def results) {
        print()
        println ""
        if (results == null|| !results.success || results.listItems.size == 0) {
            println "no results"
            return
        }
        
        results.listItems.each{ println "it.type:${it.type} it.hash:${it.hash}"}
    }
    
    void "Test getActivity for sprints"() {
		when: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
        print(results)
		
        then: "all three sprints a returned"
        results.success
		results.listItems.size() == 3
        results.listItems.find{it.type == "spr" && it.hash == sprint1.hash}
        results.listItems.find{it.type == "spr" && it.hash == sprint2.hash}
        results.listItems.find{it.type == "spr" && it.hash == sprint3.hash}        
	}
	
	void "Test offset for getActivity for sprints"() {
		when: "getActivity is called with offset"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1, 1)

        then: "middle sprint (sprint2) is the first result"        
		results.success
        print(results)
        
        then: "all three sprints a returned"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint2.hash
	}
	
	void "Test max for getActivity for sprints"() {
		when: "getActivity is called with max"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1, 0, 1)
        print(results)

        then: "only most recent sprint (sprint3) is returned"        
		results.success
		results.listItems.size() == 1
		
		and: "single result is sprint3"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint3.hash
	}
	
	void "Test max and offset for getActivity for sprints"() {
		when: "getActivity is called with max and offset"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1, 1, 1)
        print(results)

        then: "only middle sprint (sprint2) is returned"        
		results.success
		results.listItems.size() == 1
		
		and: "single result is sprint2"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint2.hash
	}
	
	void "Test max and offset for getActivity for sprints"() {
		when: "getActivity is called with max and offset"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1, 1, 1)
        print(results)

        then: "only middle sprint (sprint2) is returned"        
		results.success
		results.listItems.size() == 1
		
		and: "single result is sprint2"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint2.hash
	}
	
	void "Test order for getActivity for sprints"() {
		when: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
        print(results)

        then: "middle sprint (sprint2) is the first result"        
		results.success
		results.listItems.size() == 3
		
		and: "results are in correct order"
		results.listItems[0].type == "spr"
        results.listItems[0].hash == sprint3.hash
		results.listItems[1].type == "spr"
        results.listItems[1].hash == sprint2.hash
		results.listItems[2].type == "spr"
        results.listItems[2].hash == sprint1.hash
	}

	@spock.lang.IgnoreRest
    void "Test sprint creation date used in ordering sprint"() {
        given: "a first date"
        Date firstDate = (new Date()) - 1
        
        and: "a new sprint (sprintA)"
        def sprintA = Sprint.create(firstDate, user1, "$uniqueName", Visibility.PUBLIC)
        
        and: "another new sprint (sprintB) created at later date"
        def sprintB = Sprint.create(firstDate + 1, user1, "$uniqueName", Visibility.PUBLIC)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
        print(results)
        
        then: "sprintA and sprintB included in results"
        results.listItems.find{ it.type == "spr" && it.hash == sprintA.hash }
        results.listItems.find{ it.type == "spr" && it.hash == sprintB.hash }
        
        then: "sprintB ahead of sprintA in results"
        results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintB.hash } < 
            results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintA.hash }
    }
    
	@spock.lang.IgnoreRest
    void "Test discussion added date used in ordering sprint"() {
        given: "a first date"
        Date firstDate = (new Date()) - 2
        
        and: "a new sprint (sprintA)"
        def sprintA = Sprint.create(firstDate, user1, "$uniqueName", Visibility.PUBLIC)
        
        and: "another new sprint (sprintB) created at later date"
        def sprintB = Sprint.create(firstDate + 1, user1, "$uniqueName", Visibility.PUBLIC)
        
        and: "a new discussion is added to first sprint at a later date"
        sprintA.addWriter(user2.id)
        Discussion.create(user2, "$uniqueName", sprintA.fetchUserGroup(), firstDate + 2, Visibility.PUBLIC)
        Utils.save(sprintA, true)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
        print(results)
        
        then: "sprintA and sprintB included in results"
        results.listItems.find{ it.type == "spr" && it.hash == sprintA.hash }
        results.listItems.find{ it.type == "spr" && it.hash == sprintB.hash }
        
        then: "sprintA ahead of sprintB in results"
        results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintA.hash } < 
            results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintB.hash }
    }
    
	@spock.lang.IgnoreRest
    void "Test sprint start date not used in ordering sprint"() {
        given: "a first date"
        Date firstDate = (new Date()) - 2
        
        and: "a new sprint (sprintA)"
        def sprintA = Sprint.create(firstDate, user1, uniqueName, Visibility.PUBLIC)
        
        and: "another new sprint (sprintB) created at later date"
        def sprintB = Sprint.create(firstDate + 1, user1, uniqueName, Visibility.PUBLIC)
        
        when: "sprintA is started at a later date"
        Date startDate = firstDate + 2
		String timeZoneName = TimeZoneId.guessTimeZoneNameFromBaseDate(startDate)
		Date baseDate = Utils.getStartOfDay(startDate)
        sprintA.start(user1.id, baseDate, startDate, timeZoneName, new EntryStats())
        
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
        //print(results)
        
        then: "sprintA and sprintB included in results"
        results.listItems.find{ it.type == "spr" && it.hash == sprintA.hash }
        results.listItems.find{ it.type == "spr" && it.hash == sprintB.hash }
        
        and: "sprintB ahead of sprintA in results"
        results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintB.hash } < 
            results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintA.hash }
    }
    
	@spock.lang.IgnoreRest
    void "Test sprint stop date used in ordering sprint"() {
        given: "a first date"
        Date firstDate = (new Date()) - 3
        
        and: "a new sprint (sprintA)"
        def sprintA = Sprint.create(firstDate, user1, uniqueName, Visibility.PUBLIC)
        
        and: "sprintA is started"
        Date startDate = firstDate + 1
		String timeZoneName = TimeZoneId.guessTimeZoneNameFromBaseDate(startDate)
		Date baseDate = Utils.getStartOfDay(startDate)
        sprintA.start(user1.id, baseDate, startDate, timeZoneName, new EntryStats())
        
        and: "another new sprint (sprintB) created at later date"
        def sprintB = Sprint.create(firstDate + 2, user1, uniqueName, Visibility.PUBLIC)
        
        and: "sprintA is stopped at a later date"
        Date stopDate = firstDate + 3
		timeZoneName = TimeZoneId.guessTimeZoneNameFromBaseDate(stopDate)
		baseDate = Utils.getStartOfDay(stopDate)
        sprintA.stop(user1.id, baseDate, stopDate, timeZoneName, new EntryStats())
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
        
        and: "getActivity is called"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
        //print(results)
        
        then: "sprintA and sprintB included in results"
        results.listItems.find{ it.type == "spr" && it.hash == sprintA.hash }
        results.listItems.find{ it.type == "spr" && it.hash == sprintB.hash }
        
        then: "sprintB ahead of sprintA in results"
        results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintB.hash } < 
            results.listItems.findIndexOf{ it.type == "spr" && it.hash == sprintA.hash }
    }
}

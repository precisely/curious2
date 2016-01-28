package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class GetFeedSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {    
	//@spock.lang.IgnoreRest
	void "Test getFeed for sprints"() {
        given: "a new sprint"
        def sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		sprint.description = uniqueName
		Utils.save(sprint, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        //println "printing results..."
        //print(results)
		
		then: "sprint is returned"
        results.success
		results.listItems.size == 1
        results.listItems[0].type == "spr" 
		results.listItems[0].hash == sprint.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test offset for getFeed for sprints"() {
		given: "a date"
		Date origDate = new Date() - 2
		
        and: "a new sprint (sprint1)"
        def sprint1 = Sprint.create(origDate, user1, uniqueName, Visibility.PUBLIC)
		sprint1.description = uniqueName
		Utils.save(sprint1, true)

        and: "another sprint (sprint2)"
        def sprint2 = Sprint.create(origDate + 1, user1, uniqueName, Visibility.PUBLIC)
		sprint2.description = uniqueName
		Utils.save(sprint2, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 1)
        //println "printing results..."
        //printModelObj("sprint1", sprint1)
        //printModelObj("sprint2", sprint2)
		//print(results)
		
		then: "only oldest sprint (sprint1) is returned"
        results.success
		results.listItems.size == 1
        results.listItems[0].type == "spr" 
		results.listItems[0].hash == sprint1.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test max for getFeed for sprints"() {
		given: "a date"
		Date origDate = new Date() - 5
		
        and: "a new sprint (sprint1)"
        def sprint1 = Sprint.create(origDate, user1, uniqueName, Visibility.PUBLIC)
		sprint1.description = uniqueName
		Utils.save(sprint1, true)

        and: "another sprint (sprint2)"
        def sprint2 = Sprint.create(origDate + 1, user1, uniqueName, Visibility.PUBLIC)
		sprint2.description = uniqueName
		Utils.save(sprint2, true)
		
        and: "another sprint (sprint3)"
        def sprint3 = Sprint.create(origDate + 2, user1, uniqueName, Visibility.PUBLIC)
		sprint3.description = uniqueName
		Utils.save(sprint3, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1 with max of 2"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 0, 2)
        //println "printing results..."
        //print(results)
		
		then: "only newest 2 sprints (sprint2 and sprint3) are returned"
        results.success
		results.listItems.size == 2
        results.listItems[0].type == "spr" 
		results.listItems[0].hash == sprint3.hash
        results.listItems[1].type == "spr" 
		results.listItems[1].hash == sprint2.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test different sessionId for getFeed for sprints"() {
		given: "a date"
		Date origDate = new Date() - 5
		
        and: "a new sprint (sprint1)"
        def sprint1 = Sprint.create(origDate, user1, uniqueName, Visibility.PUBLIC)
		sprint1.description = uniqueName
		Utils.save(sprint1, true)

        and: "another sprint (sprint2)"
        def sprint2 = Sprint.create(origDate + 1, user1, uniqueName, Visibility.PUBLIC)
		sprint2.description = uniqueName
		Utils.save(sprint2, true)
		
        and: "another sprint (sprint3)"
        def sprint3 = Sprint.create(origDate + 2, user1, uniqueName, Visibility.PUBLIC)
		sprint3.description = uniqueName
		Utils.save(sprint3, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
				
		and: "getFeed is called multiple times for user2 with different sessionIds"
		Random r = new Random()
		def results1 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(1))
		def results2 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(2))
		def results3 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(3))
		def results4 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(4))
		def results5 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(5))
		def results6 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(6))
		def results7 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(7))
		def results8 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(8))
		def results9 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(9))
		def results10 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, r.nextInt(10))
		
		//this is an imperfect test, and in theory could occassionally fail.
		then: "all results return 3"
		results1.success
        results2.success
        results3.success
        results4.success
        results5.success
        results6.success
        results7.success
        results8.success
        results9.success
        results10.success
		results1.listItems.size == 3
		results2.listItems.size == 3
		results3.listItems.size == 3
		results4.listItems.size == 3
		results5.listItems.size == 3
		results6.listItems.size == 3
		results7.listItems.size == 3
		results8.listItems.size == 3
		results9.listItems.size == 3
		results10.listItems.size == 3
		
		and: "all results include all three sprints"
		results1.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results1.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results1.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results2.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results2.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results2.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results3.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results3.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results3.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results4.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results4.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results4.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results5.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results5.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results5.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results6.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results6.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results6.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results7.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results7.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results7.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results8.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results8.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results8.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results9.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results9.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results9.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results10.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results10.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results10.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		
		and: "not all results are in the same order"
		(results1.listItems[0].hash != results2.listItems[0].hash ||
			results1.listItems[0].hash != results3.listItems[0].hash ||
			results1.listItems[0].hash != results4.listItems[0].hash ||
			results1.listItems[0].hash != results5.listItems[0].hash ||
			results1.listItems[0].hash != results6.listItems[0].hash ||
			results1.listItems[0].hash != results7.listItems[0].hash ||
			results1.listItems[0].hash != results8.listItems[0].hash ||
			results1.listItems[0].hash != results9.listItems[0].hash ||
			results1.listItems[0].hash != results10.listItems[0].hash ||
			results1.listItems[1].hash != results2.listItems[1].hash ||
			results1.listItems[1].hash != results3.listItems[1].hash ||
			results1.listItems[1].hash != results4.listItems[1].hash ||
			results1.listItems[1].hash != results5.listItems[1].hash ||
			results1.listItems[1].hash != results6.listItems[1].hash ||
			results1.listItems[1].hash != results7.listItems[1].hash ||
			results1.listItems[1].hash != results8.listItems[1].hash ||
			results1.listItems[1].hash != results9.listItems[1].hash ||
			results1.listItems[1].hash != results10.listItems[1].hash )
	}

	//@spock.lang.IgnoreRest
	void "Test same sessionId for getFeed for sprints"() {
		given: "a date"
		Date origDate = new Date() - 5
		
		and: "a sessionId"
		def sessionId = 9999
		
        and: "a new sprint (sprint1)"
        def sprint1 = Sprint.create(origDate, user1, uniqueName, Visibility.PUBLIC)
		sprint1.description = uniqueName
		Utils.save(sprint1, true)

        and: "another sprint (sprint2)"
        def sprint2 = Sprint.create(origDate + 1, user1, uniqueName, Visibility.PUBLIC)
		sprint2.description = uniqueName
		Utils.save(sprint2, true)
		
        and: "another sprint (sprint3)"
        def sprint3 = Sprint.create(origDate + 2, user1, uniqueName, Visibility.PUBLIC)
		sprint3.description = uniqueName
		Utils.save(sprint3, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
				
		and: "getFeed is called multiple times for user2 with same sessionIds"
		Random r = new Random()
		def results1 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results2 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results3 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results4 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results5 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results6 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results7 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results8 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results9 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		def results10 = searchService.getFeed(SearchService.SPRINT_TYPE, user2, 0, 10, 0, sessionId)
		
		//this is an imperfect test, and in theory could occassionally fail.
		then: "all results return 3"
		results1.success
        results2.success
        results3.success
        results4.success
        results5.success
        results6.success
        results7.success
        results8.success
        results9.success
        results10.success
		results1.listItems.size == 3
		results2.listItems.size == 3
		results3.listItems.size == 3
		results4.listItems.size == 3
		results5.listItems.size == 3
		results6.listItems.size == 3
		results7.listItems.size == 3
		results8.listItems.size == 3
		results9.listItems.size == 3
		results10.listItems.size == 3
		
		and: "all results include all three sprints"
		results1.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results1.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results1.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results2.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results2.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results2.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results3.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results3.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results3.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results4.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results4.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results4.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results5.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results5.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results5.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results6.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results6.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results6.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results7.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results7.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results7.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results8.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results8.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results8.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results9.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results9.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results9.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		results10.listItems.find{ it.type == "spr" && it.hash == sprint1.hash}
		results10.listItems.find{ it.type == "spr" && it.hash == sprint2.hash}
		results10.listItems.find{ it.type == "spr" && it.hash == sprint3.hash}
		
		and: "all results are in the same order"
		results1.listItems[0].hash == results2.listItems[0].hash
		results1.listItems[0].hash == results3.listItems[0].hash
		results1.listItems[0].hash == results4.listItems[0].hash 
		results1.listItems[0].hash == results5.listItems[0].hash 
		results1.listItems[0].hash == results6.listItems[0].hash 
		results1.listItems[0].hash == results7.listItems[0].hash 
		results1.listItems[0].hash == results8.listItems[0].hash 
		results1.listItems[0].hash == results9.listItems[0].hash 
		results1.listItems[0].hash == results10.listItems[0].hash 
		results1.listItems[1].hash == results2.listItems[1].hash
		results1.listItems[1].hash == results3.listItems[1].hash
		results1.listItems[1].hash == results4.listItems[1].hash 
		results1.listItems[1].hash == results5.listItems[1].hash 
		results1.listItems[1].hash == results6.listItems[1].hash 
		results1.listItems[1].hash == results7.listItems[1].hash 
		results1.listItems[1].hash == results8.listItems[1].hash 
		results1.listItems[1].hash == results9.listItems[1].hash 
		results1.listItems[1].hash == results10.listItems[1].hash 
		results1.listItems[2].hash == results2.listItems[2].hash
		results1.listItems[2].hash == results3.listItems[2].hash
		results1.listItems[2].hash == results4.listItems[2].hash 
		results1.listItems[2].hash == results5.listItems[2].hash 
		results1.listItems[2].hash == results6.listItems[2].hash 
		results1.listItems[2].hash == results7.listItems[2].hash 
		results1.listItems[2].hash == results8.listItems[2].hash 
		results1.listItems[2].hash == results9.listItems[2].hash 
		results1.listItems[2].hash == results10.listItems[2].hash 
	}

	//@spock.lang.IgnoreRest
	void "Test nextSuggestionOffset is correct for getFeed for sprints"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "a sprint (sprint1) with tag match in name"
        def sprint1 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        sprint1.description = uniqueName
        Utils.save(sprint1)
        
        and: "another sprint (sprint2) with tag match in name"
        def sprint2 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        sprint2.description = uniqueName
        Utils.save(sprint2)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        //println "printing results..."
        //print(results)
        
        then: "sprint1 and sprint2 are returned"
        results.success
		results.listItems.size == 2
		results.listItems.find{it.type == "spr" && it.hash == sprint1.hash}
		results.listItems.find{it.type == "spr" && it.hash == sprint2.hash}
		results.nextSuggestionOffset == 2
	}
	
	//@spock.lang.IgnoreRest
	void "Test suggestionOffset returns correct value for getFeed for sprints"() {
		given: "an old date"
		Date firstDate = new Date() - 5
		
		and: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "a sprint (sprint1) with tag match in name"
        def sprint1 = Sprint.create(firstDate, user2, tagText, Visibility.PUBLIC)         
        sprint1.description = uniqueName
        Utils.save(sprint1)
        
        and: "another sprint (sprint2) with tag match in name"
        def sprint2 = Sprint.create(firstDate + 1, user2, tagText, Visibility.PUBLIC)         
        sprint2.description = uniqueName
        Utils.save(sprint2)
        
        and: "another sprint (sprint3) with tag match in name"
        def sprint3 = Sprint.create(firstDate + 1, user2, tagText, Visibility.PUBLIC)         
        sprint3.description = uniqueName
        Utils.save(sprint3)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1 with max of 2"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 0, 2)
        //println "printing results..."
        //print(results)
        
        then: "two sprints are returned"
        results.success
		results.listItems.size == 2
		results.listItems.count{it.type == "spr" && 
			(it.hash == sprint1.hash) ||
			(it.hash == sprint2.hash) ||
			(it.hash == sprint3.hash)} == 2
			
		when: "nextSuggestionOffset is used"
		def next = results.nextSuggestionOffset
		
		and: "the hashes found in first call are gathered"
		def hash1 = results.listItems[0].hash
		def hash2 = results.listItems[1].hash
		
		and: "the missing hash is found"
		def nextHash
		if (hash1 != sprint1.hash && hash2 != sprint1.hash) {
			nextHash = sprint1.hash
		} else if (hash1 != sprint2.hash && hash2 != sprint2.hash) {
			nextHash = sprint2.hash
		} else {
			nextHash = sprint3.hash
		}
		
		and: "getFeed is called with the nextSuggestionOffset"
		results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 0, 10, next)
		
		then: "the sprint not found in first call is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].type == "spr"
		results.listItems[0].hash == nextHash
	}
	
	//@spock.lang.IgnoreRest
	void "Test offset and suggestionOffset for getFeed for sprints"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "a sprint (owned1) owned by user1"
        def owned1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned1.description = uniqueName
		Utils.save(owned1, true)
		
		and: "another sprint (owned2) owned by user1"
        def owned2 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned2.description = uniqueName
		Utils.save(owned2, true)
		
		and: "a sprint (suggestion1) with tag match in name"
        def suggestion1 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion1.description = uniqueName
        Utils.save(suggestion1)
        
        and: "another sprint (suggestion2) with tag match in name"
        def suggestion2 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion2.description = uniqueName
        Utils.save(suggestion2)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called with the offset and nextSuggestionOffset set to 1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 1, 10, 1)
		
		then: "2 results are returned"
		results.success
		results.listItems.size == 2
		
		and: "one results is an owned item"
		results.listItems.count{ it.type == "spr" && 
			(it.hash == owned1.hash) ||
			(it.hash == owned2.hash) } == 1
		
		and: "one result is a suggestion"
		results.listItems.count{ it.type == "spr" && 
			(it.hash == suggestion1.hash) ||
			(it.hash == suggestion2.hash) } == 1
	}
	
	//@spock.lang.IgnoreRest
	void "Test max and suggestionOffset for getFeed for sprints"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "a sprint (owned1) owned by user1"
        def owned1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned1.description = uniqueName
		Utils.save(owned1, true)
		
		and: "another sprint (owned2) owned by user1"
        def owned2 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned2.description = uniqueName
		Utils.save(owned2, true)
		
		and: "a sprint (suggestion1) with tag match in name"
        def suggestion1 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion1.description = uniqueName
        Utils.save(suggestion1)
        
        and: "another sprint (suggestion2) with tag match in name"
        def suggestion2 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion2.description = uniqueName
        Utils.save(suggestion2)
        
        and: "another sprint (suggestion3) with tag match in name"
        def suggestion3 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion3.description = uniqueName
        Utils.save(suggestion3)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called with the max and nextSuggestionOffset set to 1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 0, 3, 1)
		
		then: "4 sprints are returned"
		results.success
		results.listItems.size == 4
		
		and: "both owned sprints are returned"		
		results.listItems.count{ it.type == "spr" && 
			(it.hash == owned1.hash) ||
			(it.hash == owned2.hash) } == 2
		
		and: "two suggestions is returned"
		results.listItems.count{ it.type == "spr" && 
			(it.hash == suggestion1.hash) ||
			(it.hash == suggestion2.hash) ||
			(it.hash == suggestion3.hash) } == 2
	}

	//@spock.lang.IgnoreRest
	void "Test max and offset for getFeed for sprints"() {
		given: "an older date"
		Date firstDate = new Date() - 5
		
		and: "a sprint (sprint1) owned by user1"
        def sprint1 = Sprint.create(firstDate, user1, uniqueName, Visibility.PUBLIC)
		sprint1.description = uniqueName
		Utils.save(sprint1, true)
		
		and: "another sprint (sprint2) owned by user1"
        def sprint2 = Sprint.create(firstDate + 1, user1, uniqueName, Visibility.PUBLIC)
		sprint2.description = uniqueName
		Utils.save(sprint2, true)
		
		and: "another sprint (sprint3) owned by user1"
        def sprint3 = Sprint.create(firstDate + 2, user1, uniqueName, Visibility.PUBLIC)
		sprint3.description = uniqueName
		Utils.save(sprint3, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called with the max and nextSuggestionOffset set to 1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 1, 1)
		
		then: "only middle sprint (sprint2) is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].type == "spr"
		results.listItems[0].hash == sprint2.hash
	}

	//@spock.lang.IgnoreRest
	void "Test max, offset and suggestionOffset for getFeed for sprints"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "a sprint (owned1) owned by user1"
        def owned1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned1.description = uniqueName
		Utils.save(owned1, true)
		
		and: "another sprint (owned2) owned by user1"
        def owned2 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned2.description = uniqueName
		Utils.save(owned2, true)
		
		and: "another sprint (owned3) owned by user1"
        def owned3 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned3.description = uniqueName
		Utils.save(owned3, true)
		
		and: "a sprint (suggestion1) with tag match in name"
        def suggestion1 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion1.description = uniqueName
        Utils.save(suggestion1)
        
        and: "another sprint (suggestion2) with tag match in name"
        def suggestion2 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion2.description = uniqueName
        Utils.save(suggestion2)
        
        and: "another sprint (suggestion3) with tag match in name"
        def suggestion3 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion3.description = uniqueName
        Utils.save(suggestion3)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called with offset, max and nextSuggestionOffset set to 1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 1, 1, 1)
		
		then: "3 sprints are returned"
		results.success
		results.listItems.size == 3
		
		and: "one owned sprint is returned"		
		results.listItems.count{ it.type == "spr" && 
			(it.hash == owned1.hash) ||
			(it.hash == owned2.hash) ||
			(it.hash == owned3.hash) } == 1
		
		and: "two suggestions are returned"
		results.listItems.count{ it.type == "spr" && 
			(it.hash == suggestion1.hash) ||
			(it.hash == suggestion2.hash) ||
			(it.hash == suggestion3.hash) } == 2
	}

	@spock.lang.IgnoreRest
	void "Test max suggestions is 3 for getFeed for sprints"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "a sprint (owned1) owned by user1"
        def owned1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned1.description = uniqueName
		Utils.save(owned1, true)
		
		and: "another sprint (owned2) owned by user1"
        def owned2 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned2.description = uniqueName
		Utils.save(owned2, true)
		
		and: "another sprint (owned3) owned by user1"
        def owned3 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		owned3.description = uniqueName
		Utils.save(owned3, true)
		
		and: "a sprint (suggestion1) with tag match in name"
        def suggestion1 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion1.description = uniqueName
        Utils.save(suggestion1)
        
        and: "another sprint (suggestion2) with tag match in name"
        def suggestion2 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion2.description = uniqueName
        Utils.save(suggestion2)
        
        and: "another sprint (suggestion3) with tag match in name"
        def suggestion3 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion3.description = uniqueName
        Utils.save(suggestion3)
        
        and: "another sprint (suggestion4) with tag match in name"
        def suggestion4 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion4.description = uniqueName
        Utils.save(suggestion4)
        
        and: "another sprint (suggestion5) with tag match in name"
        def suggestion5 = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        suggestion5.description = uniqueName
        Utils.save(suggestion5)
        
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called with offset, max and nextSuggestionOffset set to 1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 1, 1, 1)
		
		then: "4 sprints are returned"
		results.success
		results.listItems.size == 4
		
		and: "one owned sprint is returned"		
		results.listItems.count{ it.type == "spr" && 
			(it.hash == owned1.hash) ||
			(it.hash == owned2.hash) ||
			(it.hash == owned3.hash) } == 1
		
		and: "three suggestions are returned"
		results.listItems.count{ it.type == "spr" && 
			(it.hash == suggestion1.hash) ||
			(it.hash == suggestion2.hash) ||
			(it.hash == suggestion3.hash) ||
			(it.hash == suggestion4.hash) ||
			(it.hash == suggestion5.hash)} == 3
	}

	//@spock.lang.IgnoreRest
	void "Test getFeed does not return duplicates of sprints when sprint edited"() {
        given: "a new sprint"
        def sprint = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		sprint.description = uniqueName
		Utils.save(sprint, true)

		when: "the sprint is edited and saved"
		sprint.description += " $uniqueName"
		Utils.save(sprint, true)
		
		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        //println "printing results..."
        //print(results)
		
		then: "sprint is returned only once"
        results.success
		results.listItems.size > 0
        results.listItems.count{ it.type == "spr" && it.hash == sprint.hash } == 1
	}
	
	//@spock.lang.IgnoreRest
	void "Test getFeed does not return duplicates of sprints when max sprints created and last sprint edited"() {
        given: "a new sprint (sprint1)"
        def sprint1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		sprint1.description = uniqueName
		Utils.save(sprint1, true)

		and: "another new sprint (sprint2)"
		def sprint2 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		sprint2.description = uniqueName
		Utils.save(sprint2, true)
		
		and: "another new sprint (sprint3)"
		def sprint3 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		sprint3.description = uniqueName
		Utils.save(sprint3, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getFeed is called for user1 with offset of 2"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1, 2, 5)
        //println "printing results..."
        //print(results)
		
		then: "only one sprint is returned"
        results.success
		results.listItems.size == 1
        results.listItems.find{ it.type == "spr" && 
			(it.hash == sprint1.hash) ||
			(it.hash == sprint2.hash) ||
			(it.hash == sprint3.hash)}
	}
	
    //@spock.lang.IgnoreRest
    void "Test getFeed does not return unedited sprint with empty description but tag match"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with empty description, but tag match in title"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = ""
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        //println "printing results..."
        //print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getFeed does not return unedited sprint with null description but tag match"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
        
        and: "an unedited sprint with null description, but tag match in name"
        def unedited = Sprint.create(new Date(), user2, tagText, Visibility.PUBLIC)         
        unedited.description = null
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        //println "printing results..."
        //print(results)
        
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}

    //@spock.lang.IgnoreRest
    void "Test getFeed does not return unedited sprint with empty description"() {
        given: "an untitled sprint with empty description"
        def unedited = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)         
        unedited.description = ""
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        
        //println "printing results..."
        //print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}
    
    //@spock.lang.IgnoreRest
    void "Test getFeed does not return unedited sprint with null description"() {
        given: "an untitled sprint with null description"
        def unedited = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC) 
        unedited.description = null
        Utils.save(unedited)
        
		when: "getFeed is called"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
        //println "printing results..."
        //print(results)
		
        then: "unedited sprint is not returned"
        results.success
        results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}    
}

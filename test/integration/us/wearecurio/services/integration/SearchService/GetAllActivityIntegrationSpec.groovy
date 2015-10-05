package us.wearecurio.services.integration.SearchService

import java.text.DateFormat

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.UserActivity
import us.wearecurio.model.UserGroup
import us.wearecurio.model.Tag
import us.wearecurio.utility.Utils

class GetAllActivityIntegrationSpec extends SearchServiceIntegrationSpecBase {

    void "Test sprint and discussion included for all user activities"() {
		given: "user3 with interest tags"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user3.addInterestTag(tag)
		
		and: "user3 following user2"
		user2.follow(user3)	
		
		when: "a new sprint is created by user1 with name matching user2's interest tags"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint.description = getUniqueName()
		
		and: "a new discussion is created by user2 with no matching interest tags"
		def discussion = Discussion.create(user2, getUniqueName())
		discussion.visibility = Visibility.PUBLIC

		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getAllActivity returns 2 results"
		def results = searchService.getAllActivity(user3)
		results.success
		results.listItems.size() == 2
		
		and: "one result is for the creation of sprint"
		def s = results.listItems.find{ it.objectId == sprint.id && it.type == "act-created-spr" }
		s != null
		s.userId == user1.id
		s.userName == user1.name
		s.objectDescription == sprint.name
		s.otherId == null
		s.otherDescription == null
		
		and: "one result is for the creation of discussion"
		def d = results.listItems.find{ it.objectId == discussion.id && it.type == "act-created-dis" }
		d.userId == user2.id
		d.userName == user2.name
		d.objectDescription == discussion.name
		d.otherId == null
		d.otherDescription == null
    }
	
	//@spock.lang.Ignore
	void "Test max for all user activities"() {
		given: "user3 with interest tags"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user3.addInterestTag(tag)
		
		and: "user3 following user2"
		user2.follow(user3)	
		
		when: "sprint1 is created by user1 with name matching user2's interest tags"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		
		and: "sprint2 is created by user1 with name matching user2's interest tags"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint2.description = getUniqueName()
		
		and: "a new discussion is created by user2 with no matching interest tags"
		def discussion = Discussion.create(user2, getUniqueName())
		discussion.visibility = Visibility.PUBLIC

		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getAllActivity returns 2 results"
		def results = searchService.getAllActivity(user3, 0, 2)
		results.success
		results.listItems.size() == 2
		
		and: "one result is for the creation of sprint"
		def all = results.listItems.findAll{ 
			((it.objectId == sprint1.id || it.objectId == sprint2.id) && it.type == "act-created-spr") ||
			(it.objectId == discussion.id && it.type == "act-created-dis")
		}
		all != null
		all.size() == 2
	}
	
	boolean areEqual(def result1, def result2) {
		return (
			result1.userId == result2.userId &&
			result1.userName == result2.userName &&
			result1.type == result2.type &&
			result1.objectId == result2.objectId &&
			result1.objectDescription == result2.objectDescription &&
			result1.otherId == result2.otherId &&
			result1.otherDescription == result2.otherDescription
		)
	}
	
	//@spock.lang.Ignore
	void "Test offset for all user activities"() {
		given: "user3 with interest tags"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user3.addInterestTag(tag)
		
		and: "user3 following user2"
		user2.follow(user3)	
		
		when: "sprint1 is created by user1 with name matching user2's interest tags"
		def sprint1 = Sprint.create(currentTime, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		
		and: "sprint2 is created by user1 with name matching user2's interest tags"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint2.description = getUniqueName()
		
		and: "a new discussion is created by user2 with no matching interest tags"
		def discussion = Discussion.create(user2, getUniqueName())
		discussion.visibility = Visibility.PUBLIC

		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "getAllActivity is called with no offset"
		def results = searchService.getAllActivity(user3, 0, 2)
		
		then: "two results are returned"
		results.success
		results.listItems.size() == 2
		
		and: "each result corresponds to one of the items created above"
		def all1 = results.listItems.findAll{ 
			((it.objectId == sprint1.id || it.objectId == sprint2.id) && it.type == "act-created-spr") ||
			(it.objectId == discussion.id && it.type == "act-created-dis")
		}
		all1 != null
		all1.size() == 2
		
		when: "getAllActivity called with offset of 1"
		results = searchService.getAllActivity(user3, 1, 2)
		
		then: "two results are returned"
		results.success
		results.listItems.size() == 2
		
		and: "each result corresponds to one of the items created above"
		def all2 = results.listItems.findAll{
			((it.objectId == sprint1.id || it.objectId == sprint2.id) && it.type == "act-created-spr") ||
			(it.objectId == discussion.id && it.type == "act-created-dis")
		}
		all2 != null
		all2.size() == 2
		
		and: "only one result intersects with results from call to getAllActivity with no offset"
		!areEqual(all1[0], all2[0])
		!areEqual(all1[0], all2[1])
		areEqual(all1[1], all2[0])
		!areEqual(all1[1], all2[1])

		when: "getAllActivity called with offset of 2"
		results = searchService.getAllActivity(user3, 2, 2)
		
		then: "one result is returned"
		results.success
		results.listItems.size() == 1
		
		and: "result corresponds to one of the items created above"
		def all3 = results.listItems.findAll{
			((it.objectId == sprint1.id || it.objectId == sprint2.id) && it.type == "act-created-spr") ||
			(it.objectId == discussion.id && it.type == "act-created-dis")
		}
		all3 != null
		all3.size() == 1
		
		and: "the one result intersects with results from call to getAllActivity with offset of 1"
		!areEqual(all2[0], all3[0])
		areEqual(all2[1], all3[0])
		
		and: "the one result does not intersect with results from call to getAllActivity with no offset"
		!areEqual(all1[0], all3[0])
		!areEqual(all1[1], all3[0])
	}
	
	//@spock.lang.Ignore
	void "Test order descending date for all user activities"() {
		given: "user2 with interest tags"
		def tagText = "MyInterestTag"
		def tag = Tag.create(tagText)
		user2.addInterestTag(tag)		
		
		and: "the current time"
		Date now = new Date()

		when: "sprint1 is created by user1 with name matching user2's interest tags and current time + 1"
		def sprint1 = Sprint.create(now - 2, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		
		and: "sprint2 is created by user1 with name matching user2's interest tags and current time"
		def sprint2 = Sprint.create(now - 4, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint2.description = getUniqueName()
		
		and: "sprint3 is created by user1 with name matching user2's interest tags and current time - 1"
		def sprint3 = Sprint.create(now, user1, getUniqueName() + " " + tagText, Visibility.PUBLIC)
		sprint2.description = getUniqueName()

		and: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "getAllActivity is called"
		def results = searchService.getAllActivity(user2)
		println ""
		println "sprint1.id: " + sprint1.id
		println "sprint2.id: " + sprint2.id
		println "sprint3.id: " + sprint3.id
		println ""
		println "listItems"
		println ""
		results.listItems.each { println "objectId: " + it.objectId + " created: " + it.created + " score: " + it.score}

		then: "three results are returned"
		results.success
		results.listItems.size() == 3
		
		and: "sprint3 is the first result"
		results.listItems[0].type == "act-created-spr"
		results.listItems[0].objectId == sprint3.id
		
		and: "sprint1 is the second result"
		results.listItems[1].type == "act-created-spr"
		results.listItems[1].objectId == sprint1.id
		
		and: "sprint2 is the third result"
		results.listItems[2].type == "act-created-spr"
		results.listItems[2].objectId == sprint2.id		
	}
	
	@spock.lang.Ignore
	void "Test order discussion post over discussion creation for all user activities"() {
	}
	
	@spock.lang.Ignore
	void "Test order discussion post over sprint creation for all user activities"() {
	}
	
	@spock.lang.Ignore
	void "Test order sprint creation over discussion creation for all user activities"() {
	}
	
	@spock.lang.Ignore
	void "Test order followed discussion post over interest tag match public discussion for all user activities"() {
	}
	
	@spock.lang.Ignore
	void "Test public discussions show up for user not following and with no interest tags"() {
	}
	
	@spock.lang.Ignore
	void "Test public sprints show up for user not following and with no interest tags"() {
	}

	@spock.lang.Ignore
	void "Test public discussions and public sprints show up for user not following and with no interest tags"() {
	}
}

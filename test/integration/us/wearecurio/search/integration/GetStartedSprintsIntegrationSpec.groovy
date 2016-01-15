package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.TimeZoneId

import us.wearecurio.support.EntryStats

import us.wearecurio.utility.Utils

class GetStartedSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {

	void startSprint(Sprint s, Long userId) {
        Date startDate = new Date()
		String timeZoneName = TimeZoneId.guessTimeZoneNameFromBaseDate(startDate)
		Date baseDate = Utils.getStartOfDay(startDate)
        s.start(userId, baseDate, startDate, timeZoneName, new EntryStats())		
	}
	
	void stopSprint(Sprint s, Long userId) {
        Date startDate = new Date()
		String timeZoneName = TimeZoneId.guessTimeZoneNameFromBaseDate(startDate)
		Date baseDate = Utils.getStartOfDay(startDate)
        s.stop(userId, baseDate, startDate, timeZoneName, new EntryStats())		
	}
	
    void "Test getStartedSprints returns started and owned sprint"() {
		given: "a new sprint"
		Sprint s = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		
		when: "user starts sprint"
		startSprint(s, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called"
		def results = searchService.getStartedSprints(user1.id)
		
		then: "sprint is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
   }
	
    void "Test getStartedSprints does not return sprint that hasn't been started"() {	
		given: "a new sprint"
		Sprint s = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called"
		def results = searchService.getStartedSprints(user1.id)
		
		then: "sprint is returned"
		results.success
		results.listItems.size == 0
    }

    void "Test getStartedSprints does not return started sprint that has been stopped"() {	
		given: "a new sprint"
		Sprint s = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		
		when: "user starts sprint"
		startSprint(s, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		sleep(1000)
		
		and: "user stops sprint"
		stopSprint(s, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
				
		and: "getStartedSprints is called"
		def results = searchService.getStartedSprints(user1.id)
		
		then: "sprint is returned"
		results.success
		results.listItems.size == 0
    }

    void "Test getStartedSprints returns multiple started sprint"() {
		given: "a new sprint owned by user1"
		Sprint s1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		
		and: "a new sprint owned by user2"
		Sprint s2 = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
		
		when: "user1 starts sprint"
		startSprint(s1, user1.id)
		
		and: "user2 starts sprint"
		startSprint(s2, user2.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called for user1"
		def results = searchService.getStartedSprints(user1.id)
		
		then: "sprint s1 is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s1.hash
		
		when: "getStartedSprints is called for user2"
		results = searchService.getStartedSprints(user2.id)
		
		then: "sprint s1 is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s2.hash
    }
	
    void "Test getStartedSprints returns sprint that has been started, stopped and started"() {	
		given: "a new sprint"
		Sprint s = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		
		when: "user starts sprint"
		startSprint(s, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		sleep(1000)
		
		and: "user stops sprint"
		stopSprint(s, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		sleep(1000)
				
		and: "user starts sprint"
		startSprint(s, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called"
		def results = searchService.getStartedSprints(user1.id)
		
		then: "sprint is returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s.hash
    }
	
	void "Test getStartedSprints does not return sprints started by others"() {
		given: "a new sprint owned by user1"
		Sprint s = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		
		and: "user2 is sprint reader"
		s.addReader(user2.id)
		
		when: "user2 starts sprint"
		startSprint(s, user2.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called for user1"
		def results = searchService.getStartedSprints(user1.id)
		
		then: "sprint s1 is returned"
		results.success
		results.listItems.size == 0
	}
	
	void "Test getStartedSprints offset"() {
		given: "three new sprints"
		Sprint s1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		Sprint s2 = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
		Sprint s3 = Sprint.create(new Date(), user3, uniqueName, Visibility.PUBLIC)
		
		and: "user1 is reader of all sprints"
		s2.addReader(user1.id)
		s3.addReader(user1.id)
		
		when: "user starts all three sprints"
		startSprint(s1, user1.id)
		sleep(1000)
		startSprint(s2, user1.id)
		sleep(1000)
		startSprint(s3, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called with offset"
		def results = searchService.getStartedSprints(user1.id, 1)
		
		then: "only sprints s2 and s1 are returned"
		results.success
		results.listItems.size == 2
		results.listItems[0].hash == s2.hash
		results.listItems[1].hash == s1.hash
	}
	
	void "Test getStartedSprints max"() {
		given: "three new sprints"
		Sprint s1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		Sprint s2 = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
		Sprint s3 = Sprint.create(new Date(), user3, uniqueName, Visibility.PUBLIC)
		
		and: "user1 is reader of all sprints"
		s2.addReader(user1.id)
		s3.addReader(user1.id)
		
		when: "user starts all three sprints"
		startSprint(s1, user1.id)
		sleep(1000)
		startSprint(s2, user1.id)
		sleep(1000)
		startSprint(s3, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called with max"
		def results = searchService.getStartedSprints(user1.id, 0, 2)
		
		then: "only sprints s1 and s2 are returned"
		results.success
		results.listItems.size == 2
		results.listItems[0].hash == s3.hash
		results.listItems[1].hash == s2.hash		
	}
	
	void "Test getStartedSprints offset and max"() {
		given: "three new sprints"
		Sprint s1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		Sprint s2 = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
		Sprint s3 = Sprint.create(new Date(), user3, uniqueName, Visibility.PUBLIC)
		
		and: "user1 is reader of all sprints"
		s2.addReader(user1.id)
		s3.addReader(user1.id)
		
		when: "user starts all three sprints"
		startSprint(s1, user1.id)
		sleep(1000)
		startSprint(s2, user1.id)
		sleep(1000)
		startSprint(s3, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called with offset and max"
		def results = searchService.getStartedSprints(user1.id, 1, 1)
		
		then: "only sprints s1 and s2 are returned"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == s2.hash		
	}
	
	void "Test getStartedSprints offset out of range"() {
		given: "three new sprints"
		Sprint s1 = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		Sprint s2 = Sprint.create(new Date(), user2, uniqueName, Visibility.PUBLIC)
		Sprint s3 = Sprint.create(new Date(), user3, uniqueName, Visibility.PUBLIC)
		
		and: "user1 is reader of all sprints"
		s2.addReader(user1.id)
		s3.addReader(user1.id)
		
		when: "user starts all three sprints"
		startSprint(s1, user1.id)
		sleep(1000)
		startSprint(s2, user1.id)
		sleep(1000)
		startSprint(s3, user1.id)
		
		and: "elasticsearch service is indexed"
 		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		and: "getStartedSprints is called with offset"
		def results = searchService.getStartedSprints(user1.id, 5)
		
		then: "no sprints are returned"
		results.success
		results.listItems.size == 0
	}
}

package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.UserGroup
import us.wearecurio.model.Model.Visibility

import us.wearecurio.utility.Utils

class SearchDiscussionsIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//@spock.lang.IgnoreRest
	void "Test public discussion found"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new discussion"
		Discussion d = Discussion.create(user1, "$uniqueTerm $searchTerm", Visibility.PUBLIC)
		println ""
		println "user1.id: $user1.id"
		println "d.id: $d.userId"
		println ""
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user2, searchTerm)
		
		then: "single public discussion is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == d.hash
	}
	
	void "Test public single discussion found when followed and public"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a user group"
		def group = UserGroup.create(uniqueName, uniqueName, uniqueName, [isReadOnly:false, defaultNotify:false])
		group.addMember(user1)
		group.addMember(user2)
		Utils.save(group, true)

		and: "a new discussion"
		Discussion d = Discussion.create(user1, "$uniqueTerm $searchTerm", group, new Date(), Visibility.PUBLIC)
		println ""
		println "user1.id: $user1.id"
		println "d.userid: $d.userId"
		println "d.groupIds: $d.groupIds"
		println "d.name: $d.name"
		println "d.visibility: $d.visibility"
		println "group.id: $group.id"
		println ""
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user2, searchTerm)
		
		then: "single public discussion is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == d.hash
	}
	
	void "Test search for discussions"() {
	}
	
	void "Test offset for search for discussions"() {
	}
	
	void "Test max for search for discussions"() {
	}
	
	void "Test order for search for discussions"() {
	}
	
	void "Test recent discussion with name match ahead of recent discussion with first post match"(){
	}
	
	void "Test recent discussion with name match ahead of recent discussion with non-first post match"(){
	}
	
	void "Test recent discussion with first post match ahead of recent discussion with non-first post match"(){
	}

	void "Test recent discussion with name match ahead of old discussion with name match"(){
	}

	void "Test recent discussion with first post match ahead of old discussion with first post match"(){
	}
	
	void "Test recent discussion with non-first post match ahead of old discussion with non-first post match"(){
	}

	void "Test old discussion with name match behind recent discussion with first post match"(){
	}
	
	void "Test old discussion with name match behind recent discussion with non-first post match"(){
	}
	
	void "Test old discussion with first post match behind recent discussion with non-first post match"(){
	}
	
	void "Test followed discussions before public discussions"() {
	}
	
	void "Test owned discussions before public discussions"() {
	}
}

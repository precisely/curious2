package us.wearecurio.search.integration

import us.wearecurio.model.Discussion
import us.wearecurio.model.UserGroup
import us.wearecurio.model.Model.Visibility

import us.wearecurio.utility.Utils

class SearchDiscussionsIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//@spock.lang.IgnoreRest
	void "Test exact phrase found"() {
		given: "a search phrase"
		def searchPhrase = "check magnesium levels"
		
		and: "a new public discussion with searchPhrase in name"
		Discussion d = Discussion.create(user2, "$uniqueTerm $searchPhrase", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, /"$searchPhrase"/)
		
		then: "matched discussion is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == d.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test exact phrase not found even though words match"() {
		given: "a search phrase"
		def searchPhrase = "check magnesium levels"
		
		and: "a new public discussion with searchPhrase in name"
		Discussion d = Discussion.create(user2, "$uniqueTerm magnesium levels check", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, /"$searchPhrase"/)
		
		then: "matched discussion is found"
		results.success
		results.listItems.size == 0
	}
	
	//@spock.lang.IgnoreRest
	void "Test hash tag found even though words match"() {
		given: "a search phrase"
		def searchPhrase = "check magnesium levels"
		
		and: "a new public discussion with searchPhrase in name"
		Discussion d = Discussion.create(user2, "$uniqueTerm magnesium levels check", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, /"$searchPhrase"/)
		
		then: "matched discussion is found"
		results.success
		results.listItems.size == 0
	}
	
	//@spock.lang.IgnoreRest
	void "Test public discussion found"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public discussion"
		Discussion d = Discussion.create(user2, "$uniqueTerm $searchTerm", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "matched discussion is found"
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
		Discussion d = Discussion.create(user2, "$uniqueTerm $searchTerm", group, new Date(), Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, searchTerm)
		
		then: "single matched discussion is found"
		results.success
		results.listItems.size == 1
		results.listItems[0].hash == d.hash
	}
	
	//@spock.lang.IgnoreRest
	void "Test search for discussion with partial word"() {
		given: "a search term"
		def searchTerm = "magnesium"
		
		and: "a new public discussion"
		Discussion d = Discussion.create(user2, "$uniqueTerm $searchTerm", Visibility.PUBLIC)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		and: "search is performed"
		def results = searchService.search(user1, "mag")
		
		then: "matched discussion is found"
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

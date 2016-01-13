package us.wearecurio.search.integration

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.Tag
import us.wearecurio.services.SearchService
import us.wearecurio.utility.Utils

class GetOwnedSprintsIntegrationSpec extends SearchServiceIntegrationSpecBase {

	void "Test getOwned for sprints"() {
	}
	
	void "Test offset for getOwned for sprints"() {
	}
	
	void "Test max for getOwned for sprints"() {
	}
	
	void "Test order for getOwned for sprints"() {
	}

	//@spock.lang.IgnoreRest
	void "Test getOwned does not return matched unedited sprint with empty description"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)

		and: "an unedited sprint with empty description, but tag match in title"
		def unedited = Sprint.create(new Date(), user1, tagText, Visibility.PUBLIC)         
		unedited.description = ""
		Utils.save(unedited)

		when: "getOwned is called"
		def results = searchService.getOwned(SearchService.SPRINT_TYPE, user1)
		println "printing results..."
		print(results)

		then: "unedited sprint is not returned"
		results.success
		results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}

	//@spock.lang.IgnoreRest
	void "Test getOwned does not return matched unedited sprint with null description"() {
		given: "an interest tag for user1"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)

		and: "an unedited sprint with null description, but tag match in name"
		def unedited = Sprint.create(new Date(), user1, tagText, Visibility.PUBLIC)         
		unedited.description = null
		Utils.save(unedited)

		when: "getOwned is called"
		def results = searchService.getOwned(SearchService.SPRINT_TYPE, user1)
		println "printing results..."
		print(results)

		then: "unedited sprint is not returned"
		results.success
		results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}

	//@spock.lang.IgnoreRest
	void "Test getOwned does not return unedited sprint with empty description"() {
		given: "an untitled sprint with empty description"
		def unedited = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)         
		unedited.description = ""
		Utils.save(unedited)

		when: "getOwned is called"
		def results = searchService.getOwned(SearchService.SPRINT_TYPE, user1)

		println "printing results..."
		print(results)

		then: "unedited sprint is not returned"
		results.success
		results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}

	//@spock.lang.IgnoreRest
	void "Test getOwned does not return unedited sprint with null description"() {
		given: "an untitled sprint with null description"
		def unedited = Sprint.create(new Date(), user1, uniqueName, Visibility.PUBLIC)
		unedited.description = null
		Utils.save(unedited)
		
		when: "getOwned is called"
		def results = searchService.getOwned(SearchService.SPRINT_TYPE, user1)
		println "printing results..."
		print(results)

		then: "unedited sprint is not returned"
		results.success
		results.listItems.find{it.type == "spr" && it.hash == unedited.hash} == null
	}	
}

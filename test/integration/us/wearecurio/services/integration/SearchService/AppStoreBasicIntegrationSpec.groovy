package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.UserGroup
import us.wearecurio.model.Tag

import us.wearecurio.services.SearchService

import us.wearecurio.utility.Utils

class AppStoreBasicIntegrationSpec extends SearchServiceIntegrationSpecBase {

	//@spock.lang.Ignore
    void "Test getFeed for Users and Discussions"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "user2 with matching interest tags"
		def tag2 = Tag.create(tagText)
		user2.addInterestTag(tag2)
		Utils.save(user2, true)
		
		and: "discussion1 with matching interest tags"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)
		
		and: "discussion2 owned by user1 without matching interest tags"
		def discussion2 = Discussion.create(user3, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)

		and: "post made to discussion2"
		def post = discussion2.createPost(user3, getUniqueName())
		Utils.save(post, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getFeed returns 3 results"
		def results = searchService.getFeed(SearchService.DISCUSSION_TYPE | SearchService.USER_TYPE, user1)
		results.success
		results.listItems.size() == 4
		
		and: "results includes suggested user with matching tags (user2)"
		results.listItems.find{ it.type == "usr" && it.hash == user2.hash }

		and: "results includes suggested user without matching tags (user3)"
		results.listItems.find{ it.type == "usr" && it.hash == user3.hash }

		and: "results includes suggested discussion with matching tags (discussion1)"
		results.listItems.find{ it.type == "dis" && it.id == discussion1.id.toString() && it.name == discussion1.name }
		
		and: "results includes discussion with post (discussion2)"
		results.listItems.find{ it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name }
    }
	
	//@spock.lang.Ignore
    void "Test getFeed for Sprints"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)

		and: "sprint1 with matching interest tags"
		def sprint1 = Sprint.create(currentTime, user2, "${getUniqueName()} ${tagText}", Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		Utils.save(sprint1, true)

		and: "sprint2 without matching interest tags"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		sprint2.description = getUniqueName()
		Utils.save(sprint2, true)
		
		and: "discussion1 with matching interest tags not associated with sprint"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)

		and: "discussion2 without matching interest tags owned by sprint2"
		def discussion2 = Discussion.create(user2, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)
		sprint2.fetchUserGroup().addWriter(user2)
		sprint2.addDiscussion(discussion2)
		Utils.save(sprint2, true)
		
		and: "post made to discussion2"
		def post = discussion2.createPost(user3, getUniqueName())
		Utils.save(post, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getFeed returns 2 results"
		def results = searchService.getFeed(SearchService.SPRINT_TYPE, user1)
		results.success
		results.listItems.size() == 2
		
		and: "results includes suggested sprint (sprint1)"
		results.listItems.find{ it.type == "spr" && it.id == sprint1.id.toString() && it.name == sprint1.name }
		
		and: "results includes sprint with discussion with post (sprint2)"
		results.listItems.find{ it.type == "spr" && it.id == sprint2.id.toString() && it.name == sprint2.name }
    }
	
	//@spock.lang.Ignore
	void "Test getActivity for Discussion"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)

		and: "user2 with matching interest tags"
		def tag2 = Tag.create(tagText)
		user2.addInterestTag(tag2)
		Utils.save(user2, true)
		
		and: "discussion1 with matching interest tags"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)

		and: "discussion2 owned by user without matching interest tags"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)

		and: "post made to discussion2"
		def post = discussion2.createPost(user3, getUniqueName())
		Utils.save(post, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity2 returns 1 result"
		def results = searchService.getActivity(SearchService.DISCUSSION_TYPE, user1)
		results.success
		results.listItems.size() == 1
		
		and: "results includes discussion with post (discussion2)"
		results.listItems.find{ it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name }
	}

	//@spock.lang.Ignore
	void "Test getActivity for Sprint"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "sprint1 with matching interest tags"
		def sprint1 = Sprint.create(currentTime, user2, "${getUniqueName()} ${tagText}", Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		Utils.save(sprint1, true)
		
		and: "sprint2 without matching interest tags owned by user1"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		sprint2.description = getUniqueName()
		Utils.save(sprint2, true)
		
		and: "discussion1 with matching interest tags not associated with sprint"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)

		and: "discussion2 without matching interest tags owned by sprint2"
		def discussion2 = Discussion.create(user2, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)
		sprint2.fetchUserGroup().addWriter(user2)
		sprint2.addDiscussion(discussion2)
		Utils.save(sprint2, true)
		
		and: "post made to discussion2"
		def post = discussion2.createPost(user3, getUniqueName())

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getActivity returns 1 result"
		def results = searchService.getActivity(SearchService.SPRINT_TYPE, user1)
		results.success
		results.listItems.size() == 1
		
		and: "results includes sprint with discussion with post (sprint2)"
		results.listItems.find{ it.type == "spr" && it.id == sprint2.id.toString() && it.name == sprint2.name }
	}

	//@spock.lang.Ignore
	void "Test getSuggestions for Users"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "user2 with matching interest tags"
		def tag2 = Tag.create(tagText)
		user2.addInterestTag(tag2)
		Utils.save(user2, true)
						
		and: "discussion with matching interest tags"
		def discussion = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion.visibility = Visibility.PUBLIC
		Utils.save(discussion, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "getSuggestions returns all users except user1"
		def results = searchService.getSuggestions(SearchService.USER_TYPE, user1)
		results.success
		results.listItems.size() == 2
		
		and: "first result is user with matching tag (user2)"
		results.listItems[0].type == "usr"
		results.listItems[0].hash == user2.hash
		results.listItems[0].name == user2.name
		
		and: "second result is user without matching tag (user3)"
		results.listItems[1].type == "usr"
		results.listItems[1].hash == user3.hash
		results.listItems[1].name == user3.name
	}

	//@spock.lang.Ignore
	void "Test getOwned for Discussion"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)

		and: "user2 with matching interest tags"
		def tag2 = Tag.create(tagText)
		user2.addInterestTag(tag2)
		Utils.save(user2, true)
		
		and: "discussion1 with matching interest tags"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)

		and: "discussion2 without matching interest tags owned by user1"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)

		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getOwned returns 1 result"
		def results = searchService.getOwned(SearchService.DISCUSSION_TYPE, user1)
		results.success
		results.listItems.size() == 1
		
		and: "results includes discussion2"
		results.listItems.find{ it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name }
	}

	//@spock.lang.Ignore
	void "Test getOwned for Sprint"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "sprint1 with matching interest tags owned by user2"
		def sprint1 = Sprint.create(currentTime, user2, "${getUniqueName()} ${tagText}", Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		Utils.save(sprint1, true)
		
		and: "sprint2 without matching interest tags owned by user1"
		def sprint2 = Sprint.create(currentTime, user1, getUniqueName(), Visibility.PUBLIC)
		sprint2.description = getUniqueName()
		Utils.save(sprint2, true)
		
		and: "sprint3 without matching interest tags owned by user3"
		def sprint3 = Sprint.create(currentTime, user3, getUniqueName(), Visibility.PUBLIC)
		sprint3.description = getUniqueName()
		Utils.save(sprint3, true)
		
		and: "discussion1 with matching interest tags not associated with sprint"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)

		and: "discussion2 without matching interest tags associated with sprint2"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)
		sprint2.addDiscussion(discussion2)
		Utils.save(sprint2, true)
		
		and: "post made to discussion2"
		def post = discussion2.createPost(user3, getUniqueName())
		Utils.save(post, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getOwned returns 1 result"
		def results = searchService.getOwned(SearchService.SPRINT_TYPE, user3)
		results.success
		results.listItems.size() == 1
		
		and: "results includes owned sprint (sprint2)"
		results.listItems.find{ it.type == "spr" && it.id == sprint3.id.toString() && it.name == sprint3.name }
	}

	//@spock.lang.IgnoreRest
	void "Test search"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)
		
		and: "user2 with matching interest tags"
		def tag2 = Tag.create(tagText)
		user2.addInterestTag(tag2)
		Utils.save(user2, true)
		
		and: "discussion1 with matching interest tags"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)
		
		and: "discussion2 without matching interest tags"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)

		and: "post with matching tags made to discussion2"
		def post = discussion2.createPost(user3, "${getUniqueName()} ${tagText}")

		and: "discussion3 without interest tags owned by user1"
		def discussion3 = Discussion.create(user1, getUniqueName())
		discussion3.visibility = Visibility.PUBLIC
		Utils.save(discussion3, true)

		and: "sprint1 with matching interest tags owned by user2"
		def sprint1 = Sprint.create(currentTime, user2, "${getUniqueName()} ${tagText}", Visibility.PUBLIC)
		sprint1.description = getUniqueName()
		Utils.save(sprint1, true)
		
		and: "sprint2 without matching interest tags owned by user2"
		def sprint2 = Sprint.create(currentTime, user2, getUniqueName(), Visibility.PUBLIC)
		sprint2.description = getUniqueName()
		Utils.save(sprint2, true)
		
		and: "sprint3 without matching interest tags owned by user3"
		def sprint3 = Sprint.create(currentTime, user3, getUniqueName(), Visibility.PUBLIC)
		sprint3.description = getUniqueName()
		Utils.save(sprint3, true)
				
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "search returns 4 results"
		def results = searchService.search(user1, tagText)
		results.success
		results.listItems.size() == 4
		
		and: "results includes user2"
		results.listItems.find{ it.type == "usr" && it.hash == user2.hash }
		
		and: "results includes discussion1"
		results.listItems.find{ it.type == "dis" && it.hash == discussion1.hash }
		
		and: "results includes discussion2"
		results.listItems.find{ it.type == "dis" && it.hash == discussion2.hash }
		
		and: "results includes sprint1"
		results.listItems.find{ it.type == "spr" && it.hash == sprint1.hash }
	}

	//@spock.lang.Ignore
	void "Test getSprintDiscussions"() {
		given: "user1 with interest tag"
		def tagText = "MyInterestTag"
		def tag1 = Tag.create(tagText)
		user1.addInterestTag(tag1)
		Utils.save(user1, true)

		and: "discussion1 with matching interest tags"
		def discussion1 = Discussion.create(user3, "${getUniqueName()} ${tagText}")
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)

		and: "discussion2 without matching interest tags"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)

		and: "post with matching tags made to discussion2"
		def post = discussion2.createPost(user3, "${getUniqueName()} ${tagText}")
		Utils.save(post, true)

		and: "discussion3 without interest tags"
		def discussion3 = Discussion.create(user1, getUniqueName())
		discussion3.visibility = Visibility.PUBLIC
		Utils.save(discussion3, true)

		and: "sprint"
		def sprint = Sprint.create(currentTime, user3, getUniqueName(), Visibility.PUBLIC)
		sprint.description = getUniqueName()
		Utils.save(sprint, true)

		and: "discussion2 associated with sprint"
		sprint.fetchUserGroup().addWriter(user1)
		sprint.addDiscussion(discussion2)
		Utils.save(sprint, true)
		
		and: "discussion3 associated with sprint"
		sprint.addDiscussion(discussion3)
		Utils.save(sprint, true)
		
		when: "elasticsearch service is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")

		then: "getSprintDiscussions returns 2 results"
		def results = searchService.getSprintDiscussions(sprint, user1)
		results.success
		results.listItems.size() == 2
		
		and: "results includes discussion2"
		results.listItems.find{ it.type == "dis" && it.id == discussion2.id.toString() && it.name == discussion2.name }
		
		and: "results includes discussion3"
		results.listItems.find{ it.type == "dis" && it.id == discussion3.id.toString() && it.name == discussion3.name }
	}
}

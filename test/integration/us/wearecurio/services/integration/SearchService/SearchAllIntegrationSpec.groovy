package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Discussion
import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.Sprint
import us.wearecurio.model.User

import us.wearecurio.utility.Utils

class SearchAllIntegrationSpec extends SearchServiceIntegrationSpecBase {

    void "Test public discussions and public sprint returned"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = Visibility.PUBLIC
		Utils.save(discussion1, true)
		
		and: "another discussion without search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName())
		discussion2.visibility = Visibility.PUBLIC
		Utils.save(discussion2, true)
		
		and: "a new user with search term in name"
		def userA = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "user3 adds a post with search term in message to discussion2"
		def post = discussion2.createPost(user2, getUniqueName() + " " + term)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchAll returns sprint and dicussions"
		def results = searchService.searchAll(user3, term)
		results.success
		results.listItems.size() == 4
		results.listItems.find{ 
			it.type == "spr" &&
			it.id == sprint.id.toString() &&
			it.name == sprint.name
		} != null
		results.listItems.find{
			it.type == "dis" &&
			it.id == discussion1.id.toString() &&
			it.name == discussion1.name
		} != null
		results.listItems.find{
			it.type == "dis" &&
			it.id == discussion2.id.toString() &&
			it.name == discussion2.name
		} != null
		results.listItems.find{
			it.type == "usr" &&
			it.id == userA.id.toString() &&
			it.name == userA.name
		} != null
    }	
	
    void "Test private discussion and private sprint not returned"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new sprint with search term in name"
		def sprint = Sprint.create(currentTime, user1, getUniqueName() + " " + term, Visibility.PRIVATE)
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = Visibility.PRIVATE
		Utils.save(discussion1, true)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchAll returns no results"
		def results = searchService.searchAll(user2, term)
		results.success
		results.listItems.size() == 0
    }	
	
	void "Test max with searchAll"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new user"
		def userA = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)
		userA.created = curTime
		Utils.save(userA, true)
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = Visibility.PUBLIC
		discussion1.created = curTime - 1
		Utils.save(discussion1, true)
		
		and: "a new sprint with search term in name"
		def sprint1 = Sprint.create(curTime - 2, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "another user"
		def userB = User.create(
			[	username:'ernest',
				sex:'F',
				name:'Ernest Hemingway' + " " + term,
				email:'ernest@hemingway.com',
				birthdate:'01/01/1960',
				password:'ernestxyz',
				action:'doregister',
				controller:'home'	]
		)
		userB.created = curTime - 3
		Utils.save(userB, true)
		
		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion2.visibility = Visibility.PUBLIC
		discussion2.created = curTime - 4
		Utils.save(discussion2, true)
		
		and: "another sprint with search term in name"
		def sprint2 = Sprint.create(curTime - 5, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns sprint"
		def results = searchService.searchAll(user1, term, 0, 3)
		results.success
		results.listItems.size() == 3
		results.listItems[0].type == "usr"
		results.listItems[0].id == userA.id.toString()
		results.listItems[0].name == userA.name
		results.listItems[1].type == "dis"
		results.listItems[1].id == discussion1.id.toString()
		results.listItems[1].name == discussion1.name
		results.listItems[2].type == "spr"
		results.listItems[2].id == sprint1.id.toString()
		results.listItems[2].name == sprint1.name
	}
	
	void "Test max with multiple posts for discussion"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new user"
		def userA = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)
		userA.created = curTime - 2
		Utils.save(userA, true)
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = Visibility.PUBLIC
		discussion1.created = curTime - 3
		Utils.save(discussion1, true)
		
		and: "a new sprint with search term in name"
		def sprint1 = Sprint.create(curTime - 4, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "another user"
		def userB = User.create(
			[	username:'ernest',
				sex:'F',
				name:'Ernest Hemingway' + " " + term,
				email:'ernest@hemingway.com',
				birthdate:'01/01/1960',
				password:'ernestxyz',
				action:'doregister',
				controller:'home'	]
		)
		userB.created = curTime - 5
		Utils.save(userB, true)
		
		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion2.visibility = Visibility.PUBLIC
		discussion2.created = curTime - 6
		Utils.save(discussion2, true)
		
		and: "another sprint with search term in name"
		def sprint2 = Sprint.create(curTime - 7, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		when: "user1 adds a post with search term in message to discussion1"
		def post11 = discussion1.createPost(user1, getUniqueName() + " " + term)
		post11.created = curTime 
		Utils.save(post11, true)
		
		and: "user1 adds another post with search term in message to discussion1"
		def post12 = discussion1.createPost(user1, getUniqueName() + " " + term)
		post12.created = curTime 
		Utils.save(post12, true)
		
		and: "user1 adds a post with search term in message to discussion2"
		def post21 = discussion2.createPost(user1, getUniqueName() + " " + term)
		post21.created = curTime - 2
		Utils.save(post21, true)
		
		and: "user1 adds another post with search term in message to discussion2"
		def post22 = discussion2.createPost(user1, getUniqueName() + " " + term)
		post22.created = curTime - 2			
		Utils.save(post22, true)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns sprint"
		def results = searchService.searchAll(user1, term, 0, 3)
		results.success
		results.listItems.size() == 3
		results.listItems[0].type == "usr"
		results.listItems[0].id == userA.id.toString()
		results.listItems[0].name == userA.name
		results.listItems[1].type == "dis"
		results.listItems[1].id == discussion1.id.toString()
		results.listItems[1].name == discussion1.name
		results.listItems[2].type == "spr"
		results.listItems[2].id == sprint1.id.toString()
		results.listItems[2].name == sprint1.name
	}

	void "Test offset with searchAll"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new user with search term in name"
		def userA = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)
		userA.created = curTime
		Utils.save(userA, true)
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = Visibility.PUBLIC
		discussion1.created = curTime - 1
		Utils.save(discussion1, true)
		
		and: "a new sprint with search term in name"
		def sprint1 = Sprint.create(curTime - 2, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "another user"
		def userB = User.create(
			[	username:'ernest',
				sex:'F',
				name:'Ernest Hemingway' + " " + term,
				email:'ernest@hemingway.com',
				birthdate:'01/01/1960',
				password:'ernestxyz',
				action:'doregister',
				controller:'home'	]
		)
		userB.created = curTime - 3
		Utils.save(userB, true)
		
		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion2.visibility = Visibility.PUBLIC
		discussion2.created = curTime - 4
		Utils.save(discussion2, true)
		
		and: "another sprint with search term in name"
		def sprint2 = Sprint.create(curTime - 5, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "and another user"
		def userC = User.create(
			[	username:'joan',
				sex:'F',
				name:'Joan Didion' + " " + term,
				email:'joan@didion.com',
				birthdate:'01/01/1960',
				password:'joanxyz',
				action:'doregister',
				controller:'home'	]
		)
		userC.created = curTime - 6
		Utils.save(userC, true)
		
		and: "another discussion with search term in name"
		def discussion3 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion3.visibility = Visibility.PUBLIC
		discussion3.created = curTime - 7
		Utils.save(discussion3, true)
		
		and: "another sprint with search term in name"
		def sprint3 = Sprint.create(curTime - 8, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns first user for offset 0"
		def results1 = searchService.searchAll(user1, term, 0, 3)
		results1.success
		results1.listItems.size() == 3
		results1.listItems[0].type == "usr"
		results1.listItems[0].id == userA.id.toString()
		results1.listItems[0].name == userA.name
		results1.listItems[1].type == "dis"
		results1.listItems[1].id == discussion1.id.toString()
		results1.listItems[1].name == discussion1.name
		results1.listItems[2].type == "spr"
		results1.listItems[2].id == sprint1.id.toString()
		results1.listItems[2].name == sprint1.name

		and: "searchUsers returns second user for offset 1"
		def results2 = searchService.searchAll(user1, term, 3, 3)
		results2.success
		results2.listItems.size() == 3
		results2.listItems[0].type == "usr"
		results2.listItems[0].id == userB.id.toString()
		results2.listItems[0].name == userB.name
		results2.listItems[1].type == "dis"
		results2.listItems[1].id == discussion2.id.toString()
		results2.listItems[1].name == discussion2.name
		results2.listItems[2].type == "spr"
		results2.listItems[2].id == sprint2.id.toString()
		results2.listItems[2].name == sprint2.name

		and: "searchUsers returns third user for offset 2"
		def results3 = searchService.searchAll(user1, term, 6, 3)
		results3.success
		results3.listItems.size() == 3
		results3.listItems[0].type == "usr"
		results3.listItems[0].id == userC.id.toString()
		results3.listItems[0].name == userC.name
		results3.listItems[1].type == "dis"
		results3.listItems[1].id == discussion3.id.toString()
		results3.listItems[1].name == discussion3.name
		results3.listItems[2].type == "spr"
		results3.listItems[2].id == sprint3.id.toString()
		results3.listItems[2].name == sprint3.name
	}

	void "Test offset with multiple posts for dicussions"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "the current time"
		def curTime = new Date()
		
		and: "a new user with search term in name"
		def userA = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)
		userA.created = curTime
		Utils.save(userA, true)
		
		and: "a new discussion with search term in name"
		def discussion1 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion1.visibility = Visibility.PUBLIC
		discussion1.created = curTime - 2
		Utils.save(discussion1, true)
		
		and: "a new sprint with search term in name"
		def sprint1 = Sprint.create(curTime - 3, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "another user with search term in name"
		def userB = User.create(
			[	username:'ernest',
				sex:'F',
				name:'Ernest Hemingway' + " " + term,
				email:'ernest@hemingway.com',
				birthdate:'01/01/1960',
				password:'ernestxyz',
				action:'doregister',
				controller:'home'	]
		)
		userB.created = curTime - 4
		Utils.save(userB, true)

		and: "another discussion with search term in name"
		def discussion2 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion2.visibility = Visibility.PUBLIC
		discussion2.created = curTime - 6
		Utils.save(discussion2, true)
		
		and: "another sprint with search term in name"
		def sprint2 = Sprint.create(curTime - 7, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		and: "and another user with search term in name"
		def userC = User.create(
			[	username:'joan',
				sex:'F',
				name:'Joan Didion' + " " + term,
				email:'joan@didion.com',
				birthdate:'01/01/1960',
				password:'joanxyz',
				action:'doregister',
				controller:'home'	]
		)
		userC.created = curTime - 8
		Utils.save(userC, true)
		
		and: "another discussion with search term in name"
		def discussion3 = Discussion.create(user1, getUniqueName() + " " + term)
		discussion3.visibility = Visibility.PUBLIC
		discussion3.created = curTime - 10
		Utils.save(discussion3, true)
		
		and: "another sprint with search term in name"
		def sprint3 = Sprint.create(curTime - 11, user1, getUniqueName() + " " + term, Visibility.PUBLIC)
		
		when: "user1 adds a post with search term in message to discussion1"
		def post11 = discussion1.createPost(user1, getUniqueName() + " " + term)
		post11.created = curTime - 1
		Utils.save(post11, true)
		
		and: "user1 adds another post with search term in message to discussion1"
		def post12 = discussion1.createPost(user1, getUniqueName() + " " + term)
		post12.created = curTime - 1
		Utils.save(post12, true)
		
		and: "user1 adds a post with search term in message to discussion2"
		def post21 = discussion2.createPost(user1, getUniqueName() + " " + term)
		post21.created = curTime - 5
		Utils.save(post21, true)
		
		and: "user1 adds another post with search term in message to discussion2"
		def post22 = discussion2.createPost(user1, getUniqueName() + " " + term)
		post22.created = curTime - 5
		Utils.save(post22, true)
		
		and: "user1 adds a post with search term in message to discussion3"
		def post31 = discussion3.createPost(user1, getUniqueName() + " " + term)
		post31.created = curTime - 9
		Utils.save(post31, true)
		
		and: "user1 adds another post with search term in message to discussion3"
		def post32 = discussion3.createPost(user1, getUniqueName() + " " + term)
		post32.created = curTime - 9
		Utils.save(post32, true)
		
		and: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		println ""
		println "discussion1: " + discussion1
		println ""
		println "discussion2: " + discussion2
		println ""
		println "discussion3: " + discussion3
		println ""
		println "past11: " + post11
		println ""
		println "post12: " + post12
		println ""
		println "past21: " + post21
		println ""
		println "post22: " + post22
		println ""
		println "past31: " + post31
		println ""
		println "post32: " + post32

		then: "searchUsers returns first user for offset 0"
		def results1 = searchService.searchAll(user1, term, 0, 3)
		results1.success
		results1.listItems.size() == 3
		results1.listItems[0].type == "usr"
		results1.listItems[0].id == userA.id.toString()
		results1.listItems[0].name == userA.name
		results1.listItems[1].type == "dis"
		results1.listItems[1].id == discussion1.id.toString()
		results1.listItems[1].name == discussion1.name
		results1.listItems[2].type == "spr"
		results1.listItems[2].id == sprint1.id.toString()
		results1.listItems[2].name == sprint1.name

		and: "searchUsers returns second user for offset 1"
		def results2 = searchService.searchAll(user1, term, 3, 3)
		results2.success
		results2.listItems.size() == 3
		results2.listItems[0].type == "usr"
		results2.listItems[0].id == userB.id.toString()
		results2.listItems[0].name == userB.name
		results2.listItems[1].type == "dis"
		results2.listItems[1].id == discussion2.id.toString()
		results2.listItems[1].name == discussion2.name
		results2.listItems[2].type == "spr"
		results2.listItems[2].id == sprint2.id.toString()
		results2.listItems[2].name == sprint2.name

		and: "searchUsers returns third user for offset 2"
		def results3 = searchService.searchAll(user1, term, 6, 3)
		results3.success
		results3.listItems.size() == 3
		results3.listItems[0].type == "usr"
		results3.listItems[0].id == userC.id.toString()
		results3.listItems[0].name == userC.name
		results3.listItems[1].type == "dis"
		results3.listItems[1].id == discussion3.id.toString()
		results3.listItems[1].name == discussion3.name
		results3.listItems[2].type == "spr"
		results3.listItems[2].id == sprint3.id.toString()
		results3.listItems[2].name == sprint3.name
	}	
}

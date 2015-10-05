package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import us.wearecurio.model.Model.Visibility
import us.wearecurio.model.User

import us.wearecurio.utility.Utils

class SearchUsersIntegrationSpec extends SearchServiceIntegrationSpecBase {

	void "Test user returned when search term in name"() {
		given: "a search term"
		def term = getUniqueTerm()
		
		and: "a new user"
		def user = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns sprint"
		def results = searchService.searchUsers(user1, term)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "usr"
		results.listItems[0].id == user.id.toString()
		results.listItems[0].name == user.name
	}
	
	void "Test user returned when all search terms in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new user with both terms matching name"
		def user = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term1 + " " + term2,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns sprint"
		def results = searchService.searchUsers(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "usr"
		results.listItems[0].id == user.id.toString()
		results.listItems[0].name == user.name
	}
	
	void "Test user not returned when only first search term in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new user with only second term matching name"
		def user = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term1,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns sprint"
		def results = searchService.searchUsers(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}
	
	void "Test user not returned when only second search term in name"() {
		given: "search terms"
		def term1 = getUniqueTerm()
		def term2 = getUniqueTerm()
		
		and: "a new user with only second term matching name"
		def user = User.create(
			[	username:'roberto',
				sex:'F',
				name:'Roberto Bolano' + " " + term2,
				email:'raberto@bolano.com',
				birthdate:'01/01/1960',
				password:'robertoxyz',
				action:'doregister',
				controller:'home'	]
		)

		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns sprint"
		def results = searchService.searchUsers(user1, term1 + " " + term2)
		results.success
		results.listItems.size() == 0
	}
	
	void "Test max with searchUsers"() {
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
		userB.created = curTime - 1
		Utils.save(userB, true)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns sprint"
		def results = searchService.searchUsers(user1, term, 0, 1)
		results.success
		results.listItems.size() == 1
		results.listItems[0].type == "usr"
		results.listItems[0].id == userA.id.toString()
		results.listItems[0].name == userA.name
	}
	
	void "Test offset with searchUsers"() {
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
		userB.created = curTime - 1
		Utils.save(userB, true)
		
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
		userC.created = curTime - 2
		Utils.save(userC, true)
		
		when: "elasticsearch is indexed"
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		then: "searchUsers returns first user for offset 0"
		def results1 = searchService.searchUsers(user1, term, 0)
		results1.success
		results1.listItems.size() == 3
		results1.listItems[0].type == "usr"
		results1.listItems[0].id == userA.id.toString()
		results1.listItems[0].name == userA.name
		
		and: "searchUsers returns second user for offset 1"
		def results2 = searchService.searchUsers(user1, term, 1)
		results2.success
		results2.listItems.size() == 2
		results2.listItems[0].type == "usr"
		results2.listItems[0].id == userB.id.toString()
		results2.listItems[0].name == userB.name

		and: "searchUsers returns third user for offset 2"
		def results3 = searchService.searchUsers(user1, term, 2)
		results3.success
		results3.listItems.size() == 1
		results3.listItems[0].type == "usr"
		results3.listItems[0].id == userC.id.toString()
		results3.listItems[0].name == userC.name
	}
}

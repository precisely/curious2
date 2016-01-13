package us.wearecurio.search.integration

import grails.test.spock.IntegrationSpec
import groovy.lang.Closure;

import java.text.DateFormat
import java.util.concurrent.atomic.AtomicInteger

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery

import org.grails.plugins.elasticsearch.ElasticSearchAdminService;
import org.grails.plugins.elasticsearch.ElasticSearchService

import spock.lang.*

import us.wearecurio.model.*
import us.wearecurio.utility.Utils

public class SearchServiceIntegrationSpecBase extends IntegrationSpec {
	Date currentTime
	UserGroup testGroup
	
	ElasticSearchService elasticSearchService
	def searchService
	ElasticSearchAdminService elasticSearchAdminService
	def elasticSearchHelper

	User user1
	User user2
	User user3
	
	static AtomicInteger nameCount = new AtomicInteger(0)
	static String getUniqueName() {
		return "SearchServiceIntegrationSpecBasename" + nameCount.getAndIncrement()
	}
	
	static String getUniqueTerm() {
		return "searchTerm" + nameCount.getAndIncrement()
	}
	
	def setup() {
		Locale.setDefault(Locale.US)	// For to run test case in any country.
		Utils.resetForTesting()
        
		searchService
		searchService.elasticSearchService = elasticSearchService

		elasticSearchHelper.withElasticSearch{ client ->
			client.prepareDeleteByQuery("us.wearecurio.model_v0").setQuery(matchAllQuery()).execute().actionGet()
			client.admin().indices().prepareRefresh().execute().actionGet()
		}
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		user1 = User.create(
			[	username:'shane',
				sex:'F',
				name:'shane macgowen',
				email:'shane@pogues.com',
				birthdate:'01/01/1960',
				password:'shanexyz',
				action:'doregister',
				controller:'home'	]
		)
		user1.settings.makeNamePublic()
		user1.settings.makeBioPublic()
		Utils.save(user1, true)
		user2 = User.create(
			[	username:'spider',
				sex:'F',
				name:'spider stacy',
				email:'spider@pogues.com',
				birthdate:'01/01/1961',
				password:'spiderxyz',
				action:'doregister',
				controller:'home'	]
		)
		user2.settings.makeNamePublic()
		user2.settings.makeBioPublic()
		Utils.save(user2, true)
		user3 = User.create(
			[	username:'jem',
				sex:'F',
				name:'jem finer',
				email:'jem@pogues.com',
				birthdate:'01/01/1963',
				password:'jemxyz',
				action:'doregister',
				controller:'home'	]
		)
		user3.settings.makeNamePublic()
		user3.settings.makeBioPublic()
		Utils.save(user3, true)

		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		def dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		
		testGroup = UserGroup.create("group", "Curious Group", "Discussion topics for test users",
				[isReadOnly:false, defaultNotify:false])
		testGroup.addWriter(user1)
		
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
	}

	def cleanup() {
        for (GroupMemberReader o in GroupMemberReader.list()) {
            o.delete(flush:true)
        }
        for (GroupMemberWriter o in GroupMemberWriter.list()) {
            o.delete(flush:true)
        }
        for (GroupMemberAdmin o in GroupMemberAdmin.list()) {
            o.delete(flush:true)
        }
        for (GroupMemberDiscussion o in GroupMemberDiscussion.list()) {
            o.delete(flush:true)
        }
        for (UserGroup o in UserGroup.list()) {
            o.delete(flush:true)
        }
        for (DiscussionPost o in DiscussionPost.list()) {
            o.delete(flush:true)
        }
        for (Discussion o in Discussion.list()) {
            o.delete(flush:true)
        }
        for (Sprint o in Sprint.list()) {
            o.delete(flush:true)
        }
        for (User o in User.list()) {
            o.delete(flush:true)
        }
        for (Entry o in Entry.list()) {
            o.delete(flush:true)
        }
        for (Tag o in Tag.list()) {
            o.delete(flush:true)
        }
		elasticSearchService.index()
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
	}

	void printAllUserActivities() {
		println ""
		println "UserActivities:"
		us.wearecurio.model.UserActivity.getAll().each {
			println "activity: " + it
			println "activityType: " + it.activityType
			println "objectType: " + it.objectType
			println "typeString: " + it.typeString
			println "otherType: " + it.otherType
			println "objectDescription: " + it.objectDescription
			println "otherDescription: " + it.otherDescription
			println "discussionGroupIds: " + it.discussionGroupIds
			println "objectUserId: " + it.objectUserId
			println "objectVisibility: " + it.objectVisibility
			println "sprintDescription: " + it.sprintDescription
			println "sprintVirtualGroupId: " + it.sprintVirtualGroupId
			println "sprintTagName: " + it.sprintTagName
			println ""
		}
		println ""
	}	

	
}

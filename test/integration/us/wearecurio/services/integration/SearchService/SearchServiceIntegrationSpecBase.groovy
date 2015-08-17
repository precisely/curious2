package us.wearecurio.services.integration.SearchService

import grails.test.spock.IntegrationSpec

import java.text.DateFormat
import java.util.concurrent.atomic.AtomicInteger

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery

import org.grails.plugins.elasticsearch.ElasticSearchAdminService;
import org.grails.plugins.elasticsearch.ElasticSearchService

import us.wearecurio.model.GroupMemberReader;
import us.wearecurio.model.User;
import us.wearecurio.model.UserGroup
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
		return "SearchServiceIntegrationSpecBase name" + nameCount.getAndIncrement()
	}

	def setup() {
		searchService
		searchService.elasticSearchService = elasticSearchService

		elasticSearchHelper.withElasticSearch{ client ->
			client.prepareDeleteByQuery("us.wearecurio.model_v0").setQuery(matchAllQuery()).execute().actionGet()
			client.admin().indices().prepareRefresh().execute().actionGet()
		}
		
		elasticSearchAdminService.refresh("us.wearecurio.model_v0")
		
		GroupMemberReader.executeUpdate("delete GroupMemberReader r")
		UserGroup.executeUpdate("delete UserGroup g")
		
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

		def entryTimeZone = Utils.createTimeZone(-8 * 60 * 60, "GMTOFFSET8", true)
		def dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		dateFormat.setTimeZone(entryTimeZone)
		currentTime = dateFormat.parse("July 1, 2010 3:30 pm")
		
		testGroup = UserGroup.create("group", "Curious Group", "Discussion topics for test users",
				[isReadOnly:false, defaultNotify:false])
		testGroup.addWriter(user1)
		
		elasticSearchService.index()
	}

	def cleanup() {
	}

}

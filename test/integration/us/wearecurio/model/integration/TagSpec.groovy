package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec
import us.wearecurio.model.Tag
import us.wearecurio.model.TagStats
import us.wearecurio.model.User

class TagSpec extends IntegrationSpec {

	Tag tagInstance1
	Tag tagInstance2
	Tag tagInstance3

	User userInstance

	void setup() {
		tagInstance1 = Tag.look('sleep')
		tagInstance2 = Tag.look('energy')
		tagInstance3 = Tag.look('tea')

		userInstance = User.create([username :'shaneMac1', sex:'F', name:'shane macgowen', email:'sha@pogues.com',
				birthdate:'01/01/1960', password:'shanexyz', action:'doregister', controller:'home'])
	}

	void "test getRecentlyUsedTags method"() {
		given: 'TagStats for 2 tags so as to have two recent TagInputTypes'
		// Clearing old instances if any.
		TagStats.findAllByUserId(userInstance.id)*.delete(flush: true)

		TagStats tagStats1 = TagStats.createOrUpdate(userInstance.id, tagInstance1.id)
		tagStats1.mostRecentUsage = new Date() - 16
		tagStats1.save(flush: true)

		TagStats tagStats2 = TagStats.createOrUpdate(userInstance.id, tagInstance2.id)
		tagStats2.mostRecentUsage = new Date() - 18
		tagStats2.save(flush: true)

		TagStats tagStats3 = TagStats.createOrUpdate(userInstance.id, tagInstance3.id)
		tagStats3.mostRecentUsage = new Date() - 30
		tagStats3.save(flush: true)

		when: 'The getRecentlyUsedTags() method is called and startDate is not passed'
		List resultList = Tag.getRecentlyUsedTags(userInstance.id) // Default startDate is set to two weeks from today.

		then: 'The method returns empty list in response since there are no Tags being used in last two weeks'
		resultList == []

		when: 'The getRecentlyUsedTags() method is called and startDate is set'
		resultList = Tag.getRecentlyUsedTags(userInstance.id, new Date() - 21)

		then: 'All the Tag used within last 21 days are returned'
		resultList.size() == 2
		resultList[0].tagId == tagInstance2.id
		resultList[0].description == tagInstance2.description  // sorted by description
		resultList[1].tagId == tagInstance1.id
		resultList[1].description == tagInstance1.description
	}
}
package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec
import us.wearecurio.model.InputType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagInputType
import us.wearecurio.model.TagStats
import us.wearecurio.model.User

class TagInputTypeSpec extends IntegrationSpec {

	Tag tagInstance1
	Tag tagInstance2

	User userInstance

	void setup() {
		tagInstance1 = Tag.look('sleep')
		tagInstance2 = Tag.look('energy')

		// Clearing old instances if any.
		TagInputType.list()*.delete(flush: true)

		[tagInstance1.id, tagInstance2.id].each {
			new TagInputType(tagId: it, max: 10, min: 0, noOfLevels: 5,
					inputType: InputType.LEVEL, defaultUnit: 'hours').save(flush: true)
		}

		// Creating 8 default TagInputTypes
		['coffee', 'bowell movement', 'apple', 'banana', 'beer', 'dance', 'tea', 'milk'].each {
			new TagInputType(tagId: Tag.look(it).id , max: 10, min: 0, noOfLevels: 5,
					inputType: InputType.LEVEL, defaultUnit: 'hours', isDefault: true).save(flush: true)
		}

		userInstance = User.create([username :'shaneMac1', sex:'F', name:'shane macgowen', email:'sha@pogues.com',
				birthdate:'01/01/1960', password:'shanexyz', action:'doregister', controller:'home'])
	}

	void "test getAllTagInputTypes to return all TagInputTypes for valid tags"() {
		when: 'The getAllTagInputTypes method is called'
		List tagsWithInputType = TagInputType.getAllTagInputTypes()

		then: 'The result should contain all 10 instances of TagInputTypes'
		assert tagsWithInputType.size() == 10

		// 8 default TagInputTypes
		assert tagsWithInputType[0].description == 'apple'
		assert tagsWithInputType[1].description == 'banana'
		assert tagsWithInputType[2].description == 'beer'
		assert tagsWithInputType[3].description == 'bowell movement'
		assert tagsWithInputType[4].description == 'coffee'
		assert tagsWithInputType[5].description == 'dance'
		assert tagsWithInputType[6].description == 'energy'
		assert tagsWithInputType[7].description == 'milk'
		assert tagsWithInputType[8].description == 'sleep'
		assert tagsWithInputType[9].description == 'tea'
	}

	void "test getRecentTagsWithInputType method to return at least 8 TagInputTypes"() {
		given: 'TagStats for 2 tags so as to have two recent TagInputTypes'
		// Clearing old instances if any.
		TagStats.findAllByUserId(userInstance.id)*.delete(flush: true)

		TagStats tagStats1 = TagStats.createOrUpdate(userInstance.id, tagInstance1.id)
		tagStats1.mostRecentUsage = new Date() - 5
		tagStats1.save(flush: true)

		TagStats tagStats2 = TagStats.createOrUpdate(userInstance.id, tagInstance2.id)
		tagStats2.mostRecentUsage = new Date() - 7
		tagStats2.save(flush: true)

		when: 'The getRecentTagsWithInputType method is called for the User'
		List tagsWithInputType = TagInputType.getRecentTagsWithInputType(userInstance.id)

		then: 'The result should contain 8 TagInputTypes out of 10'
		assert tagsWithInputType.size() == 8

		// 6 default TagInputTypes
		assert tagsWithInputType[0].description == 'apple'
		assert tagsWithInputType[1].description == 'banana'
		assert tagsWithInputType[2].description == 'beer'
		assert tagsWithInputType[3].description == 'bowell movement'
		assert tagsWithInputType[4].description == 'coffee'
		assert tagsWithInputType[5].description == 'dance'

		// 2 Recent TagInputTypes
		assert tagsWithInputType[6].description == 'energy'
		assert tagsWithInputType[7].description == 'sleep'

		when: 'There are no tags used in the past 14 days'
		TagStats.findAllByUserId(userInstance.id)*.delete(flush: true)
		tagsWithInputType = TagInputType.getRecentTagsWithInputType(userInstance.id)

		then: 'There should still be 8 default TagInputTypes present in the result'
		assert tagsWithInputType.size() == 8

		// 8 default TagInputTypes
		assert tagsWithInputType[0].description == 'apple'
		assert tagsWithInputType[1].description == 'banana'
		assert tagsWithInputType[2].description == 'beer'
		assert tagsWithInputType[3].description == 'bowell movement'
		assert tagsWithInputType[4].description == 'coffee'
		assert tagsWithInputType[5].description == 'dance'
		assert tagsWithInputType[6].description == 'milk'
		assert tagsWithInputType[7].description == 'tea'
	}
}

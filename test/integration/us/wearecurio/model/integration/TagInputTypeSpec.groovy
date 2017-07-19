package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec
import us.wearecurio.model.InputType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagInputType
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
					inputType: InputType.LEVEL, defaultUnit: 'hours').save(flush: true)
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
}

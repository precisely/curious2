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

		userInstance = User.create([username :'shaneMac1', sex:'F', name:'shane macgowen', email:'sha@pogues.com',
				birthdate:'01/01/1960', password:'shanexyz', action:'doregister', controller:'home'])
	}

	void "test various overloaded methods from TagInputType class"() {
		given: 'A few instances of Tag, TagStats and TagInputType'
		new TagStats([userId: userInstance.id, tagId: tagInstance1.id, description: 'sleep', countLastThreeMonths: 80L,
				mostRecentUsage: new Date() - 2, countLastYear: 302L, countAllTime: 1024L]).save(flush: true)
		TagStats.createOrUpdate(userInstance.id, tagInstance2.id)

		new TagInputType(tagId: tagInstance1.id, max: 10, min: 0, noOfLevels: 5, userId: userInstance.id,
				inputType: InputType.THUMBS, defaultUnit: 'miles').save(flush: true)
		new TagInputType(tagId: tagInstance2.id, max: 10, min: 0, noOfLevels: 5, userId: userInstance.id,
				inputType: InputType.LEVEL, defaultUnit: 'hours').save(flush: true)

		when: 'getAllTagsWithInputType method is called and no data is present in cache'
		assert TagInputType.cachedTagInputTypes.size() == 0
		Map resultMap = TagInputType.getAllTagsWithInputType(null)

		then: 'Method returns all the new instances of TagInputType and cache gets updated'
		resultMap.size() == 2
		resultMap.tagsWithInputTypeList[0].tagId == tagInstance2.id
		resultMap.tagsWithInputTypeList[0].inputType == InputType.LEVEL
		resultMap.tagsWithInputTypeList[1].tagId == tagInstance1.id
		resultMap.tagsWithInputTypeList[1].inputType == InputType.THUMBS
		TagInputType.cachedTagInputTypes.size() == 2

		when: 'getAllTagsWithInputType method is called and cache has not been updated after clientCacheDate'
		resultMap = TagInputType.getAllTagsWithInputType(new Date())

		then: 'Method returns empty map in response'
		resultMap == [:]

		when: 'getTagsWithInputTypeForDateRange method is called and a date range is passed'
		List resultList = TagInputType.getTagsWithInputTypeForDateRange(new Date() - 20, new Date())

		then: 'Method should return only those tags whose mostRecentUsage lies within this range'
		resultList.size() == 1
		resultList[0].tagId == tagInstance1.id
		resultList[0].inputType == InputType.THUMBS
		resultList[0].max == 10
		resultList[0].min == 0
		resultList[0].description == 'sleep'
		resultList[0].noOfLevels == 5

		when: 'getAllTagsWithInputTypeForUser method is called and no results are found for given userId'
		resultList = TagInputType.getAllTagsWithInputTypeForUser(2093L)

		then: 'Method returns empty list in response'
		resultList == []

		when: 'getAllTagsWithInputTypeForUser method is called and no results are found for given userId'
		resultList = TagInputType.getAllTagsWithInputTypeForUser(userInstance.id)

		then: 'Method returns a list of all the matching instances'
		resultList.size() == 2
		resultList[0].tagId == tagInstance2.id
		resultList[0].inputType == InputType.LEVEL
		resultList[1].tagId == tagInstance1.id
		resultList[1].inputType == InputType.THUMBS
		TagInputType.clearCache()
	}

	void "test getDefaultTagInputTypes method"() {
		given: 'Two default TagInputType instances and one non default TagInputType instance'
		new TagInputType(tagId: 1, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.THUMBS, defaultUnit: 'miles', isDefault: true).save(flush: true)
		new TagInputType(tagId: 2, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.LEVEL, defaultUnit: 'hours', isDefault: true).save(flush: true)
		new TagInputType(tagId: 3, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.LEVEL, defaultUnit: 'hours').save(flush: true)

		when: 'The getDefaultTagInputTypes is called'
		List defaultTagInputs = TagInputType.getDefaultTagInputTypes()

		then: 'The list should contain the two default tags'
		defaultTagInputs.size() == 2
		defaultTagInputs[0].tagId == 1
		defaultTagInputs[1].tagId == 2
	}

	void "test getRecentTagsWithInputType method to return atleast 8 TagInputTypes"() {
		given: 'TagStats for User for two tags'
		TagStats.createOrUpdate(userInstance.id, tagInstance1.id)
		TagStats.createOrUpdate(userInstance.id, tagInstance2.id)

		[tagInstance1.id, tagInstance2.id].each {
			new TagInputType(tagId: it, max: 10, min: 0, noOfLevels: 5,
					inputType: InputType.LEVEL, defaultUnit: 'hours', isDefault: true).save(flush: true)
		}

		and: '6 Default TagInputTypes instances'
		['coffee', 'bowell movement', 'apple', 'banana', 'beer', 'dance'].each {
			new TagInputType(tagId: Tag.look(it).id , max: 10, min: 0, noOfLevels: 5,
					inputType: InputType.LEVEL, defaultUnit: 'hours', isDefault: true).save(flush: true)
		}

		when: 'The getRecentTagsWithInputType method is called'
		List tagsWithInputType = TagInputType.getRecentTagsWithInputType(userInstance.id)

		then: 'There should be 8 TagInputTypes'
		tagsWithInputType.size() == 8

		when: 'There are TagInputTypes for tags used in the past 14 days'
		['run', 'mood', 'misbehavior', 'coffee', 'bowell movement', 'apple', 'banana', 'beer'].each {
			TagStats.createOrUpdate(userInstance.id, Tag.look(it).id)
		}
		tagsWithInputType = TagInputType.getRecentTagsWithInputType(userInstance.id)

		then: 'There should be 10 TagInputTypes with no default TagInputTypes'
		tagsWithInputType.size() == 10
	}
}

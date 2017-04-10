package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec
import us.wearecurio.model.InputType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagInputType
import us.wearecurio.model.TagStats
import us.wearecurio.model.User

class TagInputTypeSpec extends IntegrationSpec {

	void "test various overloaded methods from TagInputType class"() {
		given: 'A few instances of Tag, TagStats and TagInputType'
		User userInstance = User.create([username :'shaneMac1', sex:'F', name:'shane macgowen', email:'sha@pogues.com',
				birthdate:'01/01/1960', password:'shanexyz', action:'doregister', controller:'home'])

		Tag tagInstance1 = Tag.create('sleep')
		Tag tagInstance2 = Tag.create('activity')

		new TagStats([userId: userInstance.id, tagId: tagInstance1.id, description: 'sleep', countLastThreeMonths: 80L,
				mostRecentUsage: new Date() - 2, countLastYear: 302L, countAllTime: 1024L]).save(flush: true)
		TagStats.createOrUpdate(userInstance.id, tagInstance2.id)

		new TagInputType(tagId: tagInstance1.id, max: 10, min: 0, noOfLevels: 5, userId: userInstance.id,
				inputType: InputType.THUMBS, defaultUnit: 'miles').save(flush: true)
		new TagInputType(tagId: tagInstance2.id, max: 10, min: 0, noOfLevels: 5, userId: userInstance.id,
				inputType: InputType.LEVEL, defaultUnit: 'hours').save(flush: true)

		when: 'getAllTagsWithInputType method is called and no data is present in cache'
		assert TagInputType.tagsWithInputTypeCache.size() == 0
		Map resultMap = TagInputType.getAllTagsWithInputType(null)

		then: 'Method returns all the new instances of TagInputType and cache gets updated'
		resultMap.size() == 2
		resultMap.tagsWithInputTypeList[0].tagId == tagInstance2.id
		resultMap.tagsWithInputTypeList[0].inputType == InputType.LEVEL
		resultMap.tagsWithInputTypeList[1].tagId == tagInstance1.id
		resultMap.tagsWithInputTypeList[1].inputType == InputType.THUMBS
		TagInputType.tagsWithInputTypeCache.size() == 2

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
}

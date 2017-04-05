package us.wearecurio.model.integration

import grails.test.spock.IntegrationSpec
import us.wearecurio.model.InputType
import us.wearecurio.model.Tag
import us.wearecurio.model.TagInputType
import us.wearecurio.model.TagStats
import us.wearecurio.model.User

class TagInputTypeSpec extends IntegrationSpec {

	void "test recentTagsWithInputType method for various cases"() {
		given: 'A few instances of Tag, TagStats and TagInputType'
		User userInstance = User.create([username :'shaneMac1', sex:'F', name:'shane macgowen', email:'sha@pogues.com',
				birthdate:'01/01/1960', password:'shanexyz', action:'doregister', controller:'home'])

		Tag tagInstance1 = Tag.create('sleep')
		Tag tagInstance2 = Tag.create('activity')

		new TagStats([userId: userInstance.id, tagId: tagInstance1.id, description: 'sleep',  countLastThreeMonths: 80L,
				mostRecentUsage: new Date() - 2, countLastYear: 302L, countAllTime: 1024L]).save(flush: true)
		TagStats.createOrUpdate(userInstance.id, tagInstance2.id)

		new TagInputType(tagId: tagInstance1.id, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.THUMBS).save(flush: true)
		new TagInputType(tagId: tagInstance2.id, max: 10, min: 0, noOfLevels: 5,
				inputType: InputType.LEVEL).save(flush: true)

		when: 'recentTagsWithInputType method is called and a date range is passed'
		List resultList = TagInputType.recentTagsWithInputType(new Date() - 20, new Date())

		then: 'Method should return only those tags whose mostRecentUsage lies within this range'
		resultList.size() == 1
		resultList[0].tagId == tagInstance1.id
		resultList[0].inputType == InputType.THUMBS
		resultList[0].max == 10
		resultList[0].min == 0
		resultList[0].description == 'sleep'
		resultList[0].noOfLevels == 5

		when: 'recentTagsWithInputType method is called and cache has not beed updated after lastInputTypeUpdate'
		resultList = TagInputType.recentTagsWithInputType(null, null, new Date())

		then: 'Method returns empty list in response'
		resultList == []

		when: 'recentTagsWithInputType method is called and cache has beed updated after lastInputTypeUpdate'
		resultList = TagInputType.recentTagsWithInputType(null, null, new Date() - 20)

		then: 'Method returns matching results in response'
		resultList.size() == 1
		resultList[0].tagId == tagInstance1.id
		resultList[0].inputType == InputType.THUMBS
		TagInputType.tagsWithInputTypeCache.size() == 1
		TagInputType.clearCache()

		when: 'recentTagsWithInputType method is called and no date range is passed'
		assert TagInputType.tagsWithInputTypeCache == [:]
		resultList = TagInputType.recentTagsWithInputType()

		then: 'Result map should contain all the available instances of TagInputType'
		resultList.size() == 2
		resultList[0].tagId == tagInstance2.id
		resultList[0].inputType == InputType.LEVEL
		resultList[1].tagId == tagInstance1.id
		resultList[1].inputType == InputType.THUMBS
		TagInputType.tagsWithInputTypeCache.size() == 2
		TagInputType.clearCache()
	}
}

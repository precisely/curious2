package us.wearecurio.controller.integration

import java.text.DateFormat
import java.util.Date

import org.junit.After
import org.junit.Before
import org.junit.Test

import us.wearecurio.controller.TagController
import grails.test.*
import us.wearecurio.model.*
import us.wearecurio.utility.Utils

class TagControllerTests extends CuriousControllerTestCase{

	def tagGroupService

	TagController controller
	DateFormat dateFormat
	Date earlyBaseDate
	Date currentTime
	Date endTime
	String timeZone // simulated server time zone
	Date baseDate
	Tag tag1, tag2
	TagGroup tagGroupInstance

	@Before
	void setUp() {
		super.setUp()

		controller = new TagController()

		tag1 = Tag.create("bread")
		tag2 = Tag.create("apple")
		tagGroupInstance = tagGroupService.createTagGroup("Demo tag group", userId, null)
		tagGroupService.addTags(tagGroupInstance, "$tag1.id, $tag2.id")
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "Test removeTagFromTagGroupData"() {
		
		assert tagGroupInstance.getTags(userId).size() == 2
		
		controller.session.userId = userId
		controller.params["tagGroupId"] = tagGroupInstance.id
		controller.params["id"] = tag1.id
		
		controller.removeTagFromTagGroupData()
		
		assert controller.response.json.success == true
		assert tagGroupInstance.getTags(userId).size() == 1
		assert !tagGroupInstance.containsTag(tag1, userId)
	}

	@Test
	void "Test deleteTagGroupData"() {
		
		assert GenericTagGroupProperties.countByUserIdAndTagGroupId(userId, tagGroupInstance.id) == 1
		
		controller.session.userId = userId
		controller.params["id"] = tagGroupInstance.id
		
		controller.deleteTagGroupData()
		
		assert controller.response.json.success == true
		assert GenericTagGroupProperties.countByUserIdAndTagGroupId(userId, tagGroupInstance.id) == 0
	}

}

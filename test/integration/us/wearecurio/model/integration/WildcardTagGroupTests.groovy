package us.wearecurio.model.integration

import static org.junit.Assert.*

import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.Tag
import us.wearecurio.model.TagExclusion
import us.wearecurio.model.WildcardTagGroup
import us.wearecurio.services.TagGroupService

class WildcardTagGroupTests extends CuriousTestCase {

	TagGroupService tagGroupService

	Tag tag1, tag2, tag3, tag4, tag5, tag6, tag7
	Entry entry1, entry2, entry3, entry4, entry5, entry6, entry7

	WildcardTagGroup wildcardTagGroupInstance1, wildcardTagGroupInstance2

	@Before
	void setUp() {
		super.setUp()

		Date currentTime = new Date()
		Date baseDate = new Date() - 5
		String timeZone = "America/Los_Angeles"

		tag1 = Tag.create("almond")
		tag2 = Tag.create("grilled chicken")
		tag3 = Tag.create("pasta tomato chicken")
		tag4 = Tag.create("dinner cobb salad with grilled chicken")
		tag5 = Tag.create("chicken")
		tag6 = Tag.create("bike")
		tag7 = Tag.create("chicken mole poblano")

		entry1 = Entry.create(userId, Entry.parse(currentTime, timeZone, tag1.description, baseDate, true), null)
		entry2 = Entry.create(userId, Entry.parse(currentTime, timeZone, tag2.description, baseDate, true), null)
		entry3 = Entry.create(userId, Entry.parse(currentTime, timeZone, tag3.description, baseDate, true), null)
		entry4 = Entry.create(userId, Entry.parse(currentTime, timeZone, tag4.description, baseDate, true), null)
		entry5 = Entry.create(userId, Entry.parse(currentTime, timeZone, tag5.description, baseDate, true), null)
		entry6 = Entry.create(userId, Entry.parse(currentTime, timeZone, tag6.description, baseDate, true), null)
		entry7 = Entry.create(userId, Entry.parse(currentTime, timeZone, tag7.description, baseDate, true), null)

		wildcardTagGroupInstance1 = tagGroupService.createOrLookupTagGroup("chicken", userId, 0, WildcardTagGroup.class)
		wildcardTagGroupInstance2 = tagGroupService.createOrLookupTagGroup("grilled chicken", userId, 0, WildcardTagGroup.class)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	void "test get tags"() {
		assert wildcardTagGroupInstance1.getTags(userId).size() == 2
		assert wildcardTagGroupInstance1.getTags(userId)*.description.contains("chicken")
		assert wildcardTagGroupInstance1.getTags(userId)*.description.contains("chicken mole poblano")
		assert wildcardTagGroupInstance2.getTags(userId).size() == 0
	}

	void "test get tags with no user id"() {
		assert wildcardTagGroupInstance1.getTags(null).size() == 0
	}

	void "test get tags with tag excluded"() {
		TagExclusion.createOrLookup(tag7, GenericTagGroupProperties.createOrLookup(userId, "User", wildcardTagGroupInstance1))

		assert wildcardTagGroupInstance1.getTags(userId).size() == 1
		assert wildcardTagGroupInstance1.getTags(userId)*.description.contains("chicken")
	}

	void "test contains tag"() {
		assert wildcardTagGroupInstance1.containsTag(tag1, userId) == false
		assert wildcardTagGroupInstance1.containsTag(tag5, userId) == true
		assert wildcardTagGroupInstance1.containsTag(tag7, userId) == true
	}

	void "test contains tag after exclusion"() {
		TagExclusion.createOrLookup(tag7, GenericTagGroupProperties.createOrLookup(userId, "User", wildcardTagGroupInstance1))

		assert wildcardTagGroupInstance1.containsTag(tag5, userId) == true
		assert wildcardTagGroupInstance1.containsTag(tag7, userId) == false
	}

	void "test contains tag string"() {
		assert wildcardTagGroupInstance1.containsTagString(tag5.description, userId) == true
		assert wildcardTagGroupInstance1.containsTagString(tag1.description, userId) == false
		assert wildcardTagGroupInstance1.containsTagString(tag7.description, userId) == true
	}
}
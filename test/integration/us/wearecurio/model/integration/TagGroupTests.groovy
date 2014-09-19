package us.wearecurio.model.integration

import static org.junit.Assert.*

import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.Tag
import us.wearecurio.model.TagExclusion
import us.wearecurio.model.TagGroup
import us.wearecurio.services.TagGroupService
import us.wearecurio.utility.Utils

class TagGroupTests extends CuriousTestCase {
	static transactional = true

	TagGroupService tagGroupService

	Tag tag1, tag2, tag3, tag4, tag5, tag6
	TagGroup tagGroup1, tagGroup2

	@Before
	void setUp() {
		super.setUp()
		
		tag1 = Tag.create("tag1")
		tag2 = Tag.create("tag2")
		tag3 = Tag.create("tag3")
		tag4 = Tag.create("tag4")
		tag5 = Tag.create("tag5")
		tag6 = Tag.create("tag6")

		tagGroup1 = tagGroupService.createOrLookupTagGroup("taggroup1", userId, null)
		tagGroup2 = tagGroupService.createOrLookupTagGroup("taggroup2", userId, null)

		tagGroup1.addToTags(tag1)
		tagGroup1.addToTags(tag3)
		tagGroup1.addToTags(tag4)

		tagGroup2.addToTags(tag2)
		tagGroup2.addToTags(tag5)
		tagGroup2.addToTags(tag6)

		Utils.save(tagGroup2, true)

		tagGroup1.addToSubTagGroups(tagGroup2)
		Utils.save(tagGroup1, true)
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void "test get tags"() {
		List associatedTags = TagGroup.findByDescription("taggroup1").getTags(userId)

		assert associatedTags.size() == 6
	}

	@Test
	void "test get tags with null user id"() {
		List associatedTags = TagGroup.findByDescription("taggroup1").getTags(null)

		assert associatedTags.size() == 6
	}

	@Test
	void "test get tags with tag excluded"() {
		TagExclusion.createOrLookup(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.createOrLookup(tag5, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup2))
		TagExclusion.createOrLookup(tag6, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup2))

		List<Tag> associatedTags = TagGroup.findByDescription("taggroup1").getTags(userId)

		assert associatedTags.size() == 3
		assert associatedTags*.description.sort() == [tag1.description, tag2.description, tag4.description]
	}

	@Test
	void "test get tags with tag & tag group excluded"() {
		TagExclusion.createOrLookup(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.createOrLookup(tagGroup2, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))

		// Will not return any tags from tagGroup2 since it is excluded
		List<Tag> associatedTags = TagGroup.findByDescription("taggroup1").getTags(userId)

		assert associatedTags.size() == 2
		assert associatedTags*.description.sort() == [tag1.description, tag4.description]
	}

	@Test
	void "test get tags with exclusion & no user id"() {
		TagExclusion.createOrLookup(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.createOrLookup(tagGroup2, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))

		// Will not return any tags from tagGroup2 since it is excluded
		List<Tag> associatedTags = TagGroup.findByDescription("taggroup1").getTags(null)

		assert associatedTags.size() == 6
	}

	@Test
	void "test contains tag"() {
		TagGroup tagGroupInstance = TagGroup.findByDescription("taggroup1")
		// Check for top level sub tag
		assert tagGroupInstance.containsTag(Tag.look("tag1"), userId) == true
		// Check for nested sub tag
		assert tagGroupInstance.containsTag(Tag.look("tag5"), userId) == true
		assert tagGroupInstance.containsTag(Tag.look("other tag"), userId) == false
	}

	@Test
	void "test contains tag after exclusion"() {
		TagGroup tagGroupInstance = TagGroup.findByDescription("taggroup1")

		TagExclusion.createOrLookup(tag3, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))
		TagExclusion.createOrLookup(tagGroup2, GenericTagGroupProperties.createOrLookup(userId, "User", tagGroup1))

		// Test after exclusion
		assert tagGroupInstance.containsTag(Tag.look("tag3"), userId) == false
		assert tagGroupInstance.containsTag(Tag.look("tag5"), userId) == false
	}

	@Test
	void "test contains tag string"() {
		TagGroup tagGroupInstance = TagGroup.findByDescription("taggroup1")
		// Check for top level sub tag string
		assert tagGroupInstance.containsTagString("tag1", userId) == true
		// Check for nested sub tag string
		assert tagGroupInstance.containsTagString("tag5", userId) == true
		assert tagGroupInstance.containsTagString("other tag", userId) == false
		// Check for blank string
		assert tagGroupInstance.containsTagString("", userId) == false
		// Test for null tag string
		assert tagGroupInstance.containsTagString(null, userId) == false
	}
}
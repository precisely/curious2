package us.wearecurio.model.integration

import static org.junit.Assert.*

import org.junit.*

import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.GenericTagGroupProperties
import us.wearecurio.model.Tag
import us.wearecurio.model.TagGroup
import us.wearecurio.model.TagExclusion
import us.wearecurio.model.WildcardTagGroup
import us.wearecurio.services.EntryParserService
import us.wearecurio.support.EntryStats

class WildcardTagGroupTests extends CuriousTestCase {

	Tag tag1, tag2, tag3, tag4, tag5, tag6, tag7
	Entry entry1, entry2, entry3, entry4, entry5, entry6, entry7

	WildcardTagGroup wildcardTagGroupInstance1, wildcardTagGroupInstance2
	
	EntryParserService entryParserService

	@Before
	void setUp() {
		super.setUp()
		
		Date currentTime = new Date()
		Date baseDate = new Date() - 5
		String timeZone = "America/Los_Angeles"

		tag1 = Tag.look("almond")
		tag2 = Tag.look("grilled chicken")
		tag3 = Tag.look("pasta tomato chicken")
		tag4 = Tag.look("dinner cobb salad with grilled chicken")
		tag5 = Tag.look("chicken")
		tag6 = Tag.look("bike")
		tag7 = Tag.look("chicken mole poblano")
		
		EntryStats stats = new EntryStats(userId)

		entry1 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, tag1.description, null, null, baseDate, true), stats)
		entry2 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, tag2.description, null, null, baseDate, true), stats)
		entry3 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, tag3.description, null, null, baseDate, true), stats)
		entry4 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, tag4.description, null, null, baseDate, true), stats)
		entry5 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, tag5.description, null, null, baseDate, true), stats)
		entry6 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, tag6.description, null, null, baseDate, true), stats)
		entry7 = Entry.create(userId, entryParserService.parse(currentTime, timeZone, tag7.description, null, null, baseDate, true), stats)

		wildcardTagGroupInstance1 = TagGroup.createOrLookupTagGroup("chicken", userId, 0, WildcardTagGroup.class)
		wildcardTagGroupInstance2 = TagGroup.createOrLookupTagGroup("grilled chicken", userId, 0, WildcardTagGroup.class)
		
		System.out.println "User.tagIdCache (setUp)"
		us.wearecurio.model.User.tagIdCache.each{ k, v -> println "${k}:${v}" }
		System.out.println "User.tagGroupIdCache (setUp)"
		us.wearecurio.model.User.tagGroupIdCache.each{ k, v -> println "${k}:${v}" }
	}

	@After
	void tearDown() {
		super.tearDown()
		
		System.out.println "User.tagIdCache (tearDown--before clear)"
		us.wearecurio.model.User.tagIdCache.each{ k, v -> println "${k}:${v}" }
		System.out.println "User.tagGroupIdCache (tearDown--before clear)"
		us.wearecurio.model.User.tagGroupIdCache.each{ k, v -> println "${k}:${v}" }

		def iterator = us.wearecurio.model.User.tagIdCache.entrySet().iterator()		
		while (iterator.hasNext()) {
			iterator.next()
			iterator.remove()
		}
		
		def iterator2 = us.wearecurio.model.User.tagGroupIdCache.entrySet().iterator()
		while (iterator2.hasNext()) {
			iterator2.next()
			iterator2.remove()
		}
		
		System.out.println "User.tagIdCache (tearDown--after clear)"
		us.wearecurio.model.User.tagIdCache.each{ k, v -> println "${k}:${v}" }
		System.out.println "User.tagGroupIdCache (tearDown--after clear)"
		us.wearecurio.model.User.tagGroupIdCache.each{ k, v -> println "${k}:${v}" }
	}

	@Test
	void "test get tags"() {
		assert wildcardTagGroupInstance1.getTags(userId).size() == 2
		assert wildcardTagGroupInstance1.getTags(userId)*.description.contains("chicken")
		assert wildcardTagGroupInstance1.getTags(userId)*.description.contains("chicken mole poblano")
		assert wildcardTagGroupInstance2.getTags(userId).size() == 0
	}

	@Test
	void "test get tags with no user id"() {
		assert wildcardTagGroupInstance1.getTags(null).size() == 0
	}

	@Test
	void "test get tags with tag excluded"() {
		TagExclusion.addToExclusion(tag7, GenericTagGroupProperties.createOrLookup(userId, "User", wildcardTagGroupInstance1))

		assert wildcardTagGroupInstance1.getTags(userId).size() == 1
		assert wildcardTagGroupInstance1.getTags(userId)*.description.contains("chicken")
	}

	@Test
	void "test contains tag"() {
		assert wildcardTagGroupInstance1.containsTag(tag1, userId) == false
		assert wildcardTagGroupInstance1.containsTag(tag5, userId) == true
		assert wildcardTagGroupInstance1.containsTag(tag7, userId) == true
	}

	@Test
	void "test contains tag after exclusion"() {
		TagExclusion.addToExclusion(tag7, GenericTagGroupProperties.createOrLookup(userId, "User", wildcardTagGroupInstance1))

		assert wildcardTagGroupInstance1.containsTag(tag5, userId) == true
		assert wildcardTagGroupInstance1.containsTag(tag7, userId) == false
	}

	@Test
	void "test contains tag string"() {
		assert wildcardTagGroupInstance1.containsTagString(tag5.description, userId) == true
		assert wildcardTagGroupInstance1.containsTagString(tag1.description, userId) == false
		assert wildcardTagGroupInstance1.containsTagString(tag7.description, userId) == true
	}
}
package us.wearecurio.services.integration

import static org.junit.Assert.*
import org.junit.*

import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.services.EntryService
import us.wearecurio.factories.EntryFactory

class EntryServiceTests extends CuriousServiceTestCase {
	def entryService
	Entry[] entries
	
	@Before
	void setUp() {
		super.setUp()
	}
	
	@After
	void tearDown() {
		super.tearDown()
	}
	
	@Test
	void testGetEntriesByTagName() {
		def entries = EntryFactory.makeN(10)
		def tag1_entries = entryService.getEntriesByUserAndTag(userId, 'tag1')
		assert tag1_entries.size() == 10
	}
	
	@Test
	void testGetEntriesByUserAndTaggable() {
		def entries = EntryFactory.makeN(10)
		def tag1_entries = entryService.getEntriesByUserAndTag(userId, 'tag1')
		assert tag1_entries.size() == 10
	}
	
	@Test
	void testGetUserStartTime() {
		def entries1 = EntryFactory.makeN(3, { Math.sin(it) },     ['tag_description': 'tag1', 'username': 'a'])
		def entries2 = EntryFactory.makeN(3, { Math.cos(it) },     ['tag_description': 'tag2', 'username': 'a'])
		def entries3 = EntryFactory.makeN(3, { Math.sin(it) },     ['tag_description': 'tag1', 'username': 'b'])
		def entries4 = EntryFactory.makeN(3, { Math.cos(it) },     ['tag_description': 'tag2', 'username': 'b'])
		entries2[1].date = EntryFactory.START_DAY - 10
		entries2[1].save()
		entries3[2].date = EntryFactory.START_DAY - 42
		entries3[2].save()
		
		assert Entry.count() == 12
		
		def user_a = User.findWhere('username': 'a')
		def start_time = entryService.userStartTime(user_a.id)
		assert start_time == EntryFactory.START_DAY - 10
		
		def user_b = User.findWhere('username': 'b')
		start_time = entryService.userStartTime(user_b.id)
		assert start_time == EntryFactory.START_DAY - 42
	}
	
	@Test
	void testGetUserStopTime() {
		def entries1 = EntryFactory.makeN(3, { Math.sin(it) },     ['tag_description': 'tag1', 'username': 'a'])
		def entries2 = EntryFactory.makeN(3, { Math.cos(it) },     ['tag_description': 'tag2', 'username': 'a'])
		def entries3 = EntryFactory.makeN(3, { Math.sin(it) },     ['tag_description': 'tag1', 'username': 'b'])
		def entries4 = EntryFactory.makeN(3, { Math.cos(it) },     ['tag_description': 'tag2', 'username': 'b'])
		entries2[1].date = EntryFactory.START_DAY + 10
		entries2[1].save()
		entries3[2].date = EntryFactory.START_DAY + 42
		entries3[2].save()
		
		assert Entry.count() == 12
		
		def user_a = User.findWhere('username': 'a')
		def start_time = entryService.userStopTime(user_a.id)
		assert start_time == EntryFactory.START_DAY + 10
		
		def user_b = User.findWhere('username': 'b')
		start_time = entryService.userStopTime(user_b.id)
		assert start_time == EntryFactory.START_DAY + 42
	}
}

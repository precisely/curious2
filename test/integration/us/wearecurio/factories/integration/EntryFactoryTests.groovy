package us.wearecurio.factories.integration

import static org.junit.Assert.*
import org.junit.*
import us.wearecurio.factories.EntryFactory
import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Entry
import us.wearecurio.model.Tag
import us.wearecurio.model.User


class EntryFactoryTests extends CuriousTestCase {
	@Before
	void setUp() {
		Entry.executeUpdate("delete Entry")
		Tag.executeUpdate("delete Tag")
		User.executeUpdate("delete User")
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
		Entry.executeUpdate("delete Entry")
		Tag.executeUpdate("delete Tag")
		User.executeUpdate("delete User")
	}

	@Test
	void testMake() {
		def entry = EntryFactory.make()
		assert entry.amount == EntryFactory.DEFAULT_AMOUNT
		assert entry.date == EntryFactory.START_DAY
		assert entry.validate()
		assert Entry.countByAmount(EntryFactory.DEFAULT_AMOUNT) == 1
	}

	@Test
	void testMakeN() {
		def entries = EntryFactory.makeN(10)
		assert Entry.count() == 10
		assert entries.class == ArrayList
		assert entries.first().class == Entry
	}

	@Test
	void testMakeNShouldHaveDifferentDates() {
		def entries = EntryFactory.makeN(10)
		assert Entry.count() == 10

		// The first and last days should be 9 days apart.
		//	 (If you have N=2 days, the difference is 1 day.
		//		If you have N days, then the difference is N-1.)
		assert (entries.last().date - entries.first().date) == 9
	}

	@Test
	void testMake2EntriesOnTheSameDate() {
		def entry1 = EntryFactory.make()
		def entry2 = EntryFactory.make()
		assert Entry.count() == 2
		assert entry2.date - entry1.date == 0
	}

	@Test
	void testMakeFirstDayHaveTwoEntries() {
		def entry1 = EntryFactory.make()
		def entries = EntryFactory.makeN(3)
		assert Entry.count == 4
		assert entry1.date - entries.first().date == 0

		entries = entries + entry1
		assert entries.size() == 4
		assert entries.collect { it.tagId } == [Tag.findAll().first().id]*4
	}

	@Test
	void testAllEntriesHaveSameTagId() {
		def entry1 = EntryFactory.make()
		def entries = EntryFactory.makeN(3)

		entries = entries + entry1
		assert entries.size() == 4
		assert entries.collect { it.tagId } == [Tag.findAll().first().id]*4
	}


	@Test
	void testWithTagDescription() {
		def entry = EntryFactory.make(['tag_description': 'my tag'])
		entry.validate()
		assert entry.errors.allErrors == []
		assert Tag.count() == 1
		assert Entry.count() == 1
		assert Tag.first().description == 'my tag'
	}

	@Test
	void testWithUsername() {
		def entry = EntryFactory.make(['tag_description': 'my tag', 'username': 'a'])
		entry.validate()
		assert entry.errors.allErrors == []
		assert Tag.count() == 1
		assert Entry.count() == 1
		assert Tag.first().description == 'my tag'

		assert User.count() == 1
		assert User.last().username == 'a'

		entry = EntryFactory.makeN(3, { it }, ['tag_description': 'tag42', 'username': 'b'])
		Tag.count() == 2
		Entry.count() == 4
		Tag.last().description == 'tag42'
		User.count() == 2
		assert User.last().username == 'b'
		assert Entry.findAllWhere('userId': User.last().id.toLong()).size() == 3
	}

	@Test
	void testMakeEntryWithRepeatTypeDaily() {
		def entry_start = EntryFactory.make(['tag_description': 'my tag', 'username': 'a', 'repeatType': Entry.RepeatType.DAILY, 'repeatEnd': EntryFactory.START_DAY + 1])
		def entry_stop = EntryFactory.make(['tag_description': 'my tag', 'username': 'a'])
		assert entry_start.repeatType == Entry.RepeatType.DAILY
		assert entry_start.repeatEnd == EntryFactory.START_DAY + 1
	}

	@Test
	void testMakeDurationEntry() {
		def entry_start = EntryFactory.make(['tag_description': 'my tag start', 'username': 'a', 'time': Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:00' )])
		def entry_stop = EntryFactory.make(['tag_description': 'my tag stop', 'username': 'a', 'time': Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:15' )])
		entry_start.units = "hours"
		entry_stop.units = "hours"
		assert Entry.count() == 2
		assert entry_start.durationType == Entry.DurationType.START
		assert entry_start.date == Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:00' )
		assert entry_stop.durationType == Entry.DurationType.END
		assert entry_stop.date == Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:15' )
		assert entry_start.calculateDuration() == null
		assert entry_stop.fetchPreviousStartOrEndEntry() == entry_start
		assert entry_stop.calculateDuration() == 15*60*1000
	}

}

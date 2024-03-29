import grails.test.*

import java.text.DateFormat

import us.wearecurio.integration.CuriousUserTestCase;
import us.wearecurio.model.TagProperties
import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.model.Tag
import us.wearecurio.services.EntryParserService
import us.wearecurio.support.EntryStats
import us.wearecurio.support.EntryCreateMap

import static org.junit.Assert.*
import org.junit.*
import grails.test.mixin.*

class TagPropertiesTests extends CuriousUserTestCase {
	static transactional = true
	
	EntryParserService entryParserService
	
	DateFormat dateFormat
	Date baseDate
	String timeZone
	Date time1
	Date time2
	Date time3

	@Before
	void setUp() {
		super.setUp()
		timeZone = "Etc/UTC"
		dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
		baseDate = dateFormat.parse("July 1, 2010 12:00 am")
		time1 = dateFormat.parse("July 1, 2010 3:30 pm")
		time2 = dateFormat.parse("July 2, 2010 3:30 pm")
		time3 = dateFormat.parse("July 3, 2010 3:30 pm")
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testIsContinuousWord() {
		assert TagProperties.isContinuousWord("not in list") == false
		assert TagProperties.isContinuousWord("heart rate") == true
		assert TagProperties.isContinuousWord("headache") == false
		assert TagProperties.isContinuousWord("cholesterol") == true
		assert TagProperties.isContinuousWord("thighs [distance]") == true
		assert TagProperties.isContinuousWord("thigh circumference [distance]") == true
		assert TagProperties.isContinuousWord("blood pressure") == true
		assert TagProperties.isContinuousWord("blood pressure [systolic]") == true
		assert TagProperties.isContinuousWord("blood pressure [diastolic]") == true
	}

/*	@Test
	void testIsEventWord() {
		assert TagProperties.isEventWord("headache") == true
		assert TagProperties.isEventWord("not in list") == false
		assert TagProperties.isEventWord("heart rate") == false
		assert TagProperties.isEventWord("ate apple") == true
		assert TagProperties.isEventWord("exercised in the morning") == true
		assert TagProperties.isEventWord("jogging with Fred") == true
	}

	@Test
	void testClassifyAsEventDeterminedByContinuousWordPattern() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "heart rate 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "heart rate 42", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "heart rate 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert TagProperties.count() == 1
		assert property.dataTypeManual == TagProperties.UNSPECIFIED

		assert property.dataTypeComputed == TagProperties.CONTINUOUS
	}

	@Test
	void testClassifyAsEventDeterminedByDefaultValue() {
		// Series that have values and not in the list of word patterns are
		//	classified as CONTINUOUS by default.
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "foobar 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "foobar 42", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "foobar 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert TagProperties.count() == 1
		assert property.dataTypeManual == TagProperties.UNSPECIFIED

		property.classifyAsEvent()
		assert property.dataTypeComputed == TagProperties.EVENT
	}

	@Test
	void testDefaultValueOfIsManuallySet() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "bread 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "bread 42", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "bread 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert TagProperties.count() == 1

		assert property.fetchDataType() == TagProperties.EVENT
		assert !property.isContinuous
	}

	@Test
	void testPercentEntriesWith3Values() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "bread 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "bread 42", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "bread 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert property.percentEntriesWithoutValues() == 0.0
	}

	@Test
	void testPercentEntriesWith2Values() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "bread 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "bread", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "bread 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert property.percentEntriesWithoutValues() == 0.3333333333
	}

	@Test
	void testPercentEntriesWith1Value() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "bread 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "bread", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "bread", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert property.percentEntriesWithoutValues() == 0.6666666667
	}

	@Test
	void testPercentEntriesWith0Values() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "bread", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "bread", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "bread", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert property.percentEntriesWithoutValues() == 1.0
	}

	@Test
	void testMatch() {
		def patterns = ["head.?ache", "pain", "heart", "high bp"]
		def not_patterns = ["foobar", "headCache", "/h bp in/"]
		assert TagProperties.match(patterns, not_patterns, "not in list") == false
		assert TagProperties.match(patterns, not_patterns, "pain") == true
		assert TagProperties.match(patterns, not_patterns, "head ache") == true
		assert TagProperties.match(patterns, not_patterns, "headache") == true
		assert TagProperties.match(patterns, not_patterns, "headCache") == false
		assert TagProperties.match(patterns, not_patterns, "good bp") == false
		assert TagProperties.match(patterns, not_patterns, "high") == false
		assert TagProperties.match(patterns, not_patterns, "bp") == false
		assert TagProperties.match(patterns, not_patterns, "high bp") == true
		assert TagProperties.match(patterns, not_patterns, "high bp in the morning") == false
	}

	@Test
	void testClassifyAsEventSetByUser() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "bread 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "bread 42", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "bread 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert TagProperties.count() == 1
		assert property.dataTypeManual == TagProperties.UNSPECIFIED

		property.dataTypeManual = TagProperties.CONTINUOUS
		property.classifyAsEvent()
		assert property.dataTypeComputed == TagProperties.CONTINUOUS
	}

	@Test
	void testClassifyAsEventDeterminedByPercentOfNoValues() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "bread 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "bread", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "bread 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert TagProperties.count() == 1
		assert property.dataTypeManual == TagProperties.UNSPECIFIED

		assert property.dataTypeComputed == TagProperties.EVENT

		property.classifyAsEvent()
		assert property.dataTypeComputed == TagProperties.EVENT
	}

	@Test
	void testClassifyAsEventDeterminedByEventWordPattern() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "head ache 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "head ache 42", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "head ache 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert TagProperties.count() == 1
		assert property.dataTypeManual == TagProperties.UNSPECIFIED

		assert property.dataTypeComputed == TagProperties.EVENT

		property.classifyAsEvent()
		assert property.dataTypeComputed == TagProperties.EVENT
	}

	@Test
	void testSetContinuous() {
		def parsedEntry1 = entryParserService.parse(time1, timeZone, "heart rate 0.0", null, null, baseDate, true)
		def parsedEntry2 = entryParserService.parse(time2, timeZone, "heart rate 42", null, null, baseDate, true)
		def parsedEntry3 = entryParserService.parse(time3, timeZone, "heart rate 500", null, null, baseDate, true)
		EntryStats stats = new EntryStats(userId)
		def entry1 = Entry.create(userId, parsedEntry1, stats)
		def entry2 = Entry.create(userId, parsedEntry2, stats)
		def entry3 = Entry.create(userId, parsedEntry3, stats)
		stats.finish()
		def tag = entry1.getTag()
		def entries = Entry.findAllWhere(userId: userId, tag: tag)
		assert entries.size == 3
		def property = TagProperties.createOrLookup(userId, tag.id)
		assert TagProperties.count() == 1
		assert property.dataTypeManual == TagProperties.UNSPECIFIED

		property.setDataType(TagProperties.EVENT)
		assert property.dataTypeComputed == TagProperties.EVENT
		property.setDataType(TagProperties.UNSPECIFIED)
		assert property.dataTypeComputed == TagProperties.CONTINUOUS
		property.setDataType(TagProperties.EVENT)
		assert property.dataTypeComputed == TagProperties.EVENT
		property.setDataType(TagProperties.CONTINUOUS)
		assert property.dataTypeComputed == TagProperties.CONTINUOUS
		assert property.fetchDataType() == TagProperties.CONTINUOUS
	}
	/**/
}

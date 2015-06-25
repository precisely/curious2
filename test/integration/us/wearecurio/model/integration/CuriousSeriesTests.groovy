package us.wearecurio.model.integration


import org.junit.*
import static org.junit.Assert.*

import us.wearecurio.model.CuriousSeries
import us.wearecurio.model.Entry
import us.wearecurio.model.RepeatType
import us.wearecurio.model.Stats
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.factories.EntryFactory
import us.wearecurio.factories.CuriousSeriesFactory
import us.wearecurio.integration.CuriousTestCase

import org.apache.commons.logging.LogFactory
import java.text.SimpleDateFormat


class CuriousSeriesTests extends CuriousTestCase {
	private static def LOG = new File("debug.out")
	
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
	
	public static def log(text) {
		LOG.withWriterAppend("UTF-8", { writer ->
			writer.write( "CuriousSeriesTests: ${text}\n")
		})
	}
	
	@Test
	void testGetFirstEntry() {
		def entries = EntryFactory.makeN(3, { it })
		entries[1].date = entries[1].date - 10
		assert CuriousSeries.firstEntry(entries) == entries[1]
	}
	
	@Test
	void testGetLastEntry() {
		def entries = EntryFactory.makeN(3, { it })
		entries[1].date = entries[1].date + 10
		assert us.wearecurio.model.CuriousSeries.lastEntry(entries) == entries[1]
	}
	
	@Test
	void testValuesShouldBeDoubles() {
		def entries = EntryFactory.makeN(10, { it })
		assert Entry.count() == 10
		def series = new us.wearecurio.model.CuriousSeries(entries)
		assert series.times.size() == 10
		assert series.times[0] == EntryFactory.START_DAY
		assert series.values.size() == 10
		assert series.values[0] == 1
	}
	
	@Test
	void testKeysShouldBeDates() {
		def entries = EntryFactory.makeN(10, { it })
		def series = new us.wearecurio.model.CuriousSeries(entries)
		assert series.times.size() == 10
		assert series.times[0] == EntryFactory.START_DAY
		assert series.times[1] == EntryFactory.START_DAY + 1
		assert series.times[9] == EntryFactory.START_DAY + 9
	}
	
	@Test
	void testThereShouldBeOneDateForEveryDayInTheInterval() {
		// 3 consecutive days.
		def entries = EntryFactory.makeN(3, { it })
		
		// The first day should be two days before the first of the previous days.
		def entry0 = EntryFactory.make()
		entry0.amount = 100
		entry0.date = EntryFactory.START_DAY - 2
		
		entries = entries + entry0
		def series = new us.wearecurio.model.CuriousSeries(entries)
		def times = series.getTimes()
		assert times[0] == EntryFactory.START_DAY - 2
		assert times[1] == EntryFactory.START_DAY - 1
		assert times[2] == EntryFactory.START_DAY
		assert times[3] == EntryFactory.START_DAY + 1
		assert times[4] == EntryFactory.START_DAY + 2
	}
	
	@Test
	void testEntryOnSameDayShouldReturnTheAverage() {
		def late_entry
		late_entry = EntryFactory.make()
		late_entry.date = EntryFactory.START_DAY + 3
		
		def entries = EntryFactory.makeN(2, { it }) + EntryFactory.make() + late_entry
		// Values on each day looks like this: [[1, 42], 2, null, 42]
		
		def series = new us.wearecurio.model.CuriousSeries(entries)
		def values = series.getValues()
		assert values[0] == 21.5
		assert values[1] == 2
		assert values[2] == null
		assert values[3] == 42
	}
	
	@Test
	void testMergedTimes() {
		def series1 = CuriousSeriesFactory.make()
		def ArrayList times = us.wearecurio.model.CuriousSeries.mergedTimes(series1, series1)
		assert times == [0, 1, 2].collect { EntryFactory.START_DAY + it }
		
		def series2 = CuriousSeriesFactory.makeShifted(3, { 2*it }, -2)
		times = us.wearecurio.model.CuriousSeries.mergedTimes(series1, series2)
		assert times == [-2, -1, 0, 1, 2].collect { EntryFactory.START_DAY + it }
		
		def series3 = CuriousSeriesFactory.makeShifted(4, { 3*it }, 2)
		times = us.wearecurio.model.CuriousSeries.mergedTimes(series3, series2)
		assert times == [-2, -1, 0, 1, 2, 3, 4, 5].collect { EntryFactory.START_DAY + it }
		
	}
	
	@Test
	void testValuesOn() {
		def series1 = CuriousSeriesFactory.make()
		def series2 = CuriousSeriesFactory.makeShifted(3, { 2*it }, -2)
		def series3 = CuriousSeriesFactory.makeShifted(4, { 3*it }, 2)
		def timeDomain = us.wearecurio.model.CuriousSeries.mergedTimes(series2, series3)
		assert us.wearecurio.model.CuriousSeries.valuesOn(series1, timeDomain) ==
		[
			null,
			null,
			1,
			2,
			3,
			null,
			null,
			null
		]
		assert us.wearecurio.model.CuriousSeries.valuesOn(series2, timeDomain) ==
		[
			2,
			4,
			6,
			null,
			null,
			null,
			null,
			null
		]
		assert us.wearecurio.model.CuriousSeries.valuesOn(series3, timeDomain) ==
		[
			null,
			null,
			null,
			null,
			3,
			6,
			9,
			12
		]
	}
	
	@Test
	void testPartialOverlapSeries() {
		//					 VALUES
		// time  series1 series2
		// ----  ------- -------
		// -1		 sin(1)  null
		// 0		 sin(2)  1
		// 1		 sin(3)  2
		// 2		 null		 3
		
		us.wearecurio.model.CuriousSeries series1 = CuriousSeriesFactory.makeShifted(3, { Math.sin(it) }, -1)
		us.wearecurio.model.CuriousSeries series2 = CuriousSeriesFactory.makeShifted(3, { it }, 0)
		
		def correct =  -0.21841
		def amu = 0.00001
		assert us.wearecurio.model.CuriousSeries.cor(series1, series2) - amu < correct && us.wearecurio.model.CuriousSeries.cor(series1, series2) + amu > correct
	}
	
	@Test
	void testReturnNullIfThereAreLessThanTwoValues() {
		def entry1 = EntryFactory.make('amount': 3)
		def series1 = new CuriousSeries([entry1])
		def entry2 = EntryFactory.make('amount': 42)
		def series2 = new CuriousSeries([entry2])
		
		//					 VALUES
		// time  series1 series2
		// ----  ------- -------
		// 0		 3			 42
		
		assert us.wearecurio.model.CuriousSeries.cor(series1, series2) == null
	}
	
	
	@Test
	void testNewSeriesFromTagAndUser() {
		def entries = EntryFactory.makeN(10, { it })
		assert Tag.count() == 1
		assert User.count() == 1
		def tag = Tag.first()
		def series = us.wearecurio.model.CuriousSeries.create(tag, User.first().id)
		assert series.entries.size() == 10
		assert series.times.size() == 10
		assert series.times[0] == EntryFactory.START_DAY
		assert series.times[1] == EntryFactory.START_DAY + 1
		assert series.times[9] == EntryFactory.START_DAY + 9
		assert series.values[0] == 1
		assert series.values[1] == 2
		assert series.values[9] == 10
		assert series.userId == User.first().id
		assert series.source == Tag
		assert series.sourceId == Tag.first().id
		assert series.sourceDescription == Tag.first().description
	}
	
	@Test
	void testNewSeriesWithOneEntry() {
		def entries = [EntryFactory.make()]
		assert entries[0].date != null
		assert entries[0].amount != null
		
		assert Tag.count() == 1
		assert User.count() == 1
		assert Entry.count() == 1
		
		def series = CuriousSeries.create(entries)
		assert series.class == CuriousSeries
		assert series.size() == 1
	}
	
	@Test
	void testNewSeriesWithNullDate() {
		// Make a CuriousSeries from an Entry with null date.
		// Remove entries with null date.
		def entries = [
			EntryFactory.make(time: null)
		]
		entries[0].date = null
		
		assert Tag.count() == 1
		assert User.count() == 1
		assert Entry.count() == 1
		
		def series = CuriousSeries.create(entries)
		assert series == null
	}
	
	@Test
	void testNewSeriesWithNullDateButOneValidEntry() {
		// Make a CuriousSeries from an Entry with null date.
		// Remove entries with null date.
		def entries = EntryFactory.makeN(2, { it })
		entries[0].date = null
		entries[0].save()
		
		assert Tag.count() == 1
		assert User.count() == 1
		assert Entry.count() == 2
		
		def series = CuriousSeries.create(entries)
		assert series.size() == 1
	}
	
	@Test
	void testNewSeriesWithNullAmount() {
		// Null amount is not an error.  It just means that we want the
		//	 tag to appear in the user's list.
		def entries = EntryFactory.makeN(2, { it })
		entries[0].amount = null
		
		def series = CuriousSeries.create(entries)
		assert series.size() == 1
		assert series.values[0] == 2
		assert series.times[0] == EntryFactory.START_DAY + 1
	}
	
	@Test
	void testStandardizedSeriesWithSameValuesShouldTurnIntoAOneSeries() {
		def entries = EntryFactory.makeN(10, { 99 })
		def series = CuriousSeries.create(entries)
		def values = Stats.standardize(series.values)
		assert values.size() == 10
		assert values[0] == 1
		assert values[1] == 1
		assert values[2] == 1
		// ...
		assert values[9] == 1
	}
	
	@Test
	void testDurationTypeUseEndMinusStartInMilliecondsAsValue() {
		def entry0 = EntryFactory.make('tag_description': 'sleep start', durationType: Entry.DurationType.START)
		entry0.amount = 100
		entry0.units = "hours"
		entry0.date = Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:00' )
		
		def entry1 = EntryFactory.make('tag_description': 'sleep stop', durationType: Entry.DurationType.END)
		entry1.amount = 100
		entry1.units = "hours"
		entry1.date = Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:17' )
		
		def entry2 = EntryFactory.make('tag_description': 'sleep start', durationType: Entry.DurationType.START)
		entry2.amount = 100
		entry2.units = "hours"
		entry2.date = Date.parse( 'dd-MM-yyyy HH:mm', '05-01-2000 05:00' )
		
		def entry3 = EntryFactory.make('tag_description': 'sleep stop', durationType: Entry.DurationType.END)
		entry3.amount = 100
		entry3.units = "hours"
		entry3.date = Date.parse( 'dd-MM-yyyy HH:mm', '05-01-2000 05:50' )
		
		def entries = [
			entry0,
			entry1,
			entry2,
			entry3
		]
		assert entries[1].fetchPreviousStartOrEndEntry() == entry0
		assert entries[3].fetchPreviousStartOrEndEntry() == entry2
		
		def series = CuriousSeries.create(entries)
		assert Stats.sizeNotNull(series.values) == 2
		assert series.values[0] == (17*60*1000)
		assert series.values[1] == null
		assert series.values[2] == null
		assert series.values[3] == (50*60*1000)
		assert series.times[0] == EntryFactory.START_DAY + 1
		assert series.times[3] == EntryFactory.START_DAY + 4
	}
	@Test
	void testDurationTypeSumDurationsForTheDay() {
		def entry0 = EntryFactory.make('tag_description': 'sleep start', durationType: Entry.DurationType.START)
		entry0.amount = 100
		entry0.units = "hours"
		entry0.date = Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:00' )
		
		def entry1 = EntryFactory.make('tag_description': 'sleep stop', durationType: Entry.DurationType.END)
		entry1.amount = 100
		entry1.units = "hours"
		entry1.date = Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 01:17' )
		
		def entry2 = EntryFactory.make('tag_description': 'sleep start', durationType: Entry.DurationType.START)
		entry2.amount = 100
		entry2.units = "hours"
		entry2.date = Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 05:00' )
		
		def entry3 = EntryFactory.make('tag_description': 'sleep stop', durationType: Entry.DurationType.END)
		entry3.amount = 100
		entry3.units = "hours"
		entry3.date = Date.parse( 'dd-MM-yyyy HH:mm', '02-01-2000 05:50' )
		
		def entries = [
			entry0,
			entry1,
			entry2,
			entry3
		]
		assert entries[1].fetchPreviousStartOrEndEntry() == entry0
		assert entries[3].fetchPreviousStartOrEndEntry() == entry2
		
		def series = CuriousSeries.create(entries)
		assert series.size() == 1
		assert series.values[0] == (17*60*1000) + (50*60*1000)
		assert series.times[0] == EntryFactory.START_DAY + 1
	}
	
	@Test
	void testDurationTypeUseEndDateAsDate() {
		def entry0 = EntryFactory.makeStart('sleep', '02-01-2000 01:17')
		def entry1 = EntryFactory.makeStop('sleep', '03-01-2000 01:17')
		
		def entries = [entry0, entry1]
		def series = CuriousSeries.create(entries)
		assert entries[1].fetchPreviousStartOrEndEntry() == entry0
		
		assert series.size() == 2
		assert series.values[0] == null
		assert series.values[1] == 1000*60*60*24
		assert series.times[1] == EntryFactory.START_DAY + 2
	}
	
	
	@Test
	void testDurationTypeStartSetAmountToNullIfNoCorrespondingEnd() {
		def entries = [
			EntryFactory.makeStart('sleep', '02-01-2000 01:17')
		]
		entries[0].durationType = Entry.DurationType.START
		assert entries[0].fetchPreviousStartOrEndEntry() == null
		def series = CuriousSeries.create(entries)
		assert series.values[0] == null
	}
	
	
	@Test
	void testDurationTypeEndSetAmountToNullIfNoCorrespondingStart() {
		def entries = [
			EntryFactory.makeStop('sleep', '02-01-2000 01:17')
		]
		entries[0].durationType = Entry.DurationType.END
		assert entries[0].fetchPreviousStartOrEndEntry() == null
		def series = CuriousSeries.create(entries)
		assert series.values[0] == null
	}
	
	
	@Test
	void testDailyRepeat() {
		// If entry.repeat == RepeatType.DAILY copy `amount` from `date` to
		//	 min(today's date, `repeat_end`).
		def entry = EntryFactory.make()
		entry.repeat = RepeatType.DAILY
		entry.repeatEnd = EntryFactory.START_DAY + 3
		def series = CuriousSeries.create([entry])
		assert series.size() == 4
		assert series.times[0] == EntryFactory.START_DAY
		assert series.times[1] == EntryFactory.START_DAY + 1
		assert series.times[2] == EntryFactory.START_DAY + 2
		assert series.times[3] == EntryFactory.START_DAY + 3
		assert series.values[0] == entry.amount
		assert series.values[1] == entry.amount
		assert series.values[2] == entry.amount
		assert series.values[3] == entry.amount
	}
	
	@Test
	void testRemindDaily() {
		// If repeatType == REMINDDAILY, treat it like a regular entry.
		def entries = EntryFactory.makeN(4, { it }, [repeatType: RepeatType.REMINDDAILY])
		for (i in [0, 1, 2, 3]) {
			assert entries[i].repeatType == RepeatType.REMINDDAILY
		}
		def series = CuriousSeries.create(entries)
		assert series.size() == 4
		for (i in [0, 1, 2, 3]) {
			assert series.times[i] == EntryFactory.START_DAY + i
			assert series.values[i] == entries[i].amount
		}
	}
	
	@Test
	void testRepeatTypeConcreteGhostDaily() {
		// If repeatType == DAILYCONCRETEGHOST, treat it like a regular entry.
		def entries = EntryFactory.makeN(4, { it }, [repeatType: RepeatType.DAILYCONCRETEGHOST])
		for (i in [0, 1, 2, 3]) {
			assert entries[i].repeatType == RepeatType.DAILYCONCRETEGHOST
		}
		def series = CuriousSeries.create(entries)
		assert series.size() == 4
		for (i in [0, 1, 2, 3]) {
			assert series.times[i] == EntryFactory.START_DAY + i
			assert series.values[i] == entries[i].amount
		}
	}
	
	@Test
	void testIgnoreOtherRepeatTypes() {
		// Generate 10 valid entries, turn 6 of them into ignorable repeat types.
		//	 Create the series.  Then were should only have 4 valid entries.
		
		def entries = EntryFactory.makeN(10, { it }, [repeatType: RepeatType.DAILYCONCRETEGHOST])
		assert entries.size() == 10
		entries[0].repeat = RepeatType.DAILYGHOST
		entries[1].repeat = RepeatType.REMINDDAILYGHOST
		entries[2].repeat = RepeatType.REMINDWEEKLYGHOST
		entries[3].repeat = RepeatType.CONTINUOUS
		entries[4].repeat = RepeatType.CONTINUOUSGHOST
		entries[5].repeat = RepeatType.DAILYCONCRETEGHOSTGHOST
		
		entries[6].repeat = null
		entries[7].repeat = RepeatType.DAILY
		entries[7].repeatEnd = EntryFactory.START_DAY
		entries[8].repeat = RepeatType.REMINDDAILY
		entries[9].repeat = RepeatType.DAILYCONCRETEGHOST
		def series = CuriousSeries.create(entries)
		assert Stats.sizeNotNull(series.values) == 4
	}
	
	
	
	// ****
	// Mean Product of Standard Score
	// ****
	@Test
	void testMipssReturnNullMSSIfThereAreNoOverlappingValues() {
		us.wearecurio.model.CuriousSeries series1 = CuriousSeriesFactory.makeShifted(3, { 2*it }, -2)
		us.wearecurio.model.CuriousSeries series2 = CuriousSeriesFactory.makeShifted(3, { 3*it }, 1)
		//					 VALUES
		// time  series1 series2
		// ----  ------- -------
		// -2		 2			 null
		// -1		 4			 null
		//	0		 6			 null
		//	1		 null		 3
		//	2		 null		 6
		//	3		 null		 9
		def mipss = us.wearecurio.model.CuriousSeries.mipss(series1, series2)
		assert mipss == null
	}
	
	@Test
	void testMipssNullIfOneOrMoreSeriesIsNull() {
		CuriousSeries series1 = CuriousSeriesFactory.makeShifted(3, { 3*it }, 1)
		assert CuriousSeries.mipss(null, null) == null
		assert CuriousSeries.mipss(null, series1) == null
		assert CuriousSeries.mipss(series1, null) == null
	}
	
	@Test
	void testMipssOfOneSeriesAndSeriesXIsTheMeanScoreOfSeriesX() {
		CuriousSeries series1 = CuriousSeriesFactory.makeShifted(6, { 42 }, -3)
		CuriousSeries seriesX = CuriousSeriesFactory.makeShifted(5, { it }, 0)
		// Let ST_i be the standardized version of series_i.
		//					 VALUES
		// time  series1	ST_1	 seriesX ST_X			product
		// ----  -------	----	 ------- -------	-------
		// -3		 42				1
		// -2		 42				1
		// -1		 42				1
		//	0		 42				1				1			 -1.2659	-1.2659
		//	1		 42				1				2			 -0.6324	-0.6324
		//	2		 42				1				3				0.0			 0.0
		//	3											4				0.6324
		//	4											5				1.2659
		//																				-------
		//																				-0.6325 MPSS (n=3)
		assertEquals CuriousSeries.mipss(series1, seriesX)['value'], (Double)-0.6325, (Double)0.0001
	}
	
	@Test
	void testMipssForOneOverlappingEntry() {
		us.wearecurio.model.CuriousSeries series1 = CuriousSeriesFactory.makeShifted(2, { 2*it }, -1)
		us.wearecurio.model.CuriousSeries series2 = CuriousSeriesFactory.makeShifted(3, { 3*it }, 0)
		
		// Let ST_i be the standardized version of series_i.
		//					 VALUES
		// time  series1	ST_1	 series2 ST_2 product
		// ----  -------	----	 ------- ---- -------
		// -1		 2			 -0.7071
		//	0		 4				0.7071 3			 -1.0 -0.7071
		//	1										 6				0.0
		//	2										 9				1.0
		//																		-------
		//																		-0.7071 MPSS
		
		assertEquals us.wearecurio.model.CuriousSeries.mipss(series1, series2)['value'], new Double(-0.7071), new Double(0.0001)
	}
	
	@Test
	void testMipssTwoOverlappingEntries() {
		us.wearecurio.model.CuriousSeries series1 = CuriousSeriesFactory.makeShifted(2, { 2*it }, -1)
		us.wearecurio.model.CuriousSeries series2 = CuriousSeriesFactory.makeShifted(4, { 3*it }, -2)
		// Overlap points: n=2
		// Let ST_i be the standardized version of series_i.
		//					 VALUES
		// time  series1	ST_1		series2  ST_2		 product
		// ----  -------	----		-------  ----		 -------
		//	0		 2			 -0.7071	3				-1.1619  0.8216
		//	1		 4				0.7071	6				-0.3873 -0.2739
		//	2											9				 0.3873  null
		//	3											12			 1.1619  null
		//																				 -------
		//																				 0.5477 / 2 overlap = 0.2739 MPSS
		
		def mpss = us.wearecurio.model.CuriousSeries.mipss(series1, series2)
		assertEquals mpss['value'], new Double(0.2739), new Double(0.0001)
		assert mpss['N'] == 2
	}
	
}

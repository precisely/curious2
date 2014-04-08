package us.wearecurio.model.integration

import org.junit.*

import us.wearecurio.model.User
import us.wearecurio.model.Tag
import us.wearecurio.model.Entry
import us.wearecurio.model.Series
import us.wearecurio.factories.EntryFactory
import us.wearecurio.factories.SeriesFactory
import org.apache.commons.logging.LogFactory

class SeriesTests {
  private static def log = LogFactory.getLog(this)

	@Test
	void testGetFirstEntry() {
		def entries = EntryFactory.makeN(3, { 42 })
		entries[1].date = entries[1].date - 10
		assert Series.firstEntry(entries) == entries[1]
	}

	@Test
	void testGetLastEntry() {
		def entries = EntryFactory.makeN(3, { 42 })
		entries[1].date = entries[1].date + 10
		assert us.wearecurio.model.Series.lastEntry(entries) == entries[1]
	}

	@Test
	void testValuesShouldBeDoubles() {
    log.debug "testValuesShouldBeDoubles()"
		def entries = EntryFactory.makeN(10, { 42 })
		def series = new us.wearecurio.model.Series(entries)
		assert series.values.size() == 10
		assert series.values[0] == 42
	}

	@Test
	void testKeysShouldBeDates() {
		def entries = EntryFactory.makeN(10, { 42 })
		def series = new us.wearecurio.model.Series(entries)
		assert series.times.size() == 10
		assert series.times[0] == EntryFactory.START_DAY
		assert series.times[1] == EntryFactory.START_DAY + 1
		assert series.times[9] == EntryFactory.START_DAY + 9
	}

	@Test
	void testThereShouldBeOneDateForEveryDayInTheInterval() {
		// 3 consecutive days.
		def entries = EntryFactory.makeN(3, { 42 })

		// The first day should be two days before the first of the previous days.
		def entry0 = EntryFactory.make()
		entry0.amount = 100
		entry0.date = EntryFactory.START_DAY - 2

		entries = entries + entry0
		def series = new us.wearecurio.model.Series(entries)
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
		
		def entries = EntryFactory.makeN(2, { 43 }) + EntryFactory.make() + late_entry
		// Values on each day looks like this: [[43, 42], 43, null, 42]
		
		def series = new us.wearecurio.model.Series(entries)
		def values = series.getValues()
		assert values[0] == 42.5
		assert values[1] == 43
		assert values[2] == null
		assert values[3] == 42
	}

  @Test
  void testMergedTimes() {
    def series1 = SeriesFactory.make()
    def ArrayList times = us.wearecurio.model.Series.mergedTimes(series1, series1)
    assert times == [0, 1, 2].collect { EntryFactory.START_DAY + it }

    def series2 = SeriesFactory.makeShifted(3, { 2*it }, -2)
    times = us.wearecurio.model.Series.mergedTimes(series1, series2)
    assert times == [-2, -1, 0, 1, 2].collect { EntryFactory.START_DAY + it }

    def series3 = SeriesFactory.makeShifted(4, { 3*it }, 2)
    times = us.wearecurio.model.Series.mergedTimes(series3, series2)
    assert times == [-2, -1, 0, 1, 2, 3, 4, 5].collect { EntryFactory.START_DAY + it }

  }

  @Test
  void testValuesOn() {
    def series1 = SeriesFactory.make()
    def series2 = SeriesFactory.makeShifted(3, { 2*it }, -2)
    def series3 = SeriesFactory.makeShifted(4, { 3*it }, 2)
    def timeDomain = us.wearecurio.model.Series.mergedTimes(series2, series3)
    assert us.wearecurio.model.Series.valuesOn(series1, timeDomain) ==
      [null, null, 1, 2, 3, null, null, null]
    assert us.wearecurio.model.Series.valuesOn(series2, timeDomain) ==
      [2, 4, 6, null, null, null, null, null]
    assert us.wearecurio.model.Series.valuesOn(series3, timeDomain) ==
      [null, null, null, null, 3, 6, 9, 12]
  }

  @Test
  void testPartialOverlapSeries() {
    //           VALUES
    // time  series1 series2
    // ----  ------- -------
    // -1    sin(1)  null
    // 0     sin(2)  1
    // 1     sin(3)  2
    // 2     null    3

    us.wearecurio.model.Series series1 = SeriesFactory.makeShifted(3, { Math.sin(it) }, -1)
    us.wearecurio.model.Series series2 = SeriesFactory.makeShifted(3, { it }, 0)

    def correct = -0.6552545863626498
    def ε = 0.00001
    assert us.wearecurio.model.Series.cor(series1, series2) - ε < correct &&
           us.wearecurio.model.Series.cor(series1, series2) + ε > correct
  }

  @Test
  void testReturnNullIfThereAreLessThanTwoOverlappingValues() {
    us.wearecurio.model.Series series1 = SeriesFactory.makeShifted(3, { it }, 0)
    us.wearecurio.model.Series series2 = SeriesFactory.makeShifted(3, { 2*it }, -2)
    //           VALUES
    // time  series1 series2
    // ----  ------- -------
    // -2    null    2
    // -1    null    4
    //  0    1       6      <---- There is only 1 overlapping value.
    //  1    2       null
    //  2    3       null

    assert us.wearecurio.model.Series.cor(series1, series2) == null

    us.wearecurio.model.Series series3 = SeriesFactory.makeShifted(3, { 2*it }, -2)
    //           VALUES
    // time  series2 series3
    // ----  ------- -------
    // -2    2       null
    // -1    4       null
    //  0    6       null
    //  1    null    3
    //  2    null    6
    //  3    null    9
    series3 = SeriesFactory.makeShifted(3, { 3*it }, 1)
    assert us.wearecurio.model.Series.cor(series2, series3) == null
  }

  @Test
  void testNewSeriesFromTagAndUser() {
		def entries = EntryFactory.makeN(10, { it })
    assert Tag.count() == 1
    assert User.count() == 1
    def tag = Tag.first()
		def series = us.wearecurio.model.Series.create(tag, User.first().id)
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
  }

}

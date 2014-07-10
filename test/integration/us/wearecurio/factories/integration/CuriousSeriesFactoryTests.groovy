package us.wearecurio.factories.integration

import static org.junit.Assert.*
import org.junit.*
import us.wearecurio.model.CuriousSeries
import us.wearecurio.model.User
import us.wearecurio.model.Tag
import us.wearecurio.factories.CuriousSeriesFactory
import us.wearecurio.factories.EntryFactory
import us.wearecurio.integration.CuriousTestCase


class CuriousSeriesFactoryTests extends CuriousTestCase {
	
	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testMake() {
		def series = CuriousSeriesFactory.make()
		assert series.values == [1, 2, 3]
		assert series.times == [EntryFactory.START_DAY + 0,
														EntryFactory.START_DAY + 1,
														EntryFactory.START_DAY + 2]
		assert series.userId == User.first().id
		assert series.source == Tag
		assert series.sourceId == Tag.first().id
	}

	@Test
	void testMakeShifted() {
		// Shift left by 1
		def n = 3
		def D = -1
		def series = CuriousSeriesFactory.makeShifted(n, { it }, D)
		assert series.times == [EntryFactory.START_DAY - 1,
															 EntryFactory.START_DAY - 0,
															 EntryFactory.START_DAY + 1]
		assert series.values == [1, 2, 3]

		// Shift left by 2
		D = -2
		series = CuriousSeriesFactory.makeShifted(n, { 2*it }, D)
		assert series.times == [EntryFactory.START_DAY - 2,
														EntryFactory.START_DAY - 1,
														EntryFactory.START_DAY + 0]
		assert series.values == [2, 4, 6]

		// Shift right by 2
		D = 2
		series = CuriousSeriesFactory.makeShifted(n, { 3*it }, D)
		assert series.times == [EntryFactory.START_DAY + 2,
														EntryFactory.START_DAY + 3,
														EntryFactory.START_DAY + 4]
		assert series.values == [3, 6, 9]
	}
}

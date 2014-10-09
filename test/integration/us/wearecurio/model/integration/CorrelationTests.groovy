package us.wearecurio.model.integration

import org.junit.*
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.Correlation
import us.wearecurio.services.CorrelationService
import us.wearecurio.factories.CuriousSeriesFactory
import us.wearecurio.integration.CuriousTestCase
import us.wearecurio.model.Stats


class CorrelationTests extends CuriousTestCase {
	static transactional = true

	@Before
	void setUp() {
		super.setUp()
	}

	@After
	void tearDown() {
		super.tearDown()
	}

	@Test
	void testCreate() {
		def series1 = CuriousSeriesFactory.make()
		def series2 = CuriousSeriesFactory.make()
		def score = Stats.cor(series1.values, series2.values)
		def correlation = new Correlation(series1, series2, score)
		correlation.validate()
		assert correlation.errors.allErrors == []
		assert Correlation.count() == 0
		correlation.save()
		assert Correlation.count() == 1
	}

	@Test
	void testCreateWithNanValueShouldFail() {
		def series1 = CuriousSeriesFactory.make()
		def series2 = CuriousSeriesFactory.make()
		def score = Double.NaN
		def correlation = new Correlation(series1, series2, score)
		assert Correlation.count() == 0
		correlation.save()
		assert Correlation.count() == 1
		assert Correlation.first().value == null
	}

	@Test
	void testComputingTheCorrelation() {
		def series1 = CuriousSeriesFactory.make()
		assert series1.values == [1, 2, 3]
		def series2 = CuriousSeriesFactory.make()
		assert series2.values == [1, 2, 3]
		def score = Stats.cor(series1.values, series2.values)
		assert score == 1.0
	}

	@Test
	void testCreateByTag() {
		def series1 = CuriousSeriesFactory.make()
		def series2 = CuriousSeriesFactory.make()
		def correlation = new Correlation(series1, series2)
		correlation.value = 42.0
		assert correlation.userId == User.first().id
		correlation.validate()
		assert correlation.errors.allErrors == []
		assert Correlation.count() == 0
		correlation.save()
		assert Correlation.count() == 1
		assert Correlation.last().series1Type == 'class us.wearecurio.model.Tag'
		assert Correlation.last().series2Type == 'class us.wearecurio.model.Tag'
	}

	@Test
	void testFindBySeries() {
		def series1 = CuriousSeriesFactory.make()
		def series2 = CuriousSeriesFactory.make()
		def correlation = new Correlation(series1, series2)
		correlation.value = 42.0
		assert correlation.userId == User.first().id
		correlation.validate()
		assert correlation.errors.allErrors == []
		assert Correlation.count() == 0
		correlation.save()
		assert Correlation.count() == 1
		def existing_correlation = Correlation.findWhere(userId: series1.userId, series1Id: series1.sourceId, series2Id: series2.sourceId)

		assert existing_correlation != null
		assert existing_correlation.value == 42.0

	}
}

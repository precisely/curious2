package us.wearecurio.model.integration

import org.junit.*
import us.wearecurio.model.Tag
import us.wearecurio.model.User
import us.wearecurio.model.Correlation
import us.wearecurio.services.CorrelationService
import us.wearecurio.factories.CuriousSeriesFactory
import us.wearecurio.model.Stats


class CorrelationTests {

	@Test
	void testCreate() {
		def series1 = CuriousSeriesFactory.make()
		def series2 = CuriousSeriesFactory.make()
		def corValue = Stats.cor(series1.values, series2.values)
		def correlation = new Correlation(series1, series2, corValue)
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
		def corValue = Double.NaN
		def correlation = new Correlation(series1, series2, corValue)
		assert Correlation.count() == 0
		correlation.save()
		assert Correlation.count() == 1
    assert Correlation.first().corValue == null

	}

	@Test
	void testComputingTheCorrelation() {
		def series1 = CuriousSeriesFactory.make()
		assert series1.values == [1, 2, 3]
		def series2 = CuriousSeriesFactory.make()
		assert series2.values == [1, 2, 3]
		def corValue = Stats.cor(series1.values, series2.values)
		assert corValue == 1.0
	}

	@Test
	void testCreateByTag() {
		def series1 = CuriousSeriesFactory.make()
		def series2 = CuriousSeriesFactory.make()
		def correlation = new Correlation(series1, series2)
    correlation.mipssValue = 42.0
    correlation.overlapN = 1
    assert correlation.userId == User.first().id
		correlation.validate()
	assert correlation.errors.allErrors == []
		assert Correlation.count() == 0
		correlation.save()
		assert Correlation.count() == 1
    assert Correlation.last().series1Type == 'class us.wearecurio.model.Tag'
    assert Correlation.last().series2Type == 'class us.wearecurio.model.Tag'
	}

}

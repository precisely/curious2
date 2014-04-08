package us.wearecurio.model.integration


import org.junit.*
import us.wearecurio.model.Correlation
import us.wearecurio.services.CorrelationService
import us.wearecurio.factories.SeriesFactory
import us.wearecurio.model.Stats


class CorrelationTests {

  @Test
  void testCreate() {
    def series1 = SeriesFactory.make()
    def series2 = SeriesFactory.make()
    def corValue = Stats.cor(series1.values, series2.values)
    def correlation = new Correlation(series1, series2, corValue)
    correlation.validate()
	assert correlation.errors.allErrors == []
    assert Correlation.count() == 0
    correlation.save()
    assert Correlation.count() == 1
  }
  
  @Test
  void testComputingTheCorrelation() {
	  def series1 = SeriesFactory.make()
	  assert series1.values == [1, 2, 3]
	  def series2 = SeriesFactory.make()
	  assert series2.values == [1, 2, 3]
	  def corValue = Stats.cor(series1.values, series2.values)
	  assert corValue == 1.0
  }
  
}

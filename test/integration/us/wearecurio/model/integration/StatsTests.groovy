package us.wearecurio.model.integration

import org.junit.*

import us.wearecurio.model.Stats

class StatsTests {
	def assertApproximatelyEqual(x, y) {
		Double epsilon = 0.00001
		def p = x + epsilon > y
		def q = x - epsilon < y
		assert p && q
	}
	
  @Test
  void testMean() {
    assert Stats.mean([1, 1]) == 1
	assert Stats.mean([1, 0]) == 0.5
	assert Stats.mean([-1, -2]) == -1.5
	assert Stats.mean([1, 2, 3]) == 2
	assert Stats.mean([4, 2, 5, 8, 6]) == 5
  }
  
  @Test
  void testMeanWithDoubleArray() {
	  Double[] values = [1, 1]
	  assert Stats.mean(values) == 1
  }
  @Test
  void testMeanShouldSkipNullValues() {
	  assert Stats.mean([1, null, 2]) == 1.5
  }

  
  @Test
  void testStandardDeviation() {
	  assertApproximatelyEqual Stats.sd([4, 2, 5, 8, 6]), 2.236068
	  assertApproximatelyEqual Stats.sd([1, 2]), 0.7071068
	  assert Stats.sd([42]) == null
  }
  
  @Test
  void testDotProduct() {
	  assert Stats.dot([1, 2, 3], [1, -3, 5]) == [1, -6, 15]
	  assert Stats.dot([1, null, 3], [1, -3, 5]) == [1, null, 15]
  }
  
  @Test
  void testPower() {
	  assert Stats.power([1, 2, 3], 2) == [1, 4, 9]
	  assert Stats.power([1, -2, 3], 3) == [1, -8, 27]
	  assert Stats.power([1, null, 3], 3) == [1, null, 27]
  }
  
  @Test
  void testCorrelationCalculation() {
	  assertApproximatelyEqual Stats.cor([1, 0], [0, 1]), -1.0
	  assertApproximatelyEqual Stats.cor([1, 0, 0], [0, 1, 0]), -0.5
	  assertApproximatelyEqual Stats.cor([1, 0, 0, 0], [0, 1, 0, 0]), -0.33333
	  assertApproximatelyEqual Stats.cor([1, 0, 0, 0, 0], [0, 1, 0, 0, 0]), -0.25
	  assertApproximatelyEqual Stats.cor([1, 2, 3], [1, 2, 3]), 1.0
  }
  
  @Test
  void testCorrelationNotEnoughData() {
	  assert Stats.cor([1], [42]) == null
  }

  @Test
  void testStandardize() {
    def standardizedSeries = Stats.standardize([null, 1, 0])
	  assert standardizedSeries[0] == null
    assertApproximatelyEqual standardizedSeries[1], 0.707106
    assertApproximatelyEqual standardizedSeries[2], -0.707106
    assert Stats.standardize([null, 1, 0]) == [null, 0.7071067811865475, -0.7071067811865475]
    assert Stats.standardize([42, 0, 1]) == [1.1544491977924927, -0.5980881386132764, -0.5563610591750437]
  }

  @Test
  void testSum() {
    // A version of sum that is resilient to null values.
    assert Stats.sum([0, 1, 2, 3]) == 6
    assert Stats.sum([0, null, 2, 3]) == 5
  }

  @Test
  void testSizeNotNull() {
    assert Stats.dot([null, 1 , 2], [4, null, 6]) == [null, null, 12]
    assert Stats.sizeNotNull([0, 1, 2], [4, 5, 6]) == 3
    assert Stats.sizeNotNull([null, 1, 2], [4, 5, 6]) == 2
    assert Stats.sizeNotNull([0, 1, 2], [4, null, 6]) == 2
    assert Stats.sizeNotNull([0, null, 2], [4, null, 6]) == 2
    assert Stats.sizeNotNull([null, 1 , 2], [4, null, 6]) == 1
  }

  @Test
  void testComputeCorrelationEvenIfThereAreNullsInSeries() {
    assertApproximatelyEqual Stats.cor([null, 1, 0], [42, 0, 1]), -0.029505500829884013
	  assertApproximatelyEqual Stats.cor([1, 42, 0, 0], [0, null, 1, 0]), -0.013852
	  assertApproximatelyEqual Stats.cor([1, 0, 0, 0, null], [0, 1, 0, 0, 42]), -0.00892
  }

  @Test
  void testCorrelationShouldReturnNullIfStandardDeviationIsZero() {
    assert Stats.cor([1, 1, 1], [1, 2, 3]) == null
  }

}

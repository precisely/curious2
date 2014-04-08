package us.wearecurio.factories

import us.wearecurio.model.Series
import us.wearecurio.model.Tag

import us.wearecurio.factories.EntryFactory

class SeriesFactory {
  public static def make(n=3, valueFun={ it }) {
		def entries = EntryFactory.makeN(n, valueFun)
		def tagId = entries.first().tagId
		def userId = entries.first().userId
		def series = new Series(entries, Tag, tagId, userId)
		series.save()
		series
  }

  public static def makeShifted(n=3, valueFun={ it }, D) {
    // INPUT: D is the shift (D < 0 shifts the series to the left).
    def series = make(n, valueFun)
    series.times = series.times.collect { it + D }
		series.save()
		series
  }
}

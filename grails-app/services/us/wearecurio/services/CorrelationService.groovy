package us.wearecurio.services

import org.apache.commons.logging.LogFactory

import us.wearecurio.utility.Utils
import us.wearecurio.model.Correlation
import us.wearecurio.model.Series
import us.wearecurio.model.Stats
import us.wearecurio.model.Entry
import us.wearecurio.model.User

class CorrelationService {

	private static def log = LogFactory.getLog(this)

	static transactional = true
	
  def entryService
  def tagService

  // NB: Series x and y can be generated from single tags or TagGroup
  //       instances.  See the Series contstructors for details.
  def correlate(Series x, Series y) {
    // Make sure they have the same start and end datetimes.
    if (x.start != y.start) {
      throw new Exception("Series X has a different start time than series Y.")
    } else if (x.end != y.end) {
      throw new Exception("Series X has a different end time than series Y.")
    } else if (x.size() != y.size()) {
      throw new Exception("Series X has a different length than series Y.")
    }

    cor(x, y)
  }

  def correlateToChangesInY(Series x, Series y) {
    Series edgeSeriesOfY = detectEdges(y)
    cor(x, edgeSeriesOfY)
  }

  def cor(Series x, Series y) {
    Stats.cor(x.values, y.values)
  }

  def updateAllUserCorrelations() {
    User.findAll().each { user ->
      updateUserCorrelations(user)
    }
  }

  def saveCorrelation(Series series1, Series series2) {
	  def value = Stats.cor(series1.values, series2.values)
	  def correlation = new Correlation(series1, series2, value)
	  correlation.save()
  }
  
  def updateUserCorrelations(def user, startTime=null, stopTime=null) {
    startTime = startTime ?: entryService.userStartTime(user.id)
    stopTime = stopTime ?: entryService.userStopTime(user.id)
    iterateOverTagPairs(user, { tag1, tag2 ->
      def series1 = Series.create(tag1, user.id)
      def series2 = Series.create(tag2, user.id)
      saveCorrelation(series1, series2)
    })
  }

  def iterateOverTagPairs(user, f) {
    def tags = Entry.findAllWhere('userId': user.id.toLong()).collect { it.tag }.unique()
    //tag_groups = tagService.getTagGroupsByUser(user.id)
    def accum = []
    for (tag1 in tags) {
      for (tag2 in tags) {
		    accum << f(tag1, tag2)
      }
    }
    accum
  }

  def iterateOverTagPairsAsSeries(user, user_defined_cb) {
    def series_cb = { tag1, tag2 ->
      def series1 = Series.create(tag1, user.id)
      def series2 = Series.create(tag2, user.id)
      user_defined_cb(series1, series2)
    }
    iterateOverTagPairs(user, series_cb)
  }
}
